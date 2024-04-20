package com.sunnychung.application.multiplatform.hellohttp.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jayway.jsonpath.JsonPath
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.error.PostflightError
import com.sunnychung.application.multiplatform.hellohttp.extension.GrpcRequestExtra
import com.sunnychung.application.multiplatform.hellohttp.extension.toHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.helper.CustomCodeExecutor
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestInput
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestResponse
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.LongDuplicateContainer
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponseByResponseTime
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.LiteCallData
import com.sunnychung.application.multiplatform.hellohttp.network.TransportClient
import com.sunnychung.application.multiplatform.hellohttp.network.hostFromUrl
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.upsert
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Structure:
 *
 *                                     +--> ApacheHttpTransportClient
 *                                     |
 * AppView --> NetworkClientManager <--+--> WebSocketTransportClient
 *                                     |
 *                                     +--> PersistResponseManager --> Repository
 *
 * There are cyclic dependencies between NetworkClientManager and *TransportClient
 */
class NetworkClientManager : CallDataStore {

    private val projectCollectionRepository by lazy { AppContext.ProjectCollectionRepository }
    private val requestCollectionRepository by lazy { AppContext.RequestCollectionRepository }
    private val apiSpecificationCollectionRepository by lazy { AppContext.ApiSpecificationCollectionRepository }
    private val persistResponseManager by lazy { AppContext.PersistResponseManager }
    private val httpTransportClient by lazy { AppContext.HttpTransportClient }
    private val webSocketTransportClient by lazy { AppContext.WebSocketTransportClient }
    private val graphqlSubscriptionTransportClient by lazy { AppContext.GraphqlSubscriptionTransportClient }
    private val grpcTransportClient by lazy { AppContext.GrpcTransportClient }

    private val callDataMap = ConcurrentHashMap<String, CallData>()
    private val liteCallDataMap = ConcurrentHashMap<String, LiteCallData>()
    private val requestExampleToCallMapping = mutableMapOf<String, String>()
    private val subprojectHostToLiteCallMapping = mutableMapOf<String, String>()

    private val callIdFlow = MutableStateFlow<String?>(null)

    override fun provideCallDataStore(): ConcurrentHashMap<String, CallData> = callDataMap
    override fun provideLiteCallDataStore(): ConcurrentHashMap<String, LiteCallData> = liteCallDataMap

    fun fireRequest(
        request: UserRequestTemplate,
        requestExampleId: String,
        environment: Environment?,
        projectId: String,
        subprojectId: String,
        fireType: UserResponse.Type,
        parentLoadTestState: LoadTestState? = null,
        client: Any? = null,
    ) : CallData {
        val callData = try {
            val networkRequest = request.toHttpRequest(
                exampleId = requestExampleId,
                environment = environment
            ).run {
                if (request.application == ProtocolApplication.Grpc) {
                    val apiSpec = runBlocking { apiSpecificationCollectionRepository.read(ApiSpecDI(projectId)) }!!
                        .grpcApiSpecs.first { it.id == request.grpc!!.apiSpecId }
                    copy(extra = (extra as GrpcRequestExtra).copy(apiSpec = apiSpec))
                } else {
                    this
                }
            }
            request.examples.firstOrNull { it.id == requestExampleId }?.let {
                if (!request.isExampleBase(it) && it.overrides?.isOverridePreFlightScript == false) {
                    request.examples.first()
                } else {
                    it
                }
            }?.let { // TODO change it to non-blocking
                if (it.preFlight.executeCode.isNotBlank()) {
                    var hasKilled = false
                    var executeException: Throwable? = null
                    val scriptExecuteThread = Thread {
                        try {
                            CustomCodeExecutor(code = it.preFlight.executeCode)
                                .executePreFlight(networkRequest, environment)
                        } catch (e: Throwable) {
                            executeException = e
                        }
                    }
                    val killThread = Thread {
                        Thread.sleep(1000L) // maximum execute for 1s
                        if (scriptExecuteThread.isAlive) {
                            hasKilled = true
                            log.d { "Killing script thread" }
                            try {
                                scriptExecuteThread.interrupt()
                                scriptExecuteThread.stop()
                            } catch (_: Throwable) {}
                        }
                    }
                    scriptExecuteThread.start()
                    killThread.start()
                    scriptExecuteThread.join()
                    killThread.interrupt()
                    if (hasKilled) {
                        throw RuntimeException("Custom script was running for too long time and has been killed")
                    } else if (executeException != null) {
                        throw executeException!!
                    }
                }
            }

            val (postFlightHeaderVars, postFlightBodyVars) = request.getPostFlightVariables(
                exampleId = requestExampleId,
                environment = environment
            )
            val postFlightEnvironment = environment

            val postFlightAction =
                if (request.application != ProtocolApplication.WebSocket &&  postFlightEnvironment != null && (postFlightHeaderVars.isNotEmpty() || postFlightBodyVars.isNotEmpty())) {
                    { resp: UserResponse ->
                        postFlightHeaderVars.forEach { v -> // O(n^2)
                            try {
                                val variable = postFlightEnvironment.variables.firstOrNull { it.key == v.key }
                                val value = resp.headers?.first { it.first == v.value }?.second ?: ""
                                if (variable != null) {
                                    variable.value = value
                                } else {
                                    postFlightEnvironment.variables += UserKeyValuePair(
                                        id = uuidString(),
                                        key = v.key,
                                        value = value,
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                }
                            } catch (e: Throwable) {
                                throw PostflightError(variable = v.key, cause = e)
                            }
                        }
                        if (postFlightBodyVars.isNotEmpty()) {
                            val responseBody = if (resp.isStreaming()) {
                                resp.payloadExchanges?.filter { it.type == PayloadMessage.Type.IncomingData }?.lastOrNull()?.data?.decodeToString()
                            } else {
                                resp.body?.decodeToString()
                            }
                            val context = if (resp.application == ProtocolApplication.Grpc || resp.headers?.firstOrNull {
                                    it.first.equals(
                                        "content-type",
                                        ignoreCase = true
                                    )
                                }?.second?.contains("json", ignoreCase = true) == true) {
                                JsonPath.parse(responseBody)
                            } else {
                                null
                            }
                            postFlightBodyVars.forEach { v ->
                                try {
                                    val variable = postFlightEnvironment.variables.firstOrNull { it.key == v.key }
                                    val value = v.value.let {
                                        if (it == "$") {
                                            responseBody
                                        } else {
                                            context?.read<Any?>(it)?.toString()
                                        }
                                    } ?: ""
                                    if (variable != null) {
                                        variable.value = value
                                    } else {
                                        postFlightEnvironment.variables += UserKeyValuePair(
                                            id = uuidString(),
                                            key = v.key,
                                            value = value,
                                            valueType = FieldValueType.String,
                                            isEnabled = true
                                        )
                                    }
                                } catch (e: Throwable) {
                                    throw PostflightError(variable = v.key, cause = e)
                                }
                            }
                        }
                        projectCollectionRepository.notifyUpdated(ProjectAndEnvironmentsDI())
                    }
                } else {
                    null
                }

            val networkManager = networkRequest.getNetworkManager()

            networkManager.sendRequest(
                request = networkRequest,
                requestExampleId = requestExampleId,
                requestId = request.id,
                subprojectId = subprojectId,
                postFlightAction = postFlightAction,
                httpConfig = environment?.httpConfig ?: HttpConfig(),
                sslConfig = environment?.sslConfig ?: SslConfig(),
                fireType = fireType,
                parentLoadTestState = parentLoadTestState,
                client = client,
            )
        } catch (error: Throwable) {
            val d = CallData(
                id = uuidString(),
                subprojectId = subprojectId,
                sslConfig = environment?.sslConfig ?: SslConfig(),
                response = UserResponse(
                    id = uuidString(),
                    requestExampleId = requestExampleId,
                    requestId = request.id,
                    isError = true,
                    errorMessage = error.message
                ),
                fireType = fireType,

                events = emptySharedFlow(),
                eventsStateFlow = MutableStateFlow(null),
                outgoingBytes = emptySharedFlow(),
                incomingBytes = emptySharedFlow(),
                optionalResponseSize = AtomicInteger(0),
                cancel = {},
            )
            log.d(error) { "Got error while firing request" }

            if (parentLoadTestState != null) {
                d.complete()
                onCompleteResponse(d)
//                callDataMap[parentLoadTestState.callId]?.cancel?.invoke(error)
            }

            // `networkManager.sendRequest` would update callDataMap, but on error nobody updates
            // so manually update here
            callDataMap[d.id] = d
            d
        }
        if (fireType != UserResponse.Type.LoadTestChild) {
            val oldCallId = requestExampleToCallMapping.put(requestExampleId, callData.id)
            if (oldCallId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    callDataMap[oldCallId]?.cancel?.invoke(null)
                    callDataMap.remove(oldCallId)
                }
            }
            callIdFlow.value = callData.id

            persistResponseManager.registerCall(callData)
        }
        if (!callData.response.isError) {
            callData.isPrepared = true
            if (parentLoadTestState != null) {
                parentLoadTestState.numRequestsSent.incrementAndGet()
            }
        }
        return callData
    }

    private fun HttpRequest.getNetworkManager(): TransportClient = when (this.application) {
        ProtocolApplication.Graphql -> graphqlSubscriptionTransportClient
        ProtocolApplication.WebSocket -> webSocketTransportClient
        ProtocolApplication.Grpc -> grpcTransportClient
        else -> httpTransportClient
    }

    fun fireLoadTestRequests(
        input: LoadTestInput,
        request: UserRequestTemplate,
        requestExampleId: String,
        environment: Environment?,
        projectId: String,
        subprojectId: String,
    ) {
        with (CoroutineScope(Dispatchers.IO)) {
            launch {
                val coroutineContext = currentCoroutineContext()
                val input = input.copy(
                    requestId = request.id,
                    requestExampleId = requestExampleId,
                    application = ProtocolApplication.Http,
//                requestData = // TODO refactor into specific transport manager
                )
                // TODO refactor into specific transport manager
                val loadTestState = LoadTestState(input = input, startAt = KInstant.now(), callId = uuidString()) { resp ->
                    if (resp.isError) {
                        LoadTestResponse.Category.ClientError
                    } else {
                        if (resp.statusCode?.let { it >= 200 && it < 300 } == true) {
                            LoadTestResponse.Category.Success
                        } else {
                            LoadTestResponse.Category.ServerError
                        }
                    }
                }

                log.d { "Load test call #${loadTestState.callId}" }

                var isCompleted = false
                val networkRequest = request.toHttpRequest(
                    exampleId = requestExampleId,
                    environment = environment
                )
                val networkManager = networkRequest.getNetworkManager()

                val client = networkManager.createReusableNonInspectableClient(
                    parentCallId = loadTestState.callId,
                    httpConfig = environment?.httpConfig ?: HttpConfig(),
                    sslConfig = environment?.sslConfig ?: SslConfig(),
                )

                val callData = networkManager.createCallData(
                    callId = loadTestState.callId,
                    requestBodySize = null,
                    requestExampleId = requestExampleId,
                    requestId = request.id,
                    subprojectId = subprojectId,
                    sslConfig = environment?.sslConfig ?: SslConfig(),
                    fireType = UserResponse.Type.LoadTest,
                    loadTestState = loadTestState,
                )
                callData.cancel = { e ->
                    isCompleted = true
                    callData.status = ConnectionStatus.DISCONNECTED
                    callData.response.loadTestResult = runBlocking { loadTestState.toResult(1000L) }
                    networkManager.emitEvent(callData.id, "Completed")
                    coroutineContext.cancel(e?.let { CancellationException("Cancelled due to error: ${e.message}", e) })
                    callData.cancel = {}
                }
                callData.status = ConnectionStatus.CONNECTING
                callData.response.startAt = KInstant.now()
                persistResponseManager.registerCall(callData)

                val endTime = callData.response.startAt!! + input.intendedDuration

                val oldCallId = requestExampleToCallMapping.put(requestExampleId, callData.id)
                if (oldCallId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        callDataMap[oldCallId]?.cancel?.invoke(null)
                        callDataMap.remove(oldCallId)
                    }
                }
                callIdFlow.value = callData.id

                val jobs = (1..input.numConcurrent).map { i ->
                    launch {
                        do {
                            log.v { "LoadTest fireRequest C#$i" }
                            val call = fireRequest(
                                request = request,
                                requestExampleId = requestExampleId,
                                environment = environment,
                                projectId = projectId,
                                subprojectId = subprojectId,
                                fireType = UserResponse.Type.LoadTestChild,
                                parentLoadTestState = loadTestState,
                                client = client,
                            )
                            call.awaitComplete()
                            log.v { "LoadTest complete C#$i" }
                            launch {
                                onCompleteResponse(call)
                                callDataMap.remove(call.id) // must discard child CallData to avoid memory leak
                                log.v { "LoadTest onCompleteResponse C#$i" }
                            }
                        } while (
                            callData.status != ConnectionStatus.DISCONNECTED // not cancelled
                            && !call.response.isError // not client-side error
                            && KInstant.now() < endTime
                        )
                    }
                }

                val reportJob = launch {
                    while (!isCompleted) {
                        callData.response.loadTestResult = loadTestState.toResult(1000L)
                        networkManager.emitEvent(callData.id, "update report")
                        delay(1000)
                    }
                    callData.response.loadTestResult = loadTestState.toResult(1000L)
                    networkManager.emitEvent(callData.id, "update report")
                }

                jobs.forEach { it.join() }
                callData.response.endAt = KInstant.now()
                isCompleted = true
                reportJob.join()
                callData.response.loadTestResult = loadTestState.toResult(1000L)
                callData.status = ConnectionStatus.DISCONNECTED
                when (client) {
                    is Closeable -> {
                        client.close()
                        log.d { "Closed long client" }
                    }
                }
                System.gc()
                networkManager.emitEvent(callData.id, "Completed")
                log.d { "Complete load test. Result: ${callData.response.loadTestResult}" }
            }
        }
    }

    private fun onCompleteResponse(call: CallData) {
        val response = call.response
        call.loadTestState?.let { loadTestState ->
            val endAt = response.endAt ?: KInstant.now()
            loadTestState.latenciesMs += LongDuplicateContainer((endAt - response.startAt!!).toMilliseconds())
            loadTestState.responsesOverResponseTime += UserResponseByResponseTime(response)
        }
    }

    fun sendPayload(selectedRequestExampleId: String, payload: String, environment: Environment?) {
        val resolvedPayload = VariableResolver(environment).resolve(payload)
        getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.sendPayload(resolvedPayload) }
    }

    fun sendEndOfStream(selectedRequestExampleId: String) {
        getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.sendEndOfStream() }
    }

    fun cancel(selectedRequestExampleId: String) {
        getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.cancel(null) }
    }

    private fun <T> emptySharedFlow() = emptyFlow<T>().shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly)

    private fun getCallDataByRequestExampleId(requestExampleId: String) =
        requestExampleToCallMapping[requestExampleId]
            ?.let { callDataMap[it] }

    fun getResponseByRequestExampleId(requestExampleId: String) =
        getCallDataByRequestExampleId(requestExampleId)
            ?.response

    fun getStatusByRequestExampleId(requestExampleId: String) =
        getCallDataByRequestExampleId(requestExampleId)
            ?.status
            ?: ConnectionStatus.DISCONNECTED

    @Composable
    fun subscribeToRequestExampleCall(requestExampleId: String) =
        getCallDataByRequestExampleId(requestExampleId)
            ?.eventsStateFlow
            ?.onEach { log.d { "callDataUpdates onEach ${it?.event}" } }
            ?.collectAsState(null)

    @Composable
    fun subscribeToNewRequests() = callIdFlow.collectAsState(null)

    private fun createLiteCallData(): LiteCallData {
        return LiteCallData(
            id = uuidString(),
            isConnecting = MutableStateFlow(false),
            cancel = {},
        )
    }

    private fun liteCallKey(url: String, subprojectId: String) = "$subprojectId|${hostFromUrl(url)}"

    fun fetchGrpcApiSpec(url: String, environment: Environment?, projectId: String, subprojectId: String): LiteCallData {
        val url0 = VariableResolver(environment, UserRequestTemplate.ExpandByEnvironment).resolve(url)
        val call = createLiteCallData()
        call.cancel = { call.isConnecting.value = false }
        liteCallDataMap[call.id] = call

        val key = liteCallKey(url0, subprojectId)
        val oldCallId = subprojectHostToLiteCallMapping.put(key, call.id)

        call.isConnecting.value = true
        // Don't reuse coroutineScope
        // https://stackoverflow.com/questions/59996928/coroutinescope-cannot-be-reused-after-an-exception-thrown
        val job = CoroutineScope(Dispatchers.IO).launch {
            if (oldCallId != null) {
                liteCallDataMap[oldCallId]?.cancel?.invoke()
                liteCallDataMap.remove(oldCallId)
            }

            val apiSpec = try {
                grpcTransportClient.fetchServiceSpec(
                    url0,
                    environment?.sslConfig ?: SslConfig()
                )
            } catch (e: StatusRuntimeException) {
                val errorMessage = if (e.status.code == Status.Code.UNAVAILABLE && e.cause != null) {
                    "${e.message}\n${e.cause!!.message}"
                } else {
                    e.message ?: e.javaClass.name
                }
                CoroutineScope(Dispatchers.Main).launch {
                    val errorMessageVM = AppContext.ErrorMessagePromptViewModel
                    errorMessageVM.showErrorMessage(errorMessage)
                }
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                CoroutineScope(Dispatchers.Main).launch {
                    val errorMessageVM = AppContext.ErrorMessagePromptViewModel
                    errorMessageVM.showErrorMessage(e.message ?: e.javaClass.name)
                }
                throw e
            } finally {
                call.isConnecting.value = false
            }
            val subproject = projectCollectionRepository.readSubproject(ProjectAndEnvironmentsDI(), subprojectId)
            val apiSpecDI = ApiSpecDI(projectId)
            val apiSpecCollection = apiSpecificationCollectionRepository.readOrCreate(apiSpecDI) { ApiSpecCollection(id = apiSpecDI) }
            val apiSpecToSave = apiSpecCollection.grpcApiSpecs.upsert(
                entity = apiSpec,
                condition = { it.id in subproject.grpcApiSpecIds && it.name == apiSpec.name },
                update = { old, new -> new.copy(id = old.id) }
            )
            apiSpecificationCollectionRepository.notifyUpdated(apiSpecDI)
            if (subproject.grpcApiSpecIds.add(apiSpecToSave.id)) {
                projectCollectionRepository.updateSubproject(ProjectAndEnvironmentsDI(), subproject)
            }
            log.d { "after fetch grpc api spec" }
        }
        call.cancel = {
            job.cancel(null)
            call.isConnecting.value = false
        }

        return call
    }

    fun isFetchingGrpcApiSpec(url: String, environment: Environment?, subprojectId: String) =
        subprojectHostToLiteCallMapping[liteCallKey(
            url = VariableResolver(environment, UserRequestTemplate.ExpandByEnvironment).resolve(url),
            subprojectId = subprojectId
        )]
            ?.let { liteCallDataMap[it] }
            ?.isConnecting
            ?.value
            ?: false

    fun subscribeGrpcApiSpecFetchingStatus(url: String, environment: Environment?, subprojectId: String): StateFlow<Boolean> =
        subprojectHostToLiteCallMapping[liteCallKey(
            url = VariableResolver(environment, UserRequestTemplate.ExpandByEnvironment).resolve(url),
            subprojectId = subprojectId
        )]
            ?.let { liteCallDataMap[it] }
            ?.isConnecting
            ?: MutableStateFlow(false)

    fun cancelFetchingGrpcApiSpec(url: String, environment: Environment?, subprojectId: String) {
        subprojectHostToLiteCallMapping[liteCallKey(
            url = VariableResolver(environment, UserRequestTemplate.ExpandByEnvironment).resolve(url),
            subprojectId = subprojectId
        )]
            ?.let { liteCallDataMap[it] }
            ?.let { it.cancel() }
    }

}

internal interface CallDataStore {
    fun provideCallDataStore(): ConcurrentHashMap<String, CallData>
    fun provideLiteCallDataStore(): ConcurrentHashMap<String, LiteCallData>
}

package com.sunnychung.application.multiplatform.hellohttp.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.jayway.jsonpath.JsonPath
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.error.PostflightError
import com.sunnychung.application.multiplatform.hellohttp.error.PreflightError
import com.sunnychung.application.multiplatform.hellohttp.extension.GrpcRequestExtra
import com.sunnychung.application.multiplatform.hellohttp.extension.toHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.helper.CustomCodeExecutor
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.LiteCallData
import com.sunnychung.application.multiplatform.hellohttp.network.NetworkEvent
import com.sunnychung.application.multiplatform.hellohttp.network.hostFromUrl
import com.sunnychung.application.multiplatform.hellohttp.network.util.Cookie
import com.sunnychung.application.multiplatform.hellohttp.util.executeWithTimeout
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.upsert
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URI
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

    private fun setEnvironmentVariable(environment: Environment, key: String, value: String) {
        val variable = environment.variables.firstOrNull { it.key == key }
        if (variable != null) {
            variable.value = value
        } else {
            environment.variables += UserKeyValuePair(
                id = uuidString(),
                key = key,
                value = value,
                valueType = FieldValueType.String,
                isEnabled = true
            )
        }
    }

    fun fireRequest(request: UserRequestTemplate, requestExampleId: String, environment: Environment?, projectId: String, subprojectId: String, subprojectConfig: SubprojectConfiguration) {
        val networkRequest = request.toHttpRequest(
            exampleId = requestExampleId,
            environment = environment,
            subprojectConfig = subprojectConfig,
        ).run {
            if (request.application == ProtocolApplication.Grpc) {
                val apiSpec = runBlocking { apiSpecificationCollectionRepository.read(ApiSpecDI(projectId)) }!!
                    .grpcApiSpecs.first { it.id == request.grpc!!.apiSpecId }
                copy(extra = (extra as GrpcRequestExtra).copy(apiSpec = apiSpec))
            } else {
                this
            }
        }
        val callData = try {
            request.examples.firstOrNull { it.id == requestExampleId }?.let {
                if (!request.isExampleBase(it) && it.overrides?.isOverridePreFlightScript == false) {
                    request.examples.first()
                } else {
                    it
                }
            }?.let { // TODO change it to non-blocking
                if (it.preFlight.executeCode.isNotBlank()) {
                    executeWithTimeout(1.seconds()) {
                        CustomCodeExecutor(code = it.preFlight.executeCode)
                            .executePreFlight(networkRequest, environment)
                    }
                }
            }

            val preFlightVars = request.getPreFlightVariables(
                exampleId = requestExampleId,
                environment = environment
            )
            val preFlightHeaderVars = preFlightVars.updateVariablesFromHeader
            val preFlightBodyVars = preFlightVars.updateVariablesFromBody
            val preFlightQueryParamVars = preFlightVars.updateVariablesFromQueryParameters
            val graphqlVars = preFlightVars.updateVariablesFromGraphqlVariables
            if (environment != null) {
                preFlightHeaderVars.forEach { v -> // O(n^2)
                    try {
                        val value = networkRequest.headers.firstOrNull { it.first == v.value }?.second
                        if (value != null) {
                            setEnvironmentVariable(environment = environment, key = v.key, value = value)
                        }
                    } catch (e: Throwable) {
                        throw PreflightError(variable = v.key, cause = e)
                    }
                }
                preFlightQueryParamVars.forEach { v -> // O(n^2)
                    try {
                        val value = networkRequest.queryParameters.firstOrNull { it.first == v.value }?.second
                        if (value != null) {
                            setEnvironmentVariable(environment = environment, key = v.key, value = value)
                        }
                    } catch (e: Throwable) {
                        throw PreflightError(variable = v.key, cause = e)
                    }
                }
                if (preFlightBodyVars.isNotEmpty()) {
                    val jsonBodyContext = if (networkRequest.contentType == ContentType.Json) {
                        JsonPath.parse((networkRequest.body as StringBody).value)
                    } else null
                    val requestParameterMap = if(networkRequest.body is RequestBodyWithKeyValuePairs) {
                        (networkRequest.body as RequestBodyWithKeyValuePairs).value.associate {
                            it.key to it.value
                        }
                    } else null
                    preFlightBodyVars.forEach { v ->
                        try {
                            val value = (v.value).let {
                                if (networkRequest.contentType == ContentType.Json) {
                                    if (it.startsWith("$.")) {
                                        jsonBodyContext?.read<Any?>(it)?.toString()
                                    } else if (it == "$") {
                                        (networkRequest.body as? StringBody)?.value
                                    } else {
                                        null
                                    }
                                } else {
                                    requestParameterMap?.get(it)
                                }
                            }
                            if (value != null) {
                                setEnvironmentVariable(environment = environment, key = v.key, value = value)
                            }
                        } catch (e: Throwable) {
                            throw PreflightError(variable = v.key, cause = e)
                        }
                    }
                }
                if (graphqlVars.isNotEmpty()) {
                    val jsonBodyContext = JsonPath.parse((networkRequest.body as StringBody).value)
                    graphqlVars.forEach { v ->
                        try {
                            val value = (v.value).let {
                                jsonBodyContext?.read<Any?>(it.replaceFirst("$.", "$.variables."))?.toString()
                            }
                            if (value != null) {
                                setEnvironmentVariable(environment = environment, key = v.key, value = value)
                            }
                        } catch (e: Throwable) {
                            throw PreflightError(variable = v.key, cause = e)
                        }
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
                                val value = resp.headers?.first { it.first == v.value }?.second ?: ""
                                setEnvironmentVariable(environment = postFlightEnvironment, key = v.key, value = value)
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
                                    val value = v.value.let {
                                        if (it == "$") {
                                            responseBody
                                        } else {
                                            context?.read<Any?>(it)?.toString()
                                        }
                                    } ?: ""
                                    setEnvironmentVariable(environment = postFlightEnvironment, key = v.key, value = value)
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

            val networkManager = when (networkRequest.application) {
                ProtocolApplication.Graphql -> graphqlSubscriptionTransportClient
                ProtocolApplication.WebSocket -> webSocketTransportClient
                ProtocolApplication.Grpc -> grpcTransportClient
                else -> httpTransportClient
            }

            networkManager.sendRequest(
                request = networkRequest,
                requestExampleId = requestExampleId,
                requestId = request.id,
                subprojectId = subprojectId,
                postFlightAction = postFlightAction,
                httpConfig = environment?.httpConfig ?: HttpConfig(),
                sslConfig = environment?.sslConfig ?: SslConfig(),
                subprojectConfig = subprojectConfig,
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
                status = ConnectionStatus.DISCONNECTED,
                events = emptySharedFlow(),
                eventsStateFlow = MutableStateFlow(null),
                outgoingBytes = emptySharedFlow(),
                incomingBytes = emptySharedFlow(),
                optionalResponseSize = AtomicInteger(0),
                cancel = {},
            )
            log.d(error) { "Got error while firing request" }
            // `networkManager.sendRequest` would update callDataMap, but on error nobody updates
            // so manually update here
            callDataMap[d.id] = d
            d
        }
        val oldCallId = requestExampleToCallMapping.put(requestExampleId, callData.id)
        if (oldCallId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                callDataMap[oldCallId]?.cancel?.invoke(null)
                callDataMap.remove(oldCallId)
            }
        }
        callIdFlow.value = callData.id
        if (environment != null && subprojectConfig.isCookieEnabled()) { // set cookie after flight
            val originalCallDataEnd = callData.end
            callData.end = {
                val setCookieHeaders = callData.response.headers
                    ?.filter { it.first.equals("Set-Cookie", ignoreCase = true) && it.second.isNotBlank() }
                    ?: emptyList()

                if (setCookieHeaders.isNotEmpty()) {
                    val uri = URI(networkRequest.url)
                    environment.cookieJar.store(uri, setCookieHeaders.map { it.second }, ::verifyCookie)
                    log.d { "Cookie JAR = " + environment.cookieJar.toString() }
                    persistResponseManager.updateSubproject(subprojectId)
                }

                originalCallDataEnd?.invoke()
            }
        }
        persistResponseManager.registerCall(callData)
        if (!callData.response.isError) {
            callData.isPrepared = true
        }
    }

    fun verifyCookie(cookie: Cookie): Boolean {
        if (cookie.value.contains("\\\$\\{\\{.*\\}\\}".toRegex())) return false
        if (cookie.value.contains("\\\$\\(\\(.*\\)\\)".toRegex())) return false
        return true
    }

    fun sendPayload(request: UserRequestTemplate, selectedRequestExampleId: String, payload: String, environment: Environment?) {
        val resolvedPayload = VariableResolver(environment, request, selectedRequestExampleId).resolve(payload)
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

    fun getResponseByRequestExampleId(requestExampleId: String): UserResponse? =
        getCallDataByRequestExampleId(requestExampleId)
            ?.response

    fun getStatusByRequestExampleId(requestExampleId: String): ConnectionStatus =
        getCallDataByRequestExampleId(requestExampleId)
            ?.status
            ?: ConnectionStatus.DISCONNECTED

    @Composable
    fun subscribeToRequestExampleCall(requestExampleId: String): State<NetworkEvent?>? =
        getCallDataByRequestExampleId(requestExampleId)
            ?.eventsStateFlow
            ?.onEach { log.d { "callDataUpdates onEach ${it?.event}" } }
            ?.collectAsState(null)

    @Composable
    fun subscribeToNewRequests() = callIdFlow.collectAsState(null)

    /**
     * For UX tests only, NOT for production.
     */
    fun cancelAllCalls(): Int {
        log.i(Throwable("cancelAllCalls() triggered")) { "cancelAllCalls() triggered" }
        val threads = callDataMap.mapNotNull { (key, call) ->
            if (call.status != ConnectionStatus.DISCONNECTED) {
                Thread {
                    log.d { "Force cancel call #$key" }
                    call.cancel(null)
                }.also { it.start() }
            } else {
                null
            }
        }
        if (threads.isNotEmpty()) {
            // wait at least this amount of time, even the threads are completed
            Thread.sleep(3000L + 30 * threads.size)

            threads.forEach {
                it.join()
            }
        }
        callDataMap.clear()
        requestExampleToCallMapping.clear()
        return threads.size
    }

    private fun createLiteCallData(): LiteCallData {
        return LiteCallData(
            id = uuidString(),
            isConnecting = MutableStateFlow(false),
            cancel = {},
        )
    }

    private fun liteCallKey(url: String, subprojectId: String) = "$subprojectId|${hostFromUrl(url)}"

    fun fetchGrpcApiSpec(request: UserRequestTemplate, exampleId: String, environment: Environment?, projectId: String, subprojectId: String): LiteCallData {
        val url0 = VariableResolver(environment, request, exampleId, UserRequestTemplate.ExpandByEnvironment)
            .resolve(request.url)
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

//    fun isFetchingGrpcApiSpec(url: String, environment: Environment?, subprojectId: String) =
//        subprojectHostToLiteCallMapping[liteCallKey(
//            url = VariableResolver(environment, UserRequestTemplate.ExpandByEnvironment).resolve(url),
//            subprojectId = subprojectId
//        )]
//            ?.let { liteCallDataMap[it] }
//            ?.isConnecting
//            ?.value
//            ?: false

    fun subscribeGrpcApiSpecFetchingStatus(request: UserRequestTemplate, exampleId: String, environment: Environment?, subprojectId: String): StateFlow<Boolean> =
        subprojectHostToLiteCallMapping[liteCallKey(
            url = VariableResolver(environment, request, exampleId, UserRequestTemplate.ExpandByEnvironment)
                .resolve(request.url),
            subprojectId = subprojectId
        )]
            ?.let { liteCallDataMap[it] }
            ?.isConnecting
            ?: MutableStateFlow(false)

    fun cancelFetchingGrpcApiSpec(request: UserRequestTemplate, exampleId: String, environment: Environment?, subprojectId: String) {
        subprojectHostToLiteCallMapping[liteCallKey(
            url = VariableResolver(environment, request, exampleId, UserRequestTemplate.ExpandByEnvironment)
                .resolve(request.url),
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

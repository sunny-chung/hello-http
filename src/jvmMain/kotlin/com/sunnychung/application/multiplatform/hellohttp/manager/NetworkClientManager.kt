package com.sunnychung.application.multiplatform.hellohttp.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jayway.jsonpath.JsonPath
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.error.PostflightError
import com.sunnychung.application.multiplatform.hellohttp.extension.GrpcRequestExtra
import com.sunnychung.application.multiplatform.hellohttp.extension.toHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.LiteCallData
import com.sunnychung.application.multiplatform.hellohttp.network.hostFromUrl
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.upsert
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
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

    fun fireRequest(request: UserRequestTemplate, requestExampleId: String, environment: Environment?, subprojectId: String) {
        val callData = try {
            val networkRequest = request.toHttpRequest(
                exampleId = requestExampleId,
                environment = environment
            ).run {
                if (request.application == ProtocolApplication.Grpc) {
                    val apiSpec = runBlocking { projectCollectionRepository.readSubproject(ProjectAndEnvironmentsDI(), subprojectId) }
                        .grpcApiSpecs.first { it.id == request.grpc!!.apiSpecId }
                    copy(extra = (extra as GrpcRequestExtra).copy(apiSpec = apiSpec))
                } else {
                    this
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
                            val responseBody = resp.body?.decodeToString()
                            val context = if (resp.headers?.firstOrNull {
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
                                            context?.read<String>(it)
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
            )
        } catch (error: Throwable) {
            val d = CallData(
                id = uuidString(),
                subprojectId = subprojectId,
                response = UserResponse(
                    id = uuidString(),
                    requestExampleId = requestExampleId,
                    requestId = request.id,
                    isError = true,
                    errorMessage = error.message
                ),

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
                callDataMap[oldCallId]?.cancel?.invoke()
                callDataMap.remove(oldCallId)
            }
        }
        callIdFlow.value = callData.id
        persistResponseManager.registerCall(callData)
        if (!callData.response.isError) {
            callData.isPrepared = true
        }
    }

    fun sendPayload(selectedRequestExampleId: String, payload: String, environment: Environment?) {
        val resolvedPayload = VariableResolver(environment).resolve(payload)
        getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.sendPayload(resolvedPayload) }
    }

    fun cancel(selectedRequestExampleId: String) {
        getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.cancel() }
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

    fun fetchGrpcApiSpec(url: String, environment: Environment?, subprojectId: String): LiteCallData {
        val call = createLiteCallData()
        call.cancel = { call.isConnecting.value = false }
        liteCallDataMap[call.id] = call

        val key = liteCallKey(url, subprojectId)
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
                    url,
                    environment?.sslConfig ?: SslConfig()
                )
            } finally {
                call.isConnecting.value = false
            }
            val subproject = projectCollectionRepository.readSubproject(ProjectAndEnvironmentsDI(), subprojectId)
            subproject.grpcApiSpecs.upsert(apiSpec, condition = { it.name == apiSpec.name }, update = { old, new -> new.copy(id = old.id) })
            log.d { "after fetch grpc api spec" }
            projectCollectionRepository.updateSubproject(ProjectAndEnvironmentsDI(), subproject)
        }
        call.cancel = {
            job.cancel(null)
            call.isConnecting.value = false
        }

        return call
    }

    fun isFetchingGrpcApiSpec(url: String, subprojectId: String) =
        subprojectHostToLiteCallMapping[liteCallKey(url, subprojectId)]
            ?.let { liteCallDataMap[it] }
            ?.isConnecting
            ?.value
            ?: false

    fun subscribeGrpcApiSpecFetchingStatus(url: String, subprojectId: String): StateFlow<Boolean> =
        subprojectHostToLiteCallMapping[liteCallKey(url, subprojectId)]
            ?.let { liteCallDataMap[it] }
            ?.isConnecting
            ?: MutableStateFlow(false)

    fun cancelFetchingGrpcApiSpec(url: String, subprojectId: String) {
        subprojectHostToLiteCallMapping[liteCallKey(url, subprojectId)]
            ?.let { liteCallDataMap[it] }
            ?.let { it.cancel() }
    }

}

internal interface CallDataStore {
    fun provideCallDataStore(): ConcurrentHashMap<String, CallData>
    fun provideLiteCallDataStore(): ConcurrentHashMap<String, LiteCallData>
}

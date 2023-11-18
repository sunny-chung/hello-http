package com.sunnychung.application.multiplatform.hellohttp.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jayway.jsonpath.JsonPath
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.error.PostflightError
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
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Structure:
 *
 *                                     +--> ApacheNetworkManager
 *                                     |
 * AppView --> NetworkClientManager <--+--> WebSocketNetworkManager
 *                                     |
 *                                     +--> PersistResponseManager --> Repository
 *
 * There are cyclic dependencies between NetworkClientManager and *NetworkManager
 */
class NetworkClientManager : CallDataStore {

    private val projectCollectionRepository by lazy { AppContext.ProjectCollectionRepository }
    private val persistResponseManager by lazy { AppContext.PersistResponseManager }
    private val httpNetworkManager by lazy { AppContext.NetworkManager }
    private val webSocketNetworkManager by lazy { AppContext.WebSocketNetworkManager }

    private val callDataMap = ConcurrentHashMap<String, CallData>()
    private val requestExampleToCallMapping = mutableMapOf<String, String>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val callIdFlow = MutableStateFlow<String?>(null)

    override fun provideCallDataStore(): ConcurrentHashMap<String, CallData> = callDataMap

    fun fireRequest(request: UserRequestTemplate, requestExampleId: String, environment: Environment?, subprojectId: String) {
        val callData = try {
            val networkRequest = request.toHttpRequest(
                exampleId = requestExampleId,
                environment = environment
            )
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

            val networkManager = if (networkRequest.application != ProtocolApplication.WebSocket) {
                httpNetworkManager
            } else {
                webSocketNetworkManager
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
            coroutineScope.launch {
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

    private fun <T> emptySharedFlow() = emptyFlow<T>().shareIn(coroutineScope, SharingStarted.Eagerly)

    private fun getCallDataByRequestExampleId(requestExampleId: String) =
        requestExampleToCallMapping[requestExampleId]
            ?.let { callDataMap[it] }

    fun getResponseByRequestExampleId(requestExampleId: String) =
        getCallDataByRequestExampleId(requestExampleId)
            ?.response

    @Composable
    fun subscribeToRequestExampleCall(requestExampleId: String) =
        getCallDataByRequestExampleId(requestExampleId)
            ?.eventsStateFlow
            ?.onEach { log.d { "callDataUpdates onEach ${it?.event}" } }
            ?.collectAsState(null)

    @Composable
    fun subscribeToNewRequests() = callIdFlow.collectAsState(null)
}

internal interface CallDataStore {
    fun provideCallDataStore(): ConcurrentHashMap<String, CallData>
}

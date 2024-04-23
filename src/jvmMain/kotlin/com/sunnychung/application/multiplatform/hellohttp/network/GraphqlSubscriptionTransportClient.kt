package com.sunnychung.application.multiplatform.hellohttp.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sunnychung.application.multiplatform.hellohttp.error.ProtocolError
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.payload.GraphqlErrorPayload
import com.sunnychung.application.multiplatform.hellohttp.model.payload.GraphqlWsMessage
import com.sunnychung.application.multiplatform.hellohttp.network.util.InspectedWebSocketClient
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GraphqlSubscriptionTransportClient(networkClientManager: NetworkClientManager) : WebSocketTransportClient(networkClientManager) {

    override fun sendRequest(
        callId: String,
        coroutineScope: CoroutineScope,
        client: Any?,
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        fireType: UserResponse.Type,
        parentLoadTestState: LoadTestState?,
    ): CallData {
        val payload = request.extra as GraphqlRequestBody

        val data = createCallData(
            callId = callId,
            coroutineScope = coroutineScope,
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            fireType = fireType,
            loadTestState = parentLoadTestState,
        )
        val callId = data.id
        val uri: URI = request.getResolvedUri()
        val out = data.response
        out.application = ProtocolApplication.WebSocket
        out.payloadExchanges = mutableListOf()
        out.requestData = RequestData(
            method = "GET",
            url = uri.toASCIIString(),
        )

        coroutineScope.launch {
            val jsonMapper = jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            val isConnected = MutableStateFlow<Boolean?>(null)
            val messages = Channel<GraphqlWsMessage<JsonNode>>()

            val client = object : InspectedWebSocketClient(
                callId = callId,
                uri = uri,
                data = data,
                request = request,
                emitEvent = { callId, event -> emitEvent(callId = callId, event = event) },
            ) {
                override fun onOpen(handshake: ServerHandshake) {
                    super.onOpen(handshake)
                    isConnected.value = true
                }

                override fun onMessage(message: String) {
                    emitEvent(callId, "Received message: $message")
                    val decoded = jsonMapper.readValue<GraphqlWsMessage<JsonNode>>(message)
                    coroutineScope.launch {
                        messages.send(decoded)
                    }
                }

                override fun onMessage(bytes: ByteBuffer) {
                    val byteArray = ByteArray(bytes.remaining())
                    bytes.get(byteArray)
                    val data = byteArray.decodeToString()
                    onMessage(data)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    super.onClose(code, reason, remote)
                    isConnected.value = false
                    out.payloadExchanges!! += PayloadMessage(
                        id = uuidString(),
                        instant = KInstant.now(),
                        type = PayloadMessage.Type.Disconnected,
                        data = "Disconnected.".encodeToByteArray()
                    )
                }

                override fun send(message: String) {
                    emitEvent(callId, "Sending message: $message")
                    super.send(message)
                }
            }
            configureWebSocketClient(client = client, callId = callId, sslConfig = sslConfig)

            suspend fun awaitConnect() = suspendCancellableCoroutine { continuation ->
                isConnected.filterNotNull()
                    .take(1)
                    .onEach { continuation.resume(it) }
                    .catch { error -> continuation.resumeWithException(error) }
                    .launchIn(coroutineScope)
                continuation.invokeOnCancellation { client.close() }
            }

            suspend fun awaitNextMessage() = messages.receive()

            fun send(message: GraphqlWsMessage<*>) {
                val encoded = jsonMapper.writeValueAsString(message)
                client.send(encoded)
            }

            data.cancel = { client.close() }

            try {
                out.startAt = KInstant.now()
                out.isCommunicating = true
                data.status = ConnectionStatus.CONNECTING

                client.connect()

                val isConnected = awaitConnect()
                if (!isConnected) throw IOException("Not connected")
                send(GraphqlWsMessage<Nothing>(type = "connection_init"))
                var message = awaitNextMessage()
                if (message.type != "connection_ack") {
                    throw ProtocolError("connection_ack was not received")
                }
                emitEvent(callId, "GraphQL WebSocket connection established")
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.Connected,
                    data = "Connected.".encodeToByteArray()
                )

                val operationId = uuidString()
                var isConnectionActive = true
                data.cancel = {
                    send(GraphqlWsMessage<Nothing>(id = operationId, type = "complete"))
                    isConnectionActive = false
                    client.close()
                    messages.close()
                }
                data.status = ConnectionStatus.OPEN_FOR_STREAMING
                send(GraphqlWsMessage(
                    id = operationId,
                    type = "subscribe",
                    payload = payload
                ))
                while (isConnectionActive) {
                    message = awaitNextMessage()
                    val messageTime = KInstant.now()
                    when (message.type) {
                        "error" -> {
                            out.payloadExchanges!! += PayloadMessage(
                                id = uuidString(),
                                instant = messageTime,
                                type = PayloadMessage.Type.IncomingData,
                                data = jsonMapper.writeValueAsBytes(GraphqlErrorPayload(message.payload))
                            )
                            isConnectionActive = false
                        }
                        "complete" -> {
                            if (message.id == operationId) {
                                isConnectionActive = false
                            }
                        }
                        "next" -> {
                            if (message.id == operationId) {
                                out.payloadExchanges!! += PayloadMessage(
                                    id = uuidString(),
                                    instant = messageTime,
                                    type = PayloadMessage.Type.IncomingData,
                                    data = jsonMapper.writeValueAsBytes(message.payload)
                                )
                            }
                        }
                    }
                }

            } catch (e: Throwable) {
                log.d(e) { "Got error in GraphQL subscription communication" }
                emitEvent(callId, "Error: ${e.message}")
            }
            out.isCommunicating = false
            data.status = ConnectionStatus.DISCONNECTED
            client.close()
        }

        return data
    }

    override fun createReusableNonInspectableClient(
        parentCallId: String,
        httpConfig: HttpConfig,
        sslConfig: SslConfig
    ): Any? {
        return null
    }
}

package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.util.InspectedWebSocketClient
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import org.java_websocket.client.DnsResolver
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.InetAddress
import java.net.URI
import java.nio.ByteBuffer

open class WebSocketTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {

    fun createDnsResolver(callId: String): DnsResolver {
        return DnsResolver { uri ->
            emitEvent(callId, "DNS resolution of domain [${uri.host}] started")
            val result = InetAddress.getByName(uri.host)
            emitEvent(callId, "DNS resolved to ${result.toString()}")
            result
        }
    }

    override fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        environment: Environment?,
        subprojectConfig: SubprojectConfiguration,
    ): CallData {
        val data = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            environment = environment,
            subprojectConfig = subprojectConfig,
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

        val client = object : InspectedWebSocketClient(
            callId = callId,
            uri = uri,
            data = data,
            request = request,
            emitEvent = { callId, event -> emitEvent(callId = callId, event = event) },
        ) {
            override fun onOpen(handshake: ServerHandshake) {
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.Connected,
                    data = "Connected".encodeToByteArray()
                )
                super.onOpen(handshake)
                data.status = ConnectionStatus.OPEN_FOR_STREAMING
            }

            override fun onMessage(message: String) {
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.IncomingData,
                    data = message.encodeToByteArray()
                )
            }

            override fun onMessage(buffer: ByteBuffer) {
                if (buffer.hasRemaining()) {
                    out.payloadExchanges!! += PayloadMessage(
                        id = uuidString(),
                        instant = KInstant.now(),
                        type = PayloadMessage.Type.IncomingData,
                        data = buffer.array().copyOfRange(buffer.position(), buffer.limit())
                    )
                }
            }

            override fun send(data: ByteArray) {
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.OutgoingData,
                    data = data
                )
                super.send(data)
            }

            override fun send(buffer: ByteBuffer) {
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.OutgoingData,
                    data = buffer.array().copyOfRange(buffer.position(), buffer.limit())
                )
                super.send(buffer)
            }

            override fun send(text: String) {
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.OutgoingData,
                    data = text.encodeToByteArray()
                )
                super.send(text)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                val appendReason = if (!reason.isNullOrEmpty()) ", reason $reason" else ""
                out.payloadExchanges!! += PayloadMessage(
                    id = uuidString(),
                    instant = KInstant.now(),
                    type = PayloadMessage.Type.Disconnected,
                    data = "Connection closed by ${if (remote) "server" else "us"} with code $code$appendReason".encodeToByteArray()
                )
                super.onClose(code, reason, remote)
                data.end()
            }
        }
        configureWebSocketClient(client = client, callId = callId, sslConfig = sslConfig)

        out.startAt = KInstant.now()
        out.isCommunicating = true
        data.cancel = { client.close() }
        data.sendPayload = {
            try {
                client.send(it)
            } catch (e: Throwable) {
                log.w(e) { "Cannot send payload" }
            }
        }
        data.status = ConnectionStatus.CONNECTING
        client.connect()

        return data
    }

    fun configureWebSocketClient(client: WebSocketClient, callId: String, sslConfig: SslConfig) {
        with (client) {
            setDnsResolver(createDnsResolver(callId))
            if (uri.scheme == "wss" && sslConfig.hasCustomConfig()) {
                setSocketFactory(createSslContext(sslConfig).sslContext.socketFactory)
            }
        }
    }


}

package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.InspectInputStream
import com.sunnychung.application.multiplatform.hellohttp.network.InspectOutputStream
import com.sunnychung.application.multiplatform.hellohttp.network.InspectedWebSocketClient
import com.sunnychung.application.multiplatform.hellohttp.util.llog
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.apache.hc.core5.net.URIBuilder
import org.java_websocket.client.DnsResolver
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.Handshakedata
import org.java_websocket.handshake.ServerHandshake
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer

open class WebSocketNetworkManager(networkClientManager: NetworkClientManager) : AbstractNetworkManager(networkClientManager) {

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
        sslConfig: SslConfig
    ): CallData {
        val data = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
        )
        val callId = data.id
        val uri: URI = request.getResolvedUri()
        val out = data.response
        out.application = ProtocolApplication.WebSocket
        out.payloadExchanges = mutableListOf()

        val client = object : InspectedWebSocketClient(
            callId = callId,
            uri = uri,
            data = data,
            request = request,
            emitEvent = { callId, event -> emitEvent(callId = callId, event = event) },
        ) {
            override fun onOpen(handshake: ServerHandshake) {
                out.payloadExchanges!! += PayloadMessage(
                    KInstant.now(),
                    PayloadMessage.Type.Connected,
                    "Connected".encodeToByteArray()
                )
                super.onOpen(handshake)
            }

            override fun onMessage(message: String) {
                out.payloadExchanges!! += PayloadMessage(
                    KInstant.now(),
                    PayloadMessage.Type.IncomingData,
                    message.encodeToByteArray()
                )
            }

            override fun onMessage(buffer: ByteBuffer) {
                if (buffer.hasRemaining()) {
                    out.payloadExchanges!! += PayloadMessage(
                        KInstant.now(),
                        PayloadMessage.Type.IncomingData,
                        buffer.array().copyOfRange(buffer.position(), buffer.limit())
                    )
                }
            }

            override fun send(data: ByteArray) {
                out.payloadExchanges!! += PayloadMessage(KInstant.now(), PayloadMessage.Type.OutgoingData, data)
                super.send(data)
            }

            override fun send(buffer: ByteBuffer) {
                out.payloadExchanges!! += PayloadMessage(
                    KInstant.now(),
                    PayloadMessage.Type.OutgoingData,
                    buffer.array().copyOfRange(buffer.position(), buffer.limit())
                )
                super.send(buffer)
            }

            override fun send(text: String) {
                out.payloadExchanges!! += PayloadMessage(
                    KInstant.now(),
                    PayloadMessage.Type.OutgoingData,
                    text.encodeToByteArray()
                )
                super.send(text)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                val appendReason = if (!reason.isNullOrEmpty()) ", reason $reason" else ""
                out.payloadExchanges!! += PayloadMessage(
                    out.endAt!!,
                    PayloadMessage.Type.Disconnected,
                    "Connection closed by ${if (remote) "server" else "us"} with code $code$appendReason".encodeToByteArray()
                )
                super.onClose(code, reason, remote)
            }
        }
        configureWebSocketClient(client = client, callId = callId, sslConfig = sslConfig)

        out.startAt = KInstant.now()
        out.isCommunicating = true
        data.cancel = { client.close() }
        data.sendPayload = { client.send(it) }

        client.connect()

        return data
    }

    fun configureWebSocketClient(client: WebSocketClient, callId: String, sslConfig: SslConfig) {
        with (client) {
            setDnsResolver(createDnsResolver(callId))
            if (uri.scheme == "wss" && sslConfig.isInsecure == true) {
                setSocketFactory(createSslContext(sslConfig).first.socketFactory)
            }
        }
    }


}

package com.sunnychung.application.multiplatform.hellohttp.network.util

import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurity
import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurityType
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.RawPayload
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.llog
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.Handshakedata
import org.java_websocket.handshake.ServerHandshake
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import javax.net.ssl.HandshakeCompletedEvent

abstract class InspectedWebSocketClient(
    val callId: String,
    uri: URI,
    val data: CallData,
    request: HttpRequest,
    val out: UserResponse = data.response,
    val emitEvent: (String, String) -> Unit,
) : WebSocketClient(
    uri,
    object : Draft_6455() {
        override fun translateHandshake(buf: ByteBuffer): Handshakedata {
            val copy = buf.duplicate()
            val line = readStringLine(copy)
            if (line != null) {
                val s = line.split(" ", limit = 3)
                if (s[0] == "HTTP/1.1") {
                    out.statusCode = s[1].toIntOrNull()
                    out.statusText = s[2]
                }
            }

            return super.translateHandshake(buf)
        }

        override fun copyInstance(): Draft {
            return this
        }
    },
    request.headers.toMap(), // TODO allow repeated headers
) {

    override fun wrapInputStream(`is`: InputStream): InputStream {
        return InspectInputStream(`is`, data.incomingBytes as MutableSharedFlow<RawPayload>)
    }

    override fun wrapOutputStream(os: OutputStream): OutputStream {
        return InspectOutputStream(os, data.outgoingBytes as MutableSharedFlow<RawPayload>)
    }

    override fun onConnectStart(address: InetSocketAddress) {
        emitEvent(callId, "Connecting to $address")
    }

    override fun onConnected(address: InetSocketAddress) {
        CallDataUserResponseUtil.onConnected(out)
        emitEvent(callId, "Connected to $address")
    }

    override fun onSSLHandshakeCompleted(event: HandshakeCompletedEvent) {
        val protocol = event.session.protocol
        val cipherSuite = event.cipherSuite
        val localPrincipal = event.localPrincipal
        val peerPrincipal = event.peerPrincipal
        CallDataUserResponseUtil.onTlsUpgraded(
            callData = data,
            localCertificates = event.session.localCertificates,
            peerCertificates = event.session.peerCertificates
        )
        var event = "Established TLS upgrade with protocol '$protocol', cipher suite '$cipherSuite'"
        event += ".\n\n" +
                "Client principal = $localPrincipal\n" +
//                                "Client certificates = ${localCertificates?.firstOrNull()}\n" +
                "\n" +
                "Server principal = $peerPrincipal\n"
//                                "Server certificates = ${peerCertificates?.firstOrNull()}\n"
        emitEvent(callId, event)
    }

    override fun onOpen(handshake: ServerHandshake) {
        out.statusCode = handshake.httpStatus.toInt()
        out.statusText = handshake.httpStatusMessage
        out.headers = handshake.iterateHttpFields().asSequence().map { it to handshake.getFieldValue(it) }.toList()
        data.status = ConnectionStatus.CONNECTED
        emitEvent(callId, "Connected with WebSocket")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        out.endAt = KInstant.now()
        data.status = ConnectionStatus.DISCONNECTED
        out.isCommunicating = false
        val appendReason = if (!reason.isNullOrEmpty()) ", reason $reason" else ""
        emitEvent(callId, "WebSocket channel closed by ${if (remote) "server" else "us"} with code $code$appendReason")
    }

    override fun onError(error: Exception) {
        // WebSocketClient implementation interrupts the thread executing `onError` very early,
        // not allowing error escalating completes
        CoroutineScope(Dispatchers.Default).launch {
            out.errorMessage = error.message
            out.isError = true
            emitEvent(callId, "Error encountered: ${error.message}")
            llog.w(error) { "WebSocket error" }
        }
    }

}

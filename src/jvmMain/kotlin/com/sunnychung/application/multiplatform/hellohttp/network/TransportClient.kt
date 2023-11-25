package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

interface TransportClient {
    fun getCallData(callId: String): CallData?

    fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig
    ): CallData
}

class NetworkEvent(val callId: String, val instant: KInstant, val event: String)
class CallData(
    val id: String,
    val subprojectId: String,
    var isPrepared: Boolean = false,
    var status: ConnectionStatus = ConnectionStatus.PREPARING,

    val events: SharedFlow<NetworkEvent>,
    val eventsStateFlow: StateFlow<NetworkEvent?>,
    val outgoingBytes: SharedFlow<RawPayload>,
    val incomingBytes: SharedFlow<RawPayload>,
    val optionalResponseSize: AtomicInteger,
    val response: UserResponse,

    var cancel: () -> Unit,
    var sendPayload: (String) -> Unit = {},
)

sealed interface RawPayload {
    val instant: KInstant
    val payload: ByteArray
}

/**
 * PREPARING --> CONNECTING --> [CONNECTED] --> [OPEN_FOR_STREAMING] --> DISCONNECTED
 *                                   ^                  |
 *                                   +------------------+
 */
enum class ConnectionStatus {
    PREPARING, CONNECTING, CONNECTED, OPEN_FOR_STREAMING, DISCONNECTED;

    fun isConnectionActive() = this >= CONNECTING && this < DISCONNECTED
}

data class Http1Payload(override val instant: KInstant, override val payload: ByteArray) : RawPayload

data class Http2Frame(override val instant: KInstant, val streamId: Int?, val content: String) : RawPayload {
    override val payload: ByteArray
        get() = content.encodeToByteArray()
}

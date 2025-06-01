package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

interface TransportClient {
    fun getCallData(callId: String): CallData?

    fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        environment: Environment?,
        subprojectConfig: SubprojectConfiguration,
    ): CallData
}

class NetworkEvent(val callId: String, val instant: KInstant, val event: String)
class CallData(
    val id: String,
    val subprojectId: String,
    var isPrepared: Boolean = false,
    var status: ConnectionStatus = ConnectionStatus.PREPARING,

    val sslConfig: SslConfig,

    val events: SharedFlow<NetworkEvent>,
    val eventsStateFlow: StateFlow<NetworkEvent?>,
    val outgoingBytes: SharedFlow<RawPayload>,
    val incomingBytes: SharedFlow<RawPayload>,
    val optionalResponseSize: AtomicInteger,
    val response: UserResponse,

    val jobs: MutableList<Job> = mutableListOf(),

    var cancel: (Throwable?) -> Unit,
    var sendPayload: (String) -> Unit = {},
    var sendEndOfStream: () -> Unit = {},
    var end: (() -> Unit)? = null,
) {
    /**
     * Signals a call is completed and releases resources.
     * Calling this function manually is necessary, as CallData is supposed to live in memory after completion.
     */
    fun end() {
        this.end?.invoke()
        CoroutineScope(Dispatchers.IO).launch {
            delay(1.seconds().millis)
            jobs.forEach { it.cancel() }
            consumePayloads(isComplete = true)
        }
        sendPayload = {}
        sendEndOfStream = {}
        cancel = {}
    }
}

class LiteCallData(
    val id: String,
    var isConnecting: MutableStateFlow<Boolean>,
    var cancel: () -> Unit,
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
    fun isNotIdle() = this != DISCONNECTED
}

data class Http1Payload(override val instant: KInstant, override val payload: ByteArray) : RawPayload

data class Http2Frame(override val instant: KInstant, val streamId: Int?, val content: String) : RawPayload {
    override val payload: ByteArray
        get() = content.encodeToByteArray()
}

data class CustomSsl(
    val sslContext: SSLContext,
    val keyManager: KeyManager?,
    val trustManager: X509TrustManager?
)

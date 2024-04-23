package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

interface TransportClient {
    fun getCallData(callId: String): CallData?

    fun createCallData(
        callId: String? = null,
        coroutineScope: CoroutineScope,
        requestBodySize: Int?,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        sslConfig: SslConfig,
        fireType: UserResponse.Type,
        loadTestState: LoadTestState? = null,
    ): CallData

    fun sendRequest(
        callId: String,
        coroutineScope: CoroutineScope,
        client: Any? = null,
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        fireType: UserResponse.Type,
        parentLoadTestState: LoadTestState?,
    ): CallData

    fun emitEvent(callId: String, event: String, isForce: Boolean = false)

    fun createReusableNonInspectableClient(
        parentCallId: String,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
    ): Any?
}

class NetworkEvent(val callId: String, val instant: KInstant, val event: String, val callData: CallData, val isEnd: Boolean = false)
class CallData(
    val id: String,
    val subprojectId: String,
    var isPrepared: Boolean = false,
    var status: ConnectionStatus = ConnectionStatus.PREPARING,
    val coroutineScope: CoroutineScope,

    val sslConfig: SslConfig,

    val events: SharedFlow<NetworkEvent>
    val eventsStateFlow: StateFlow<NetworkEvent?>,
    val outgoingBytes: SharedFlow<RawPayload>,
    val incomingBytes: SharedFlow<RawPayload>,
    val optionalResponseSize: AtomicInteger,
    val response: UserResponse,

    // load test specific
    val fireType: UserResponse.Type,
    val loadTestState: LoadTestState? = null,

    var cancel: (Throwable?) -> Unit,
    var sendPayload: (String) -> Unit = {},
    var sendEndOfStream: () -> Unit = {},
) {
    private var isCompleted = MutableStateFlow(false)

    fun complete() {
        isCompleted.value = true
    }

    suspend fun awaitComplete() {
        isCompleted
            .onSubscription { emit(isCompleted.value) }
            .filter { it == true }
            .first()
    }

    fun isCompleted(): Boolean = isCompleted.value
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

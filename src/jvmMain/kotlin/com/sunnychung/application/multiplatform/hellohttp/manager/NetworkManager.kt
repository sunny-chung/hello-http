package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Request
import java.util.concurrent.atomic.AtomicInteger

interface NetworkManager {
    fun getCallData(callId: String): CallData?

    fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        sslConfig: SslConfig
    ): CallData
}

class NetworkEvent(val callId: String, val instant: KInstant, val event: String)
class CallData(
    val id: String,
    val subprojectId: String,
    var isPrepared: Boolean = false,

    val events: SharedFlow<NetworkEvent>,
    val eventsStateFlow: StateFlow<NetworkEvent?>,
    val outgoingBytes: SharedFlow<Pair<KInstant, ByteArray>>,
    val incomingBytes: SharedFlow<Pair<KInstant, ByteArray>>,
    val optionalResponseSize: AtomicInteger,
    val response: UserResponse,
)

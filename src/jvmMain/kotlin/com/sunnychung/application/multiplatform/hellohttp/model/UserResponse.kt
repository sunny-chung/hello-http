package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.util.*

class UserResponse {
    var startAt: KInstant? = null
    var endAt: KInstant? = null
    var isCommunicating: Boolean = false
    var statusCode: Int? = null
    var statusText: String? = null
    var responseSizeInBytes: Long? = null
    var body: ByteArray? = null // TODO support non-string body
    var headers: List<Pair<String, String>>? = null
    var rawExchange: RawExchange = RawExchange(exchanges = Collections.synchronizedList(mutableListOf()))
}

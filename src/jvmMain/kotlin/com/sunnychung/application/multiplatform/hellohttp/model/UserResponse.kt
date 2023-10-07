package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.lib.multiplatform.kdatetime.KInstant

data class UserResponse(
    val startAt: KInstant?,
    val endAt: KInstant?,
    val isCommunicating: Boolean,
    val statusCode: Int?,
    val statusText: String?,
    val responseSizeInBytes: Long?,
    val body: String?, // TODO support non-string body
    val headers: List<Pair<String, String>>?,
    val rawExchange: RawExchange
)

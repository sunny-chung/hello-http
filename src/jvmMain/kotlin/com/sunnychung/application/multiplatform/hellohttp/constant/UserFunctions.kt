package com.sunnychung.application.multiplatform.hellohttp.constant

import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

val UserFunctions = listOf(
    UserFunction("uuid") { uuidString() },
    UserFunction("now.iso8601") { KDateTimeFormat.ISO8601_DATETIME.format(KInstant.now()) },
)
    .associateBy { it.name }

data class UserFunction(
    val name: String,
    val function: () -> String,
)

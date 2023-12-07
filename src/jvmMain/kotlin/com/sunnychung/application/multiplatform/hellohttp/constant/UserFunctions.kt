package com.sunnychung.application.multiplatform.hellohttp.constant

import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

val UserFunctions = listOf(
    UserFunction("uuid") { uuidString() },
    UserFunction("now.iso8601") { KDateTimeFormat.ISO8601_DATETIME.format(KInstant.now()) },
    UserFunction("now.epochMills") { KInstant.now().toEpochMilliseconds().toString() },
    UserFunction("now.epochSeconds") {
        KDuration.of(KInstant.now().toEpochMilliseconds(), KFixedTimeUnit.MilliSecond)
            .toTimeUnitValue(KFixedTimeUnit.Second).toString()
    },
)
    .associateBy { it.name }

data class UserFunction(
    val name: String,
    val function: () -> String,
)

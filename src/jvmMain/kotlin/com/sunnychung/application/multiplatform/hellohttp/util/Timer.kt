package com.sunnychung.application.multiplatform.hellohttp.util

import co.touchlab.kermit.Severity
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

fun <R> time(operation: () -> R): Pair<KDuration, R> {
    val start = KInstant.now()
    val result = operation()
    val end = KInstant.now()
    return (end - start) to result
}

fun <R> timeAndLog(level: Severity, tag: String, operation: () -> R): R {
    if (level < log.config.minSeverity) {
        return operation()
    }
    val (duration, result) = time(operation)
    log.log(level, message = "[$tag] took $duration", throwable = null)
    return result
}

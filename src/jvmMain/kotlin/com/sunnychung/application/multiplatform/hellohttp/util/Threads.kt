package com.sunnychung.application.multiplatform.hellohttp.util

import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds

fun <R> executeWithTimeout(timeout: KDuration, action: () -> R) : R {
    var hasKilled = false
    var executeException: Throwable? = null
    var result: R? = null
    val executeThread = Thread {
        try {
            result = action()
        } catch (e: Throwable) {
            executeException = e
        }
    }
    val killThread = Thread {
        Thread.sleep(timeout.toMilliseconds())
        if (executeThread.isAlive) {
            hasKilled = true
            log.d { "Killing execution thread" }
            try {
                executeThread.interrupt()
                executeThread.stop()
            } catch (_: Throwable) {}
        }
    }
    executeThread.start()
    killThread.start()
    executeThread.join((timeout + 10.seconds()).toMilliseconds())
    killThread.interrupt()
    if (hasKilled) {
        throw RuntimeException("Custom script was running for too long time and has been killed")
    } else if (executeException != null) {
        log.d(executeException!!) { "Got exception during executeWithTimeout" }
        throw executeException!!
    }
    return result!!
}

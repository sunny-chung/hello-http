package com.sunnychung.application.multiplatform.hellohttp.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant

val log = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "Hello")
val llog = log

val logR = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "Hello.Repository")

class JvmLogger : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val str = "[${KDateTimeFormat.FULL.format(KZonedInstant.nowAtLocalZoneOffset())}] ${severity.name.uppercase()} [${Thread.currentThread().name}] ($tag) -- $message"
        if (severity == Severity.Error) {
            System.err.println(str)
        } else {
            println(str)
        }
        throwable?.let {
            val thString = it.stackTraceToString()
            if (severity == Severity.Error) {
                System.err.println(thString)
            } else {
                println(thString)
            }
        }
    }

}

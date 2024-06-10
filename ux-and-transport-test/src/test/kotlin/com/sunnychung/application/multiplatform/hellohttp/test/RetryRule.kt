package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(val maxRetryCount: Int = 5) : TestRule {
    init {
        if (maxRetryCount < 1) {
            throw IllegalArgumentException("maxRetryCount should be at least 1")
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var attempt = -1
                do {
                    ++attempt
                    try {
                        base.evaluate()
                        return
                    } catch (e: Throwable) {
                        if (attempt < maxRetryCount) {
                            with("[${KZonedInstant.nowAtLocalZoneOffset()}] Retrying test ${description.displayName} for #${attempt + 1}") {
                                println(this)
                                System.err.println(this)
                            }
                            continue
                        } else {
                            throw e
                        }
                    }
                } while (true)
            }
        }
    }
}

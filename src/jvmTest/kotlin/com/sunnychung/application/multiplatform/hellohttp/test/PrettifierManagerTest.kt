package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.manager.PrettifierManager
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import kotlin.test.Test

class PrettifierManagerTest {
    val instance = PrettifierManager()

    @Test
    fun hasJsonPrettifier() {
        assert(instance.matchPrettifiers(ProtocolApplication.Http, "application/json").isNotEmpty())
        assert(instance.matchPrettifiers(ProtocolApplication.Http, "application/json; charset=utf-8").isNotEmpty())
    }
}

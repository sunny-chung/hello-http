package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest

import kotlinx.coroutines.delay
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("rest")
@RestController
class SpecialApi {

    @RequestMapping("earlyError")
    fun earlyError() {
        throw RuntimeException("Early error")
    }

    @RequestMapping("wait")
    suspend fun wait(@RequestParam ms: Long): String {
        delay(ms)
        return "OK"
    }
}

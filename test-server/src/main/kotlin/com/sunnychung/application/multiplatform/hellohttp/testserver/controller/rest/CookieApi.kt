package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RequestMapping("rest/cookie")
@RestController
class CookieApi {

    @GetMapping
    fun getCookies(request: ServerHttpRequest, exchange: ServerWebExchange) =
        request.cookies.toList()

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setCookie(@RequestBody cookies: Map<String, String>, exchange: ServerWebExchange) {
        exchange.response.addCookie(ResponseCookie.from("after1h", "after1h").maxAge(1 * 60 * 60).build())
        exchange.response.addCookie(ResponseCookie.from("after1d", "after1d").maxAge(1 * 24 * 60 * 60).secure(true).build())
        exchange.response.addCookie(ResponseCookie.from("after3d", "after3d").maxAge(3 * 24 * 60 * 60).httpOnly(true).secure(true).build())
        cookies.forEach {
            exchange.response.addCookie(ResponseCookie.from(it.key, it.value).build())
        }
    }
}

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
        cookies.forEach {
            exchange.response.addCookie(ResponseCookie.from(it.key, it.value).build())
        }
    }
}

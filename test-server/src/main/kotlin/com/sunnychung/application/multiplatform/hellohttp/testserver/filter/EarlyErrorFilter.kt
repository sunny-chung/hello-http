package com.sunnychung.application.multiplatform.hellohttp.testserver.filter

import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@Order(-101)
class EarlyErrorFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange.request.path.value().startsWith("/rest/earlyError")) {
            exchange.response.statusCode = HttpStatusCode.valueOf(429)
            return Mono.empty()
        } else if (exchange.request.path.value().startsWith("/rest/error")) {
            val statusCode = exchange.request.queryParams.getFirst("code")!!.toInt()
            exchange.response.statusCode = HttpStatusCode.valueOf(statusCode)
            exchange.response.headers["Content-Type"] = "application/json"
            val responseBody = "{\"error\":\"Some message\"}"
            return exchange.response.writeWith(
                Flux.just(
                    exchange.response.bufferFactory().wrap(responseBody.encodeToByteArray())
                )
            )
        } else {
            return chain.filter(exchange)
        }
    }
}

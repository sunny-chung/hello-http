package com.sunnychung.application.multiplatform.hellohttp.testserver.filter

import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(-101)
class EarlyErrorFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange.request.path.value().startsWith("/rest/earlyError")) {
            exchange.response.statusCode = HttpStatusCode.valueOf(429)
            return Mono.empty()
        } else {
            return chain.filter(exchange)
        }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.testserver.config

import com.sunnychung.application.multiplatform.hellohttp.testserver.controller.websocket.EntryWebSocketHandler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
class WebSocketConfig {
    @Autowired
    lateinit var entryWebSocketHandler: EntryWebSocketHandler

    @Bean
    fun handlerMapping(): HandlerMapping {
        return SimpleUrlHandlerMapping(
            /* urlMap = */ mapOf(
                "/ws" to entryWebSocketHandler
            ),
            /* order = */ -1
        )
    }

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter(object : HandshakeWebSocketService() {
        init {
            setSessionAttributePredicate { true }
        }

        override fun handleRequest(exchange: ServerWebExchange, handler: WebSocketHandler): Mono<Void> {
            return exchange.session.asFlow()
                .map { session ->
                    session.attributes["request"] = exchange.request
                }
                .asFlux()
                .then(super.handleRequest(exchange, handler))
        }
    })
}

package com.sunnychung.application.multiplatform.hellohttp.testserver.config

import com.sunnychung.application.multiplatform.hellohttp.testserver.controller.websocket.EntryWebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

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
    fun handlerAdapter() = WebSocketHandlerAdapter()
}

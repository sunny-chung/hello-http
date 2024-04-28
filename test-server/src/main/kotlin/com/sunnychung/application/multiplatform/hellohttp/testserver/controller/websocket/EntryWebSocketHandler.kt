package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.websocket

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Controller
class EntryWebSocketHandler : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.receive()
            .asFlow()
            .map {
                val req = it.payloadAsText
                // use `awaitFirstOrNull` instead of `awaitFirst` or `awaitSingle` so that the session won't close immediately after sending message
                session.send(Mono.just(session.textMessage("Hello $req"))).awaitFirstOrNull()
                Unit
            }.asFlux()
            .then()
    }
}

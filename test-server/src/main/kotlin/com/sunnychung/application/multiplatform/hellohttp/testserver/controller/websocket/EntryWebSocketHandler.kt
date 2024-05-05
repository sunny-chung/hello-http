package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest.toParameterList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URLDecoder

@Controller
class EntryWebSocketHandler : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.receive()
            .asFlow()
            .map {
                val req = it.payloadAsText
                val resp = when (req) {
                    "!echo" -> {
                        val requestData = RequestData(
                            path = session.handshakeInfo.uri.path,
                            method = (session.attributes["request"] as ServerHttpRequest).method.name(),
                            headers = session.handshakeInfo.headers.toParameterList(),
                            queryParameters = UriComponentsBuilder
                                .fromUri(session.handshakeInfo.uri)
                                .build()
                                .queryParams
                                .mapValues {
                                    it.value.map {
                                        URLDecoder.decode(it, Charsets.UTF_8)
                                    }
                                }
                                .let { MultiValueMapAdapter(it) }
                                .toParameterList(),
                            formData = emptyList(),
                            multiparts = emptyList(),
                            body = null,
                        )
                        jacksonObjectMapper().writeValueAsString(requestData)
                    }
                    else -> "Hello $req"
                }
                // use `awaitFirstOrNull` instead of `awaitFirst` or `awaitSingle` so that the session won't close immediately after sending message
                session.send(Mono.just(session.textMessage(resp))).awaitFirstOrNull()
                Unit
            }.asFlux()
            .then()
    }
}

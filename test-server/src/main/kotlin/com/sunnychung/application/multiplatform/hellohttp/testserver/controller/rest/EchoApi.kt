package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest

import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.PartData
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.awaitFormData
import org.springframework.web.server.awaitMultipartData
import reactor.core.publisher.Flux
import reactor.kotlin.extra.math.sumAsInt
import java.util.concurrent.atomic.AtomicInteger

@RequestMapping("rest")
@RestController
class EchoApi {
    val log = LoggerFactory.getLogger(this::class.java)

    @RequestMapping("echo")
    suspend fun echo(request: ServerHttpRequest, exchange: ServerWebExchange): RequestData {
        return RequestData(
            path = request.path.value(),
            method = request.method.name(),
            headers = request.headers.toParameterList(),
            queryParameters = request.queryParams.toParameterList(),
            formData = exchange.awaitFormData().toParameterList(),
            multiparts = exchange.awaitMultipartData().flatMap { it.value.map {
                val data = it.content().toByteArray()
                PartData(
                    name = it.name(),
                    headers = it.headers().toParameterList(),
                    size = data?.size ?: 0,
                    data = data?.decodeToString(),
                )
            } },
            body = try {
                request.body.toByteArray()?.decodeToString()
            } catch (_: IllegalStateException) {
                null
            },
        )
    }

    @RequestMapping("echoWithoutBody")
    suspend fun echoWithoutBody(request: ServerHttpRequest, exchange: ServerWebExchange): RequestData {
        log.debug("echoWithoutBody start")
        val i = AtomicInteger(0)
        return RequestData(
            path = request.path.value(),
            method = request.method.name(),
            headers = request.headers.toParameterList(),
            queryParameters = request.queryParams.toParameterList(),
            formData = exchange.awaitFormData().toParameterList(),
            multiparts = exchange.awaitMultipartData().flatMap {
                log.debug("echoWithoutBody flatMap ${i.incrementAndGet()}")
                val j = AtomicInteger(0)
                it.value.map {
                    log.debug("echoWithoutBody value.map ${j.incrementAndGet()}")
                    val k = AtomicInteger(0)
//                val dataSize = DataBufferUtils.join(it.content()).awaitSingleOrNull()?.readableByteCount() ?: 0
                    val dataSize = it.content().map {
                        it.readableByteCount().also {
                            log.debug("echoWithoutBody content.map ${k.incrementAndGet()} -> $it")
                        }
                    }.sumAsInt().awaitSingleOrNull() ?: 0
                    log.debug("echoWithoutBody PartData ${j.get() - 1}")
                    PartData(
                        name = it.name(),
                        headers = it.headers().toParameterList(),
                        size = dataSize,
                        data = null,
                    )
                }
            },
            body = try {
                log.debug("echoWithoutBody body start")
                request.body.toByteArray()?.size?.toString().also {
                    log.debug("echoWithoutBody body returning $it")
                }
            } catch (_: IllegalStateException) {
                log.debug("echoWithoutBody body caught ISE")
                null
            },
        ).also {
            log.debug("echoWithoutBody return")
        }
    }
}

fun MultiValueMap<String, String>.toParameterList(): List<Parameter> =
    flatMap { q -> q.value.map { Parameter(name = q.key, value = it) } }

suspend fun Flux<DataBuffer>.toByteArray(): ByteArray? =
    DataBufferUtils.join(this).map { buffer ->
        val result = ByteArray(buffer.readableByteCount())
        buffer.read(result)
        DataBufferUtils.release(buffer)
        result
    }.awaitSingleOrNull()

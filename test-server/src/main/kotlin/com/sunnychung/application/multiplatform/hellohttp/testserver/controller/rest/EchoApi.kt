package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest

import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.PartData
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.flux
import org.slf4j.LoggerFactory
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePartEvent
import org.springframework.http.codec.multipart.FormPartEvent
import org.springframework.http.codec.multipart.PartEventHttpMessageReader
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.awaitFormData
import org.springframework.web.server.awaitMultipartData
import reactor.core.publisher.Flux
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

        val formData = mutableListOf<Parameter>()
        val multipartData = mutableListOf<PartData>()

        val contentType = request.headers.contentType
        if (contentType?.getType()?.equals("multipart", ignoreCase = true) == true) {
            val partsReader = PartEventHttpMessageReader()
            val allPartEvents = partsReader.read(ResolvableType.NONE, request, emptyMap())

            allPartEvents.windowUntil { it.isLast }
                .concatMap {
                    it.switchOnFirst { signal, partEvents ->
                        if (signal.hasValue()) {
                            val event = signal.get()
                            log.debug("echoWithoutBody ${i.incrementAndGet()} ${event?.name()} ${event?.javaClass?.simpleName}")
                            when (event) {
                                is FormPartEvent -> {
//                                formData += Parameter(name = event.name(), value = event.value())
                                    multipartData += PartData(
                                        name = event.name(),
                                        headers = event.headers().toParameterList(),
                                        size = event.content().let {
                                            it.readableByteCount().also { _ ->
                                                DataBufferUtils.release(it)
                                            }
                                        },
                                        data = null,
                                    )
                                    Flux.just(Unit)
                                }
                                is FilePartEvent -> {
                                    flux<Unit> {
                                        val j = AtomicInteger(0)
                                        val dataSize = partEvents
                                            .map {
                                                log.debug("echoWithoutBody $i ${j.incrementAndGet()} start read")
                                                it.content()
                                            }
                                            .map {
                                                it.readableByteCount().also { count ->
                                                    DataBufferUtils.release(it)
                                                    log.debug("echoWithoutBody $i ${j.incrementAndGet()} read $count")
                                                }
                                            }
                                            .asFlow()
                                            .toList()
                                            .sum()
                                        multipartData += PartData(
                                            name = event.name(),
                                            headers = event.headers().toParameterList(),
                                            size = dataSize,
                                            data = null,
                                        )
                                        log.debug("echoWithoutBody $i end")
                                    }
                                }
                                else -> throw UnsupportedOperationException()
                            }
                        } else {
                            partEvents
                        }
                    }
                }
                .asFlow()
                .lastOrNull()
        }

        return RequestData(
            path = request.path.value(),
            method = request.method.name(),
            headers = request.headers.toParameterList(),
            queryParameters = request.queryParams.toParameterList(),
//            formData = formData,
            multiparts = multipartData,
            formData = exchange.awaitFormData().toParameterList(),
//            multiparts = exchange.multipartData.asFlow().map {
//                log.debug("echoWithoutBody flatMap ${i.incrementAndGet()}")
//                val j = AtomicInteger(0)
//                it.values.map {
//                    log.debug("echoWithoutBody value.map ${j.incrementAndGet()}")
//                    val k = AtomicInteger(0)
////                val dataSize = DataBufferUtils.join(it.content()).awaitSingleOrNull()?.readableByteCount() ?: 0
//                    val dataSize = it.flatMap { it.content().asFlow().toList() }.map {
//                        it.readableByteCount().also {
//                            log.debug("echoWithoutBody content.map ${k.incrementAndGet()} -> $it")
//                        }
//                    }.sum()
//                    log.debug("echoWithoutBody PartData ${j.get() - 1}")
//                    PartData(
//                        name = it.first().name(),
//                        headers = it.first().headers().toParameterList(),
//                        size = dataSize,
//                        data = null,
//                    )
//                }
//            }.toList().flatten(),
            body = try {
                log.debug("echoWithoutBody body start")
                request.body.toSize()?.toString().also {
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

suspend fun Flux<DataBuffer>.toSize(): Int? =
    this.map { buffer ->
        val result = buffer.readableByteCount()
        DataBufferUtils.release(buffer)
        result
    }.asFlow()
        .toList()
        .sum()

package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.annotation.Reflection
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.MAX_CAPTURED_REQUEST_BODY_SIZE
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.http.client.reactive.ClientHttpRequestDecorator
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClientRequest
import reactor.netty.http.client.HttpClientResponse
import java.io.File
import java.io.FileInputStream

@Reflection private val DefaultClientResponseClass = Class.forName("org.springframework.web.reactive.function.client.DefaultClientResponse")
@Reflection private val DefaultClientResponseFieldResponse = DefaultClientResponseClass.getDeclaredField("response").also { it.isAccessible = true }
@Reflection private val ReactorClientHttpResponseClass = Class.forName("org.springframework.http.client.reactive.ReactorClientHttpResponse")
@Reflection private val ReactorClientHttpResponseFieldResponse = ReactorClientHttpResponseClass.getDeclaredField("response").also { it.isAccessible = true }

class SpringWebClientTransportClient(networkClientManager: NetworkClientManager) : ReactorNettyHttpTransportClient(networkClientManager) {

    fun buildWebClient(
        callId: String,
        callData: CallData,
        isSsl: Boolean,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        outgoingBytesFlow: MutableSharedFlow<RawPayload>,
        incomingBytesFlow: MutableSharedFlow<RawPayload>,
        http2AccumulatedOutboundDataSerializeLimit: Int,
        http2AccumulatedInboundDataSerializeLimit: Int,
        doOnRequestSent: (HttpClientRequest) -> Unit,
    ) : WebClient {
        log.d { "spring building client" }
        return WebClient.builder()
            .clientConnector(
                buildHttpClient(
                    callId = callId,
                    callData = callData,
                    isSsl = isSsl,
                    httpConfig = httpConfig,
                    sslConfig = sslConfig,
                    outgoingBytesFlow = outgoingBytesFlow,
                    incomingBytesFlow = incomingBytesFlow,
                    http2AccumulatedOutboundDataSerializeLimit = http2AccumulatedOutboundDataSerializeLimit,
                    http2AccumulatedInboundDataSerializeLimit = http2AccumulatedInboundDataSerializeLimit,
                    onRequestSent = doOnRequestSent,
                )
                .also { log.d { "spring building connector" } }
                .let { ReactorClientHttpConnector(it) }
                .also { log.d { "spring init connector done" } }
            )
            .codecs { conf ->
                conf.defaultCodecs()
                    .maxInMemorySize(22 * 1024 * 1024) // 22 MB
                    .also { log.d { "spring init codecs done" } }
            }
            .build()
    }

    override fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        subprojectConfig: SubprojectConfiguration
    ): CallData {
        log.d { "sendRequest start" }
        val uri = request.getResolvedUri()
        val data = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            subprojectConfig = subprojectConfig,
        )
        val callId = data.id

        val sentRequestHeaders: MutableList<Pair<String, String>> = mutableListOf()

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler()) {
            log.d { "net start coroutine" }
            data.cancel = {
                log.i { "Cancel call #$callId" }
                this.cancel("Cancel", it)
                data.status = ConnectionStatus.DISCONNECTED
                emitEvent(callId, "Cancelled")
            }

            val client = buildWebClient(
                callId = callId,
                callData = data,
                isSsl = uri.scheme !in setOf("http", "ws"),
                httpConfig = httpConfig,
                sslConfig = sslConfig,
                outgoingBytesFlow = data.outgoingBytes as MutableSharedFlow<RawPayload>,
                incomingBytesFlow = data.incomingBytes as MutableSharedFlow<RawPayload>,
                http2AccumulatedOutboundDataSerializeLimit = (subprojectConfig.accumulatedOutboundDataStorageLimitPerCall.takeIf { it >= 0 }
                    ?: DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT).toInt(),
                http2AccumulatedInboundDataSerializeLimit = (subprojectConfig.accumulatedInboundDataStorageLimitPerCall.takeIf { it >= 0 }
                    ?: DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT).toInt(),
                doOnRequestSent = { req ->
                    log.d { "Req header = ${req.requestHeaders()}" }
                    sentRequestHeaders += req.requestHeaders().map { e ->
                        e.toPair()
                    }
                },
            )
            log.d { "prepared client" }

            data.waitForPreparation()
            log.d { "Call $callId is prepared" }

            val out = data.response
            out.requestData = RequestData(
                method = request.method,
                url = request.url,
                headers = sentRequestHeaders,
            )

            data.cancel = {
                log.i { "Cancel call #$callId" }
                this.cancel("Cancel", it)
                data.status = ConnectionStatus.DISCONNECTED
                data.consumePayloads(isComplete = true)
                data.end()
            }

            try {
                val requestBuilder = client
                    .method(org.springframework.http.HttpMethod.valueOf(request.method.uppercase()))
                    .uri(uri)
                    .headers { builder ->
                        log.v { "Building header" }
                        request.headers.forEach { (h, v) ->
                            builder.add(h, v)
                        }
                    }
                    .run {
                        val bodyInserter = when (val body = request.body) {
                            is StringBody -> BodyInserters.fromValue(body.value)

                            is FileBody -> BodyInserters.fromResource(InputStreamResource(FileInputStream(File(body.filePath!!))))

                            is FormUrlEncodedBody -> BodyInserters.fromFormData(
                                body.value.groupBy({ it.key }, { it.value })
                                    .let { LinkedMultiValueMap(it) }
                            )

                            is MultipartBody -> {
                                BodyInserters.fromMultipartData(
                                    MultipartBodyBuilder()
                                        .apply {
                                            (body as RequestBodyWithKeyValuePairs).value.forEach {
                                                when (it.valueType) {
                                                    FieldValueType.String -> part(it.key, it.value)
                                                    FieldValueType.File -> part(it.key, FileSystemResource(File(it.value)))
                                                }
                                            }
                                        }
                                        .build()
                                )
                            }

                            is GraphqlBody -> throw UnsupportedOperationException()
                            null -> null
                        }
                        bodyInserter
                            ?.let { inserter ->
                                inserter as BodyInserter<*, in ClientHttpRequest>
                                body { outputMessage, context ->
                                    val interceptor: ClientHttpRequestDecorator = object : ClientHttpRequestDecorator(outputMessage) {
                                        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                                            return Flux.from(body)
                                                .doOnNext { buffer ->
                                                    out.requestData?.appendBody(buffer)
                                                }
                                                .let {
                                                    super.writeWith(it)
                                                }
                                        }
                                    }
                                    inserter.insert(interceptor, context)
                                }
                            }
                            ?: this
                    }

                out.startAt = KInstant.now()
                data.status = ConnectionStatus.CONNECTING

                log.i { "Request started at ${out.startAt!!.atLocalZoneOffset()}" }

                emitEvent(data.id, "") // update the UI

                requestBuilder
                    .awaitExchangeOrNull { resp ->
                        val response: HttpClientResponse = DefaultClientResponseFieldResponse.get(resp)
                            .let { ReactorClientHttpResponseFieldResponse.get(it) as HttpClientResponse }
                        out.statusCode = response.status().code()
                        out.statusText = response.status().reasonPhrase()
                        out.headers = resp.headers().asHttpHeaders().flatMap { (key, values) ->
                            values.map { key to it }
                        }
                        val body = resp.awaitBodyOrNull(ByteArrayResource::class)?.byteArray
                        out.responseSizeInBytes = body?.size?.toLong() ?: 0L
                        out.body = body
                    }
            } catch (e: Throwable) {
                log.i(e) { "Encountered error during HTTP call via WebClient" }
                out.errorMessage = e.message
                out.isError = true
            } finally {
                out.endAt = KInstant.now()
                data.status = ConnectionStatus.DISCONNECTED
                data.consumePayloads(isComplete = true)
                data.end()
            }

            if (!out.isError && postFlightAction != null) {
                executePostFlightAction(callId, out, postFlightAction)
            }
            emitEvent(callId, "Response completed")
        }
        log.d { "sendRequest return" }
        return data
    }
}

fun RequestData.appendBody(buffer: DataBuffer) {
    val bodyBuilder = bodyPayloadBuilder ?: return
    val numBytesStoreCapacity = MAX_CAPTURED_REQUEST_BODY_SIZE - bodyBuilder.size()
    val bufferSize = buffer.readableByteCount()
    if (bufferSize <= 0) return
    bodySizeBuilder.addAndGet(bufferSize.toLong())
    if (numBytesStoreCapacity <= 0) {
        flush(isRemoveBuilder = true)
        return
    }
    val numBytesToTake = minOf(numBytesStoreCapacity, bufferSize)
    val tempBytesArray = ByteArray(numBytesToTake)
    val originalReadPos = buffer.readPosition()
    buffer.read(tempBytesArray, 0, numBytesToTake)
    buffer.readPosition(originalReadPos)
    bodyBuilder.writeBytes(tempBytesArray)
}

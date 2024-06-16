package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.logging.LogLevel
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContextBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.HttpProtocol
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Path

private const val REQUEST_CHUNK_SIZE = 8192

open class ReactorNettyHttpTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {

    protected fun buildHttpClient(
        callId: String,
        callData: CallData,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        outgoingBytesFlow: MutableSharedFlow<RawPayload>,
        incomingBytesFlow: MutableSharedFlow<RawPayload>,
        http2AccumulatedOutboundDataSerializeLimit: Int,
        http2AccumulatedInboundDataSerializeLimit: Int,
    ) : HttpClient {
        val sslContext = createSslContext(sslConfig)

        return HttpClient.newConnection()
//            .wiretap("NettyIO", LogLevel.ERROR, AdvancedByteBufFormat.SIMPLE, Charsets.UTF_8) // FIXME remove
            .protocol(
                *when (httpConfig.protocolVersion) {
                    HttpConfig.HttpProtocolVersion.Http1Only -> arrayOf(HttpProtocol.HTTP11)
                    HttpConfig.HttpProtocolVersion.Http2Only -> arrayOf(HttpProtocol.H2, HttpProtocol.H2C)
                    null, HttpConfig.HttpProtocolVersion.Negotiate -> arrayOf(HttpProtocol.H2, HttpProtocol.H2C, HttpProtocol.HTTP11)
                }
            )
            .secure { spec ->
                spec.sslContext(
                    SslContextBuilder.forClient()
                        .applicationProtocolConfig(ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT,
                            *when (httpConfig.protocolVersion) {
                                HttpConfig.HttpProtocolVersion.Http1Only -> arrayOf(ApplicationProtocolNames.HTTP_1_1)
                                HttpConfig.HttpProtocolVersion.Http2Only -> arrayOf(ApplicationProtocolNames.HTTP_2)
                                null, HttpConfig.HttpProtocolVersion.Negotiate -> arrayOf(ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1)
                            }
                        ))
                        .run {
                            sslContext.keyManager?.let {
                                keyManager(it)
                            } ?: this
                        }
                        .run {
                            sslContext.trustManager?.let {
                                trustManager(it)
                            } ?: this
                        }
                        .build()
                )
            }
            .doOnResolve {
                emitEvent(callId, "DNS resolution of domain [] started") // TODO add domain name
            }
            .doAfterResolve { conn, addr ->
                emitEvent(callId, "DNS resolved to ${(addr as InetSocketAddress).address}")
            }
//            .resolver(DnsNameResolverBuilder().dnsQueryLifecycleObserverFactory {
//                emitEvent(callId, "DNS resolution of domain [${it.name()}] started")
//                object : DnsQueryLifecycleObserver {
//                    override fun queryWritten(p0: InetSocketAddress?, p1: ChannelFuture?) {
//
//                    }
//
//                    override fun queryCancelled(p0: Int) {
//
//                    }
//
//                    override fun queryRedirected(p0: MutableList<InetSocketAddress>?): DnsQueryLifecycleObserver {
//
//                    }
//
//                    override fun queryCNAMEd(p0: DnsQuestion?): DnsQueryLifecycleObserver {
//
//                    }
//
//                    override fun queryNoAnswer(p0: DnsResponseCode?): DnsQueryLifecycleObserver {
//
//                    }
//
//                    override fun queryFailed(p0: Throwable?) {
//
//                    }
//
//                    override fun querySucceed() {
//                        emitEvent(callId, "DNS resolved to ${it.}")
//                    }
//
//                }
//            })
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
        val data = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            subprojectConfig = subprojectConfig,
        )
        val callId = data.id

        val httpClient = buildHttpClient(
            callId = callId,
            callData = data,
            httpConfig = httpConfig,
            sslConfig = sslConfig,
            outgoingBytesFlow = data.outgoingBytes as MutableSharedFlow<RawPayload>,
            incomingBytesFlow = data.incomingBytes as MutableSharedFlow<RawPayload>,
            http2AccumulatedOutboundDataSerializeLimit = (subprojectConfig.accumulatedOutboundDataStorageLimitPerCall.takeIf { it >= 0 }
                ?: DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT).toInt(),
            http2AccumulatedInboundDataSerializeLimit = (subprojectConfig.accumulatedInboundDataStorageLimitPerCall.takeIf { it >= 0 }
                ?: DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT).toInt(),
        )

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler()) {
            httpClient.warmup().awaitSingleOrNull()

            data.waitForPreparation()
            log.d { "Call $callId is prepared" }

            val out = data.response
            out.requestData = RequestData() // TODO

            data.cancel = {
                this.cancel("Cancel", it)
                data.status = ConnectionStatus.DISCONNECTED
                data.consumePayloads(isComplete = true)
                data.end()
            }

            // TODO push promise
            // TODO gzip, x-gzip, deflate

            try {
                val requestBuilder = httpClient
                    .headers { builder ->
                        request.headers.forEach { (h, v) ->
                            builder.add(h, v)
                        }
                    }
                    .request(HttpMethod(request.method))
                    .uri(request.getResolvedUri())
                    .run {
                        when (val body = request.body) {
                            is StringBody -> send(
                                ByteBufFlux.fromString(
                                    Mono.just(body.value),
                                    Charsets.UTF_8,
                                    ByteBufAllocator.DEFAULT
                                )
                            )

                            is FileBody -> send(
                                ByteBufFlux.fromPath(Path.of(body.filePath!!), REQUEST_CHUNK_SIZE)
                            )

                            is FormUrlEncodedBody, is MultipartBody -> {
                                sendForm { req, form ->
                                    form.multipart(body is MultipartBody)
                                        .run {
                                            var b = this
                                            (body as RequestBodyWithKeyValuePairs).value.forEach {
                                                b = when (it.valueType) {
                                                    FieldValueType.String -> b.attr(it.key, it.value)
                                                    FieldValueType.File -> b.file(it.key, File(it.value))
                                                }
                                            }
                                            b
                                        }
                                }
                            }

                            is GraphqlBody -> throw UnsupportedOperationException()
                            null -> this
                        }
                    }
                    .responseSingle { response, bbuf ->
                        out.statusCode = response.status().code()
                        out.statusText = response.status().reasonPhrase()
                        out.headers = response.responseHeaders().map {
                            it.toPair()
                        }
                        bbuf.asByteArray()
                    }

                out.startAt = KInstant.now()
                data.status = ConnectionStatus.CONNECTING

                log.i { "Request started at ${out.startAt!!.atLocalZoneOffset()}" }

                requestBuilder
                    .awaitSingleOrNull()
                    .let {
                        out.responseSizeInBytes = it?.size?.toLong() ?: 0L
                        out.body = it
                    }
            } catch (e: Throwable) {
                log.i(e) { "Encountered error during HTTP call via Netty" }
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
        return data
    }
}

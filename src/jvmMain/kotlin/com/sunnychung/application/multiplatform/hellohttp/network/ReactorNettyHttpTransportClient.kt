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
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufHolder
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http2.Http2FrameCodec
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.NettyPipeline
import reactor.netty.http.HttpInfos
import reactor.netty.http.HttpProtocol
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.file.Path
import java.util.Deque
import java.util.LinkedList

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
        fun emitRawPayload(direction: RawExchange.Direction, channel: Channel?, payload: Any) {
            val receiveTime = KInstant.now()
            val streamId = (channel as? Http2StreamChannel)?.stream()?.id()
            val payloadHolder = if (streamId == null) {
                Http1Payload(receiveTime, readPayload(payload))
            } else {
                return // skip HTTP/2 frames. They are logged in Http2FramePeeker instead.
            }
            return runBlocking {
                when (direction) {
                    RawExchange.Direction.Outgoing -> outgoingBytesFlow.emit(payloadHolder)
                    RawExchange.Direction.Incoming -> incomingBytesFlow.emit(payloadHolder)
                    RawExchange.Direction.Unspecified -> throw IllegalArgumentException("direction `$direction` is invalid")
                }
            }
        }

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
            .doOnResolve { conn, addr ->
                emitEvent(callId, "DNS resolution of domain [${(addr as InetSocketAddress).hostName}] started") // TODO add domain name
            }
            .doAfterResolve { conn, addr ->
                emitEvent(callId, "DNS resolved to [${(addr as InetSocketAddress).address}]")
            }
            .doOnResolveError { conn, e ->
                emitEvent(callId, "DNS resolve error: ${e.message}")
            }
            .doOnConnected { conn ->
                val eventDescription = buildString {
                    // this logic is based on the implementation of static methods of the class `reactor.netty.http.client.HttpClientConfig`
                    val httpCodec = conn.channel().pipeline().get(NettyPipeline.HttpCodec)
                    val httpProtocolVersion = if (httpCodec is Http2FrameCodec) {
                        "HTTP/2"
                    } else { // null or HttpFrameCodec
                        if (conn is HttpInfos) {
                            conn.version().text()
                        } else {
                            "HTTP/1.1"
                        }
                    }
                    log.i { "onConnected protocol $httpProtocolVersion" }

                    append("Established [$httpProtocolVersion] connection")

                    val sslHandler: SslHandler? = conn.channel().pipeline().get(SslHandler::class.java)
                    val sslSession = sslHandler?.engine()?.session
                    if (sslSession != null) {
                        log.i { "onConnected ssl protocol [${sslSession.protocol}] cipher [${sslSession.cipherSuite}] app protocol [${sslHandler.applicationProtocol()}]" }
                        log.i { "onConnected ssl principal client [${sslSession.localPrincipal}], peer [${sslSession.peerPrincipal}]" }

                        append(" with SSL protocol [${sslSession.protocol}], cipher suite [${sslSession.cipherSuite}]")
                        sslHandler.applicationProtocol()?.let {
                            append(" and application protocol [$it]")
                        }

                        append(".\n\n")
                        append("Client principal = [${sslSession.localPrincipal}]\n")
                        append("\n")
                        append("Server principal = [${sslSession.peerPrincipal}]\n")
                    } else {
                        append(" without encryption.")
                    }
                }
                emitEvent(callId, eventDescription)
            }
            .loggingHandler(object : ChannelDuplexHandler() {
                val writeQueue: Deque<ByteArray> = LinkedList<ByteArray>()
                var currentTotalWrittenBytes = 0
                val writeLock = Any()

                override fun channelActive(ctx: ChannelHandlerContext) {
                    val httpCodec = ctx.pipeline().get(NettyPipeline.HttpCodec)
                    val httpProtocolVersion = if (httpCodec is Http2FrameCodec) {
                        "HTTP/2"
                    } else { // null or HttpFrameCodec
                        "HTTP/1.1"
                    }

                    val cid = ctx.channel().id()
                    val remoteAddress = ctx.channel().remoteAddress() as InetSocketAddress

                    log.i { "c active -- $httpProtocolVersion -- ${cid::class.qualifiedName} -- $cid" }

                    emitEvent(callId, "Connected to [${remoteAddress.address.hostAddress}:${remoteAddress.port}] with [$httpProtocolVersion]")

                    super.channelActive(ctx)
                }

                override fun channelInactive(ctx: ChannelHandlerContext) {
                    super.channelInactive(ctx)
                }

                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    emitRawPayload(direction = RawExchange.Direction.Incoming, channel = ctx.channel(), payload = msg)
                    super.channelRead(ctx, msg)
                }

                override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
                    super.userEventTriggered(ctx, evt)
                }

                override fun connect(
                    ctx: ChannelHandlerContext?,
                    remoteAddress: SocketAddress?,
                    localAddress: SocketAddress?,
                    promise: ChannelPromise?
                ) {
                    super.connect(ctx, remoteAddress, localAddress, promise)
                }

                override fun disconnect(ctx: ChannelHandlerContext?, promise: ChannelPromise?) {
                    emitEvent(callId, "Disconnected")
                    super.disconnect(ctx, promise)
                }

                override fun close(ctx: ChannelHandlerContext?, promise: ChannelPromise?) {
                    emitEvent(callId, "Terminating")
                    super.close(ctx, promise)
                }

                override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
                    synchronized(writeLock) {
                        readPayload(msg).let {
                            if (it.isNotEmpty()) {
                                writeQueue.addLast(it)
                                currentTotalWrittenBytes += it.size
                            }
                        }
                    }
                    super.write(ctx, msg, promise)
                }

                override fun flush(ctx: ChannelHandlerContext?) {
                    if (synchronized(writeLock) { writeQueue.isNotEmpty() }) {
                        // does not guarantee the size of ByteArrayOutputStream is not changing
                        val baos = ByteArrayOutputStream(currentTotalWrittenBytes)
                        while (true) {
                            val e = synchronized(writeLock) {
                                writeQueue.pollFirst()?.also {
                                    currentTotalWrittenBytes -= it.size
                                }
                            }
                            if (e == null) {
                                break
                            }
                            baos.write(e)
                        }
                        emitRawPayload(RawExchange.Direction.Outgoing, ctx?.channel(), baos.toByteArray())
                    }
                    super.flush(ctx)
                }
            })
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
            .doOnResponse { resp, conn ->
                log.i { "NettyIO onResponse" }
            }
    }

    protected fun readPayload(payload: Any?): ByteArray {
        return when (payload) {
            is ByteBuf -> ByteArray(payload.readableBytes()).apply {
                // read bytes without changing readerIndex
                val readerIndex = payload.readerIndex()
                payload.getBytes(readerIndex, this)
            }

            is ByteBufHolder -> readPayload(payload.content())

            is ByteArray -> payload

            null -> byteArrayOf()

            else -> "$payload".toByteArray()
        }
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
                    .awaitFirstOrNull()
//                    .awaitSingleOrNull()
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

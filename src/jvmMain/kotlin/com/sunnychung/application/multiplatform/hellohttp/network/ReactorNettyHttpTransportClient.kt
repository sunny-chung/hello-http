package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolVersion
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.netty.DelegatedTerminalChannelInboundHandler
import com.sunnychung.application.multiplatform.hellohttp.network.netty.Http2FramePeeker
import com.sunnychung.application.multiplatform.hellohttp.network.util.CallDataUserResponseUtil
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufHolder
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent
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
import okhttp3.internal.http2.Http2.CONNECTION_PREFACE
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
private val HTTP2_CONNECTION_PREFACE_BYTES = CONNECTION_PREFACE.toByteArray()

open class ReactorNettyHttpTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {

    companion object {
        var IS_ENABLE_WIRETAP_LOG = false
    }

    protected fun buildHttpClient(
        callId: String,
        callData: CallData?,
        isSsl: Boolean,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        isKeepAlive: Boolean,
        isLogTransport: Boolean,
        outgoingBytesFlow: MutableSharedFlow<RawPayload>,
        incomingBytesFlow: MutableSharedFlow<RawPayload>,
        http2AccumulatedOutboundDataSerializeLimit: Int,
        http2AccumulatedInboundDataSerializeLimit: Int,
    ) : HttpClient {
//        System.setProperty(ReactorNetty.SSL_CLIENT_DEBUG, "true")

        val sslContext = createSslContext(sslConfig)
        val httpClientId = uuidString()

        var isHttp2Established = false
        var isHttp2ClientConnectionPrefaceSent = false
        val writeQueue: Deque<ByteArray> = LinkedList<ByteArray>()
        var currentTotalWrittenBytes = 0
        val writeLock = Any()

        return HttpClient.newConnection()
//            .wiretap("NettyIO", LogLevel.ERROR, AdvancedByteBufFormat.SIMPLE, Charsets.UTF_8) // FIXME remove
            .protocol(
                *when (httpConfig.protocolVersion) {
                    HttpConfig.HttpProtocolVersion.Http1Only -> arrayOf(HttpProtocol.HTTP11)
                    HttpConfig.HttpProtocolVersion.Http2Only -> arrayOf(HttpProtocol.H2, HttpProtocol.H2C)
                    HttpConfig.HttpProtocolVersion.Negotiate -> arrayOf(HttpProtocol.H2, HttpProtocol.H2C, HttpProtocol.HTTP11)
                    null -> if (isSsl) {
                        arrayOf(HttpProtocol.H2, HttpProtocol.H2C, HttpProtocol.HTTP11)
                    } else {
                        arrayOf(HttpProtocol.HTTP11)
                    }
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
            .keepAlive(isKeepAlive)
            .doOnChannelInit { connectionObserver, channel, socketAddress ->
                if (!isLogTransport) {
                    return@doOnChannelInit
                }

                fun http2ClientConnectionPrefaceListener(isPropagateEvent: Boolean): ChannelInboundHandlerAdapter = @ChannelHandler.Sharable object : ChannelInboundHandlerAdapter() {
                    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
                        log.i { "received ev $evt" }
                        if (evt is Http2ConnectionPrefaceAndSettingsFrameWrittenEvent && !isHttp2ClientConnectionPrefaceSent) {
                            isHttp2ClientConnectionPrefaceSent = true
                            // remove HTTP/2 frames, which suppose to be sent after client connection preface
                            // HTTP/2 frames are emitted via Http2FramePeeker
                            synchronized(writeLock) {
                                while (writeQueue.isNotEmpty()) {
                                    val e = writeQueue.last
                                    if (e.contentEquals(HTTP2_CONNECTION_PREFACE_BYTES)) {
                                        break
                                    } else {
                                        writeQueue.removeLast()
                                    }
                                }
                            }
                            log.i { "received Http2ConnectionPrefaceAndSettingsFrameWrittenEvent" }
                        }
                        if (isPropagateEvent) {
                            super.userEventTriggered(ctx, evt)
                        }
                    }

                    override fun handlerAdded(ctx: ChannelHandlerContext) {
                        if (ctx.handler() == this) {
                            if (isPropagateEvent) super.handlerAdded(ctx)
                            return // avoid infinite recursion
                        }
                        val handlerId = "com.sunnychung.application.multiplatform.hellohttp.network.http2ClientConnectionPrefaceListener.first2"
                        if (channel.pipeline().get(handlerId) != null) {
                            channel.pipeline().remove(handlerId)
                        }
                        channel.pipeline()
                            .addBefore(
                                /* baseName = */ NettyPipeline.ReactiveBridge,
                                /* name = */ handlerId,
                                /* handler = */ http2ClientConnectionPrefaceListener(true)
                            )

                        if (isPropagateEvent) super.handlerAdded(ctx)
                    }

                    override fun channelRegistered(ctx: ChannelHandlerContext?) {
                        if (isPropagateEvent) super.channelRegistered(ctx)
                    }

                    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
                        if (isPropagateEvent) super.channelUnregistered(ctx)
                    }

                    override fun channelActive(ctx: ChannelHandlerContext) {
                        if (isPropagateEvent) super.channelActive(ctx)
                    }

                    override fun channelInactive(ctx: ChannelHandlerContext) {
                        if (isPropagateEvent) super.channelInactive(ctx)
                    }

                    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                        if (isPropagateEvent) super.channelRead(ctx, msg)
                    }

                    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
                        if (isPropagateEvent) super.channelReadComplete(ctx)
                    }

                    override fun channelWritabilityChanged(ctx: ChannelHandlerContext?) {
                        if (isPropagateEvent) super.channelWritabilityChanged(ctx)
                    }

                    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
                        if (isPropagateEvent) super.exceptionCaught(ctx, cause)
                    }
                }

//                channel.pipeline().addBefore( // happy cases
//                    /* baseName = */ NettyPipeline.ReactiveBridge,
//                    /* name = */ "com.sunnychung.application.multiplatform.hellohttp.network.http2ClientConnectionPrefaceListener.first",
//                    /* handler = */ http2ClientConnectionPrefaceListener
//                )

                val realReactiveBridge = channel.pipeline().get(NettyPipeline.ReactiveBridge) as ChannelInboundHandler
                channel.pipeline()
                    .remove(realReactiveBridge)
                    .addLast( // for HTTP/1.1 upgrade to H2, also cover other happy cases
                        /* name = */ NettyPipeline.ReactiveBridge,
                        /* handler = */ DelegatedTerminalChannelInboundHandler(
                            listOf(
                                http2ClientConnectionPrefaceListener(isPropagateEvent = false),
                                realReactiveBridge
                            )
                        )
                    )
                    .addLast( // for HTTP/1.1 upgrade to H2C
                        /* name = */ "com.sunnychung.application.multiplatform.hellohttp.network.http2ClientConnectionPrefaceListener.last",
                        /* handler = */ http2ClientConnectionPrefaceListener(isPropagateEvent = true),
                    )

                if (IS_ENABLE_WIRETAP_LOG) {
                    channel.pipeline().addBefore(
                        /* baseName = */ NettyPipeline.LoggingHandler,
                        /* name = */ "${NettyPipeline.LoggingHandler}0",
                        /* handler = */
                        AdvancedByteBufFormat.SIMPLE.toLoggingHandler(".LoggingHandler0", LogLevel.INFO, Charsets.UTF_8)
                    )
                }
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
                val eventInstant = KInstant.now()
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
                    try {
                        callData?.response?.protocol = ProtocolVersion(
                            protocol = Protocol.Http,
                            versionName = httpProtocolVersion.drop("HTTP/".length)
                        )
                    } catch (_: Throwable) { /* ignore */ }

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

                        if (callData != null) {
                            CallDataUserResponseUtil.onTlsUpgraded(
                                callData = callData,
                                localCertificates = sslSession.localCertificates,
                                peerCertificates = sslSession.peerCertificates
                            )
                        }

                        append(".\n\n")
                        append("Client principal = [${sslSession.localPrincipal}]\n")
                        append("\n")
                        append("Server principal = [${sslSession.peerPrincipal}]\n")
                    } else {
                        append(" without encryption.")

                        if (callData != null) {
                            CallDataUserResponseUtil.onConnected(callData.response)
                        }
                    }
                }
                emitEvent(instant = eventInstant, callId = callId, event = eventDescription)

                log.d { "pipeline =>\n${conn.channel().pipeline().joinToString("\n") { it.key }}" }
            }
            .doOnDisconnected {
                emitEvent(callId, "Disconnected")
            }
            .observe { connection, state ->
//                emitEvent(callId, "Connection state => $state")
            }
            .run {
                if (!isLogTransport) {
                    return@run this
                }

                val frameLogger = Http2FramePeeker(
                    outgoingBytesFlow = outgoingBytesFlow,
                    incomingBytesFlow = incomingBytesFlow,
                    http2AccumulatedOutboundDataSerializeLimit = http2AccumulatedOutboundDataSerializeLimit,
                    http2AccumulatedInboundDataSerializeLimit = http2AccumulatedInboundDataSerializeLimit,
                )

                loggingHandler(object : ChannelDuplexHandler() {
                    fun emitRawPayload(direction: RawExchange.Direction, channel: Channel?, payload: Any) {
                        if (isHttp2Established) {
                            return
                        }
                        val receiveTime = KInstant.now()
                        if (isHttp2ClientConnectionPrefaceSent && direction == RawExchange.Direction.Incoming && isStartWithHttp2ServerConnectionPrefaceFrame(payload)) {
                            isHttp2Established = true
                            return
                        }
                        log.v { "emitRawPayload $httpClientId h2=$isHttp2Established p=$isHttp2ClientConnectionPrefaceSent" }
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

                        log.d { "channelActive pipeline =>\n${ctx.pipeline().joinToString("\n") { it.key }}" }

                        super.channelActive(ctx)
                    }

                    override fun channelInactive(ctx: ChannelHandlerContext) {
    //                    emitEvent(callId, "Connection is inactive") // no use
                        super.channelInactive(ctx)
                    }

                    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                        emitRawPayload(direction = RawExchange.Direction.Incoming, channel = ctx.channel(), payload = msg)
                        super.channelRead(ctx, msg)
                    }

                    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
                        log.i { "userEventTriggered $evt" }
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
    //                    emitEvent(callId, "Disconnected") // no use
                        super.disconnect(ctx, promise)
                    }

                    override fun close(ctx: ChannelHandlerContext?, promise: ChannelPromise?) {
    //                    emitEvent(callId, "Terminating") // no use
                        super.close(ctx, promise)
                    }

                    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
                        if (!isHttp2Established && !isHttp2ClientConnectionPrefaceSent) {
                            synchronized(writeLock) {
                                readPayload(msg).let {
                                    if (it.isNotEmpty()) {
                                        writeQueue.addLast(it)
                                        currentTotalWrittenBytes += it.size
                                    }
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
                        frameLogger.flush()
                        super.flush(ctx)
                    }
                })
                .http2FrameLogger(frameLogger)
            }
            .compress(true)
            .headers { it.remove(HttpHeaderNames.ACCEPT_ENCODING) } // don't add "Accept-Encoding: gzip" header by default
            .doOnResponse { resp, conn ->
                log.d { "NettyIO onResponse" }
            }
            .doOnRequestError { req, err ->
                log.d(err) { "NettyIO doOnRequestError" }
            }
            .doOnResponseError { resp, err ->
                log.d(err) { "NettyIO doOnResponseError" }
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

    /**
     * Check according to RFC 9113.
     */
    protected fun isStartWithHttp2ServerConnectionPrefaceFrame(payload: Any?): Boolean {
        return when (payload) {
            is ByteBuf -> {
                val size = payload.readableBytes()
                if (size < 9) {
                    return false
                }

                // read without changing readerIndex
                val readerIndex = payload.readerIndex()

                val frameType = payload.getByte(readerIndex + 3)
                if (frameType.toInt() != 0x04) { // not Setting frame
                    return false
                }

                val streamId = payload.getInt(readerIndex + 5)
                // The stream identifier for a SETTINGS frame MUST be zero (0x00).
                if (streamId and 0x7FFFFFFF != 0) {
                    return false
                }

                val payloadLength = payload.getUnsignedMedium(readerIndex)
                // A SETTINGS frame with a length other than a multiple of 6 octets MUST be treated as
                // a connection error (Section 5.4.1) of type FRAME_SIZE_ERROR.
                if (payloadLength % 6 != 0) {
                    return false
                }

                return true
            }

            is ByteBufHolder -> isStartWithHttp2ServerConnectionPrefaceFrame(payload.content())

            is ByteArray -> isStartWithHttp2ServerConnectionPrefaceFrame(Unpooled.wrappedBuffer(payload))

            else -> false
        }
    }

    override fun sendRequest(
        callId: String,
        coroutineScope: CoroutineScope,
        client: Any?,
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        subprojectConfig: SubprojectConfiguration,
        fireType: UserResponse.Type,
        loadTestState: LoadTestState?,
    ): CallData {
        val uri = request.getResolvedUri()
        val data = createCallData(
            coroutineScope = coroutineScope,
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            subprojectConfig = subprojectConfig,
            fireType = fireType,
            loadTestState = loadTestState,
        )
        val callId = data.id

        val httpClient = buildHttpClient(
            callId = callId,
            callData = data,
            isSsl = uri.scheme !in setOf("http", "ws"),
            isKeepAlive = false,
            httpConfig = httpConfig,
            sslConfig = sslConfig,
            isLogTransport = true,
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
                    .uri(uri)
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

//                            is FileBody -> send { req, outbound ->
//                                outbound.send(ByteBufFlux.fromPath(Path.of(body.filePath!!), REQUEST_CHUNK_SIZE))
//                                    .then()
//                            }

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

                log.d { "Request started at ${out.startAt!!.atLocalZoneOffset()}" }

                requestBuilder
                    .awaitFirstOrNull()
//                    .awaitSingleOrNull()
                    .let {
//                        val it: ByteArray? = null
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

    override fun createReusableNonInspectableClient(
        parentCallId: String,
        concurrency: Int,
        request: HttpRequest,
        httpConfig: HttpConfig,
        sslConfig: SslConfig
    ): Any? {
        return null
    }
}

package com.sunnychung.application.multiplatform.hellohttp.network

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import com.sunnychung.application.multiplatform.hellohttp.extension.GrpcRequestExtra
import com.sunnychung.application.multiplatform.hellohttp.helper.CountDownLatch
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcMethod
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolVersion
import com.sunnychung.application.multiplatform.hellohttp.model.RequestData
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.util.CallDataUserResponseUtil
import com.sunnychung.application.multiplatform.hellohttp.network.util.flowAndStreamObserver
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.GrpcChannelListener
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2CodecUtil
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Flags
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2FrameLogger
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2FrameLogger.Direction
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Settings
import io.grpc.netty.shaded.io.netty.handler.logging.LogLevel
import io.grpc.netty.shaded.io.netty.util.AsciiString
import io.grpc.protobuf.ProtoUtils
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.hc.core5.http2.H2Error
import java.net.SocketAddress
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLSession

class GrpcTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {

    fun buildChannel(
        callId: String, uri: URI, sslConfig: SslConfig,
        outgoingBytesFlow: MutableSharedFlow<RawPayload>?,
        incomingBytesFlow: MutableSharedFlow<RawPayload>?,
        callData: CallData?, out: UserResponse?
    ): ManagedChannel {
        val uri0 = uri.let {
            val uriString = it.toString()
            if (!uriString.contains("://")) {
                URI.create("grpcs://$uriString")
            } else {
                it
            }
        }
        return (ManagedChannelBuilder.forAddress(uri0.host, uri0.port) as NettyChannelBuilder)
            .apply {
                if (uri0.scheme in setOf("http", "grpc")) {
                    usePlaintext();
                } else {
                    if (sslConfig.hasCustomConfig()) {
                        val customSsl = createSslContext(sslConfig)
                        GrpcSslContexts.forClient()
                            .keyManager(customSsl.keyManager)
                            .trustManager(customSsl.trustManager)
                            .build()
                            .let { sslContext(it) }
                    }
                }
                if (outgoingBytesFlow != null && incomingBytesFlow != null) {
                    fun emitFrame(direction: Direction, streamId: Int?, content: String) = runBlocking {
                        when (direction) {
                            Direction.OUTBOUND -> outgoingBytesFlow
                            Direction.INBOUND -> incomingBytesFlow
                        }.emit(Http2Frame(
                            instant = KInstant.now(),
                            streamId = streamId?.takeIf { it >= 1 },
                            content = content
                        ))
                    }

                    fun serializeFlags(flags: Map<String, Boolean>): String {
                        return flags.filter { it.value }
                            .keys
                            .joinToString(separator = " ")
                            .emptyToNull()
                            ?: "-"
                    }

                    frameLogger(object : Http2FrameLogger(LogLevel.DEBUG) {
                        var isFirstSettingFrame = true

                        override fun logSettings(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            settings: Http2Settings
                        ) {
                            if (isFirstSettingFrame) {
                                // TODO log the bytes directly in netty ChannelOutboundInvoker#write
                                emitFrame(direction, null, Http2CodecUtil.connectionPrefaceBuf().serialize())
                                isFirstSettingFrame = false
                            }

                            emitFrame(
                                direction,
                                null,
                                "Frame: SETTINGS; flags: -\n" +
                                        settings.map { "${http2SettingKey(it.key)}: ${it.value}" }.joinToString("\n")
                            )
                        }

                        override fun logSettingsAck(direction: Direction, ctx: ChannelHandlerContext) {
                            emitFrame(
                                direction,
                                null,
                                "Frame: SETTINGS; flags: ACK"
                            )
                        }

                        override fun logWindowsUpdate(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            windowSizeIncrement: Int
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: WINDOW_UPDATE\nIncrement $windowSizeIncrement"
                            )
                        }

                        override fun logPing(direction: Direction, ctx: ChannelHandlerContext, data: Long) {
                            emitFrame(
                                direction,
                                null,
                                "Frame: PING; flags: -\nData: $data"
                            )
                        }

                        override fun logPingAck(direction: Direction, ctx: ChannelHandlerContext, data: Long) {
                            emitFrame(
                                direction,
                                null,
                                "Frame: PING; flags: ACK\nData: $data"
                            )
                        }

                        override fun logHeaders(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            headers: Http2Headers,
                            padding: Int,
                            endStream: Boolean
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: HEADERS; flags: ${
                                    serializeFlags(mapOf("END_STREAM" to endStream))
                                }\n${
                                    headers.joinToString("\n") { "${it.key}: ${it.value}" }
                                }"
                            )

                            if (out != null && headers.contains(AsciiString("grpc-status"))) {
                                try {
                                    val status = headers.get(AsciiString("grpc-status")).toString().toIntOrNull()
                                    status?.let { status ->
                                        out.statusCode = status
                                        out.statusText = Status.fromCodeValue(status).code.name
                                    }
                                } catch (e: Throwable) {
                                    log.w(e) { "Cannot parse grpc status code" }
                                }
                            }

                            handleHeaders(direction = direction, headers = headers)
                        }

                        fun handleHeaders(direction: Direction, headers: Http2Headers) {
                            if (out == null) return
                            when (direction) {
                                Direction.OUTBOUND -> {
                                    out.requestData!!.headers = (out.requestData!!.headers ?: emptyList()) +
                                            headers.map { it.key.toString() to it.value.toString() }
                                }
                                Direction.INBOUND -> {
                                    out.headers = (out.headers ?: emptyList()) +
                                            headers.map { it.key.toString() to it.value.toString() }
                                }
                            }

                        }

                        override fun logHeaders(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            headers: Http2Headers,
                            streamDependency: Int,
                            weight: Short,
                            exclusive: Boolean,
                            padding: Int,
                            endStream: Boolean
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: HEADERS; streamDependency: $streamDependency; weight: $weight; flags: ${
                                    serializeFlags(mapOf("EXCLUSIVE" to exclusive, "END_STREAM" to endStream))
                                }\n${
                                    headers.joinToString("\n") { "${it.key}: ${it.value}" }
                                }"
                            )

                            handleHeaders(direction = direction, headers = headers)
                        }

                        override fun logData(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            data: ByteBuf,
                            padding: Int,
                            endStream: Boolean
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: DATA; flags: ${
                                    serializeFlags(mapOf("END_STREAM" to endStream))
                                }; length: ${data.readableBytes()}\n${
                                    data.serialize()
                                }"
                            )
                        }

                        override fun logPushPromise(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            promisedStreamId: Int,
                            headers: Http2Headers,
                            padding: Int
                        ) {
                            // actually gRPC won't have this

                            emitFrame(
                                direction,
                                streamId,
                                "Frame: PUSH_PROMISE; promisedStreamId: $promisedStreamId\n${
                                    headers.joinToString("\n") { "${it.key}: ${it.value}" }
                                }"
                            )
                        }

                        override fun logRstStream(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            errorCode: Long
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: RST_STREAM\n" +
                                "Code ${serializeErrorCode(errorCode)}"
                            )
                        }

                        override fun logGoAway(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            lastStreamId: Int,
                            errorCode: Long,
                            debugData: ByteBuf
                        ) {
                            emitFrame(
                                direction,
                                null,
                                "Frame: GOAWAY; length: ${debugData.readableBytes()}\n" +
                                        "Last stream $lastStreamId\n" +
                                        "Code ${serializeErrorCode(errorCode)}\n" +
                                        debugData.serialize()
                            )
                        }

                        override fun logPriority(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            streamId: Int,
                            streamDependency: Int,
                            weight: Short,
                            exclusive: Boolean
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: PRIORITY; streamDependency: $streamDependency; weight: $weight; flags: ${
                                    serializeFlags(mapOf("EXCLUSIVE" to exclusive))
                                }"
                            )
                        }

                        override fun logUnknownFrame(
                            direction: Direction,
                            ctx: ChannelHandlerContext,
                            frameType: Byte,
                            streamId: Int,
                            flags: Http2Flags,
                            data: ByteBuf
                        ) {
                            emitFrame(
                                direction,
                                streamId,
                                "Frame: Unknown (0x${Integer.toHexString(frameType.toInt())}); flags: $flags; length: ${data.readableBytes()}\n${
                                    data.serialize()
                                }"
                            )
                        }

                        fun ByteBuf.serialize(): String {
                            val bytes = ByteArray(readableBytes())
                            getBytes(readerIndex(), bytes)
                            return bytes.decodeToString()
                        }

                        fun serializeErrorCode(errorCode: Long): String {
                            return H2Error.getByCode(errorCode.toInt())?.name ?: "0x${Integer.toHexString(errorCode.toInt())}"
                        }

                        fun http2SettingKey(key: Char): String {
                            return when (key) {
                                '\u0001' -> "HEADER_TABLE_SIZE"
                                '\u0002' -> "ENABLE_PUSH"
                                '\u0003' -> "MAX_CONCURRENT_STREAMS"
                                '\u0004' -> "INITIAL_WINDOW_SIZE"
                                '\u0005' -> "MAX_FRAME_SIZE"
                                '\u0006' -> "MAX_HEADER_LIST_SIZE"
                                else -> "0x" + Integer.toHexString(key.code)
                            }
                        }
                    })
                }
            }
            .channelListener(object : GrpcChannelListener {
                override fun onDnsStartResolve(host: String?) {
                    emitEvent(callId, "DNS resolution of domain [$host] started")
                }

                override fun onDnsResolved(addresses: List<SocketAddress>) {
                    emitEvent(callId, "DNS resolved to $addresses")
                    emitEvent(callId, "Connecting to the host") // TODO can the actual host be grabbed?
                }

                override fun onChannelActive() {
                    if (callData != null) {
                        CallDataUserResponseUtil.onConnected(callData.response)
                    }
                    emitEvent(callId, "HTTP/2 channel established.")
                }

                override fun onChannelInactive() {
                    emitEvent(callId, "Channel closed.")
                }

                override fun onTlsHandshakeComplete(session: SSLSession, applicationProtocol: String?) {
                    if (callData != null) {
                        CallDataUserResponseUtil.onTlsUpgraded(
                            callData = callData,
                            localCertificates = session.localCertificates,
                            peerCertificates = session.peerCertificates,
                        )
                    }

                    var event = "Established TLS upgrade with protocol '${session.protocol}', cipher suite '${session.cipherSuite}'"
                    if (!applicationProtocol.isNullOrBlank()) {
                        event += " and application protocol '$applicationProtocol'"
                    }
                    event += ".\n\n" +
                            "Client principal = ${session.localPrincipal}\n" +
                            "Server principal = ${session.peerPrincipal}\n"
                    emitEvent(callId, event)
                }

            })
            .disableRetry()
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
        subprojectConfig: SubprojectConfiguration,
    ): CallData {
        val uri = URI.create(request.url)

        val call = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            subprojectConfig = subprojectConfig,
        )

        val extra = request.extra as GrpcRequestExtra
        val `package` = extra.destination.service.substringBeforeLast(delimiter = '.', missingDelimiterValue = "")
        val serviceName = extra.destination.service.substringAfterLast('.')
        val serviceFullName = listOf(`package`, serviceName).joinToString(separator = ".")
        val methodName = extra.destination.method

        val apiSpec: GrpcApiSpec = extra.apiSpec!!

        val out = call.response
        out.requestData = RequestData(
            method = "POST",
            url = uri.toASCIIString(),
        )

        CoroutineScope(Dispatchers.IO).launch {
            call.status = ConnectionStatus.CONNECTING

            try {

                call.waitForPreparation()
                out.startAt = KInstant.now()
                out.application = ProtocolApplication.Grpc
                out.protocol = ProtocolVersion(Protocol.Http, 2, 0)

                val channel0 = buildChannel(
                    callId = call.id,
                    uri = uri,
                    sslConfig = sslConfig,
                    outgoingBytesFlow = call.outgoingBytes as MutableSharedFlow<RawPayload>,
                    incomingBytesFlow = call.incomingBytes as MutableSharedFlow<RawPayload>,
                    callData = call,
                    out = out,
                ) // blocking call
                val channel = ClientInterceptors.intercept(channel0, object : ClientInterceptor {
                    override fun <ReqT : Any?, RespT : Any?> interceptCall(
                        method: MethodDescriptor<ReqT, RespT>,
                        callOptions: CallOptions,
                        next: Channel
                    ): ClientCall<ReqT, RespT> =
                        object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                                request.headers.forEach {
                                    headers.put(Metadata.Key.of(it.first, Metadata.ASCII_STRING_MARSHALLER), it.second)
                                }
                                super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener) {
                                    override fun onHeaders(headers: Metadata) {
                                        super.onHeaders(headers)
                                    }

                                    override fun onMessage(message: RespT) {
                                        log.d { "grpc inte onMessage" }
                                        super.onMessage(message)
                                    }

                                    override fun onClose(status: Status?, trailers: Metadata?) {
                                        log.d { "grpc inte onClose $status [$trailers]" }
                                        super.onClose(status, trailers)
                                    }
                                }, headers)
                            }
                        }

                })

                val fileDescriptorProtoByFilename = apiSpec.rawFileDescriptors.map { FileDescriptorProto.parseFrom(it) }
                    .associateBy { it.name }
                val fileDescriptorProto = fileDescriptorProtoByFilename.values
                    .first { it.`package` == `package` && it.serviceList.any { it.name == serviceName && it.methodList.any { it.name == methodName } } }
                val fileDescriptor = buildFileDescriptor(fileDescriptorProto, fileDescriptorProtoByFilename)
                val methodDescriptor0 =
                    fileDescriptor.services.first { it.fullName == serviceFullName }.methods.first { it.name == methodName }
                val grpcMethodDescriptor = MethodDescriptor.newBuilder<DynamicMessage, DynamicMessage>()
                    .setFullMethodName(
                        MethodDescriptor.generateFullMethodName(
                            methodDescriptor0.service.fullName,
                            methodDescriptor0.name
                        )
                    )
                    .setRequestMarshaller(
                        ProtoUtils.marshaller(
                            DynamicMessage.newBuilder(methodDescriptor0.inputType).build()
                        )
                    )
                    .setResponseMarshaller(
                        ProtoUtils.marshaller(
                            DynamicMessage.newBuilder(methodDescriptor0.outputType).build()
                        )
                    )
                    .setType(MethodDescriptor.MethodType.UNKNOWN) // TODO
                    .build()

                val typeRegistry = TypeRegistry.newBuilder().add(fileDescriptor.messageTypes).build()
                val jsonParser = JsonFormat.parser().usingTypeRegistry(typeRegistry)

                fun buildGrpcRequest(json: String): DynamicMessage {
                    return DynamicMessage.newBuilder(methodDescriptor0.inputType)
                        .apply {
                            jsonParser.merge(json, this)
                        }
                        .build()
                }

                fun setStreamError(e: Throwable) {
                    if (methodDescriptor0.isClientStreaming || methodDescriptor0.isServerStreaming) {
                        out.payloadExchanges!! += PayloadMessage(
                            instant = KInstant.now(),
                            type = PayloadMessage.Type.Error,
                            data = (e.message?.let { "Error: $it" } ?: "Error").encodeToByteArray()
                        )
                    } else {
                        out.errorMessage = e.message
                    }
                    out.isError = true
                    emitEvent(call.id, "Encountered error - ${e.message}")
                    log.d(e) { "gRPC stream error" }
                }

                var (responseFlow, responseObserver) = flowAndStreamObserver<DynamicMessage>()
                try {
                    val cancel = { _: Throwable? ->
                        if (call.status.isConnectionActive()) {
                            try {
                                responseObserver.onCompleted()
                            } catch (_: Throwable) {}
                            try {
                                channel0.shutdown()
                            } catch (_: Throwable) {}
                            call.status = ConnectionStatus.DISCONNECTED
                        }
                    }
                    call.cancel = cancel

                    val grpcCall = channel.newCall(grpcMethodDescriptor, CallOptions.DEFAULT)
                    val jsonPrinter = JsonFormat.printer()
                        .usingTypeRegistry(typeRegistry)
                        .includingDefaultValueFields()

                    if (methodDescriptor0.isClientStreaming || methodDescriptor0.isServerStreaming) {
                        out.payloadExchanges = mutableListOf()
                    }

                    responseFlow = responseFlow.onEach {
                        try {
                            val responseJsonData = jsonPrinter.print(it).encodeToByteArray()
                            if (methodDescriptor0.isClientStreaming || methodDescriptor0.isServerStreaming) {
                                out.payloadExchanges!! += PayloadMessage(
                                    instant = KInstant.now(),
                                    type = PayloadMessage.Type.IncomingData,
                                    data = responseJsonData
                                )
                            } else {
                                out.body = responseJsonData
                            }
                            log.d { "Response = ${responseJsonData.decodeToString()}" }
                        } catch (e: Throwable) {
                            setStreamError(e)
                            call.cancel(e)
                        }

                        if (postFlightAction != null) {
                            executePostFlightAction(call.id, out, postFlightAction)
                        }
                    }

                    fun buildSendPayloadFunction(requestObserver: StreamObserver<DynamicMessage>): (String) -> Unit {
                        return {
                            try {
                                val request = buildGrpcRequest(it)
                                out.payloadExchanges!! += PayloadMessage(
                                    instant = KInstant.now(),
                                    type = PayloadMessage.Type.OutgoingData,
                                    data = it.encodeToByteArray()
                                )
                                requestObserver.onNext(request)
                            } catch (e: Throwable) {
                                setStreamError(e)
                                call.cancel(e)
                            }
                        }
                    }

                    fun buildSendEndOfStream(requestObserver: StreamObserver<DynamicMessage>): () -> Unit {
                        var hasInvokedCancelDueToError = false
                        return {
                            try {
                                requestObserver.onCompleted()
                            } catch (e: Throwable) {
                                if (!hasInvokedCancelDueToError) {
                                    hasInvokedCancelDueToError = true
                                    setStreamError(e)
                                    call.cancel(e)
                                }
                            }
                        }
                    }

                    fun initiateClientStreamableCall(requestObserver: StreamObserver<DynamicMessage>) {
                        call.sendPayload = buildSendPayloadFunction(requestObserver)
                        call.sendEndOfStream = buildSendEndOfStream(requestObserver)
                        call.cancel = { e ->
                            if (call.status.isConnectionActive()) {
                                try {
                                    call.sendEndOfStream()
                                } catch (_: Throwable) {}
                                cancel(e)
                            }
                        }
                        // actually, at this stage it could be not yet connected
                        call.status = ConnectionStatus.OPEN_FOR_STREAMING
                        emitEvent(call.id, "Created a channel for streaming")
                    }

                    if (!methodDescriptor0.isClientStreaming && !methodDescriptor0.isServerStreaming) {
                        ClientCalls.asyncUnaryCall(
                            grpcCall,
                            buildGrpcRequest((request.body as StringBody).value),
                            responseObserver
                        )
                    } else if (methodDescriptor0.isClientStreaming && !methodDescriptor0.isServerStreaming) {
                        initiateClientStreamableCall(ClientCalls.asyncClientStreamingCall(
                            grpcCall, responseObserver
                        ))
                    } else if (!methodDescriptor0.isClientStreaming && methodDescriptor0.isServerStreaming) {
                        ClientCalls.asyncServerStreamingCall(
                            grpcCall,
                            buildGrpcRequest((request.body as StringBody).value),
                            responseObserver
                        )
                    } else if (methodDescriptor0.isClientStreaming && methodDescriptor0.isServerStreaming) {
                        initiateClientStreamableCall(ClientCalls.asyncBidiStreamingCall(
                            grpcCall, responseObserver
                        ))
                    } else {
                        throw IllegalStateException("This condition should not be reached")
                    }

                    responseFlow.collect()

                    emitEvent(call.id, "Received response message") // TODO better events?

                    out.endAt = KInstant.now()
                    if (!out.isError && out.statusCode == null && out.statusText == null) {
                        out.statusText = "No error"
                    }
                    call.status = ConnectionStatus.DISCONNECTED

                    if (!out.isError && postFlightAction != null) {
                        executePostFlightAction(call.id, out, postFlightAction)
                    }
                } catch (e: StatusRuntimeException) {
                    out.endAt = KInstant.now()
                    call.status = ConnectionStatus.DISCONNECTED

                    out.statusCode = e.status.code.value()
                    out.statusText = e.status.code.name
                    setStreamError(e)

                    log.d(e) { "Grpc Status Error" }
                } catch (e: Throwable) {
                    out.endAt = KInstant.now()
                    call.status = ConnectionStatus.DISCONNECTED

                    setStreamError(e)

                    log.d(e) { "Grpc Error" }
                } finally {
                    channel0.shutdownNow()
                    if (methodDescriptor0.isClientStreaming || methodDescriptor0.isServerStreaming) {
                        out.payloadExchanges!! += PayloadMessage(
                            instant = KInstant.now(),
                            type = PayloadMessage.Type.Disconnected,
                            data = "Disconnected.".encodeToByteArray(),
                        )
                    }

                    call.consumePayloads()
                    emitEvent(call.id, "Response completed")
                    call.end()
                }
            } catch (e: Throwable) {
                log.d(e) { "Grpc Outer Error" }

                call.status = ConnectionStatus.DISCONNECTED
                out.errorMessage = e.message
                out.isError = true

                emitEvent(call.id, "Terminated with error")
                call.end()
            }
        }
        return call
    }

    private fun buildFileDescriptor(proto: FileDescriptorProto, fileDescriptorProtoByFilename: Map<String, FileDescriptorProto>): FileDescriptor {
        return FileDescriptor.buildFrom(proto, proto.dependencyList.map { buildFileDescriptor(fileDescriptorProtoByFilename[it]!!, fileDescriptorProtoByFilename) }.toTypedArray())
    }

    suspend fun fetchServiceSpec(
        url: String,
        sslConfig: SslConfig
    ): GrpcApiSpec {
        val uri = URI.create(url)
        log.d { "fetchServiceSpec $uri" }
        val channel = buildChannel("", uri, sslConfig, null, null, null, null)
        try {
            return fetchServiceSpec("${uri.host}:${uri.port}", channel)
        } finally {
            channel.shutdownNow()
        }
    }

    suspend fun fetchServiceSpec(name: String, channel: ManagedChannel): GrpcApiSpec {
        val stub = ServerReflectionGrpc.newStub(channel)

        val (responseFlow, responseObserver) = flowAndStreamObserver<ServerReflectionResponse>()
//        val serviceResponseFlow = responseFlow.map {
//            it.listServicesResponse.serviceList
//        }
        val request = stub.serverReflectionInfo(responseObserver)
        request.onNext(ServerReflectionRequest.newBuilder()
            .setListServices("*")
            .build())
        val services = responseFlow
            .filter { it.messageResponseCase == ServerReflectionResponse.MessageResponseCase.LIST_SERVICES_RESPONSE }
            .map {
                it.listServicesResponse.serviceList
            }.first()

        val fileDescriptorByServiceNameByProtoFilename = ConcurrentHashMap<String, Map<String, FileDescriptorProto>>()
        val rawFileDescriptors = Collections.synchronizedList(mutableListOf<ByteArray>())
        val latch = CountDownLatch(services.size)
        services.forEach {
            log.d { "grpc refl svc '${it.name}'" }
//            serviceToFileDescriptor[it.name] = ""
            request.onNext(ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(it.name)
                .build())
        }

        val flow2 = responseFlow
            .filter { it.messageResponseCase == ServerReflectionResponse.MessageResponseCase.FILE_DESCRIPTOR_RESPONSE }
            .map {
//            log.d { "resp [${it.originalRequest.fileContainingSymbol}] = $it" }

                rawFileDescriptors += it.fileDescriptorResponse.fileDescriptorProtoList.map { it.toByteArray() }
                fileDescriptorByServiceNameByProtoFilename[it.originalRequest.fileContainingSymbol] =
                    it.fileDescriptorResponse.fileDescriptorProtoList
                        .map { FileDescriptorProto.parseFrom(it) }
                        .onEach { log.d { "fd = $it" } }
                        .associateBy { it.name }

                latch.decrement()
            }
            .launchIn(CoroutineScope(Dispatchers.IO))
        latch.await()
        flow2.cancel() // release resource

        val methods = fileDescriptorByServiceNameByProtoFilename.flatMap { (serviceName, fileDescriptors) ->
            fileDescriptors.flatMap { (k, v) ->
                v.serviceList.flatMap {
                    val serviceFullName = "${v.`package`}.${it.name}"
                    it.methodList.map { GrpcMethod(serviceFullName, it.name, it.clientStreaming, it.serverStreaming) }
                }
            }
        }
            .distinct()
//            .groupBy { it.serviceFullName }
//            .mapValues { it.value.distinct() }

        log.d { "Fetch spec result:\n$methods" }

        log.d { "done" }

        return GrpcApiSpec(
            id = uuidString(),
            name = name,
            methods = methods,
            rawFileDescriptors = rawFileDescriptors.distinct(),
            source = GrpcApiSpec.Source.Reflection,
            isActive = true,
            updateTime = KInstant.now(),
        )
    }

}

fun hostFromUrl(url: String): String {
    return try {
        val uri = URI.create(url)
        "${uri.host}:${uri.port}"
    } catch (_: Throwable) {
        ""
    }
}

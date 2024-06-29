package com.sunnychung.application.multiplatform.hellohttp.network.netty

import com.sunnychung.application.multiplatform.hellohttp.network.Http2Frame
import com.sunnychung.application.multiplatform.hellohttp.network.RawPayload
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2Flags
import io.netty.handler.codec.http2.Http2FrameLogger
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.codec.http2.InspectedHttp2Headers
import io.netty.handler.codec.http2.InspectedHttp2Headers.InspectedHttp2HeaderEntry
import io.netty.handler.logging.LogLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.apache.hc.core5.http2.H2Error
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

private const val LONGEST_HEADER_LOG_PREFIX = "NAME INDEXED (123)"

/**
 * Peek all the HTTP/2 frames to given MutableSharedFlows.
 *
 * Note the implementation is slightly different from that in GrpcTransportClient. In particular,
 * gRPC-specific handlings are removed. Also, packages of Netty stuffs is different, which makes it
 * impossible to commonize duplicated codes.
 */
class Http2FramePeeker(
    private val outgoingBytesFlow: MutableSharedFlow<RawPayload>,
    private val incomingBytesFlow: MutableSharedFlow<RawPayload>,
    http2AccumulatedOutboundDataSerializeLimit: Int,
    http2AccumulatedInboundDataSerializeLimit: Int,
) : Http2FrameLogger(LogLevel.TRACE) {
    val outboundDataSerializeCredit = AtomicInteger(http2AccumulatedOutboundDataSerializeLimit)
    val inboundDataSerializeCredit = AtomicInteger(http2AccumulatedInboundDataSerializeLimit)

    val writeQueue = ConcurrentLinkedDeque<Http2Frame>()

    val outboundTransmitCreditsForDebug = ConcurrentHashMap<Int, AtomicInteger>()

//    var isFirstSettingFrame = true

    fun outboundTransmitCreditForDebug(streamId: Int) = outboundTransmitCreditsForDebug.getOrPut(streamId) {
        AtomicInteger(65535) // TODO respect the value of SETTINGS_INITIAL_WINDOW_SIZE
    }

    fun emitFrame(direction: Direction, streamId: Int?, content: String) =
        emitFrame(emitInstant = KInstant.now(), direction = direction, streamId = streamId, content = content)

    fun emitFrame(emitInstant: KInstant, direction: Direction, streamId: Int?, content: String) {
        val frame = Http2Frame(
            instant = emitInstant,
            streamId = streamId?.takeIf { it >= 1 } ?: 0,
            content = content
        )
        when (direction) {
            Direction.OUTBOUND -> {
                log.v { "Http2FramePeeker writeQueue.addLast start" }
                writeQueue.addLast(frame)
                log.v { "Http2FramePeeker writeQueue.addLast end" }
            }
            Direction.INBOUND -> runBlocking {
                incomingBytesFlow.emit(frame)
            }
        }
    }

    fun serializeFlags(flags: Map<String, Boolean>): String {
        return flags.filter { it.value }
            .keys
            .joinToString(separator = " ")
            .emptyToNull()
            ?: "-"
    }

    fun serializeHeader(it: Map.Entry<CharSequence, CharSequence>): String {
        return if (it is InspectedHttp2HeaderEntry) {
            val prefix = when (it.format) {
                InspectedHttp2Headers.BinaryFormat.INDEXED -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "INDEXED", "(${it.index})")}] "
                InspectedHttp2Headers.BinaryFormat.LITERAL_WITH_INDEXING -> {
                    if (it.index != null) {
                        "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "NAME INDEXED", "(${it.index})")}] "
                    } else {
                        "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "+ NEW INDEX", "")}] "
                    }
                }
                InspectedHttp2Headers.BinaryFormat.LITERAL_WITHOUT_INDEXING -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "- NOT INDEXED", "")}] " //HEADER_LOG_PREFIX_SPACES
                InspectedHttp2Headers.BinaryFormat.LITERAL_NEVER_INDEXED -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "SENSITIVE", "")}] "
                InspectedHttp2Headers.BinaryFormat.OTHER -> ""
            }
            val content = when (it.key) {
                InspectedHttp2Headers.PSEUDO_HEADER_KEY_DYNAMIC_TABLE_SIZE_UPDATE -> {
                    "Update dynamic table size to ${it.value}"
                }
                else -> "${it.key}: ${it.value}"
            }
            "$prefix$content"
        } else {
            "${it.key}: ${it.value}"
        }
    }

    private fun formatHeaderPrefix(length: Int, begin: String, end: String): String {
        return begin + " ".repeat(maxOf(length - begin.length - end.length, 0)) + end
    }

    fun flush() {
        log.v { "Http2FramePeeker flush start" }
        if (writeQueue.isEmpty()) {
            log.v { "Http2FramePeeker flush empty" }
            return
        }
        val flushTime = KInstant.now()
        log.v { "Http2FramePeeker flush b4 runBlocking" }
        runBlocking {
            log.v { "Http2FramePeeker flush runBlocking" }
            while (writeQueue.isNotEmpty()) {
                log.v { "Http2FramePeeker flush poll" }
                val frame = writeQueue.pollFirst()
                log.v { "Http2FramePeeker flush emit" }
                outgoingBytesFlow.emit(frame.copy(instant = flushTime))
                log.v { "Http2FramePeeker flush emited" }
            }
            log.v { "Http2FramePeeker flush after while" }
        }
        log.v { "Http2FramePeeker flush end" }
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun logSettings(
        direction: Direction,
        ctx: ChannelHandlerContext,
        settings: Http2Settings
    ) {
//        if (isFirstSettingFrame) {
//            // TODO log the bytes directly in netty ChannelOutboundInvoker#write
//            emitFrame(direction, null, Http2CodecUtil.connectionPrefaceBuf().serialize())
//            isFirstSettingFrame = false
//        }

        emitFrame(
            direction,
            null,
            "Frame: SETTINGS; flags: -\n" +
                    settings.map { "${http2SettingKey(it.key)}: ${it.value}" }.joinToString("\n")
        )

        log.v { "${if (direction == Direction.OUTBOUND) ">" else "<"} HTTP2 Frame: SETTINGS; flags: -\n" +
                settings.map { "${http2SettingKey(it.key)}: ${it.value}" }.joinToString("\n") }
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

        log.v {
            if (direction == Direction.INBOUND) outboundTransmitCreditForDebug(streamId).addAndGet(windowSizeIncrement)
            "${if (direction == Direction.OUTBOUND) ">" else "<"} HTTP2 Frame: WINDOW_UPDATE {$streamId}\nIncrement $windowSizeIncrement (= ${outboundTransmitCreditForDebug(streamId)})"
        }
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
        val receiveInstant = KInstant.now()
        emitFrame(
            receiveInstant,
            direction,
            streamId,
            "Frame: HEADERS; flags: ${
                serializeFlags(mapOf("END_STREAM" to endStream))
            }\n${
                headers.joinToString("\n") { serializeHeader(it) }
            }"
        )

        log.v { "${if (direction == Direction.OUTBOUND) ">" else "<"} HTTP2 Frame: HEADERS; flags: ${
            serializeFlags(mapOf("END_STREAM" to endStream))
        }\n${
            headers.joinToString("\n") { "${it.key}: ${it.value}" }
        }" }

//        if (out != null && headers.contains(AsciiString("grpc-status"))) {
//            try {
//                val status = headers.get(AsciiString("grpc-status")).toString().toIntOrNull()
//                status?.let { status ->
//                    out.statusCode = status
//                    out.statusText = Status.fromCodeValue(status).code.name
//                }
//            } catch (e: Throwable) {
//                log.w(e) { "Cannot parse grpc status code" }
//            }
//        }
//
//        handleHeaders(direction = direction, headers = headers)
    }

//    fun handleHeaders(direction: Direction, headers: Http2Headers) {
//        if (out == null) return
//        when (direction) {
//            Direction.OUTBOUND -> {
//                out.requestData!!.headers = (out.requestData!!.headers ?: emptyList()) +
//                        headers.map { it.key.toString() to it.value.toString() }
//            }
//            Direction.INBOUND -> {
//                out.headers = (out.headers ?: emptyList()) +
//                        headers.map { it.key.toString() to it.value.toString() }
//            }
//        }
//
//    }

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

//        handleHeaders(direction = direction, headers = headers)
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
                data.serialize(
                    if (direction == Direction.OUTBOUND) {
                        outboundDataSerializeCredit
                    } else {
                        inboundDataSerializeCredit
                    }
                )
            }"
        )

        if (direction == Direction.OUTBOUND) {
            log.v {
                outboundTransmitCreditForDebug(streamId).addAndGet(- data.readableBytes())
                outboundTransmitCreditForDebug(0).addAndGet(- data.readableBytes())
                "> HTTP2 Frame DATA ({$streamId} window size = ${outboundTransmitCreditForDebug(streamId)}, conn win s = ${outboundTransmitCreditForDebug(0)})"
            }
        }
    }

    override fun logPushPromise(
        direction: Direction,
        ctx: ChannelHandlerContext,
        streamId: Int,
        promisedStreamId: Int,
        headers: Http2Headers,
        padding: Int
    ) {
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

    fun ByteBuf.serialize(readCredit: AtomicInteger? = null): String {
        val readableBytesCount = readableBytes()
        val bytesCountToRead = if (readCredit != null) {
            synchronized(readCredit) {
                val bytesToRead = minOf(readCredit.get(), readableBytesCount)
                val newCredit = readCredit.addAndGet(- bytesToRead)
                log.v { "new credit = $newCredit" }
                bytesToRead
            }
        } else {
            readableBytesCount
        }
        if (bytesCountToRead <= 0) {
            return ""
        }
        val bytes = ByteArray(bytesCountToRead)
        getBytes(readerIndex(), bytes, 0, bytesCountToRead)
        return bytes.decodeToString() + if (bytesCountToRead < readableBytesCount) {
            " ...(truncated, total size: ${DecimalFormat("#,###").format(readableBytesCount)} bytes)"
        } else {
            ""
        }
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
}

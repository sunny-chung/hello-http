package com.sunnychung.application.multiplatform.hellohttp.network.apache

import org.apache.hc.core5.http2.H2Error
import org.apache.hc.core5.http2.config.H2Param
import org.apache.hc.core5.http2.frame.FrameFlag
import org.apache.hc.core5.http2.frame.FrameType
import org.apache.hc.core5.http2.frame.RawFrame
import org.apache.hc.core5.http2.hpack.HPackInspectHeader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

private const val LONGEST_HEADER_LOG_PREFIX = "NAME INDEXED (123)"
private val HEADER_LOG_PREFIX_SPACES = " ".repeat("[$LONGEST_HEADER_LOG_PREFIX] ".length)

class Http2FrameSerializer {

    fun serializeFrame(frame: RawFrame): String {
        val s = StringBuilder()
        serializeFrameHeader(frame, s)
        s.append('\n')
        if (serializeFramePayload(frame, s)) {
            s.append('\n')
        }
        return s.toString()
    }

    private fun serializeFrameHeader(frame: RawFrame, s: Appendable) {
        s.append("Frame: ")
        val type = FrameType.valueOf(frame.type)
        s.append(type.toString())
            .append(" (0x").append(Integer.toHexString(frame.type)).append("); flags: ")
        val flags = frame.flags
        if (flags > 0) {
            when (type) {
                FrameType.SETTINGS, FrameType.PING -> {
                    if (flags and FrameFlag.ACK.value > 0) {
                        s.append(FrameFlag.ACK.name).append(" ")
                    }
                }

                FrameType.DATA -> {
                    if (flags and FrameFlag.END_STREAM.value > 0) {
                        s.append(FrameFlag.END_STREAM.name).append(" ")
                    }
                }

                FrameType.HEADERS -> {
                    if (flags and FrameFlag.END_STREAM.value > 0) {
                        s.append(FrameFlag.END_STREAM.name).append(" ")
                    }
                    if (flags and FrameFlag.END_HEADERS.value > 0) {
                        s.append(FrameFlag.END_HEADERS.name).append(" ")
                    }
                    if (flags and FrameFlag.PRIORITY.value > 0) {
                        s.append(FrameFlag.PRIORITY.name).append(" ")
                    }
                }

                FrameType.PUSH_PROMISE -> {
                    if (flags and FrameFlag.END_HEADERS.value > 0) {
                        s.append(FrameFlag.END_HEADERS.name).append(" ")
                    }
                }

                FrameType.CONTINUATION -> {
                    if (flags and FrameFlag.END_HEADERS.value > 0) {
                        s.append(FrameFlag.END_HEADERS.name).append(" ")
                    }
                }
                else -> {}
            }
        }
        s.append("(0x").append(Integer.toHexString(flags)).append("); length: ")
            .append(frame.length.toString())
    }

    private fun serializeFramePayload(frame: RawFrame, s: Appendable): Boolean {
        val type = FrameType.valueOf(frame.type)
        val buf = frame.payloadContent
        if (buf != null) {
            when (type) {
                FrameType.HEADERS -> { return false /* intended to skip. decode and print in H2InspectListener */ }

                FrameType.SETTINGS -> if (buf.remaining() % 6 == 0) {
                    while (buf.hasRemaining()) {
                        val code = buf.getShort().toInt()
                        val param = H2Param.valueOf(code)
                        val value = buf.getInt()
                        if (param != null) {
                            s.append(param.name)
                        } else {
                            s.append("0x").append(Integer.toHexString(code))
                        }
                        s.append(": ").append(value.toString()).append("\r\n")
                    }
                } else {
                    s.append("Invalid\r\n")
                }

                FrameType.RST_STREAM -> if (buf.remaining() == 4) {
                    s.append("Code ")
                    val code = buf.getInt()
                    val error = H2Error.getByCode(code)
                    if (error != null) {
                        s.append(error.name)
                    } else {
                        s.append("0x").append(Integer.toHexString(code))
                    }
                    s.append("\r\n")
                } else {
                    s.append("Invalid\r\n")
                }

                FrameType.GOAWAY -> if (buf.remaining() >= 8) {
                    val lastStream = buf.getInt()
                    s.append("Last stream ").append( lastStream.toString()).append("\r\n")
                    s.append("Code ")
                    val code2 = buf.getInt()
                    val error2 = H2Error.getByCode(code2)
                    if (error2 != null) {
                        s.append(error2.name)
                    } else {
                        s.append("0x").append(Integer.toHexString(code2))
                    }
                    s.append("\r\n")
                    val tmp = ByteArray(buf.remaining())
                    buf.get(tmp)
                    s.append(String(tmp, StandardCharsets.US_ASCII))
                    s.append("\r\n")
                } else {
                    s.append("Invalid\r\n")
                }

                FrameType.WINDOW_UPDATE -> if (buf.remaining() == 4) {
                    val increment = buf.getInt()
                    s.append("Increment ").append(increment.toString()).append("\r\n")
                } else {
                    s.append("Invalid\r\n")
                }

                FrameType.PUSH_PROMISE -> if (buf.remaining() > 4) {
                    val streamId = buf.getInt()
                    s.append("Promised stream ").append(streamId.toString()).append("\r\n")
                    printData(buf, s)
                } else {
                    s.append("Invalid\r\n")
                }

                else -> printData(frame.payload, s)
            }
            return true
        }
        return false
    }

    private fun printData(data: ByteBuffer, s: Appendable) {
        s.append(data.array().copyOfRange(data.position(), data.limit()).decodeToString())
    }

    fun serializeHeaders(headers: List<HPackInspectHeader>): String {
        val s = StringBuilder()
        headers.forEach {
            val prefix = when (it.format) {
                HPackInspectHeader.Format.INDEXED -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "INDEXED", "(${it.index})")}] "
                HPackInspectHeader.Format.LITERAL_WITH_INDEXING -> {
                    if (it.index != null) {
                        "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "NAME INDEXED", "(${it.index})")}] "
                    } else {
                        "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "+ NEW INDEX", "")}] "
                    }
                }
                HPackInspectHeader.Format.LITERAL_WITHOUT_INDEXING -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "- NOT INDEXED", "")}] " //HEADER_LOG_PREFIX_SPACES
                HPackInspectHeader.Format.LITERAL_NEVER_INDEXED -> "[${formatHeaderPrefix(LONGEST_HEADER_LOG_PREFIX.length, "SENSITIVE", "")}] "
                HPackInspectHeader.Format.OTHER -> ""
            }
            val content = when (it.name) {
                HPackInspectHeader.PSEUDO_HEADER_KEY_DYNAMIC_TABLE_SIZE_UPDATE -> {
                    "Update dynamic table size to ${it.value}"
                }
                else -> "${it.name}: ${it.value}"
            }
            s.append(prefix).append(content).append('\n')
        }
        return s.toString()
    }

    private fun formatHeaderPrefix(length: Int, begin: String, end: String): String {
        return begin + " ".repeat(maxOf(length - begin.length - end.length, 0)) + end
    }

    fun wrapStreamId(streamId: Int): Int? {
        return if (streamId > 0) streamId else null
    }
}
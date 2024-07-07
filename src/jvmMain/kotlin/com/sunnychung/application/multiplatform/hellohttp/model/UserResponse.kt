package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.extension.endWithNewLine
import com.sunnychung.application.multiplatform.hellohttp.serializer.SynchronizedListSerializer
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.text.DecimalFormat
import java.util.*

const val MAX_CAPTURED_REQUEST_BODY_SIZE = 2 * 1024 * 1024 // 2 MB

@Persisted
@Serializable
data class UserResponse(
    override val id: String,
    val requestId: String, // corresponding id of UserRequest
    val requestExampleId: String, // corresponding id of UserRequestExample
    var protocol: ProtocolVersion? = null,
    var application: ProtocolApplication = ProtocolApplication.Http,

    var startAt: KInstantAsLong? = null,
    var endAt: KInstantAsLong? = null,
    @Transient @Deprecated("Use CallData#status") var isCommunicating: Boolean = false,
    var isError: Boolean = false,
    var statusCode: Int? = null,
    var statusText: String? = null,
    var responseSizeInBytes: Long? = null,
    var body: ByteArray? = null, // original bytes are stored, EXCEPT gRPC
    var errorMessage: String? = null,
    @Transient var postFlightErrorMessage: String? = null,
    var headers: List<Pair<String, String>>? = null,
    var connectionSecurity: ConnectionSecurity? = null,
    var rawExchange: RawExchange = RawExchange(exchanges = Collections.synchronizedList(mutableListOf())),
    @Serializable(with = SynchronizedListSerializer::class) var payloadExchanges: MutableList<PayloadMessage>? =
        // null = not support streaming; empty list = streaming without data
        if (application in setOf(ProtocolApplication.WebSocket, ProtocolApplication.Graphql))
            Collections.synchronizedList(mutableListOf())
        else
            null,
    var requestData: RequestData? = null,
    var closeReason: String? = null,
    @Transient var uiVersion: String = uuidString(),
) : Identifiable {
    override fun equals(other: Any?): Boolean {
        var i = 0
        log.v { "e${i++}" }
        if (this === other) return true
        log.v { "e${i++}" }
        if (javaClass != other?.javaClass) return false

        log.v { "e${i++}" }
        other as UserResponse

        log.v { "e${i++}" }
        if (startAt != other.startAt) return false
        log.v { "e${i++}" }
        if (endAt != other.endAt) return false
        log.v { "e${i++}" }
        if (isCommunicating != other.isCommunicating) return false
        log.v { "e${i++}" }
        if (isError != other.isError) return false
        log.v { "e${i++}" }
        if (statusCode != other.statusCode) return false
        log.v { "e${i++}" }
        if (statusText != other.statusText) return false
        log.v { "e${i++}" }
        if (responseSizeInBytes != other.responseSizeInBytes) return false
        log.v { "e${i++}" } // e10
        if (body != null) {
            if (other.body == null) return false
            if (body !== other.body) return false // modified
        } else if (other.body != null) return false
        log.v { "e${i++}" }
        if (errorMessage != other.errorMessage) return false
        log.v { "e${i++}" }
        if (headers != other.headers) return false
        log.v { "e${i++}" }
        if (rawExchange != other.rawExchange) return false

        log.v { "e${i++}" }
        if (connectionSecurity != other.connectionSecurity) return false
        log.v { "e${i++}" }
        if (payloadExchanges != other.payloadExchanges) return false

        log.v { "e${i++}" }
        return true
    }

    override fun hashCode(): Int {
        var result = startAt?.hashCode() ?: 0
        result = 31 * result + (endAt?.hashCode() ?: 0)
        result = 31 * result + isCommunicating.hashCode()
        result = 31 * result + isError.hashCode()
        result = 31 * result + (statusCode ?: 0)
        result = 31 * result + (statusText?.hashCode() ?: 0)
        result = 31 * result + (responseSizeInBytes?.hashCode() ?: 0)
        result = 31 * result + (body?.hashCode() ?: 0) // modified
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (headers?.hashCode() ?: 0)
        result = 31 * result + rawExchange.hashCode()
        return result
    }

    fun isStreaming() = payloadExchanges != null

    fun contentEquals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserResponse) return false

        if (id != other.id) return false
        if (requestId != other.requestId) return false
        if (requestExampleId != other.requestExampleId) return false
        if (protocol != other.protocol) return false
        if (application != other.application) return false
        if (startAt != other.startAt) return false
        if (endAt != other.endAt) return false
        if (isError != other.isError) return false
        if (statusCode != other.statusCode) return false
        if (statusText != other.statusText) return false
        if (responseSizeInBytes != other.responseSizeInBytes) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false
        if (errorMessage != other.errorMessage) return false
        if (headers != other.headers) return false
        if (connectionSecurity != other.connectionSecurity) return false
        if (rawExchange != other.rawExchange) return false
        if (payloadExchanges != other.payloadExchanges) return false
        if (requestData != other.requestData) return false
        if (closeReason != other.closeReason) return false

        return true
    }
}

@Persisted
@Serializable
class RequestData(
    var method: String? = null,
    var url: String? = null,
    var headers: List<Pair<String, String>>? = null,
    var body: ByteArray? = null,
    var bodySize: Long? = null,
) {
    fun isNotEmpty() = headers != null
}

val TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.lll (Z)"
val BODY_BLOCK_DELIMITER = "`````"

fun UserResponse.hasSomethingToCopy() = !isError && requestData?.isNotEmpty() == true

fun UserResponse.describeApplicationLayer() =
    when {
        payloadExchanges == null -> """
Request
=======
Start Time: ${startAt?.atZoneOffset(KZoneOffset.local())?.format(TIME_FORMAT) ?: "-"}

${protocol?.toString().orEmpty()}

${requestData?.method.orEmpty()} ${requestData?.url.orEmpty()}

Headers:
$BODY_BLOCK_DELIMITER
${
    requestData?.headers?.joinToString("\n") {
        "${
            it.first.toByteArray(Charsets.ISO_8859_1).decodeToString()
        }: ${it.second.toByteArray(Charsets.ISO_8859_1).decodeToString()}"
    }.orEmpty()
}
$BODY_BLOCK_DELIMITER

${
    if (requestData?.body?.isNotEmpty() == true) {
"""Body:
$BODY_BLOCK_DELIMITER
${requestData?.body?.decodeToString().orEmpty()}
$BODY_BLOCK_DELIMITER${
    com.sunnychung.application.multiplatform.hellohttp.util.let(
        requestData?.body,
        requestData?.bodySize
    ) { body, actualSize ->
        if (body.size < actualSize) {
            " ...(truncated, total size: ${DecimalFormat("#,###").format(actualSize)} bytes)"
        } else {
            null
        }
    } ?: ""
}

""" } else ""
}Response
========
${
    if (endAt != null) {
"""Completion Time: ${endAt?.atZoneOffset(KZoneOffset.local())?.format(TIME_FORMAT) ?: "-"}

Duration: ${String.format("%.3f", (endAt!! - startAt!!).millis / 1000.0)}s

Status Code: ${statusCode ?: "-"}${statusText?.let { " $it" } ?: ""}

Headers:
$BODY_BLOCK_DELIMITER
${headers?.joinToString("\n") { "${it.first}: ${it.second}" }.orEmpty()}
$BODY_BLOCK_DELIMITER

${ if (body?.isNotEmpty() == true) {
"""Body:
$BODY_BLOCK_DELIMITER
${body?.decodeToString()?.endWithNewLine().orEmpty()}
$BODY_BLOCK_DELIMITER
"""
} else "" }"""
    } else {
        "Not Available"
    }
}
    """.trim() + "\n"

    payloadExchanges != null -> buildString {
        append("""
Request
=======
Start Time: ${startAt?.atZoneOffset(KZoneOffset.local())?.format(TIME_FORMAT) ?: "-"}

${protocol?.toString().orEmpty()}

${requestData?.method.orEmpty()} ${requestData?.url.orEmpty()}

Headers:
$BODY_BLOCK_DELIMITER
${
            requestData?.headers?.joinToString("\n") {
                "${
                    it.first.toByteArray(Charsets.ISO_8859_1).decodeToString()
                }: ${it.second.toByteArray(Charsets.ISO_8859_1).decodeToString()}"
            }.orEmpty()
        }
$BODY_BLOCK_DELIMITER
        """.trim())

        headers?.takeIf { application == ProtocolApplication.WebSocket }?.let { headers ->
            append("\n\n")
            append("""
Response
========
Status Code: ${statusCode ?: "-"}${statusText?.let { " $it" } ?: ""}

Headers:
$BODY_BLOCK_DELIMITER
${headers?.joinToString("\n") { "${it.first}: ${it.second}" }.orEmpty()}
$BODY_BLOCK_DELIMITER
            """.trim())
        }

        var outgoingCount = 0
        var incomingCount = 0
        payloadExchanges?.forEach {
            if (it.type in setOf(PayloadMessage.Type.IncomingData, PayloadMessage.Type.OutgoingData)) {
                val title = if (it.type == PayloadMessage.Type.IncomingData) {
                    "Incoming #${++incomingCount}"
                } else {
                    "Outgoing #${++outgoingCount}"
                }
                append("\n\n", title, "\n")
                append("=".repeat(title.length), "\n")
                append("Time: ${it.instant.atZoneOffset(KZoneOffset.local()).format(TIME_FORMAT)}\n\n")
                append("Body:\n$BODY_BLOCK_DELIMITER\n")
                append(it.data?.decodeToString()?.endWithNewLine().orEmpty())
                append("$BODY_BLOCK_DELIMITER\n")
            }
        }

        if (endAt != null) {
            append("\n\n", """
End
===
Completion Time: ${endAt?.atZoneOffset(KZoneOffset.local())?.format(TIME_FORMAT) ?: "-"}

Duration: ${String.format("%.3f", (endAt!! - startAt!!).millis / 1000.0)}s
            """.trim())

            // gRPC only
            headers?.takeIf { application != ProtocolApplication.WebSocket }?.let { headers ->
                append("\n\nHeaders:\n$BODY_BLOCK_DELIMITER\n")
                append(headers.joinToString("") { "${it.first}: ${it.second}\n" }.orEmpty())
                append("$BODY_BLOCK_DELIMITER\n")
            }

            closeReason?.let { closeReason ->
                append("\n\n$closeReason")
            }
        }
    }.trim() + "\n"

    else -> throw UnsupportedOperationException()
}

fun UserResponse.describeTransportLayer(isRelativeTimeDisplay: Boolean) = buildString {
    val events = synchronized(rawExchange.exchanges) {
        rawExchange.exchanges.toList()
    }
    val startInstant = events.firstOrNull()?.instant ?: run {
        appendLine("No transportation")
        return@buildString
    }
    val titles = listOf(
        "Time",
        "Dir",
        "Stream",
        "Detail"
    )
    fun KZonedInstant.formatAbsTimeOrRelativeTime(): String =
        if (isRelativeTimeDisplay) {
            (this - startInstant).format("HH:mm:ss.lll")
        } else {
            this.format(TIME_FORMAT)
        }

    val exportedData = events.map {
        listOf(
            it.instant.atZoneOffset(KZoneOffset.local()).formatAbsTimeOrRelativeTime() +
                if (it.lastUpdateInstant != null && it.lastUpdateInstant != it.instant) {
                    " ~ " + it.lastUpdateInstant!!.atZoneOffset(KZoneOffset.local()).formatAbsTimeOrRelativeTime()
                } else {
                    ""
                },
            when (it.direction) {
                RawExchange.Direction.Outgoing -> "Out"
                RawExchange.Direction.Incoming -> "In"
                RawExchange.Direction.Unspecified -> "-"
            },
            it.streamId?.toString() ?: if (protocol?.isHttp2() == true) "*" else "",
            it.detail
                ?: (it.payload ?: it.payloadBuilder?.toByteArray())
                    ?.let { bytes ->
                        val text = bytes.decodeToString()
                        if (bytes.size < (it.payloadSize ?: 0)) {
                            "$text ...(truncated, total size: ${DecimalFormat("#,###").format(it.payloadSize)} bytes)"
                        } else {
                            text
                        }
                    }
                ?: "<Payload Lost>",
        )
    }
    val maxLengths = (0 .. titles.lastIndex).map { i ->
        exportedData.maxOf { it[i].length }
    }
    val columnLengths = maxLengths.mapIndexed { i, it ->
        if (it > 0) {
            maxOf(it, titles[i].length)
        } else {
            0
        }
    }

    // TODO optimize the loops
    val columnIndices = titles.withIndex().filter { (i, _) -> columnLengths[i] > 0 }.map { (i, _) -> i }
    appendLine(titles.withIndex().filter { (i, _) -> columnLengths[i] > 0 }.joinToString(" | ") { (i, s) -> s.padEnd(columnLengths[i], ' ') })
    appendLine(titles.withIndex().filter { (i, _) -> columnLengths[i] > 0 }.joinToString("=|=") { (i, _) -> "".padEnd(columnLengths[i], '=') })
    exportedData.forEach { texts ->
        // assume only the last column can have multiple lines
        append(texts.withIndex().filter { (i, _) -> columnLengths[i] > 0 && i < columnIndices.lastIndex }.joinToString("") { (i, s) -> s.padEnd(columnLengths[i], ' ') + " | " })
        val lines = texts.last().lines()
        if (lines.size <= 1) {
            appendLine(lines.firstOrNull() ?: "")
        } else {
            val linePrefix = titles.withIndex().filter { (ii, _) -> columnLengths[ii] > 0 && ii < columnIndices.lastIndex }.joinToString("") { (i, _) -> "".padEnd(columnLengths[i], ' ') + " | " }
            lines.forEachIndexed { i, line ->
                if (i > 0) append(linePrefix)
                appendLine(line)
            }
        }
    }
}

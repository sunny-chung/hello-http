package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.extension.endWithNewLine
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

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
    var payloadExchanges: MutableList<PayloadMessage>? = // null = not support streaming; empty list = streaming without data
        if (application in setOf(ProtocolApplication.WebSocket, ProtocolApplication.Graphql))
            Collections.synchronizedList(mutableListOf())
        else
            null,
    @Transient var requestData: RequestData? = null,
    @Transient var closeReason: String? = null,
    @Transient var uiVersion: String = uuidString(),
) : Identifiable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserResponse

        if (startAt != other.startAt) return false
        if (endAt != other.endAt) return false
        if (isCommunicating != other.isCommunicating) return false
        if (isError != other.isError) return false
        if (statusCode != other.statusCode) return false
        if (statusText != other.statusText) return false
        if (responseSizeInBytes != other.responseSizeInBytes) return false
        if (body != null) {
            if (other.body == null) return false
            if (body != other.body) return false // modified
        } else if (other.body != null) return false
        if (errorMessage != other.errorMessage) return false
        if (headers != other.headers) return false
        if (rawExchange != other.rawExchange) return false

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
}

data class RequestData(
    var method: String? = null,
    var url: String? = null,
    var headers: List<Pair<String, String>>? = null,
    var body: ByteArray? = null,
) {
    fun isNotEmpty() = headers != null
}

val TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.lll (Z)"

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
```
${
    requestData?.headers?.joinToString("\n") {
        "${
            it.first.toByteArray(Charsets.ISO_8859_1).decodeToString()
        }: ${it.second.toByteArray(Charsets.ISO_8859_1).decodeToString()}"
    }.orEmpty()
}
```

${
    if (requestData?.body?.isNotEmpty() == true) {
"""Body:
```
${requestData?.body?.decodeToString()?.endWithNewLine().orEmpty()}
```

""" } else ""
}Response
========
${
    if (endAt != null) {
"""Completion Time: ${endAt?.atZoneOffset(KZoneOffset.local())?.format(TIME_FORMAT) ?: "-"}

Duration: ${String.format("%.3f", (endAt!! - startAt!!).millis / 1000.0)}s

Status Code: ${statusCode ?: "-"}${statusText?.let { " $it" } ?: ""}

Headers:
```
${headers?.joinToString("\n") { "${it.first}: ${it.second}" }.orEmpty()}
```

${ if (body?.isNotEmpty() == true) {
"""Body:
```
${body?.decodeToString()?.endWithNewLine().orEmpty()}
```
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
```
${
            requestData?.headers?.joinToString("\n") {
                "${
                    it.first.toByteArray(Charsets.ISO_8859_1).decodeToString()
                }: ${it.second.toByteArray(Charsets.ISO_8859_1).decodeToString()}"
            }.orEmpty()
        }
```
        """.trim())

        headers?.takeIf { application == ProtocolApplication.WebSocket }?.let { headers ->
            append("\n\n")
            append("""
Response
========
Status Code: ${statusCode ?: "-"}${statusText?.let { " $it" } ?: ""}

Headers:
```
${headers?.joinToString("\n") { "${it.first}: ${it.second}" }.orEmpty()}
```
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
                append("Body:\n```\n")
                append(it.data?.decodeToString()?.endWithNewLine().orEmpty())
                append("```\n")
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
                append("\n\nHeaders:\n```\n")
                append(headers.joinToString("") { "${it.first}: ${it.second}\n" }.orEmpty())
                append("```\n")
            }

            closeReason?.let { closeReason ->
                append("\n\n$closeReason")
            }
        }
    }.trim() + "\n"

    else -> throw UnsupportedOperationException()
}

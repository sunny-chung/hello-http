package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.KInstantAsLongCompat
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class PayloadMessage(
    val id: String = uuidString(),
    val instant: KInstantAsLongCompat,
    val type: Type,
    val data: ByteArray?, // original bytes are stored, EXCEPT gRPC
) {
    enum class Type {
        IncomingData, OutgoingData, Connected, Disconnected, Error
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PayloadMessage

        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

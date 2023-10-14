package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import java.util.*

@Persisted
@Serializable
data class UserResponse(
    override val id: String,
    val requestId: String, // corresponding id of UserRequest

    var startAt: KInstantAsLong? = null,
    var endAt: KInstantAsLong? = null,
    var isCommunicating: Boolean = false,
    var isError: Boolean = false,
    var statusCode: Int? = null,
    var statusText: String? = null,
    var responseSizeInBytes: Long? = null,
    var body: ByteArray? = null,
    var errorMessage: String? = null,
    var headers: List<Pair<String, String>>? = null,
    var rawExchange: RawExchange = RawExchange(exchanges = Collections.synchronizedList(mutableListOf())),
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

}

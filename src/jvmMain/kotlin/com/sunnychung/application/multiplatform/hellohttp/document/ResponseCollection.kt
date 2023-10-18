package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@DocumentRoot
@Persisted
@Serializable
data class ResponseCollection(
    override val id: ResponsesDI,
    @SerialName("responses") private var _responsesByRequestExampleId: MutableMap<String, UserResponse>
) : Document<ResponsesDI> {

    init {
        _responsesByRequestExampleId = ConcurrentHashMap<String, UserResponse>(_responsesByRequestExampleId.size).apply {
            putAll(_responsesByRequestExampleId)
        }
    }

    val responsesByRequestExampleId: MutableMap<String, UserResponse>
        get() = _responsesByRequestExampleId
}

@Serializable
@Persisted
class ResponsesDI(val subprojectId: String) : DocumentIdentifier(type = PersistenceDocumentType.Responses) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResponsesDI
        return subprojectId == other.subprojectId
    }

    override fun hashCode(): Int {
        return subprojectId.hashCode()
    }

    override fun toString(): String {
        return "ResponsesDI(subprojectId='$subprojectId')"
    }
}

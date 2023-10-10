package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import kotlinx.serialization.Serializable

@DocumentRoot
@Persisted
@Serializable
data class RequestCollection(override val id: RequestsDI, val requests: MutableList<UserRequest>) : Document<RequestsDI>

@Persisted
@Serializable
class RequestsDI(val subprojectId: String) : DocumentIdentifier(type = PersistenceDocumentType.Requests) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RequestsDI
        return subprojectId == other.subprojectId
    }

    override fun hashCode(): Int {
        return subprojectId.hashCode()
    }

    override fun toString(): String {
        return "RequestsDI(subprojectId='$subprojectId')"
    }
}

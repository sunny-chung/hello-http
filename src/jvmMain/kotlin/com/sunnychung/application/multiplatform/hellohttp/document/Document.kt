package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

interface Document<ID : DocumentIdentifier> {
    val id: ID
}

@Serializable
@Persisted
sealed class DocumentIdentifier(val type: PersistenceDocumentType)

enum class PersistenceDocumentType {
    ProjectAndEnvironments, Requests, Responses
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

package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

interface Document<ID : DocumentIdentifier> {
    val id: ID
}

interface Identifiable {
    val id: String
}

@Serializable
@Persisted
sealed class DocumentIdentifier(val type: PersistenceDocumentType)

enum class PersistenceDocumentType {
    ProjectAndEnvironments, Requests, Responses, UserPreference
}

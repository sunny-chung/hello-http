package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.UserPreference
import kotlinx.serialization.Serializable

@DocumentRoot
@Persisted
@Serializable
data class UserPreferenceDocument(override val id: UserPreferenceDI, var preference: UserPreference) : Document<UserPreferenceDI>

@Persisted
@Serializable
class UserPreferenceDI : DocumentIdentifier(type = PersistenceDocumentType.UserPreference) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "UserPreferenceDI()"
    }
}

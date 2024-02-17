package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.OperationalInfo
import kotlinx.serialization.Serializable

@DocumentRoot
@Persisted
@Serializable
data class OperationalDocument(override val id: OperationalDI, var data: OperationalInfo) : Document<OperationalDI>

@Persisted
@Serializable
class OperationalDI : DocumentIdentifier(type = PersistenceDocumentType.Operational) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "OperationalDI()"
    }
}

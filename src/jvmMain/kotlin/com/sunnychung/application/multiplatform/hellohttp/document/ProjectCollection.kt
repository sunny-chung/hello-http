package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import kotlinx.serialization.Serializable

@DocumentRoot
@Persisted
@Serializable
data class ProjectCollection(override val id: ProjectAndEnvironmentsDI, val projects: MutableList<Project>) : Document<ProjectAndEnvironmentsDI>

@Persisted
@Serializable
class ProjectAndEnvironmentsDI : DocumentIdentifier(type = PersistenceDocumentType.ProjectAndEnvironments) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "ProjectAndEnvironmentsDI()"
    }
}

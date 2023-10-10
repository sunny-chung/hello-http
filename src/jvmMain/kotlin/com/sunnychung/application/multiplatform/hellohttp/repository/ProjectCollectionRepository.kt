package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectCollection
import kotlinx.serialization.serializer

class ProjectCollectionRepository : BaseCollectionRepository<ProjectCollection, ProjectAndEnvironmentsDI>(serializer()) {
    override fun relativeFilePath(id: ProjectAndEnvironmentsDI): String = "projects.db"
}

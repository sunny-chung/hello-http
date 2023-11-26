package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectCollection
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer

class ProjectCollectionRepository : BaseCollectionRepository<ProjectCollection, ProjectAndEnvironmentsDI>(serializer()) {

    private val publishNonPersistedSubprojectUpdates = MutableSharedFlow<Pair<String, String>>()

    override fun relativeFilePath(id: ProjectAndEnvironmentsDI): String = "projects.db"

    fun updateSubproject(di: ProjectAndEnvironmentsDI, update: Subproject) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            notifyUpdated(di)
            publishNonPersistedSubprojectUpdates.emit(Pair(update.id, uuidString()))
        }
    }

    suspend fun readSubproject(di: ProjectAndEnvironmentsDI, subprojectId: String): Subproject {
        fun find(projects: List<Project>): Subproject { // O(n*m)
            for (it in projects) {
                val subp = it.subprojects.firstOrNull {
                    it.id == subprojectId
                }
                if (subp != null) {
                    return subp
                }
            }
            throw NoSuchElementException()
        }
        return find(read(di)!!.projects)
    }

    fun subscribeLatestSubproject(di: ProjectAndEnvironmentsDI, subprojectId: String): Flow<Pair<Subproject?, String>> = publishNonPersistedSubprojectUpdates
        .onSubscription {
            emit(Pair(subprojectId, "onSub"))
        }
        .filter { it.first == subprojectId }
        .map {
            log.d { "re-read subproject" }
            readSubproject(di, subprojectId) to it.second
        }
}

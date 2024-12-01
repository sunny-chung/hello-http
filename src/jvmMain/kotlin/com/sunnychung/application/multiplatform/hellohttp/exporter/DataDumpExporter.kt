package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.DataDump
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class DataDumpExporter {
    val schemaVersion = 1

    private val codec = Cbor {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToFile(projects: List<Project>, file: File) {
        run {
            val subprojects = projects.flatMap { it.subprojects }
            val requests = subprojects
                .mapNotNull {
                    val id = RequestsDI(subprojectId = it.id)
                    val doc = AppContext.RequestCollectionRepository.read(id, isKeepInCache = false) ?: return@mapNotNull null
                    Pair(id, doc.requests)
                }
            val responses = subprojects
                .mapNotNull {
                    val id = ResponsesDI(subprojectId = it.id)
                    val doc = AppContext.ResponseCollectionRepository.read(id, isKeepInCache = false) ?: return@mapNotNull null
                    Pair(id, doc.responsesByRequestExampleId)
                }
            val apiSpecs = projects
                .mapNotNull {
                    val id = ApiSpecDI(projectId = it.id)
                    val doc = AppContext.ApiSpecificationCollectionRepository.read(id, isKeepInCache = false) ?: return@mapNotNull null
                    Pair(id, doc)
                }

            val dump = DataDump(
                schemaVersion = schemaVersion,
                createdAt = KInstant.now(),
                data = DataDump.Data(
                    projects = projects,
                    requests = requests,
                    responses = responses,
                    apiSpecs = apiSpecs,
                ),
            )
            val bytes = codec.encodeToByteArray(dump)

            // use FileManager for file locking, because export can be done both manually and by schedule
            // not using FileManager's schema because the dump is supposed to be read by any application
            AppContext.FileManager.withLock(file) {
                file.writeBytes(bytes)
            }
        }

        System.gc()
    }
}

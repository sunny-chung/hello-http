package com.sunnychung.application.multiplatform.hellohttp.importer

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.DataDump
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.error.UnsupportedFileSchemaVersionError
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class DataDumpImporter {
    val schemaVersion = 1

    private val codec = Cbor {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun importAsProjects(file: File) {
        val bytes = file.readBytes()
        val dump = codec.decodeFromByteArray<DataDump>(bytes)
        if (dump.schemaVersion != schemaVersion) {
            throw UnsupportedFileSchemaVersionError()
        }
        val data = dump.data

        val requestCollectionRepository = AppContext.RequestCollectionRepository
        val responseCollectionRepository = AppContext.ResponseCollectionRepository
        val projectCollectionRepository = AppContext.ProjectCollectionRepository
        val apiSpecificationCollectionRepository = AppContext.ApiSpecificationCollectionRepository

        // IMPORTANT to import old data with new ID, otherwise existing data would be overwritten
        // except pair ID, it can be duplicated among Request Examples (but not within a Request Example)
        val projectIds = mutableMapOf<String, String>() // (old id -> new id)
        val newSubprojectIds = mutableMapOf<String, String>() // (old id -> new id)
        val newRequestTemplateIds = mutableMapOf<String, String>() // (old id -> new id)
        val newRequestExampleIds = mutableMapOf<String, String>() // (old id -> new id)
        val newApiSpecIds = mutableMapOf<String, String>() // (old id -> new id)
        data.projects.forEach {
            projectIds[it.id] = uuidString()
        }
        data.projects.flatMap { it.subprojects }.forEach {
            newSubprojectIds[it.id] = uuidString()
        }
        data.apiSpecs.flatMap { it.second.grpcApiSpecs }.forEach {
            newApiSpecIds[it.id] = uuidString()
        }

        data.requests.forEach { (id, requests) ->
            val newDI = newSubprojectIds[id.subprojectId]?.let { RequestsDI(it) }
            newDI?.let { newDI ->
                requestCollectionRepository.readOrCreate(newDI) {
                    RequestCollection(id = newDI, requests = requests.map {
                        val newId = uuidString()
                        newRequestTemplateIds[it.id] = newId
                        it.copy(
                            id = newId,
                            examples = it.examples.map {
                                val newId = uuidString()
                                newRequestExampleIds[it.id] = newId
                                it.copy(id = newId)
                            },
                            grpc = it.grpc?.let {
                                it.copy(apiSpecId = newApiSpecIds[it.apiSpecId] ?: it.apiSpecId)
                            }
                        )
                    }.toMutableList())
                }
            }
        }
        data.responses.forEach { (id, responses) ->
            val newDI = newSubprojectIds[id.subprojectId]?.let { ResponsesDI(it) }
            newDI?.let { newDI ->
                responseCollectionRepository.readOrCreate(newDI) {
                    ResponseCollection(id = newDI, _responsesByRequestExampleId = responses.mapNotNull { entry ->
                        newRequestExampleIds[entry.key]?.let { newExampleId ->
                            newExampleId to entry.value.copy(
                                id = uuidString(),
                            )
                        }
                    }.toMap().toMutableMap())
                }
            }
        }
        data.apiSpecs.forEach { (id, spec) ->
            val newDI = projectIds[id.projectId]?.let { ApiSpecDI(it) } ?: return@forEach
            apiSpecificationCollectionRepository.readOrCreate(newDI) {
                ApiSpecCollection(
                    id = newDI,
                    _grpcApiSpecs = spec.grpcApiSpecs.map {
                        it.copy(id = newApiSpecIds[it.id]!!)
                    }.toMutableList()
                )
            }
        }

        fun copyWithNewId(current: TreeObject): TreeObject {
            return when (current) {
                is TreeFolder -> current.copy(
                    id = uuidString(),
                    childs = current.childs.map { copyWithNewId(it) }.toMutableList(),
                )
                is TreeRequest -> current.copy(
                    id = newRequestTemplateIds[current.id]!!
                )
            }
        }

        projectCollectionRepository.readOrCreate(ProjectAndEnvironmentsDI()) {
            throw IllegalStateException() // should not happen
        }.projects += data.projects.map {
            it.copy(
                id = projectIds[it.id]!!,
                subprojects = it.subprojects.map {
                    it.copy(
                        id = newSubprojectIds[it.id]!!,
                        environments = it.environments.map {
                            it.copy(id = uuidString())
                        }.toMutableList(),
                        treeObjects = it.treeObjects.map { copyWithNewId(it) }.toMutableList(),
                        grpcApiSpecIds = it.grpcApiSpecIds.mapNotNull { newApiSpecIds[it] }.toMutableSet(),
                    )
                }.toMutableList()
            )
        }
        projectCollectionRepository.notifyUpdated(ProjectAndEnvironmentsDI())
    }
}

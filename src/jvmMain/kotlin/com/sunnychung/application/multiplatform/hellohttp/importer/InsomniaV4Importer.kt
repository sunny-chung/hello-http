package com.sunnychung.application.multiplatform.hellohttp.importer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.error.UnsupportedFileSchemaVersionError
import com.sunnychung.application.multiplatform.hellohttp.extension.`if`
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.insomniav4.InsomniaV4
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import java.io.File

class InsomniaV4Importer {

    suspend fun importAsProject(file: File, projectName: String) {
        val jsonParser = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val root = jsonParser.readTree(file)
        if (
            root["_type"]?.textValue() != "export"
            || root["__export_format"]?.intValue() != 4
            || root["resources"]?.isArray != true
            ) {
            throw UnsupportedFileSchemaVersionError()
        }
        val resources = root["resources"].toList()

        val subprojectMap = mutableMapOf<String, Subproject>() // key is _id in the file
        val environmentMap = mutableMapOf<String, Pair<Environment, Subproject?>>() // key is _id in the file
        val folderMap = mutableMapOf<String, Pair<TreeFolder, Subproject?>>() // key is _id in the file

        val subprojects = mutableListOf<Subproject>()
        val requestsBySubproject = mutableMapOf<String, MutableList<UserRequest>>()

        resources.filter { it["_type"]?.textValue() == "workspace" && it["scope"]?.textValue() == "collection" }
            .map { jsonParser.treeToValue(it, InsomniaV4.Workspace::class.java) }
            .forEach {
                val subproject = Subproject(
                    id = uuidString(),
                    name = it.name,
                    treeObjects = mutableListOf(),
                    environments = mutableListOf(),
                )
                subprojects += subproject
                subprojectMap[it.id] = subproject
            }

        resources.filter { it["_type"]?.textValue() == "environment" }
            .map { jsonParser.treeToValue(it, InsomniaV4.Environment::class.java) }
            .forEach { // assume parent env must appear before child env
                var (parentEnv, subproject) = environmentMap[it.parentId] ?: Pair(null, null)

                val env = Environment(
                    id = uuidString(),
                    name = it.name,
                    variables = ((parentEnv?.variables ?: emptyList()) +
                        it.data.fields().asSequence().map {
                            UserKeyValuePair(
                                id = uuidString(),
                                key = it.key,
                                value = it.value.stringify(),
                                valueType = FieldValueType.String,
                                isEnabled = true,
                            )
                        }).toMutableList()
                )
                if (parentEnv == null) {
                    subproject = subprojectMap[it.parentId]
                }
                subproject?.environments?.add(env)
                environmentMap[it.id] = Pair(env, subproject)
            }

        val insomniaGroups = resources.filter { it["_type"]?.textValue() == "request_group" }
            .map { jsonParser.treeToValue(it, InsomniaV4.RequestGroup::class.java) }
            .associateBy { it.id }

        fun processFolder(current: InsomniaV4.RequestGroup): Pair<TreeFolder, Subproject?> {
            var (parent, subproject) = folderMap[current.parentId] ?: insomniaGroups[current.parentId]?.let { processFolder(it) } ?: Pair(null, null)

            val thisFolder = TreeFolder(
                id = uuidString(),
                name = current.name,
                childs = mutableListOf(),
            )

            if (parent != null) {
                parent.childs += thisFolder
            } else {
                subproject = subprojectMap[current.parentId]
                subproject?.treeObjects?.add(thisFolder)
            }
            folderMap[current.id] = Pair(thisFolder, subproject)

            return folderMap[current.id]!!
        }

        insomniaGroups.values.forEach {
            processFolder(it)
        }

        resources.filter { it["_type"]?.textValue() == "request" }
            .map { log.d { it.toString() }; jsonParser.treeToValue(it, InsomniaV4.HttpRequest::class.java) }
            .forEach {
                val req = UserRequest(
                    id = uuidString(),
                    name = it.name,
                    protocol = Protocol.Http,
                    method = it.method,
                    url = it.url.convertVariables(),
                    examples = listOf(UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = when (it.body.mimeType) {
                            null -> ContentType.None
                            "application/json" -> ContentType.Json
                            // TODO multipart
                            // TODO form urlencoded
                            else -> ContentType.Raw
                        },
                        body = when (it.body.mimeType) {
                            null -> null
                            "application/json" -> StringBody(it.body.text?.convertVariables() ?: "")
                            // TODO multipart
                            // TODO form urlencoded
                            else -> StringBody(it.body.text?.convertVariables() ?: "")
                        },
                        headers = it.headers.map { UserKeyValuePair(
                            id = uuidString(),
                            key = it.name.convertVariables(),
                            value = it.value.convertVariables(),
                            valueType = FieldValueType.String, // TODO file
                            isEnabled = it.disabled != true,
                        ) } + (it.authentication.`if` { it.type == "bearer" }?.let { listOf(UserKeyValuePair(
                            id = uuidString(),
                            key = "Authorization",
                            value = "${it.prefix?.convertVariables() ?: "Bearer"} ${it.token?.convertVariables()}",
                            valueType = FieldValueType.String,
                            isEnabled = it.disabled != true,
                        )) } ?: emptyList()),
                        queryParameters = it.parameters.map {
                            UserKeyValuePair(
                                id = uuidString(),
                                key = it.name.convertVariables(),
                                value = it.value.convertVariables(),
                                valueType = FieldValueType.String,
                                isEnabled = it.disabled != true,
                            )
                        },
                    ))
                )
                val parent = folderMap[it.parentId]
                val subproject: Subproject?
                if (parent != null) {
                    parent.first.childs += TreeRequest(id = req.id)
                    subproject = parent.second
                } else {
                    subproject = subprojectMap[it.parentId]
                    subproject?.treeObjects?.add(TreeRequest(id = req.id))
                }
                requestsBySubproject.getOrPut(subproject?.id ?: "") { mutableListOf() } += req
            }

        val requestCollectionRepository = AppContext.RequestCollectionRepository
        val projectCollectionRepository = AppContext.ProjectCollectionRepository

        requestsBySubproject.filter { it.key.isNotEmpty() }
            .forEach {
                requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = it.key)) { id ->
                    RequestCollection(id = id, requests = it.value)
                }
            }

        projectCollectionRepository.readOrCreate(ProjectAndEnvironmentsDI()) {
            throw IllegalStateException() // should not happen
        }.projects += Project(
            id = uuidString(),
            name = projectName,
            subprojects = subprojects
        )

        projectCollectionRepository.notifyUpdated(ProjectAndEnvironmentsDI())
    }

    fun String.convertVariables(): String {
        return this.replace("\\{\\{([^{}]+)\\}\\}".toRegex(), "\\\${{\$1}}")
            .replace("\\{% variable '([^{}%']*)' %\\}".toRegex(), "\\\${{\$1}}")
    }

    fun JsonNode.stringify(): String {
        return if (isValueNode) {
            asText() ?: toString()
        } else {
            toString()
        }
    }
}

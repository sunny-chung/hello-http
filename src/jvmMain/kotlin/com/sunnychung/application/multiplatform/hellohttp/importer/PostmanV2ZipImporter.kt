package com.sunnychung.application.multiplatform.hellohttp.importer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.postmanv2.PostmanV2
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PostmanV2ZipImporter {

    suspend fun importAsProjects(file: File) {
        val jsonParser = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val postmanEnvironments = mutableListOf<PostmanV2.Environment>()

        // read all environments first, then clone (important!) and add them to all projects

        readZip(file) { ze, zis ->
            log.d { "ZipEntry ${ze.name}"}
            if (ze.name.contains(File.separator + "environment" + File.separator) && ze.name.endsWith(".json", ignoreCase = true)) {
                val bytes = zis.readAllBytes()
                postmanEnvironments += jsonParser.readValue(bytes, PostmanV2.Environment::class.java)
            }
        }

        readZip(file) { ze, zis ->
            if (ze.name.contains(File.separator + "collection" + File.separator) && ze.name.endsWith(".json", ignoreCase = true)) {
                val bytes = zis.readAllBytes()
                val collection = jsonParser.readValue(bytes, PostmanV2.Collection::class.java)

                importCollection(collection, postmanEnvironments)
            }
        }

    }

    suspend fun importCollection(collection: PostmanV2.Collection, postmanEnvironments: List<PostmanV2.Environment>) {
        val (requests, newProject) = parseCollection(collection, postmanEnvironments)

        val requestCollectionRepository = AppContext.RequestCollectionRepository
        val projectCollectionRepository = AppContext.ProjectCollectionRepository

        requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = newProject.subprojects.first().id)) { id ->
            RequestCollection(id = id, requests = requests)
        }

        projectCollectionRepository.readOrCreate(ProjectAndEnvironmentsDI()) {
            throw IllegalStateException() // should not happen
        }.projects += newProject

        projectCollectionRepository.notifyUpdated(ProjectAndEnvironmentsDI())
    }

    fun parseCollection(collection: PostmanV2.Collection, postmanEnvironments: List<PostmanV2.Environment>): Pair<MutableList<UserRequestTemplate>, Project> {
        val collectionName = collection.info.name

        val auths = mutableMapOf<String, PostmanV2.Auth>()
        val requests = mutableListOf<UserRequestTemplate>()
        /**
         * The processing order is deterministic for auth inheritance to be processed correctly
         */
        fun processItem(current: PostmanV2.Item): TreeObject {
            if (current.item != null) { // an item group
                // only supports "bearer" now
                val authId = if (current.auth != null && current.auth.type in setOf("bearer")) {
                    val tempId = uuidString()
                    auths[tempId] = current.auth
                    tempId
                } else null
                val domainObject = TreeFolder(
                    id = uuidString(),
                    name = current.name,
                    childs = current.item.map { processItem(it) }.toMutableList(),
                )
                authId?.let { auths.remove(it) }
                return domainObject
            } else {
                val domainRequest = current.toUserRequest(auths.values)
                requests += domainRequest

                return TreeRequest(domainRequest.id)
            }
        }

        val newProject = Project(
            id = uuidString(),
            name = collectionName,

            subprojects = mutableListOf(
                Subproject(
                    id = uuidString(),
                    name = collectionName,
                    treeObjects = collection.item.map { processItem(it) }.toMutableList(),
                    environments = postmanEnvironments.map { it.toAppEnvironment() }.toMutableList(),
                )
            )
        )

        return Pair(requests, newProject)
    }

    private suspend fun readZip(file: File, operation: suspend (ze: ZipEntry, zis: ZipInputStream) -> Unit) {
        file.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var ze = zis.nextEntry
                while (ze != null) {
                    operation(ze, zis)
                    ze = zis.nextEntry
                }
                zis.closeEntry()
            }
        }

    }

    fun PostmanV2.Environment.toAppEnvironment() =
        Environment(
            id = uuidString(),
            name = name,
            variables = values.map { it.toUserKeyValuePair() }.toMutableList()
        )

    fun String.convertVariables(): String {
        return this.replace("\\{\\{([^{}]+)\\}\\}".toRegex(), "\\\${{\$1}}")
    }

    fun PostmanV2.KeyValue.toUserKeyValuePair() = UserKeyValuePair(
        id = uuidString(),
        key = key.convertVariables(),
        value = (value ?: src)?.convertVariables() ?: "",
        valueType = if (type.equals("file", ignoreCase = true)) FieldValueType.File else FieldValueType.String,
        isEnabled = disabled != true,
    )

    fun PostmanV2.EnvKeyValue.toUserKeyValuePair() = UserKeyValuePair(
        id = uuidString(),
        key = key.convertVariables(),
        value = value?.convertVariables() ?: "",
        valueType = FieldValueType.String,
        isEnabled = enabled != false,
    )

    fun PostmanV2.Auth.toUserKeyValuePair(): UserKeyValuePair? {
        if (type != "bearer") return null
        return bearer?.map {
            it.toUserKeyValuePair().copy(
                key = "Authorization",
                value = "Bearer ${it.value}"
            )
        }?.firstOrNull()
    }

    fun PostmanV2.Item.toUserRequest(inheritedAuths: Collection<PostmanV2.Auth>): UserRequestTemplate {
        request!!
        val headers: List<UserKeyValuePair> = request.header.map { it.toUserKeyValuePair() } +
                inheritedAuths.mapNotNull { it.toUserKeyValuePair() } +
                listOfNotNull(request.auth?.toUserKeyValuePair())
        return UserRequestTemplate(
            id = uuidString(),
            name = name,
            method = request.method,
            url = request.url?.raw?.convertVariables() ?: "",
            application = if (request.body?.mode == "graphql") ProtocolApplication.Graphql else ProtocolApplication.Http,
            examples = listOf(UserRequestExample(
                id = uuidString(),
                name = "Base",
                headers = headers,
                queryParameters = request.url?.query?.map { it.toUserKeyValuePair() } ?: emptyList(),
                contentType = when (request.body?.mode) {
                    null -> ContentType.None
                    "formdata" -> ContentType.Multipart
                    "urlencoded" -> ContentType.FormUrlEncoded
                    "file" -> ContentType.BinaryFile
                    "graphql" -> ContentType.Graphql
                    "raw" -> if (headers.firstOrNull {
                            it.key.equals(
                                "content-type",
                                ignoreCase = true
                            )
                        }?.value?.contains("json") == true
                        || request.body.options?.raw?.language == "json"
                    ) {
                        ContentType.Json
                    } else {
                        ContentType.Raw
                    }
                    else -> ContentType.Raw
                },
                body = with (request.body) {
                    when (this?.mode) {
                        null -> null
                        "formdata" -> MultipartBody(
                            formdata?.map { it.toUserKeyValuePair() } ?: emptyList()
                        )

                        "urlencoded" -> FormUrlEncodedBody(
                            urlencoded?.map { it.toUserKeyValuePair() } ?: emptyList()
                        )

                        "file" -> FileBody(file?.src)
                        "graphql" -> GraphqlBody(
                            document = graphql?.query?.convertVariables() ?: "",
                            variables = graphql?.variables?.convertVariables() ?: "",
                            operationName = null
                        )

                        "raw" -> StringBody(raw?.convertVariables() ?: "")
                        else -> StringBody(raw?.convertVariables() ?: "")
                    }
                }
            ))
        )
    }
}

package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
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
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.postmanv2.PostmanV2
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import java.io.File

/**
 * Exports each project as one collection and each subproject environment as one environment.
 * Subprojects are exported as first-level folders under each project collection.
 */
class PostmanV2MultiFileExporter {
    private val jsonWriter = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    suspend fun exportToFile(projects: List<Project>, directory: File) {
        // cache all export data in memory and output only if all processing succeed
        // the memory needed should not be high

        val exports = projects.flatMap { prepareProjectExport(it) }
        val archiveData = PostmanV2.Archive(
            environment = exports.filterIsInstance<PostmanV2.Environment>().associate { it.id to true },
            collection = exports.filterIsInstance<PostmanV2.Collection>().associate { it.info.id to true },
        )

        val dateTimeString = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
        val contentRootDir = File(directory, "HelloHTTP_export-to-postman_$dateTimeString")
        exports.forEach {
            val file = when (it) {
                is PostmanV2.Collection -> File(File(contentRootDir, "collection"), "${it.info.id}.json")
                is PostmanV2.Environment -> File(File(contentRootDir, "environment"), "${it.id}.json")
                else -> throw UnsupportedOperationException()
            }
            file.parentFile.mkdirs()
            jsonWriter.writeValue(file, it)
        }
        jsonWriter.writeValue(File(contentRootDir, "archive.json"), archiveData)
    }

    suspend fun prepareProjectExport(project: Project): List<PostmanV2.File> {
        val subprojectExports = project.subprojects.map { prepareSubprojectExport(project, it) }

        return listOf(
            PostmanV2.Collection(
                info = PostmanV2.FileInfo(
                    id = project.id,
                    name = project.name,
                    schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                ),
                item = subprojectExports.map { it.first },
            )
        ) + subprojectExports.flatMap { it.second }
    }

    suspend fun prepareSubprojectExport(project: Project, subproject: Subproject): Pair<PostmanV2.Item, List<PostmanV2.Environment>> {
        val requests = AppContext.RequestCollectionRepository.read(RequestsDI(subprojectId = subproject.id))
            ?.requests
            ?.associateBy { it.id }
            ?: emptyMap()

        fun TreeObject.toPostmanItem() : List<PostmanV2.Item> {
            return when (this) {
                is TreeFolder -> listOf(PostmanV2.Item(
                    id = id,
                    name = name,
                    item = childs.flatMap { it.toPostmanItem() }
                ))
                is TreeRequest -> requests[id]?.let { requestToItem(it) } ?: emptyList()
            }
        }

        val itemData = PostmanV2.Item(
            id = subproject.id,
            name = subproject.name,
            item = subproject.treeObjects.flatMap { it.toPostmanItem() },
        )

        val environments = subproject.environments.map {
            PostmanV2.Environment(
                id = it.id,
                name = "${project.name}/${subproject.name} - ${it.name}",
                values = it.variables.map {
                    PostmanV2.EnvKeyValue(
                        key = it.key,
                        value = it.value,
                        enabled = it.isEnabled,
                        type = "default",
                    )
                }
            )
        }

        return Pair(itemData, environments)
    }

    fun requestToItem(req: UserRequestTemplate): List<PostmanV2.Item> {
        if (req.application in setOf(ProtocolApplication.WebSocket, ProtocolApplication.Grpc)) {
            return emptyList() // Postman doesn't import them. Skip.
        }

        return req.examples.mapIndexed { index, it ->
            val uri = "(?:([^:/]*)://)?([^/:]*)(?::([0-9]{1,5}))?/?([^?]*).*".toRegex().matchEntire(req.url.resolveVariables())!!
//            fun String.decodeUrl() = URLDecoder.decode(this, StandardCharsets.UTF_8)

            PostmanV2.Item(
                id = it.id,
                name = req.name + if (index > 0) " (${it.name})" else "",
                request = PostmanV2.Request(
                    method = req.method.emptyToNull() ?: "POST",
                    url = PostmanV2.Url(
                        raw = req.url.resolveVariables(),
                        protocol = uri.groupValues[1].emptyToNull(),
                        host = uri.groupValues[2].emptyToNull()?.split('.'),
                        port = uri.groupValues[3].emptyToNull(),
                        path = uri.groupValues[4].emptyToNull()?.split('/'), //?.split('/')?.map { it.decodeUrl() },

                        query = req.getMergedProperty(index) { it.queryParameters }
                            .map { it.toPostmanKeyValue() },
                    ),
                    header = req.getMergedProperty(index) { it.headers }
                        .map { it.toPostmanKeyValue() },
                    auth = null,
                    body = when (it.body) {
                        is FileBody -> PostmanV2.Body(
                            mode = "file",
                            file = PostmanV2.Body.File(it.body.filePath),
                        )
                        is FormUrlEncodedBody -> PostmanV2.Body(
                            mode = "urlencoded",
                            urlencoded = it.body.value.map { it.toPostmanKeyValue() },
                        )
                        is GraphqlBody -> PostmanV2.Body(
                            mode = "graphql",
                            graphql = PostmanV2.Body.Graphql(
                                query = it.body.document.resolveVariables(),
                                variables = it.body.variables.resolveVariables(),
                            )
                        )
                        is MultipartBody -> PostmanV2.Body(
                            mode = "formdata",
                            formdata = it.body.value.map { it.toPostmanKeyValue() },
                        )

                        is StringBody -> PostmanV2.Body(
                            mode = "raw",
                            raw = it.body.value.resolveVariables(),
                        )
                        null -> null
                    },
                )
            )
        }
    }

    fun String.resolveVariables(): String {
        return replace("\\\$\\{\\{([^{}]+)\\}\\}".toRegex(), "{{\$1}}")
    }

    fun UserRequestTemplate.getMergedProperty(exampleIndex: Int, getter: (UserRequestExample) -> List<UserKeyValuePair>?): List<UserKeyValuePair> {
        val baseExample = examples.first()
        if (exampleIndex == 0) {
            return getter(baseExample) ?: emptyList()
        }
        return (getter(baseExample) ?: emptyList()) + (getter(examples[exampleIndex]) ?: emptyList())
    }

    fun UserKeyValuePair.toPostmanKeyValue() : PostmanV2.KeyValue =
        PostmanV2.KeyValue(
            key = key.resolveVariables(),
            value = if (valueType == FieldValueType.String) value.resolveVariables() else null,
            type = when (valueType) {
                FieldValueType.String -> "text"
                FieldValueType.File -> "file"
            },
            disabled = !isEnabled,
            src = if (valueType == FieldValueType.File) value else null,
        )

}

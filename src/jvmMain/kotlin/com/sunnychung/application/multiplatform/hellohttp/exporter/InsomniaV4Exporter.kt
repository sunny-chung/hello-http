package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlRequestBody
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
import com.sunnychung.application.multiplatform.hellohttp.model.insomniav4.InsomniaV4
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.io.File

class InsomniaV4Exporter {

    private val jsonWriter = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    suspend fun exportToFile(project: Project, file: File) {
        val resources = project.subprojects.flatMap {
            exportSubproject(it)
        }

        val metadataManager = AppContext.MetadataManager
        val document = InsomniaV4.Dump(
            type = "export",
            exportFormat = 4,
            exportDate = KDateTimeFormat.ISO8601_DATETIME.format(KInstant.now()),
            exportSource = "sunnychung.hellohttp.desktop:v${metadataManager.version}:${metadataManager.gitCommitHash}",
            resources = resources,
        )
        jsonWriter.writeValue(file, document)
    }

    suspend fun exportSubproject(subproject: Subproject): List<Any> {
        var result = mutableListOf<Any>()
        val parentIdByRequestId = mutableMapOf<String, String>() // (original id, converted parentId)

        result += InsomniaV4.Workspace(
            id = subproject.id.convertId("wrk_"),
            parentId = null,
            name = subproject.name,
            description = "",
            scope = "collection",
            type = "workspace",
        )

        fun transverse(convertedParentId: String, current: TreeObject) {
            when (current) {
                is TreeFolder -> {
                    val currentId = current.id.convertId("fld_")
                    result += InsomniaV4.RequestGroup(
                        id = currentId,
                        parentId = convertedParentId,
                        name = current.name,
                        type = "request_group",
                    )
                    current.childs.forEach {
                        transverse(
                            convertedParentId = currentId,
                            current = it,
                        )
                    }
                }
                is TreeRequest -> {
                    parentIdByRequestId[current.id] = convertedParentId
                }
            }
        }

        subproject.treeObjects.forEach {
            transverse(
                convertedParentId = subproject.id.convertId("wrk_"),
                current = it,
            )
        }

        val requests = AppContext.RequestCollectionRepository.read(RequestsDI(subprojectId = subproject.id))
            ?.requests
            ?.filter { parentIdByRequestId.containsKey(it.id) }
            ?: emptyList()

        result.addAll(requests.flatMap { req ->
            val convertedParentId = parentIdByRequestId[req.id]!!
            when (req.application) {
                ProtocolApplication.WebSocket -> {
                    val id = req.id.convertId("ws-req_")
                    val it = req.payloadExamples!!.first()
                    listOf(
                        InsomniaV4.WebSocketRequest(
                            id = id,
                            parentId = convertedParentId,
                            url = req.url.resolveVariables(),
                            name = req.name,
                            description = "",
                            headers = req.getMergedProperty(0) { it.headers }
                                .map { it.toInsomniaKeyValue() },
                            parameters = req.getMergedProperty(0) { it.queryParameters }
                                .map { it.toInsomniaKeyValue() },
                            authentication = InsomniaV4.HttpRequest.Authentication(null, null, null, null),
                            type = "websocket_request",
                        ),
                        InsomniaV4.RequestPayload(
                            id = uuidString().convertId("ws-payload_"),
                            parentId = id,
                            name = it.name,
                            value = it.body,
                            mode = "text/plain",
                            type = "websocket_payload",
                        )
                    )
                }

                else -> req.examples.mapIndexed { index, it ->
                    val baseExample = req.examples.first()
                    InsomniaV4.HttpRequest(
                        id = it.id.convertId("req_"),
                        parentId = convertedParentId,
                        url = req.url.resolveVariables(),
                        name = req.name + if (index > 0) {
                            " (${it.name})"
                        } else {
                            ""
                        },
                        description = "",
                        method = if (req.application == ProtocolApplication.Graphql) "POST" else req.method,
                        headers = req.getMergedProperty(index) { it.headers }
                            .filter { !it.key.equals("Content-Type", true) && !it.key.equals("Accept", true) }
                            .let {
                                it +
                                    UserKeyValuePair(key = "Content-Type", value = "application/json") +
                                    UserKeyValuePair(key = "Accept", value =  "application/graphql-response+json; charset=utf-8, application/json; charset=utf-8")
                            }
                            .map { it.toInsomniaKeyValue() },
                        parameters = req.getMergedProperty(index) { it.queryParameters }
                            .map { it.toInsomniaKeyValue() },
                        body = when (it.body) {
                            is FormUrlEncodedBody -> InsomniaV4.HttpRequest.Body(
                                mimeType = "application/x-www-form-urlencoded",
                                text = null,
                                params = req.getMergedProperty(index) { (it.body as? FormUrlEncodedBody)?.value }
                                    .map { it.toInsomniaKeyValue() },
                            )

                            is MultipartBody -> InsomniaV4.HttpRequest.Body(
                                mimeType = "multipart/form-data",
                                text = null,
                                params = req.getMergedProperty(index) { (it.body as? MultipartBody)?.value }
                                    .map {
                                        it.toInsomniaKeyValue().copy(
                                            type = if (it.valueType == FieldValueType.File) "file" else null,
                                            fileName = if (it.valueType == FieldValueType.File) it.value.resolveVariables() else null,
                                            value = if (it.valueType == FieldValueType.File) "" else it.value.resolveVariables(),
                                        )
                                    },
                            )

                            is StringBody -> InsomniaV4.HttpRequest.Body(
                                mimeType = "application/json",
                                text = it.body.value.resolveVariables(), // FIXME inheritance
                                params = null,
                            )

                            is FileBody -> InsomniaV4.HttpRequest.Body(
                                mimeType = "application/octet-stream",
                                text = null,
                                fileName = it.body.filePath,
                                params = null,
                            )

                            is GraphqlBody -> it.body
                                .let { body ->
                                    val jsonMapper = jacksonObjectMapper()
                                    val graphqlRequest = GraphqlRequestBody(
                                        query = (if (it.overrides?.isOverrideBodyContent != false) body else baseExample.body as GraphqlBody).document.resolveVariables(),
                                        variables = jsonMapper.readTree((if (it.overrides?.isOverrideBodyVariables != false) body else baseExample.body as GraphqlBody).variables.resolveVariables()),
                                        operationName = body.operationName.emptyToNull()
                                    )
                                    InsomniaV4.HttpRequest.Body(
                                        mimeType = "application/graphql",
                                        text = jsonMapper.writeValueAsString(graphqlRequest),
                                        params = null,
                                    )
                                }

                                null -> InsomniaV4.HttpRequest.Body(mimeType = null, text = null, params = null)
                        },
                        authentication = InsomniaV4.HttpRequest.Authentication(null, null, null, null),
                        type = "request",
                    )
                }
            }
        })

        val baseEnvironment = InsomniaV4.Environment(
            id = uuidString().convertId("env_"),
            parentId = subproject.id.convertId("wrk_"),
            name = "Base Environment",
            data = jsonWriter.createObjectNode(),
            type = "environment",
        )
        result += baseEnvironment
        result.addAll(subproject.environments.map {
            InsomniaV4.Environment(
                id = it.id.convertId("env_"),
                parentId = baseEnvironment.id,
                name = it.name,
                data = jsonWriter.valueToTree(it.variables.map {
                    it.key to it.value
                }.toMap()),
                type = "environment",
            )
        })
        return result
    }

    fun String.convertId(prefix: String): String {
        return prefix + replace("-", "")
    }

    fun String.resolveVariables(): String {
        return replace("\\\$\\{\\{([^{}]+)\\}\\}".toRegex(), "{{\$1}}")
            .replace("$((uuid))", "{% uuid 'v4' %}")
            .replace("$((now.iso8601))", "{% now 'iso-8601', '' %}")
    }

    fun UserRequestTemplate.getMergedProperty(exampleIndex: Int, getter: (UserRequestExample) -> List<UserKeyValuePair>?): List<UserKeyValuePair> {
        val baseExample = examples.first()
        if (exampleIndex == 0) {
            return getter(baseExample) ?: emptyList()
        }
        return (getter(baseExample) ?: emptyList()) + (getter(examples[exampleIndex]) ?: emptyList())
    }

    fun UserKeyValuePair.toInsomniaKeyValue(): InsomniaV4.HttpRequest.KeyValue {
        return InsomniaV4.HttpRequest.KeyValue(
            id = uuidString().convertId("pair_"),
            name = key.resolveVariables(),
            value = value.resolveVariables(),
            description = "",
            disabled = !isEnabled,
            type = null,
            fileName = null,
        )
    }
}

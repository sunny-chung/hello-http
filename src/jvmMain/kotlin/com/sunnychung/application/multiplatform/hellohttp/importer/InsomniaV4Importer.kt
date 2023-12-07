package com.sunnychung.application.multiplatform.hellohttp.importer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.error.UnsupportedFileSchemaVersionError
import com.sunnychung.application.multiplatform.hellohttp.extension.`if`
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.insomniav4.InsomniaV4
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.replaceIf
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
        val requestsBySubproject = mutableMapOf<String, MutableList<UserRequestTemplate>>()
        val requestByInsomniaId = mutableMapOf<String, UserRequestTemplate>()

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

        fun addRequestToSubproject(it: InsomniaV4.Request, req: UserRequestTemplate) {
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
            requestByInsomniaId[it.id] = req
        }

        resources.filter { it["_type"]?.textValue() == "request" }
            .map { log.d { it.toString() }; jsonParser.treeToValue(it, InsomniaV4.HttpRequest::class.java) }
            .forEach {
                val postFlightBodyVariables = mutableListOf<UserKeyValuePair>()

                var req = UserRequestTemplate(
                    id = uuidString(),
                    name = it.name,
                    application = when (it.body.mimeType) {
                        "application/graphql" -> ProtocolApplication.Graphql
                        else -> ProtocolApplication.Http
                    },
                    method = when (it.body.mimeType) {
                        "application/graphql" -> ""
                        else -> it.method ?: ""
                    },
                    url = it.url.convertVariables(postFlightBodyVariables),
                    examples = listOf(
                        UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = when (it.body.mimeType) {
                            null -> ContentType.None
                            "application/json" -> ContentType.Json
                            "multipart/form-data" -> ContentType.Multipart
                            "application/x-www-form-urlencoded" -> ContentType.FormUrlEncoded
                            "application/octet-stream" -> ContentType.BinaryFile
                            "application/graphql" -> ContentType.Graphql
                            else -> ContentType.Raw
                        },
                        body = when (it.body.mimeType) {
                            null -> null
                            "application/json" -> StringBody(it.body.text?.convertVariables(postFlightBodyVariables) ?: "")
                            "multipart/form-data" -> MultipartBody(
                                it.body.params?.map {
                                    if (it.type == "file") {
                                        UserKeyValuePair(
                                            id = uuidString(),
                                            key = it.name.convertVariables(postFlightBodyVariables),
                                            value = it.fileName ?: "",
                                            valueType = FieldValueType.File,
                                            isEnabled = it.disabled != true,
                                        )
                                    } else {
                                        UserKeyValuePair(
                                            id = uuidString(),
                                            key = it.name.convertVariables(postFlightBodyVariables),
                                            value = it.value.convertVariables(postFlightBodyVariables),
                                            valueType = FieldValueType.String,
                                            isEnabled = it.disabled != true,
                                        )
                                    }
                                }?.filterNonEmpty()
                                    ?: emptyList()
                            )
                            "application/x-www-form-urlencoded" -> FormUrlEncodedBody(
                                it.body.params?.map {
                                    UserKeyValuePair(
                                        id = uuidString(),
                                        key = it.name.convertVariables(postFlightBodyVariables),
                                        value = it.value.convertVariables(postFlightBodyVariables),
                                        valueType = FieldValueType.String,
                                        isEnabled = it.disabled != true,
                                    )
                                }?.filterNonEmpty()
                                    ?: emptyList()
                            )
                            "application/octet-stream" -> FileBody(
                                filePath = it.body.fileName
                            )
                            "application/graphql" -> it.body.text.emptyToNull()?.let { body ->
                                try {
                                    jsonParser.readValue<GraphqlRequestBody>(body)
                                        .let {
                                            GraphqlBody(
                                                document = it.query.convertVariables(postFlightBodyVariables),
                                                variables = jsonParser.writerWithDefaultPrettyPrinter()
                                                    .writeValueAsString(it.variables)
                                                    .let { if (it == "null") "" else it }
                                                    .convertVariables(postFlightBodyVariables),
                                                operationName = it.operationName,
                                            )
                                        }
                                } catch (e: Throwable) {
                                    log.w(e) { "Insomnia import error while parsing JSON of GraphQL content. Will use default empty value." }
                                    null
                                }
                            } ?: GraphqlBody(document = "", variables = "", operationName = null)
                            else -> StringBody(it.body.text?.convertVariables(postFlightBodyVariables) ?: "")
                        },
                        headers = it.parseHeaders(postFlightBodyVariables),
                        queryParameters = it.parseQueryParameters(postFlightBodyVariables),
                    ))
                )
                req = req.copy(examples = req.examples.map {
                    it.copy(
                        postFlight = PostFlightSpec(
                            updateVariablesFromBody = postFlightBodyVariables
                        )
                    )
                })
                addRequestToSubproject(it, req)
            }

        resources.filter { it["_type"]?.textValue() == "grpc_request" }
            .map { log.d { it.toString() }; jsonParser.treeToValue(it, InsomniaV4.HttpRequest::class.java) }
            .forEach {
                val postFlightBodyVariables = mutableListOf<UserKeyValuePair>()

                var req = UserRequestTemplate(
                    id = uuidString(),
                    name = it.name,
                    application = ProtocolApplication.Grpc,
                    method = "",
                    url = it.url.convertVariables(postFlightBodyVariables),
                    examples = listOf(
                        UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Json,
                        body = StringBody(it.body.text?.convertVariables(postFlightBodyVariables) ?: ""),
                        headers = it.parseHeaders(postFlightBodyVariables),
                        queryParameters = it.parseQueryParameters(postFlightBodyVariables),
                    )),
                    payloadExamples = listOf(
                        PayloadExample(
                            id = uuidString(),
                            name = "Payload 1",
                            body = it.body.text?.convertVariables(postFlightBodyVariables) ?: "",
                        )
                    ),
                )
                val grpcServiceMethodSplitted = it.protoMethodName?.split('/') ?: emptyList()
                if (grpcServiceMethodSplitted.size == 3) {
                    req = req.copy(
                        grpc = UserGrpcRequest(
                            service = grpcServiceMethodSplitted[1],
                            method = grpcServiceMethodSplitted[2],
                        )
                    )
                }

                req = req.copy(examples = req.examples.map {
                    it.copy(
                        postFlight = PostFlightSpec(
                            updateVariablesFromBody = postFlightBodyVariables
                        )
                    )
                })
                addRequestToSubproject(it, req)
            }

        resources.filter { it["_type"]?.textValue() == "websocket_request" }
            .map { log.d { it.toString() }; jsonParser.treeToValue(it, InsomniaV4.WebSocketRequest::class.java) }
            .forEach {
                val postFlightBodyVariables = mutableListOf<UserKeyValuePair>()
                val req = UserRequestTemplate(
                    id = uuidString(),
                    name = it.name,
                    application = ProtocolApplication.WebSocket,
                    method = "",
                    url = it.url.convertVariables(postFlightBodyVariables),
                    examples = listOf(UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.None,
                        body = null,
                        headers = it.parseHeaders(postFlightBodyVariables),
                        queryParameters = it.parseQueryParameters(postFlightBodyVariables),
                    )),
                    payloadExamples = mutableListOf(
                        PayloadExample(
                            id = uuidString(),
                            name = "Payload 1",
                            body = "",
                        )
                    ),
                )
                addRequestToSubproject(it, req)
            }

        resources.filter { it["_type"]?.textValue() == "websocket_payload" }
            .map { log.d { it.toString() }; jsonParser.treeToValue(it, InsomniaV4.RequestPayload::class.java) }
            .forEach {
                (requestByInsomniaId[it.parentId]?.payloadExamples as? MutableList<PayloadExample>)?.replaceIf(
                    replacement = PayloadExample(
                        id = uuidString(),
                        name = it.name,
                        body = it.value,
                    ),
                    condition = { _ -> true },
                )
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

    fun InsomniaV4.Request.parseHeaders(postFlightBodyVariables: MutableList<UserKeyValuePair>): List<UserKeyValuePair> {
        return ((this.headers ?: emptyList()).map {
            UserKeyValuePair(
                id = uuidString(),
                key = it.name.convertVariables(postFlightBodyVariables),
                value = it.value.convertVariables(postFlightBodyVariables),
                valueType = FieldValueType.String,
                isEnabled = it.disabled != true,
            )
        } + (this.authentication.`if` { it?.type == "bearer" }?.let {
            listOf(
                UserKeyValuePair(
                    id = uuidString(),
                    key = "Authorization",
                    value = "${it.prefix?.convertVariables(postFlightBodyVariables) ?: "Bearer"} ${
                        it.token?.convertVariables(postFlightBodyVariables)
                    }",
                    valueType = FieldValueType.String,
                    isEnabled = it.disabled != true,
                )
            )
        } ?: emptyList())).filterNonEmpty()
    }

    fun InsomniaV4.Request.parseQueryParameters(postFlightBodyVariables: MutableList<UserKeyValuePair>): List<UserKeyValuePair> {
        return (this.parameters ?: emptyList()).map {
            UserKeyValuePair(
                id = uuidString(),
                key = it.name.convertVariables(postFlightBodyVariables),
                value = it.value.convertVariables(postFlightBodyVariables),
                valueType = FieldValueType.String,
                isEnabled = it.disabled != true,
            )
        }.filterNonEmpty()
    }

    val INSOMNIA_SAVE_VARIABLE_REGEX = "\\{% savevariable '([^{}%']*)', '([^{}%']*)', '([^{}%']*)', '([^{}%']*)', '([^{}%']*)' %\\}".toRegex()
    fun String.convertVariables(postFlightBodyVariables: MutableList<UserKeyValuePair>): String {
        var s = this.replace("\\{\\{([^{}]+)\\}\\}".toRegex(), "\\\${{\$1}}")
            .replace("\\{% variable '([^{}%']*)' %\\}".toRegex(), "\\\${{\$1}}")
            .replace("\\{% uuid 'v4' %\\}".toRegex(), "\\\$((uuid))")
            .replace("\\{% now 'iso-8601'[^{}%]* %\\}".toRegex(), "\\\$((now.iso8601))")
            .replace("\\{% now 'millis'[^{}%]* %\\}".toRegex(), "\\\$((now.epochMills))")
            .replace("\\{% now 'unix'[^{}%]* %\\}".toRegex(), "\\\$((now.epochSeconds))")
        INSOMNIA_SAVE_VARIABLE_REGEX.findAll(s)
            .filter { it.groupValues[2] == "responseBody" && it.groupValues[4] == "jsonPath" }
            .forEach {
                postFlightBodyVariables += UserKeyValuePair(
                    id = uuidString(),
                    key = it.groupValues[1],
                    value = it.groupValues[5],
                    valueType = FieldValueType.String,
                    isEnabled = true,
                )
            }
        return s.replace(INSOMNIA_SAVE_VARIABLE_REGEX, "")
    }

    fun JsonNode.stringify(): String {
        return if (isValueNode) {
            asText() ?: toString()
        } else {
            toString()
        }
    }

    fun List<UserKeyValuePair>.filterNonEmpty(): List<UserKeyValuePair> {
        return filter { it.key.isNotBlank() && it.value.isNotBlank() }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.importer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.exporter.RequestSelectionExport
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.isValidHttpMethod
import java.io.File

private const val INVALID_REQUEST_EXPORT_JSON = "Invalid request export JSON"
private const val EXPECTED_EXPORT_APP_NAME = "Hello HTTP"

class RequestSelectionImporter {

    private val jsonParser = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(
            SimpleModule().addDeserializer(
                UserRequestExample::class.java,
                UserRequestExampleDeserializer(),
            )
        )

    fun readMetadata(json: String): RequestSelectionImportMetadata {
        if (json.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        val root = try {
            jsonParser.readTree(json)
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        } ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)

        if (!root.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        val appNode = root["app"] ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (!appNode.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        val appName = appNode["name"]?.takeIf { it.isTextual }?.asText()
            ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        val appVersion = appNode["version"]?.takeIf { it.isTextual }?.asText()
            ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (appName != EXPECTED_EXPORT_APP_NAME || appVersion.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        return RequestSelectionImportMetadata(
            appName = appName,
            appVersion = appVersion,
        )
    }

    fun readMetadata(file: File): RequestSelectionImportMetadata {
        val root = try {
            jsonParser.readTree(file)
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        } ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)

        if (!root.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        val appNode = root["app"] ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (!appNode.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        val appName = appNode["name"]?.takeIf { it.isTextual }?.asText()
            ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        val appVersion = appNode["version"]?.takeIf { it.isTextual }?.asText()
            ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (appName != EXPECTED_EXPORT_APP_NAME || appVersion.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        return RequestSelectionImportMetadata(
            appName = appName,
            appVersion = appVersion,
        )
    }

    fun importFromJson(json: String): RequestSelectionImport {
        val metadata = readMetadata(json)

        val payload = try {
            jsonParser.readValue(json, RequestSelectionExport::class.java)
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        validatePayload(payload)
        return RequestSelectionImport(
            appVersion = metadata.appVersion,
            requests = payload.requests.map {
                validateRequest(it)
                try {
                    it.deepCopyWithNewId()
                } catch (_: Throwable) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }
        )
    }

    fun importFromJson(file: File): RequestSelectionImport {
        val metadata = readMetadata(file)

        val payload = try {
            jsonParser.readValue(file, RequestSelectionExport::class.java)
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        validatePayload(payload)
        return RequestSelectionImport(
            appVersion = metadata.appVersion,
            requests = payload.requests.map {
                validateRequest(it)
                try {
                    it.deepCopyWithNewId()
                } catch (_: Throwable) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }
        )
    }

    private fun validatePayload(payload: RequestSelectionExport) {
        if (payload.app.name != EXPECTED_EXPORT_APP_NAME) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        if (payload.app.version.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        if (payload.requests.isEmpty()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
    }

    private fun validateRequest(request: UserRequestTemplate) {
        if (request.examples.isEmpty() || request.examples.first().name != "Base") {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        validateMethod(request)
        validateProtocolDependentFields(request)
        validateExamples(request)
        validateOverrideReferences(request)
        validateUniqueIds(request)
    }

    private fun validateMethod(request: UserRequestTemplate) {
        val isValidMethod = when (request.application) {
            ProtocolApplication.Http -> request.method.isValidHttpMethod()
            else -> request.method.isEmpty() || request.method.isValidHttpMethod()
        }
        if (!isValidMethod) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
    }

    private fun validateProtocolDependentFields(request: UserRequestTemplate) {
        when (request.application) {
            ProtocolApplication.Http -> {
                if (request.grpc != null || request.payloadExamples != null) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }

            ProtocolApplication.WebSocket -> {
                if (request.grpc != null || request.payloadExamples.isNullOrEmpty()) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }

            ProtocolApplication.Grpc -> {
                if (request.grpc == null || request.payloadExamples.isNullOrEmpty()) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }

            ProtocolApplication.Graphql -> {
                if (request.grpc != null || request.payloadExamples != null) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
            }
        }
    }

    private fun validateExamples(request: UserRequestTemplate) {
        request.examples.forEachIndexed { index, example ->
            if (example.id.isBlank()) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (index > 0 && example.overrides == null) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            when (request.application) {
                ProtocolApplication.Http -> Unit
                ProtocolApplication.WebSocket -> {
                    if (example.contentType != ContentType.None) {
                        throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                    }
                }
                ProtocolApplication.Grpc -> {
                    if (example.contentType != ContentType.Json) {
                        throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                    }
                }
                ProtocolApplication.Graphql -> {
                    if (example.contentType != ContentType.Graphql) {
                        throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                    }
                }
            }
            validateBodyType(example)
        }
    }

    private fun validateOverrideReferences(request: UserRequestTemplate) {
        if (request.examples.size <= 1) {
            return
        }
        val baseExample = request.examples.first()
        val baseHeaderIds = baseExample.headers.map { it.id }.toSet()
        val baseCookieIds = baseExample.cookies.map { it.id }.toSet()
        val baseQueryParameterIds = baseExample.queryParameters.map { it.id }.toSet()
        val baseVariableIds = baseExample.variables.map { it.id }.toSet()
        val baseBodyKeyValueIds = (baseExample.body as? RequestBodyWithKeyValuePairs)?.value?.map { it.id }?.toSet() ?: emptySet()
        val basePreFlightUpdateVarIds = with(baseExample.preFlight) {
            updateVariablesFromHeader + updateVariablesFromQueryParameters + updateVariablesFromBody + updateVariablesFromGraphqlVariables
        }.map { it.id }.toSet()
        val basePostFlightUpdateVarIds = with(baseExample.postFlight) {
            updateVariablesFromHeader + updateVariablesFromBody
        }.map { it.id }.toSet()

        request.examples.drop(1).forEach { example ->
            val overrides = example.overrides ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            if (!overrides.disabledHeaderIds.all { it in baseHeaderIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disabledCookieIds.all { it in baseCookieIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disabledQueryParameterIds.all { it in baseQueryParameterIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disabledVariables.all { it in baseVariableIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disabledBodyKeyValueIds.all { it in baseBodyKeyValueIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disablePreFlightUpdateVarIds.all { it in basePreFlightUpdateVarIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
            if (!overrides.disablePostFlightUpdateVarIds.all { it in basePostFlightUpdateVarIds }) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
        }
    }

    private fun validateUniqueIds(request: UserRequestTemplate) {
        val allIds = mutableSetOf<String>()

        fun addId(id: String) {
            if (id.isBlank() || !allIds.add(id)) {
                throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
            }
        }

        addId(request.id)
        request.examples.forEach { example ->
            addId(example.id)
            example.headers.forEach { addId(it.id) }
            example.cookies.forEach { addId(it.id) }
            example.queryParameters.forEach { addId(it.id) }
            example.variables.forEach { addId(it.id) }
            (example.body as? RequestBodyWithKeyValuePairs)?.value?.forEach { addId(it.id) }
            example.preFlight.updateVariablesFromHeader.forEach { addId(it.id) }
            example.preFlight.updateVariablesFromQueryParameters.forEach { addId(it.id) }
            example.preFlight.updateVariablesFromBody.forEach { addId(it.id) }
            example.preFlight.updateVariablesFromGraphqlVariables.forEach { addId(it.id) }
            example.postFlight.updateVariablesFromHeader.forEach { addId(it.id) }
            example.postFlight.updateVariablesFromBody.forEach { addId(it.id) }
        }
        request.payloadExamples?.forEach { addId(it.id) }
    }

    private fun validateBodyType(example: UserRequestExample) {
        val isValid = when (example.contentType) {
            ContentType.None -> example.body == null
            ContentType.Json, ContentType.Raw -> example.body is StringBody
            ContentType.FormUrlEncoded -> example.body is FormUrlEncodedBody
            ContentType.Multipart -> example.body is MultipartBody
            ContentType.BinaryFile -> example.body is FileBody
            ContentType.Graphql -> example.body is GraphqlBody
        }
        if (!isValid) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
    }
}

data class RequestSelectionImport(
    val appVersion: String,
    val requests: List<UserRequestTemplate>,
)

data class RequestSelectionImportMetadata(
    val appName: String,
    val appVersion: String,
)

private class UserRequestExampleDeserializer : JsonDeserializer<UserRequestExample>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UserRequestExample {
        val node = p.codec.readTree<JsonNode>(p)
            ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (!node.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        val contentTypeNode = node["contentType"] ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        val contentType = p.codec.treeToValue(contentTypeNode, ContentType::class.java)
        val body = parseBody(p, node["body"], contentType)

        return UserRequestExample(
            id = node.requiredText("id"),
            name = node.requiredText("name"),
            contentType = contentType,
            headers = node.requiredList(p, "headers", UserKeyValuePair::class.java),
            cookies = node.requiredList(p, "cookies", UserKeyValuePair::class.java),
            queryParameters = node.requiredList(p, "queryParameters", UserKeyValuePair::class.java),
            body = body,
            variables = node.requiredList(p, "variables", UserKeyValuePair::class.java),
            preFlight = node.requiredObject(p, "preFlight", PreFlightSpec::class.java),
            postFlight = node.requiredObject(p, "postFlight", PostFlightSpec::class.java),
            overrides = node["overrides"]?.let {
                if (it.isNull) {
                    null
                } else {
                    p.codec.treeToValue(it, UserRequestExample.Overrides::class.java)
                }
            },
        )
    }

    private fun parseBody(p: JsonParser, bodyNode: JsonNode?, contentType: ContentType): UserRequestBody? {
        return when (contentType) {
            ContentType.None -> {
                if (bodyNode != null && !bodyNode.isNull) {
                    throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
                }
                null
            }

            ContentType.Json, ContentType.Raw -> StringBody(bodyNode.requiredText("value"))

            ContentType.FormUrlEncoded -> FormUrlEncodedBody(
                value = bodyNode.requiredList(p, "value", UserKeyValuePair::class.java),
            )

            ContentType.Multipart -> MultipartBody(
                value = bodyNode.requiredList(p, "value", UserKeyValuePair::class.java),
            )

            ContentType.BinaryFile -> FileBody(
                filePath = bodyNode.optionalNullableText("filePath"),
            )

            ContentType.Graphql -> GraphqlBody(
                document = bodyNode.requiredText("document"),
                variables = bodyNode.requiredText("variables"),
                operationName = bodyNode.optionalNullableText("operationName"),
            )
        }
    }

    private fun JsonNode.requiredNode(fieldName: String): JsonNode {
        return this[fieldName] ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
    }

    private fun JsonNode?.requiredNode(): JsonNode {
        return this ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
    }

    private fun JsonNode?.requiredText(fieldName: String): String {
        val fieldNode = this.requiredNode().requiredNode(fieldName)
        if (!fieldNode.isTextual) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return fieldNode.asText()
    }

    private fun JsonNode?.optionalNullableText(fieldName: String): String? {
        val fieldNode = this.requiredNode()[fieldName] ?: return null
        if (fieldNode.isNull) {
            return null
        }
        if (!fieldNode.isTextual) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return fieldNode.asText()
    }

    private fun <T> JsonNode?.requiredList(p: JsonParser, fieldName: String, clazz: Class<T>): List<T> {
        val fieldNode = this.requiredNode().requiredNode(fieldName)
        if (!fieldNode.isArray) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return fieldNode.map {
            p.codec.treeToValue(it, clazz)
        }
    }

    private fun <T> JsonNode.requiredObject(p: JsonParser, fieldName: String, clazz: Class<T>): T {
        val fieldNode = requiredNode(fieldName)
        if (!fieldNode.isObject) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return p.codec.treeToValue(fieldNode, clazz)
    }
}

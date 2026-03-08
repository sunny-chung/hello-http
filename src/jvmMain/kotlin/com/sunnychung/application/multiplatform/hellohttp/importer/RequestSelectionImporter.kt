package com.sunnychung.application.multiplatform.hellohttp.importer

import com.sunnychung.application.multiplatform.hellohttp.exporter.RequestSelectionExport
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.isValidHttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

private const val INVALID_REQUEST_EXPORT_JSON = "Invalid request export JSON"
private const val EXPECTED_EXPORT_APP_NAME = "Hello HTTP"
private const val BODY_TYPE_DISCRIMINATOR = "type"

class RequestSelectionImporter {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun readMetadata(json: String): RequestSelectionImportMetadata {
        val root = parseRootJsonObject(json)
        val appNode = root["app"] as? JsonObject ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        val appName = appNode.getRequiredString("name")
        val appVersion = appNode.getRequiredString("version")
        if (appName != EXPECTED_EXPORT_APP_NAME || appVersion.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }

        return RequestSelectionImportMetadata(
            appName = appName,
            appVersion = appVersion,
        )
    }

    fun readMetadata(file: File): RequestSelectionImportMetadata {
        val text = try {
            file.readText()
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return readMetadata(text)
    }

    fun importFromJson(json: String): RequestSelectionImport {
        val root = parseRootJsonObject(json)
        val metadata = readMetadata(json)
        val normalizedRoot = normalizeLegacyBodyEncoding(root)

        val payload = try {
            jsonParser.decodeFromJsonElement(RequestSelectionExport.serializer(), normalizedRoot)
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
        val text = try {
            file.readText()
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return importFromJson(text)
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

    private fun parseRootJsonObject(json: String): JsonObject {
        if (json.isBlank()) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        val root = try {
            jsonParser.parseToJsonElement(json)
        } catch (_: Throwable) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return root as? JsonObject ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
    }

    /**
     * Backward compatibility for JSON exported by Jackson that encodes body shape by `contentType`
     * and does not include polymorphic type discriminator in `body`.
     */
    private fun normalizeLegacyBodyEncoding(root: JsonObject): JsonObject {
        val requests = root["requests"] as? JsonArray ?: return root
        val normalizedRequests = requests.map { requestElement ->
            val requestObject = requestElement as? JsonObject ?: return@map requestElement
            val examples = requestObject["examples"] as? JsonArray ?: return@map requestElement
            val normalizedExamples = examples.map(::normalizeExampleBodyEncoding)
            JsonObject(
                requestObject.toMutableMap().apply {
                    put("examples", JsonArray(normalizedExamples))
                }
            )
        }

        return JsonObject(
            root.toMutableMap().apply {
                put("requests", JsonArray(normalizedRequests))
            }
        )
    }

    private fun normalizeExampleBodyEncoding(exampleElement: JsonElement): JsonElement {
        val example = exampleElement as? JsonObject ?: return exampleElement
        val body = example["body"] as? JsonObject ?: return exampleElement
        if (BODY_TYPE_DISCRIMINATOR in body) {
            return exampleElement
        }

        val contentType = (example["contentType"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?: return exampleElement
        val serialName = when (contentType) {
            ContentType.Json.name, ContentType.Raw.name -> "StringBody"
            ContentType.FormUrlEncoded.name -> "FormUrlEncodedBody"
            ContentType.Multipart.name -> "MultipartBody"
            ContentType.BinaryFile.name -> "FileBody"
            ContentType.Graphql.name -> "GraphqlBody"
            else -> null
        } ?: return exampleElement

        val normalizedBody = JsonObject(
            body.toMutableMap().apply {
                put(BODY_TYPE_DISCRIMINATOR, JsonPrimitive(serialName))
            }
        )
        return JsonObject(
            example.toMutableMap().apply {
                put("body", normalizedBody)
            }
        )
    }

    private fun JsonObject.getRequiredString(key: String): String {
        val primitive = this[key] as? JsonPrimitive ?: throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        if (!primitive.isString) {
            throw IllegalArgumentException(INVALID_REQUEST_EXPORT_JSON)
        }
        return primitive.content
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

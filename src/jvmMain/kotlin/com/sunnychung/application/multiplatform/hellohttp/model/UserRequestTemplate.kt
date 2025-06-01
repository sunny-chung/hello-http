package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.bigtext.extension.runIf
import graphql.language.OperationDefinition
import graphql.parser.InvalidSyntaxException
import graphql.parser.Parser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URI

@Persisted
@Serializable
data class UserRequestTemplate(
    val id: String,
    val name: String = "",
//    val protocol: Protocol = Protocol.Http, // this field is removed and cannot be reused
    val application: ProtocolApplication = ProtocolApplication.Http,
    val method: String = "",
    val url: String = "",
    val grpc: UserGrpcRequest? = null,

    val examples: List<UserRequestExample> = listOf(UserRequestExample(id = uuidString(), name = "Base")),
    val payloadExamples: List<PayloadExample>? = null,
) {
    init {
        if (method != method.trim().uppercase()) {
            throw IllegalArgumentException("`name` must be in upper case")
        }
    }

    fun isExampleBase(example: UserRequestExample): Boolean {
        return examples.indexOfFirst { it.id == example.id } == 0
    }

    fun isExampleIdBase(exampleId: String): Boolean {
        return examples.indexOfFirst { it.id == exampleId } == 0
    }

    fun copyForApplication(application: ProtocolApplication, method: String) =
        if (application == ProtocolApplication.WebSocket && payloadExamples.isNullOrEmpty()) {
            copy(
                application = application,
                method = method,
                payloadExamples = listOf(PayloadExample(id = uuidString(), name = "New Payload", body = ""))
            )
        } else if (application == ProtocolApplication.Graphql) {
            copy(
                application = application,
                method = method,
                examples = examples.map {
                    it.copy(
                        contentType = ContentType.Graphql,
                        body = if (it.body is GraphqlBody) it.body else GraphqlBody(
                            document = "",
                            variables = "",
                            operationName = null
                        )
                    )
                }
            )
        } else if (application == ProtocolApplication.Grpc) {
            copy(
                application = application,
                method = method,
                examples = examples.map {
                    it.copy(
                        contentType = ContentType.Json,
                        body = if (it.body is StringBody) it.body else StringBody("")
                    )
                },
                grpc = grpc ?: UserGrpcRequest(),
                payloadExamples = payloadExamples ?: listOf(PayloadExample(id = uuidString(), name = "New Payload", body = "")),
            )
        } else {
            copy(application = application, method = method)
        }

    fun deepCopyWithNewId(): UserRequestTemplate {
        val idMapping = mutableMapOf<String, String>()

        fun List<UserKeyValuePair>.deepCopyWithNewId(
            isSaveIdMapping: Boolean = false,
            isUseIdMapping: Boolean = false
        ) = map {
            val newId = if (isUseIdMapping) {
                idMapping[it.id] ?: uuidString()
            } else {
                uuidString()
            }
            if (isSaveIdMapping) {
                idMapping[it.id] = newId
            }
            it.copy(
                id = newId,
            )
        }

        fun UserRequestBody.deepCopyWithNewId(
            isSaveIdMapping: Boolean = false,
            isUseIdMapping: Boolean = false
        ) = when(this) {
            is FileBody -> FileBody(filePath)
            is FormUrlEncodedBody -> FormUrlEncodedBody(value.deepCopyWithNewId(
                isSaveIdMapping = isSaveIdMapping,
                isUseIdMapping = isUseIdMapping
            ))
            is GraphqlBody -> GraphqlBody(document, variables, operationName)
            is com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody -> MultipartBody(
                value.deepCopyWithNewId(
                    isSaveIdMapping = isSaveIdMapping,
                    isUseIdMapping = isUseIdMapping
                )
            )
            is StringBody -> StringBody(value)
        }

        return copy(
            id = uuidString(),
            examples = examples.mapIndexed { index, it ->
                it.copy(
                    id = uuidString(),
                    headers = it.headers.deepCopyWithNewId(isSaveIdMapping = index == 0),
                    cookies = it.cookies.deepCopyWithNewId(isSaveIdMapping = index == 0),
                    queryParameters = it.queryParameters.deepCopyWithNewId(isSaveIdMapping = index == 0),
                    body = it.body?.deepCopyWithNewId(isSaveIdMapping = index == 0),
                    variables = it.variables.deepCopyWithNewId(isSaveIdMapping = index == 0),
                    preFlight = with(it.preFlight) {
                        copy(
                            updateVariablesFromHeader = updateVariablesFromHeader.deepCopyWithNewId(isSaveIdMapping = index == 0),
                            updateVariablesFromBody = updateVariablesFromBody.deepCopyWithNewId(isSaveIdMapping = index == 0),
                            updateVariablesFromGraphqlVariables = updateVariablesFromGraphqlVariables.deepCopyWithNewId(isSaveIdMapping = index == 0),
                            updateVariablesFromQueryParameters = updateVariablesFromQueryParameters.deepCopyWithNewId(isSaveIdMapping = index == 0),
                        )
                    },
                    postFlight = with (it.postFlight) {
                        copy(
                            updateVariablesFromHeader = updateVariablesFromHeader.deepCopyWithNewId(isSaveIdMapping = index == 0),
                            updateVariablesFromBody = updateVariablesFromBody.deepCopyWithNewId(isSaveIdMapping = index == 0),
                        )
                    },
                    overrides = it.overrides?.let { o ->
                        o.copy(
                            disabledHeaderIds = o.disabledHeaderIds.map { idMapping[it]!! }.toSet(),
                            disabledCookieIds = o.disabledCookieIds.map { idMapping[it]!! }.toSet(),
                            disabledQueryParameterIds = o.disabledQueryParameterIds.map { idMapping[it]!! }.toSet(),
                            disabledBodyKeyValueIds = o.disabledBodyKeyValueIds.map { idMapping[it]!! }.toSet(),
                            disablePreFlightUpdateVarIds = o.disablePreFlightUpdateVarIds.map { idMapping[it]!! }.toSet(),
                            disablePostFlightUpdateVarIds = o.disablePostFlightUpdateVarIds.map { idMapping[it]!! }.toSet(),
                            disabledVariables = o.disabledVariables.map { idMapping[it]!! }.toSet(),
                        )
                    },
                )
            },
            payloadExamples = payloadExamples?.map {
                it.copy(
                    id = uuidString(),
                )
            },
            grpc = grpc?.copy(),
        )
    }

    sealed class ResolveVariableMode
    object ExpandByEnvironment : ResolveVariableMode()
    data class ReplaceAsString(val replacement: String = "{{\$1}}") : ResolveVariableMode()

    data class Scope(val baseExample: UserRequestExample, val selectedExample: UserRequestExample, val variableResolver: VariableResolver) {
        fun String.resolveVariables(): String = variableResolver.resolve(this)

        fun getMergedKeyValues(
            propertyGetter: (UserRequestExample) -> List<UserKeyValuePair>?,
            disabledIds: Set<String>?,
            isResolveKeyVariable: Boolean = true,
            isUniqueKey: Boolean = false,
            environmentPropertyGetter: (Environment) -> List<UserKeyValuePair> = { emptyList() },
        ): List<UserKeyValuePair> {
            val envValues = (variableResolver.environment?.let { env -> environmentPropertyGetter(env) } ?: emptyList())
                .filter { it.isEnabled && (disabledIds == null || !disabledIds.contains(it.id)) }

            val baseValues = (baseExample.takeIf { selectedExample.id != baseExample.id }?.let { propertyGetter(it) } ?: emptyList())
                .filter { it.isEnabled && (disabledIds == null || !disabledIds.contains(it.id)) }

            val currentValues = (propertyGetter(selectedExample) ?: emptyList())
                .filter { it.isEnabled }

            return (envValues + baseValues + currentValues)
                .map { it.copy(
                    key = it.key.let { if (isResolveKeyVariable) it.resolveVariables() else it },
                    value = it.value.resolveVariables(),
                ) }
                .runIf(isUniqueKey) {
                    val keys = mutableSetOf<String>()
                    val remainingReversedList = mutableListOf<UserKeyValuePair>()
                    asReversed().forEach {
                        val key = it.key
                        if (key !in keys) {
                            remainingReversedList += it
                            keys += key
                        }
                    }
                    remainingReversedList.reversed()
                }
        }

        fun getApplicableCookiesForUrl(url: String): List<UserKeyValuePair> {
            return getMergedKeyValues(
                propertyGetter = { it.cookies },
                disabledIds = selectedExample.overrides?.disabledCookieIds,
                isResolveKeyVariable = false,
                isUniqueKey = true,
                environmentPropertyGetter = { environment ->
                    try {
                        environment.cookieJar.getCookiesFor(
                            URI(url)
                        )
                    } catch (_: Exception) {
                        emptyList()
                    }
                        .map { UserKeyValuePair("env/${it.name}", it.name, it.value, FieldValueType.String, true) }
                }
            )
        }
    }

    fun <R> withScope(exampleId: String, environment: Environment?, resolveVariableMode: ResolveVariableMode = ExpandByEnvironment, action: Scope.() -> R): R {
        val baseExample = examples.first()
        val selectedExample = examples.first { it.id == exampleId }

        return Scope(baseExample, selectedExample, VariableResolver(environment, this, exampleId, resolveVariableMode)).action()
    }

    fun getPreFlightVariables(exampleId: String, environment: Environment?) = withScope(exampleId, environment) {
        val headerVariables = getMergedKeyValues(
            propertyGetter = { it.preFlight.updateVariablesFromHeader },
            disabledIds = selectedExample.overrides?.disablePreFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        val bodyVariables = getMergedKeyValues(
            propertyGetter = { it.preFlight.updateVariablesFromBody },
            disabledIds = selectedExample.overrides?.disablePreFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        val queryParamVariables = getMergedKeyValues(
            propertyGetter = { it.preFlight.updateVariablesFromQueryParameters },
            disabledIds = selectedExample.overrides?.disablePreFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        val graphqlVariables = getMergedKeyValues(
            propertyGetter = { it.preFlight.updateVariablesFromGraphqlVariables },
            disabledIds = selectedExample.overrides?.disablePreFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        PreFlightSpec(
            updateVariablesFromHeader = headerVariables,
            updateVariablesFromQueryParameters = queryParamVariables,
            updateVariablesFromBody = bodyVariables,
            updateVariablesFromGraphqlVariables = graphqlVariables,
        )
    }

    fun getPostFlightVariables(exampleId: String, environment: Environment?) = withScope(exampleId, environment) {
        val headerVariables = getMergedKeyValues(
            propertyGetter = { it.postFlight.updateVariablesFromHeader },
            disabledIds = selectedExample.overrides?.disablePostFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        val bodyVariables = getMergedKeyValues(
            propertyGetter = { it.postFlight.updateVariablesFromBody },
            disabledIds = selectedExample.overrides?.disablePostFlightUpdateVarIds
        )
            .filter { it.key.isNotBlank() }

        Pair(headerVariables, bodyVariables)
    }

    fun getExampleVariablesOnly(exampleId: String): Map<String, String> {
        val baseExample = examples.first()
        val selectedExample = examples.first { it.id == exampleId }
        return (
                baseExample.variables
                    .filter { it.isEnabled && it.id !in (selectedExample.overrides?.disabledVariables ?: emptySet()) }
                    .map { it.key to it.value } +
                        selectedExample.variables.filter { it.isEnabled }.map { it.key to it.value }
                ).toMap()
    }

    fun getAllVariables(exampleId: String, environment: Environment?): Map<String, String> {
        val environmentVariables = environment?.variables?.filter { it.isEnabled }?.associate { it.key to it.value } ?: emptyMap()
        return environmentVariables + getExampleVariablesOnly(exampleId)
    }
}

enum class ProtocolApplication {
    Http, WebSocket, Grpc, Graphql
}

@Persisted
@Serializable
data class UserGrpcRequest(
    val apiSpecId: String = "",
    val service: String = "",
    val method: String = "",
)

@Persisted
@Serializable
data class UserRequestExample(
    override val id: String,
    val name: String,
    val contentType: ContentType = ContentType.None,
    val headers: List<UserKeyValuePair> = mutableListOf(),
    val cookies: List<UserKeyValuePair> = mutableListOf(),
    val queryParameters: List<UserKeyValuePair> = mutableListOf(),
    val body: UserRequestBody? = null,
    val variables: List<UserKeyValuePair> = mutableListOf(),
    val preFlight: PreFlightSpec = PreFlightSpec(),
    val postFlight: PostFlightSpec = PostFlightSpec(),
    val overrides: Overrides? = null, // only the Base example can be null
) : Identifiable {

    @Persisted
    @Serializable
    data class Overrides(
        val disabledHeaderIds: Set<String> = emptySet(),
        val disabledQueryParameterIds: Set<String> = emptySet(),
        val disabledCookieIds: Set<String> = emptySet(),
        val disabledVariables: Set<String> = emptySet(),

        /**
         * Only for raw body and JSON body
         */
        val isOverrideBody: Boolean = true,

        /**
         * For GraphQL, whether override the query document
         */
        val isOverrideBodyContent: Boolean = true,

        /**
         * For GraphQL, whether override the input variables
         */
        val isOverrideBodyVariables: Boolean = true,

        val disabledBodyKeyValueIds: Set<String> = emptySet(),

        val isOverridePreFlightScript: Boolean = true,

        val disablePreFlightUpdateVarIds: Set<String> = emptySet(),

        val disablePostFlightUpdateVarIds: Set<String> = emptySet(),
    ) {
        fun hasNoDisable(): Boolean =
            disabledHeaderIds.isEmpty()
                && disabledQueryParameterIds.isEmpty()
                && disabledBodyKeyValueIds.isEmpty()
                && disablePostFlightUpdateVarIds.isEmpty()
    }

    fun deepCopyWithNewId(isCreateOverridesIfMissing: Boolean = true): UserRequestExample {
        fun List<UserKeyValuePair>.deepCopyWithNewId() = map {
            it.copy(
                id = uuidString(),
            )
        }

        fun UserRequestBody.deepCopyWithNewId() = when(this) {
            is FileBody -> FileBody(filePath)
            is FormUrlEncodedBody -> FormUrlEncodedBody(value.deepCopyWithNewId())
            is GraphqlBody -> GraphqlBody(document, variables, operationName)
            is com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody -> MultipartBody(
                value.deepCopyWithNewId()
            )
            is StringBody -> StringBody(value)
        }

        return copy(
            id = uuidString(),
            headers = headers.deepCopyWithNewId(),
            cookies = cookies.deepCopyWithNewId(),
            queryParameters = queryParameters.deepCopyWithNewId(),
            body = body?.deepCopyWithNewId(),
            variables = variables.deepCopyWithNewId(),
            preFlight = with(preFlight) {
                copy(
                    updateVariablesFromHeader = updateVariablesFromHeader.deepCopyWithNewId(),
                    updateVariablesFromBody = updateVariablesFromBody.deepCopyWithNewId(),
                    updateVariablesFromGraphqlVariables = updateVariablesFromGraphqlVariables.deepCopyWithNewId(),
                    updateVariablesFromQueryParameters = updateVariablesFromQueryParameters.deepCopyWithNewId(),
                )
            },
            postFlight = with (postFlight) {
                copy(
                    updateVariablesFromHeader = updateVariablesFromHeader.deepCopyWithNewId(),
                    updateVariablesFromBody = updateVariablesFromBody.deepCopyWithNewId(),
                )
            },
            overrides = overrides?.let { o ->
                o.copy(
                    disabledHeaderIds = o.disabledHeaderIds.map { it }.toSet(),
                    disabledCookieIds = o.disabledCookieIds.map { it }.toSet(),
                    disabledQueryParameterIds = o.disabledQueryParameterIds.map { it }.toSet(),
                    disabledBodyKeyValueIds = o.disabledBodyKeyValueIds.map { it }.toSet(),
                    disablePreFlightUpdateVarIds = o.disablePreFlightUpdateVarIds.map { it }.toSet(),
                    disablePostFlightUpdateVarIds = o.disablePostFlightUpdateVarIds.map { it }.toSet(),
                    disabledVariables = o.disabledVariables.map { it }.toSet(),
                )
            } ?: if (isCreateOverridesIfMissing) {
                Overrides()
            } else {
                null
            },
        )
    }

    companion object {
        fun create(application: ProtocolApplication): UserRequestExample {
            return when (application) {
                ProtocolApplication.Graphql -> {
                    UserRequestExample(
                        id = uuidString(),
                        name = "New Example",
                        overrides = UserRequestExample.Overrides(),
                        contentType = ContentType.Graphql,
                        body = GraphqlBody(
                            document = "",
                            variables = "",
                            operationName = null
                        ),
                    )
                }
                else -> {
                    UserRequestExample(
                        id = uuidString(),
                        name = "New Example",
                        overrides = UserRequestExample.Overrides(),
                    )
                }
            }
        }
    }
}

@Persisted
@Serializable
data class PayloadExample(
    override val id: String,
    val name: String,
    val body: String,
) : Identifiable

@Persisted
@Serializable
data class PreFlightSpec(
    val executeCode: String = "",
    val updateVariablesFromHeader: List<UserKeyValuePair> = mutableListOf(),
    val updateVariablesFromQueryParameters: List<UserKeyValuePair> = mutableListOf(),
    val updateVariablesFromBody: List<UserKeyValuePair> = mutableListOf(),
    val updateVariablesFromGraphqlVariables: List<UserKeyValuePair> = mutableListOf(),
) {
    fun isNotEmpty(): Boolean = executeCode.isNotEmpty() ||
        updateVariablesFromHeader.isNotEmpty() ||
        updateVariablesFromQueryParameters.isNotEmpty() ||
        updateVariablesFromBody.isNotEmpty() ||
        updateVariablesFromGraphqlVariables.isNotEmpty()

    fun hasUpdateVariables(): Boolean =
        updateVariablesFromHeader.isNotEmpty() ||
        updateVariablesFromQueryParameters.isNotEmpty() ||
        updateVariablesFromBody.isNotEmpty() ||
        updateVariablesFromGraphqlVariables.isNotEmpty()
}

@Persisted
@Serializable
data class PostFlightSpec(
    val updateVariablesFromHeader: List<UserKeyValuePair> = mutableListOf(),
    val updateVariablesFromBody: List<UserKeyValuePair> = mutableListOf(),
)

//enum class ContentType {
//    None, Raw, Json, FormData, Multipart
//}

enum class ContentType(val displayText: String, val headerValue: String?) {
    Json(displayText = "JSON", headerValue = "application/json"),
    Multipart(displayText = "Multipart Form", headerValue = "multipart/form-data"), // "multipart/form-data; boundary=<generated>"
    FormUrlEncoded(displayText = "Form URL-Encoded", headerValue = "application/x-www-form-urlencoded"),
    Raw(displayText = "Raw", headerValue = null),
    BinaryFile(displayText = "Binary File", headerValue = null),
    Graphql(displayText = "GraphQL", headerValue = "application/json"),
    None(displayText = "None", headerValue = null),
}

@Persisted
@Serializable
data class UserKeyValuePair(
    val id: String,
    val key: String,

    /**
     * If valueType = File, this value is a relative path
     */
    var value: String,

    val valueType: FieldValueType,
    val isEnabled: Boolean
) {
    constructor(key: String, value: String): this(
        id = uuidString(),
        key = key,
        value = value,
        valueType = FieldValueType.String,
        isEnabled = true,
    )
}

enum class FieldValueType {
    String, File
}

@Serializable
sealed interface UserRequestBody {
    fun toOkHttpBody(mediaType: MediaType?): RequestBody
}

@Persisted
@Serializable
@SerialName("StringBody")
class StringBody(val value: String) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody = value.toRequestBody(mediaType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringBody) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "${super.toString()}:${value.length}"
    }
}

interface RequestBodyWithKeyValuePairs {
    val value: List<UserKeyValuePair>
}

@Persisted
@Serializable
@SerialName("FormUrlEncodedBody")
class FormUrlEncodedBody(override val value: List<UserKeyValuePair>) : UserRequestBody, RequestBodyWithKeyValuePairs {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody {
        val builder = FormBody.Builder()
        value.forEach { builder.add(it.key, it.value) }
        return builder.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FormUrlEncodedBody) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@Persisted
@Serializable
@SerialName("MultipartBody")
class MultipartBody(override val value: List<UserKeyValuePair>) : UserRequestBody, RequestBodyWithKeyValuePairs {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody {
        val b = MultipartBody.Builder()
        value.filter { it.isEnabled }.forEach {
            when (it.valueType) {
                FieldValueType.String -> b.addFormDataPart(it.key, it.value)
                FieldValueType.File -> {
                    val f = File(it.value)
                    b.addFormDataPart(name = it.key, filename = f.name, body = f.asRequestBody("application/octet-stream".toMediaType()))
                }
            }
        }
        return b.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@Persisted
@Serializable
@SerialName("FileBody")
class FileBody(val filePath: String?) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody {
        return filePath?.let { File(it).asRequestBody(mediaType) } ?: byteArrayOf().toRequestBody(mediaType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileBody) return false

        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int {
        return filePath?.hashCode() ?: 0
    }
}

@Persisted
@Serializable
@SerialName("GraphqlBody")
data class GraphqlBody(val document: String, val variables: String, val operationName: String?) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody = throw NotImplementedError()

    fun getAllOperations(isThrowError: Boolean = false): List<OperationDefinition> {
        try {
            val parsedDocument = Parser().parseDocument(document)
            val operations = parsedDocument.definitions.filterIsInstance<OperationDefinition>()
            return operations
        } catch (e: InvalidSyntaxException) {
            if (isThrowError) throw e
        }
        return listOf()
    }

    fun getOperation(isThrowError: Boolean = false): OperationDefinition? {
        try {
            val operations = getAllOperations(isThrowError)
            val operation = if (operationName?.isNotBlank() == true) {
                operations.firstOrNull { it.name == operationName }
                    ?: if (isThrowError) throw IllegalArgumentException("The operation that corresponds to the operation name could not be found.\n\nPlease make sure the operation name exists in the body, or optionally omits if there is only one operation.") else null
            } else {
                operations.firstOrNull()
                    ?: if (isThrowError) throw IllegalArgumentException("Missing operation") else null
            }
            return operation
        } catch (e: InvalidSyntaxException) {
            if (isThrowError) throw e
        }
        return null
    }
}

fun UserRequestTemplate.describeHeading(exampleId: String): String {
    val example = examples.firstOrNull { it.id == exampleId } ?: return ""

    val title = buildString {
        append(name)
        if (!isExampleBase(example)) {
            append(" - ")
            append(example.name)
        }
    }

    return "$title\n${"=".repeat(title.length)}\n\n"
}

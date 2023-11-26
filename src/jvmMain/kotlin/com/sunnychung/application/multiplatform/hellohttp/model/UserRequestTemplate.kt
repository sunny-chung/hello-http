package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
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

@Persisted
@Serializable
data class UserRequestTemplate(
    val id: String,
    val name: String = "",
//    val protocol: Protocol = Protocol.Http, // this field is removed and cannot be reused
    val application: ProtocolApplication = ProtocolApplication.Http,
    val method: String = "",
    val url: String = "",

    val examples: List<UserRequestExample> = listOf(UserRequestExample(id = uuidString(), name = "Base")),
    val payloadExamples: List<PayloadExample>? = null,
) {
    init {
        if (method != method.trim().uppercase()) {
            throw IllegalArgumentException("`name` must be in upper case")
        }
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
        } else {
            copy(application = application, method = method)
        }

    sealed class ResolveVariableMode
    object ExpandByEnvironment : ResolveVariableMode()
    data class ReplaceAsString(val replacement: String = "{{\$1}}") : ResolveVariableMode()

    data class Scope(val baseExample: UserRequestExample, val selectedExample: UserRequestExample, val variableResolver: VariableResolver) {
        fun String.resolveVariables(): String = variableResolver.resolve(this)

        fun getMergedKeyValues(
            propertyGetter: (UserRequestExample) -> List<UserKeyValuePair>?,
            disabledIds: Set<String>?
        ): List<UserKeyValuePair> {
            if (selectedExample.id == baseExample.id) { // the Base example is selected
                return propertyGetter(baseExample)?.filter { it.isEnabled }
                    ?.map { it.copy(key = it.key.resolveVariables(), value = it.value.resolveVariables()) }
                    ?: emptyList() // TODO reduce code duplication
            }

            val baseValues = (propertyGetter(baseExample) ?: emptyList())
                .filter { it.isEnabled && (disabledIds == null || !disabledIds.contains(it.id)) }

            val currentValues = (propertyGetter(selectedExample) ?: emptyList())
                .filter { it.isEnabled }

            return (baseValues + currentValues)
                .map { it.copy(key = it.key.resolveVariables(), value = it.value.resolveVariables()) }
        }
    }

    fun <R> withScope(exampleId: String, environment: Environment?, resolveVariableMode: ResolveVariableMode = ExpandByEnvironment, action: Scope.() -> R): R {
        val baseExample = examples.first()
        val selectedExample = examples.first { it.id == exampleId }

        return Scope(baseExample, selectedExample, VariableResolver(environment, resolveVariableMode)).action()
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
}

enum class ProtocolApplication {
    Http, WebSocket, Grpc, Graphql
}

@Persisted
@Serializable
data class UserRequestExample(
    override val id: String,
    val name: String,
    val contentType: ContentType = ContentType.None,
    val headers: List<UserKeyValuePair> = mutableListOf(),
    val queryParameters: List<UserKeyValuePair> = mutableListOf(),
    val body: UserRequestBody? = null,
    val postFlight: PostFlightSpec = PostFlightSpec(),
    val overrides: Overrides? = null, // only the Base example can be null
) : Identifiable {

    @Persisted
    @Serializable
    data class Overrides(
        val disabledHeaderIds: Set<String> = emptySet(),
        val disabledQueryParameterIds: Set<String> = emptySet(),

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

        val disablePostFlightUpdateVarIds: Set<String> = emptySet(),
    )

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
}

@Persisted
@Serializable
@SerialName("FormUrlEncodedBody")
class FormUrlEncodedBody(val value: List<UserKeyValuePair>) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody {
        val builder = FormBody.Builder()
        value.forEach { builder.add(it.key, it.value) }
        return builder.build()
    }
}

@Persisted
@Serializable
@SerialName("MultipartBody")
class MultipartBody(val value: List<UserKeyValuePair>) : UserRequestBody {
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
}

@Persisted
@Serializable
@SerialName("FileBody")
class FileBody(val filePath: String?) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType?): RequestBody {
        return filePath?.let { File(it).asRequestBody(mediaType) } ?: byteArrayOf().toRequestBody(mediaType)
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

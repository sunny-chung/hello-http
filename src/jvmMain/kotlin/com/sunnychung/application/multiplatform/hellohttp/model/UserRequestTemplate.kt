package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
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
    val protocol: Protocol = Protocol.Http,
    val method: String = "",
    val url: String = "",

    val examples: List<UserRequestExample> = listOf(UserRequestExample(id = uuidString(), name = "Base")),
) {
    init {
        if (method != method.trim().uppercase()) {
            throw IllegalArgumentException("`name` must be in upper case")
        }
    }

    data class Scope(val baseExample: UserRequestExample, val selectedExample: UserRequestExample, val environmentVariables: Map<String, String>) {
        fun String.expandVariables(): String {
            var s = this
            environmentVariables.forEach {
                s = s.replace("\${{${it.key}}}", it.value)
            }
            return s
        }

        fun getMergedKeyValues(
            propertyGetter: (UserRequestExample) -> List<UserKeyValuePair>?,
            disabledIds: Set<String>?
        ): List<UserKeyValuePair> {
            if (selectedExample.id == baseExample.id) { // the Base example is selected
                return propertyGetter(baseExample)?.filter { it.isEnabled }
                    ?.map { it.copy(key = it.key.expandVariables(), value = it.value.expandVariables()) }
                    ?: emptyList() // TODO reduce code duplication
            }

            val baseValues = (propertyGetter(baseExample) ?: emptyList())
                .filter { it.isEnabled && (disabledIds == null || !disabledIds.contains(it.id)) }

            val currentValues = (propertyGetter(selectedExample) ?: emptyList())
                .filter { it.isEnabled }

            return (baseValues + currentValues)
                .map { it.copy(key = it.key.expandVariables(), value = it.value.expandVariables()) }
        }
    }

    fun <R> withScope(exampleId: String, environment: Environment?, action: Scope.() -> R): R {
        val baseExample = examples.first()
        val selectedExample = examples.first { it.id == exampleId }

        val environmentVariables = environment?.variables
            ?.filter { it.isEnabled }
            ?.associate { it.key to it.value }
            ?: emptyMap()
        return Scope(baseExample, selectedExample, environmentVariables).action()
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

enum class Protocol {
    Http, Grpc, Graphql
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
        val isOverrideBody: Boolean = true, // only for raw body and its similar alternatives (e.g. JSON body)
        val disabledBodyKeyValueIds: Set<String> = emptySet(),

        val disablePostFlightUpdateVarIds: Set<String> = emptySet(),
    )
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

enum class ContentType(override val displayText: String, val headerValue: String?) : DropDownable {
    Json(displayText = "JSON", headerValue = "application/json"),
    Multipart(displayText = "Multipart Form", headerValue = "multipart/form-data"), // "multipart/form-data; boundary=<generated>"
    FormUrlEncoded(displayText = "Form URL-Encoded", headerValue = "application/x-www-form-urlencoded"),
    Raw(displayText = "Raw", headerValue = null),
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
)

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

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
data class UserRequest(
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
) : Identifiable

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
    val key: String,

    /**
     * If valueType = File, this value is a relative path
     */
    val value: String,

    val valueType: FieldValueType,
    val isEnabled: Boolean
)

enum class FieldValueType {
    String, File
}

@Persisted
@Serializable
sealed interface UserRequestBody {
    fun toOkHttpBody(mediaType: MediaType): RequestBody
}

@Persisted
@Serializable
@SerialName("StringBody")
class StringBody(val value: String) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType): RequestBody = value.toRequestBody(mediaType)
}

@Persisted
@Serializable
@SerialName("FormUrlEncodedBody")
class FormUrlEncodedBody(val value: List<UserKeyValuePair>) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType): RequestBody {
        val builder = FormBody.Builder()
        value.forEach { builder.add(it.key, it.value) }
        return builder.build()
    }
}

@Persisted
@Serializable
@SerialName("MultipartBody")
class MultipartBody(val value: List<UserKeyValuePair>) : UserRequestBody {
    override fun toOkHttpBody(mediaType: MediaType): RequestBody {
        val b = MultipartBody.Builder()
        value.forEach {
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

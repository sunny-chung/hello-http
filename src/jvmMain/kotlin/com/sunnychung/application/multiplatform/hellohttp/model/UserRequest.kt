package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable

data class UserRequest(
    val name: String,
    val protocol: Protocol,
    val method: String,
    val url: String,

    val examples: List<UserRequestExample>,
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

data class UserRequestExample(
    val name: String,
    val contentType: ContentType,
    val headers: List<UserKeyValuePair>,
    val queryParameters: List<UserKeyValuePair>,
    val body: UserRequestBody?,
)

//enum class ContentType {
//    None, Raw, Json, FormData, Multipart
//}

enum class ContentType(override val displayText: String, val headerValue: String?) : DropDownable {
    Json(displayText = "JSON", headerValue = "application/json"),
    Multipart(displayText = "Multipart Form", headerValue = "multipart/form-data; boundary=<generated>"),
    FormUrlEncoded(displayText = "Form URL-Encoded", headerValue = "application/x-www-form-urlencoded"),
    Raw(displayText = "Raw", headerValue = null),
    None(displayText = "None", headerValue = null),
}

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

interface UserRequestBody {

}

class StringBody(val value: String) : UserRequestBody

class FormUrlEncodedBody(val value: List<UserKeyValuePair>) : UserRequestBody

class MultipartBody(val value: List<UserKeyValuePair>) : UserRequestBody

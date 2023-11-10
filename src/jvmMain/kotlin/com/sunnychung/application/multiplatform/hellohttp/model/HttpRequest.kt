package com.sunnychung.application.multiplatform.hellohttp.model

data class HttpRequest(
    val method: String = "",
    val url: String = "",
    val headers: List<Pair<String, String>> = mutableListOf(),
    val queryParameters: List<Pair<String, String>> = mutableListOf(),
    val body: UserRequestBody? = null,
    val contentType: ContentType,
)

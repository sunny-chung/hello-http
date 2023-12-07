package com.sunnychung.application.multiplatform.hellohttp.model

data class GraphqlRequestBody(
    val query: String,
    val variables: Any?,
    val operationName: String?,
)

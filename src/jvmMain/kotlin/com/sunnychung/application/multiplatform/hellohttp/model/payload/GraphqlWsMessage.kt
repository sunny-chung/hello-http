package com.sunnychung.application.multiplatform.hellohttp.model.payload

data class GraphqlWsMessage<T>(
    val id: String? = null,
    val type: String,
    val payload: T? = null,
)

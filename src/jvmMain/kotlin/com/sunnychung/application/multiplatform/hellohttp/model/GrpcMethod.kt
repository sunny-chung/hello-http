package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class GrpcMethod(
    val serviceFullName: String,
    val methodName: String,
    val isClientStreaming: Boolean,
    val isServerStreaming: Boolean,
)

package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class SubprojectConfiguration(
    val id: String = uuidString(),
    val subprojectId: String,
    var outboundPayloadStorageLimit: Long = -1,
    var inboundPayloadStorageLimit: Long = -1,
    var accumulatedOutboundDataStorageLimitPerCall: Long = -1,
    var accumulatedInboundDataStorageLimitPerCall: Long = -1,
)

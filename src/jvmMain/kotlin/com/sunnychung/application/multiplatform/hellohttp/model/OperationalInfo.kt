package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class OperationalInfo(
    var appVersion: String,
    var installationId: String,
)

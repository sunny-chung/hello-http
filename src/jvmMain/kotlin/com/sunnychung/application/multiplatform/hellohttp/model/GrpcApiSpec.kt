package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class GrpcApiSpec(
    val id: String, // unique
    val name: String, // unique within a Subproject
    val methods: List<GrpcMethod>,
    val rawFileDescriptors: List<ByteArray>,
    val source: Source,
    val isActive: Boolean,
    val updateTime: KInstant,
) {
    enum class Source {
        Reflection, ProtoFiles
    }
}

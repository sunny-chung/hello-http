package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable

@DocumentRoot
@Persisted
@Serializable
data class DataDump(
    val schemaVersion: Int,
    val createdAt: KInstantAsLongCompat,
    val data: Data,
) {

    @Persisted
    @Serializable
    data class Data(
        val projects: List<Project>,
        // jackson-cbor doesn't support maps with non-primitive keys
        val requests: List<Pair<RequestsDI, List<UserRequestTemplate>>>,
        val responses: List<Pair<ResponsesDI, MutableMap<String, UserResponse>>>,
        val apiSpecs: List<Pair<ApiSpecDI, ApiSpecCollection>> = mutableListOf(),
    )
}

package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.application.multiplatform.hellohttp.annotation.DocumentRoot
import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Collections

@DocumentRoot
@Persisted
@Serializable
data class ApiSpecCollection(
    override val id: ApiSpecDI,
    @SerialName("grpcApiSpecs") private var _grpcApiSpecs: MutableList<GrpcApiSpec> = mutableListOf(),
) : Document<ApiSpecDI> {
    init {
        _grpcApiSpecs = Collections.synchronizedList(_grpcApiSpecs)
    }

    val grpcApiSpecs: MutableList<GrpcApiSpec>
        get() = _grpcApiSpecs
}

@Serializable
@Persisted
data class ApiSpecDI(val projectId: String) : DocumentIdentifier(type = PersistenceDocumentType.ApiSpecifications)

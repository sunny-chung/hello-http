package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import kotlinx.serialization.serializer

class ApiSpecificationCollectionRepository : BaseCollectionRepository<ApiSpecCollection, ApiSpecDI>(serializer()) {

    override fun relativeFilePath(id: ApiSpecDI): String = "apispec/apispec-${id.projectId}.db"
}
package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import kotlinx.serialization.serializer

class ResponseCollectionRepository : BaseCollectionRepository<ResponseCollection, ResponsesDI>(serializer()) {
    override fun relativeFilePath(id: ResponsesDI): String = "responses/resp-${id.subprojectId}.db"
}

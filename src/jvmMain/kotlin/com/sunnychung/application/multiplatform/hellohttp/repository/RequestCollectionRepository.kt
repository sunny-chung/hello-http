package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import kotlinx.serialization.serializer

class RequestCollectionRepository : BaseCollectionRepository<RequestCollection, RequestsDI>(serializer()) {
//    private val index = ConcurrentHashMap<String, UserRequest>()

    override fun relativeFilePath(id: RequestsDI): String = "requests/req-${id.subprojectId}.db"
}
package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDI
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDocument
import kotlinx.serialization.serializer

class OperationalRepository : BaseCollectionRepository<OperationalDocument, OperationalDI>(serializer()) {
    override fun relativeFilePath(id: OperationalDI): String = "operational.db"
}

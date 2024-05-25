package com.sunnychung.application.multiplatform.hellohttp.repository

import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.serialization.serializer

class ResponseCollectionRepository : BaseCollectionRepository<ResponseCollection, ResponsesDI>(serializer()) {
    override fun relativeFilePath(id: ResponsesDI): String = "responses/resp-${id.subprojectId}.db"

    override fun notifyUpdated(identifier: ResponsesDI) {
        if (log.config.minSeverity <= Severity.Verbose) {
            log.v(Throwable()) { "notifyUpdated" }
        }
        super.notifyUpdated(identifier)
    }
}

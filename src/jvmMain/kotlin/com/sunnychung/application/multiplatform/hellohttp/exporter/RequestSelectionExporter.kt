package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate

class RequestSelectionExporter {

    private val jsonWriter = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun exportAsJson(requests: List<UserRequestTemplate>): String {
        val payload = RequestSelectionExport(
            app = RequestSelectionExport.AppMetadata(
                name = "Hello HTTP",
                version = AppContext.MetadataManager.version,
            ),
            requests = requests.map {
                it.deepCopyWithNewId()
                    .sanitizeForApplication()
            },
        )
        return jsonWriter.writeValueAsString(payload)
    }
}

data class RequestSelectionExport(
    val app: AppMetadata,
    val requests: List<UserRequestTemplate>,
) {
    data class AppMetadata(
        val name: String,
        val version: String,
    )
}

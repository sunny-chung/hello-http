package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RequestSelectionExporter {

    private val jsonWriter = Json {
        explicitNulls = false
        encodeDefaults = true
    }

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
        return jsonWriter.encodeToString(RequestSelectionExport.serializer(), payload)
    }
}

@Serializable
data class RequestSelectionExport(
    val app: AppMetadata,
    val requests: List<UserRequestTemplate>,
) {
    @Serializable
    data class AppMetadata(
        val name: String,
        val version: String,
    )
}

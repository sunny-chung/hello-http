package com.sunnychung.application.multiplatform.hellohttp.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.exporter.RequestSelectionExporter
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RequestSelectionExporterTest {

    @Test
    fun exportJsonShouldContainAppMetadataAndRequestList() {
        val request = UserRequestTemplate(
            id = "req-1",
            name = "List Users",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com/users",
            examples = listOf(UserRequestExample(id = "example-1", name = "Base")),
        )

        val exportedJson = RequestSelectionExporter().exportAsJson(listOf(request))
        val exportedObject = jacksonObjectMapper().readTree(exportedJson)

        assertFalse(exportedJson.contains('\n'))
        assertEquals("Hello HTTP", exportedObject["app"]["name"].asText())
        assertEquals(AppContext.MetadataManager.version, exportedObject["app"]["version"].asText())
        assertEquals(1, exportedObject["requests"].size())
        assertEquals("req-1", exportedObject["requests"][0]["id"].asText())
        assertEquals("List Users", exportedObject["requests"][0]["name"].asText())
    }
}

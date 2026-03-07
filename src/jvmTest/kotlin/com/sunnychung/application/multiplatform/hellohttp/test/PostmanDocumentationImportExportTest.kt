package com.sunnychung.application.multiplatform.hellohttp.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.exporter.PostmanV2MultiFileExporter
import com.sunnychung.application.multiplatform.hellohttp.importer.PostmanV2ZipImporter
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.postmanv2.PostmanV2
import kotlin.test.Test
import kotlin.test.assertEquals

class PostmanDocumentationImportExportTest {

    @Test
    fun exporterShouldWriteDocumentationToItemAndRequestDescriptions() {
        val exporter = PostmanV2MultiFileExporter()
        val documentation = "API notes for this request"
        val request = UserRequestTemplate(
            id = "request-1",
            name = "Sample",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com",
            examples = listOf(
                UserRequestExample(
                    id = "example-1",
                    name = "Base",
                    documentation = documentation,
                )
            ),
        )

        val exported = exporter.requestToItem(request).first()

        assertEquals(documentation, exported.description?.asText())
        assertEquals(documentation, exported.request?.description?.asText())
    }

    @Test
    fun importerShouldReadDocumentationFromItemDescriptionContent() {
        val importer = PostmanV2ZipImporter()
        val jsonMapper = jacksonObjectMapper()
        val item = PostmanV2.Item(
            id = "item-1",
            name = "Get Users",
            description = jsonMapper.valueToTree(mapOf("content" to "Imported docs from item description")),
            request = PostmanV2.Request(
                method = "GET",
                header = emptyList(),
                auth = null,
                body = null,
                url = PostmanV2.Url(raw = "https://example.com/users", query = null),
                description = jsonMapper.valueToTree("request-level description"),
            ),
        )

        val imported = importer.run { item.toUserRequest(emptyList()) }

        assertEquals("Imported docs from item description", imported.examples.first().documentation)
    }

    @Test
    fun importerShouldFallbackToRequestDescriptionWhenItemDescriptionMissing() {
        val importer = PostmanV2ZipImporter()
        val jsonMapper = jacksonObjectMapper()
        val item = PostmanV2.Item(
            id = "item-2",
            name = "Get Orders",
            description = null,
            request = PostmanV2.Request(
                method = "GET",
                header = emptyList(),
                auth = null,
                body = null,
                url = PostmanV2.Url(raw = "https://example.com/orders", query = null),
                description = jsonMapper.valueToTree("request-level documentation"),
            ),
        )

        val imported = importer.run { item.toUserRequest(emptyList()) }

        assertEquals("request-level documentation", imported.examples.first().documentation)
    }
}

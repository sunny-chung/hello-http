package com.sunnychung.application.multiplatform.hellohttp.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.exporter.OpenApiV32Exporter
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiV32ExporterTest {

    @Test
    fun shouldMapRequestExampleDocumentationToOpenApiExampleDescriptions() {
        val baseExample = UserRequestExample(
            id = "example-base",
            name = "Base",
            contentType = ContentType.Json,
            queryParameters = listOf(
                kv(id = "q-base", key = "q", value = "base"),
            ),
            body = StringBody("{\"name\":\"base\"}"),
            documentation = "Base documentation",
        )
        val variantExample = UserRequestExample(
            id = "example-variant",
            name = "Variant",
            contentType = ContentType.Json,
            queryParameters = listOf(
                kv(id = "q-variant", key = "q", value = "variant"),
            ),
            body = StringBody("{\"name\":\"variant\"}"),
            documentation = "Variant documentation",
            overrides = UserRequestExample.Overrides(
                isOverrideDocumentation = true,
            ),
        )
        val request = UserRequestTemplate(
            id = "request-1",
            name = "Create User",
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://api.example.com/users",
            examples = listOf(baseExample, variantExample),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
        )
        val document = jacksonObjectMapper().readTree(json)
        val operation = document["paths"]["/users"]["post"]

        assertEquals("Base documentation", operation["description"].asText())

        val requestBodyExamples = operation["requestBody"]["content"]["application/json"]["examples"]
            .elementsAsSequence()
            .mapNotNull { it["description"]?.asText() }
            .toSet()
        assertTrue("Variant documentation" in requestBodyExamples)
        assertFalse("Base documentation" in requestBodyExamples)

        val queryParameter = operation["parameters"]
            .elementsAsSequence()
            .firstOrNull { it["in"].asText() == "query" && it["name"].asText() == "q" }
        assertNotNull(queryParameter)
        assertEquals(2, queryParameter["examples"].size())
        assertEquals(1, "\"Base documentation\"".toRegex().findAll(json).count())
        assertEquals(1, "\"Variant documentation\"".toRegex().findAll(json).count())
    }

    @Test
    fun shouldResolveOverridesAndOmitDisabledItems() {
        val baseExample = UserRequestExample(
            id = "example-base",
            name = "Base",
            contentType = ContentType.FormUrlEncoded,
            headers = listOf(
                kv(id = "h-keep", key = "X-Keep", value = "base"),
                kv(id = "h-disabled", key = "X-Disabled", value = "base"),
            ),
            queryParameters = listOf(
                kv(id = "q-keep", key = "keep", value = "1"),
                kv(id = "q-disabled", key = "remove", value = "1"),
            ),
            body = FormUrlEncodedBody(
                listOf(
                    kv(id = "b-keep", key = "keep", value = "A"),
                    kv(id = "b-disabled", key = "remove", value = "B"),
                )
            ),
            documentation = "Base docs",
        )
        val variantExample = UserRequestExample(
            id = "example-variant",
            name = "Variant",
            contentType = ContentType.FormUrlEncoded,
            headers = listOf(
                kv(id = "h-new", key = "X-New", value = "variant"),
            ),
            queryParameters = listOf(
                kv(id = "q-new", key = "new", value = "2"),
            ),
            body = FormUrlEncodedBody(
                listOf(
                    kv(id = "b-new", key = "new", value = "C"),
                )
            ),
            documentation = "Variant docs",
            overrides = UserRequestExample.Overrides(
                disabledHeaderIds = setOf("h-disabled"),
                disabledQueryParameterIds = setOf("q-disabled"),
                disabledBodyKeyValueIds = setOf("b-disabled"),
                isOverrideDocumentation = true,
            ),
        )
        val request = UserRequestTemplate(
            id = "request-1",
            name = "Submit",
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://api.example.com/forms",
            examples = listOf(baseExample, variantExample),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
        )
        val document = jacksonObjectMapper().readTree(json)
        val operation = document["paths"]["/forms"]["post"]

        val disabledHeader = operation["parameters"]
            .elementsAsSequence()
            .firstOrNull { it["in"].asText() == "header" && it["name"].asText() == "X-Disabled" }
        assertNotNull(disabledHeader)
        assertEquals(1, disabledHeader["examples"].size())

        val variantBodyValue = operation["requestBody"]["content"]["application/x-www-form-urlencoded"]["examples"]
            .elementsAsSequence()
            .first { it["summary"].asText() == "Variant" }["value"]
        assertTrue(variantBodyValue.has("keep"))
        assertTrue(variantBodyValue.has("new"))
        assertFalse(variantBodyValue.has("remove"))

        assertFalse(json.contains("disabledHeaderIds"))
        assertFalse(json.contains("disabledQueryParameterIds"))
        assertFalse(json.contains("disabledBodyKeyValueIds"))
        assertFalse(json.contains("isOverrideBody"))
    }

    @Test
    fun shouldCreateEmptyBodyExamplesForDocumentationOnlyExamples() {
        val request = UserRequestTemplate(
            id = "request-1",
            name = "Health",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://api.example.com/health",
            examples = listOf(
                UserRequestExample(
                    id = "example-base",
                    name = "Base",
                    documentation = "Base docs",
                ),
                UserRequestExample(
                    id = "example-variant",
                    name = "Variant",
                    documentation = "Variant docs",
                    overrides = UserRequestExample.Overrides(
                        isOverrideDocumentation = true,
                    ),
                ),
            ),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
        )
        val document = jacksonObjectMapper().readTree(json)
        val operation = document["paths"]["/health"]["get"]

        val examples = operation["requestBody"]["content"]["text/plain"]["examples"]
            .elementsAsSequence()
            .toList()
        assertEquals(2, examples.size)
        assertTrue(examples.all { it["value"].asText() == "" })
        assertTrue(examples.any { it["description"]?.asText() == "Variant docs" })
        assertEquals("Base docs", operation["description"].asText())
    }

    @Test
    fun shouldOmitInheritedDocumentationInNonBaseExamples() {
        val request = UserRequestTemplate(
            id = "request-1",
            name = "Docs Test",
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://api.example.com/docs",
            examples = listOf(
                UserRequestExample(
                    id = "example-base",
                    name = "Base",
                    contentType = ContentType.Json,
                    body = StringBody("{\"a\":1}"),
                    documentation = "Base docs",
                ),
                UserRequestExample(
                    id = "example-variant",
                    name = "Variant",
                    contentType = ContentType.Json,
                    body = StringBody("{\"a\":2}"),
                    documentation = "Variant should not be exported",
                    overrides = UserRequestExample.Overrides(
                        isOverrideDocumentation = false,
                    ),
                ),
            ),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
        )
        val document = jacksonObjectMapper().readTree(json)
        val operation = document["paths"]["/docs"]["post"]

        assertEquals("Base docs", operation["description"].asText())
        assertFalse(json.contains("Variant should not be exported"))
    }

    @Test
    fun shouldIncludeResponseExamplesForRequestExamples() {
        val baseExample = UserRequestExample(
            id = "example-base",
            name = "Base",
            contentType = ContentType.Json,
            body = StringBody("{\"x\":1}"),
            documentation = "Base docs",
        )
        val variantExample = UserRequestExample(
            id = "example-variant",
            name = "Variant",
            contentType = ContentType.Json,
            body = StringBody("{\"x\":2}"),
            documentation = "Variant docs",
            overrides = UserRequestExample.Overrides(
                isOverrideDocumentation = true,
            ),
        )
        val request = UserRequestTemplate(
            id = "request-1",
            name = "With Responses",
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://api.example.com/resp",
            examples = listOf(baseExample, variantExample),
        )
        val baseResponse = com.sunnychung.application.multiplatform.hellohttp.model.UserResponse(
            id = "resp-base",
            requestId = request.id,
            requestExampleId = baseExample.id,
            statusCode = 200,
            statusText = "OK",
            headers = listOf("Content-Type" to "application/json"),
            body = "{\"ok\":true}".toByteArray(),
        )
        val variantResponse = com.sunnychung.application.multiplatform.hellohttp.model.UserResponse(
            id = "resp-variant",
            requestId = request.id,
            requestExampleId = variantExample.id,
            statusCode = 400,
            statusText = "Bad Request",
            headers = listOf("Content-Type" to "text/plain"),
            body = "bad request".toByteArray(),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
            responsesBySubprojectId = mapOf(
                subproject.id to mapOf(
                    baseExample.id to baseResponse,
                    variantExample.id to variantResponse,
                )
            ),
        )
        val document = jacksonObjectMapper().readTree(json)
        val operation = document["paths"]["/resp"]["post"]

        assertTrue(operation["responses"].has("200"))
        assertTrue(operation["responses"].has("400"))
        val response200Example = operation["responses"]["200"]["content"]["application/json"]["examples"]
            .elementsAsSequence()
            .firstOrNull()
        val response400Example = operation["responses"]["400"]["content"]["text/plain"]["examples"]
            .elementsAsSequence()
            .firstOrNull()
        assertNotNull(response200Example)
        assertNotNull(response400Example)
        assertEquals("Base", response200Example["summary"].asText())
        assertEquals("Variant", response400Example["summary"].asText())
    }

    @Test
    fun shouldSkipGraphqlSubscriptionRequests() {
        val request = UserRequestTemplate(
            id = "request-graphql",
            name = "Subscription Request",
            application = ProtocolApplication.Graphql,
            method = "",
            url = "https://api.example.com/graphql",
            examples = listOf(
                UserRequestExample(
                    id = "example-base",
                    name = "Base",
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = "subscription Ping { ping }",
                        variables = "{}",
                    ),
                    documentation = "Subscription docs",
                ),
            ),
        )
        val (project, subproject) = createProjectWithSubprojectAndTree(request.id)

        val json = OpenApiV32Exporter().exportAsJson(
            project = project,
            requestsBySubprojectId = mapOf(subproject.id to listOf(request)),
        )
        val document = jacksonObjectMapper().readTree(json)

        assertEquals(0, document["paths"].size())
        val skippedRequests = document["x-hello-http"]["skippedRequests"]
        assertEquals(1, skippedRequests.size())
        assertTrue(skippedRequests[0]["reason"].asText().contains("GraphQL subscriptions"))
    }

    private fun createProjectWithSubprojectAndTree(requestId: String): Pair<Project, Subproject> {
        val subproject = Subproject(
            id = "subproject-1",
            name = "Subproject",
            treeObjects = mutableListOf(TreeRequest(requestId)),
            environments = mutableListOf(),
        )
        val project = Project(
            id = "project-1",
            name = "Project",
            subprojects = mutableListOf(subproject),
        )
        return Pair(project, subproject)
    }

    private fun kv(
        id: String,
        key: String,
        value: String,
        valueType: FieldValueType = FieldValueType.String,
        isEnabled: Boolean = true,
    ): UserKeyValuePair {
        return UserKeyValuePair(
            id = id,
            key = key,
            value = value,
            valueType = valueType,
            isEnabled = isEnabled,
        )
    }
}

private fun JsonNode.elementsAsSequence() = elements().asSequence()

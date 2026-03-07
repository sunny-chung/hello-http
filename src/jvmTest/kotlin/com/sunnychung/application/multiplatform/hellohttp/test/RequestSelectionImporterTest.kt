package com.sunnychung.application.multiplatform.hellohttp.test

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.exporter.RequestSelectionExporter
import com.sunnychung.application.multiplatform.hellohttp.importer.RequestSelectionImporter
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestSelectionImporterTest {

    private val jsonMapper = jacksonObjectMapper()

    @Test
    fun importShouldPreserveFieldsAndRenewIdsForAllProtocolsAndBodyTypes() {
        val exporter = RequestSelectionExporter()
        val importer = RequestSelectionImporter()

        protocolBodyPairs().forEach { (application, bodyType) ->
            val request = createRequest(application = application, bodyType = bodyType)
            val exportedJson = exporter.exportAsJson(listOf(request))

            val imported = importer.importFromJson(exportedJson)

            assertEquals(1, imported.requests.size)
            assertTrue(imported.appVersion.isNotBlank())
            val actual = imported.requests.first()

            assertRequestSemanticsEqual(expected = request, actual = actual)
            assertAllIdsRenewed(
                expected = request,
                actual = actual,
                context = "$application/$bodyType",
            )
            assertOverrideReferencesAreValid(actual, "$application/$bodyType")
        }
    }

    @Test
    fun importShouldRejectMalformedJson() {
        assertFailsWith<IllegalArgumentException> {
            RequestSelectionImporter().importFromJson("{invalid")
        }
    }

    @Test
    fun importShouldRejectInvalidBodyInsteadOfNormalizing() {
        val request = createRequest(application = ProtocolApplication.Http, bodyType = ContentType.Json)
        val payload = exportPayloadNode(request)
        val firstExample = payload
            .withArray("requests")
            .first() as ObjectNode
        val exampleNode = firstExample
            .withArray("examples")
            .first() as ObjectNode
        exampleNode.put("contentType", ContentType.Multipart.name)

        val json = jsonMapper.writeValueAsString(payload)
        assertFailsWith<IllegalArgumentException> {
            RequestSelectionImporter().importFromJson(json)
        }
    }

    @Test
    fun importShouldRejectBrokenOverrideReferences() {
        val request = createRequest(application = ProtocolApplication.Http, bodyType = ContentType.FormUrlEncoded)
        val payload = exportPayloadNode(request)
        val firstRequest = payload
            .withArray("requests")
            .first() as ObjectNode
        val secondExample = firstRequest
            .withArray("examples")
            .get(1) as ObjectNode
        val overridesNode = secondExample.with("overrides")
        val disabledHeaderIds = overridesNode.withArray("disabledHeaderIds")
        disabledHeaderIds.removeAll()
        disabledHeaderIds.add("unknown-header-id")

        val json = jsonMapper.writeValueAsString(payload)
        assertFailsWith<IllegalArgumentException> {
            RequestSelectionImporter().importFromJson(json)
        }
    }

    @Test
    fun importShouldRejectMissingGrpcPayloadExamples() {
        val request = createRequest(application = ProtocolApplication.Grpc, bodyType = ContentType.Json)
        val payload = exportPayloadNode(request)
        val firstRequest = payload
            .withArray("requests")
            .first() as ObjectNode
        firstRequest.remove("payloadExamples")

        val json = jsonMapper.writeValueAsString(payload)
        assertFailsWith<IllegalArgumentException> {
            RequestSelectionImporter().importFromJson(json)
        }
    }

    @Test
    fun importShouldExposeSourceAppVersion() {
        val request = createRequest(application = ProtocolApplication.Http, bodyType = ContentType.Raw)
        val payload = exportPayloadNode(request)
        val appNode = payload.with("app")
        appNode.put("version", "999.0.0")

        val imported = RequestSelectionImporter().importFromJson(jsonMapper.writeValueAsString(payload))
        assertEquals("999.0.0", imported.appVersion)
    }

    @Test
    fun importShouldDefaultMissingDocumentationToEmptyString() {
        val request = createRequest(application = ProtocolApplication.Http, bodyType = ContentType.Json)
        val payload = exportPayloadNode(request)
        val firstRequest = payload
            .withArray("requests")
            .first() as ObjectNode
        val examples = firstRequest.withArray("examples")
        (examples.get(0) as ObjectNode).remove("documentation")
        (examples.get(1) as ObjectNode).remove("documentation")

        val imported = RequestSelectionImporter().importFromJson(jsonMapper.writeValueAsString(payload))
        assertEquals("", imported.requests.first().examples[0].documentation)
        assertEquals("", imported.requests.first().examples[1].documentation)
    }

    @Test
    fun importShouldAllowMissingBinaryFilePathField() {
        val request = createRequest(application = ProtocolApplication.Http, bodyType = ContentType.BinaryFile)
        val payload = exportPayloadNode(request)
        val firstRequest = payload
            .withArray("requests")
            .first() as ObjectNode
        val examples = firstRequest.withArray("examples")
        (examples.get(0) as ObjectNode).with("body").remove("filePath")
        (examples.get(1) as ObjectNode).with("body").remove("filePath")

        val imported = RequestSelectionImporter().importFromJson(jsonMapper.writeValueAsString(payload))
        assertTrue(imported.requests.first().examples.all { (it.body as FileBody).filePath == null })
    }

    @Test
    fun importShouldAllowMissingGraphqlOperationNameField() {
        val request = createRequest(application = ProtocolApplication.Graphql, bodyType = ContentType.Graphql)
        val payload = exportPayloadNode(request)
        val firstRequest = payload
            .withArray("requests")
            .first() as ObjectNode
        val examples = firstRequest.withArray("examples")
        (examples.get(0) as ObjectNode).with("body").remove("operationName")
        (examples.get(1) as ObjectNode).with("body").remove("operationName")

        val imported = RequestSelectionImporter().importFromJson(jsonMapper.writeValueAsString(payload))
        imported.requests.first().examples.forEach {
            assertTrue(it.body is GraphqlBody)
            assertEquals(null, (it.body as GraphqlBody).operationName)
        }
    }

    @Test
    fun readMetadataShouldSucceedBeforeFullImportParsing() {
        val json = """
            {"app":{"name":"Hello HTTP","version":"999.0.0"},"requests":[{"unsupported":"field-shape"}]}
        """.trimIndent()

        val importer = RequestSelectionImporter()
        val metadata = importer.readMetadata(json)
        assertEquals("Hello HTTP", metadata.appName)
        assertEquals("999.0.0", metadata.appVersion)
        assertFailsWith<IllegalArgumentException> {
            importer.importFromJson(json)
        }
    }

    private fun protocolBodyPairs(): List<Pair<ProtocolApplication, ContentType>> {
        return ProtocolApplication.values().flatMap { protocol ->
            when (protocol) {
                ProtocolApplication.Http -> ContentType.values().map { protocol to it }
                ProtocolApplication.WebSocket -> listOf(protocol to ContentType.None)
                ProtocolApplication.Grpc -> listOf(protocol to ContentType.Json)
                ProtocolApplication.Graphql -> listOf(protocol to ContentType.Graphql)
            }
        }
    }

    private fun createRequest(application: ProtocolApplication, bodyType: ContentType): UserRequestTemplate {
        fun kv(
            id: String,
            key: String,
            value: String,
            valueType: FieldValueType = FieldValueType.String,
            isEnabled: Boolean = true,
        ) = UserKeyValuePair(
            id = id,
            key = key,
            value = value,
            valueType = valueType,
            isEnabled = isEnabled,
        )

        val baseBody = createBody("base", bodyType)
        val baseExample = UserRequestExample(
            id = "example-base",
            name = "Base",
            contentType = bodyType,
            headers = listOf(
                kv("h1", "X-A", "1"),
                kv("h2", "X-B", "2"),
            ),
            cookies = listOf(
                kv("c1", "sid", "abc"),
                kv("c2", "theme", "dark"),
            ),
            queryParameters = listOf(
                kv("q1", "page", "1"),
                kv("q2", "size", "20"),
            ),
            body = baseBody,
            variables = listOf(
                kv("v1", "baseUrl", "https://example.com"),
                kv("v2", "token", "abc123"),
            ),
            preFlight = PreFlightSpec(
                executeCode = "println(\"pre\")",
                updateVariablesFromHeader = listOf(kv("pfh1", "auth", "Authorization")),
                updateVariablesFromQueryParameters = listOf(kv("pfq1", "page", "page")),
                updateVariablesFromBody = listOf(kv("pfb1", "userId", "$.id")),
                updateVariablesFromGraphqlVariables = listOf(kv("pfg1", "gqlVar", "$.variables.id")),
            ),
            postFlight = PostFlightSpec(
                updateVariablesFromHeader = listOf(kv("poh1", "respEtags", "ETag")),
                updateVariablesFromBody = listOf(kv("pob1", "respId", "$.id")),
            ),
            documentation = """
# Base Example

This is the base request documentation.
""".trimIndent(),
            overrides = null,
        )

        val baseBodyKeyIds = (baseBody as? RequestBodyWithKeyValuePairs)?.value?.map { it.id } ?: emptyList()
        val secondaryExample = UserRequestExample(
            id = "example-2",
            name = "Secondary",
            contentType = bodyType,
            headers = listOf(kv("h3", "X-C", "3")),
            cookies = listOf(kv("c3", "lang", "en")),
            queryParameters = listOf(kv("q3", "sort", "name")),
            body = createBody("secondary", bodyType),
            variables = listOf(kv("v3", "region", "us")),
            preFlight = PreFlightSpec(
                executeCode = "println(\"pre2\")",
                updateVariablesFromHeader = listOf(kv("pfh2", "auth2", "Authorization")),
                updateVariablesFromQueryParameters = listOf(kv("pfq2", "page2", "page")),
                updateVariablesFromBody = listOf(kv("pfb2", "userId2", "$.id")),
                updateVariablesFromGraphqlVariables = listOf(kv("pfg2", "gqlVar2", "$.variables.id")),
            ),
            postFlight = PostFlightSpec(
                updateVariablesFromHeader = listOf(kv("poh2", "respEtags2", "ETag")),
                updateVariablesFromBody = listOf(kv("pob2", "respId2", "$.id")),
            ),
            documentation = """
## Secondary Example

This example overrides runtime behavior.
""".trimIndent(),
            overrides = UserRequestExample.Overrides(
                disabledHeaderIds = setOf("h1"),
                disabledCookieIds = setOf("c1"),
                disabledQueryParameterIds = setOf("q1"),
                disabledVariables = setOf("v1"),
                disabledBodyKeyValueIds = baseBodyKeyIds.take(1).toSet(),
                disablePreFlightUpdateVarIds = setOf("pfh1", "pfq1"),
                disablePostFlightUpdateVarIds = setOf("poh1", "pob1"),
                isOverrideBody = true,
                isOverrideBodyContent = true,
                isOverrideBodyVariables = true,
                isOverridePreFlightScript = true,
            ),
        )

        return UserRequestTemplate(
            id = "request-1",
            name = "Request ${application.name} ${bodyType.name}",
            application = application,
            method = when (application) {
                ProtocolApplication.Http -> "PUT"
                ProtocolApplication.WebSocket -> "GET"
                ProtocolApplication.Grpc -> "POST"
                ProtocolApplication.Graphql -> "POST"
            },
            url = "https://example.com/${application.name.lowercase()}",
            grpc = if (application == ProtocolApplication.Grpc) {
                UserGrpcRequest(
                    apiSpecId = "spec-1",
                    service = "hello.UserService",
                    method = "GetUser",
                )
            } else {
                null
            },
            examples = listOf(baseExample, secondaryExample),
            payloadExamples = if (application in setOf(ProtocolApplication.WebSocket, ProtocolApplication.Grpc)) {
                listOf(
                    PayloadExample(id = "payload-1", name = "Payload A", body = "{\"a\":1}"),
                    PayloadExample(id = "payload-2", name = "Payload B", body = "{\"b\":2}"),
                )
            } else {
                null
            },
        )
    }

    private fun createBody(prefix: String, contentType: ContentType): UserRequestBody? {
        fun kv(id: String, key: String, value: String, valueType: FieldValueType = FieldValueType.String) =
            UserKeyValuePair(
                id = id,
                key = key,
                value = value,
                valueType = valueType,
                isEnabled = true,
            )

        return when (contentType) {
            ContentType.None -> null
            ContentType.Json -> StringBody("""{"$prefix":true}""")
            ContentType.Raw -> StringBody("raw-$prefix")
            ContentType.FormUrlEncoded -> FormUrlEncodedBody(
                listOf(
                    kv("${prefix}-f1", "f1", "v1"),
                    kv("${prefix}-f2", "f2", "v2"),
                )
            )
            ContentType.Multipart -> MultipartBody(
                listOf(
                    kv("${prefix}-m1", "m1", "v1"),
                    kv("${prefix}-m2", "file", "/tmp/demo.txt", FieldValueType.File),
                )
            )
            ContentType.BinaryFile -> FileBody("/tmp/$prefix.bin")
            ContentType.Graphql -> GraphqlBody(
                document = "query GetUser { user { id } }",
                variables = """{"id":"1"}""",
                operationName = "GetUser",
            )
        }
    }

    private fun assertRequestSemanticsEqual(expected: UserRequestTemplate, actual: UserRequestTemplate) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.application, actual.application)
        assertEquals(expected.method, actual.method)
        assertEquals(expected.url, actual.url)
        assertEquals(expected.grpc, actual.grpc)

        assertEquals(expected.examples.size, actual.examples.size)
        expected.examples.zip(actual.examples).forEach { (exp, act) ->
            assertEquals(exp.name, act.name)
            assertEquals(exp.contentType, act.contentType)
            assertKeyValueListEqual(exp.headers, act.headers)
            assertKeyValueListEqual(exp.cookies, act.cookies)
            assertKeyValueListEqual(exp.queryParameters, act.queryParameters)
            assertBodyEqual(exp.body, act.body)
            assertKeyValueListEqual(exp.variables, act.variables)
            assertEquals(exp.preFlight.executeCode, act.preFlight.executeCode)
            assertKeyValueListEqual(exp.preFlight.updateVariablesFromHeader, act.preFlight.updateVariablesFromHeader)
            assertKeyValueListEqual(exp.preFlight.updateVariablesFromQueryParameters, act.preFlight.updateVariablesFromQueryParameters)
            assertKeyValueListEqual(exp.preFlight.updateVariablesFromBody, act.preFlight.updateVariablesFromBody)
            assertKeyValueListEqual(exp.preFlight.updateVariablesFromGraphqlVariables, act.preFlight.updateVariablesFromGraphqlVariables)
            assertKeyValueListEqual(exp.postFlight.updateVariablesFromHeader, act.postFlight.updateVariablesFromHeader)
            assertKeyValueListEqual(exp.postFlight.updateVariablesFromBody, act.postFlight.updateVariablesFromBody)
            assertEquals(exp.documentation, act.documentation)
            assertOverridesEquivalent(exp.overrides, act.overrides)
        }

        val expectedPayloads = expected.payloadExamples ?: emptyList()
        val actualPayloads = actual.payloadExamples ?: emptyList()
        assertEquals(expectedPayloads.size, actualPayloads.size)
        expectedPayloads.zip(actualPayloads).forEach { (exp, act) ->
            assertEquals(exp.name, act.name)
            assertEquals(exp.body, act.body)
        }
    }

    private fun assertKeyValueListEqual(expected: List<UserKeyValuePair>, actual: List<UserKeyValuePair>) {
        assertEquals(
            expected.map { listOf(it.key, it.value, it.valueType.name, it.isEnabled.toString()) },
            actual.map { listOf(it.key, it.value, it.valueType.name, it.isEnabled.toString()) },
        )
    }

    private fun assertBodyEqual(expected: UserRequestBody?, actual: UserRequestBody?) {
        when (expected) {
            null -> assertNull(actual)
            is StringBody -> {
                assertTrue(actual is StringBody)
                assertEquals(expected.value, actual.value)
            }
            is FormUrlEncodedBody -> {
                assertTrue(actual is FormUrlEncodedBody)
                assertKeyValueListEqual(expected.value, actual.value)
            }
            is MultipartBody -> {
                assertTrue(actual is MultipartBody)
                assertKeyValueListEqual(expected.value, actual.value)
            }
            is FileBody -> {
                assertTrue(actual is FileBody)
                assertEquals(expected.filePath, actual.filePath)
            }
            is GraphqlBody -> {
                assertTrue(actual is GraphqlBody)
                assertEquals(expected, actual)
            }
        }
    }

    private fun assertOverridesEquivalent(
        expected: UserRequestExample.Overrides?,
        actual: UserRequestExample.Overrides?,
    ) {
        if (expected == null) {
            assertNull(actual)
            return
        }
        val actualNonNull = assertNotNull(actual)
        assertEquals(expected.isOverrideBody, actualNonNull.isOverrideBody)
        assertEquals(expected.isOverrideBodyContent, actualNonNull.isOverrideBodyContent)
        assertEquals(expected.isOverrideBodyVariables, actualNonNull.isOverrideBodyVariables)
        assertEquals(expected.isOverridePreFlightScript, actualNonNull.isOverridePreFlightScript)
        assertEquals(expected.isOverrideDocumentation, actualNonNull.isOverrideDocumentation)
        assertEquals(expected.disabledHeaderIds.size, actualNonNull.disabledHeaderIds.size)
        assertEquals(expected.disabledCookieIds.size, actualNonNull.disabledCookieIds.size)
        assertEquals(expected.disabledQueryParameterIds.size, actualNonNull.disabledQueryParameterIds.size)
        assertEquals(expected.disabledVariables.size, actualNonNull.disabledVariables.size)
        assertEquals(expected.disabledBodyKeyValueIds.size, actualNonNull.disabledBodyKeyValueIds.size)
        assertEquals(expected.disablePreFlightUpdateVarIds.size, actualNonNull.disablePreFlightUpdateVarIds.size)
        assertEquals(expected.disablePostFlightUpdateVarIds.size, actualNonNull.disablePostFlightUpdateVarIds.size)
    }

    private fun assertAllIdsRenewed(expected: UserRequestTemplate, actual: UserRequestTemplate, context: String) {
        val expectedIds = collectAllIds(expected)
        collectAllIds(actual).forEach {
            assertTrue(it !in expectedIds, "ID is not renewed in $context: $it")
        }
    }

    private fun collectAllIds(request: UserRequestTemplate): Set<String> {
        return buildSet {
            add(request.id)
            request.examples.forEach { example ->
                add(example.id)
                example.headers.forEach { add(it.id) }
                example.cookies.forEach { add(it.id) }
                example.queryParameters.forEach { add(it.id) }
                example.variables.forEach { add(it.id) }
                (example.body as? RequestBodyWithKeyValuePairs)?.value?.forEach { add(it.id) }
                example.preFlight.updateVariablesFromHeader.forEach { add(it.id) }
                example.preFlight.updateVariablesFromQueryParameters.forEach { add(it.id) }
                example.preFlight.updateVariablesFromBody.forEach { add(it.id) }
                example.preFlight.updateVariablesFromGraphqlVariables.forEach { add(it.id) }
                example.postFlight.updateVariablesFromHeader.forEach { add(it.id) }
                example.postFlight.updateVariablesFromBody.forEach { add(it.id) }
                example.overrides?.disabledHeaderIds?.forEach { add(it) }
                example.overrides?.disabledCookieIds?.forEach { add(it) }
                example.overrides?.disabledQueryParameterIds?.forEach { add(it) }
                example.overrides?.disabledVariables?.forEach { add(it) }
                example.overrides?.disabledBodyKeyValueIds?.forEach { add(it) }
                example.overrides?.disablePreFlightUpdateVarIds?.forEach { add(it) }
                example.overrides?.disablePostFlightUpdateVarIds?.forEach { add(it) }
            }
            request.payloadExamples?.forEach { add(it.id) }
        }
    }

    private fun assertOverrideReferencesAreValid(request: UserRequestTemplate, context: String) {
        val baseExample = request.examples.first()
        val baseHeaderIds = baseExample.headers.map { it.id }.toSet()
        val baseCookieIds = baseExample.cookies.map { it.id }.toSet()
        val baseQueryIds = baseExample.queryParameters.map { it.id }.toSet()
        val baseVariableIds = baseExample.variables.map { it.id }.toSet()
        val baseBodyIds = (baseExample.body as? RequestBodyWithKeyValuePairs)?.value?.map { it.id }?.toSet() ?: emptySet()
        val basePreFlightIds = with(baseExample.preFlight) {
            updateVariablesFromHeader + updateVariablesFromQueryParameters + updateVariablesFromBody + updateVariablesFromGraphqlVariables
        }.map { it.id }.toSet()
        val basePostFlightIds = with(baseExample.postFlight) {
            updateVariablesFromHeader + updateVariablesFromBody
        }.map { it.id }.toSet()

        request.examples.drop(1).forEach { example ->
            val overrides = example.overrides ?: return@forEach
            assertTrue(overrides.disabledHeaderIds.all { it in baseHeaderIds }, "Broken header override IDs in $context")
            assertTrue(overrides.disabledCookieIds.all { it in baseCookieIds }, "Broken cookie override IDs in $context")
            assertTrue(overrides.disabledQueryParameterIds.all { it in baseQueryIds }, "Broken query override IDs in $context")
            assertTrue(overrides.disabledVariables.all { it in baseVariableIds }, "Broken variable override IDs in $context")
            assertTrue(overrides.disabledBodyKeyValueIds.all { it in baseBodyIds }, "Broken body override IDs in $context")
            assertTrue(overrides.disablePreFlightUpdateVarIds.all { it in basePreFlightIds }, "Broken pre-flight override IDs in $context")
            assertTrue(overrides.disablePostFlightUpdateVarIds.all { it in basePostFlightIds }, "Broken post-flight override IDs in $context")
        }
    }

    private fun exportPayloadNode(request: UserRequestTemplate): ObjectNode {
        val json = RequestSelectionExporter().exportAsJson(listOf(request))
        return jsonMapper.readTree(json) as ObjectNode
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.payload.GraphqlResponseBody
import com.sunnychung.application.multiplatform.hellohttp.test.payload.IntervalResource
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeOutputResource
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class GraphqlRequestResponseTest(testName: String, httpVersion: HttpConfig.HttpProtocolVersion?, isSsl: Boolean, isMTls: Boolean) {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }

        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any?>> = listOf(
            arrayOf("Default", null, false, false),
            arrayOf("H2C", HttpConfig.HttpProtocolVersion.Http2Only, false, false),
            arrayOf("HTTP/1 only", HttpConfig.HttpProtocolVersion.Http1Only, false, false),
            arrayOf("HTTP/1 SSL", HttpConfig.HttpProtocolVersion.Http1Only, true, false),
            arrayOf("Default SSL", null, true, false),
            arrayOf("HTTP/2 SSL", HttpConfig.HttpProtocolVersion.Http2Only, true, false),
            arrayOf("Default mTLS", null, true, true),
        )
    }

    val graphqlUrl = "http${if (isSsl) "s" else ""}://${RequestResponseTest.hostAndPort(httpVersion = httpVersion, isSsl = isSsl, isMTls = isMTls)}/graphql"
    val environment = RequestResponseTest.environment(httpVersion = httpVersion, isSsl = isSsl, isMTls = isMTls)

    @JvmField
    @Rule
    val retryRule = RetryRule()

    @Test
    fun query() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = """
                            query {
                              sum(input: {
                                a: "abcde",
                                b: 19,
                                c: 10
                              }) {
                                a
                                sum
                              }
                            }
                        """.trimIndent(),
                        variables = "",
                        operationName = null,
                    )
                )
            )
        ), environment = environment)
        val body = assertHttpSuccessResponseAndGetResponseBody()
        val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<SomeOutputResource>>(body!!)
        assertEquals("abcde", resp.data.sum.a)
        assertEquals(29, resp.data.sum.sum)
    }

    @Test
    fun queryWithVariables() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = """
                            query MyQuery(${'$'}in: SomeInput!) {
                              sum(input: ${'$'}in) {
                                a
                                sum
                              }
                            }
                        """.trimIndent(),
                        variables = """
                            {
                                "in": {
                                    "a": "abcd",
                                    "b": 12,
                                    "c": 3
                                }
                            }
                        """.trimIndent(),
                        operationName = null,
                    )
                )
            )
        ), environment = environment)
        val body = assertHttpSuccessResponseAndGetResponseBody()
        val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<SomeOutputResource>>(body!!)
        assertEquals("abcd", resp.data.sum.a)
        assertEquals(15, resp.data.sum.sum)
    }

    @Test
    fun preflightSaveVariablesToEnvironment() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.Graphql,
                    queryParameters = listOf(
                        UserKeyValuePair("abc", "中文字"),
                        UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                        UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                    ),
                    headers = listOf(UserKeyValuePair("my-time", "2025-05-18T12:34:56.789")),
                    body = GraphqlBody(
                        document = """
                            query MyQuery(${'$'}in: SomeInput!) {
                              sum(input: ${'$'}in) {
                                a
                                sum
                              }
                            }
                        """.trimIndent(),
                        variables = """
                            {
                                "in": {
                                    "a": "abcd",
                                    "b": 12,
                                    "c": 3
                                }
                            }
                        """.trimIndent(),
                        operationName = null,
                    ),
                    preFlight = PreFlightSpec(
                        updateVariablesFromHeader = listOf(
                            UserKeyValuePair("eMyHeader", "my-time"),
                        ),
                        updateVariablesFromQueryParameters = listOf(
                            UserKeyValuePair("eQ1", "MyQueryParam"),
                            UserKeyValuePair("eQ2", "emoji"),
                        ),
                        updateVariablesFromGraphqlVariables = listOf(
                            UserKeyValuePair("eB1", "\$.in.a"),
                            UserKeyValuePair("eB2", "\$.in.c"),
                        ),
                    ),
                )
            )
        ), environment = environment, assertEnvVariables = mapOf(
            "eMyHeader" to "2025-05-18T12:34:56.789",
            "eQ1" to "abc def_gh+i=?j/k",
            "eQ2" to "A\uD83D\uDE0Eb",
            "eB1" to "abcd",
            "eB2" to "3",
        ))
        val body = assertHttpSuccessResponseAndGetResponseBody()
        val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<SomeOutputResource>>(body!!)
        assertEquals("abcd", resp.data.sum.a)
        assertEquals(15, resp.data.sum.sum)
    }

    @Test
    fun queryWithVariablesAndOperationName() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    headers = listOf(
                        UserKeyValuePair("h1", "abcd"),
                        UserKeyValuePair("x-My-Header", "defg HIjk"),
                    ),
                    queryParameters = listOf(
                        UserKeyValuePair("abc", "中文字"),
                        UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                        UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                    ),
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = """
                            query MyQuery(${'$'}in: SomeInput!) {
                              sum(input: ${'$'}in) {
                                a
                                sum
                              }
                            }
                        """.trimIndent(),
                        variables = """
                            {
                                "in": {
                                    "a": "abcdc",
                                    "b": 12,
                                    "c": 13
                                }
                            }
                        """.trimIndent(),
                        operationName = "MyQuery",
                    )
                )
            )
        ), environment = environment)
        val body = assertHttpSuccessResponseAndGetResponseBody()
        val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<SomeOutputResource>>(body!!)
        assertEquals("abcdc", resp.data.sum.a)
        assertEquals(25, resp.data.sum.sum)
    }

    @Test
    fun subscription() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = """
                            subscription MySubscription {
                              interval(seconds: 1, stopAt: 3) {
                                id
                                instant
                              }
                            }
                        """.trimIndent(),
                        variables = "",
                        operationName = null,
                    )
                )
            )
        ), environment = environment)

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Disconnect")

        wait(400.milliseconds())
        (0..3).forEach { i ->
            val body = assertHttpSuccessResponseAndGetResponseBody(isSubscriptionRequest = true)!!
            val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<IntervalResource>>(body)
            assertEquals(i, resp.data.interval.id)
            wait(1.seconds())
        }

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Connect")
    }

    @Test
    fun subscriptionWithVariablesAndOperationName() = runTest {
        createAndSendGraphqlRequest(UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            application = ProtocolApplication.Graphql,
            url = graphqlUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    headers = listOf(
                        UserKeyValuePair("h1", "abcd"),
                        UserKeyValuePair("x-My-Header", "defg HIjk"),
                    ),
                    queryParameters = listOf(
                        UserKeyValuePair("abc", "中文字"),
                        UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                        UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                    ),
                    contentType = ContentType.Graphql,
                    body = GraphqlBody(
                        document = """
                            subscription MySubscription(${'$'}seconds: Int!, ${'$'}stopAt: Int) {
                              interval(seconds: ${'$'}seconds, stopAt: ${'$'}stopAt) {
                                id
                                instant
                              }
                            }
                        """.trimIndent(),
                        variables = """
                            {
                                "seconds": 1,
                                "stopAt": 4
                            }
                        """.trimIndent(),
                        operationName = "MySubscription",
                    )
                )
            )
        ), environment = environment)

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Disconnect")

        wait(400.milliseconds())
        (0..4).forEach { i ->
            val body = assertHttpSuccessResponseAndGetResponseBody(isSubscriptionRequest = true)!!
            val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<IntervalResource>>(body)
            assertEquals(i, resp.data.interval.id)
            wait(1.seconds())
        }

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Connect")
    }
}

suspend fun DesktopComposeUiTest.createGraphqlRequest(request: UserRequestTemplate, environment: TestEnvironment) {
    createRequest(request = request, environment = environment)

    onNodeWithTag(TestTag.RequestParameterTypeTabContainer.name, useUnmergedTree = true)
        .performScrollToIndex(0)
    delay(3.seconds().millis)

    onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Body")))
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    delayShort()

    onNodeWithTag(buildTestTag(TestTagPart.RequestGraphqlOperationName.name, TestTagPart.DropdownButton)!!)
        .assertIsDisplayed()
        .assertTextEquals("--")

    onNodeWithTag(TestTag.RequestGraphqlDocumentTextField.name)
        .assertIsDisplayed()
        .assertTextEquals("")

    onNodeWithTag(TestTag.RequestGraphqlVariablesTextField.name)
        .assertIsDisplayed()
        .assertTextEquals("")

    val body = request.examples.first().body as GraphqlBody

    onNodeWithTag(TestTag.RequestGraphqlDocumentTextField.name)
        .performTextInput(this, body.document)

    delayShort() // needed, otherwise document text field sometimes have no text inputted

    onNodeWithTag(TestTag.RequestGraphqlVariablesTextField.name)
        .performTextInput(this, body.variables)

    if (body.operationName != null) {
        delayShort()

        onNodeWithTag(buildTestTag(TestTagPart.RequestGraphqlOperationName.name, TestTagPart.DropdownButton)!!)
            .performClickWithRetry(this)

        delayShort()

        onNodeWithTag(buildTestTag(TestTagPart.RequestGraphqlOperationName.name, TestTagPart.DropdownItem, body.operationName)!!)
            .performClickWithRetry(this)
    }
}

suspend fun DesktopComposeUiTest.createAndSendGraphqlRequest(request: UserRequestTemplate, timeout: KDuration = 2500.milliseconds(), environment: TestEnvironment, assertEnvVariables: Map<String, String> = emptyMap()) {
    createGraphqlRequest(request = request, environment = environment)

    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitForIdle()

    // wait for response
    waitUntil(5.seconds().toMilliseconds()) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty() }
    waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }

    if (assertEnvVariables.isNotEmpty()) {
        assertPreflightHaveUpdatedEnvironmentVariables(request, assertEnvVariables)
    }
}

fun DesktopComposeUiTest.assertHttpSuccessResponseAndGetResponseBody(isSubscriptionRequest: Boolean = false): String? {
    onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals(if (!isSubscriptionRequest) {
        "200 OK"
    } else {
        "101 Switching Protocols"
    })
    if (isSubscriptionRequest) {
        onNodeWithTag(TestTag.ResponseStreamLog.name)
            .assertIsDisplayedWithRetry(this)
    }
    return getResponseBody()
}

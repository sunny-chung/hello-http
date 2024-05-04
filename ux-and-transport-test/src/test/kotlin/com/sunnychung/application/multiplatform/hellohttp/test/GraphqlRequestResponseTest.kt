@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.payload.GraphqlResponseBody
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeOutputResource
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class GraphqlRequestResponseTest {

    companion object {
        lateinit var bigDataFile: File

        @BeforeAll
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }

        val graphqlUrl = "${RequestResponseTest.httpUrlPrefix}/graphql"
    }

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
        ))
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
        ))
        val body = assertHttpSuccessResponseAndGetResponseBody()
        val resp = jacksonObjectMapper().readValue<GraphqlResponseBody<SomeOutputResource>>(body!!)
        assertEquals("abcdc", resp.data.sum.a)
        assertEquals(25, resp.data.sum.sum)
    }
}

suspend fun ComposeUiTest.createGraphqlRequest(request: UserRequestTemplate) {
    createRequest(request)
    selectRequestMethod("GraphQL")
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
        .performTextInput(body.document)

    delayShort() // needed, otherwise document text field sometimes have no text inputted

    onNodeWithTag(TestTag.RequestGraphqlVariablesTextField.name)
        .performTextInput(body.variables)

    if (body.operationName != null) {
        delayShort()

        onNodeWithTag(buildTestTag(TestTagPart.RequestGraphqlOperationName.name, TestTagPart.DropdownButton)!!)
            .performClickWithRetry(this)

        delayShort()

        onNodeWithTag(buildTestTag(TestTagPart.RequestGraphqlOperationName.name, TestTagPart.DropdownItem, body.operationName)!!)
            .performClickWithRetry(this)
    }
}

suspend fun ComposeUiTest.createAndSendGraphqlRequest(request: UserRequestTemplate, timeout: KDuration = 1.seconds()) {
    createGraphqlRequest(request)

    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitForIdle()

    // wait for response
    waitUntil(1.seconds().toMilliseconds()) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty() }
    waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }
}

fun ComposeUiTest.assertHttpSuccessResponseAndGetResponseBody(isSubscriptionRequest: Boolean = false): String? {
    onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals(if (!isSubscriptionRequest) {
        "200 OK"
    } else {
        "101 Switching Protocols"
    })
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
        .getTexts()
        .singleOrNull()
    return responseBody
}

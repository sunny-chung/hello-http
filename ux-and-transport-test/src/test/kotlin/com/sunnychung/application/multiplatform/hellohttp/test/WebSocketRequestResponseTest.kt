@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.net.URI

@RunWith(Parameterized::class)
class WebSocketRequestResponseTest(testName: String, isSsl: Boolean, isMTls: Boolean) {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any>> = listOf(
            arrayOf("HTTP", false, false),
            arrayOf("SSL", true, false),
            arrayOf("mTLS", true, true),
        )
    }

    val webSocketUrl = "ws${if (isSsl) "s" else ""}://${
        RequestResponseTest.hostAndPort(
            httpVersion = HttpConfig.HttpProtocolVersion.Http1Only,
            isSsl = isSsl,
            isMTls = isMTls
        )
    }/ws"
    val environment = RequestResponseTest.environment(isSsl = isSsl, isMTls = isMTls)

    @JvmField
    @Rule
    val retryRule = RetryRule()

    @Test
    fun sendAndReceivePayloads() = runTest {
        val request = createAndFireWebSocketRequest(environment = environment)
        wait(500.milliseconds())
        assertHttpStatus()

        try {
            sendPayload("abc", isCreatePayloadExample = false)
            waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello abc" }
            sendPayload("defg\nhi")
            waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello defg\nhi" }
            sendPayload("中文字")
            waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello 中文字" }
            sendPayload("Yeah 😎🍣✌🏽!!")
            waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello Yeah \uD83D\uDE0E\uD83C\uDF63✌\uD83C\uDFFD!!" }

            sendPayload("!echo")
            waitUntil(400.milliseconds().millis) { getResponseBody() != "Hello Yeah \uD83D\uDE0E\uD83C\uDF63✌\uD83C\uDFFD!!" }
            verifyEchoResponse(request, getResponseBody()!!)

            sendPayload("aaaaa")
            waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello aaaaa" }
        } catch (e: AssertionError) {
            captureScreenToFile("sendAndReceivePayloads")
            throw e
        } catch (e: ComposeTimeoutException) {
            captureScreenToFile("sendAndReceivePayloads")
            throw e
        }

        disconnect()
    }

    @Test
    fun withCustomHeadersAndQueryParameters() = runTest {
        val request = createAndFireWebSocketRequest(environment = environment) { request ->
            request.copy(
                examples = request.examples.map { ex ->
                    ex.copy(
                        headers = listOf(
                            UserKeyValuePair("h1", "abcd"),
                            UserKeyValuePair("x-My-Header", "defg HIjk"),
                        ),
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "中文字"),
                            UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                            UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                        ),
                    )
                }
            )
        }
        assertHttpStatus()

        sendPayload("中文字", isCreatePayloadExample = false)
        waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello 中文字" }
        sendPayload("Yeah 😎🍣✌🏽!!")
        waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello Yeah \uD83D\uDE0E\uD83C\uDF63✌\uD83C\uDFFD!!" }

        sendPayload("!echo")
        waitUntil(400.milliseconds().millis) { getResponseBody() != "Hello Yeah \uD83D\uDE0E\uD83C\uDF63✌\uD83C\uDFFD!!" }
        verifyEchoResponse(request, getResponseBody()!!)

        sendPayload("aaaaa")
        waitUntil(400.milliseconds().millis) { getResponseBody() == "Hello aaaaa" }

        disconnect()
    }

    suspend fun DesktopComposeUiTest.createAndFireWebSocketRequest(
        environment: TestEnvironment,
        requestDecorator: (UserRequestTemplate) -> UserRequestTemplate = { it }
    ): UserRequestTemplate {
        val request = UserRequestTemplate(
            id = uuidString(),
            method = "",
            application = ProtocolApplication.WebSocket,
            url = webSocketUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                )
            ),
            payloadExamples = listOf(PayloadExample(id = uuidString(), name = "New Payload", body = ""))
        ).let(requestDecorator)
        createRequest(request = request, environment = environment)
        selectRequestMethod("WebSocket")
        delayShort()
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Connect")
            .performClickWithRetry(this)

        delayShort()

        waitUntil(1.seconds().millis) {
            onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty()
        }

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Disconnect")

        return request
    }
}

fun DesktopComposeUiTest.assertHttpStatus() {
    onNodeWithTag(TestTag.ResponseStatus.name)
        .assertTextEquals("101 Switching Protocols")
}

fun verifyEchoResponse(request: UserRequestTemplate, echoResponse: String) {
    val r = jacksonObjectMapper().readValue<RequestData>(echoResponse)
    assertEquals(URI.create(request.url).path, r.path)
    assertEquals("GET", r.method)
    assertTrue(r.headers.any { it.name == "Connection" && it.value == "Upgrade" })
    assertTrue(r.headers.any { it.name == "Host" })
    assertTrue(r.headers.any { it.name == "Sec-WebSocket-Key" })
    assertTrue(r.headers.any { it.name == "Sec-WebSocket-Version" })
    assertTrue(r.headers.any { it.name == "Upgrade" && it.value == "websocket" })
    assertEquals(
        request.examples.first().queryParameters.map { Parameter(it.key, it.value) }.sortedBy { it.name },
        r.queryParameters.sortedBy { it.name }
    )
    assertEquals(0, r.formData.size)
    assertEquals(0, r.multiparts.size)
    assertEquals(null, r.body)
}

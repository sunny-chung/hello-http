@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.junit.Test
import java.io.File
import java.net.URI

class WebSocketRequestResponseTest {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }

        val webSocketUrl = "ws://${RequestResponseTest.hostAndPort}/ws"
    }

    @Test
    fun sendAndReceivePayloads() = runTest {
        val request = createAndFireWebSocketRequest()
        assertHttpStatus()

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

        disconnect()
    }

    @Test
    fun withCustomHeadersAndQueryParameters() = runTest {
        val request = createAndFireWebSocketRequest { request ->
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
}

suspend fun ComposeUiTest.createAndFireWebSocketRequest(requestDecorator: (UserRequestTemplate) -> UserRequestTemplate = { it }): UserRequestTemplate {
    val request = UserRequestTemplate(
        id = uuidString(),
        method = "",
        application = ProtocolApplication.WebSocket,
        url = WebSocketRequestResponseTest.webSocketUrl,
        examples = listOf(
            UserRequestExample(
                id = uuidString(),
                name = "Base",
            )
        ),
        payloadExamples = listOf(PayloadExample(id = uuidString(), name = "New Payload", body = ""))
    ).let(requestDecorator)
    createRequest(request)
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

fun ComposeUiTest.assertHttpStatus() {
    onNodeWithTag(TestTag.ResponseStatus.name)
        .assertTextEquals("101 Switching Protocols")
}

fun verifyEchoResponse(request: UserRequestTemplate, echoResponse: String) {
    val r = jacksonObjectMapper().readValue<RequestData>(echoResponse)
    assertEquals(URI.create(WebSocketRequestResponseTest.webSocketUrl).path, r.path)
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

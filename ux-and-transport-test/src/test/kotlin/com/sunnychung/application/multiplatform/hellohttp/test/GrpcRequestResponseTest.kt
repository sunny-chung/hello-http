@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.GrpcRequestResponseTest.Companion.hostAndPort
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt

class GrpcRequestResponseTest {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }

        val hostAndPort = "localhost:18082"
        val grpcUrl = "grpc://$hostAndPort"
    }

    @JvmField
    @Rule
    val retryRule = RetryRule(3)

    @Test
    fun unaryWithoutInput() = runTest {
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "Hi",
                ),
                examples = req.examples.map { ex ->
                    ex.copy(
                        body = StringBody("{}")
                    )
                }
            )
        }
        fireRequest()
        assertSuccessStatus()
        assertEquals("""
            {
              "data": "Hello HTTP!"
            }
        """.trimIndent(), getResponseBody())
    }

    @Test
    fun unaryWithInput() = runTest {
        val input = "中文字${uuidString()}✌🏽😎abcd"
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "SayHello",
                ),
                examples = req.examples.map { ex ->
                    ex.copy(
                        headers = listOf(
                            UserKeyValuePair("h1", "abcd"),
                            UserKeyValuePair("x-My-Header", "defg HIjk"),
                        ),
                        body = StringBody("{\"name\": \"$input\"}")
                    )
                }
            )
        }
        fireRequest(timeout = 4.seconds())
        assertSuccessStatus()
        assertEquals("""
            {
              "data": "Hello HTTP, $input!"
            }
        """.trimIndent(), getResponseBody())
    }

    @Test
    fun error() = runTest {
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "Error",
                ),
                examples = req.examples.map { ex ->
                    ex.copy(
                        body = StringBody("{\"data\":5}")
                    )
                }
            )
        }
        fireRequest()
        assertStatus("5 NOT_FOUND")
    }

    @Test
    fun clientStreaming() = runTest {
        val random = Random
        val input = (0 until 5).map { random.nextInt(0..20) }
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "ClientStream",
                ),
            )
        }
        fireRequest(isClientStreaming = true)
        input.forEachIndexed { index, it ->
            sendPayload("""
                {
                    "data": $it
                }
            """.trimIndent(), index > 0)
            waitForIdle()
            assert(getResponseBody().isNullOrEmpty())
        }
        completeRequest()
        wait(1.seconds())
        assertSuccessStatus()
        assertEquals("""
            {
              "data": ${input.sum()}
            }
        """.trimIndent(), getResponseBody())
    }

    @Test
    fun cancelStream() = runTest {
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "ClientStream",
                ),
            )
        }
        fireRequest(isClientStreaming = true)
        sendPayload("""
                {
                  "data": 25
                }
            """.trimIndent(), isCreatePayloadExample = false)
        sendPayload("""
                {
                  "data": 6
                }
            """.trimIndent())
        disconnect()
        assertStatus("No error")
    }

    @Test
    fun serverStreaming() = runTest {
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "ServerStream",
                ),
                examples = req.examples.map { ex ->
                    ex.copy(
                        body = StringBody("{\"data\":5}")
                    )
                }
            )
        }
        fireRequest(isServerStreaming = true)
        wait(400.milliseconds())
        (1..5).forEach { i ->
            wait(1.seconds())
            if (i < 5) {
                assertStatus("Communicating")
            }
            assertEquals("""
                {
                  "data": $i
                }
            """.trimIndent(), getResponseBody())
        }
        assertSuccessStatus()

        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Send")
    }

    @Test
    fun bidirectionalStreaming() = runTest {
        val random = Random
        val input = (0 until 6).map { random.nextInt(0..20) }
        createGrpcRequest { req ->
            req.copy(
                grpc = UserGrpcRequest(
                    service = "sunnychung.grpc.services.MyService",
                    method = "BiStream",
                ),
            )
        }
        fireRequest(isClientStreaming = true, isServerStreaming = true)
        input.forEachIndexed { index, it ->
            assertStatus("Communicating")
            sendPayload("""{"data":$it}""", index > 0)
            wait(500.milliseconds())
            assertEquals("""
                {
                  "data": ${it + 100}
                }
            """.trimIndent(), getResponseBody())
            assertStatus("Communicating")
        }
        completeRequest()
        wait(1.seconds())
        assertSuccessStatus()
    }
}

suspend fun ComposeUiTest.createGrpcRequest(requestDecorator: (UserRequestTemplate) -> UserRequestTemplate) {
    val request = UserRequestTemplate(
        id = uuidString(),
        application = ProtocolApplication.Grpc,
        url = GrpcRequestResponseTest.grpcUrl,
    ).let(requestDecorator)
    createRequest(request)
    selectRequestMethod("gRPC")
    delayShort()

    var apiSpecNodeInteraction = onAllNodes(hasTestTag(buildTestTag(TestTagPart.RequestApiSpecDropdown, TestTagPart.DropdownButton)!!).and(
        hasRole(Role.Button)
    ))
    var apiSpecNodes = apiSpecNodeInteraction.fetchSemanticsNodes()

    if (apiSpecNodes.isEmpty()) {
        onNodeWithTag(TestTag.RequestFetchApiSpecButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        delayShort()

        waitUntil(3.seconds().millis) {
            onAllNodesWithTag(TestTag.RequestFetchApiSpecButton.name).fetchSemanticsNodes().isNotEmpty()
        }

        delayShort()

        apiSpecNodeInteraction = onAllNodes(hasTestTag(buildTestTag(TestTagPart.RequestApiSpecDropdown, TestTagPart.DropdownButton)!!).and(
            hasRole(Role.Button)
        ))
            .assertCountEquals(1)
        apiSpecNodes = apiSpecNodeInteraction.fetchSemanticsNodes()
    }

    if (apiSpecNodes.single().getTexts() == listOf("--")) {
        selectDropdownItem(TestTagPart.RequestApiSpecDropdown.name, hostAndPort)
        delayShort()
    }

    selectDropdownItem(TestTagPart.RequestGrpcServiceDropdown.name, request.grpc!!.service)
    delayShort()

    selectDropdownItem(TestTagPart.RequestGrpcMethodDropdown.name, request.grpc!!.method)
    delayShort()

    if (request.examples.first().body is StringBody) {
        onNodeWithTag(TestTag.RequestStringBodyTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextInput((request.examples.first().body as StringBody).value)
        delayShort()
    }
}

fun ComposeUiTest.assertStatus(expected: String) {
    onNodeWithTag(TestTag.ResponseStatus.name)
        .assertTextEquals(expected)
}

fun ComposeUiTest.assertSuccessStatus() {
    assertStatus("0 OK")
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.io.File

class RequestResponseTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initTests() {
            val appDir = File("build/testrun/data")
            AppContext.dataDir = appDir
            AppContext.SingleInstanceProcessService.apply { dataDir = appDir }.enforce()
            runBlocking {
                AppContext.PersistenceManager.initialize()
            }
        }

        val echoUrl = "http://localhost:18081/rest/echo"


    }

    @Test
    fun echoGet() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
            )
        )
    }

    @Test
    fun echoGetWithQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "def"),
                            UserKeyValuePair("ghijK", "lmnopqrS"),
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun echoGetWithQueryParametersAndHeaders() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        headers = listOf(
                            UserKeyValuePair("h1", "abcd"),
                            UserKeyValuePair("x-My-Header", "defg"),
                        ),
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "def"),
                            UserKeyValuePair("ghijK", "lmnopqrS"),
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun echoGetWithHeaders() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        headers = listOf(
                            UserKeyValuePair("h1", "abcd"),
                            UserKeyValuePair("x-My-Header", "defg"),
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun echoGetWithUnicodeSpecialCharacterQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        headers = listOf(
                            UserKeyValuePair("h1", "abcd"),
                            UserKeyValuePair("x-My-Header", "defg"),
                        ),
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "中文字"),
                            UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                            UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                        ),
                    )
                )
            )
        )
    }
}

fun runTest(testBlock: suspend ComposeUiTest.() -> Unit) =
    runComposeUiTest {
        setContent {
            Window(
                title = "Hello HTTP",
                onCloseRequest = {},
                state = rememberWindowState(width = 1024.dp, height = 560.dp)
            ) {
                with(LocalDensity.current) {
                    window.minimumSize = if (isMacOs()) {
                        Dimension(800, 450)
                    } else {
                        Dimension(800.dp.roundToPx(), 450.dp.roundToPx())
                    }
                }
                AppView()
            }
        }
        runBlocking {
            testBlock()
        }
    }

fun ComposeUiTest.createProjectIfNeeded() {
    if (onAllNodesWithTag(TestTag.FirstTimeCreateProjectButton.name).fetchSemanticsNodes().isNotEmpty()) {
        // create first project
        onNodeWithTag(TestTag.FirstTimeCreateProjectButton.name)
            .performClick()
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput("Test Project")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .performClick()

        // create first subproject
        waitUntilExactlyOneExists(hasTestTag(TestTag.FirstTimeCreateSubprojectButton.name), 500L)
        onNodeWithTag(TestTag.FirstTimeCreateSubprojectButton.name)
            .performClick()
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput("Test Subproject")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .performClick()

        println("created first project and subproject")
    }
    waitUntilExactlyOneExists(hasTestTag(TestTag.CreateRequestOrFolderButton.name), 5000L)
}

suspend fun ComposeUiTest.createAndSendHttpRequest(request: UserRequestTemplate, timeout: KDuration = 1.seconds(), isOneOffRequest: Boolean = true) {
    createProjectIfNeeded()
    val baseExample = request.examples.first()

    onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
        .performClick()
    waitUntilExactlyOneExists(hasTextExactly("Request", includeEditableText = false))
    onNodeWithText("Request")
        .performClick()
    waitUntilExactlyOneExists(hasTestTag(TestTag.RequestUrlTextField.name), 1000L)

    // TODO method

    onNodeWithTag(TestTag.RequestUrlTextField.name)
        .performTextInput(request.url)

    delayShort()
    waitForIdle()

    if (baseExample.queryParameters.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Query")))
            .performClickWithRetry(this)

        baseExample.queryParameters.forEachIndexed { index, it ->
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Value, index)!!))
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
                .performTextInput(it.key)
            delayShort()
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
                .assertTextEquals(it.key)
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Value, index)!!))
                .performTextInput(it.value)
            delayShort()
        }
    }

    if (baseExample.headers.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Header")))
            .performClickWithRetry(this)
        baseExample.headers.forEachIndexed { index, it ->
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Key, index)!!))
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Key, index)!!))
                .performTextInput(it.key)
            delayShort()
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
                .performTextInput(it.value)
            delayShort()
        }
    }

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .performClickWithRetry(this)
    waitForIdle()

    // wait for response
    delay(500L)
    waitForIdle()
    if (isOneOffRequest) {
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }
    }
}

suspend fun ComposeUiTest.createAndSendRestEchoRequestAndAssertResponse(request: UserRequestTemplate) {
    val baseExample = request.examples.first()
    createAndSendHttpRequest(request)

    onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
        .getTexts()
        .single()
    val resp = jacksonObjectMapper().readValue(responseBody, RequestData::class.java)
    assertEquals("GET", resp.method)
    assertEquals("/rest/echo", resp.path)
    assertTrue(resp.headers.size >= 2) // at least have "Host" and "User-Agent" headers
    if (baseExample.headers.isNotEmpty()) {
        assertTrue(resp.headers.containsAll(baseExample.headers.map { Parameter(it.key, it.value) }))
    }
    assertEquals(
        baseExample.queryParameters.map { Parameter(it.key, it.value) }.sortedBy { it.name },
        resp.queryParameters.sortedBy { it.name }
    )
    if (baseExample.body is FormUrlEncodedBody) {
        assertEquals(
            (baseExample.body as FormUrlEncodedBody).value.map { Parameter(it.key, it.value) }.sortedBy { it.name },
            resp.formData.sortedBy { it.name }
        )
    } else {
        assertEquals(0, resp.formData.size)
    }
    assertEquals(0, resp.multiparts.size)
    when (val body = baseExample.body) {
        null, is FormUrlEncodedBody, is MultipartBody -> assertEquals(null, resp.body)
        is FileBody -> TODO()
        is GraphqlBody -> TODO()
        is StringBody -> assertEquals(body.value, resp.body)
    }
}

suspend fun ComposeUiTest.delayShort() {
    delay(200L)
    waitForIdle()
}

/**
 * retry to prevent illegal state after sleeping
 */
fun SemanticsNodeInteraction.performClickWithRetry(host: ComposeUiTest): SemanticsNodeInteraction {
    while (true) {
        try {
            performClick()
            return this
        } catch (e: IllegalArgumentException) {
            host.waitForIdle()
        }
    }
}

fun SemanticsNode.getTexts(): List<String> {
    val actual = mutableListOf<String>()
    config.getOrNull(SemanticsProperties.EditableText)
        ?.let { actual.add(it.text) }
    config.getOrNull(SemanticsProperties.Text)
        ?.let { actual.addAll(it.map { anStr -> anStr.text }) }
    return actual.filter { it.isNotBlank() }
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.test.RequestResponseTest.Companion.echoUrl
import com.sunnychung.application.multiplatform.hellohttp.test.RequestResponseTest.Companion.httpUrlPrefix
import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.testChooseFile
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.awt.Dimension
import java.io.File

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
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput("Test Project")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .performClickWithRetry(this)

        // create first subproject
        waitUntilExactlyOneExists(hasTestTag(TestTag.FirstTimeCreateSubprojectButton.name), 500L)
        onNodeWithTag(TestTag.FirstTimeCreateSubprojectButton.name)
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput("Test Subproject")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .performClickWithRetry(this)

        println("created first project and subproject")
    }
//    while (true) {
//        try {
            waitUntilExactlyOneExists(hasTestTag(TestTag.CreateRequestOrFolderButton.name), 5000L)
//        } catch (_: IllegalArgumentException) {
//            println("waiting")
//            waitForIdle()
//        }
//    }
}

fun ComposeUiTest.selectRequestMethod(itemDisplayText: String) {
    // TODO support custom method
    selectDropdownItem(TestTagPart.RequestMethodDropdown.name, itemDisplayText)
}

fun ComposeUiTest.selectDropdownItem(testTagPart: String, itemDisplayText: String) {
    onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownButton)!!)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)

    val nextTag = buildTestTag(testTagPart, TestTagPart.DropdownItem, itemDisplayText)!!
    waitUntilExactlyOneExists(hasTestTag(nextTag))
    onNodeWithTag(nextTag)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
}

suspend fun ComposeUiTest.createRequest(request: UserRequestTemplate) {
    createProjectIfNeeded()
    val baseExample = request.examples.first()

    onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    delayShort()
    waitUntilExactlyOneExists(hasTextExactly("Request", includeEditableText = false))
    onNodeWithText("Request")
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitUntilExactlyOneExists(hasTestTag(TestTag.RequestUrlTextField.name), 1000L)

    delayShort()

    if (request.application == ProtocolApplication.Http && request.method != "GET") {
        selectRequestMethod(request.method)
        delayShort()
    }

    onNodeWithTag(TestTag.RequestUrlTextField.name)
        .assertIsDisplayedWithRetry(this)
        .performTextInput(request.url)

    delayShort()

    if (request.application == ProtocolApplication.Http && baseExample.contentType != ContentType.None) {
        onNodeWithTag(buildTestTag(TestTagPart.RequestBodyTypeDropdown, TestTagPart.DropdownButton)!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        delayShort()

        val nextTag = buildTestTag(
            TestTagPart.RequestBodyTypeDropdown,
            TestTagPart.DropdownItem,
            baseExample.contentType.displayText
        )!!
        waitUntilExactlyOneExists(hasTestTag(nextTag))
        onNodeWithTag(nextTag)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        delayShort()

        when (baseExample.contentType) {
            ContentType.Json, ContentType.Raw -> {
                val body = (baseExample.body as StringBody).value
                if (body.isNotEmpty()) {
                    onNodeWithTag(TestTag.RequestStringBodyTextField.name)
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(body)
                    delayShort()
                }
            }

            ContentType.Multipart -> {
                val body = (baseExample.body as MultipartBody).value
                body.forEachIndexed { index, it ->
                    waitUntilExactlyOneExists(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyMultipartForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                    waitUntilExactlyOneExists(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyMultipartForm,
                                TestTagPart.Current,
                                TestTagPart.Value,
                                index
                            )!!
                        )
                    )
                    onNode(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyMultipartForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.key)
                    delayShort()
                    onNode(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyMultipartForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                        .assertIsDisplayedWithRetry(this)
                        .assertTextEquals(it.key)

                    when (it.valueType) {
                        FieldValueType.String -> {
                            onNode(
                                hasTestTag(
                                    buildTestTag(
                                        TestTagPart.RequestBodyMultipartForm,
                                        TestTagPart.Current,
                                        TestTagPart.Value,
                                        index
                                    )!!
                                )
                            )
                                .assertIsDisplayedWithRetry(this)
                                .performTextInput(it.value)
                            delayShort()
                        }

                        FieldValueType.File -> {
                            onNode(
                                hasTestTag(
                                    buildTestTag(
                                        TestTagPart.RequestBodyMultipartForm,
                                        TestTagPart.Current,
                                        index,
                                        TestTagPart.ValueTypeDropdown,
                                        TestTagPart.DropdownButton
                                    )!!
                                )
                            )
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)
                            delayShort()

                            onNode(
                                hasTestTag(
                                    buildTestTag(
                                        TestTagPart.RequestBodyMultipartForm,
                                        TestTagPart.Current,
                                        index,
                                        TestTagPart.ValueTypeDropdown,
                                        TestTagPart.DropdownItem,
                                        "File"
                                    )!!
                                )
                            )
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)
                            delayShort()

                            testChooseFile = File(it.value)
                            val filename = testChooseFile!!.name
                            onNode(
                                hasTestTag(
                                    buildTestTag(
                                        TestTagPart.RequestBodyMultipartForm,
                                        TestTagPart.Current,
                                        index,
                                        TestTagPart.FileButton
                                    )!!
                                )
                            )
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)

                            delay(100L)
                            mainClock.advanceTimeBy(100L)
                            delayShort()
                            onNode(
                                hasTestTag(
                                    buildTestTag(
                                        TestTagPart.RequestBodyMultipartForm,
                                        TestTagPart.Current,
                                        index,
                                        TestTagPart.FileButton
                                    )!!
                                )
                            )
                                .assertTextEquals(filename, includeEditableText = false)

                        }
                    }
                }
            }

            ContentType.FormUrlEncoded -> {
                val body = (baseExample.body as FormUrlEncodedBody).value
                body.forEachIndexed { index, it ->
                    waitUntilExactlyOneExists(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyFormUrlEncodedForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                    waitUntilExactlyOneExists(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyFormUrlEncodedForm,
                                TestTagPart.Current,
                                TestTagPart.Value,
                                index
                            )!!
                        )
                    )
                    onNode(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyFormUrlEncodedForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.key)
                    delayShort()

                    onNode(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyFormUrlEncodedForm,
                                TestTagPart.Current,
                                TestTagPart.Key,
                                index
                            )!!
                        )
                    )
                        .assertIsDisplayedWithRetry(this)
                        .assertTextEquals(it.key)
                    onNode(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.RequestBodyFormUrlEncodedForm,
                                TestTagPart.Current,
                                TestTagPart.Value,
                                index
                            )!!
                        )
                    )
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.value)
                    delayShort()
                }
            }

            ContentType.BinaryFile -> {
                val body = baseExample.body as FileBody
                testChooseFile = File(body.filePath!!)
                val filename = testChooseFile!!.name
                onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFileForm, TestTagPart.FileButton)!!))
                    .assertIsDisplayedWithRetry(this)
                    .performClickWithRetry(this)

                delay(100L)
                mainClock.advanceTimeBy(100L)
                delayShort()
                onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFileForm, TestTagPart.FileButton)!!))
                    .assertTextEquals(filename, includeEditableText = false)
            }

            ContentType.Graphql -> TODO()
            ContentType.None -> throw IllegalStateException()
        }
    }

    if (baseExample.queryParameters.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Query")))
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        baseExample.queryParameters.forEachIndexed { index, it ->
            waitUntilExactlyOneExists(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestQueryParameter,
                        TestTagPart.Current,
                        TestTagPart.Key,
                        index
                    )!!
                )
            )
            waitUntilExactlyOneExists(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestQueryParameter,
                        TestTagPart.Current,
                        TestTagPart.Value,
                        index
                    )!!
                )
            )
            onNode(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestQueryParameter,
                        TestTagPart.Current,
                        TestTagPart.Key,
                        index
                    )!!
                )
            )
                .assertIsDisplayedWithRetry(this)
                .performTextInput(it.key)
            delayShort()
            onNode(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestQueryParameter,
                        TestTagPart.Current,
                        TestTagPart.Key,
                        index
                    )!!
                )
            )
                .assertIsDisplayedWithRetry(this)
                .assertTextEquals(it.key)
            onNode(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestQueryParameter,
                        TestTagPart.Current,
                        TestTagPart.Value,
                        index
                    )!!
                )
            )
                .assertIsDisplayedWithRetry(this)
                .performTextInput(it.value)
            delayShort()
        }
    }

    if (baseExample.headers.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Header")))
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        baseExample.headers.forEachIndexed { index, it ->
            waitUntilExactlyOneExists(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestHeader,
                        TestTagPart.Current,
                        TestTagPart.Key,
                        index
                    )!!
                )
            )
            waitUntilExactlyOneExists(
                hasTestTag(
                    buildTestTag(
                        TestTagPart.RequestHeader,
                        TestTagPart.Current,
                        TestTagPart.Value,
                        index
                    )!!
                )
            )
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Key, index)!!))
                .assertIsDisplayedWithRetry(this)
                .performTextInput(it.key)
            delayShort()
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
                .assertIsDisplayedWithRetry(this)
                .performTextInput(it.value)
            delayShort()
        }
    }
}

suspend fun ComposeUiTest.createAndSendHttpRequest(request: UserRequestTemplate, timeout: KDuration = 1.seconds(), isOneOffRequest: Boolean = true) {
    createRequest(request)

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitForIdle()

    // wait for response
    waitUntil(5000L) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty() }
    if (isOneOffRequest) {
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }
    }
}

suspend fun ComposeUiTest.createAndSendRestEchoRequestAndAssertResponse(request: UserRequestTemplate, timeout: KDuration = 1.seconds()) {
    val baseExample = request.examples.first()
    val isAssertBodyContent = request.url == echoUrl
    createAndSendHttpRequest(request, timeout)

    onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
        .getTexts()
        .single()
    println(responseBody)
    val resp = jacksonObjectMapper().readValue(responseBody, RequestData::class.java)
    assertEquals(request.method, resp.method)
    assertEquals(request.url.removePrefix(httpUrlPrefix), resp.path)
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
    if (baseExample.body is MultipartBody) {
        val body = (baseExample.body as MultipartBody).value.sortedBy { it.key }
        resp.multiparts.sortedBy { it.name }.forEachIndexed { index, part ->
            val reqPart = body[index]
            assertEquals(reqPart.key, part.name)
            when (reqPart.valueType) {
                FieldValueType.String -> {
                    assertEquals(reqPart.value.encodeToByteArray().size, part.size)
                    if (isAssertBodyContent) {
                        assertEquals(reqPart.value, part.data)
                    }
                }
                FieldValueType.File -> {
                    val file = File(reqPart.value)
                    assertEquals(file.length().toInt(), part.size)
                    if (isAssertBodyContent) {
                        assertEquals(file.readText(), part.data)
                    }
                }
            }
        }
    } else {
        assertEquals(0, resp.multiparts.size)
    }
    when (val body = baseExample.body) {
        null, is FormUrlEncodedBody, is MultipartBody -> assertEquals(null, resp.body)
        is FileBody -> {
            if (isAssertBodyContent) {
                assertEquals(File(body.filePath).readText(), resp.body)
            } else {
                assertEquals(File(body.filePath).length().toString(), resp.body)
            }
        }
        is GraphqlBody -> TODO()
        is StringBody -> {
            if (isAssertBodyContent) {
                assertEquals(body.value, resp.body)
            } else {
                assertEquals(body.value.encodeToByteArray().size.toString(), resp.body)
            }
        }
    }
}

suspend fun ComposeUiTest.sendPayload(payload: String, isCreatePayloadExample: Boolean = true) {
    fun getStreamPayloadLatestTimeString(): String {
        waitForIdle()
        return (onAllNodesWithTag(TestTag.ResponseStreamLogItemTime.name, useUnmergedTree = true)
            .fetchSemanticsNodes()//.also { println("getStreamPayloadLatestTimeString() size ${it.size}") }
            .firstOrNull()
            ?.getTexts()
            ?.firstOrNull()
            ?: "")
            .also {
                println("getStreamPayloadLatestTimeString() = $it")
            }
    }

    if (isCreatePayloadExample) {
        delayShort()

        onNodeWithTag(TestTag.RequestAddPayloadExampleButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        delayShort()

        onNodeWithTag(TestTag.RequestPayloadTextField.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("")
    }

    onNodeWithTag(TestTag.RequestPayloadTextField.name)
        .assertIsDisplayedWithRetry(this)
        .performTextInput(payload)

    delayShort()

    val streamCountBeforeSend = getStreamPayloadLatestTimeString()

    onNodeWithTag(TestTag.RequestSendPayloadButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)

    waitUntil(600.milliseconds().millis) { getStreamPayloadLatestTimeString() != streamCountBeforeSend }
}

suspend fun ComposeUiTest.fireRequest(timeout: KDuration = 1.seconds(), isClientStreaming: Boolean = false, isServerStreaming: Boolean = false) {
    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .assertTextEquals(if (isClientStreaming) "Connect" else "Send")
        .performClickWithRetry(this)

    delayShort()

    // wait for response
    waitUntil(5000L) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty() }
    if (!isClientStreaming && !isServerStreaming) {
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }
    } else {
        waitUntil(1.seconds().millis) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isNotEmpty() }
    }
}

suspend fun ComposeUiTest.completeRequest() {
    onNodeWithTag(TestTag.RequestCompleteStreamButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
}

fun ComposeUiTest.disconnect() {
    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .assertTextEquals("Disconnect")
        .performClickWithRetry(this)

    waitUntil(2.seconds().millis) {
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .fetchSemanticsNode()
            .getTexts() == listOf("Connect")
    }
}

suspend fun ComposeUiTest.delayShort() {
    wait(250L)
}

suspend fun ComposeUiTest.wait(duration: KDuration) {
    wait(duration.toMilliseconds())
}

suspend fun ComposeUiTest.wait(ms: Long) {
    mainClock.advanceTimeBy(ms)
    delay(ms)
    waitForIdle()
}

fun ComposeUiTest.getResponseBody(): String? {
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
        .getTexts()
        .singleOrNull()
    return responseBody
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

fun SemanticsNodeInteraction.assertIsDisplayedWithRetry(host: ComposeUiTest): SemanticsNodeInteraction {
    while (true) {
        try {
            assertIsDisplayed()
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

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
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
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
import com.sunnychung.application.multiplatform.hellohttp.ux.testChooseFile
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
                            UserKeyValuePair("x-My-Header", "defg HIjk"),
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
                            UserKeyValuePair("x-My-Header", "defg HIjk"),
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
                            UserKeyValuePair("x-My-Header", "defg HIjk"),
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

    @Test
    fun echoPostWithoutBody() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
            )
        )
    }

    @Test
    fun echoPostWithRawBody() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Raw,
                        body = StringBody("abcde\n\tfgh中文字_;-+/\\n✌🏽"),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithRawBodyAndHeaderAndQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
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
                        contentType = ContentType.Raw,
                        body = StringBody("abcde\n\tfgh中文字_;-+/\\n✌🏽"),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithJsonBody() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Json,
                        body = StringBody("""
                            {
                                "s": "abcde\\n\tfgh中文字_;-+/\\\\n✌🏽",
                                "abc": 123,
                                "de": true,
                                "fgh": 45.67
                            }
                        """.trimIndent()),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithJsonBodyAndHeaderAndQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
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
                        contentType = ContentType.Json,
                        body = StringBody("""
                            {
                                "s": "abcde\\n\tfgh中文字_;-+/\\\\n✌🏽",
                                "abc": 123,
                                "de": true,
                                "fgh": 45.67
                            }
                        """.trimIndent()),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithFormUrlEncoded() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.FormUrlEncoded,
                        body = FormUrlEncodedBody(listOf(
                            UserKeyValuePair("abcc", "中文字123"),
                            UserKeyValuePair("MyFormParam", "abcc def_gh+i=?j/k"),
                            UserKeyValuePair("emoj", "a\uD83D\uDE0EBC"),
                        )),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithFormUrlEncodedAndHeaderAndQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
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
                        contentType = ContentType.FormUrlEncoded,
                        body = FormUrlEncodedBody(listOf(
                            UserKeyValuePair("abcc", "中文字123"),
                            UserKeyValuePair("MyFormParam", "abcc def_gh+i=?j/k"),
                            UserKeyValuePair("emoj", "a\uD83D\uDE0EBC"),
                        )),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithMultipartStrings() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Multipart,
                        body = MultipartBody(listOf(
                            UserKeyValuePair("abcc", "中文字123"),
                            UserKeyValuePair("MyFormParam", "abcc def_gh+i=?j/k"),
                            UserKeyValuePair("emoj", "a\uD83D\uDE0EBC"),
                        )),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithMultipartFiles() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Multipart,
                        body = MultipartBody(listOf(
                            UserKeyValuePair("abcc", "中文字123"),
                            UserKeyValuePair(
                                id = uuidString(),
                                key = "file2",
                                value = "src/test/resources/testFile2.txt",
                                valueType = FieldValueType.File,
                                isEnabled = true,
                            ),
                            UserKeyValuePair(
                                id = uuidString(),
                                key = "file1",
                                value = "src/test/resources/testFile1中文字.txt",
                                valueType = FieldValueType.File,
                                isEnabled = true,
                            ),
                            UserKeyValuePair("MyFormParam", "abcc def_gh+i=?j/k"),
                            UserKeyValuePair("emoj", "a\uD83D\uDE0EBC"),
                        )),
                    )
                )
            )
        )
    }

    @Test
    fun echoPostWithMultipartFilesAndHeaderAndQueryParameters() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
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
                        contentType = ContentType.Multipart,
                        body = MultipartBody(listOf(
                            UserKeyValuePair("abcc", "中文字123"),
                            UserKeyValuePair(
                                id = uuidString(),
                                key = "file2",
                                value = "src/test/resources/testFile2.txt",
                                valueType = FieldValueType.File,
                                isEnabled = true,
                            ),
                            UserKeyValuePair(
                                id = uuidString(),
                                key = "file1",
                                value = "src/test/resources/testFile1中文字.txt",
                                valueType = FieldValueType.File,
                                isEnabled = true,
                            ),
                            UserKeyValuePair("MyFormParam", "abcc def_gh+i=?j/k"),
                            UserKeyValuePair("emoj", "a\uD83D\uDE0EBC"),
                        )),
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
    waitUntilExactlyOneExists(hasTestTag(TestTag.CreateRequestOrFolderButton.name), 5000L)
}

suspend fun ComposeUiTest.createAndSendHttpRequest(request: UserRequestTemplate, timeout: KDuration = 1.seconds(), isOneOffRequest: Boolean = true) {
    createProjectIfNeeded()
    val baseExample = request.examples.first()

    onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitUntilExactlyOneExists(hasTextExactly("Request", includeEditableText = false))
    onNodeWithText("Request")
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitUntilExactlyOneExists(hasTestTag(TestTag.RequestUrlTextField.name), 1000L)

    delayShort()

    if (request.application == ProtocolApplication.Http && request.method != "GET") {
        // TODO support custom method
        onNodeWithTag(buildTestTag(TestTagPart.RequestMethodDropdown, TestTagPart.DropdownButton)!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        val nextTag = buildTestTag(TestTagPart.RequestMethodDropdown, TestTagPart.DropdownItem, request.method)!!
        waitUntilExactlyOneExists(hasTestTag(nextTag))
        onNodeWithTag(nextTag)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        delayShort()
    }

    onNodeWithTag(TestTag.RequestUrlTextField.name)
        .assertIsDisplayedWithRetry(this)
        .performTextInput(request.url)

    delayShort()

    if (baseExample.contentType != ContentType.None) {
        onNodeWithTag(buildTestTag(TestTagPart.RequestBodyTypeDropdown, TestTagPart.DropdownButton)!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        val nextTag = buildTestTag(TestTagPart.RequestBodyTypeDropdown, TestTagPart.DropdownItem, baseExample.contentType.displayText)!!
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
                    waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                    waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, TestTagPart.Value, index)!!))
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.key)
                    delayShort()
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                        .assertIsDisplayedWithRetry(this)
                        .assertTextEquals(it.key)

                    when (it.valueType) {
                        FieldValueType.String -> {
                            onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, TestTagPart.Value, index)!!))
                                .assertIsDisplayedWithRetry(this)
                                .performTextInput(it.value)
                            delayShort()
                        }
                        FieldValueType.File -> {
                            onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, index, TestTagPart.ValueTypeDropdown, TestTagPart.DropdownButton)!!))
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)
                            delayShort()

                            onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, index, TestTagPart.ValueTypeDropdown, TestTagPart.DropdownItem, "File")!!))
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)
                            delayShort()

                            testChooseFile = File(it.value)
                            val filename = testChooseFile!!.name
                            onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, index, TestTagPart.FileButton)!!))
                                .assertIsDisplayedWithRetry(this)
                                .performClickWithRetry(this)

                            delay(100)
                            delayShort()
                            onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyMultipartForm, TestTagPart.Current, index, TestTagPart.FileButton)!!))
                                .assertTextEquals(filename, includeEditableText = false)

                        }
                    }
                }
            }

            ContentType.FormUrlEncoded -> {
                val body = (baseExample.body as FormUrlEncodedBody).value
                body.forEachIndexed { index, it ->
                    waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestBodyFormUrlEncodedForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                    waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestBodyFormUrlEncodedForm, TestTagPart.Current, TestTagPart.Value, index)!!))
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFormUrlEncodedForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.key)
                    delayShort()

                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFormUrlEncodedForm, TestTagPart.Current, TestTagPart.Key, index)!!))
                        .assertIsDisplayedWithRetry(this)
                        .assertTextEquals(it.key)
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFormUrlEncodedForm, TestTagPart.Current, TestTagPart.Value, index)!!))
                        .assertIsDisplayedWithRetry(this)
                        .performTextInput(it.value)
                    delayShort()
                }
            }
            ContentType.BinaryFile -> TODO()
            ContentType.Graphql -> TODO()
            ContentType.None -> throw IllegalStateException()
        }
    }

    if (baseExample.queryParameters.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Query")))
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        baseExample.queryParameters.forEachIndexed { index, it ->
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Value, index)!!))
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
                .assertIsDisplayedWithRetry(this)
                .performTextInput(it.key)
            delayShort()
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Key, index)!!))
                .assertIsDisplayedWithRetry(this)
                .assertTextEquals(it.key)
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestQueryParameter, TestTagPart.Current, TestTagPart.Value, index)!!))
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
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Key, index)!!))
            waitUntilExactlyOneExists(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
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

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
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
    assertEquals(request.method, resp.method)
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
    if (baseExample.body is MultipartBody) {
        val body = (baseExample.body as MultipartBody).value.sortedBy { it.key }
        resp.multiparts.sortedBy { it.name }.forEachIndexed { index, part ->
            val reqPart = body[index]
            assertEquals(reqPart.key, part.name)
            when (reqPart.valueType) {
                FieldValueType.String -> assertEquals(reqPart.value, part.data)
                FieldValueType.File -> {
                    val file = File(reqPart.value)
                    assertEquals(file.length().toInt(), part.size)
                    if (part.data != null) {
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
        is FileBody -> TODO()
        is GraphqlBody -> TODO()
        is StringBody -> assertEquals(body.value, resp.body)
    }
}

suspend fun ComposeUiTest.delayShort() {
    delay(250L)
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

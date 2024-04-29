@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
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
        createAndSendHttpRequest(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
            )
        )
        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
        val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
            .getTexts()
            .single()
        val resp = jacksonObjectMapper().readValue(responseBody, RequestData::class.java)
        assertEquals("GET", resp.method)
        assertEquals("/rest/echo", resp.path)
        assertTrue { resp.headers.size > 1 }
        assertEquals(0, resp.queryParameters.size)
        assertEquals(0, resp.formData.size)
        assertEquals(0, resp.multiparts.size)
        assertEquals(null, resp.body)
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

    onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
        .performClick()
    waitUntilExactlyOneExists(hasTextExactly("Request", includeEditableText = false))
    onNodeWithText("Request")
        .performClick()
    waitUntilExactlyOneExists(hasTestTag(TestTag.RequestUrlTextField.name), 1000L)

    onNodeWithTag(TestTag.RequestUrlTextField.name)
        .performTextInput(request.url)

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delay(400L)
    waitForIdle() // prevent illegal state after sleeping

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .performClick()
    waitForIdle()

    // wait for response
    delay(400L)
    waitForIdle()
    if (isOneOffRequest) {
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty() }
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

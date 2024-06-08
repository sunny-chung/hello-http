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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
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
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.executeWithTimeout
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.testChooseFile
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.awt.Dimension
import java.io.File
import java.net.URL

fun runTest(testBlock: suspend ComposeUiTest.() -> Unit) =
    executeWithTimeout(120.seconds()) {
        try {
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
                runBlocking { // don't use Dispatchers.Main, or most tests would fail with ComposeTimeoutException
                    testBlock()
                }
            }
        } catch (e: Throwable) {
            RuntimeException("Exception thrown during test", e)
                .printStackTrace()
            throw e
        } finally { // await repositories to finish update operations regardless of success or error, so that it won't pollute the next test case
            println("UX test case ends, await all repositories updates")
            val numActiveCalls = AppContext.NetworkClientManager.cancelAllCalls()
            runBlocking {
                if (numActiveCalls > 0) {
                    delay(2.seconds().millis) // wait for cancelling calls
                }

                AppContext.allRepositories.forEach {
                    it.awaitAllUpdates()
                }
            }
            println("All repositories updated. Finish test case.")
        }
    }

enum class TestEnvironment(val displayName: String) {
    Local("Local"),
    LocalSsl("Local-SSL"),
    LocalMTls("Local-mTLS"),
}

suspend fun ComposeUiTest.createProjectIfNeeded() {
    if (runOnUiThread { onAllNodesWithTag(TestTag.FirstTimeCreateProjectButton.name).fetchSemanticsNodes().isNotEmpty() }) {
        // create first project
        runOnUiThread {
            onNodeWithTag(TestTag.FirstTimeCreateProjectButton.name)
                .performClickWithRetry(this)
        }
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 1500L)
        runOnUiThread {
            onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
                .performTextInput("Test Project ${KInstant.now().format("HH:mm:ss")}")
        }
        waitForIdle()
        runOnUiThread {
            onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
                .performClickWithRetry(this)
        }

        // create first subproject
        delayShort()
        waitForIdle()
        waitUntilExactlyOneExists(hasTestTag(TestTag.FirstTimeCreateSubprojectButton.name), 1500L)
        runOnUiThread {
            onNodeWithTag(TestTag.FirstTimeCreateSubprojectButton.name)
                .performClickWithRetry(this)
        }
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 1500L)
        runOnUiThread {
            onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
                .performTextInput("Test Subproject")
        }
        waitForIdle()
        runOnUiThread {
            onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitUntil {
            runOnUiThread {
                onAllNodesWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }

        println("created first project and subproject")

        waitForIdle()
        runOnUiThread {
            onNodeWithTag(TestTag.EditEnvironmentsButton.name)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitUntilExactlyOneExists(hasTestTag(TestTag.EnvironmentDialogCreateButton.name))

        createEnvironmentInEnvDialog(TestEnvironment.Local.displayName)

        fun switchToSslTabAndAddServerCaCert() {
            runOnUiThread {
                onNode(hasTestTag(TestTag.EnvironmentEditorTab.name).and(hasTextExactly("SSL")))
                    .assertIsDisplayedWithRetry(this)
                    .performClickWithRetry(this)
            }

            waitUntil {
                runOnUiThread {
                    onAllNodesWithTag(
                        buildTestTag(
                            TestTagPart.EnvironmentSslTrustedServerCertificates,
                            TestTagPart.CreateButton
                        )!!
                    )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            mockChosenFile(File("../test-common/src/main/resources/tls/serverCACert.pem"))
            runOnUiThread {
                onNodeWithTag(
                    buildTestTag(
                        TestTagPart.EnvironmentSslTrustedServerCertificates,
                        TestTagPart.CreateButton
                    )!!
                )
                    .assertIsDisplayedWithRetry(this)
                    .performClickWithRetry(this)
            }
            waitUntil(3.seconds().millis) {
                runOnUiThread {
                    onAllNodes(
                        hasTestTag(
                            buildTestTag(
                                TestTagPart.EnvironmentSslTrustedServerCertificates,
                                TestTagPart.ListItemLabel
                            )!!
                        )
                            .and(hasText("CN=Test Server CA", substring = true)),
                        useUnmergedTree = true
                    )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }
        }

        createEnvironmentInEnvDialog(TestEnvironment.LocalSsl.displayName)
        switchToSslTabAndAddServerCaCert()

        createEnvironmentInEnvDialog(TestEnvironment.LocalMTls.displayName)
        switchToSslTabAndAddServerCaCert()
        selectDropdownItem(TestTagPart.EnvironmentDisableSystemCaCertificates.name, "Yes")
        runOnUiThread {
            onNodeWithTag(TestTag.EnvironmentEditorSslTabContent.name, useUnmergedTree = true)
                .performScrollToNode(
                    hasTestTag(
                        buildTestTag(
                            TestTagPart.EnvironmentSslClientCertificates,
                            TestTagPart.CreateButton,
                        )!!
                    )
                )
        }
        waitUntil {
            runOnUiThread {
                onNodeWithTag(buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.CreateButton,
                )!!)
                    .isDisplayed()
            }
        }
        delay(1.seconds().millis)
        waitForIdle()
        val certFile = mockChosenFile(File("../test-common/src/main/resources/tls/clientCert.pem"))
        runOnUiThread {
            onNodeWithTag(
                buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.ClientCertificate,
                    TestTagPart.FileButton,
                )!!
            )
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitForIdle()
        waitUntil(3.seconds().millis) {
            runOnUiThread {
                onNodeWithTag(
                    buildTestTag(
                        TestTagPart.EnvironmentSslClientCertificates,
                        TestTagPart.ClientCertificate,
                        TestTagPart.FileButton,
                    )!!
                )
                    .fetchSemanticsNode()
                    .getTexts()
                    .firstOrNull() == certFile.name
            }
        }
            val keyFile = mockChosenFile(File("../test-common/src/main/resources/tls/clientKey.pkcs8.der"))
        runOnUiThread {
            onNodeWithTag(
                buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.PrivateKey,
                    TestTagPart.FileButton,
                )!!
            )
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitForIdle()
        waitUntil(3.seconds().millis) {
            runOnUiThread {
                onNodeWithTag(
                    buildTestTag(
                        TestTagPart.EnvironmentSslClientCertificates,
                        TestTagPart.PrivateKey,
                        TestTagPart.FileButton,
                    )!!
                )
                    .fetchSemanticsNode()
                    .getTexts()
                    .firstOrNull() == keyFile.name
            }
        }
        runOnUiThread {
            onNodeWithTag(
                buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.CreateButton,
                )!!
            )
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitUntil(3.seconds().millis) {
            runOnUiThread {
                onAllNodes(
                    hasTestTag(
                        buildTestTag(
                            TestTagPart.EnvironmentSslClientCertificates,
                            TestTagPart.ListItemLabel
                        )!!
                    )
                        .and(hasText("CN=Test Client", substring = true)),
                    useUnmergedTree = true
                )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
//                .firstOrNull()
//                ?.getTexts()
//                ?.firstOrNull().also { println(">>> CC = $it") }
//                ?.contains("CN=Test Client") == true
            }
        }

        runOnUiThread {
            onNodeWithTag(TestTag.DialogCloseButton.name)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
        waitUntil {
            runOnUiThread {
                onAllNodesWithTag(TestTag.DialogCloseButton.name).fetchSemanticsNodes().isEmpty()
            }
        }
        waitForIdle()
    }
    runOnUiThread {
        waitUntilExactlyOneExists(hasTestTag(TestTag.CreateRequestOrFolderButton.name), 5000L)
    }
}

fun ComposeUiTest.mockChosenFile(file: File): File {
    testChooseFile = file
    return file
}

suspend fun ComposeUiTest.selectEnvironment(environment: TestEnvironment) {
    if (runOnUiThread {
            onNodeWithTag(buildTestTag(TestTagPart.EnvironmentDropdown, TestTagPart.DropdownLabel)!!, useUnmergedTree = true)
                .assertIsDisplayedWithRetry(this)
                .fetchSemanticsNode()
                .getTexts()
                .first() == environment.displayName
        }
    ) {
        return
    }

    selectDropdownItem(TestTagPart.EnvironmentDropdown.name, environment.displayName)
    delayShort()
    waitForIdle()
}

/**
 * @param name A unique name.
 */
suspend fun ComposeUiTest.createEnvironmentInEnvDialog(name: String) {
    runOnUiThread {
        onNodeWithTag(TestTag.EnvironmentDialogCreateButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }

    waitUntil {
        runOnUiThread {
            onAllNodes(
                hasTestTag(TestTag.EnvironmentDialogEnvNameTextField.name)
                    .and(isFocusable())
                    .and(hasTextExactly("New Environment"))
            )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    runOnUiThread {
        onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextClearance()
    }

    delayShort()
    waitForIdle()
    runOnUiThread {
        onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
            .performTextInput(name)
    }

    waitUntil(3.seconds().millis) {
        runOnUiThread {
            // one in list view and one in text field
            onAllNodesWithText(name).fetchSemanticsNodes().size == 2
        }
    }
}

fun ComposeUiTest.selectRequestMethod(itemDisplayText: String) {
    // TODO support custom method
    selectDropdownItem(TestTagPart.RequestMethodDropdown.name, itemDisplayText)
}

fun ComposeUiTest.selectDropdownItem(testTagPart: String, itemDisplayText: String) {
    val itemTag = buildTestTag(testTagPart, TestTagPart.DropdownItem, itemDisplayText)!!
    // if drop down menu is expanded, click the item directly; otherwise, open the menu first.
    if (runOnUiThread { onAllNodesWithTag(itemTag).fetchSemanticsNodes().isEmpty() }) {
        runOnUiThread {
            onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownButton)!!)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }

        waitUntilExactlyOneExists(hasTestTag(itemTag), 3.seconds().millis)
        runOnUiThread {
            onNodeWithTag(itemTag)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
    }
}

suspend fun ComposeUiTest.createRequest(request: UserRequestTemplate, environment: TestEnvironment) {
    createProjectIfNeeded()
    selectEnvironment(environment)
    val baseExample = request.examples.first()

    runOnUiThread {
        onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }
    delayShort()
    waitUntilExactlyOneExists(hasTextExactly("Request", includeEditableText = false))
    runOnUiThread {
        onNodeWithText("Request")
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }
    waitUntilExactlyOneExists(hasTestTag(TestTag.RequestUrlTextField.name), 1000L)

    delayShort()

    if (request.application == ProtocolApplication.Http && request.method != "GET") {
        selectRequestMethod(request.method)
        delayShort()
    }

    runOnUiThread {
        onNodeWithTag(TestTag.RequestUrlTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextInput(request.url)
    }

    delayShort()

    if (request.application == ProtocolApplication.Http && baseExample.contentType != ContentType.None) {
        runOnUiThread {
            onNodeWithTag(buildTestTag(TestTagPart.RequestBodyTypeDropdown, TestTagPart.DropdownButton)!!)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }

        delayShort()

        val nextTag = buildTestTag(
            TestTagPart.RequestBodyTypeDropdown,
            TestTagPart.DropdownItem,
            baseExample.contentType.displayText
        )!!
        waitUntilExactlyOneExists(hasTestTag(nextTag))
        runOnUiThread {
            onNodeWithTag(nextTag)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }

        delayShort()

        when (baseExample.contentType) {
            ContentType.Json, ContentType.Raw -> {
                val body = (baseExample.body as StringBody).value
                if (body.isNotEmpty()) {
                    runOnUiThread {
                        onNodeWithTag(TestTag.RequestStringBodyTextField.name)
                            .assertIsDisplayedWithRetry(this)
                            .performTextInput(body)
                    }
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
                    runOnUiThread {
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
                    }
                    delayShort()
                    runOnUiThread {
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
                    }

                    when (it.valueType) {
                        FieldValueType.String -> {
                            runOnUiThread {
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
                            }
                            delayShort()
                        }

                        FieldValueType.File -> {
                            runOnUiThread {
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
                            }
                            delayShort()

                            runOnUiThread {
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
                            }
                            delayShort()

                            val filename = mockChosenFile(File(it.value)).name
                            runOnUiThread {
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
                            }

                            waitUntil(3.seconds().millis) {
                                runOnUiThread {
                                    onAllNodes(
                                        hasTestTag(
                                            buildTestTag(
                                                TestTagPart.RequestBodyMultipartForm,
                                                TestTagPart.Current,
                                                index,
                                                TestTagPart.FileButton
                                            )!!
                                        ).and(
                                            hasTextExactly(filename, includeEditableText = false)
                                        )
                                    )
                                        .fetchSemanticsNodes()
                                        .isNotEmpty()
                                }
                            }

//                            delay(100L)
//                            mainClock.advanceTimeBy(100L)
//                            delayShort()
//                            onNode(
//                                hasTestTag(
//                                    buildTestTag(
//                                        TestTagPart.RequestBodyMultipartForm,
//                                        TestTagPart.Current,
//                                        index,
//                                        TestTagPart.FileButton
//                                    )!!
//                                )
//                            )
//                                .assertTextEquals(filename, includeEditableText = false)

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
                    runOnUiThread {
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
                    }
                    delayShort()

                    runOnUiThread {
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
                    }
                    delayShort()
                }
            }

            ContentType.BinaryFile -> {
                val body = baseExample.body as FileBody
                testChooseFile = File(body.filePath!!)
                val filename = testChooseFile!!.name
                runOnUiThread {
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFileForm, TestTagPart.FileButton)!!))
                        .assertIsDisplayedWithRetry(this)
                        .performClickWithRetry(this)
                }

                delay(100L)
                runOnUiThread {
                    mainClock.advanceTimeBy(100L)
                }
                delayShort()
                runOnUiThread {
                    onNode(hasTestTag(buildTestTag(TestTagPart.RequestBodyFileForm, TestTagPart.FileButton)!!))
                        .assertTextEquals(filename, includeEditableText = false)
                }
            }

            ContentType.Graphql -> TODO()
            ContentType.None -> throw IllegalStateException()
        }
    }

    if (baseExample.queryParameters.isNotEmpty()) {
        runOnUiThread {
            onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Query")))
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }

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
            runOnUiThread {
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
            }
            delayShort()
            runOnUiThread {
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
            }
            delayShort()
        }
    }

    if (baseExample.headers.isNotEmpty()) {
        runOnUiThread {
            onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Header")))
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }
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
            runOnUiThread {
                onNode(
                    hasTestTag(
                        buildTestTag(
                            TestTagPart.RequestHeader,
                            TestTagPart.Current,
                            TestTagPart.Key,
                            index
                        )!!
                    )
                )
                    .assertIsDisplayedWithRetry(this)
                    .performTextInput(it.key)
            }
            delayShort()
            runOnUiThread {
                onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
                    .assertIsDisplayedWithRetry(this)
                    .performTextInput(it.value)
            }
            delayShort()
        }
    }
}

suspend fun ComposeUiTest.createAndSendHttpRequest(request: UserRequestTemplate, timeout: KDuration = 2500.milliseconds(), isOneOffRequest: Boolean = true, isExpectResponseBody: Boolean = false, environment: TestEnvironment) {
    createRequest(request = request, environment = environment)

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delayShort()
    waitForIdle()
    runOnUiThread {
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }
    waitForIdle()

    // wait for response
    waitUntil(5000L) {
        runOnUiThread {
            onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty()
        }
    }
    if (isOneOffRequest) {
        waitUntil(maxOf(1L, timeout.millis)) {
            runOnUiThread {
                onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty()
            }
        }
    }

    if (isExpectResponseBody) {
        waitUntil(1500.milliseconds().millis) {
            runOnUiThread {
                onAllNodesWithTag(TestTag.ResponseBody.name).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}

suspend fun ComposeUiTest.createAndSendRestEchoRequestAndAssertResponse(request: UserRequestTemplate, timeout: KDuration = 2500.milliseconds(), environment: TestEnvironment) {
    val baseExample = request.examples.first()
    val isAssertBodyContent = request.url.endsWith("/rest/echo")
    createAndSendHttpRequest(request = request, timeout = timeout, environment = environment, isExpectResponseBody = true)

    val responseBody = runOnUiThread {
        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
        onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
            .getTexts()
            .single()
    }
    println(responseBody)
    val resp = jacksonObjectMapper().readValue(responseBody, RequestData::class.java)
    assertEquals(request.method, resp.method)
    assertEquals(URL(request.url).path, resp.path)
    assertTrue(resp.headers.size >= 2) // at least have "Host" and "User-Agent" headers
    if (baseExample.headers.isNotEmpty()) {
        assertTrue(resp.headers.map { Parameter(it.name.lowercase(), it.value) }
            .containsAll(baseExample.headers.map { Parameter(it.key.lowercase(), it.value) }))
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
        return runOnUiThread {
            (onAllNodesWithTag(TestTag.ResponseStreamLogItemTime.name, useUnmergedTree = true)
                .fetchSemanticsNodes()//.also { println("getStreamPayloadLatestTimeString() size ${it.size}") }
                .firstOrNull()
                ?.getTexts()
                ?.firstOrNull()
                ?: "")
                .also {
                    println("getStreamPayloadLatestTimeString() = $it")
                }
        }
    }

    if (isCreatePayloadExample) {
        delayShort()

        runOnUiThread {
            onNodeWithTag(TestTag.RequestAddPayloadExampleButton.name)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
        }

        delayShort()

        runOnUiThread {
            onNodeWithTag(TestTag.RequestPayloadTextField.name)
                .assertIsDisplayedWithRetry(this)
                .assertTextEquals("")
        }
    }

    runOnUiThread {
        onNodeWithTag(TestTag.RequestPayloadTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextInput(payload)
    }

    delayShort()

    val streamCountBeforeSend = getStreamPayloadLatestTimeString()

    runOnUiThread {
        onNodeWithTag(TestTag.RequestSendPayloadButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }

    waitUntil(600.milliseconds().millis) { getStreamPayloadLatestTimeString() != streamCountBeforeSend }
}

suspend fun ComposeUiTest.fireRequest(timeout: KDuration = 1.seconds(), isClientStreaming: Boolean = false, isServerStreaming: Boolean = false) {
    runOnUiThread {
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals(if (isClientStreaming) "Connect" else "Send")
            .performClickWithRetry(this)
    }

    delayShort()

    // wait for response
    waitUntil(5000L) {
        runOnUiThread {
            onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodes().isNotEmpty()
        }
    }
    if (!isClientStreaming && !isServerStreaming) {
        waitUntil(maxOf(1L, timeout.millis)) {
            runOnUiThread {
                onAllNodesWithText("Communicating").fetchSemanticsNodes().isEmpty()
            }
        }
    } else {
        waitUntil(1.seconds().millis) {
            runOnUiThread {
                onAllNodesWithText("Communicating").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}

suspend fun ComposeUiTest.completeRequest() {
    runOnUiThread {
        onNodeWithTag(TestTag.RequestCompleteStreamButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
    }
}

fun ComposeUiTest.disconnect() {
    runOnUiThread {
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .assertTextEquals("Disconnect")
            .performClickWithRetry(this)
    }

    waitUntil(2.seconds().millis) {
        runOnUiThread {
            onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
                .assertIsDisplayedWithRetry(this)
                .fetchSemanticsNode()
                .getTexts() == listOf("Connect")
        }
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
    val responseBody = runOnUiThread {
        onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
            .getTexts()
            .singleOrNull()
    }
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

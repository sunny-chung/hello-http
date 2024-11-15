@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
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
import androidx.compose.ui.test.runDesktopComposeUiTest
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
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.test.payload.Parameter
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.executeWithTimeout
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownDisplayTexts
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import com.sunnychung.application.multiplatform.hellohttp.ux.buildTestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.testChooseFile
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skiko.toBufferedImage
import org.jetbrains.skiko.toImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File
import java.net.URL

fun runTest(testBlock: suspend DesktopComposeUiTest.() -> Unit) =
    executeWithTimeout(120.seconds()) {
        try {
            runDesktopComposeUiTest {
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
            System.err.println("[${KZonedInstant.nowAtLocalZoneOffset()}] Exception thrown during test")
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
    LocalDefault("Local Default"),
    LocalHttp2Only("Local HTTP/2 Only"),
    LocalHttp1Only("Local HTTP/1 Only"),
    LocalHttp1Ssl("Local HTTP/1 SSL"),
    LocalHttp2Ssl("Local HTTP/2 SSL"),
    LocalSsl("Local SSL"),
    LocalMTls("Local mTLS"),
}

suspend fun DesktopComposeUiTest.createProjectIfNeeded() {
    if (onAllNodesWithTag(TestTag.FirstTimeCreateProjectButton.name).fetchSemanticsNodesWithRetry(this).isNotEmpty()) {
        // create first project
        onNodeWithTag(TestTag.FirstTimeCreateProjectButton.name)
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 1500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput(this, "Test Project ${KZonedInstant.nowAtLocalZoneOffset().format("HH:mm:ss")}")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .performClickWithRetry(this)

        // create first subproject
        delayShort()
        waitForIdle()
        waitUntilExactlyOneExists(hasTestTag(TestTag.FirstTimeCreateSubprojectButton.name), 1500L)
        onNodeWithTag(TestTag.FirstTimeCreateSubprojectButton.name)
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(hasTestTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name), 1500L)
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name)
            .performTextInput(this, "Test Subproject")
        waitForIdle()
        onNodeWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitUntil {
            onAllNodesWithTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name)
                .fetchSemanticsNodesWithRetry(this)
                .isEmpty()
        }

        println("created first project and subproject")

        // below create multiple environments

        waitForIdle()
        onNodeWithTag(TestTag.EditEnvironmentsButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(hasTestTag(TestTag.EnvironmentDialogCreateButton.name))

        createEnvironmentInEnvDialog(TestEnvironment.LocalDefault.displayName)

        fun switchToHttpTabAndUpdateHttpVersion(httpProtocolVersion: HttpConfig.HttpProtocolVersion?) {
            if (httpProtocolVersion == null) {
                return
            }

            val httpProtocolVersionCaption = when (httpProtocolVersion) {
                HttpConfig.HttpProtocolVersion.Http1Only -> "HTTP/1 only"
                HttpConfig.HttpProtocolVersion.Http2Only -> "HTTP/2 only"
                HttpConfig.HttpProtocolVersion.Negotiate -> "Prefer HTTP/2"
            }

            onNode(hasTestTag(TestTag.EnvironmentEditorTab.name).and(hasTextExactly("HTTP")))
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)

            waitUntil {
                onAllNodesWithTag(buildTestTag(
                    TestTagPart.EnvironmentHttpProtocolVersionDropdown,
                    TestTagPart.DropdownButton
                )!!)
                    .fetchSemanticsNodesWithRetry(this)
                    .isNotEmpty()
            }
            selectDropdownItem(TestTagPart.EnvironmentHttpProtocolVersionDropdown.name, httpProtocolVersionCaption)
        }

        fun switchToSslTabAndAddServerCaCert() {
            var retryAttempt = 0
            while (true) { // add this loop because failing with ComposeTimeoutException
                waitForIdle()

                onNode(hasTestTag(TestTag.EnvironmentEditorTab.name).and(hasTextExactly("SSL")))
                    .assertIsDisplayedWithRetry(this)
                    .performClickWithRetry(this)

                waitForIdle()

                try {
                    waitUntil {
                        onAllNodesWithTag(
                            buildTestTag(
                                TestTagPart.EnvironmentSslTrustedServerCertificates,
                                TestTagPart.CreateButton
                            )!!
                        )
                            .fetchSemanticsNodesWithRetry(this)
                            .isNotEmpty()
                    }
                } catch (e: ComposeTimeoutException) {
                    e.printStackTrace()
                    println("Retry the buggy Compose Test click until passing. #attempt: ${++retryAttempt}")
                    continue
                }
                break
            }
            waitForIdle()

            mockChosenFile(File("../test-common/src/main/resources/tls/serverCACert.pem"))
            onNodeWithTag(buildTestTag(TestTagPart.EnvironmentSslTrustedServerCertificates, TestTagPart.CreateButton)!!)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
            waitUntil(3.seconds().millis) {
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
                    .fetchSemanticsNodesWithRetry(this)
                    .isNotEmpty()
            }
            waitForIdle()
        }

        createEnvironmentInEnvDialog(TestEnvironment.LocalSsl.displayName)
        switchToSslTabAndAddServerCaCert()

        createEnvironmentInEnvDialog(TestEnvironment.LocalMTls.displayName)
        switchToSslTabAndAddServerCaCert()
        selectDropdownItem(TestTagPart.EnvironmentDisableSystemCaCertificates.name, "Yes")
        onNodeWithTag(TestTag.EnvironmentEditorSslTabContent.name, useUnmergedTree = true)
            .performScrollToNode(hasTestTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.Bundle,
                TestTagPart.FileButton,
            )!!))
        waitUntil {
            onNodeWithTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.Bundle,
                TestTagPart.FileButton,
            )!!)
                .isDisplayed()
        }
        delay(1.seconds().millis)
        waitForIdle()
        val certFile = mockChosenFile(File("../test-common/src/main/resources/tls/clientCert.pem"))
        onNodeWithTag(buildTestTag(
            TestTagPart.EnvironmentSslClientCertificates,
            TestTagPart.ClientCertificate,
            TestTagPart.FileButton,
        )!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitForIdle()
        waitUntil(3.seconds().millis) {
            onNodeWithTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.ClientCertificate,
                TestTagPart.FileButton,
            )!!)
                .fetchSemanticsNodeWithRetry(this)
                .getTexts()
                .firstOrNull() == certFile.name
        }
        val keyFile = mockChosenFile(File("../test-common/src/main/resources/tls/clientKey.pkcs8.der"))
        onNodeWithTag(buildTestTag(
            TestTagPart.EnvironmentSslClientCertificates,
            TestTagPart.PrivateKey,
            TestTagPart.FileButton,
        )!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitForIdle()
        waitUntil(3.seconds().millis) {
            onNodeWithTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.PrivateKey,
                TestTagPart.FileButton,
            )!!)
                .fetchSemanticsNodeWithRetry(this)
                .getTexts()
                .firstOrNull() == keyFile.name
        }

        onNodeWithTag(TestTag.EnvironmentEditorSslTabContent.name, useUnmergedTree = true)
            .performScrollToNode(hasTestTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.CreateButton,
            )!!))
        waitUntil {
            onNodeWithTag(buildTestTag(
                TestTagPart.EnvironmentSslClientCertificates,
                TestTagPart.CreateButton,
            )!!)
                .isDisplayed()
        }

        retryForUnresponsiveBuggyComposeTest {
            onNodeWithTag(
                buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.CreateButton,
                )!!
            )
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)
            waitUntil(3.seconds().millis) {
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
                    .fetchSemanticsNodesWithRetry(this)
                    .isNotEmpty()
//                .firstOrNull()
//                ?.getTexts()
//                ?.firstOrNull().also { println(">>> CC = $it") }
//                ?.contains("CN=Test Client") == true
            }
        }
        waitForIdle()

        createEnvironmentInEnvDialog(TestEnvironment.LocalHttp1Only.displayName)
        switchToHttpTabAndUpdateHttpVersion(HttpConfig.HttpProtocolVersion.Http1Only)

        createEnvironmentInEnvDialog(TestEnvironment.LocalHttp1Ssl.displayName)
        switchToHttpTabAndUpdateHttpVersion(HttpConfig.HttpProtocolVersion.Http1Only)
        switchToSslTabAndAddServerCaCert()

        createEnvironmentInEnvDialog(TestEnvironment.LocalHttp2Only.displayName)
        switchToHttpTabAndUpdateHttpVersion(HttpConfig.HttpProtocolVersion.Http2Only)

        createEnvironmentInEnvDialog(TestEnvironment.LocalHttp2Ssl.displayName)
        switchToHttpTabAndUpdateHttpVersion(HttpConfig.HttpProtocolVersion.Http2Only)
        switchToSslTabAndAddServerCaCert()

        onNodeWithTag(TestTag.DialogCloseButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitUntil { onAllNodesWithTag(TestTag.DialogCloseButton.name).fetchSemanticsNodesWithRetry(this).isEmpty() }
        waitForIdle()
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

fun DesktopComposeUiTest.mockChosenFile(file: File): File {
    testChooseFile = file
    return file
}

suspend fun DesktopComposeUiTest.selectEnvironment(environment: TestEnvironment) {
    if (onNodeWithTag(buildTestTag(TestTagPart.EnvironmentDropdown, TestTagPart.DropdownLabel)!!, useUnmergedTree = true)
        .assertIsDisplayedWithRetry(this)
        .fetchSemanticsNodeWithRetry(this)
        .getTexts()
        .first() == environment.displayName
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
suspend fun DesktopComposeUiTest.createEnvironmentInEnvDialog(name: String) {
    println("createEnvironmentInEnvDialog start '$name'")

    var retryAttempt = 0
    while (true) { // add this loop because the click on EnvironmentDialogCreateButton often is not performed
        waitForIdle()

        onNodeWithTag(TestTag.EnvironmentDialogCreateButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this) // this click often is not performed

        waitForIdle()

        try {
            waitUntil {
                println("EnvironmentDialogEnvNameTextField: [${
                    onAllNodesWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
                        .fetchSemanticsNodesWithRetry(this)
                        .joinToString { it.config.toString() }
                }]")

                onAllNodes(
                    hasTestTag(TestTag.EnvironmentDialogEnvNameTextField.name)
                        .and(isFocusable())
                        .and(hasTextExactly("New Environment"))
                )
                    .fetchSemanticsNodesWithRetry(this)
                    .isNotEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            e.printStackTrace()
            println("Retry the buggy Compose Test click until passing. #attempt: ${++retryAttempt}")
            continue
        }
        break
    }

    do {
        onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextClearance() // not always working

        delayShort()
        waitForIdle()
//        println("Env Text: [${
//            onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
//                .fetchSemanticsNodeWithRetry(this)
//                .getTexts()
//        }]")
    } while(
        onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
            .fetchSemanticsNodeWithRetry(this)
            .getTexts()
            .filter { it != "Environment Name" }
            .isNotEmpty()
    )

    onNodeWithTag(TestTag.EnvironmentDialogEnvNameTextField.name)
        .performTextInput(this, name)

    delayShort()
    waitForIdle()

    try {
        waitUntil(30.seconds().millis) { // easy to fail
            waitForIdle()

            // one in list view and one in text field
            onAllNodesWithText(name, useUnmergedTree = true).fetchSemanticsNodesWithRetry(this).size == 2
        }
    } catch (e: ComposeTimeoutException) {
//        val screenshot = captureToImage().asSkiaBitmap().toBufferedImage().toImage()
//        File("test-error.png").writeBytes(screenshot.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        captureScreenToFile("createEnvironmentInEnvDialog")
        throw e
    }

    waitForIdle()

    println("createEnvironmentInEnvDialog done '$name'")
    println("createEnvironmentInEnvDialog '$name' list ${onAllNodesWithText(name).fetchSemanticsNodesWithRetry(this).joinToString("|") { it.config.toString() }}")
}

fun DesktopComposeUiTest.selectRequestMethod(itemDisplayText: String) {
    // TODO support custom method
    selectDropdownItem(TestTagPart.RequestMethodDropdown.name, itemDisplayText)
}

fun DesktopComposeUiTest.selectDropdownItem(testTagPart: String, itemDisplayText: String, assertDisplayText: String = itemDisplayText) {
    val itemTag = buildTestTag(testTagPart, TestTagPart.DropdownItem, itemDisplayText)!!
    // if drop down menu is expanded, click the item directly; otherwise, open the menu first.
    if (onAllNodesWithTag(itemTag, useUnmergedTree = true).fetchSemanticsNodesWithRetry(this).isEmpty()) {
        onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownButton)!!)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        waitUntil(3.seconds().millis) {
            onAllNodes(hasTestTag(buildTestTag(testTagPart, TestTagPart.DropdownMenu)!!))
                .fetchSemanticsNodesWithRetry(this)
                .size == 1
        }

        println("DropdownMenu items: ${
            onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownMenu)!!)
                .fetchSemanticsNodeWithRetry(this)
                .config
                .getOrNull(DropDownDisplayTexts)
        }")

        onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownMenu)!!)
            .performScrollToNode(hasTestTag(itemTag))

        waitUntil(3.seconds().millis) {
            onAllNodes(hasTestTag(itemTag))
                .fetchSemanticsNodesWithRetry(this)
                .size == 1
        }
    }
    onNodeWithTag(buildTestTag(testTagPart, TestTagPart.DropdownMenu)!!)
        .performScrollToNode(hasTestTag(itemTag))
    
    onNodeWithTag(itemTag, useUnmergedTree = true)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitUntil {
        onNodeWithTag(buildTestTag(
            testTagPart,
            TestTagPart.DropdownLabel
        )!!, useUnmergedTree = true)
            .fetchSemanticsNodeWithRetry(this)
            .getTexts() == listOf(assertDisplayText)
    }
}

suspend fun DesktopComposeUiTest.createRequest(request: UserRequestTemplate, environment: TestEnvironment?) {
    createProjectIfNeeded()
    if (environment != null) {
        selectEnvironment(environment)
    }
    println("start run createRequest content ---")
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
        .performTextInput(this, request.url)

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
                        .performTextInput(this, body)
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
                        .performTextInput(this, it.key)
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
                                .performTextInput(this, it.value)
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

                            val filename = mockChosenFile(File(it.value)).name
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

                            waitUntil(3.seconds().millis) {
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
                                    .fetchSemanticsNodesWithRetry(this)
                                    .isNotEmpty()
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
                        .performTextInput(this, it.key)
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
                        .performTextInput(this, it.value)
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
                .performTextInput(this, it.key)
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
                .performTextInput(this, it.value)
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
                .performTextInput(this, it.key)
            delayShort()
            onNode(hasTestTag(buildTestTag(TestTagPart.RequestHeader, TestTagPart.Current, TestTagPart.Value, index)!!))
                .assertIsDisplayedWithRetry(this)
                .performTextInput(this, it.value)
            delayShort()
        }
    }

    if (baseExample.preFlight.isNotEmpty()) {
        onNode(hasTestTag(TestTag.RequestParameterTypeTab.name).and(hasTextExactly("Pre Flight")))
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        waitUntilExactlyOneExists(hasTestTag(TestTag.RequestPreFlightScriptTextField.name))

        onNode(hasTestTag(TestTag.RequestPreFlightScriptTextField.name))
            .assertIsDisplayedWithRetry(this)
            .performTextInput(this, baseExample.preFlight.executeCode)

        waitUntil {
            onNode(hasTestTag(TestTag.RequestPreFlightScriptTextField.name))
                .fetchSemanticsNodeWithRetry(this)
                .getTexts()
                .joinToString("") == baseExample.preFlight.executeCode
        }
    }
}

suspend fun DesktopComposeUiTest.createAndSendHttpRequest(request: UserRequestTemplate, timeout: KDuration = 2500.milliseconds(), isOneOffRequest: Boolean = true, isExpectResponseBody: Boolean = false, renderResponseTimeout: KDuration = 1500.milliseconds(), environment: TestEnvironment?) {
    createRequest(request = request, environment = environment)

    waitForIdle()
//    mainClock.advanceTimeBy(500L)
    delayShort()
    waitForIdle()

    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
    waitForIdle()

    // wait for response
    waitUntil(5000L) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodesWithRetry(this).isNotEmpty() }
    if (isOneOffRequest) {
        val startTime = KInstant.now()
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodesWithRetry(this).isEmpty() }
        println("Call Duration: ${KInstant.now() - startTime}")
        println("Response status: ${onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodesWithRetry(this).joinToString("\\\\") { it.getTexts().joinToString("|") }}")
        println("Response error: ${onAllNodesWithTag(TestTag.ResponseError.name).fetchSemanticsNodesWithRetry(this).joinToString("\\\\") { it.getTexts().joinToString("|") }}")
    }

    if (isExpectResponseBody) {
        waitUntil(renderResponseTimeout.millis) {
            onAllNodesWithTag(TestTag.ResponseBody.name).fetchSemanticsNodesWithRetry(this).isNotEmpty()
        }
    }
}

suspend fun DesktopComposeUiTest.createAndSendRestEchoRequestAndAssertResponse(request: UserRequestTemplate, timeout: KDuration = 2500.milliseconds(), environment: TestEnvironment?, ignoreAssertQueryParameters: Set<String> = emptySet()): RequestData {
    val baseExample = request.examples.first()
    val isAssertBodyContent = request.url.endsWith("/rest/echo")
    createAndSendHttpRequest(request = request, timeout = timeout, environment = environment, isExpectResponseBody = true)

    onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNodeWithRetry(this)
        .getTexts()
        .single()
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
        baseExample.queryParameters.map { Parameter(it.key, it.value) }
            .filter { it.name !in ignoreAssertQueryParameters }
            .sortedBy { it.name },
        resp.queryParameters
            .filter { it.name !in ignoreAssertQueryParameters }
            .sortedBy { it.name }
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
        null, is FormUrlEncodedBody, is MultipartBody -> assertTrue(resp.body.isNullOrEmpty())
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
    return resp
}

suspend fun DesktopComposeUiTest.sendPayload(payload: String, isCreatePayloadExample: Boolean = true) {
    fun getStreamPayloadLatestTimeString(): String {
        waitForIdle()
        return (onAllNodesWithTag(TestTag.ResponseStreamLogItemTime.name, useUnmergedTree = true)
            .fetchSemanticsNodesWithRetry(this)//.also { println("getStreamPayloadLatestTimeString() size ${it.size}") }
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
        .performTextInput(this, payload)

    delayShort()

    val streamCountBeforeSend = getStreamPayloadLatestTimeString()

    onNodeWithTag(TestTag.RequestSendPayloadButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)

    waitUntil(600.milliseconds().millis) { getStreamPayloadLatestTimeString() != streamCountBeforeSend }
}

suspend fun DesktopComposeUiTest.fireRequest(timeout: KDuration = 1.seconds(), isClientStreaming: Boolean = false, isServerStreaming: Boolean = false) {
    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .assertTextEquals(if (isClientStreaming) "Connect" else "Send")
        .performClickWithRetry(this)

    delayShort()

    // wait for response
    waitUntil(5000L) { onAllNodesWithTag(TestTag.ResponseStatus.name).fetchSemanticsNodesWithRetry(this).isNotEmpty() }
    if (!isClientStreaming && !isServerStreaming) {
        waitUntil(maxOf(1L, timeout.millis)) { onAllNodesWithText("Communicating").fetchSemanticsNodesWithRetry(this).isEmpty() }
    } else {
        waitUntil(1.seconds().millis) { onAllNodesWithText("Communicating").fetchSemanticsNodesWithRetry(this).isNotEmpty() }
    }
}

suspend fun DesktopComposeUiTest.completeRequest() {
    onNodeWithTag(TestTag.RequestCompleteStreamButton.name)
        .assertIsDisplayedWithRetry(this)
        .performClickWithRetry(this)
}

fun DesktopComposeUiTest.disconnect() {
    onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
        .assertIsDisplayedWithRetry(this)
        .assertTextEquals("Disconnect")
        .performClickWithRetry(this)

    waitUntil(2.seconds().millis) {
        onNodeWithTag(TestTag.RequestFireOrDisconnectButton.name)
            .assertIsDisplayedWithRetry(this)
            .fetchSemanticsNodeWithRetry(this)
            .getTexts() == listOf("Connect")
    }
}

suspend fun DesktopComposeUiTest.delayShort() {
    wait(250L)
}

suspend fun DesktopComposeUiTest.wait(duration: KDuration) {
    wait(duration.toMilliseconds())
}

suspend fun DesktopComposeUiTest.wait(ms: Long) {
    mainClock.advanceTimeBy(ms)
    delay(ms)
    waitForIdle()
}

fun DesktopComposeUiTest.getResponseBody(): String? {
    val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNodeWithRetry(this)
        .getTexts()
        .joinToString("")
        .ifEmpty { null }
    return responseBody
}

fun DesktopComposeUiTest.retryForUnresponsiveBuggyComposeTest(testContent: () -> Unit) {
    var retryAttempt = 0
    while (true) { // add this loop because the click is often not performed
        waitForIdle()

        try {
            testContent()
        } catch (e: ComposeTimeoutException) {
            e.printStackTrace()
            println("Retry the buggy Compose Test click until passing. #attempt: ${++retryAttempt}")
            continue
        }
        break
    }
}


/**
 * retry to prevent illegal state after sleeping
 */
fun SemanticsNodeInteraction.performClickWithRetry(host: ComposeUiTest): SemanticsNodeInteraction {
    while (true) {
        try {
            host.runOnUiThread {
                performClick()
            }
            return this
        } catch (e: IllegalArgumentException) {
            host.waitForIdle()
        }
    }
}

fun SemanticsNodeInteraction.assertIsDisplayedWithRetry(host: ComposeUiTest): SemanticsNodeInteraction {
    while (true) {
        try {
            host.runOnUiThread {
                assertIsDisplayed()
            }
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

fun SemanticsNodeInteraction.fetchSemanticsNodeWithRetry(host: ComposeUiTest): SemanticsNode {
    while (true) {
        try {
            return host.runOnUiThread {
                fetchSemanticsNode()
            }
        } catch (e: IllegalArgumentException) {
            host.waitForIdle()
        }
    }
}

fun SemanticsNodeInteractionCollection.fetchSemanticsNodesWithRetry(host: ComposeUiTest): List<SemanticsNode> {
    while (true) {
        try {
            return host.runOnUiThread {
                fetchSemanticsNodes()
            }
        } catch (e: IllegalArgumentException) {
            host.waitForIdle()
        }
    }
}

/**
 * To work around the bug: https://issuetracker.google.com/issues/319395743
 */
fun SemanticsNodeInteraction.performTextInput(host: ComposeUiTest, s: String) {
    host.runOnUiThread {
        performTextInput(s)
    }
}

fun captureScreenToFile(filename: String) {
    val image = Robot().createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
    val parent = File("test-error-screenshot")
        .also { it.mkdirs() }
    File(parent, "$filename.png").writeBytes(image.toImage().encodeToData(EncodedImageFormat.PNG)!!.bytes)
}

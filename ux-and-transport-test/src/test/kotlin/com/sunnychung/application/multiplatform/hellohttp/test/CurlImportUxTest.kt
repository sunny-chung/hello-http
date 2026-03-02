@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.ux.RequestTreeRowMethodKey
import com.sunnychung.application.multiplatform.hellohttp.ux.RequestTreeRowNameKey
import com.sunnychung.application.multiplatform.hellohttp.ux.RequestTreeRowRequestIdKey
import com.sunnychung.application.multiplatform.hellohttp.ux.RequestTreeRowUrlKey
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class CurlImportUxTest {

    companion object {
        private const val IMPORT_DIALOG_OPEN_TIMEOUT_MILLIS = 10_000L

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }
    }

    @JvmField
    @Rule
    val retryRule = RetryRule()

    private data class ImportedRequestExpectation(
        val method: String,
        val pathSegment: String,
        val request: UserRequestTemplate,
    )

    @Test
    fun createButtonMenuShouldContainCurlImportEntry() = runTest {
        createProjectIfNeeded()

        onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        waitUntilExactlyOneExists(
            this,
            hasTextExactly("Import cURL command (Linux / macOS)", includeEditableText = false),
            2_000L,
        )
        onNodeWithText("Import cURL command (Linux / macOS)")
            .assertIsDisplayedWithRetry(this)
    }

    @Test
    fun importCurlJsonBodyShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --request POST --url 'https://example.com/import/path' --header 'X-Test: abc' --data '{\"name\":\"imported\"}'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/import/path",
            contentType = ContentType.Json,
            body = StringBody("{\"name\":\"imported\"}"),
            headers = listOf(kvString("X-Test", "abc")),
        )
        importCurlAndAssert(command = command, expectedRequest = expected)
    }

    @Test
    fun importCurlUrlEncodedBodyShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --request POST --url 'https://example.com/form' --data-urlencode 'a=1' --data-urlencode 'b=two%20words'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/form",
            contentType = ContentType.FormUrlEncoded,
            body = FormUrlEncodedBody(
                value = listOf(
                    kvString("a", "1"),
                    kvString("b", "two words"),
                )
            ),
        )
        importCurlAndAssert(command = command, expectedRequest = expected)
    }

    @Test
    fun importCurlMultipartWithoutFileShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --request POST --url 'https://example.com/multipart' --form 'a=1' --form 'b=two'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/multipart",
            contentType = ContentType.Multipart,
            body = MultipartBody(
                value = listOf(
                    kvString("a", "1"),
                    kvString("b", "two"),
                )
            ),
        )
        importCurlAndAssert(command = command, expectedRequest = expected)
    }

    @Test
    fun importCurlMultipartWithFileShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --request POST --url 'https://example.com/upload' --form 'upload=@/tmp/import.bin' --form 'note=ok'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/upload",
            contentType = ContentType.Multipart,
            body = MultipartBody(
                value = listOf(
                    kvFile("upload", "/tmp/import.bin"),
                    kvString("note", "ok"),
                )
            ),
        )
        importCurlAndAssert(command = command, expectedRequest = expected)
    }

    @Test
    fun importCurlWithoutBodyShouldCreateAndPopulateRequest() = runTest {
        val command = "curl https://example.com/empty"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/empty",
            contentType = ContentType.None,
            body = null,
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = emptyList(),
                )
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = emptyList(),
                )
            }
        )
    }

    @Test
    fun importCurlShortFormJsonBodyWithHeaderAndCookieShouldCreateAndPopulateRequest() = runTest {
        val command = "curl -X POST 'https://example.com/short/json?source=short&trace=t%2B1' -H 'X-Trace: t-1' -H 'Content-Type: application/json' -b 'sid=s%3D1' -d '{\"name\":\"short\"}'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/short/json?source=short&trace=t%2B1",
            contentType = ContentType.Json,
            body = StringBody("{\"name\":\"short\"}"),
            headers = listOf(
                kvString("X-Trace", "t-1"),
                kvString("Content-Type", "application/json"),
            ),
            queryParameters = listOf(
                kvString("source", "short"),
                kvString("trace", "t+1"),
            ),
            cookies = listOf(
                kvString("sid", "s%3D1"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            beforeImport = { enableCookieForCurrentSubproject() },
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = listOf(
                        "sid" to "s%3D1",
                    ),
                )
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "source" to "short",
                        "trace" to "t+1",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlShortFormFormUrlEncodedBodyWithHeaderAndCookieShouldCreateAndPopulateRequest() = runTest {
        val command = "curl -X POST 'https://example.com/short/form?source=short-form&x=1' -H 'X-Trace: t-2' -H 'Content-Type: application/x-www-form-urlencoded' -b 'sid=abc123; theme=light' -d 'a=1&b=two%2Bthree'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/short/form?source=short-form&x=1",
            contentType = ContentType.FormUrlEncoded,
            body = FormUrlEncodedBody(
                value = listOf(
                    kvString("a", "1"),
                    kvString("b", "two%2Bthree"),
                ),
            ),
            headers = listOf(
                kvString("X-Trace", "t-2"),
                kvString("Content-Type", "application/x-www-form-urlencoded"),
            ),
            queryParameters = listOf(
                kvString("source", "short-form"),
                kvString("x", "1"),
            ),
            cookies = listOf(
                kvString("sid", "abc123"),
                kvString("theme", "light"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            beforeImport = { enableCookieForCurrentSubproject() },
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = listOf(
                        "sid" to "abc123",
                        "theme" to "light",
                    ),
                )
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "source" to "short-form",
                        "x" to "1",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlShortFormMultipartBodyWithHeaderAndCookieShouldCreateAndPopulateRequest() = runTest {
        val command = "curl -X POST 'https://example.com/short/multipart?source=short-multipart&escaped=a%2Bb' -H 'X-Trace: t-3' -b 'sid=xyz' -F 'note=ok' -F 'upload=@/tmp/short-form.bin'"
        val expected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/short/multipart?source=short-multipart&escaped=a%2Bb",
            contentType = ContentType.Multipart,
            body = MultipartBody(
                value = listOf(
                    kvString("note", "ok"),
                    kvFile("upload", "/tmp/short-form.bin"),
                ),
            ),
            headers = listOf(
                kvString("X-Trace", "t-3"),
            ),
            queryParameters = listOf(
                kvString("source", "short-multipart"),
                kvString("escaped", "a+b"),
            ),
            cookies = listOf(
                kvString("sid", "xyz"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            beforeImport = { enableCookieForCurrentSubproject() },
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = listOf(
                        "sid" to "xyz",
                    ),
                )
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "source" to "short-multipart",
                        "escaped" to "a+b",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlQueryParametersInUrlShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --request GET --url 'https://www.postb.in/1772351307095-9288514354266?hello=world&escaped=a%2Bb%26c%3Dd'"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://www.postb.in/1772351307095-9288514354266?hello=world&escaped=a%2Bb%26c%3Dd",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("hello", "world"),
                kvString("escaped", "a+b&c=d"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "hello" to "world",
                        "escaped" to "a+b&c=d",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlSingleQueryParameterShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --get --url 'https://example.com/query/single' --data-urlencode 'q=a%2Bb%26c%3Dd'"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/query/single",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("q", "a+b&c=d"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "q" to "a+b&c=d",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlMultipleQueryParametersShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --get --url 'https://example.com/query/multiple' --data-urlencode 'a=1' --data-urlencode 'b=two'"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/query/multiple",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("a", "1"),
                kvString("b", "two"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Query",
                    testTagPart = TestTagPart.RequestQueryParameter,
                    expectedKeyValuePairs = listOf(
                        "a" to "1",
                        "b" to "two",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlSingleCookieShouldCreateAndPopulateRequest() = runTest {
        val command = "curl --url 'https://example.com/cookies/single' --cookie 'session=abc'"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/cookies/single",
            contentType = ContentType.None,
            body = null,
            cookies = listOf(
                kvString("session", "abc"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            beforeImport = { enableCookieForCurrentSubproject() },
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = listOf(
                        "session" to "abc",
                    ),
                )
            }
        )
    }

    @Test
    fun importCurlMultipleCookiesShouldCreateAndPopulateRequest() = runTest {
        val command = "curl 'https://example.com/cookies/multiple' --cookie 'session=abc; theme=light'"
        val expected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/cookies/multiple",
            contentType = ContentType.None,
            body = null,
            cookies = listOf(
                kvString("session", "abc"),
                kvString("theme", "light"),
            ),
        )
        importCurlAndAssert(
            command = command,
            expectedRequest = expected,
            beforeImport = { enableCookieForCurrentSubproject() },
            afterAssert = {
                assertRequestParameterTabContent(
                    tabLabel = "Cookie",
                    testTagPart = TestTagPart.Cookie,
                    expectedKeyValuePairs = listOf(
                        "session" to "abc",
                        "theme" to "light",
                    ),
                )
            }
        )
    }

    @Test
    fun importMultipleCurlCommandsShouldCreateMultipleRequestsAndSelectFirstImportedRequest() = runTest {
        val runId = (System.nanoTime() % 1000000L).toString()
        val firstPath = "mf$runId"
        val secondPath = "ms$runId"
        val command = """
            curl --request GET --url 'https://example.com/$firstPath?source=multi'
            curl --request POST --url 'https://example.com/$secondPath' --header 'Content-Type: application/json' --data '{"name":"second"}'
        """.trimIndent()
        val firstExpected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/$firstPath?source=multi",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("source", "multi"),
            ),
        )
        val secondExpected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/$secondPath",
            contentType = ContentType.Json,
            body = StringBody("""{"name":"second"}"""),
            headers = listOf(
                kvString("Content-Type", "application/json"),
            ),
        )

        createProjectIfNeeded()
        val initialRequestRowCount = getRequestTreeRequestRowCount()
        importCurlCommand(
            command = command,
            dialogCloseTimeoutMillis = 12_000L,
        )
        assertRequestTreeRequestRowCount(
            initialRequestRowCount + 2,
        )
        assertImportedRequests(
            listOf(
                ImportedRequestExpectation(method = "GET", pathSegment = firstPath, request = firstExpected),
                ImportedRequestExpectation(method = "POST", pathSegment = secondPath, request = secondExpected),
            )
        )
    }

    @Test
    fun importMultipleCurlCommandsWithBlankLinesShouldCreateAllRequests() = runTest {
        val runId = (System.nanoTime() % 1000000L).toString()
        val firstPath = "mba$runId"
        val secondPath = "mbb$runId"
        val thirdPath = "mbc$runId"
        val command = """
            time curl --request GET --url 'https://example.com/$firstPath'

            curl --request GET --url 'https://example.com/$secondPath?x=1'
            curl --request POST --url 'https://example.com/$thirdPath' --data-urlencode 'a=1'
        """.trimIndent()
        val firstExpected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/$firstPath",
            contentType = ContentType.None,
            body = null,
        )
        val secondExpected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/$secondPath?x=1",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("x", "1"),
            ),
        )
        val thirdExpected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/$thirdPath",
            contentType = ContentType.FormUrlEncoded,
            body = FormUrlEncodedBody(
                value = listOf(
                    kvString("a", "1"),
                ),
            ),
        )

        createProjectIfNeeded()
        val initialRequestRowCount = getRequestTreeRequestRowCount()
        importCurlCommand(
            command = command,
            dialogCloseTimeoutMillis = 12_000L,
        )
        assertRequestTreeRequestRowCount(
            initialRequestRowCount + 3,
        )
        assertImportedRequests(
            listOf(
                ImportedRequestExpectation(method = "GET", pathSegment = firstPath, request = firstExpected),
                ImportedRequestExpectation(method = "GET", pathSegment = secondPath, request = secondExpected),
                ImportedRequestExpectation(method = "POST", pathSegment = thirdPath, request = thirdExpected),
            )
        )
    }

    @Test
    fun importMultipleCurlCommandsWithCommentsShouldIgnoreCommentsAndCreateRequests() = runTest {
        val runId = (System.nanoTime() % 1000000L).toString()
        val firstPath = "ca$runId"
        val secondPath = "cb$runId"
        val command = """
            # Query with URL parameters
            curl --request GET --url 'https://example.com/$firstPath?source=comment' # this should be ignored

            # JSON submit
            curl --request POST --url 'https://example.com/$secondPath' --header 'Content-Type: application/json' --data '{"id":1}' # trailing comment
        """.trimIndent()
        val firstExpected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/$firstPath?source=comment",
            contentType = ContentType.None,
            body = null,
            queryParameters = listOf(
                kvString("source", "comment"),
            ),
        )
        val secondExpected = expectedHttpRequest(
            method = "POST",
            url = "https://example.com/$secondPath",
            contentType = ContentType.Json,
            body = StringBody("""{"id":1}"""),
            headers = listOf(
                kvString("Content-Type", "application/json"),
            ),
        )

        createProjectIfNeeded()
        val initialRequestRowCount = getRequestTreeRequestRowCount()
        importCurlCommand(
            command = command,
            dialogCloseTimeoutMillis = 12_000L,
        )
        assertRequestTreeRequestRowCount(
            initialRequestRowCount + 2,
        )
        assertImportedRequests(
            listOf(
                ImportedRequestExpectation(method = "GET", pathSegment = firstPath, request = firstExpected),
                ImportedRequestExpectation(method = "POST", pathSegment = secondPath, request = secondExpected),
            )
        )
    }

    @Test
    fun importTwentyThenImportThreeShouldScrollToNewlySelectedRequestAndVerifyAllCreatedRequests() = runTest {
        val runId = (System.nanoTime() % 1000000L).toString()
        val batch1Paths = (1..20).map { index -> "b1${runId}r${index.toString().padStart(2, '0')}" }
        val batch2Paths = (1..3).map { index -> "b2${runId}r${index.toString().padStart(2, '0')}" }
        val batch1Command = batch1Paths.joinToString("\n") { path ->
            "curl --request GET --url 'https://example.com/$path'"
        }
        val batch2Command = batch2Paths.joinToString("\n") { path ->
            "curl --request GET --url 'https://example.com/$path'"
        }
        val batch2FirstExpected = expectedHttpRequest(
            method = "GET",
            url = "https://example.com/${batch2Paths.first()}",
            contentType = ContentType.None,
            body = null,
        )
        val batch1Expectations = batch1Paths.map { path ->
            ImportedRequestExpectation(
                method = "GET",
                pathSegment = path,
                request = expectedHttpRequest(
                    method = "GET",
                    url = "https://example.com/$path",
                    contentType = ContentType.None,
                    body = null,
                ),
            )
        }
        val batch2Expectations = batch2Paths.map { path ->
            ImportedRequestExpectation(
                method = "GET",
                pathSegment = path,
                request = expectedHttpRequest(
                    method = "GET",
                    url = "https://example.com/$path",
                    contentType = ContentType.None,
                    body = null,
                ),
            )
        }

        createProjectIfNeeded()
        val initialRequestRowCount = getRequestTreeRequestRowCount()

        importCurlCommand(
            command = batch1Command,
            dialogCloseTimeoutMillis = 16_000L,
        )
        assertRequestTreeRequestRowCount(initialRequestRowCount + 20)
        batch1Expectations.forEach { expected ->
            assertRequestExistsInTreeByMethodAndPath(
                method = expected.method,
                pathSegment = expected.pathSegment,
            )
        }
        assertCurrentRequestEditor(batch1Expectations.first().request)

        // move selection near the top to ensure the second import must auto-scroll to selected request
        assertCurrentRequestEditor(batch1Expectations.first().request)

        importCurlCommand(
            command = batch2Command,
            dialogCloseTimeoutMillis = 16_000L,
        )
        assertRequestTreeRequestRowCount(initialRequestRowCount + 23)
        batch1Expectations.forEach { expected ->
            assertRequestExistsInTreeByMethodAndPath(
                method = expected.method,
                pathSegment = expected.pathSegment,
            )
        }

        assertCurrentRequestEditor(batch2FirstExpected)
        assertRequestVisibleByMethodAndPath(method = "GET", pathSegment = batch2Paths.first())

        assertImportedRequests(batch2Expectations)
    }

    private suspend fun DesktopComposeUiTest.importCurlAndAssert(
        command: String,
        expectedRequest: UserRequestTemplate,
        beforeImport: suspend DesktopComposeUiTest.() -> Unit = {},
        afterAssert: suspend DesktopComposeUiTest.() -> Unit = {},
    ) {
        createProjectIfNeeded()
        beforeImport(this)
        importCurlCommand(command)
        assertCurrentRequestEditor(expectedRequest)
        afterAssert(this)
    }

    private suspend fun DesktopComposeUiTest.importCurlCommand(
        command: String,
        dialogCloseTimeoutMillis: Long = 8_000L,
    ) {
        onNodeWithTag(TestTag.CreateRequestOrFolderButton.name)
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)
        waitUntilExactlyOneExists(
            this,
            hasTextExactly("Import cURL command (Linux / macOS)", includeEditableText = false),
            2_000L,
        )
        onNodeWithText("Import cURL command (Linux / macOS)")
            .assertIsDisplayedWithRetry(this)
            .performClickWithRetry(this)

        waitUntilExactlyOneExists(
            this,
            testTag = TestTag.ImportCurlCommandDialogCommandTextField.name,
            timeoutMillis = IMPORT_DIALOG_OPEN_TIMEOUT_MILLIS,
            useUnmergedTree = true,
        )
        onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name, useUnmergedTree = true)
            .assertIsDisplayedWithRetry(this)

        wait(220L)
        onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name, useUnmergedTree = true)
            .assertIsDisplayedWithRetry(this)
            .performTextInput(this, command)
        wait(220L)
        waitUntil(3_000L) {
            onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name, useUnmergedTree = true)
                .fetchSemanticsNodeWithRetry(this)
                .getTexts() == listOf(command)
        }

        retryForUnresponsiveBuggyComposeTest(maxRetryCount = 8, retryDelayMillis = 220L) {
            onNodeWithTag(TestTag.ImportCurlCommandDialogImportButton.name, useUnmergedTree = true)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)

            waitUntil(dialogCloseTimeoutMillis) {
                onAllNodesWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
                    .fetchSemanticsNodesWithRetry(this)
                    .isEmpty()
            }
        }
    }

    private fun DesktopComposeUiTest.getRequestTreeRequestRowCount(): Int {
        return onAllNodes(hasRequestTreeRequestRowTag(), useUnmergedTree = true)
            .fetchSemanticsNodesWithRetry(this)
            .size
    }

    private suspend fun DesktopComposeUiTest.assertRequestTreeRequestRowCount(expectedCount: Int) {
        waitUntil(6_000L) {
            getRequestTreeRequestRowCount() == expectedCount
        }
    }

    private suspend fun DesktopComposeUiTest.assertImportedRequests(
        expectedRequests: List<ImportedRequestExpectation>,
    ) {
        check(expectedRequests.isNotEmpty()) { "expectedRequests should not be empty" }
        expectedRequests.forEach { expected ->
            assertRequestExistsInTreeByMethodAndPath(
                method = expected.method,
                pathSegment = expected.pathSegment,
            )
        }

        assertCurrentRequestEditor(expectedRequests.first().request)
        assertRequestVisibleByMethodAndPath(
            method = expectedRequests.first().method,
            pathSegment = expectedRequests.first().pathSegment,
        )

        expectedRequests.drop(1).forEach { expected ->
            selectRequestByMethodAndPath(
                method = expected.method,
                pathSegment = expected.pathSegment,
            )
            assertCurrentRequestEditor(expected.request)
            assertRequestVisibleByMethodAndPath(
                method = expected.method,
                pathSegment = expected.pathSegment,
            )
        }
    }

    private suspend fun DesktopComposeUiTest.assertRequestVisibleByMethodAndPath(
        method: String,
        pathSegment: String,
    ) {
        waitUntil(10_000L) {
            try {
                val rowTestTag = findRequestRowTestTag(method = method, pathSegment = pathSegment)
                if (rowTestTag == null) {
                    return@waitUntil false
                }
                onNodeWithTag(rowTestTag, useUnmergedTree = true)
                    .assertIsDisplayedWithRetry(this)
                true
            } catch (_: Throwable) {
                false
            }
        }
        val rowTestTag = findRequestRowTestTag(method = method, pathSegment = pathSegment)
        check(rowTestTag != null) {
            "Cannot find request row for $method $pathSegment. rows=${getRequestRowDebugSummary()}"
        }
        onNodeWithTag(rowTestTag, useUnmergedTree = true)
            .assertIsDisplayedWithRetry(this)
    }

    private suspend fun DesktopComposeUiTest.assertRequestExistsInTreeByMethodAndPath(
        method: String,
        pathSegment: String,
    ) {
        try {
            waitUntil(10_000L) {
                findRequestRowTestTag(method = method, pathSegment = pathSegment) != null
            }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError(
                "Cannot find request row for $method $pathSegment. rows=${getRequestRowDebugSummary()}",
                e,
            )
        }
    }

    private suspend fun DesktopComposeUiTest.selectRequestByMethodAndPath(
        method: String,
        pathSegment: String,
    ) {
        waitUntil(10_000L) {
            findRequestRowTestTag(method = method, pathSegment = pathSegment) != null
        }
        val rowTestTag = findRequestRowTestTag(method = method, pathSegment = pathSegment)
        check(rowTestTag != null) {
            "Cannot find request row for $method $pathSegment. rows=${getRequestRowDebugSummary()}"
        }
        retryForUnresponsiveBuggyComposeTest(maxRetryCount = 6, retryDelayMillis = 220L) {
            // Use semantics OnClick instead of pointer click.
            // Pointer click is display-dependent and was flaky for this test path; semantics click targets the node
            // action directly and is more stable across Compose Desktop test runs.
            onNodeWithTag(rowTestTag, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    private fun DesktopComposeUiTest.findRequestRowTestTag(
        method: String,
        pathSegment: String,
    ): String? {
        // Intentionally locate rows by explicit semantics fields instead of text search.
        //
        // Why:
        // - `onNodeWithText` / `getTexts()` behavior differs between merged and unmerged trees.
        // - For request-tree rows, the node we interact with can have empty text even when descendants render labels.
        // - Matching by raw substring also creates false positives (`r1` matches `r10`).
        //
        // Using row semantics keys avoids all three issues.
        val rows = onAllNodes(hasRequestTreeRequestRowTag(), useUnmergedTree = true)
            .fetchSemanticsNodesWithRetry(this)
        return rows.firstOrNull { row ->
            val rowMethod = row.config.getOrNull(RequestTreeRowMethodKey)
            val rowName = row.config.getOrNull(RequestTreeRowNameKey).orEmpty()
            val rowUrl = row.config.getOrNull(RequestTreeRowUrlKey).orEmpty()
            rowMethod == method && (
                hasPathToken(text = rowUrl, pathSegment = pathSegment) ||
                hasPathToken(text = rowName, pathSegment = pathSegment)
            )
        }?.config?.getOrNull(SemanticsProperties.TestTag)
    }

    private fun hasPathToken(text: String, pathSegment: String): Boolean {
        val escapedPathSegment = Regex.escape(pathSegment)
        return Regex("(^|[^A-Za-z0-9_-])$escapedPathSegment([^A-Za-z0-9_-]|$)").containsMatchIn(text)
    }

    private fun DesktopComposeUiTest.getRequestRowDebugSummary(): String {
        val unmerged = onAllNodes(hasRequestTreeRequestRowTag(), useUnmergedTree = true)
            .fetchSemanticsNodesWithRetry(this)
            .joinToString(" | ") { row ->
                val tag = row.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                val requestId = row.config.getOrNull(RequestTreeRowRequestIdKey).orEmpty()
                val method = row.config.getOrNull(RequestTreeRowMethodKey).orEmpty()
                val name = row.config.getOrNull(RequestTreeRowNameKey).orEmpty()
                val url = row.config.getOrNull(RequestTreeRowUrlKey).orEmpty()
                "$tag:id=$requestId,method=$method,name=$name,url=$url"
            }
        return "unmerged=[$unmerged]"
    }

    private fun hasRequestTreeRequestRowTag(): SemanticsMatcher {
        return SemanticsMatcher("has request tree row tag") { node ->
            val testTag = node.config.getOrNull(SemanticsProperties.TestTag) ?: return@SemanticsMatcher false
            val prefix = "${TestTag.RequestTreeRequestRow.name}/"
            testTag.startsWith(prefix)
        }
    }

    private fun expectedHttpRequest(
        method: String,
        url: String,
        contentType: ContentType,
        body: UserRequestBody?,
        headers: List<UserKeyValuePair> = emptyList(),
        queryParameters: List<UserKeyValuePair> = emptyList(),
        cookies: List<UserKeyValuePair> = emptyList(),
    ): UserRequestTemplate {
        return UserRequestTemplate(
            id = "expected-request",
            name = "Expected Request",
            method = method,
            url = url,
            examples = listOf(
                UserRequestExample(
                    id = "expected-example",
                    name = "Base",
                    contentType = contentType,
                    headers = headers,
                    queryParameters = queryParameters,
                    cookies = cookies,
                    body = body,
                )
            ),
        )
    }

    private fun kvString(key: String, value: String): UserKeyValuePair {
        return UserKeyValuePair(
            id = "$key-string",
            key = key,
            value = value,
            valueType = FieldValueType.String,
            isEnabled = true,
        )
    }

    private fun kvFile(key: String, value: String): UserKeyValuePair {
        return UserKeyValuePair(
            id = "$key-file",
            key = key,
            value = value,
            valueType = FieldValueType.File,
            isEnabled = true,
        )
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTagPart
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class CurlImportUxTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }
    }

    @JvmField
    @Rule
    val retryRule = RetryRule()

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

    private suspend fun DesktopComposeUiTest.importCurlCommand(command: String) {
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

        waitUntilExactlyOneExists(this, hasTestTag(TestTag.ImportCurlCommandDialogCommandTextField.name), 2_000L)
        onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
            .assertIsDisplayedWithRetry(this)
        waitUntil(2_000L) {
            onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
                .fetchSemanticsNodeWithRetry(this)
                .config
                .getOrNull(SemanticsProperties.Focused) == true
        }

        wait(220L)
        onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
            .assertIsDisplayedWithRetry(this)
            .performTextInput(this, command)
        wait(220L)
        waitUntil(1_000L) {
            onNodeWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
                .fetchSemanticsNodeWithRetry(this)
                .getTexts() == listOf(command)
        }

        retryForUnresponsiveBuggyComposeTest(maxRetryCount = 8, retryDelayMillis = 220L) {
            onNodeWithTag(TestTag.ImportCurlCommandDialogImportButton.name, useUnmergedTree = true)
                .assertIsDisplayedWithRetry(this)
                .performClickWithRetry(this)

            waitUntil(3_000L) {
                onAllNodesWithTag(TestTag.ImportCurlCommandDialogCommandTextField.name)
                    .fetchSemanticsNodesWithRetry(this)
                    .isEmpty()
            }
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

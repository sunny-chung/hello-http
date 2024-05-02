@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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

    @Test
    fun echoPostWithFileBody() = runTest {
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
                        contentType = ContentType.BinaryFile,
                        body = FileBody("src/test/resources/testFile1中文字.txt"),
                    )
                )
            )
        )
    }
}

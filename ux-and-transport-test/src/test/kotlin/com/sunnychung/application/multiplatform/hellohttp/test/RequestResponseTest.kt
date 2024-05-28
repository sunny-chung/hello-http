@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
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
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.random.Random

@RunWith(Parameterized::class)
class RequestResponseTest(testName: String, isHttp1Only: Boolean, isSsl: Boolean, isMTls: Boolean) {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests() {
            File("build/testrun").apply {
                if (exists()) {
                    deleteRecursively()
                }
            }

            val appDir = File("build/testrun/data")
            AppContext.dataDir = appDir
            AppContext.SingleInstanceProcessService.apply { dataDir = appDir }.enforce()
            runBlocking {
                AppContext.PersistenceManager.initialize()
            }

            bigDataFile = File("build/testrun/resources/bigfile.dat").apply {
                if (exists()) return@apply
                parentFile.mkdirs()
                val bytesBlock = Random.nextBytes(1 * 1024 * 1024) // 1 MB
                repeat(70) { // 70 MB
                    appendBytes(bytesBlock)
                }
                appendBytes(byteArrayOf(0, -5, 3, 124, 59))
            }
        }

        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any>> = listOf(
            arrayOf("HTTP/2", false, false, false),
            arrayOf("HTTP/1 only", true, false, false),
            arrayOf("HTTP/1 SSL", true, true, false),
            arrayOf("SSL", false, true, false),
            arrayOf("mTLS", false, true, true),
        )

        fun hostAndPort(isHttp1Only: Boolean, isSsl: Boolean, isMTls: Boolean) = when {
            isSsl && isMTls -> "localhost:18086"
            !isHttp1Only && isSsl && !isMTls -> "localhost:18084"
            isHttp1Only && isSsl && !isMTls -> "localhost:18088"
            !isHttp1Only && !isSsl && !isMTls -> "localhost:18081"
            isHttp1Only && !isSsl && !isMTls -> "localhost:18083"
            else -> throw UnsupportedOperationException()
        }

        fun environment(isSsl: Boolean, isMTls: Boolean): TestEnvironment = when {
            !isSsl -> TestEnvironment.Local
            isSsl && isMTls -> TestEnvironment.LocalMTls
            isSsl && !isMTls -> TestEnvironment.LocalSsl
            else -> throw UnsupportedOperationException()
        }

        private var lastExecutedEnvironment: TestEnvironment? = null
    }

    val hostAndPort = hostAndPort(isHttp1Only = isHttp1Only, isSsl = isSsl, isMTls = isMTls)
    val httpUrlPrefix = "http${if (isSsl) "s" else ""}://$hostAndPort"
    val echoUrl = "$httpUrlPrefix/rest/echo"
    val echoWithoutBodyUrl = "$httpUrlPrefix/rest/echoWithoutBody"
    val earlyErrorUrl = "$httpUrlPrefix/rest/earlyError"
    val errorUrl = "$httpUrlPrefix/rest/error"
    val environment = environment(isSsl = isSsl, isMTls = isMTls)

    @JvmField
    @Rule
    val retryRule = RetryRule()

    @Before
    fun beforeEach() {
        if (environment != lastExecutedEnvironment) {
            println("beforeEach re-init")
            lastExecutedEnvironment = environment
        } else {
            println("No init is needed")
        }
    }

    @Test
    fun echoGet() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
        )
    }

    @Test
    fun echoPostWithoutBody() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoUrl,
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
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
            ),
            environment = environment,
        )
    }

    @Test
    fun echoPostWithBigFileBody() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoWithoutBodyUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.BinaryFile,
                        body = FileBody(bigDataFile.absolutePath),
                    )
                )
            ),
            timeout = 25.seconds(),
            environment = environment,
        )
    }

    @Test
    fun echoPostWithBigMultipartFiles() = runTest {
        createAndSendRestEchoRequestAndAssertResponse(
            request = UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = echoWithoutBodyUrl,
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
                            UserKeyValuePair(
                                id = uuidString(),
                                key = "big_file",
                                value = bigDataFile.absolutePath,
                                valueType = FieldValueType.File,
                                isEnabled = true,
                            ),
                        )),
                    )
                )
            ),
            timeout = 25.seconds(),
            environment = environment,
        )
    }

    /************** Special Cases **************/

    @Test
    fun earlyErrorWithBigFile() = runTest {
        val request = UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            url = earlyErrorUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.BinaryFile,
                    body = FileBody(bigDataFile.absolutePath),
                )
            )
        )
        createAndSendHttpRequest(request = request, timeout = 600.milliseconds(), environment = environment)

        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("429 Too Many Requests")
        onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
            .getTexts()
            .isEmpty()
    }

    @Test
    fun error() = runTest {
        val request = UserRequestTemplate(
            id = uuidString(),
            method = "GET",
            url = errorUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    queryParameters = listOf(
                        UserKeyValuePair("abc", "中文字"),
                        UserKeyValuePair("MyQueryParam", "abc def_gh+i=?j/k"),
                        UserKeyValuePair("emoji", "A\uD83D\uDE0Eb"),
                        UserKeyValuePair("code", "409"),
                    ),
                )
            )
        )
        createAndSendHttpRequest(request = request, environment = environment)

        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("409 Conflict")
        val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNode()
            .getTexts()
            .single()
        assertEquals("""
            {
              "error": "Some message"
            }
        """.trimIndent(), responseBody.trim())
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.loadNativeLibraries
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.network.ReactorNettyHttpTransportClient
import com.sunnychung.application.multiplatform.hellohttp.test.payload.RequestData
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.TestTag
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.random.Random

@RunWith(Parameterized::class)
class RequestResponseTest(testName: String, httpVersion: HttpConfig.HttpProtocolVersion?, isSsl: Boolean, isMTls: Boolean) {

    companion object {
        lateinit var bigDataFile: File

        @BeforeClass
        @JvmStatic
        fun initTests(): Unit = try {
            // cleanup previous test files
            AppContext.SingleInstanceProcessService.tryUnlock() // needed for Windows
            File("build/testrun").apply {
                if (exists()) {
                    deleteRecursively()
                }
            }

            val appDir = File("build/testrun/data")
            AppContext.dataDir = appDir
            AppContext.SingleInstanceProcessService.apply { dataDir = appDir }.enforce()
            loadNativeLibraries()
            runBlocking {
                AppContext.PersistenceManager.initialize()
                AppContext.ResourceManager.loadAllResources()
            }

            bigDataFile = File("build/testrun/resources/bigfile.dat").apply {
                if (exists()) return@apply
                parentFile.mkdirs()
                val bytesBlock = Random.nextBytes(75 * 1024 * 1024) // 75 MB
                repeat(2) { // 150 MB
                    appendBytes(bytesBlock)
                }
                appendBytes(byteArrayOf(0, -5, 3, 124, 59))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }

        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any?>> = listOf(
            arrayOf("Default", null, false, false),
            arrayOf("H2C", HttpConfig.HttpProtocolVersion.Http2Only, false, false),
            arrayOf("HTTP/1 only", HttpConfig.HttpProtocolVersion.Http1Only, false, false),
            arrayOf("HTTP/1 SSL", HttpConfig.HttpProtocolVersion.Http1Only, true, false),
            arrayOf("Default SSL", null, true, false),
            arrayOf("HTTP/2 SSL", HttpConfig.HttpProtocolVersion.Http2Only, true, false),
            arrayOf("Default mTLS", null, true, true),
        )

        fun hostAndPort(httpVersion: HttpConfig.HttpProtocolVersion?, isSsl: Boolean, isMTls: Boolean) = when {
            isSsl && isMTls -> "localhost:18086"
            httpVersion != HttpConfig.HttpProtocolVersion.Http1Only && isSsl && !isMTls -> "localhost:18084"
            httpVersion == HttpConfig.HttpProtocolVersion.Http1Only && isSsl && !isMTls -> "localhost:18088"
            httpVersion != HttpConfig.HttpProtocolVersion.Http1Only && !isSsl && !isMTls -> "localhost:18081"
            httpVersion == HttpConfig.HttpProtocolVersion.Http1Only && !isSsl && !isMTls -> "localhost:18083"
            else -> throw UnsupportedOperationException()
        }

        fun environment(isSsl: Boolean, isMTls: Boolean): TestEnvironment = when {
            !isSsl -> TestEnvironment.LocalDefault
            isSsl && isMTls -> TestEnvironment.LocalMTls
            isSsl && !isMTls -> TestEnvironment.LocalSsl
            else -> throw UnsupportedOperationException()
        }

        fun environment(httpVersion: HttpConfig.HttpProtocolVersion?, isSsl: Boolean, isMTls: Boolean): TestEnvironment = when {
            httpVersion == null && !isSsl -> TestEnvironment.LocalDefault
            httpVersion == null && isSsl && !isMTls -> TestEnvironment.LocalSsl
            httpVersion == null && isSsl && isMTls -> TestEnvironment.LocalMTls
            httpVersion == HttpConfig.HttpProtocolVersion.Http1Only && !isSsl -> TestEnvironment.LocalHttp1Only
            httpVersion == HttpConfig.HttpProtocolVersion.Http2Only && !isSsl -> TestEnvironment.LocalHttp2Only
            httpVersion == HttpConfig.HttpProtocolVersion.Http1Only && isSsl -> TestEnvironment.LocalHttp1Ssl
            httpVersion == HttpConfig.HttpProtocolVersion.Http2Only && isSsl -> TestEnvironment.LocalHttp2Ssl
            else -> throw UnsupportedOperationException()
        }

        private var lastExecutedEnvironment: TestEnvironment? = null
    }

    val hostAndPort = hostAndPort(httpVersion = httpVersion, isSsl = isSsl, isMTls = isMTls)
    val httpUrlPrefix = "http${if (isSsl) "s" else ""}://$hostAndPort"
    val echoUrl = "$httpUrlPrefix/rest/echo"
    val echoWithoutBodyUrl = "$httpUrlPrefix/rest/echoWithoutBody"
    val earlyErrorUrl = "$httpUrlPrefix/rest/earlyError"
    val errorUrl = "$httpUrlPrefix/rest/error"
    val bigDocumentUrl = "$httpUrlPrefix/rest/bigDocument"
    val cookieUrl = "$httpUrlPrefix/rest/cookie"
    val environment = environment(httpVersion = httpVersion, isSsl = isSsl, isMTls = isMTls)

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

    @After
    fun afterEach() {
        ReactorNettyHttpTransportClient.IS_ENABLE_WIRETAP_LOG = false
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
    fun preflightSaveJsonFieldToEnvironment() = runTest {
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
                                "s": "abcdefgh中文字_;-+",
                                "abc": 123,
                                "de": true,
                                "fgh": 45.67,
                                "obj": {
                                    "wxyz": "X"
                                }
                            }
                        """.trimIndent()),
                        preFlight = PreFlightSpec(
                            updateVariablesFromBody = listOf(
                                UserKeyValuePair("eB1", "\$.de"),
                                UserKeyValuePair("eB2", "\$.s"),
                                UserKeyValuePair("eQ1", "\$.fgh"), // reuse env var name to avoid the need of scrolling in tests
                                UserKeyValuePair("eQ2", "\$.obj.wxyz"), // reuse env var name to avoid the need of scrolling in tests
                            ),
                        )
                    )
                )
            ),
            environment = environment,
            assertEnvVariables = mapOf(
                "eB1" to "true",
                "eB2" to "abcdefgh中文字_;-+",
                "eQ1" to "45.67",
                "eQ2" to "X",
            ),
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
    fun preflightSaveHeaderAndQueryParametersAndFormParametersToEnvironment() = runTest {
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
                        preFlight = PreFlightSpec(
                            updateVariablesFromHeader = listOf(
                                UserKeyValuePair("eMyHeader", "x-My-Header"),
                            ),
                            updateVariablesFromQueryParameters = listOf(
                                UserKeyValuePair("eQ1", "MyQueryParam"),
                                UserKeyValuePair("eQ2", "emoji"),
                            ),
                            updateVariablesFromBody = listOf(
                                UserKeyValuePair("eB1", "abcc"),
                            ),
                        ),
                    )
                )
            ),
            environment = environment,
            assertEnvVariables = mapOf(
                "eMyHeader" to "defg HIjk",
                "eQ1" to "abc def_gh+i=?j/k",
                "eQ2" to "A\uD83D\uDE0Eb",
                "eB1" to "中文字123",
            ),
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
            timeout = 35.seconds(),
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
            timeout = 35.seconds(),
            environment = environment,
        )
    }

    @Test
    fun receiveAndSendCookie() = runTest {
        run {
            createProjectIfNeeded()
            enableCookieForCurrentSubproject()
        }

        run {
            val setCookieRequest = UserRequestTemplate(
                id = uuidString(),
                method = "POST",
                url = cookieUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.Json,
                        body = StringBody(
                            """
                            {
                                "cook1": "abcdeeeee",
                                "auth": "asgfa2980jngfnGsbXsMGKLFeo4acbkmloe421TtjTERSjfgnngkVMZKNo39r/ghsIOWhP=="
                            }
                        """.trimIndent()
                        ),
                    )
                )
            )

            createAndSendHttpRequest(
                request = setCookieRequest,
                timeout = 2500.milliseconds(),
                environment = environment,
                isExpectResponseBody = false,
            )

            onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("204 No Content")
//            val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNodeWithRetry(this)
//                .getTexts()
//                .single()
//            println("responseBody = $responseBody")
//            assertEquals("", responseBody)
        }

        run {
            val getCookieRequest = UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = cookieUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        contentType = ContentType.None,
                        body = null,
                    )
                )
            )

            createAndSendHttpRequest(
                request = getCookieRequest,
                timeout = 2500.milliseconds(),
                environment = environment,
                isExpectResponseBody = true
            )

            onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
            val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNodeWithRetry(this)
                .getTexts()
                .single()
            println(responseBody)

            data class NameValuePair(val name: String, val value: String)

            val resp = jacksonObjectMapper().readValue(responseBody, object : TypeReference<List<Pair<String, List<NameValuePair>>>>() {})!!
            resp.first { it.first == "cook1" }.second.let {
                assertEquals(1, it.size)
                assertEquals("abcdeeeee", it.first().value)
            }
            resp.first { it.first == "auth" }.second.let {
                assertEquals(1, it.size)
                assertEquals("asgfa2980jngfnGsbXsMGKLFeo4acbkmloe421TtjTERSjfgnngkVMZKNo39r/ghsIOWhP==", it.first().value)
            }
        }
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
        createAndSendHttpRequest(request = request, timeout = 6.seconds(), isExpectResponseBody = false, environment = environment)

        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("429 Too Many Requests")

        val durationText = onNodeWithTag(TestTag.ResponseDuration.name)
            .fetchSemanticsNode()
            .getTexts()
            .first()
        println("> durationText = $durationText")
        assertTrue(durationText.endsWith(" ms"))
        assertTrue(durationText.removeSuffix(" ms").toLong() in 0L..500L) // expected to fail fast

        onNodeWithTag(TestTag.ResponseBody.name).assertDoesNotExist()
        onNodeWithTag(TestTag.ResponseBodyEmpty.name).assertIsDisplayedWithRetry(this)
    }

    @Test
    fun earlyErrorWithBigMultipartFile() = runTest {
        val request = UserRequestTemplate(
            id = uuidString(),
            method = "POST",
            url = earlyErrorUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    contentType = ContentType.Multipart,
                    body = MultipartBody(listOf(
                        UserKeyValuePair(
                            id = uuidString(),
                            key = "file",
                            value = bigDataFile.absolutePath,
                            valueType = FieldValueType.File,
                            isEnabled = true,
                        ),
                    )),
                )
            )
        )
        createAndSendHttpRequest(request = request, timeout = 6.seconds(), isExpectResponseBody = false, environment = environment)

        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("429 Too Many Requests")

        val durationText = onNodeWithTag(TestTag.ResponseDuration.name)
            .fetchSemanticsNode()
            .getTexts()
            .first()
        println("> durationText = $durationText")
        assertTrue(durationText.endsWith(" ms"))
        assertTrue(durationText.removeSuffix(" ms").toLong() in 0L..500L) // expected to fail fast

        onNodeWithTag(TestTag.ResponseBody.name).assertDoesNotExist()
        onNodeWithTag(TestTag.ResponseBodyEmpty.name).assertIsDisplayedWithRetry(this)
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

    @Test
    fun bigDocument() = runTest {
        val size = 3 * 1024 * 1024 + 1 // around 3 MB
        // note that the app display will trim response bodies that are over 4 MB, so choose a smaller size
        
        val request = UserRequestTemplate(
            id = uuidString(),
            method = "GET",
            url = bigDocumentUrl,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    queryParameters = listOf(
                        UserKeyValuePair("size", size.toString()),
                    ),
                )
            )
        )
        // timeout includes frontend rendering time
        createAndSendHttpRequest(request = request, environment = environment, timeout = 6.seconds(), isExpectResponseBody = true, renderResponseTimeout = 6.seconds())

        onNodeWithTag(TestTag.ResponseStatus.name).assertTextEquals("200 OK")
        val responseBody = onNodeWithTag(TestTag.ResponseBody.name).fetchSemanticsNodeWithRetry(this)
            .getTexts()
            .single()
        assertEquals(size, responseBody.length)
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.test.ExperimentalTestApi
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.RequestResponseTest.Companion.hostAndPort
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class CustomScriptRequestResponseTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun initTests() {
            RequestResponseTest.initTests()
        }
    }

    val isSsl = false
    val hostAndPort = hostAndPort(httpVersion = null, isSsl = isSsl, isMTls = false)
    val httpUrlPrefix = "http${if (isSsl) "s" else ""}://$hostAndPort"
    val echoUrl = "$httpUrlPrefix/rest/echo"

    @JvmField
    @Rule
    val retryRule = RetryRule()

    @Test
    fun preflightScriptAddHeader() = runTest {
        val echoResponse = createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "de K"),
                            UserKeyValuePair("abc", "asdf"),
                            UserKeyValuePair("ghijK", "hi"),
                        ),
                        preFlight = PreFlightSpec( // TODO use "|" instead of "+"
                            executeCode = """
                                val signature = request
                                    .queryParameters
                                    .sortedBy { it.first }
                                    .joinToString("+") { it.second }
                                    .let {
                                        it.encodeToByteArray()
                                            .toSha256Hash()
                                            .encodeToBase64String()
                                    }

                                request.addHeader(
                                    key = "My-Signature",
                                    value = signature
                                )
                            """.trimIndent()
                        )
                    )
                )
            ),
            environment = TestEnvironment.LocalDefault,
        )

        assertEquals(
            "4i+aLUo5nm8GQPEkKT+brpQIGYexiqDol2kfxnJHAgk=",
            echoResponse.headers.first { it.name.equals("my-signature", ignoreCase = true) }.value
        )
    }

    @Test
    fun preflightScriptAddQueryParameter() = runTest {
        val echoResponse = createAndSendRestEchoRequestAndAssertResponse(
            UserRequestTemplate(
                id = uuidString(),
                method = "GET",
                url = echoUrl,
                examples = listOf(
                    UserRequestExample(
                        id = uuidString(),
                        name = "Base",
                        queryParameters = listOf(
                            UserKeyValuePair("abc", "de K"),
                            UserKeyValuePair("abc", "asdf"),
                            UserKeyValuePair("ghijK", "hi"),
                        ),
                        preFlight = PreFlightSpec( // TODO use "|" instead of "+"
                            executeCode = """
                                val signature = request
                                    .queryParameters
                                    .sortedBy { it.first }
                                    .joinToString("+") { it.second }
                                    .let {
                                        it.encodeToByteArray()
                                            .toSha256Hash()
                                            .encodeToBase64String()
                                    }

                                request.addQueryParameter(
                                    key = "mysignature",
                                    value = signature
                                )
                            """.trimIndent()
                        )
                    )
                )
            ),
            environment = TestEnvironment.LocalDefault,
            ignoreAssertQueryParameters = setOf("mysignature"),
        )

        assertEquals(
            "4i+aLUo5nm8GQPEkKT+brpQIGYexiqDol2kfxnJHAgk=",
            echoResponse.queryParameters.first { it.name == "mysignature" }.value
        )
    }
}

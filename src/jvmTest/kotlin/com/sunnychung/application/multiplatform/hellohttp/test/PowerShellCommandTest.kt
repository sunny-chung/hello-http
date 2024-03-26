package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.extension.CommandGenerator
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.WindowsOS
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlin.test.Test
import kotlin.test.assertEquals

class PowerShellCommandTest {

    @Test
    fun getRequest() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://httpbin.org/get",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.None,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    queryParameters = listOf(
                        UserKeyValuePair(key = "a", value = "b"),
                        UserKeyValuePair(key = "c", value = "d\"e"),
                    ),
                )
            )
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, null)
        }
        assertEquals("""
            Invoke-WebRequest `
              -Method "GET" `
              -Uri "https://httpbin.org/get?a=b&c=d%22e" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
              } | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun jsonRequest() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://httpbin.org/post",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.Json,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = StringBody("{\n\n  \"abc\": \"def\"\n\n}"),
                )
            )
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, null)
        }
        assertEquals("""
            Invoke-WebRequest `
              -Method "POST" `
              -Uri "https://httpbin.org/post" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "Content-Type" = "application/json"
              } `
              -Body "{`
            `
              `"abc`": `"def`"`
            `
            }" | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun formUrlEncoded() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://httpbin.org/post",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.FormUrlEncoded,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = FormUrlEncodedBody(listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                        UserKeyValuePair(key = "content-type", value = "application/json; charset=UTF-8"),
                    )),
                )
            )
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, null)
        }
        assertEquals("""
            Invoke-WebRequest `
              -Method "POST" `
              -Uri "https://httpbin.org/post" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "Content-Type" = "application/x-www-form-urlencoded"
              } `
              -Body @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "content-type" = "application/json; charset=UTF-8"
              } | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun multipartForm() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://httpbin.org/post",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.Multipart,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = MultipartBody(listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                        UserKeyValuePair(key = "content-type", value = "application/json; charset=UTF-8"),
                    )),
                )
            )
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, null)
        }
        assertEquals("""
            Invoke-WebRequest `
              -Method "POST" `
              -Uri "https://httpbin.org/post" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "Content-Type" = "multipart/form-data"
              } `
              -Form @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "content-type" = "application/json; charset=UTF-8"
              } | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun multipartFormWithFile() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://httpbin.org/post",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.Multipart,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = MultipartBody(listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                        UserKeyValuePair(id = "", key = "myfile", valueType = FieldValueType.File, value = "test.txt", isEnabled = true),
                        UserKeyValuePair(key = "content-type", value = "application/json; charset=UTF-8"),
                    )),
                )
            )
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, null)
        }
        assertEquals("""
            Invoke-WebRequest `
              -Method "POST" `
              -Uri "https://httpbin.org/post" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "Content-Type" = "multipart/form-data"
              } `
              -Form @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "myfile" = Get-Item -Path "test.txt"
                "content-type" = "application/json; charset=UTF-8"
              } | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun binaryBodyAndHttp2() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://httpbin.org/post",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.BinaryFile,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = FileBody("test.txt"),
                )
            )
        )
        val env = Environment(
            id = uuidString(),
            name = "Test",
            variables = mutableListOf(),
            httpConfig = HttpConfig(protocolVersion = HttpConfig.HttpProtocolVersion.Http2Only),
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, env)
        }
        assertEquals("""
            Invoke-WebRequest `
              -HttpVersion 2.0 `
              -Method "POST" `
              -Uri "https://httpbin.org/post" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
              } `
              -ContentType "" `
              -InFile "test.txt" | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }

    @Test
    fun insecureSsl() {
        val request = UserRequestTemplate(
            id = uuidString(),
            application = ProtocolApplication.Http,
            method = "POST",
            url = "https://192.168.1.2:8080/echo",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Test",
                    contentType = ContentType.Json,
                    headers = listOf(
                        UserKeyValuePair(key = "User-Agent", value = "Hello-HTTP/1.5.0-SNAPSHOT"),
                    ),
                    body = StringBody("{\"echo\":\"def\"}"),
                )
            )
        )
        val env = Environment(
            id = uuidString(),
            name = "Test",
            variables = mutableListOf(),
            sslConfig = SslConfig(isInsecure = true)
        )
        val generatedCommand = with (CommandGenerator(WindowsOS)) {
            request.toPowerShellInvokeWebRequestCommand(request.examples.first().id, env)
        }
        assertEquals("""
            Invoke-WebRequest `
              -SkipCertificateCheck `
              -Method "POST" `
              -Uri "https://192.168.1.2:8080/echo" `
              -Headers @{`
                "User-Agent" = "Hello-HTTP/1.5.0-SNAPSHOT"
                "Content-Type" = "application/json"
              } `
              -Body "{`"echo`":`"def`"}" | Select-Object -Expand RawContent
        """.trimIndent(), generatedCommand)
    }
}

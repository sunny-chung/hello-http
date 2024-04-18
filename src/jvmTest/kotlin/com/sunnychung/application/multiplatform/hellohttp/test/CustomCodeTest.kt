package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.helper.CustomCodeExecutor
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomCodeTest {
    @Test
    fun customFileAndSha256RsaSignature() {
        val httpRequest = HttpRequest(
            method = "POST",
            url = "https://httpbin.org/post",
            contentType = ContentType.None,
            application = ProtocolApplication.Http,
        )
        assertEquals(0, httpRequest.headers.size)
        assertEquals(0, httpRequest.queryParameters.size)

        CustomCodeExecutor("""
            environment ?: throw Exception("Missing environment")

            val keyFile = environment!!.userFiles.first { it.name == "my RSA private key" }
            val clientId = environment!!.variables.first { it.key == "clientId" }.value
            val merchantReference = environment!!.variables.first { it.key == "merchantReference" }.value

            val now = KInstant.parseFrom("2024-04-16T06:16:21.754Z", KDateTimeFormat.ISO8601_FORMATS)
            val r = request
            r.addHeader(key = "Time", value = now.toString())
            r.addHeader(
                "Signature",
                "${'$'}{clientId}${'$'}{merchantReference}${'$'}{now}"
                    .encodeToByteArray()
                    .toSha256WithRsaSignature(
                        keyFile.content.toPkcs8RsaPrivateKey()
                    )
                    .encodeToBase64String()
            )
        """.trimIndent())
            .executePreFlight(
                request = httpRequest,
                environment = Environment(
                    id = uuidString(),
                    name = "test",
                    variables = mutableListOf(
                        UserKeyValuePair("merchantReference", "1712890486962"),
                        UserKeyValuePair("clientId", "000000000000001"),
                    ),
                    userFiles = listOf(
                        ImportedFile(
                            id = uuidString(),
                            name = "my RSA private key",
                            originalFilename = "private.key",
                            createdWhen = KInstant.now(),
                            isEnabled = true,
                            content = (this::class.java.getResourceAsStream("/rsa/private.der") ?: throw RuntimeException("Missing test resource file ./rsa/private.der"))
                                .use {
                                    it.readAllBytes()
                                },
                        )
                    ),
                )
            )

        assertEquals(2, httpRequest.headers.size)
        assertEquals("2024-04-16T06:16:21.754Z", httpRequest.headers.first { it.first == "Time" }.second)
        assertEquals("VihNuqhs02Lbr3fcf0lu21DVDZ1N6S6+TESmHMR2O7DM/rlh6Lt/hVnu2d0GDfM6tcT99sEOfJ8xPB56uC+UfxLo7zE45QlXfqyF4w59T4s5oCDXAX3AhWTo8W6cgvJwXql3Jn3qg/Parr/NYElZQJjvzVU4+HstdXpjDvRkDIhWb4FUTAC9g2rGPg1uKd/mmwdITLfhjACMtueDcnLBjg4C72ZGRV0AOUqpgR1odwFQD+o/bET+geMISZD+6sSoaAC8RabLjjiB4zw6EVIxGMduSGwu3UIuxP9EKiY3SfSArqAPwmqRBlX8a/HT3SNFhBkleai21szAVgIROyytFQ==", httpRequest.headers.first { it.first == "Signature" }.second)

        assertEquals(0, httpRequest.queryParameters.size)
    }

    @Test
    fun decodeJsonToMap() {
        val httpRequest = HttpRequest(
            method = "GET",
            url = "https://httpbin.org/get",
            contentType = ContentType.None,
            application = ProtocolApplication.Http,
        )
        assertEquals(0, httpRequest.headers.size)
        assertEquals(0, httpRequest.queryParameters.size)

        CustomCodeExecutor("""
            val m = "{\"abc\":[{\"def\":{\"a\":\"b123\"}}]}".decodeJsonStringToMap() as Map<String, List<Map<String, Map<String, String>>>>
            request.addQueryParameter("a", m["abc"]!![0]!!["def"]!!["a"]!!)
        """.trimIndent())
            .executePreFlight(
                request = httpRequest,
                environment = null,
            )

        assertEquals(0, httpRequest.headers.size)

        assertEquals(1, httpRequest.queryParameters.size)
        assertEquals("b123", httpRequest.queryParameters.first().second)
    }

    @Test
    fun aesEncryptDecrypt() {
        val httpRequest = HttpRequest(
            method = "GET",
            url = "https://httpbin.org/get",
            contentType = ContentType.None,
            application = ProtocolApplication.Http,
        )
        assertEquals(0, httpRequest.headers.size)
        assertEquals(0, httpRequest.queryParameters.size)

        CustomCodeExecutor("""
            val key: SecretKey = "aDN6UTM5YlBLR3BoNGdydzMzb3hUTHpNam9ibFVrWnk=".decodeBase64StringToByteArray().toAesSecretKey()
            val iv: ByteArray = "f2e95da7ae1a20857c4c619c0018fd67".decodeHexStringToByteArray()

            val dataToBeEncrypted = "abcD123_45aavs".encodeToByteArray()

            val encrypted: ByteArray = dataToBeEncrypted.asEncrypted("AES/CBC/PKCS5Padding", key, iv)

            request.addQueryParameter("encrypted", encrypted.encodeToHexString())

            val decrypted: ByteArray = encrypted.asDecrypted("AES/CBC/PKCS5Padding", key, iv)

            request.addQueryParameter("decrypted", decrypted.decodeToString())
        """.trimIndent())
            .executePreFlight(
                request = httpRequest,
                environment = null,
            )

        assertEquals(0, httpRequest.headers.size)

        assertEquals(2, httpRequest.queryParameters.size)
        assertEquals("51d65fe1fe9750891a9e91f2aa7247f6", httpRequest.queryParameters.first { it.first == "encrypted" }.second)
        assertEquals("abcD123_45aavs", httpRequest.queryParameters.first { it.first == "decrypted" }.second)
    }
}

package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.importer.CurlCommandImporter
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class CurlCommandImporterTest {

    private val importer = CurlCommandImporter()

    @Test
    fun parseJsonRequest() {
        val request = importer.parseRequest(
            """
            time curl \
              --verbose \
              --request "POST" \
              --url "https://api.example.com/v1/users" \
              --header "Accept: application/json" \
              --header "Content-Type: application/json" \
              --data "{\"name\":\"Alice\"}"
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        assertEquals("https://api.example.com/v1/users", request.url)
        assertEquals("users", request.name)
        val example = request.examples.first()
        assertEquals(ContentType.Json, example.contentType)
        val body = assertIs<StringBody>(example.body)
        assertEquals("""{"name":"Alice"}""", body.value)
        assertEquals(
            listOf("Accept" to "application/json", "Content-Type" to "application/json"),
            example.headers.map { it.key to it.value },
        )
    }

    @Test
    fun parseMultipartRequestWithFile() {
        val request = importer.parseRequest(
            """
            curl \
              --request "POST" \
              --url "https://example.com/upload" \
              --form "description=\"upload-file\"" \
              --form "file=@\"/tmp/a.txt\""
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        val example = request.examples.first()
        assertEquals(ContentType.Multipart, example.contentType)
        val body = assertIs<MultipartBody>(example.body)
        assertEquals(2, body.value.size)
        assertEquals("description", body.value[0].key)
        assertEquals("upload-file", body.value[0].value)
        assertEquals(FieldValueType.String, body.value[0].valueType)
        assertEquals("file", body.value[1].key)
        assertEquals("/tmp/a.txt", body.value[1].value)
        assertEquals(FieldValueType.File, body.value[1].valueType)
    }

    @Test
    fun parseGetWithDataUrlEncoded() {
        val request = importer.parseRequest(
            """
            curl -G "https://example.com/search" \
              --data-urlencode "q=hello world" \
              --data-urlencode "lang=en"
            """.trimIndent()
        )

        assertEquals("GET", request.method)
        assertEquals("search", request.name)
        val example = request.examples.first()
        assertEquals(ContentType.None, example.contentType)
        assertNull(example.body)
        assertEquals(
            listOf("q" to "hello world", "lang" to "en"),
            example.queryParameters.map { it.key to it.value },
        )
    }

    @Test
    fun parseQueryParametersFromUrlWithQuestionMark() {
        val request = importer.parseRequest(
            """
            curl --request GET --url "https://www.postb.in/1772351307095-9288514354266?hello=world&escaped=a%2Bb%26c%3Dd"
            """.trimIndent()
        )

        assertEquals("GET", request.method)
        val example = request.examples.first()
        assertEquals(
            listOf("hello" to "world", "escaped" to "a+b&c=d"),
            example.queryParameters.map { it.key to it.value },
        )
    }

    @Test
    fun parseQueryParametersFromUrlUsingShortOptions() {
        val request = importer.parseRequest(
            """
            curl -X GET "https://example.com/path?from=short&hello=world"
            """.trimIndent()
        )

        assertEquals("GET", request.method)
        val example = request.examples.first()
        assertEquals(
            listOf("from" to "short", "hello" to "world"),
            example.queryParameters.map { it.key to it.value },
        )
    }

    @Test
    fun parseUrlOnly() {
        val request = importer.parseRequest(
            """
            curl http://example.com/empty
            """.trimIndent()
        )

        assertEquals("GET", request.method)
        val example = request.examples.first()
        assertEquals(
            "http://example.com/empty",
            request.url,
        )
    }

    @Test
    fun parseBinaryBody() {
        val request = importer.parseRequest(
            """
            curl --request POST --url https://example.com/upload --data-binary "@/tmp/payload.bin"
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        val example = request.examples.first()
        assertEquals(ContentType.BinaryFile, example.contentType)
        val body = assertIs<FileBody>(example.body)
        assertEquals("/tmp/payload.bin", body.filePath)
    }

    @Test
    fun rejectNonCurlCommand() {
        assertFailsWith<IllegalArgumentException> {
            importer.parseRequest("echo \"hello\"")
        }
    }

    @Test
    fun parseFormUrlEncodedWhenHeaderIndicatesForm() {
        val request = importer.parseRequest(
            """
            curl --request POST --url "https://example.com/post" \
              --header "Content-Type: application/x-www-form-urlencoded" \
              --data "name=alice&age=20"
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        val example = request.examples.first()
        assertEquals(ContentType.FormUrlEncoded, example.contentType)
        val body = assertIs<FormUrlEncodedBody>(example.body)
        assertEquals(
            listOf("name" to "alice", "age" to "20"),
            body.value.map { it.key to it.value },
        )
    }

    @Test
    fun parseJsonRequestUsingShortOptions() {
        val request = importer.parseRequest(
            """
            curl -X POST https://api.example.com/v1/users \
              -H "Accept: application/json" \
              -H "Content-Type: application/json" \
              -d "{\"name\":\"Bob\"}"
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        assertEquals("https://api.example.com/v1/users", request.url)
        val example = request.examples.first()
        assertEquals(ContentType.Json, example.contentType)
        val body = assertIs<StringBody>(example.body)
        assertEquals("""{"name":"Bob"}""", body.value)
        assertEquals(
            listOf("Accept" to "application/json", "Content-Type" to "application/json"),
            example.headers.map { it.key to it.value },
        )
    }

    @Test
    fun parseFormAndCookieUsingShortOptions() {
        val request = importer.parseRequest(
            """
            curl -X POST https://example.com/upload \
              -A "hello-http-test" \
              -b "sid=abc123; theme=dark" \
              -F "description=upload-short" \
              -F "file=@\"/tmp/short.txt\""
            """.trimIndent()
        )

        assertEquals("POST", request.method)
        val example = request.examples.first()
        assertEquals(ContentType.Multipart, example.contentType)
        assertEquals(
            listOf("User-Agent" to "hello-http-test"),
            example.headers.map { it.key to it.value },
        )
        assertEquals(
            listOf("sid" to "abc123", "theme" to "dark"),
            example.cookies.map { it.key to it.value },
        )
        val body = assertIs<MultipartBody>(example.body)
        assertEquals(2, body.value.size)
        assertEquals("description", body.value[0].key)
        assertEquals("upload-short", body.value[0].value)
        assertEquals(FieldValueType.String, body.value[0].valueType)
        assertEquals("file", body.value[1].key)
        assertEquals("/tmp/short.txt", body.value[1].value)
        assertEquals(FieldValueType.File, body.value[1].valueType)
    }
}

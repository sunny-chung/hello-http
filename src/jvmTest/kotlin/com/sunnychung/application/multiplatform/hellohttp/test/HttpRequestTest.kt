package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestTest {

    @Test
    fun `plus char is encoded`() {
        HttpRequest(
            method = "GET",
            url = "http://sunnychung.application.multiplatform.hellohttp/path",
            queryParameters = listOf("a" to "b+c", "d" to "e+f"),
            contentType = ContentType.None,
            application = ProtocolApplication.Http,
        ).run {
            assertEquals("http://sunnychung.application.multiplatform.hellohttp/path?a=b%2bc&d=e%2bf", getResolvedUri().toString().lowercase())
        }

//        HttpRequest(
//            method = "GET",
//            url = "http://sunnychung.application.multiplatform.hellohttp/path+xyz",
//            queryParameters = listOf("a" to "b+c", "d" to "e+f"),
//            contentType = ContentType.None,
//            application = ProtocolApplication.Http,
//        ).run {
//            assertEquals("http://sunnychung.application.multiplatform.hellohttp/path%2bxyz?a=b%2bc&d=e%2bf", getResolvedUri().toString().lowercase())
//        }
    }

    @Test
    fun `unicode is encoded`() {
        HttpRequest(
            method = "GET",
            url = "http://sunnychung.application.multiplatform.hellohttp/anything/中文字",
            queryParameters = listOf("a" to "b+c", "d" to "e+f"),
            contentType = ContentType.None,
            application = ProtocolApplication.Http,
        ).run {
            assertEquals("http://sunnychung.application.multiplatform.hellohttp/anything/%E4%B8%AD%E6%96%87%E5%AD%97?a=b%2bc&d=e%2bf".lowercase(), getResolvedUri().toString().lowercase())
        }
    }
}

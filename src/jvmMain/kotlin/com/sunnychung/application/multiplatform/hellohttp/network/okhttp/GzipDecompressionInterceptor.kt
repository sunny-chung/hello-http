package com.sunnychung.application.multiplatform.hellohttp.network.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSource

class GzipDecompressionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val source = response.body?.source()?.apply {
            request(Long.MAX_VALUE) // Buffer the entire body.
        }

        if ("gzip".equals(response.headers["Content-Encoding"], ignoreCase = true) && source != null) {
            var buffer = source.buffer
            val gzippedLength = buffer.size
            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            }
            return response.newBuilder().body(buffer.readByteArray().toResponseBody(response.body?.contentType())).build()
        }

        return response
    }
}

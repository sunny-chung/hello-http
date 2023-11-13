package com.sunnychung.application.multiplatform.hellohttp.network

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

class ContentEncodingDecompressProcessor {

    fun process(bodyBytes: ByteArray, contentEncoding: String): ByteArray {
        val encodings = contentEncoding.split(',')
            .map { it.trim().lowercase(Locale.US) }

        var stream: InputStream? = null

        fun ensureInputStream(): InputStream {
            return if (stream == null) {
                ByteArrayInputStream(bodyBytes)
            } else {
                stream!!
            }
        }

        encodings.asReversed().forEach {
            when (it) {
                "gzip", "x-gzip" -> stream = GZIPInputStream(ensureInputStream())
                "deflate" -> stream = InflaterInputStream(ensureInputStream())
            }
        }

        return if (stream != null) {
            stream!!.readAllBytes()
        } else {
            bodyBytes
        }
    }
}

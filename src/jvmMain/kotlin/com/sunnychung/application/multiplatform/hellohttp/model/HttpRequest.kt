package com.sunnychung.application.multiplatform.hellohttp.model

import org.apache.hc.core5.net.URIBuilder
import java.net.URI

data class HttpRequest(
    val method: String = "",
    val url: String = "",
    val headers: List<Pair<String, String>> = mutableListOf(),
    val queryParameters: List<Pair<String, String>> = mutableListOf(),
    val body: UserRequestBody? = null,
    val contentType: ContentType,
    val application: ProtocolApplication,
    val extra: Any? = null
) {
    fun getResolvedUri(): URI {
        return URIBuilder(url.replace(" ", "+"))
            .run {
                var b = this
                queryParameters.forEach {
                    b = b.addParameter(it.first, it.second)
                }
                b
            }
            .build()
            .let { URI.create(it.toASCIIString()) }
    }
}

/**
 * According to RFC 2616,
 *
 *        Method         = "OPTIONS"                ; Section 9.2
 *                       | "GET"                    ; Section 9.3
 *                       | "HEAD"                   ; Section 9.4
 *                       | "POST"                   ; Section 9.5
 *                       | "PUT"                    ; Section 9.6
 *                       | "DELETE"                 ; Section 9.7
 *                       | "TRACE"                  ; Section 9.8
 *                       | "CONNECT"                ; Section 9.9
 *                       | extension-method
 *        extension-method = token
 *
 *        token          = 1*<any CHAR except CTLs or separators>
 *        separators     = "(" | ")" | "<" | ">" | "@"
 *                       | "," | ";" | ":" | "\" | <">
 *                       | "/" | "[" | "]" | "?" | "="
 *                       | "{" | "}" | SP | HT
 *
 *        CHAR           = <any US-ASCII character (octets 0 - 127)>
 *        CTL            = <any US-ASCII control character
 *                         (octets 0 - 31) and DEL (127)>
 *        SP             = <US-ASCII SP, space (32)>
 *        HT             = <US-ASCII HT, horizontal-tab (9)>
 *        <">            = <US-ASCII double-quote mark (34)>
 */
fun String.isValidHttpMethod(): Boolean {
    if (isEmpty()) return false
    if (any { it.code <= 31 || it.code >= 127 || it in setOf('(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' ', '\t') }) {
        return false
    }
    return true
}

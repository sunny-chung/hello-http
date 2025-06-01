package com.sunnychung.application.multiplatform.hellohttp.model

//import org.apache.hc.core5.net.URIBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class HttpRequest(
    val method: String = "",
    val url: String = "",
    headers: List<Pair<String, String>> = mutableListOf(),
    queryParameters: List<Pair<String, String>> = mutableListOf(),
    val body: UserRequestBody? = null,
    val contentType: ContentType,
    val application: ProtocolApplication,
    val extra: Any? = null,

    /**
     * Not used in transmission.
     */
    val applicableVariables: Map<String, String> = emptyMap(),

    /**
     * Not used in transmission.
     */
    val applicableCookies: Map<String, String> = emptyMap(),
) {
    private val initialHeaders = headers
    private val newHeaders: MutableList<Pair<String, String>> = mutableListOf()
    private val initialQueryParameters = queryParameters
    private val newQueryParameters: MutableList<Pair<String, String>> = mutableListOf()

    val headers get() = initialHeaders + newHeaders
    val queryParameters get() = initialQueryParameters + newQueryParameters

    fun getResolvedUri(): URI {
        return UriComponentsBuilder.fromUriString(url.replace(" ", "+"))
            .run {
                var b = this
                queryParameters.forEach {
                    b = b.queryParam(it.first, it.second)
                }
                b
            }
            .encode()
            .build()
            .let { URI.create(it.toString()) }
    }

    fun addHeader(key: String, value: String) {
        newHeaders += key to value
    }

    fun addQueryParameter(key: String, value: String) {
        newQueryParameters += key to value
    }

    fun copy(
        method: String = this.method,
        url: String = this.url,
        headers: List<Pair<String, String>> = this.headers,
        queryParameters: List<Pair<String, String>> = this.queryParameters,
        body: UserRequestBody? = this.body,
        contentType: ContentType = this.contentType,
        application: ProtocolApplication = this.application,
        extra: Any? = this.extra,
        applicableVariables: Map<String, String> = this.applicableVariables,
    ): HttpRequest {
        return HttpRequest(
            method = method,
            url = url,
            headers = headers,
            queryParameters = queryParameters,
            body = body,
            contentType = contentType,
            application = application,
            extra = extra,
            applicableVariables = applicableVariables,
        )
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

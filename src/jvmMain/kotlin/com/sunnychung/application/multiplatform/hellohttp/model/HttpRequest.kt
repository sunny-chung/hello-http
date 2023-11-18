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
        return URIBuilder(url)
            .run {
                var b = this
                queryParameters.forEach {
                    b = b.addParameter(it.first, it.second)
                }
                b
            }
            .build()
    }
}

package com.sunnychung.application.multiplatform.hellohttp.model.insomniav4

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

object InsomniaV4 {
    data class Dump(
        @JsonProperty("_type") val type: String,
        @JsonProperty("__export_format") val exportFormat: Int,
        @JsonProperty("__export_date") val exportDate: String,
        @JsonProperty("__export_source") val exportSource: String,
        val resources: List<Any>,
    )

    interface Request {
        val id: String
        val parentId: String
        val url: String
        val name: String
        val description: String
        val parameters: List<HttpRequest.KeyValue>
        val headers: List<HttpRequest.KeyValue>
        val authentication: HttpRequest.Authentication
        val type: String
    }

    data class HttpRequest(
        @JsonProperty("_id") override val id: String,
        override val parentId: String,
        override val url: String,
        override val name: String,
        override val description: String,
        val method: String,
        val body: Body,
        override val parameters: List<KeyValue>,
        override val headers: List<KeyValue>,
        override val authentication: Authentication,
        @JsonProperty("_type") override val type: String,
    ) : Request {
        data class Body(
            val mimeType: String?,
            val text: String?,
            val params: List<KeyValue>?,
            val fileName: String? = null,
        )

        data class KeyValue(
            val id: String?,
            val name: String,
            val value: String,
            val description: String?,
            val disabled: Boolean?,
            val type: String?, // only in multipart body params
            val fileName: String?, // only in multipart body params
        )

        data class Authentication(
            val type: String?,
            val token: String?,
            val prefix: String?,
            val disabled: Boolean?,
        )
    }

    data class WebSocketRequest(
        @JsonProperty("_id") override val id: String,
        override val parentId: String,
        override val url: String,
        override val name: String,
        override val description: String,
        override val parameters: List<HttpRequest.KeyValue>,
        override val headers: List<HttpRequest.KeyValue>,
        override val authentication: HttpRequest.Authentication,
        @JsonProperty("_type") override val type: String,
    ) : Request

    data class RequestPayload(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val name: String,
        val value: String,
        val mode: String,
        @JsonProperty("_type") val type: String,
    )

    data class RequestGroup(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val name: String,
        @JsonProperty("_type") val type: String,
    )

    data class Environment(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val name: String,
        val data: JsonNode,
        @JsonProperty("_type") val type: String,
    )

    data class Workspace(
        @JsonProperty("_id") val id: String,
        @JsonInclude val parentId: String?, // always null
        val name: String,
        val description: String,
        val scope: String,
        @JsonProperty("_type") val type: String,
    )
}

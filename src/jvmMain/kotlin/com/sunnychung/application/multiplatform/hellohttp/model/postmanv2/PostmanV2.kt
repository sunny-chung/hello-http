package com.sunnychung.application.multiplatform.hellohttp.model.postmanv2

import com.fasterxml.jackson.annotation.JsonProperty

object PostmanV2 {
    sealed interface File

    data class Collection(
        val info: FileInfo,
        val item: List<Item>,
    ) : File

    data class Environment(
        val id: String,
        val name: String,
        val values: List<EnvKeyValue>,
    ) : File

    data class SingleCollection(
        val info: FileInfo,
        val item: List<Item>,
        val variable: List<SimpleKeyValue>?,
    ) : File

    data class Archive(
        val environment: Map<String, Boolean>,
        val collection: Map<String, Boolean>,
    ) : File

    data class FileInfo(
        @JsonProperty("_postman_id") val id: String,
        val name: String,
        val schema: String,
    )

    data class Item(
        val id: String,
        val name: String,
        val item: List<Item>? = null,
        val request: Request? = null,
        val auth: Auth? = null,
    )

    data class Request(
        val method: String,
        val header: List<KeyValue>,
        val auth: Auth?,
        val body: Body?,
        val url: Url?,
    )

    data class KeyValue(
        val key: String,
        val value: String?, // missing if type = file
        val disabled: Boolean?,
        val type: String?, // known values: text, file
        val src: String?, // file path
    )

    data class EnvKeyValue(
        val key: String,
        val value: String?,
        val enabled: Boolean?,
        val type: String?, // known values: default, secret
    )

    data class Url(
        val raw: String,
        val query: List<KeyValue>?,

        val protocol: String? = null,
        val host: List<String>? = null,
        val port: String? = null,
        val path: List<String>? = null,
    )

    data class Body(
        val mode: String, // known values: formdata, raw, urlencoded, file
        val formdata: List<KeyValue>? = null,
        val urlencoded: List<KeyValue>? = null,
        val file: File? = null,
        val raw: String? = null, // json
        val graphql: Graphql? = null, // TODO import
        val options: Options? = null,
    ) {
        data class Options(
            val raw: Raw?
        )
        data class Raw(
            val language: String
        )

        data class File(
            val src: String?
        )

        data class Graphql(
            val query: String?,
            val variables: String?,
        )
    }

    data class Auth(
        val type: String, // bearer
        val bearer: List<KeyValue>?,
    )

    data class SimpleKeyValue(
        val key: String,
        val value: String?,
    )
}

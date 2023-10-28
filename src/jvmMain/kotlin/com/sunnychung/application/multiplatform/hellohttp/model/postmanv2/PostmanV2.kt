package com.sunnychung.application.multiplatform.hellohttp.model.postmanv2

import com.fasterxml.jackson.annotation.JsonProperty

object PostmanV2 {
    data class Collection(
        val info: FileInfo,
        val item: List<Item>,
    )

    data class Environment(
        val id: String,
        val name: String,
        val values: List<EnvKeyValue>,
    )

    data class SingleCollection(
        val info: FileInfo,
        val item: List<Item>,
        val variable: List<SimpleKeyValue>?,
    )

    data class FileInfo(
        @JsonProperty("_postman_id") val id: String,
        val name: String,
        val schema: String,
    )

    data class Item(
        val name: String,
        val item: List<Item>?,
        val request: Request?,
        val auth: Auth?,
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
    )

    data class Body(
        val mode: String, // known values: formdata, raw, urlencoded, file
        val formdata: List<KeyValue>?,
        val urlencoded: List<KeyValue>?,
        val file: File?,
        val raw: String?, // json
        val options: Options?,
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

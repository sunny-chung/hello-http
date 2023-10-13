package com.sunnychung.application.multiplatform.hellohttp.manager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class PrettifierManager {
    private val registrations: MutableMap<String, MutableSet<Prettifier>> = mutableMapOf()

    fun register(contentType: String, prettifier: Prettifier) {
        registrations.getOrPut(contentType) { linkedSetOf() } += prettifier
    }

    init {
        // JSON
        register(
            contentType = "application/json",
            prettifier = Prettifier(
                formatName = "JSON (Prettified)",
                prettify = {
                    jacksonObjectMapper().readTree(it).toPrettyString()
                }
            )
        )
    }

    fun matchPrettifiers(contentType: String): List<Prettifier> {
        return registrations
            .filter { Regex("\\b${Regex.escape(it.key)}\\b").containsMatchIn(contentType) }
            .flatMap { it.value }
            .distinct()
    }
}

class Prettifier(
    val formatName: String,
    val prettify: (ByteArray) -> String,
)

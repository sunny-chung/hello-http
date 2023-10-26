package com.sunnychung.application.multiplatform.hellohttp.manager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class PrettifierManager {
    private val registrations: MutableSet<PrettifierRegistration> = mutableSetOf()

    fun register(contentTypeRegex: Regex, prettifier: Prettifier) {
        registrations += PrettifierRegistration(contentTypeRegex = contentTypeRegex, prettifier = prettifier)
    }

    init {
        // JSON
        register(
            contentTypeRegex = "application\\/(json|.+\\+json)".toRegex(),
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
            .filter { it.contentTypeRegex.matches(contentType) }
            .map { it.prettifier }
            .distinct()
    }
}

class Prettifier(
    val formatName: String,
    val prettify: (ByteArray) -> String,
)

data class PrettifierRegistration(
    val contentTypeRegex: Regex,
    val prettifier: Prettifier,
)

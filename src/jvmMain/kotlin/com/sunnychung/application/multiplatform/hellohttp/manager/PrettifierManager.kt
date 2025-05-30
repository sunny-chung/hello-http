package com.sunnychung.application.multiplatform.hellohttp.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.model.PrettifyResult
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.parser.JsonParser
import com.sunnychung.application.multiplatform.hellohttp.util.log

class PrettifierManager {
    private val registrations: MutableSet<PrettifierRegistration> = mutableSetOf()

    fun register(contentTypeRegex: Regex, prettifier: Prettifier) {
        registrations += PrettifierRegistration(contentTypeRegex = contentTypeRegex, prettifier = prettifier)
    }

    init {
        // JSON
        register(
            contentTypeRegex = "application\\/(json|.+\\+json)\\b.*".toRegex(),
            prettifier = Prettifier(
                formatName = "JSON (Prettified)",
                prettify = {
                    val start = System.currentTimeMillis()
                    JsonParser(it).prettify().also {
                        log.i { "Prettify took ${System.currentTimeMillis() - start}ms" }
                    }
                }
            )
        )
    }

    fun matchPrettifiers(application: ProtocolApplication, contentType: String): List<Prettifier> {
        if (application == ProtocolApplication.Grpc) { // TODO refactor to a better logical structure
            return matchPrettifiers(ProtocolApplication.Http, "application/json")
        }

        return registrations
            .filter { it.contentTypeRegex.matches(contentType) }
            .map { it.prettifier }
            .distinct()
    }

    fun allPrettifiers(): List<Prettifier> {
        return registrations.map { it.prettifier }.distinct()
    }
}

class Prettifier(
    val formatName: String,
    val prettify: (ByteArray) -> PrettifyResult,
)

data class PrettifierRegistration(
    val contentTypeRegex: Regex,
    val prettifier: Prettifier,
)

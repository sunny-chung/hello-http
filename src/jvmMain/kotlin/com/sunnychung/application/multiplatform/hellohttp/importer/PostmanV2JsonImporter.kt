package com.sunnychung.application.multiplatform.hellohttp.importer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.extension.`if`
import com.sunnychung.application.multiplatform.hellohttp.model.postmanv2.PostmanV2
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import java.io.File

class PostmanV2JsonImporter {

    suspend fun importAsProject(file: File, projectName: String) {
        val jsonParser = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val singleCollection = jsonParser.readValue(file, PostmanV2.SingleCollection::class.java)

        val environment = singleCollection.variable?.`if` { it.isNotEmpty() }?.let {
            PostmanV2.Environment(
                id = uuidString(),
                name = "Default",
                values = it.map {
                    PostmanV2.EnvKeyValue(
                        key = it.key,
                        value = it.value ?: "",
                        enabled = true,
                        type = "default"
                    )
                }
            )
        }

        PostmanV2ZipImporter().importCollection(
            singleCollection.let {
                PostmanV2.Collection(
                    info = it.info.copy(name = projectName),
                    item = it.item
                )
            },
            listOfNotNull(environment)
        )

    }
}
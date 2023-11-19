package com.sunnychung.application.multiplatform.hellohttp.model.payload

import com.fasterxml.jackson.databind.JsonNode

data class GraphqlErrorPayload(
    val errors: JsonNode? // it is an array
)

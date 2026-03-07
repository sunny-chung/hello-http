package com.sunnychung.application.multiplatform.hellohttp.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.exporter.InsomniaV4Exporter
import com.sunnychung.application.multiplatform.hellohttp.importer.InsomniaV4Importer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InsomniaGraphqlVariablesConversionTest {

    @Test
    fun exporterShouldFallbackToStringWhenGraphqlVariablesContainTemplatePlaceholders() {
        val exporter = InsomniaV4Exporter()
        val rawVariables = """
            {
              "input": {
                "name": "Alice",
                "value": {{dynamicValue}}
              },
              "stopAt": {{stopAt}}
            }
        """.trimIndent()

        val parsed = exporter.parseInsomniaGraphqlVariablesOrFallbackToString(rawVariables)

        assertEquals(rawVariables, parsed)
    }

    @Test
    fun exporterShouldParseJsonWhenGraphqlVariablesAreValidJson() {
        val exporter = InsomniaV4Exporter()
        val rawVariables = """{"input":{"name":"Alice","count":3}}"""

        val parsed = exporter.parseInsomniaGraphqlVariablesOrFallbackToString(rawVariables)

        assertIs<JsonNode>(parsed)
        assertEquals(3, parsed["input"]["count"].asInt())
    }

    @Test
    fun importerShouldKeepGraphqlVariableStringWithoutAddingQuotes() {
        val importer = InsomniaV4Importer()
        val variables = """{"value": {{dynamicValue}}}"""

        val converted = importer.run {
            variables.toInsomniaGraphqlVariablesText(jacksonObjectMapper())
        }

        assertEquals("""{"value": {{dynamicValue}}}""", converted)
    }

    @Test
    fun importerShouldSerializeNonStringGraphqlVariablesToJsonText() {
        val importer = InsomniaV4Importer()
        val jsonParser = jacksonObjectMapper()
        val variables = mapOf(
            "input" to mapOf(
                "name" to "Alice",
                "count" to 3,
            ),
        )

        val converted = importer.run {
            variables.toInsomniaGraphqlVariablesText(jsonParser)
        }

        assertEquals("{\n  \"input\" : {\n    \"name\" : \"Alice\",\n    \"count\" : 3\n  }\n}", converted)
    }
}

package com.sunnychung.application.multiplatform.hellohttp.parser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.model.PrettifyResult

class JsonParser(jsonBytes: ByteArray) {
    private val tree = jacksonObjectMapper().apply {
        enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    }.readTree(jsonBytes)

    constructor(json: String) : this(json.encodeToByteArray())

    fun prettify(): PrettifyResult {
        var lineIndex = 0
        val lineGroups = mutableListOf<IntRange>()
        val startLineStack = mutableListOf<Int>()
        val charGroups = mutableListOf<IntRange>()
        val startCharStack = mutableListOf<Int>()

        return PrettifyResult(
            prettyString = buildString {
                fun indent(level: Int) {
                    if (level > 0) {
                        append(" ".repeat(2 * level))
                    }
                }

                fun StringBuilder.transverse(node: JsonNode, indentLevel: Int) {
                    if (node.isArray) {
                        if (node.isEmpty) {
                            append("[]")
                        } else {
                            startCharStack += lastIndex + 1
                            append('[')
                            startLineStack += lineIndex
                            if (node.size() <= 20 && node.all { it.isValueNode }) {
                                node.forEachIndexed { i, it ->
                                    if (i > 0) {
                                        append(", ")
                                    }
                                    transverse(it, 0)
                                }
                            } else {
                                append("\n")
                                ++lineIndex
                                node.forEachIndexed { i, it ->
                                    if (i > 0) {
                                        append(",\n")
                                        ++lineIndex
                                    }
                                    indent(indentLevel + 1)
                                    transverse(it, indentLevel + 1)
                                }
                                append("\n")
                                ++lineIndex
                                indent(indentLevel)
                            }
                            append(']')
                            lineGroups += startLineStack.removeLast() .. lineIndex
                            charGroups += startCharStack.removeLast() .. lastIndex
                        }
                    } else if (node.isObject) {
                        if (node.isEmpty) {
                            append("{}")
                        } else {
                            startCharStack += lastIndex + 1
                            startLineStack += lineIndex
                            append("{\n")
                            ++lineIndex
                            node.fields().withIndex().forEach { (i, it) ->
                                if (i > 0) {
                                    append(",\n")
                                    ++lineIndex
                                }
                                indent(indentLevel + 1)
                                append("\"${it.key}\": ")
                                transverse(it.value, indentLevel + 1)
                            }
                            append("\n")
                            ++lineIndex
                            indent(indentLevel)
                            append('}')
                            lineGroups += startLineStack.removeLast() .. lineIndex
                            charGroups += startCharStack.removeLast() .. lastIndex
                        }
                    } else if (node.isValueNode) {
                        append(node.toPrettyString())
                    } else {
                        throw RuntimeException("what is this? -- $node")
                    }
                }

                transverse(tree, 0)
            },
            collapsableLineRange = lineGroups,
            collapsableCharRange = charGroups,
        )
    }
}

package com.sunnychung.application.multiplatform.hellohttp.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class JsonParser(jsonBytes: ByteArray) {
    val tree = jacksonObjectMapper().readTree(jsonBytes)

    constructor(json: String) : this(json.encodeToByteArray())

    fun prettify(): String {
        return buildString {
            fun indent(level: Int) {
                if (level > 0) {
                    append(" ".repeat(2 * level))
                }
            }

            fun StringBuilder.transverse(node: JsonNode, indentLevel: Int) {
                if (node.isArray) {
                    append('[')
                    if (node.size() <= 20 && node.all { it.isValueNode }) {
                        node.forEachIndexed { i, it ->
                            if (i > 0) {
                                append(", ")
                            }
                            transverse(it, 0)
                        }
                    } else {
                        append("\n")
                        node.forEachIndexed { i, it ->
                            if (i > 0) {
                                append(",\n")
                            }
                            indent(indentLevel + 1)
                            transverse(it, indentLevel + 1)
                        }
                        append("\n")
                        indent(indentLevel)
                    }
                    append(']')
                } else if (node.isObject) {
                    if (node.isEmpty) {
                        append("{}")
                    } else {
                        append("{\n")
                        node.fields().withIndex().forEach { (i, it) ->
                            if (i > 0) {
                                append(",\n")
                            }
                            indent(indentLevel + 1)
                            append("\"${it.key}\": ")
                            transverse(it.value, indentLevel + 1)
                        }
                        append("\n")
                        indent(indentLevel)
                        append('}')
                    }
                } else if (node.isValueNode) {
                    append(node.toPrettyString())
                } else {
                    throw RuntimeException("what is this? -- $node")
                }
            }

            transverse(tree, 0)
        }
    }
}

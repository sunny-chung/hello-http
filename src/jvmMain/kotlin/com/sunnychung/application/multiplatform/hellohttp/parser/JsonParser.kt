package com.sunnychung.application.multiplatform.hellohttp.parser

import com.dslplatform.json.DslJson
import com.dslplatform.json.ObjectConverter
import com.dslplatform.json.StringConverter
import com.sunnychung.application.multiplatform.hellohttp.model.PrettifyResult
import com.sunnychung.application.multiplatform.hellohttp.util.log

private val WHITESPACE_BYTES: Set<Byte> = listOf(' ', '\n', '\r', '\t').map { it.code.toByte() }.toSet()

class JsonParser(jsonBytes: ByteArray) {
    private val parser = DslJson<Any?>()
    private val writer = parser.newWriter()
    private val tree: Any?
    init {
        val reader = parser.newReader().process(jsonBytes, jsonBytes.size)
        reader.nextToken
        tree = ObjectConverter.deserializeObject(reader)
        if (reader.currentIndex != jsonBytes.size) { // there is something after a valid JSON
            // it is considered to be valid if the extra string contains only whitespaces
            while (reader.currentIndex < jsonBytes.size && reader.read() in WHITESPACE_BYTES) {
                // continue
            }
            // otherwise, the string is not a valid JSON
            if (reader.last() !in WHITESPACE_BYTES) {
                throw IllegalArgumentException("There is something extra after a JSON to make it invalid")
            }
        }
    }

    constructor(json: String) : this(json.encodeToByteArray())

    fun prettify(): PrettifyResult {
        var lineIndex = 0
        val lineGroups = mutableListOf<IntRange>()
        val startLineStack = mutableListOf<Int>()
        val charGroups = mutableListOf<IntRange>()
        val startCharStack = mutableListOf<Int>()
        val literalRange = mutableListOf<IntRange>()

        return PrettifyResult(
            prettyString = buildString {
                fun indent(level: Int) {
                    if (level > 0) {
                        append(" ".repeat(2 * level))
                    }
                }

                fun StringBuilder.transverse(node: Any?, indentLevel: Int) {
                    if (node is List<*>) {
                        if (node.isEmpty()) {
                            append("[]")
                        } else {
                            startCharStack += lastIndex + 1
                            append('[')
                            startLineStack += lineIndex
                            if (node.size <= 20 && node.all { it.isValue }) {
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
                    } else if (node is Map<*, *>) {
                        if (node.isEmpty()) {
                            append("{}")
                        } else {
                            startCharStack += lastIndex + 1
                            startLineStack += lineIndex
                            append("{\n")
                            ++lineIndex
                            node.entries.forEachIndexed { i, it ->
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
                    } else if (node is String) {
//                        val baos = ByteArrayOutputStream()
//                        parser.serialize(node, baos)
//                        append(baos.toByteArray().decodeToString())

                        val start = lastIndex + 1

                        StringConverter.serialize(node, writer)
                        append(writer.toString())
                        writer.reset()

                        val end = lastIndex
                        log.v { "literalRange += $start .. $end" }

                        literalRange += start .. end
                    } else {
                        val start = lastIndex + 1

                        append(node.toString())

                        literalRange += start .. lastIndex

//                    } else {
//                        throw RuntimeException("what is this? -- $node")
                    }
                }

                transverse(tree, 0)
            },
            collapsableLineRange = lineGroups,
            collapsableCharRange = charGroups,
            literalRange = literalRange,
        )
    }

    private val Any?.isValue: Boolean
        get() = this !is Map<*, *> && this !is List<*>
}

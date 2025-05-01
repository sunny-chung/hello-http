package com.sunnychung.application.multiplatform.hellohttp.parser

import com.dslplatform.json.JsonReader
import com.dslplatform.json.NumberConverter

/**
 * This implementation is based on `com.dslplatform.json.ObjectConverter` of DSL-JSON v2.0.2.
 */
class JsonParser3(val reader: JsonReader<*>) {

    private val tokens = mutableListOf<TokenWithoutValue<JsonNodeType>>()

    private val currentPos: Int
        get() = reader.currentIndex

    fun parse(): List<TokenWithoutValue<JsonNodeType>> {
        deserializeObject()
        return tokens
    }

    private fun deserializeObject(): Any? {
        val start = currentPos - 1
        when (reader.last().toInt().toChar()) {
            'n' -> {
                if (!reader.wasNull()) {
                    throw reader.newParseErrorAt("Expecting 'null' for null constant", 0)
                }
                tokens += TokenWithoutValue(start ..< currentPos, JsonNodeType.NOTHING)
                return null
            }

            't' -> {
                if (!reader.wasTrue()) {
                    throw reader.newParseErrorAt("Expecting 'true' for true constant", 0)
                }
                tokens += TokenWithoutValue(start ..< currentPos, JsonNodeType.TRUE)
                return true
            }

            'f' -> {
                if (!reader.wasFalse()) {
                    throw reader.newParseErrorAt("Expecting 'false' for false constant", 0)
                }
                tokens += TokenWithoutValue(start ..< currentPos, JsonNodeType.FALSE)
                return false
            }

            '"' -> return reader.readString().also { tokens += TokenWithoutValue(start ..< currentPos, JsonNodeType.STRING) }
            '{' -> return deserializeMap()
            '[' -> return deserializeList()
            else -> return NumberConverter.deserializeNumber(reader).also { tokens += TokenWithoutValue(start ..< currentPos, JsonNodeType.NUMBER) }
        }
    }

    private fun deserializeList(): ArrayList<Any?> {
        if (reader.last() != '['.code.toByte()) throw reader.newParseError("Expecting '[' for list start")
        var nextToken = reader.nextToken
        if (nextToken == ']'.code.toByte()) return ArrayList(0)
        val res = ArrayList<Any?>(0)
//        res.add(deserializeObject())
        deserializeObject()
        while ((reader.nextToken.also { nextToken = it }) == ','.code.toByte()) {
            reader.nextToken
//            res.add(deserializeObject())
            deserializeObject()
        }
        if (nextToken != ']'.code.toByte()) throw reader.newParseError("Expecting ']' for list end")
        return res
    }

    private fun deserializeMap(): LinkedHashMap<String, Any?> {
        if (reader.last() != '{'.code.toByte()) throw reader.newParseError("Expecting '{' for map start")
        var nextToken = reader.nextToken
        if (nextToken == '}'.code.toByte()) return LinkedHashMap(0)
        val res = LinkedHashMap<String, Any?>(0)

        val start = currentPos - 1
        var key = reader.readKey()
        tokens += TokenWithoutValue(start ..< start + 1 /* '"' */ + key.length + 1 /* '"' */, JsonNodeType.OBJECT_KEY)

        /*res[key] =*/ deserializeObject()
        while ((reader.nextToken.also { nextToken = it }) == ','.code.toByte()) {
            reader.nextToken
            val start = currentPos - 1
            key = reader.readKey()
            tokens += TokenWithoutValue(start ..< start + 1 /* '"' */ + key.length + 1 /* '"' */, JsonNodeType.OBJECT_KEY)
            /*res[key] =*/ deserializeObject()
        }
        if (nextToken != '}'.code.toByte()) throw reader.newParseError("Expecting '}' for map end")
        return res
    }
}

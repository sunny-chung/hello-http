package com.sunnychung.application.multiplatform.hellohttp.parser

import com.sunnychung.application.multiplatform.hellohttp.error.ParseException
import com.sunnychung.application.multiplatform.hellohttp.util.log

/**
 * This implementation does not stand out from commonly seen JSON parsers.
 */
class JsonParser2(val json: CharSequence) {
    val lexer = JsonLexer(json)
    private var currentToken: TokenWithoutValue<JsonTokenType> = readToken()
    private val pos: Int
        get() = currentToken.range.start

    private fun readToken(): TokenWithoutValue<JsonTokenType> {
        val t = lexer.readToken()
        currentToken = t
        return t
    }

    private inline fun isSymbol(c: Char): Boolean {
        return currentToken.type == JsonTokenType.SYMBOL && json[currentToken.range.start] == c
    }

    private inline fun isIdentifier(s: String): Boolean {
        return currentToken.type == JsonTokenType.IDENTIFIER && json.subSequence(currentToken.range) == s
    }

    private fun eatSymbol(expected: Char) {
        if (isSymbol(expected)) {
            readToken()
        } else {
            throw ParseException("Expected symbol $expected but found '${json[currentToken.range.start]}'")
        }
    }

    fun document(): NodeWithoutValue<JsonNodeType>? {
        val t = anyValue()
        if (currentToken.type != JsonTokenType.EOF) {
            throw ParseException("Expected EOF but got token ${currentToken.type}")
        }
        return t
    }

    fun anyValue(): NodeWithoutValue<JsonNodeType>? {
        return when (currentToken.type) {
            JsonTokenType.SYMBOL -> when (json[currentToken.range.start]) {
                '{' -> {
                    `object`()
                }
                '[' -> {
                    list()
                }
                else -> throw ParseException("Unexpected symbol ${json[currentToken.range.start]}")
            }

            JsonTokenType.STRING -> {
                NodeWithoutValue(currentToken.range, JsonNodeType.STRING)
                    .also { readToken() }
            }
            JsonTokenType.NUMBER -> {
                NodeWithoutValue(currentToken.range, JsonNodeType.NUMBER)
                    .also { readToken() }
            }
            JsonTokenType.IDENTIFIER -> when {
                isIdentifier("true") -> NodeWithoutValue(currentToken.range, JsonNodeType.TRUE)
                isIdentifier("false") -> NodeWithoutValue(currentToken.range, JsonNodeType.FALSE)
                isIdentifier("null") -> NodeWithoutValue(currentToken.range, JsonNodeType.NOTHING)
                else -> throw ParseException("Unexpected identifier ${json.subSequence(currentToken.range)}")
            }.also { readToken() }
            JsonTokenType.EOF -> null
        }
    }

    fun `object`(): NodeWithoutValue<JsonNodeType> {
        val start = pos
        eatSymbol('{')
        val childs = buildList {
            var isFirst = true
            while (!isSymbol('}')) {
                if (!isFirst) {
                    eatSymbol(',')
                }
                val t = currentToken.also {
                    if (it.type != JsonTokenType.STRING) {
                        throw ParseException("Expected string token at $pos but found ${it.type}")
                    }
                }
                add(NodeWithoutValue(t.range, JsonNodeType.OBJECT_KEY))
                readToken()
                eatSymbol(':')
                add(anyValue()!!) // TODO addAll seems bad in performance
                isFirst = false
            }
        }
        eatSymbol('}')
        return NodeWithoutValue(start ..< pos, JsonNodeType.OBJECT, childs = childs)
    }

    fun list(): NodeWithoutValue<JsonNodeType> {
        val start = pos
        eatSymbol('[')
        val childs = buildList {
            var isFirst = true
            while (!isSymbol(']')) {
                if (!isFirst) {
                    eatSymbol(',')
                }
                add(anyValue()!!)
                isFirst = false
            }
        }
        eatSymbol(']')
        return NodeWithoutValue(start ..< pos, JsonNodeType.LIST, childs = childs)
    }
}

class JsonLexer(val json: CharSequence) {
    val length = json.length
    var pos = 0

    inline fun currentChar() = if (pos < length) json[pos] else null

    private inline fun advanceChar() {
        if (pos < length) {
            ++pos
        }
    }

    fun eat(expected: Char) {
        if (currentChar() == expected) {
            advanceChar()
        } else {
            throw ParseException("Expected $expected at $pos but found '${currentChar() ?: "EOF"}'")
        }
    }

    internal fun Char.isIdentifierChar() = isLetter()

    fun readChar() {
        if (currentChar() == '\\') {
            advanceChar()
        }
        advanceChar()
    }

    fun readStringContent(): IntRange {
        val start = pos
        while (currentChar() !in setOf('"', null)) {
            readChar()
        }
        return start until pos
    }

    fun readInteger(): IntRange {
        val start = pos
        while (currentChar()?.isDigit() == true) {
            advanceChar()
        }
        if (start == pos) {
            throw ParseException("Invalid number")
        }
        return start until pos
    }

    fun readNumber(): IntRange {
        val start = pos
        if (currentChar() == '-') {
            advanceChar() // eat
        }
        readInteger()
        if (currentChar() == '.') {
            advanceChar()
            readInteger()
        }
        return start until pos
    }

    fun readIdentifier(): IntRange {
        val start = pos
        while (currentChar()?.isIdentifierChar() == true) {
            advanceChar()
        }
        return start until pos
    }

    fun readToken(): TokenWithoutValue<JsonTokenType> {
        val start = pos
        while (currentChar() != null) {
            val c = currentChar()!!
            val token = when {
                c.isWhitespace() -> {
                    advanceChar()
                    continue
                }
                c.isDigit() || c == '-' -> readNumber() to JsonTokenType.NUMBER
                c.isIdentifierChar() -> readIdentifier() to JsonTokenType.IDENTIFIER
                c == '"' -> {
                    val start = pos
                    advanceChar()
                    readStringContent()
                    eat('"')
                    start ..< pos to JsonTokenType.STRING
                }
                c in setOf(',', ':', '{', '}', '[', ']') -> {
                    val start = pos
                    advanceChar()
                    start .. start to JsonTokenType.SYMBOL
                }
                else -> throw ParseException("Unexpected character '$c'")
            }
            log.v { "readToken($start) = ${token.first} ${token.second}"}
            return TokenWithoutValue(token.first, token.second)
        }
        return TokenWithoutValue(pos .. pos, JsonTokenType.EOF)
    }

    /**
     * For testing use only
     */
    fun readAllTokens(): List<TokenWithoutValue<JsonTokenType>> {
        val result = mutableListOf<TokenWithoutValue<JsonTokenType>>()
        do {
            val t = readToken()
            result += t
        } while (t.type != JsonTokenType.EOF)
        return result
    }
}

data class NodeWithoutValue<T>(val range: IntRange, val type: T, val child: NodeWithoutValue<T>? = null, val childs: List<NodeWithoutValue<T>>? = null)

data class TokenWithoutValue<T>(val range: IntRange, val type: T)

enum class JsonTokenType {
    STRING, NUMBER, IDENTIFIER, SYMBOL, EOF
}

enum class JsonNodeType {
    OBJECT, LIST,
    OBJECT_KEY, STRING, NUMBER, TRUE, FALSE, NOTHING,
}

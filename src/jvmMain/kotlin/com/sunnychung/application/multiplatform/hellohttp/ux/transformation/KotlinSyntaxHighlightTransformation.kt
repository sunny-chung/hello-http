package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.kotlite.lexer.Lexer
import com.sunnychung.lib.multiplatform.kotlite.model.Token
import com.sunnychung.lib.multiplatform.kotlite.model.TokenType

class KotlinSyntaxHighlightTransformation(private val colours: AppColor) : VisualTransformation {
    fun Token.toSpanStyleRange(style: SpanStyle): AnnotatedString.Range<SpanStyle> =
        AnnotatedString.Range(style, position.index, endExclusive.index)

    override fun filter(text: AnnotatedString): TransformedText {
        val styles = text.spanStyles.toMutableList()

        try { // TODO performance is poor, because lexer re-parses the text on every character change
            val lexer = Lexer(filename = "", code = text.text, isParseComment = true)
            val curlyBracketLevel = mutableListOf(0)
            while (true) {
                val token = lexer.readToken()
                if (token.type == TokenType.EOF) {
                    break
                }
                with (token) {
                    when (token.type) {
                        TokenType.Integer, TokenType.Long, TokenType.Double ->
                            styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.numberLiteral))
                        TokenType.Char -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.objectKey))
                        TokenType.Operator, TokenType.Symbol -> {
                            if (value == "'") {
                                styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.objectKey))
                            } else if (value in setOf("\"", "\"\"\"")) {
                                styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.stringLiteral))
                                if (value == "\"") {
                                    when (lexer.currentMode()) {
                                        Lexer.Mode.Main -> {
                                            lexer.switchToMode(Lexer.Mode.QuotedString)
                                            curlyBracketLevel += 0
                                        }
                                        Lexer.Mode.QuotedString -> {
                                            lexer.switchToPreviousMode()
                                            curlyBracketLevel.removeLast()
                                        }
                                        Lexer.Mode.MultilineString -> {}
                                    }
                                } else if (value == "\"\"\"") {
                                    when (lexer.currentMode()) {
                                        Lexer.Mode.Main -> {
                                            lexer.switchToMode(Lexer.Mode.MultilineString)
                                            curlyBracketLevel += 0
                                        }
                                        Lexer.Mode.QuotedString -> {}
                                        Lexer.Mode.MultilineString -> {
                                            lexer.switchToPreviousMode()
                                            curlyBracketLevel.removeLast()
                                        }
                                    }
                                }
                            }
                            if (lexer.currentMode() == Lexer.Mode.Main) {
                                if (value == "{") {
                                    ++curlyBracketLevel[curlyBracketLevel.lastIndex]
                                } else if (value == "}") {
                                    if (--curlyBracketLevel[curlyBracketLevel.lastIndex] < 0) {
                                        lexer.switchToPreviousMode()
                                        curlyBracketLevel.removeLast()
                                        styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.variable))
                                    }
                                }
                            } else if (lexer.currentMode() in setOf(Lexer.Mode.QuotedString, Lexer.Mode.MultilineString)) {
                                if (value in setOf("\${", "\$")) {
                                    styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.variable))
                                }
                                if (value == "\${") {
                                    lexer.switchToMode(Lexer.Mode.Main)
                                    curlyBracketLevel += 0
                                }
                            }
                        }
                        TokenType.Identifier -> {
                            when (value) {
                                "true" -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.booleanTrueLiteral))
                                "false" -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.booleanFalseLiteral))
                                "null" -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.nothingLiteral))
                                "get", "set", "class", "interface", "fun", "val", "var", "constructor", "init", "this", "super", "if", "else", "when", "try", "catch", "finally", "for", "do", "while", "throw", "return", "continue", "break", "as", "is", "in", "enum", "operator", "infix", "override", "abstract", "open", "vararg" ->
                                    styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.keyword, fontWeight = FontWeight.Bold))
                                "it" ->
                                    styles += toSpanStyleRange(SpanStyle(fontWeight = FontWeight.Bold))
                            }
                        }
                        TokenType.StringLiteral -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.stringLiteral))
                        TokenType.StringFieldIdentifier -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.variable))
                        TokenType.NewLine -> {}
                        TokenType.Semicolon -> {}
                        TokenType.Comment -> styles += toSpanStyleRange(SpanStyle(color = colours.syntaxColor.comment))
                        TokenType.EOF -> return@with
                        TokenType.Unknown -> {}
                    }
                }
            }
        } catch (e: Throwable) {
            // syntax highlight is an optional feature and should not make anything else crash
        }
        return TransformedText(text = AnnotatedString(text = text.text, spanStyles = styles), offsetMapping = OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinSyntaxHighlightTransformation) return false

        if (colours !== other.colours) return false

        return true
    }

    override fun hashCode(): Int {
        return colours.hashCode()
    }
}

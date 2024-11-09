package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

private val TOKEN_REGEX = "(?<!\\\\)(\".*?(?<!\\\\)\"(?:\\s*:)?)|(?<=[,\\[\\]{}:])\\s*([^,\\s\"\\[\\]{}]+?)\\s*(?=[,\\[\\]{}:])".toRegex()

private val OBJECT_KEY_REGEX = "(\".*?(?<!\\\\)\")\\s*:".toRegex()
private val STRING_LITERAL_REGEX = "\".*?(?<!\\\\)\"".toRegex()
private val NUMBER_LITERAL_REGEX = "-?\\d+(?:\\.\\d+)?".toRegex()
private val BOOLEAN_TRUE_LITERAL_REGEX = "true".toRegex()
private val BOOLEAN_FALSE_LITERAL_REGEX = "false".toRegex()
private val NOTHING_LITERAL_REGEX = "null|undefined".toRegex()

class JsonSyntaxHighlightTransformation(val colours: AppColor) : VisualTransformation {

    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    val subPatterns = listOf(
        OBJECT_KEY_REGEX to objectKeyStyle,
        STRING_LITERAL_REGEX to stringLiteralStyle,
        NUMBER_LITERAL_REGEX to numberLiteralStyle,
        BOOLEAN_TRUE_LITERAL_REGEX to booleanTrueLiteralStyle,
        BOOLEAN_FALSE_LITERAL_REGEX to booleanFalseLiteralStyle,
        NOTHING_LITERAL_REGEX to nothingLiteralStyle,
    )

    var lastTextHash: Int? = null
    var lastResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        if (lastTextHash == text.text.hashCode() && lastResult != null) {
            return lastResult!!
        }

        val start = KInstant.now()

        TOKEN_REGEX.findAll(s).forEach { m ->
            val match = (m.groups[1] ?: m.groups[2])!!
            subPatterns.firstOrNull { (pattern, style) ->
                val subMatch = pattern.matchEntire(match.value)
                if (subMatch != null) {
                    val range = if (subMatch.groups.size > 1) {
                        subMatch.groups[1]!!.range
                            .let { it.start + match.range.start .. it.endInclusive + match.range.start }
                    } else {
                        match.range
                    }
                    spans += AnnotatedString.Range(style, range.start, range.endInclusive + 1)
                    true
                } else {
                    false
                }
            }
        }
        val timeCost = KInstant.now() - start
        // took 40ms for a 300k-length string
        log.d { "JsonSyntaxHighlightTransformation took ${timeCost.millis}ms to process ${s.length}" }

        lastTextHash = text.text.hashCode()
        lastResult = TransformedText(AnnotatedString(s, text.spanStyles + spans), OffsetMapping.Identity)
        return lastResult!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonSyntaxHighlightTransformation) return false

        if (colours !== other.colours) return false

        return true
    }

    override fun hashCode(): Int {
        return colours.hashCode()
    }
}

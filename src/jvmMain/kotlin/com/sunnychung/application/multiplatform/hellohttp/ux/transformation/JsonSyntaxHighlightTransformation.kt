package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

val OBJECT_KEY_REGEX = "(?<!\\\\)(\".*?(?<!\\\\)\")\\s*:".toRegex()
val STRING_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(?<!\\\\)(\".*?(?<!\\\\)\")".toRegex()
val NUMBER_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)\\b".toRegex()
val BOOLEAN_TRUE_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(true)\\b".toRegex()
val BOOLEAN_FALSE_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(false)\\b".toRegex()
val NOTHING_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(null|undefined)\\b".toRegex()

class JsonSyntaxHighlightTransformation(val colours: AppColor) : VisualTransformation {

    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    var lastTextHash: Int? = null
    var lastResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        if (lastTextHash == text.text.hashCode() && lastResult != null) {
            return lastResult!!
        }

        val start = KInstant.now()
        listOf(
            OBJECT_KEY_REGEX to objectKeyStyle,
            STRING_LITERAL_REGEX to stringLiteralStyle,
            NUMBER_LITERAL_REGEX to numberLiteralStyle,
            BOOLEAN_TRUE_LITERAL_REGEX to booleanTrueLiteralStyle,
            BOOLEAN_FALSE_LITERAL_REGEX to booleanFalseLiteralStyle,
            NOTHING_LITERAL_REGEX to nothingLiteralStyle,
        ).forEach { (regex, style) ->
            regex.findAll(s).forEach { m ->
                val range = m.groups[1]!!.range
                spans += AnnotatedString.Range(style, range.start, range.endInclusive + 1)
            }
        }
        val timeCost = KInstant.now() - start
        // took 53ms for a 300k-length string
        log.d { "JsonSyntaxHighlightTransformation took ${timeCost.millis}ms to process ${s.length}" }

        lastTextHash = text.text.hashCode()
        lastResult = TransformedText(AnnotatedString(s, text.spanStyles + spans), OffsetMapping.Identity)
        return lastResult!!
    }
}

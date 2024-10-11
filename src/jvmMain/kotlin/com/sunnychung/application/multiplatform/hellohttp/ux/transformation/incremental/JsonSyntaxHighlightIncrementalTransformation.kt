package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.IncrementalTextTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor

private val TOKEN_REGEX = "(?<!\\\\)(\".*?(?<!\\\\)\"(?:\\s*:)?)|(?<=[,\\[\\]{}:])\\s*([^,\\s\"\\[\\]{}]+?)\\s*(?=[,\\[\\]{}:])".toRegex()

private val OBJECT_KEY_REGEX = "(\".*?(?<!\\\\)\")\\s*:".toRegex()
private val STRING_LITERAL_REGEX = "\".*?(?<!\\\\)\"".toRegex()
private val NUMBER_LITERAL_REGEX = "-?\\d+(?:\\.\\d+)?".toRegex()
private val BOOLEAN_TRUE_LITERAL_REGEX = "true".toRegex()
private val BOOLEAN_FALSE_LITERAL_REGEX = "false".toRegex()
private val NOTHING_LITERAL_REGEX = "null|undefined".toRegex()

class JsonSyntaxHighlightIncrementalTransformation(val colours: AppColor) : IncrementalTextTransformation<Unit> {
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

    override fun initialize(text: BigText, transformer: BigTextTransformer) {
        val s = text.buildString()
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
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

        if (spans.isNotEmpty()) {
            transformer.replace(
                range = 0 until text.length,
                text = AnnotatedString(s, spans),
                offsetMapping = BigTextTransformOffsetMapping.Incremental
            )
        }
    }

    override fun onTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {

    }


}

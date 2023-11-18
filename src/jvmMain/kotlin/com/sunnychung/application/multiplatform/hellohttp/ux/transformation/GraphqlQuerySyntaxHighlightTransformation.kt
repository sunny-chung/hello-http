package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.kdatetime.KInstant

private val ROI_REGEX = "#[^\\n]*|\".*(?!<\\\\)\"|\\b(?:query|mutation|subscription|fragment|on)\\s+[^\\s()]+?\\b|\\\$[^\\s()]+\\s*:\\s*[^$\\s()]+(?=[),])|\\.\\.\\.\\s*[^\\s]+?\\b|(?:\\b|\\\$|@)[^\\s()]+?\\b(?:\\s*:)?".toRegex()

private val COMMENT_REGEX = "#[^\\n]*".toRegex()
private val STRING_LITERAL_REGEX = "\".*(?!<\\\\)\"".toRegex()
private val OPERATION_OR_FRAGMENT_DECLARATION_REGEX = "\\b(query|mutation|subscription|fragment|on)(?:\\s+([^\\s()]+?))?\\b".toRegex()
private val VARIABLE_DECLARATION_REGEX = "(\\\$[^\\s()]+)\\s*:\\s*([^\$\\s()]+)".toRegex()
private val FRAGMENT_REFERENCE_REGEX = "\\.\\.\\.\\s*([^\\s]+?)\\b".toRegex()
private val VARIABLE_REGEX = "\\\$[^\\s()]+".toRegex()
private val DIRECTIVE_REGEX = "@[^\\s()]+".toRegex()
private val OBJECT_KEY_REGEX = "([^\\s()]+)\\s*:".toRegex()
private val NUMBER_LITERAL_REGEX = "-?\\d+(?:\\.\\d+)?".toRegex()
private val BOOLEAN_TRUE_LITERAL_REGEX = "true".toRegex()
private val BOOLEAN_FALSE_LITERAL_REGEX = "false".toRegex()
private val NOTHING_LITERAL_REGEX = "null|undefined".toRegex()
private val FIELD_REGEX = "[^\\s()]+".toRegex() // TODO handle keywords such as "true" as field names

class GraphqlQuerySyntaxHighlightTransformation(val colours: AppColor) : VisualTransformation {

    private val SUBPATTERNS = listOf(
        COMMENT_REGEX to listOf(SpanStyle(color = colours.syntaxColor.comment, fontStyle = FontStyle.Italic)),
        STRING_LITERAL_REGEX to listOf(SpanStyle(color = colours.syntaxColor.stringLiteral)),
        OPERATION_OR_FRAGMENT_DECLARATION_REGEX to listOf(
            SpanStyle(color = colours.syntaxColor.keyword, fontWeight = FontWeight.Bold),
            SpanStyle(color = colours.syntaxColor.otherName)
        ),
        VARIABLE_DECLARATION_REGEX to listOf(
            SpanStyle(color = colours.syntaxColor.variable),
            SpanStyle(color = colours.syntaxColor.type),
        ),
        FRAGMENT_REFERENCE_REGEX to listOf(SpanStyle(color = colours.syntaxColor.otherName)),
        VARIABLE_REGEX to listOf(SpanStyle(color = colours.syntaxColor.variable)),
        DIRECTIVE_REGEX to listOf(SpanStyle(color = colours.syntaxColor.directive, fontStyle = FontStyle.Italic)),
        OBJECT_KEY_REGEX to listOf(SpanStyle(color = colours.syntaxColor.objectKey)),
        NUMBER_LITERAL_REGEX to listOf(SpanStyle(color = colours.syntaxColor.numberLiteral)),
        BOOLEAN_TRUE_LITERAL_REGEX to listOf(SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)),
        BOOLEAN_FALSE_LITERAL_REGEX to listOf(SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)),
        NOTHING_LITERAL_REGEX to listOf(SpanStyle(color = colours.syntaxColor.nothingLiteral)),
        FIELD_REGEX to listOf(SpanStyle(color = colours.syntaxColor.field)),
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        val start = KInstant.now()
        ROI_REGEX.findAll(s).forEach { match ->
            log.v { "Match: ${match.value}" }
            SUBPATTERNS.firstOrNull { (pattern, styles) ->
                val subMatch = pattern.matchEntire(match.value)
                if (subMatch != null) {
                    log.v { "subMatch: $pattern" }
                    styles.forEachIndexed { styleIndex, spanStyle ->
                        val range = if (subMatch.groups.size > 1 + styleIndex) {
                            subMatch.groups[1 + styleIndex]?.range
                                ?.let { it.start + match.range.start .. it.endInclusive + match.range.start }
                                ?: return@forEachIndexed
                        } else {
                            match.range
                        }
                        spans += AnnotatedString.Range(spanStyle, range.start, range.endInclusive + 1)
                    }
                    true
                } else {
                    false
                }
            }
        }
        val timeCost = KInstant.now() - start
        log.d { "GraphqlQuerySyntaxHighlightTransformation took ${timeCost.millis}ms to process ${s.length}" }

        return TransformedText(AnnotatedString(s, text.spanStyles + spans), OffsetMapping.Identity)
    }

}

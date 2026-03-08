package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.MarkdownInlineCustomDecorationType
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseMarkdownCustomizedAst
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.safeText
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.typeName
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.walkDepthFirst
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextChangeEvent
import com.sunnychung.lib.multiplatform.bigtext.core.CacheableBigTextDecorator

class MarkdownSyntaxHighlightDecorator(private val colours: AppColor) : CacheableBigTextDecorator() {

    private val headingStyle = SpanStyle(
        color = colours.syntaxColor.keyword,
        fontWeight = FontWeight.Bold,
    )
    private val emphasisStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val strongStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val strikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
    private val codeStyle = SpanStyle(
        color = colours.syntaxColor.stringLiteral,
        background = colours.backgroundLight.copy(alpha = 0.5f),
    )
    private val linkStyle = SpanStyle(
        color = colours.syntaxColor.objectKey,
        textDecoration = TextDecoration.Underline,
    )
    private val quoteStyle = SpanStyle(color = colours.syntaxColor.comment)
    private val listStyle = SpanStyle(color = colours.syntaxColor.directive)
    private val tableStyle = SpanStyle(color = colours.syntaxColor.type)
    private val dividerStyle = SpanStyle(color = colours.unimportant)
    private var transformedTextCache = AnnotatedString("")

    override fun doInitialize(text: BigText) {
        transformedTextCache = buildHighlightedTextSafely(text.buildString())
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        transformedTextCache = buildHighlightedTextSafely(change.bigText.buildString())
    }

    private fun buildHighlightedTextSafely(source: String): AnnotatedString {
        return try {
            buildHighlightedText(source)
        } catch (_: Throwable) {
            AnnotatedString(source)
        }
    }

    private fun buildHighlightedText(source: String): AnnotatedString {
        if (source.isEmpty()) {
            return AnnotatedString("")
        }

        val parsedAst = parseMarkdownCustomizedAst(source)
        val root = parsedAst.root
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        fun addRange(start: Int, endExclusive: Int, style: SpanStyle) {
            val boundedStart = start.coerceIn(0, source.length)
            val boundedEnd = endExclusive.coerceIn(boundedStart, source.length)
            if (boundedEnd > boundedStart) {
                spans += AnnotatedString.Range(
                    item = style,
                    start = boundedStart,
                    end = boundedEnd,
                )
            }
        }

        parsedAst.inlineCustomDecorations.forEach { decoration ->
            val style = when (decoration.type) {
                MarkdownInlineCustomDecorationType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
                MarkdownInlineCustomDecorationType.Strikethrough -> strikeStyle
            }
            addRange(decoration.start, decoration.endExclusive, style)
        }

        root.walkDepthFirst { node ->
            val typeName = node.typeName()
            val start = node.startOffset
            val end = node.endOffset
            when {
                typeName.startsWith("ATX_") || typeName.startsWith("SETEXT_") -> addRange(start, end, headingStyle)
                typeName == "STRONG" -> {
                    val strongText = node.safeText(source).trim()
                    addRange(
                        start,
                        end,
                        if (strongText.startsWith("__") && strongText.endsWith("__")) {
                            SpanStyle(textDecoration = TextDecoration.Underline)
                        } else {
                            strongStyle
                        }
                    )
                }
                typeName == "EMPH" -> addRange(start, end, emphasisStyle)
                typeName == "STRIKETHROUGH" -> addRange(start, end, strikeStyle)
                typeName == "CODE_SPAN" || typeName == "CODE_FENCE_CONTENT" || typeName == "CODE_LINE" -> addRange(start, end, codeStyle)
                typeName == "INLINE_LINK" || typeName == "GFM_AUTOLINK" -> addRange(start, end, linkStyle)
                typeName == "BLOCK_QUOTE" -> addRange(start, end, quoteStyle)
                typeName == "UNORDERED_LIST" || typeName == "ORDERED_LIST" || typeName == "LIST_BULLET" || typeName == "LIST_NUMBER" ->
                    addRange(start, end, listStyle)
                typeName == "TABLE" || typeName == "HEADER" || typeName == "ROW" || typeName == "CELL" ->
                    addRange(start, end, tableStyle)
                typeName == "HORIZONTAL_RULE" -> addRange(start, end, dividerStyle)
            }
        }

        return AnnotatedString(
            text = source,
            spanStyles = spans,
        )
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        return try {
            val spans = (transformedTextCache.subSequence(originalRange) as AnnotatedString).spanStyles
            AnnotatedString(text.string(), spans)
        } catch (_: Throwable) {
            buildHighlightedTextSafely(text.string())
        }
    }
}

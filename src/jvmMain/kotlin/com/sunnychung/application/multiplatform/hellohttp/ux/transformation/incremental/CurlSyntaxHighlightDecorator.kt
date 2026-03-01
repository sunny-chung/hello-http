package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextChangeEvent
import com.sunnychung.lib.multiplatform.bigtext.core.CacheableBigTextDecorator

/**
 * Lightweight syntax highlighting for shell-style cURL commands.
 *
 * Parsing/highlighting is best-effort only. Any failure must not break editing/import.
 */
class CurlSyntaxHighlightDecorator(colours: AppColor) : CacheableBigTextDecorator() {

    private val commentStyle = SpanStyle(color = colours.syntaxColor.comment, fontStyle = FontStyle.Italic)
    private val commandStyle = SpanStyle(color = colours.syntaxColor.keyword, fontWeight = FontWeight.Bold)
    private val timingCommandStyle = SpanStyle(
        color = colours.syntaxColor.keyword,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    )
    private val optionStyle = SpanStyle(color = colours.syntaxColor.directive, fontStyle = FontStyle.Italic)
    private val stringStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    private val variableStyle = SpanStyle(color = colours.syntaxColor.variable)
    private val urlStyle = SpanStyle(color = colours.syntaxColor.objectKey)

    private val longOptionRegex = Regex("""(?<!\S)--[A-Za-z0-9][A-Za-z0-9-]*""")
    private val shortOptionRegex = Regex("""(?<!\S)-(?!-)[A-Za-z](?:[A-Za-z]+)?""")
    private val variableRegex = Regex("""\$\{[^}\n]+\}|\$[A-Za-z_][A-Za-z0-9_]*""")
    private val singleQuotedRegex = Regex("""'(?:[^'\\]|\\.)*'""")
    private val doubleQuotedRegex = Regex(""""(?:[^"\\]|\\.)*"""")
    private val urlRegex = Regex("""https?://[^\s'"]+""")
    private val commentRegex = Regex("""(?m)(^|[\s;&|()])#.*$""")
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
            // Syntax highlighting is optional and should never block import UX.
            AnnotatedString(source)
        }
    }

    private fun buildHighlightedText(source: String): AnnotatedString {
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        fun addCommandMatches() {
            var i = 0
            while (i < source.length) {
                val commandStart = findCommandStart(source, i) ?: break
                val firstToken = readToken(source, commandStart)
                if (firstToken != null) {
                    when (firstToken.text) {
                        "curl" -> {
                            spans += AnnotatedString.Range(
                                item = commandStyle,
                                start = firstToken.start,
                                end = firstToken.endExclusive,
                            )
                        }

                        "time" -> {
                            spans += AnnotatedString.Range(
                                item = timingCommandStyle,
                                start = firstToken.start,
                                end = firstToken.endExclusive,
                            )
                            val secondTokenStart = skipShellSpacesAndContinuations(source, firstToken.endExclusive)
                            val secondToken = readToken(source, secondTokenStart)
                            if (secondToken?.text == "curl") {
                                spans += AnnotatedString.Range(
                                    item = commandStyle,
                                    start = secondToken.start,
                                    end = secondToken.endExclusive,
                                )
                            }
                        }
                    }
                }
                i = commandStart + 1
            }
        }

        fun addMatches(regex: Regex, style: SpanStyle, groupName: String? = null) {
            regex.findAll(source).forEach { match ->
                val range = if (groupName != null) {
                    match.groups[groupName]?.range
                } else {
                    match.range
                } ?: return@forEach
                if (range.first < 0 || range.last >= source.length || range.isEmpty()) {
                    return@forEach
                }
                spans += AnnotatedString.Range(
                    item = style,
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }

        addCommandMatches()
        addMatches(longOptionRegex, optionStyle)
        addMatches(shortOptionRegex, optionStyle)
        addMatches(variableRegex, variableStyle)
        addMatches(singleQuotedRegex, stringStyle)
        addMatches(doubleQuotedRegex, stringStyle)
        addMatches(urlRegex, urlStyle)
        addMatches(commentRegex, commentStyle)

        return AnnotatedString(
            text = source,
            spanStyles = spans,
        )
    }

    private data class TokenRange(val text: String, val start: Int, val endExclusive: Int)

    private fun findCommandStart(source: String, fromIndex: Int): Int? {
        var i = fromIndex
        while (i < source.length) {
            when {
                i == 0 -> return skipInlineWhitespace(source, i)

                source[i - 1] in ";|&()" -> return skipInlineWhitespace(source, i)

                source[i - 1] == '\n' -> {
                    if (!isContinuedLine(source, i)) {
                        return skipInlineWhitespace(source, i)
                    }
                }
            }
            i += 1
        }
        return null
    }

    private fun skipInlineWhitespace(source: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < source.length && (source[i] == ' ' || source[i] == '\t' || source[i] == '\r')) {
            i += 1
        }
        return i
    }

    private fun skipShellSpacesAndContinuations(source: String, fromIndex: Int): Int {
        var i = fromIndex
        while (i < source.length) {
            while (i < source.length && (source[i] == ' ' || source[i] == '\t' || source[i] == '\r')) {
                i += 1
            }
            if (i < source.length && source[i] == '\\') {
                val newlineStart = i + 1
                if (newlineStart < source.length && source[newlineStart] == '\n') {
                    i = newlineStart + 1
                    continue
                }
            }
            return i
        }
        return i
    }

    private fun readToken(source: String, start: Int): TokenRange? {
        if (start < 0 || start >= source.length) {
            return null
        }
        val c = source[start]
        if (!c.isLetterOrDigit() && c != '_' && c != '-') {
            return null
        }
        var end = start + 1
        while (end < source.length) {
            val ch = source[end]
            if (!ch.isLetterOrDigit() && ch != '_' && ch != '-') {
                break
            }
            end += 1
        }
        return TokenRange(
            text = source.substring(start, end),
            start = start,
            endExclusive = end,
        )
    }

    private fun isContinuedLine(source: String, lineStartIndex: Int): Boolean {
        val lineEndExclusive = (lineStartIndex - 1).coerceAtLeast(0)
        var i = lineEndExclusive - 1
        while (i >= 0 && (source[i] == ' ' || source[i] == '\t' || source[i] == '\r')) {
            i -= 1
        }
        return i >= 0 && source[i] == '\\'
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        val source = text.string()
        return try {
            // BigText may request decoration on segmented ranges where a token starts outside the range.
            // Keep a full-text cache and slice styles from it, otherwise options like "--request" lose prefix "-".
            val spanStyles = (transformedTextCache.subSequence(originalRange) as AnnotatedString).spanStyles
            AnnotatedString(source, spanStyles)
        } catch (_: Throwable) {
            AnnotatedString(source)
        }
    }
}

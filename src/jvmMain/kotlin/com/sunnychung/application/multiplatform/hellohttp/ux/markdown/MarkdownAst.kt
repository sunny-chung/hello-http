package com.sunnychung.application.multiplatform.hellohttp.ux.markdown

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

private val markdownFlavour = GFMFlavourDescriptor()
private val codeContextNodeTypes = setOf(
    "CODE_SPAN",
    "CODE_BLOCK",
    "CODE_FENCE",
    "CODE_FENCE_CONTENT",
    "CODE_LINE",
)
private val inlineTextNodeTypes = setOf("TEXT", "WHITE_SPACE", "EOL")
private val skipInlineCustomSyntaxNodeTypes = setOf("INLINE_LINK", "GFM_AUTOLINK", "IMAGE")

enum class MarkdownInlineCustomDecorationType {
    Underline,
    Strikethrough,
}

data class MarkdownInlineCustomDecoration(
    val start: Int,
    val endExclusive: Int,
    val type: MarkdownInlineCustomDecorationType,
)

data class MarkdownInlineTextSegment(
    val text: String,
    val decorationType: MarkdownInlineCustomDecorationType? = null,
)

data class MarkdownInlineCustomParseResult(
    val segments: List<MarkdownInlineTextSegment>,
    val decorations: List<MarkdownInlineCustomDecoration>,
)

data class MarkdownCustomizedAst(
    val root: ASTNode,
    val inlineCustomDecorations: List<MarkdownInlineCustomDecoration>,
)

fun parseMarkdownTree(source: String): ASTNode {
    return MarkdownParser(markdownFlavour).buildMarkdownTreeFromString(source)
}

fun parseMarkdownCustomizedAst(source: String): MarkdownCustomizedAst {
    val root = parseMarkdownTree(source)
    return MarkdownCustomizedAst(
        root = root,
        inlineCustomDecorations = findInlineCustomDecorations(root = root, source = source),
    )
}

fun parseInlineTextWithCustomDecorations(
    text: String,
    baseOffset: Int = 0,
): MarkdownInlineCustomParseResult {
    if (text.isEmpty()) {
        return MarkdownInlineCustomParseResult(
            segments = emptyList(),
            decorations = emptyList(),
        )
    }

    val segments = mutableListOf<MarkdownInlineTextSegment>()
    val decorations = mutableListOf<MarkdownInlineCustomDecoration>()
    var cursor = 0

    while (cursor < text.length) {
        val nextMarker = findNextCustomMarker(text = text, startIndex = cursor)
        if (nextMarker == null) {
            val remaining = text.substring(cursor)
            if (remaining.isNotEmpty()) {
                segments += MarkdownInlineTextSegment(text = remaining)
            }
            break
        }

        val (markerStart, marker) = nextMarker
        if (markerStart > cursor) {
            segments += MarkdownInlineTextSegment(text = text.substring(cursor, markerStart))
        }

        val markerLength = marker.length
        val contentStart = markerStart + markerLength
        val closingStart = text.indexOf(marker, startIndex = contentStart)
        if (closingStart < contentStart) {
            segments += MarkdownInlineTextSegment(text = marker)
            cursor = contentStart
            continue
        }

        val content = text.substring(contentStart, closingStart)
        if (content.isEmpty() || content.contains('\n')) {
            segments += MarkdownInlineTextSegment(text = marker)
            cursor = contentStart
            continue
        }

        val decorationType = if (marker == "~~") {
            MarkdownInlineCustomDecorationType.Strikethrough
        } else {
            MarkdownInlineCustomDecorationType.Underline
        }
        segments += MarkdownInlineTextSegment(
            text = content,
            decorationType = decorationType,
        )
        decorations += MarkdownInlineCustomDecoration(
            start = baseOffset + contentStart,
            endExclusive = baseOffset + closingStart,
            type = decorationType,
        )

        cursor = closingStart + markerLength
    }

    return MarkdownInlineCustomParseResult(
        segments = segments,
        decorations = decorations,
    )
}

fun findInlineCustomDecorations(root: ASTNode, source: String): List<MarkdownInlineCustomDecoration> {
    val decorations = mutableListOf<MarkdownInlineCustomDecoration>()

    fun walk(node: ASTNode, isCodeContext: Boolean) {
        val nodeType = node.typeName()
        val nextIsCodeContext = isCodeContext || nodeType in codeContextNodeTypes
        if (!nextIsCodeContext) {
            when (nodeType) {
                "STRIKETHROUGH" -> findDelimitedInnerRange(
                    node = node,
                    source = source,
                    delimiter = "~~",
                )?.let {
                    decorations += MarkdownInlineCustomDecoration(
                        start = it.first,
                        endExclusive = it.second,
                        type = MarkdownInlineCustomDecorationType.Strikethrough,
                    )
                }

                "STRONG" -> {
                    val strongNodeText = node.safeText(source)
                    if (strongNodeText.startsWith("__") && strongNodeText.endsWith("__")) {
                        findDelimitedInnerRange(
                            node = node,
                            source = source,
                            delimiter = "__",
                        )?.let {
                            decorations += MarkdownInlineCustomDecoration(
                                start = it.first,
                                endExclusive = it.second,
                                type = MarkdownInlineCustomDecorationType.Underline,
                            )
                        }
                    }
                }
            }
        }
        if (
            !nextIsCodeContext &&
            node.children.isEmpty() &&
            nodeType in inlineTextNodeTypes
        ) {
            decorations += parseInlineTextWithCustomDecorations(
                text = node.safeText(source),
                baseOffset = node.startOffset,
            ).decorations
        }

        if (nodeType in skipInlineCustomSyntaxNodeTypes) {
            return
        }
        node.children.forEach { walk(node = it, isCodeContext = nextIsCodeContext) }
    }

    walk(node = root, isCodeContext = false)
    return decorations
}

private fun findDelimitedInnerRange(
    node: ASTNode,
    source: String,
    delimiter: String,
): Pair<Int, Int>? {
    val nodeText = node.safeText(source)
    if (nodeText.length <= delimiter.length * 2) {
        return null
    }
    if (!nodeText.startsWith(delimiter) || !nodeText.endsWith(delimiter)) {
        return null
    }
    val contentStart = node.startOffset + delimiter.length
    val contentEndExclusive = node.endOffset - delimiter.length
    if (contentEndExclusive <= contentStart) {
        return null
    }
    return contentStart to contentEndExclusive
}

private fun findNextCustomMarker(text: String, startIndex: Int): Pair<Int, String>? {
    val nextTildeMarkerStart = text.indexOf("~~", startIndex = startIndex).takeIf { it >= 0 }
    val nextUnderlineMarkerStart = text.indexOf("__", startIndex = startIndex).takeIf { it >= 0 }
    val markerStart = listOfNotNull(nextTildeMarkerStart, nextUnderlineMarkerStart).minOrNull() ?: return null
    return if (nextTildeMarkerStart == markerStart) {
        markerStart to "~~"
    } else {
        markerStart to "__"
    }
}

fun ASTNode.typeName(): String = type.toString().removePrefix("Markdown:")

fun ASTNode.safeText(source: String): String {
    val sourceLength = source.length
    if (sourceLength == 0) {
        return ""
    }
    val start = startOffset.coerceIn(0, sourceLength)
    val end = endOffset.coerceIn(start, sourceLength)
    return source.substring(start, end)
}

fun ASTNode.walkDepthFirst(visitor: (ASTNode) -> Unit) {
    visitor(this)
    children.forEach { it.walkDepthFirst(visitor) }
}

fun ASTNode.findFirstDescendant(predicate: (ASTNode) -> Boolean): ASTNode? {
    if (predicate(this)) {
        return this
    }
    children.forEach { child ->
        child.findFirstDescendant(predicate)?.let { return it }
    }
    return null
}

fun ASTNode.findAllDescendants(predicate: (ASTNode) -> Boolean): List<ASTNode> {
    val result = mutableListOf<ASTNode>()
    walkDepthFirst {
        if (predicate(it)) {
            result += it
        }
    }
    return result
}

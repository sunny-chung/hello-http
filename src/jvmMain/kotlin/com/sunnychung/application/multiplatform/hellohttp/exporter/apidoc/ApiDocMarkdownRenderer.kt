package com.sunnychung.application.multiplatform.hellohttp.exporter.apidoc

import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.MarkdownInlineCustomDecorationType
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.findAllDescendants
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.findFirstDescendant
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseInlineTextWithCustomDecorations
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseMarkdownCustomizedAst
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.safeText
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.typeName
import org.intellij.markdown.ast.ASTNode

internal class ApiDocMarkdownRenderer {

    fun render(markdownText: String): String {
        val parsedAst = runCatching { parseMarkdownCustomizedAst(markdownText) }.getOrNull()
            ?: return renderFallbackCodeBlock(markdownText)

        val blockNodes = parsedAst.root.children.filter {
            val typeName = it.typeName()
            typeName != "EOL" && typeName != "WHITE_SPACE"
        }
        if (blockNodes.isEmpty()) {
            return renderFallbackCodeBlock(markdownText)
        }

        val builder = StringBuilder()
        blockNodes.forEach { node ->
            builder.append(renderBlock(node = node, source = markdownText, listDepth = 0))
        }
        return builder.toString()
    }

    private fun renderBlock(node: ASTNode, source: String, listDepth: Int): String {
        return when (val nodeType = node.typeName()) {
            "PARAGRAPH" -> {
                val content = renderInline(nodes = node.children, source = source, isTrimContent = false)
                "<p class=\"md-paragraph\">$content</p>"
            }

            "ATX_1", "ATX_2", "ATX_3", "ATX_4", "ATX_5", "ATX_6", "SETEXT_1", "SETEXT_2" -> {
                val headingLevel = when {
                    nodeType.startsWith("ATX_") -> nodeType.removePrefix("ATX_").toIntOrNull() ?: 1
                    nodeType == "SETEXT_2" -> 2
                    else -> 1
                }.coerceIn(1, 6)
                val content = renderInline(nodes = node.children, source = source, isTrimContent = true)
                "<h$headingLevel class=\"md-heading md-h$headingLevel\">$content</h$headingLevel>"
            }

            "HORIZONTAL_RULE" -> "<hr class=\"md-hr\" />"

            "CODE_FENCE", "CODE_BLOCK" -> renderCodeBlock(node = node, source = source)

            "BLOCK_QUOTE" -> {
                val childBlocks = node.children.filter {
                    val typeName = it.typeName()
                    typeName != "EOL" &&
                        typeName != "WHITE_SPACE" &&
                        !(typeName == "BLOCK_QUOTE" && it.children.isEmpty())
                }
                if (childBlocks.isEmpty()) {
                    renderFallbackCodeBlock(node.safeText(source))
                } else {
                    "<blockquote class=\"md-blockquote\">" +
                        childBlocks.joinToString(separator = "") { child ->
                            renderBlock(node = child, source = source, listDepth = listDepth)
                        } +
                        "</blockquote>"
                }
            }

            "UNORDERED_LIST", "ORDERED_LIST" -> renderListBlock(
                node = node,
                source = source,
                isOrdered = nodeType == "ORDERED_LIST",
                listDepth = listDepth,
            )

            "TABLE" -> renderTable(node = node, source = source)

            else -> renderFallbackCodeBlock(node.safeText(source))
        }
    }

    private fun renderListBlock(
        node: ASTNode,
        source: String,
        isOrdered: Boolean,
        listDepth: Int,
    ): String {
        val items = node.children.filter { it.typeName() == "LIST_ITEM" }
        if (items.isEmpty()) {
            return renderFallbackCodeBlock(node.safeText(source))
        }

        val listTag = if (isOrdered) "ol" else "ul"
        val builder = StringBuilder("<$listTag class=\"md-list ${if (isOrdered) "md-ordered" else "md-unordered"} depth-$listDepth\">")
        items.forEach { item ->
            val childBlocks = item.children.filter {
                val typeName = it.typeName()
                typeName != "EOL" &&
                    typeName != "WHITE_SPACE" &&
                    typeName != "LIST_BULLET" &&
                    typeName != "LIST_NUMBER"
            }

            builder.append("<li class=\"md-list-item\">")
            if (childBlocks.isEmpty()) {
                builder.append(renderInline(nodes = item.children, source = source, isTrimContent = false))
            } else {
                childBlocks.forEach { child ->
                    builder.append(renderBlock(node = child, source = source, listDepth = listDepth + 1))
                }
            }
            builder.append("</li>")
        }
        builder.append("</$listTag>")
        return builder.toString()
    }

    private fun renderTable(node: ASTNode, source: String): String {
        val rows = node.children
            .filter { it.typeName() == "HEADER" || it.typeName() == "ROW" }
            .map { rowNode ->
                rowNode.children
                    .filter { it.typeName() == "CELL" }
                    .map { cell ->
                        renderInline(nodes = cell.children, source = source, isTrimContent = false)
                    }
            }

        if (rows.isEmpty() || rows.all { it.isEmpty() }) {
            return renderFallbackCodeBlock(node.safeText(source))
        }

        val builder = StringBuilder()
        builder.append("<div class=\"md-table-wrapper\">")
        builder.append("<table class=\"md-table\">")

        val headerRow = rows.first()
        builder.append("<thead><tr>")
        headerRow.forEach { cell ->
            builder.append("<th class=\"md-table-head\">$cell</th>")
        }
        builder.append("</tr></thead>")

        if (rows.size > 1) {
            builder.append("<tbody>")
            rows.drop(1).forEach { row ->
                builder.append("<tr>")
                row.forEach { cell ->
                    builder.append("<td class=\"md-table-cell\">$cell</td>")
                }
                builder.append("</tr>")
            }
            builder.append("</tbody>")
        }

        builder.append("</table>")
        builder.append("</div>")
        return builder.toString()
    }

    private fun renderCodeBlock(node: ASTNode, source: String): String {
        val language = node.findFirstDescendant { it.typeName() == "FENCE_LANG" }
            ?.safeText(source)
            ?.trim()
            .orEmpty()
        val codeLines = node.findAllDescendants {
            val typeName = it.typeName()
            typeName == "CODE_FENCE_CONTENT" || typeName == "CODE_LINE"
        }.map { it.safeText(source) }

        val code = if (codeLines.isNotEmpty()) {
            stripUnnecessaryIndent(codeLines.joinToString("\n"))
        } else {
            stripUnnecessaryIndent(node.safeText(source))
        }

        return buildString {
            append("<div class=\"md-code-block\">")
            if (language.isNotBlank()) {
                append("<div class=\"md-code-language\">")
                append(escapeHtml(language))
                append("</div>")
            }
            append("<pre class=\"md-code-pre\"><code class=\"md-code\">")
            append(escapeHtml(code))
            append("</code></pre>")
            append("</div>")
        }
    }

    private fun renderInline(nodes: List<ASTNode>, source: String, isTrimContent: Boolean): String {
        val builder = StringBuilder()
        var isHtmlUnderline = false
        var isHtmlStrike = false

        fun parseHtmlToggleTag(text: String): Boolean {
            return when (text.trim().lowercase()) {
                "<u>", "<ins>" -> {
                    isHtmlUnderline = true
                    true
                }

                "</u>", "</ins>" -> {
                    isHtmlUnderline = false
                    true
                }

                "<s>", "<strike>", "<del>" -> {
                    isHtmlStrike = true
                    true
                }

                "</s>", "</strike>", "</del>" -> {
                    isHtmlStrike = false
                    true
                }

                else -> false
            }
        }

        fun appendStyledText(
            text: String,
            customDecoration: MarkdownInlineCustomDecorationType? = null,
        ) {
            if (text.isEmpty()) return

            val normalizedText = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
            val classNames = buildList {
                if (isHtmlUnderline || customDecoration == MarkdownInlineCustomDecorationType.Underline) {
                    add("md-underline")
                }
                if (isHtmlStrike || customDecoration == MarkdownInlineCustomDecorationType.Strikethrough) {
                    add("md-strike")
                }
            }
            normalizedText.split('\n').forEachIndexed { index, segment ->
                if (index > 0) {
                    builder.append("<br class=\"md-br\" />")
                }
                if (segment.isEmpty()) {
                    return@forEachIndexed
                }
                val escaped = escapeHtml(segment)
                if (classNames.isEmpty()) {
                    builder.append(escaped)
                } else {
                    builder.append("<span class=\"")
                    builder.append(classNames.joinToString(separator = " "))
                    builder.append("\">")
                    builder.append(escaped)
                    builder.append("</span>")
                }
            }
        }

        fun appendNode(node: ASTNode) {
            val typeName = node.typeName()
            val rawNodeText = node.safeText(source)

            if (node.children.isEmpty() && parseHtmlToggleTag(rawNodeText)) {
                return
            }

            when {
                typeName == "IMAGE" -> {
                    val image = parseImage(node, source)
                    if (image != null) {
                        builder.append("<img class=\"md-inline-image\" src=\"")
                        builder.append(escapeHtmlAttribute(image.dataUrl))
                        builder.append("\" alt=\"")
                        builder.append(escapeHtmlAttribute(image.altText))
                        builder.append("\" />")
                    } else {
                        appendStyledText(rawNodeText)
                    }
                }

                typeName == "INLINE_LINK" -> {
                    val label = node.findFirstDescendant { it.typeName() == "LINK_TEXT" }
                        ?.safeText(source)
                        ?.removePrefix("[")
                        ?.removeSuffix("]")
                        ?: rawNodeText
                    val url = node.findFirstDescendant { it.typeName() == "LINK_DESTINATION" }
                        ?.safeText(source)
                        ?.trim()
                    if (url.isNullOrBlank()) {
                        appendStyledText(label)
                    } else {
                        builder.append("<a class=\"md-link\" data-external-link=\"true\" href=\"")
                        builder.append(escapeHtmlAttribute(url))
                        builder.append("\">")
                        appendStyledText(label)
                        builder.append("</a>")
                    }
                }

                typeName == "GFM_AUTOLINK" -> {
                    val url = rawNodeText.trim()
                    if (url.isBlank()) {
                        appendStyledText(url)
                    } else {
                        builder.append("<a class=\"md-link\" data-external-link=\"true\" href=\"")
                        builder.append(escapeHtmlAttribute(url))
                        builder.append("\">")
                        appendStyledText(url)
                        builder.append("</a>")
                    }
                }

                typeName == "STRONG" -> {
                    val strongText = rawNodeText.trim()
                    val openTag = if (strongText.startsWith("__") && strongText.endsWith("__")) {
                        "<span class=\"md-underline\">"
                    } else {
                        "<strong class=\"md-strong\">"
                    }
                    builder.append(openTag)
                    node.children.forEach { appendNode(it) }
                    builder.append(if (openTag.startsWith("<strong")) "</strong>" else "</span>")
                }

                typeName == "EMPH" && node.children.isNotEmpty() -> {
                    builder.append("<em class=\"md-emph\">")
                    node.children.forEach { appendNode(it) }
                    builder.append("</em>")
                }

                typeName == "STRIKETHROUGH" -> {
                    builder.append("<span class=\"md-strike\">")
                    node.children.forEach { appendNode(it) }
                    builder.append("</span>")
                }

                typeName == "CODE_SPAN" -> {
                    val code = node.findFirstDescendant { it.typeName() == "TEXT" }
                        ?.safeText(source)
                        ?: rawNodeText.trim('`')
                    builder.append("<code class=\"md-inline-code\">")
                    builder.append(escapeHtml(code))
                    builder.append("</code>")
                }

                typeName == "TEXT" || typeName == "WHITE_SPACE" || typeName == "EOL" -> {
                    parseInlineTextWithCustomDecorations(
                        text = rawNodeText,
                        baseOffset = node.startOffset,
                    ).segments.forEach { segment ->
                        appendStyledText(
                            text = segment.text,
                            customDecoration = segment.decorationType,
                        )
                    }
                }

                shouldSkipInlineNode(node) -> Unit

                node.children.isNotEmpty() -> node.children.forEach { appendNode(it) }

                else -> appendStyledText(rawNodeText)
            }
        }

        nodes.forEach { appendNode(it) }
        val content = builder.toString()
        return if (isTrimContent) content.trim() else content
    }

    private fun parseImage(node: ASTNode, source: String): MarkdownImageData? {
        val linkNode = node.findFirstDescendant { it.typeName() == "INLINE_LINK" } ?: return null
        val destination = linkNode.findFirstDescendant { it.typeName() == "LINK_DESTINATION" }
            ?.safeText(source)
            ?.trim()
            ?: return null
        val alt = linkNode.findFirstDescendant { it.typeName() == "LINK_TEXT" }
            ?.safeText(source)
            ?.removePrefix("[")
            ?.removeSuffix("]")
            .orEmpty()
        if (!base64ImageRegex.matches(destination)) {
            return null
        }
        return MarkdownImageData(
            altText = alt,
            dataUrl = destination,
        )
    }

    private fun renderFallbackCodeBlock(text: String): String {
        return "<pre class=\"md-code-fallback\"><code>${escapeHtml(stripUnnecessaryIndent(text))}</code></pre>"
    }

    private fun shouldSkipInlineNode(node: ASTNode): Boolean {
        val typeName = node.typeName()
        if (node.children.isNotEmpty()) {
            return false
        }
        if (typeName == "EMPH") {
            return true
        }
        return typeName in setOf(
            "!",
            "[",
            "]",
            "(",
            ")",
            "~",
            "BACKTICK",
            "ATX_HEADER",
            "LIST_BULLET",
            "LIST_NUMBER",
            "TABLE_SEPARATOR",
            "BLOCK_QUOTE",
        )
    }

    private fun escapeHtml(value: String): String {
        val builder = StringBuilder(value.length)
        value.forEach { ch ->
            when (ch) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                '\'' -> builder.append("&#39;")
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun escapeHtmlAttribute(value: String): String = escapeHtml(value)

    private fun stripUnnecessaryIndent(text: String): String {
        val lines = text.split("\n")
        if (lines.size <= 1) {
            return text
        }
        val nonBlankTailLines = lines.drop(1).filter { it.isNotBlank() }
        if (nonBlankTailLines.isEmpty()) {
            return text
        }
        val minTailIndent = nonBlankTailLines.minOf { line ->
            line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
        }
        if (minTailIndent <= 0) {
            return text
        }
        return buildList {
            add(lines.first())
            lines.drop(1).forEach { line ->
                add(if (line.length >= minTailIndent) line.drop(minTailIndent) else line.trimStart())
            }
        }.joinToString("\n")
    }

    private data class MarkdownImageData(
        val altText: String,
        val dataUrl: String,
    )

    companion object {
        private val base64ImageRegex =
            Regex("^data:image/[A-Za-z0-9.+-]+;base64,([A-Za-z0-9+/=\\r\\n]+)$")
    }
}

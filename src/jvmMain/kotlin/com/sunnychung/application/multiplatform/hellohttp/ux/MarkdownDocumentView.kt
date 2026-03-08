package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.MarkdownInlineCustomDecorationType
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.findAllDescendants
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.findFirstDescendant
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseInlineTextWithCustomDecorations
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseMarkdownCustomizedAst
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.safeText
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.typeName
import org.intellij.markdown.ast.ASTNode
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

private const val MarkdownLinkAnnotationTag = "markdown_link"
private val Base64ImageRegex = Regex("^data:image/[A-Za-z0-9.+-]+;base64,([A-Za-z0-9+/=\\r\\n]+)$")
private const val InlineImageAlternateText = "\u200B"

private data class MarkdownImageData(
    val altText: String,
    val dataUrl: String,
)

private data class InlineRenderResult(
    val text: AnnotatedString,
    val inlineImages: Map<String, MarkdownImageData>,
)

@Composable
fun MarkdownDocumentView(
    modifier: Modifier = Modifier,
    markdownText: String,
    onClickLink: (String) -> Unit,
) {
    val colors = LocalColor.current
    val scrollState = rememberScrollState()
    val parsedAst = remember(markdownText) {
        runCatching { parseMarkdownCustomizedAst(markdownText) }.getOrNull()
    }

    Box(
        modifier = modifier
            .border(width = 1.dp, color = colors.line)
            .background(colors.backgroundInputField)
            .padding(8.dp)
    ) {
        if (parsedAst == null) {
            FallbackCodeBlock(text = markdownText)
            return@Box
        }

        val blockNodes = parsedAst.root.children.filter {
            val typeName = it.typeName()
            typeName != "EOL" && typeName != "WHITE_SPACE"
        }

        SelectionContainer {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (blockNodes.isEmpty()) {
                        FallbackCodeBlock(text = markdownText)
                    } else {
                        blockNodes.forEach {
                            MarkdownBlockView(
                                node = it,
                                source = markdownText,
                                listDepth = 0,
                                onClickLink = onClickLink,
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(scrollState),
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlockView(
    node: ASTNode,
    source: String,
    listDepth: Int,
    onClickLink: (String) -> Unit,
) {
    val colors = LocalColor.current
    val fonts = LocalFont.current
    when (val nodeType = node.typeName()) {
        "PARAGRAPH" -> MarkdownInlineParagraph(
            nodes = node.children,
            source = source,
            style = TextStyle(
                color = colors.text,
                fontSize = fonts.bodyFontSize,
                fontFamily = fonts.normalFontFamily,
                lineHeight = 22.sp,
            ),
            onClickLink = onClickLink,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        )

        "ATX_1", "ATX_2", "ATX_3", "ATX_4", "ATX_5", "ATX_6", "SETEXT_1", "SETEXT_2" -> {
            val headingLevel = when {
                nodeType.startsWith("ATX_") -> nodeType.removePrefix("ATX_").toIntOrNull() ?: 1
                nodeType == "SETEXT_2" -> 2
                else -> 1
            }.coerceIn(1, 6)
            val headingSize = when (headingLevel) {
                1 -> 29.sp
                2 -> 25.sp
                3 -> 22.sp
                4 -> 20.sp
                5 -> 18.sp
                else -> 16.sp
            }
            MarkdownInlineParagraph(
                nodes = node.children,
                source = source,
                style = TextStyle(
                    color = colors.bright,
                    fontSize = headingSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fonts.normalFontFamily,
                    lineHeight = (headingSize.value + 8f).sp,
                ),
                onClickLink = onClickLink,
                isTrimContent = true,
            )
        }

        "HORIZONTAL_RULE" -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.line)
        )

        "CODE_FENCE", "CODE_BLOCK" -> MarkdownCodeBlock(
            node = node,
            source = source,
        )

        "BLOCK_QUOTE" -> {
            val childBlocks = node.children.filter {
                val typeName = it.typeName()
                typeName != "EOL" &&
                    typeName != "WHITE_SPACE" &&
                    !(typeName == "BLOCK_QUOTE" && it.children.isEmpty())
            }
            if (childBlocks.isEmpty()) {
                FallbackCodeBlock(text = node.safeText(source))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(colors.unimportant)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        childBlocks.forEach {
                            MarkdownBlockView(
                                node = it,
                                source = source,
                                listDepth = listDepth,
                                onClickLink = onClickLink,
                            )
                        }
                    }
                }
            }
        }

        "UNORDERED_LIST", "ORDERED_LIST" -> MarkdownListBlock(
            node = node,
            source = source,
            isOrdered = nodeType == "ORDERED_LIST",
            listDepth = listDepth,
            onClickLink = onClickLink,
        )

        "TABLE" -> MarkdownTable(node = node, source = source, onClickLink = onClickLink)

        else -> FallbackCodeBlock(text = node.safeText(source))
    }
}

@Composable
private fun MarkdownListBlock(
    node: ASTNode,
    source: String,
    isOrdered: Boolean,
    listDepth: Int,
    onClickLink: (String) -> Unit,
) {
    val items = node.children.filter { it.typeName() == "LIST_ITEM" }
    if (items.isEmpty()) {
        FallbackCodeBlock(text = node.safeText(source))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                val bullet = if (isOrdered) {
                    "${index + 1}."
                } else if (listDepth % 2 == 0) {
                    "•"
                } else {
                    "◦"
                }
                AppText(text = bullet, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.width(6.dp))

                val childBlocks = item.children.filter {
                    val typeName = it.typeName()
                    typeName != "EOL" &&
                        typeName != "WHITE_SPACE" &&
                        typeName != "LIST_BULLET" &&
                        typeName != "LIST_NUMBER"
                }
                if (childBlocks.isEmpty()) {
                    MarkdownInlineParagraph(
                        nodes = item.children,
                        source = source,
                        style = TextStyle(
                            color = LocalColor.current.text,
                            fontSize = LocalFont.current.bodyFontSize,
                            fontFamily = LocalFont.current.normalFontFamily,
                            lineHeight = 22.sp,
                        ),
                        onClickLink = onClickLink,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        childBlocks.forEach {
                            MarkdownBlockView(
                                node = it,
                                source = source,
                                listDepth = listDepth + 1,
                                onClickLink = onClickLink,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    node: ASTNode,
    source: String,
    onClickLink: (String) -> Unit,
) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    val rows = node.children
        .filter { it.typeName() == "HEADER" || it.typeName() == "ROW" }
        .map { rowNode ->
            rowNode.children
                .filter { it.typeName() == "CELL" }
                .map { cell ->
                    buildInlineRenderResult(
                        nodes = cell.children,
                        source = source,
                        linkColor = colors.highlight,
                        codeColor = colors.syntaxColor.stringLiteral,
                        codeBackgroundColor = colors.backgroundSemiLight,
                        codeFontFamily = fonts.monospaceFontFamily,
                    )
                }
        }

    if (rows.isEmpty() || rows.all { it.isEmpty() }) {
        FallbackCodeBlock(text = node.safeText(source))
        return
    }

    val totalColumns = rows.maxOfOrNull { it.size } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = colors.line)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                repeat(totalColumns) { columnIndex ->
                    val cellContent = row.getOrNull(columnIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = colors.line)
                            .background(if (rowIndex == 0) colors.backgroundSemiLight else colors.backgroundInputField)
                            .padding(6.dp)
                    ) {
                        if (cellContent == null) {
                            AppText("", modifier = Modifier.fillMaxWidth())
                        } else {
                            MarkdownInlineText(
                                content = cellContent,
                                style = TextStyle(
                                    color = colors.text,
                                    fontSize = fonts.bodyFontSize,
                                    fontFamily = fonts.normalFontFamily,
                                    lineHeight = 22.sp,
                                ),
                                onClickLink = onClickLink,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlock(node: ASTNode, source: String) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    val language = node.findFirstDescendant { it.typeName() == "FENCE_LANG" }?.safeText(source)?.trim().orEmpty()
    val codeLines = node.findAllDescendants {
        val typeName = it.typeName()
        typeName == "CODE_FENCE_CONTENT" || typeName == "CODE_LINE"
    }.map { it.safeText(source) }
    val code = if (codeLines.isNotEmpty()) {
        codeLines.joinToString("\n")
    } else {
        node.safeText(source)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = colors.line)
            .background(colors.backgroundSemiLight)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (language.isNotBlank()) {
            AppText(
                text = language,
                color = colors.unimportant,
                fontSize = fonts.supplementSize,
                fontFamily = fonts.normalFontFamily,
            )
        }
        AppText(
            text = code,
            fontFamily = fonts.monospaceFontFamily,
            fontSize = fonts.codeEditorBodyFontSize,
            color = colors.text,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun MarkdownInlineParagraph(
    nodes: List<ASTNode>,
    source: String,
    style: TextStyle,
    onClickLink: (String) -> Unit,
    isTrimContent: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val colors = LocalColor.current
    val fonts = LocalFont.current
    val linkColor = LocalColor.current.highlight
    val inline = remember(nodes, source, linkColor, colors.syntaxColor.stringLiteral, fonts.monospaceFontFamily) {
        buildInlineRenderResult(
            nodes = nodes,
            source = source,
            linkColor = linkColor,
            codeColor = colors.syntaxColor.stringLiteral,
            codeBackgroundColor = colors.backgroundSemiLight,
            codeFontFamily = fonts.monospaceFontFamily,
        )
    }.let {
        if (isTrimContent) {
            trimInlineRenderResult(it)
        } else {
            it
        }
    }
    Column(modifier = modifier) {
        MarkdownInlineText(
            content = inline,
            style = style,
            onClickLink = onClickLink,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MarkdownInlineText(
    content: InlineRenderResult,
    style: TextStyle,
    onClickLink: (String) -> Unit,
) {
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    val inlineContent = remember(content.inlineImages) {
        content.inlineImages.mapValues { (_, imageData) ->
            val ratio = parseDataImageRatio(imageData.dataUrl).coerceIn(0.5f, 6f)
            InlineTextContent(
                placeholder = Placeholder(
                    width = (ratio * 1.2f).em,
                    height = 1.2.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                children = {
                    MarkdownDataImageInline(imageData = imageData)
                },
            )
        }
    }

    BasicText(
        text = content.text,
        style = style,
        inlineContent = inlineContent,
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.onPointerEvent(PointerEventType.Release) { event ->
            val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            val offset = textLayoutResult?.getOffsetForPosition(position) ?: return@onPointerEvent
            content.text.getStringAnnotations(
                tag = MarkdownLinkAnnotationTag,
                start = offset,
                end = offset,
            ).firstOrNull()?.let { link ->
                onClickLink(link.item)
            }
        },
    )
}

private fun buildInlineRenderResult(
    nodes: List<ASTNode>,
    source: String,
    linkColor: androidx.compose.ui.graphics.Color,
    codeColor: androidx.compose.ui.graphics.Color,
    codeBackgroundColor: androidx.compose.ui.graphics.Color,
    codeFontFamily: androidx.compose.ui.text.font.FontFamily,
): InlineRenderResult {
    val builder = AnnotatedString.Builder()
    val inlineImages = mutableMapOf<String, MarkdownImageData>()
    var isHtmlUnderline = false
    var isHtmlStrike = false

    val linkStyle = SpanStyle(
        color = linkColor,
        textDecoration = TextDecoration.Underline,
    )
    val codeStyle = SpanStyle(
        color = codeColor,
        fontFamily = codeFontFamily,
        background = codeBackgroundColor,
    )

    fun appendRawText(text: String) {
        if (text.isNotEmpty()) {
            val start = builder.length
            builder.append(text)
            val endExclusive = builder.length
            val decoration = when {
                isHtmlUnderline && isHtmlStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                isHtmlUnderline -> TextDecoration.Underline
                isHtmlStrike -> TextDecoration.LineThrough
                else -> null
            }
            if (decoration != null && endExclusive > start) {
                builder.addStyle(
                    style = SpanStyle(textDecoration = decoration),
                    start = start,
                    end = endExclusive,
                )
            }
        }
    }

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
                    val imageId = "markdown_inline_image_${inlineImages.size}"
                    inlineImages[imageId] = image
                    builder.appendInlineContent(id = imageId, alternateText = InlineImageAlternateText)
                } else {
                    appendRawText(rawNodeText)
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
                val start = builder.length
                appendRawText(label)
                if (!url.isNullOrBlank() && builder.length > start) {
                    builder.addStyle(linkStyle, start, builder.length)
                    builder.addStringAnnotation(
                        tag = MarkdownLinkAnnotationTag,
                        annotation = url,
                        start = start,
                        end = builder.length,
                    )
                }
            }

            typeName == "GFM_AUTOLINK" -> {
                val url = node.safeText(source).trim()
                val start = builder.length
                appendRawText(url)
                if (url.isNotBlank() && builder.length > start) {
                    builder.addStyle(linkStyle, start, builder.length)
                    builder.addStringAnnotation(
                        tag = MarkdownLinkAnnotationTag,
                        annotation = url,
                        start = start,
                        end = builder.length,
                    )
                }
            }

            typeName == "STRONG" -> {
                val start = builder.length
                node.children.forEach { appendNode(it) }
                if (builder.length > start) {
                    val strongText = rawNodeText.trim()
                    val strongStyle = if (strongText.startsWith("__") && strongText.endsWith("__")) {
                        SpanStyle(textDecoration = TextDecoration.Underline)
                    } else {
                        SpanStyle(fontWeight = FontWeight.Bold)
                    }
                    builder.addStyle(strongStyle, start, builder.length)
                }
            }

            typeName == "EMPH" && node.children.isNotEmpty() -> {
                val start = builder.length
                node.children.forEach { appendNode(it) }
                if (builder.length > start) {
                    builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, builder.length)
                }
            }

            typeName == "STRIKETHROUGH" -> {
                val start = builder.length
                node.children.forEach { appendNode(it) }
                if (builder.length > start) {
                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, builder.length)
                }
            }

            typeName == "CODE_SPAN" -> {
                val code = node.findFirstDescendant { it.typeName() == "TEXT" }
                    ?.safeText(source)
                    ?: rawNodeText.trim('`')
                val start = builder.length
                appendRawText(code)
                if (builder.length > start) {
                    builder.addStyle(codeStyle, start, builder.length)
                }
            }

            typeName == "TEXT" || typeName == "WHITE_SPACE" || typeName == "EOL" -> {
                parseInlineTextWithCustomDecorations(
                    text = rawNodeText,
                    baseOffset = node.startOffset,
                ).segments.forEach { segment ->
                    val start = builder.length
                    appendRawText(segment.text)
                    if (builder.length > start) {
                        val customDecoration = when (segment.decorationType) {
                            MarkdownInlineCustomDecorationType.Underline -> TextDecoration.Underline
                            MarkdownInlineCustomDecorationType.Strikethrough -> TextDecoration.LineThrough
                            null -> null
                        }
                        if (customDecoration != null) {
                            builder.addStyle(
                                style = SpanStyle(textDecoration = customDecoration),
                                start = start,
                                end = builder.length,
                            )
                        }
                    }
                }
            }

            shouldSkipInlineNode(node) -> Unit

            node.children.isNotEmpty() -> node.children.forEach { appendNode(it) }

            else -> appendRawText(rawNodeText)
        }
    }

    nodes.forEach { appendNode(it) }

    return InlineRenderResult(text = builder.toAnnotatedString(), inlineImages = inlineImages)
}

private fun trimInlineRenderResult(renderResult: InlineRenderResult): InlineRenderResult {
    val text = renderResult.text
    var start = 0
    var end = text.length
    while (start < end && text[start].isWhitespace()) {
        start++
    }
    while (end > start && text[end - 1].isWhitespace()) {
        end--
    }
    if (start == 0 && end == text.length) {
        return renderResult
    }
    return renderResult.copy(text = text.subSequence(start, end))
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
    if (!Base64ImageRegex.matches(destination)) {
        return null
    }
    return MarkdownImageData(
        altText = alt,
        dataUrl = destination,
    )
}

@Composable
private fun MarkdownDataImageInline(imageData: MarkdownImageData) {
    val colors = LocalColor.current
    val bitmap = remember(imageData.dataUrl) {
        decodeBase64ImageBitmap(imageData.dataUrl)
    }
    if (bitmap == null) {
        AppText(
            text = "",
            color = colors.unimportant,
            fontFamily = LocalFont.current.monospaceFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    AppTooltipArea(
        tooltipText = if (imageData.altText.isBlank()) "Image" else imageData.altText
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = imageData.altText,
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = colors.line),
        )
    }
}

private fun parseDataImageRatio(dataUrl: String): Float {
    return try {
        val encoded = Base64ImageRegex.matchEntire(dataUrl)?.groupValues?.getOrNull(1) ?: return 1f
        val bytes = Base64.getDecoder().decode(encoded)
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return 1f
        image.width.toFloat() / image.height.toFloat()
    } catch (_: Throwable) {
        1f
    }
}

private fun decodeBase64ImageBitmap(dataUrl: String): ImageBitmap? {
    return try {
        val encoded = Base64ImageRegex.matchEntire(dataUrl)?.groupValues?.getOrNull(1) ?: return null
        val bytes = Base64.getDecoder().decode(encoded)
        ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}

@Composable
private fun FallbackCodeBlock(text: String) {
    val colors = LocalColor.current
    val fonts = LocalFont.current
    AppText(
        text = text,
        color = colors.text,
        fontFamily = fonts.monospaceFontFamily,
        fontSize = fonts.codeEditorBodyFontSize,
        lineHeight = 21.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSemiLight)
            .padding(8.dp)
    )
}

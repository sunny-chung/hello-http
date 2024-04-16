package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxTheme

class SyntaxHighlighterTransformation(val highlighter: Highlights) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styles = text.spanStyles.toMutableList()
        val highlights = highlighter.getHighlightsWithBoldKeywords()
        highlights.forEach {
            styles += when (it) {
                is ColorHighlight -> AnnotatedString.Range(SpanStyle(color = Color(it.rgb.toLong() or 0xff000000)), it.location.start, it.location.end)
                is BoldHighlight -> AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), it.location.start, it.location.end)
            }
        }
        return TransformedText(text = AnnotatedString(text = text.text, spanStyles = styles), offsetMapping = OffsetMapping.Identity)
    }

}

// Modified from `Highlights.getHighlights()`
fun Highlights.getHighlightsWithBoldKeywords(): List<CodeHighlight> {
    val theme: SyntaxTheme = getTheme()

    val highlights = mutableListOf<CodeHighlight>()
    val structure = getCodeStructure()
    with(structure) {
        marks.forEach { highlights.add(ColorHighlight(it, theme.mark)) }
        punctuations.forEach { highlights.add(ColorHighlight(it, theme.punctuation)) }
        keywords.forEach {
            highlights.add(ColorHighlight(it, theme.keyword))
            highlights.add(BoldHighlight(it))
        }
        strings.forEach { highlights.add(ColorHighlight(it, theme.string)) }
        literals.forEach { highlights.add(ColorHighlight(it, theme.literal)) }
        annotations.forEach { highlights.add(ColorHighlight(it, theme.metadata)) }
        comments.forEach { highlights.add(ColorHighlight(it, theme.comment)) }
        multilineComments.forEach { highlights.add(ColorHighlight(it, theme.multilineComment)) }
    }

    getEmphasis().forEach { highlights.add(BoldHighlight(it)) }

    return highlights
}

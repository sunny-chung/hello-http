package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor

class SearchHighlightTransformation(private val searchPattern: Regex, private val currentIndex: Int?, colours: AppColor) : VisualTransformation {
    val highlightStyle = SpanStyle(background = colours.backgroundInputFieldHighlight)
    val currentHighlightStyle = SpanStyle(background = colours.backgroundInputFieldHighlightEmphasize)

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        // highlightRanges must be recalculated at this point, because text offset can be changed by other VisualTransformation
        val highlightRanges = searchPattern.findAll(s).map { it.range }.sortedBy { it.start }.toList()
        val spans = highlightRanges.mapIndexed { index, it ->
            val style = if (index == currentIndex) currentHighlightStyle else highlightStyle
            AnnotatedString.Range(style, it.start, it.endInclusive + 1)
        }
        return TransformedText(AnnotatedString(s, text.spanStyles + spans), OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchHighlightTransformation) return false

        if (searchPattern != other.searchPattern) return false
        if (currentIndex != other.currentIndex) return false
        if (highlightStyle != other.highlightStyle) return false
        if (currentHighlightStyle != other.currentHighlightStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = searchPattern.hashCode()
        result = 31 * result + (currentIndex ?: 0)
        result = 31 * result + highlightStyle.hashCode()
        result = 31 * result + currentHighlightStyle.hashCode()
        return result
    }
}
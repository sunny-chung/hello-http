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
}
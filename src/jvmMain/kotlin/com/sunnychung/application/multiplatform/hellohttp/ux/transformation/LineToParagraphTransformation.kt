package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle

val emptyParagraphStyle = ParagraphStyle(
//    platformStyle = PlatformParagraphStyle(),
    lineHeightStyle = LineHeightStyle(
        trim = LineHeightStyle.Trim.Both,
        alignment = LineHeightStyle.Alignment.Proportional
    )
)

class LineToParagraphTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        var start = 0
        val ranges = mutableListOf<IntRange>()
        "\n".toRegex().findAll(s).forEach {
            ranges += start .. it.range.endInclusive
            start = it.range.endInclusive + 1
        }
        return TransformedText(
            AnnotatedString(
                text = s,
                spanStyles = text.spanStyles,
                paragraphStyles = ranges.map {
                    AnnotatedString.Range<ParagraphStyle>(
                        item = emptyParagraphStyle,
                        start = it.start,
                        end = it.endInclusive + 1
                    )
                }
            ),
            offsetMapping = OffsetMapping.Identity,
        )
    }

}

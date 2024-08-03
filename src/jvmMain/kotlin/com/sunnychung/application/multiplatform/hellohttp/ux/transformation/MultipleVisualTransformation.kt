package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log

data class MultipleVisualTransformation(val transforms: List<VisualTransformation>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var annotatedStringResult = text
        val mappings = mutableListOf<OffsetMapping>()
        transforms.forEach {
            val transformed = it.filter(annotatedStringResult)
            annotatedStringResult = transformed.text
            mappings += transformed.offsetMapping
        }
//        log.v { "spans = ${annotatedStringResult.spanStyles}" }
        return TransformedText(text = annotatedStringResult, offsetMapping = MultipleOffsetMapping(mappings))
    }
}

class MultipleOffsetMapping(val mappings: List<OffsetMapping>) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var result = offset
        mappings.forEach { result = it.originalToTransformed(result) }
        return result
    }

    override fun transformedToOriginal(offset: Int): Int {
        var result = offset
        for (i in mappings.indices.reversed()) {
            result = mappings[i].transformedToOriginal(result)
        }
        return result
    }

}

//fun mergeSpanStyles(styles: List<AnnotatedString.Range<SpanStyle>>): List<AnnotatedString.Range<SpanStyle>> {
//    val sortedStyles = styles.mapIndexed { index, it -> Pair(it, index) }
//        .sortedWith(compareBy<Pair<AnnotatedString.Range<SpanStyle>, Int>> { it.first.start }.thenBy { it.first.end }.thenBy { it.second })
//
//    val result = mutableListOf<AnnotatedString.Range<SpanStyle>>()
//    for (i in 0 until sortedStyles.size) {
//        if (i >= 1 && sortedStyles[i].first.start <= sortedStyles[i-1].first.end) {
//            continue
//        }
//        result += sortedStyles[i].first
//    }
//    return result
//}

// TODO refactor this into MultipleVisualTransformation so that each transformations do not have to call individually
fun mergeSpanStylesWithTransformedOffset(previousSpanStyles: List<AnnotatedString.Range<SpanStyle>>, currentOffsetMapping: OffsetMapping) : List<AnnotatedString.Range<SpanStyle>> {
    return previousSpanStyles.map {
        it.copy(
            start = currentOffsetMapping.originalToTransformed(it.start),
            end = currentOffsetMapping.originalToTransformed(it.end),
        )
    }
}

fun AnnotatedString.correctSpanStyleRanges(): AnnotatedString {
    val length = text.length
    return AnnotatedString(
        text = text,
        spanStyles = spanStyles.mapNotNull {
            if (it.start >= length) {
                null
            } else if (it.end >= length) {
                it.copy(end = length)
            } else {
                it
            }
        }
    )
}

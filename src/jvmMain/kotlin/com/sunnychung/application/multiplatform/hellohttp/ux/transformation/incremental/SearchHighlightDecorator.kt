package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.google.common.collect.Range
import com.google.common.collect.TreeRangeMap
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.TreeRangeMaps
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator

class SearchHighlightDecorator(private val searchResultRangesTree: TreeRangeMap<Int, Int>, val currentIndex: Int?, colours: AppColor)
    : BigTextDecorator {

    companion object {
        fun create(ranges: Iterable<IntRange>, currentIndex: Int?, colours: AppColor) =
            SearchHighlightDecorator(TreeRangeMaps.from(ranges), currentIndex, colours)
    }

    val highlightStyle = SpanStyle(background = colours.backgroundInputFieldHighlight)
    val currentHighlightStyle = SpanStyle(background = colours.backgroundInputFieldHighlightEmphasize)

    init {
        log.d { "SearchHighlightDecorator searchResultRangesTree size ${searchResultRangesTree.asMapOfRanges().size}" }
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        val previousSpanStyles = if (text is AnnotatedString) {
            text.spanStyles
        } else {
            emptyList()
        }
        val string = if (text is AnnotatedString) {
            text.text
        } else if (text is String) {
            text
        } else {
            text.toString()
        }

        val newSpanStyles = searchResultRangesTree
            .subRangeMap(Range.closed(originalRange.start, originalRange.endInclusive))
            .asMapOfRanges()
            .map {
                val start = maxOf(0, it.key.lowerEndpoint() - originalRange.start)
                val endExclusive = minOf(originalRange.length, it.key.upperEndpoint() + 1 - originalRange.start)
                log.d { "search hl ${it.key.lowerEndpoint()} .. ${it.key.upperEndpoint()}" }
                AnnotatedString.Range(
                    item = if (it.value == currentIndex) {
                        currentHighlightStyle
                    } else {
                        highlightStyle
                    },
                    start = start,
                    end = endExclusive,
                )
            }

        return if (newSpanStyles.isNotEmpty()) {
            AnnotatedString(string, previousSpanStyles + newSpanStyles)
        } else {
            text
        }
    }
}

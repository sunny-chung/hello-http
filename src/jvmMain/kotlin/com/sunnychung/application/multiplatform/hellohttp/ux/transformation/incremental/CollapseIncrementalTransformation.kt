package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.transform.BigTextTransformOffsetMapping
import com.sunnychung.lib.multiplatform.bigtext.core.transform.BigTextTransformer
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextViewState

class CollapseIncrementalTransformation(colours: AppColor, collapsedCharRanges: List<IntRange>) :
    IncrementalTextTransformation<Unit> {
    val collapsedStyle = SpanStyle(background = colours.backgroundCollapsed)

    private var collapsedCharRanges = collapsedCharRanges

    override fun initialize(text: BigText, transformer: BigTextTransformer) {
        transform(emptyList(), collapsedCharRanges, transformer, null)
    }

    fun update(newCollapsedCharRanges: List<IntRange>, viewState: BigTextViewState) {
        val transformer: BigTextTransformer = viewState.transformedText?.get() ?: return
//        val newCollapsedCharRanges = filterOverlappedIntervals(newCollapsedCharRanges)
        val old = collapsedCharRanges
        collapsedCharRanges = newCollapsedCharRanges

        val restores = old.filter { r1 ->
            newCollapsedCharRanges.none { r2 -> r1 hasIntersectWith r2 && !(r1 surrounds r2) }
        }
        val additions = filterOverlappedIntervals(
            old.filter { r1 ->
                restores.any { r2 -> r1 hasIntersectWith r2 && r2 surrounds r1 }
            } +
            newCollapsedCharRanges.filter { r1 ->
                old.none { r2 -> r1 hasIntersectWith r2 && !(r1 surrounds r2) }
            }
        )
        transform(restores, additions, transformer, viewState)
    }

    private fun filterOverlappedIntervals(intervals: List<IntRange>): List<IntRange> {
        return intervals.filter { r1 ->
            intervals.none { r2 ->
                r1 !== r2 && r1 hasIntersectWith r2 && !(r1 surrounds r2)
            }
        }
    }

    private infix fun IntRange.surrounds(other: IntRange): Boolean {
        return start < other.start && endInclusive > other.endInclusive
    }

    private fun transform(restores: List<IntRange>, additions: List<IntRange>, transformer: BigTextTransformer, viewState: BigTextViewState?) {
        log.d { "CollapseBigTransformation restores=$restores, additions=$additions" }

        restores.forEach {
            transformer.restoreToOriginal(it)
            viewState?.requestReapplyTransformation(it)
        }

        additions.forEach {
            log.d { "CollapseBigTransformation Replace ${it}" }
            if (it.length <= 2) return@forEach
            transformer.replace(it.first + 1 .. it.last - 1, buildAnnotatedString {
                append(" ")
                append(AnnotatedString("...", collapsedStyle))
                append(" ")
            }, BigTextTransformOffsetMapping.WholeBlock)
        }
    }
}

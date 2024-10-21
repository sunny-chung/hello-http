package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.sunnychung.application.multiplatform.hellohttp.constant.UserFunctions
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.util.RangeWithResult
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEventType
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.IncrementalTextTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor

class FunctionIncrementalTransformation(private val themeColors: AppColor) : IncrementalTextTransformation<Unit> {
    val processLengthLimit = 30

    private val functionRegex =
        ("\\$\\(\\((" + UserFunctions.keys.joinToString("|") { Regex.escape(it) } + ")\\)\\)").toRegex()

    private val style = SpanStyle(
        color = themeColors.functionTextColor,
        background = themeColors.functionBackgroundColor,
        fontFamily = FontFamily.Monospace,
    )

    override fun initialize(text: BigText, transformer: BigTextTransformer) {
        val targets = functionRegex.findAll(text.buildString())
        targets.forEach {
            val name = it.groups[1]!!.value
            transformer.replace(it.range, createSpan(name), BigTextTransformOffsetMapping.WholeBlock)
        }
    }

    override fun afterTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {
        val originalText = change.bigText
        when (change.eventType) {
            BigTextChangeEventType.Insert -> {
                val changeRange = change.changeStartIndex until change.changeEndExclusiveIndex
                val targets = findNearbyPatterns(change)
                targets.filter { it.range hasIntersectWith changeRange }
                    .forEach {
                        val name = it.result.groups[1]!!.value
                        transformer.restoreToOriginal(it.range)
                        transformer.replace(it.range, createSpan(name), BigTextTransformOffsetMapping.WholeBlock)
                    }
            }

            else -> {}
        }
    }

    private fun findNearbyPatterns(change: BigTextChangeEvent): Sequence<RangeWithResult<MatchResult>> {
        val startOffset = maxOf(0, change.changeStartIndex - processLengthLimit)
        val substring = change.bigText.substring(
            startOffset
            until
            minOf(change.bigText.length, change.changeEndExclusiveIndex + processLengthLimit)
        )
        return functionRegex.findAll(substring)
            .map {
                RangeWithResult(
                    range = it.range.start + startOffset .. it.range.endInclusive + startOffset,
                    result = it
                )
            }
    }

    override fun beforeTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {
        val originalText = change.bigText
        when (change.eventType) {
            BigTextChangeEventType.Delete -> {
                val changeRange = change.changeStartIndex until change.changeEndExclusiveIndex
                val targets = findNearbyPatterns(change)
                targets.filter { it.range hasIntersectWith changeRange }
                    .forEach {
                        transformer.restoreToOriginal(it.range)
                    }
            }

            else -> {}
        }
    }

    private fun createSpan(name: String): CharSequence {
        return AnnotatedString(name, listOf(AnnotatedString.Range(style, 0, name.length)))
    }
}

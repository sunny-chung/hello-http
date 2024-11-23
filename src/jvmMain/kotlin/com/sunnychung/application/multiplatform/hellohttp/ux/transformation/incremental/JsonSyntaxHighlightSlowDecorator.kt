package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.util.timeAndLog
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.AnnotatedStringTextBuffer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.CacheableBigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.JsonSyntaxHighlightTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.KotlinSyntaxHighlightTransformation

class JsonSyntaxHighlightSlowDecorator(private val colours: AppColor) : CacheableBigTextDecorator() {

    private val transformation = JsonSyntaxHighlightTransformation(colours)
    private var transformedTextCache = AnnotatedString("")
    private var spanStyles = emptyList<AnnotatedString.Range<SpanStyle>>()

    override fun doInitialize(text: BigText) {
        transformedTextCache = transformation.filter(AnnotatedString(text.buildString())).text
        buildCache()
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        timeAndLog(Severity.Verbose, "json sh textChange") {
            transformedTextCache = transformation.filter(AnnotatedString(change.bigText.buildString())).text
            buildCache()
        }
    }

    private fun buildCache() {
        spanStyles = transformedTextCache.spanStyles // assume sorted by range and has no overlapping
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
//        val spanStyles = (transformedTextCache.subSequence(originalRange) as AnnotatedString).spanStyles
//        return AnnotatedString(text.string(), spanStyles)

        val startIndex = binarySearchForMinIndexOfValueAtLeast(spanStyles.indices, originalRange.start) { spanStyles[it].end }
        val endIndex = minOf(spanStyles.lastIndex, binarySearchForMaxIndexOfValueAtMost(spanStyles.indices, originalRange.endInclusive) { spanStyles[it].start })
        val spanStylesSubList = spanStyles.subList(startIndex, endIndex + 1)
            .map { it.copy(start = maxOf(0, it.start - originalRange.start), end = minOf(originalRange.length, it.end - originalRange.start)) }
        return AnnotatedString(text.string(), spanStylesSubList)
    }
}

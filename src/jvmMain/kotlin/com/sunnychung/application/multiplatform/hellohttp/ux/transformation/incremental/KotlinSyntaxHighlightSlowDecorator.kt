package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.CacheableBigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.KotlinSyntaxHighlightTransformation

class KotlinSyntaxHighlightSlowDecorator(private val colours: AppColor) : CacheableBigTextDecorator() {

    private val transformation = KotlinSyntaxHighlightTransformation(colours)
    private var transformedTextCache = AnnotatedString("")

    override fun doInitialize(text: BigText) {
        transformedTextCache = transformation.filter(AnnotatedString(text.buildString())).text
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        transformedTextCache = transformation.filter(AnnotatedString(change.bigText.buildString())).text
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        val spanStyles = (transformedTextCache.subSequence(originalRange) as AnnotatedString).spanStyles
        return AnnotatedString(text.string(), spanStyles)
    }
}

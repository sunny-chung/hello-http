package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.SpanStyle

interface BigTextTransformer {

//    fun resetToOriginal()

//    fun applyStyle(style: SpanStyle, range: IntRange)

    fun append(text: String): Int

    fun insertAt(pos: Int, text: String): Int

    fun delete(range: IntRange): Int

    fun replace(range: IntRange, text: String, offsetMapping: BigTextTransformOffsetMapping)

    fun restoreToOriginal(range: IntRange)
}

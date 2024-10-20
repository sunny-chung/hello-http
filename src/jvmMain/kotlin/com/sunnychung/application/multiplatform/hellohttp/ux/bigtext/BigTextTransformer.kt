package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.SpanStyle

interface BigTextTransformer {

//    fun resetToOriginal()

//    fun applyStyle(style: SpanStyle, range: IntRange)

    fun append(text: CharSequence): Int

    fun insertAt(pos: Int, text: CharSequence): Int

    fun delete(range: IntRange): Int

    fun replace(range: IntRange, text: CharSequence, offsetMapping: BigTextTransformOffsetMapping)

    fun restoreToOriginal(range: IntRange)

//    fun layoutTransaction(transaction: BigTextLayoutTransaction.() -> Unit)
}

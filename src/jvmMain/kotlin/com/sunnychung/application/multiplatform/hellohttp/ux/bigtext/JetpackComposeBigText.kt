package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.AnnotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.AnnotatedStringBuilder

fun BigText.Companion.createFromLargeAnnotatedString(initialContent: AnnotatedString) = BigTextImpl(
    textBufferFactory = { AnnotatedStringTextBuffer(it) },
//    charSequenceBuilderFactory = { AnnotatedString.Builder(it) },
//    charSequenceFactory = { (it as AnnotatedString.Builder).toAnnotatedString() },
    charSequenceBuilderFactory = { AnnotatedStringBuilder(it) },
    charSequenceFactory = { (it as AnnotatedStringBuilder).toAnnotatedString() },
).apply {
    log.d { "createFromLargeAnnotatedString ${initialContent.length}" }
    append(initialContent)
    isUndoEnabled = true // it has to be after append to avoid recording into the undo history
}

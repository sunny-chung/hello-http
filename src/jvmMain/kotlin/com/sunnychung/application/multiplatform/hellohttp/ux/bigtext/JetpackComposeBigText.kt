package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.AnnotatedString

fun BigText.Companion.createFromLargeAnnotatedString(initialContent: AnnotatedString) = BigTextImpl(
    textBufferFactory = { AnnotatedStringTextBuffer(it) },
    charSequenceBuilderFactory = { AnnotatedString.Builder(it) },
    charSequenceFactory = { (it as AnnotatedString.Builder).toAnnotatedString() },
).apply {
    log.d { "createFromLargeAnnotatedString ${initialContent.length}" }
    append(initialContent)
}

package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.ui.text.AnnotatedString

fun String?.emptyToNull(): String? {
    return if (this == "") null else this
}

fun CharSequence.findAllIndicesOfChar(char: Char): List<Int> {
    val result = mutableListOf<Int>()
    for (i in this.indices) {
        if (char == this[i]) {
            result.add(i)
        }
    }
    return result
}

fun CharSequence.string(): String = when (this) {
    is String -> this
    is AnnotatedString -> text
    else -> toString()
}

fun CharSequence.annotatedString(): AnnotatedString = when (this) {
    is String -> AnnotatedString(this)
    is AnnotatedString -> this
    else -> AnnotatedString(toString())
}

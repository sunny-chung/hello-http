package com.sunnychung.application.multiplatform.hellohttp.util

fun <T> MutableList<T>.replaceIf(replacement: T, condition: (T) -> Boolean) {
    val index = indexOfFirst(condition)
    if (index >= 0) {
        set(index, replacement)
    } else {
        throw IllegalStateException("Cannot replace state in iterables")
    }
}

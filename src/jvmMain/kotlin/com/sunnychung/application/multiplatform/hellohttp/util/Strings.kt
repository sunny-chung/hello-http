package com.sunnychung.application.multiplatform.hellohttp.util

fun String?.emptyToNull(): String? {
    return if (this == "") null else this
}

fun String.findAllIndicesOfChar(char: Char): List<Int> {
    val result = mutableListOf<Int>()
    for (i in this.indices) {
        if (char == this[i]) {
            result.add(i)
        }
    }
    return result
}

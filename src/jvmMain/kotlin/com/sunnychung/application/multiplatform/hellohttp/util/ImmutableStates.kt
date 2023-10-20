package com.sunnychung.application.multiplatform.hellohttp.util

import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable

fun <T: Identifiable> List<T>.copyWithChange(change: T): List<T> {
    var isChanged = false
    val result = toMutableList()
        .map {
            if (it.id == change.id) {
                isChanged = true
                change
            } else {
                it
            }
        }
    if (!isChanged) {
        log.w { "An update did not preserve." }
    }
    return result
}

fun <T> List<T>.copyWithIndexedChange(index: Int, change: T): List<T> {
    return toMutableList()
        .apply { set(index, change) }
        .toList()
}

fun <T> List<T>.copyWithRemovedIndex(index: Int): List<T> {
    return toMutableList()
        .apply { removeAt(index) }
        .toList()
}

fun <T> List<T>.copyWithRemoval(removeIf: (T) -> Boolean): List<T> {
    return toMutableList()
        .apply { removeIf(removeIf) }
        .toList()
}

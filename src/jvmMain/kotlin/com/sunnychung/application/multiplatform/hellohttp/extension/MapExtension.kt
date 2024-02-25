package com.sunnychung.application.multiplatform.hellohttp.extension

import java.util.SortedMap

fun <K, V> SortedMap<K, V>.lastOrNull(): V? =
    if (isEmpty()) {
        null
    } else {
        get(lastKey())
    }

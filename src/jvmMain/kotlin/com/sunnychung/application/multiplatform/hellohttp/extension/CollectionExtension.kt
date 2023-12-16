package com.sunnychung.application.multiplatform.hellohttp.extension

fun <T, C: Collection<T>> C.emptyToNull(): C? {
    return if (isEmpty()) {
        null
    } else {
        this
    }
}

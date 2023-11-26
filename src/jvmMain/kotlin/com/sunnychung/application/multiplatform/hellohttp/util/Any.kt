package com.sunnychung.application.multiplatform.hellohttp.util

inline fun <T1, T2, R> let(a1: T1?, a2: T2?, compute: (T1, T2) -> R): R? {
    return if (listOf(a1, a2).all { it != null }) {
        compute(a1!!, a2!!)
    } else {
        null
    }
}

inline fun <T1, T2, T3, R> let(a1: T1?, a2: T2?, a3: T3?, compute: (T1, T2, T3) -> R): R? {
    return if (listOf(a1, a2, a3).all { it != null }) {
        compute(a1!!, a2!!, a3!!)
    } else {
        null
    }
}

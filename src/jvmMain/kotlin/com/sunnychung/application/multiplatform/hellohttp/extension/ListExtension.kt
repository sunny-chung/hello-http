package com.sunnychung.application.multiplatform.hellohttp.extension

import kotlin.math.roundToInt

/**
 * Can only be used on a **sorted** list.
 *
 * @param comparison This function should never return 0
 */
fun <T> List<T>.binarySearchForInsertionPoint(comparison: (T) -> Int): Int {
    val r = binarySearch(comparison = comparison)
    if (r >= 0) throw IllegalArgumentException("Parameter `comparison` should never return 0")
    return -(r + 1)
}

fun <T : Number> List<T>.atPercent(percent: Int): Double {
    if (isEmpty()) return 0.0
    return if (percent == 50) {
        if (size % 2 == 0) {
            this[lastIndex / 2].toDouble() / 2.0 + this[lastIndex / 2 + 1].toDouble() / 2.0
        } else {
            this[lastIndex / 2].toDouble()
        }
    } else {
        this[(lastIndex * percent.toDouble() / 100.0).roundToInt()].toDouble()
    }
}

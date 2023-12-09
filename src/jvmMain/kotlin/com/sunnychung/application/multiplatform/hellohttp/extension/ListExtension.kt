package com.sunnychung.application.multiplatform.hellohttp.extension

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

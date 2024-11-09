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

/**
 * Can only be used on an **ascending** list.
 *
 * R = A.f(x);
 * Return maximum R so that A[R] <= x.
 *
 * For example, for following values:
 *
 * [0, 2, 37, 57, 72, 85, 91, 113]
 * f(-1) = -1, f(0) = 0, f(72) = 4, f(73) = 4, f(84) = 4, f(85) = 5, f(113) = 7, f(999) = 7
 *
 * @param searchValue
 */
fun List<Int>.binarySearchForMaxIndexOfValueAtMost(searchValue: Int): Int {
    val insertionPoint = binarySearchForInsertionPoint { if (it >= searchValue) 1 else -1 }
    if (insertionPoint > lastIndex) return lastIndex
    return if (searchValue < this[insertionPoint]) {
        insertionPoint - 1
    } else {
        insertionPoint
    }
}

/**
 * Can only be used on an **ascending** list.
 *
 * R = A.f(x);
 * Return minimum R so that A[R] >= x.
 *
 * For example, for following values:
 *
 * [0, 2, 37, 57, 72, 85, 91, 113]
 * f(-1) = 0, f(0) = 0, f(72) = 4, f(73) = 5, f(84) = 5, f(85) = 5, f(113) = 7, f(999) = 8
 *
 *
 *
 * @param searchValue
 */
fun List<Int>.binarySearchForMinIndexOfValueAtLeast(searchValue: Int): Int {
    val insertionPoint = binarySearchForInsertionPoint { if (it >= searchValue) 1 else -1 }
    return insertionPoint
}

/**
 * `this` has to be ascending. `newElements` has to be strictly ascending.
 */
fun <T : Comparable<T>> MutableList<T>.addToThisAscendingListWithoutDuplicate(newElements: List<T>) {
    if (isEmpty()) {
        this.addAll(newElements)
        return
    }
    val thisLast = last()
    var insertStartIndex = 0
    while (insertStartIndex <= newElements.lastIndex && thisLast >= newElements[insertStartIndex]) {
        ++insertStartIndex
    }
    if (insertStartIndex > newElements.lastIndex) {
        return
    } else if (insertStartIndex == 0) {
        this.addAll(newElements)
        return
    }
    this.addAll(newElements.subList(insertStartIndex, newElements.size))
}

/**
 * `this` has to be ascending. `newElements` has to be strictly ascending.
 */
fun <T : Comparable<T>> MutableList<T>.addToThisAscendingListWithoutDuplicate(newElement: T) {
    if (isEmpty()) {
        this.add(newElement)
        return
    }
    val thisLast = last()
    if (thisLast == newElement) {
        return
    }
    this.add(newElement)
}

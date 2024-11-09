package com.sunnychung.application.multiplatform.hellohttp.extension

operator fun IntRange.contains(other: IntRange): Boolean {
    return other.start in this && other.endInclusive in this
}

infix fun IntRange.intersect(other: IntRange): IntRange {
    if (start > other.endInclusive || endInclusive < other.start) {
        return IntRange.EMPTY
    }
    val from = if (start in other) start else other.start
    val to = if (last in other) last else other.last
    return from .. to
}

infix fun IntRange.hasIntersectWith(other: IntRange): Boolean {
    return !intersect(other).isEmpty()
}

/**
 * Use this function may overflow.
 */
infix fun UIntRange.hasIntersectWith(other: UIntRange): Boolean {
    fun UIntRange.toIntRange() = start.toInt() .. endInclusive.toInt()

    return !toIntRange().intersect(other.toIntRange()).isEmpty()
}

fun IntRange.toNonEmptyRange(): IntRange {
    if (length <= 0) {
        return start .. start
    }
    return this
}

val IntRange.length: Int
    get() = this.endInclusive - this.start + 1

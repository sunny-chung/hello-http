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

val IntRange.length: Int
    get() = this.endInclusive - this.start + 1

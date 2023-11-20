package com.sunnychung.application.multiplatform.hellohttp.extension

operator fun IntRange.contains(other: IntRange): Boolean {
    return other.start in this && other.endInclusive in this
}

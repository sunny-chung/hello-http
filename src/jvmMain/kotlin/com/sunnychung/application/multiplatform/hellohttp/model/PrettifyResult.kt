package com.sunnychung.application.multiplatform.hellohttp.model

data class PrettifyResult(
    val prettyString: String,
    val collapsableLineRange: List<IntRange> = emptyList(),
    val collapsableCharRange: List<IntRange> = emptyList(),

    /**
     * For String, quotes are included into the ranges. Guaranteed to be sorted.
     */
    val literalRange: List<IntRange> = emptyList(),
)

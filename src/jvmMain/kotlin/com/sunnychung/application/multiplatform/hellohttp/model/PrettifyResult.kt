package com.sunnychung.application.multiplatform.hellohttp.model

data class PrettifyResult(
    val prettyString: String,
    val collapsableLineRange: List<IntRange> = emptyList(),
    val collapsableCharRange: List<IntRange> = emptyList(),
)

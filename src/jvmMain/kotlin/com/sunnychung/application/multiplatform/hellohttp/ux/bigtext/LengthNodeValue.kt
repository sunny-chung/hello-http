package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface LengthNodeValue {
    val leftStringLength: Int

    val bufferLength: Int

    val leftOverallLength: Int
    val currentOverallLength: Int

    val leftRenderLength: Int
    val currentRenderLength: Int

    val transformOffsetMapping: BigTextTransformOffsetMapping
}

package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.util.CharMeasurer

class FixedWidthCharMeasurer(private val charWidth: Float) : CharMeasurer {
    override fun measureFullText(text: String) {
        // Nothing
    }

    override fun findCharWidth(char: String): Float = charWidth
}

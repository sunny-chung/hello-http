package com.sunnychung.application.multiplatform.hellohttp.util

interface CharMeasurer {

    fun measureFullText(text: String)

    fun findCharWidth(char: String): Float
}

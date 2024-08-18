package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface TextLayouter {

    fun indexCharWidth(text: String)

    fun layoutOneLine(line: CharSequence, contentWidth: Float, firstRowOccupiedWidth: Float, offset: Int): Pair<List<Int>, Float>
}

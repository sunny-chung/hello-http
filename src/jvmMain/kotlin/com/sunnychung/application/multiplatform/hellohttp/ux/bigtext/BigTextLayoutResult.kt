package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.annotation.TemporaryApi
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.util.CharMeasurer

@OptIn(TemporaryApi::class)
@Deprecated("Slow")
class BigTextLayoutResult(
    /** Number of transformed row spans of non-transformed lines */
    @Deprecated("Slow") @property:TemporaryApi val lineRowSpans: List<Int>, // O(L)
    /** First transformed row index of non-transformed lines */
    @Deprecated("Slow") @property:TemporaryApi val lineFirstRowIndices: List<Int>, // O(L)
    /** Transformed start char index of transformed rows */
    internal val rowStartCharIndices: List<Int>, // O(R)
    @Deprecated("Slow") val rowHeight: Float,
    @Deprecated("Slow") val totalLines: Int,
    @Deprecated("Slow") val totalRows: Int,
    /** Total number of lines before transformation */ val totalLinesBeforeTransformation: Int,
    private val charMeasurer: CharMeasurer,
) {
    @Deprecated("Slow") fun findLineNumberByRowNumber(rowNumber: Int): Int {
        return lineFirstRowIndices.binarySearchForMaxIndexOfValueAtMost(rowNumber)
    }

    @Deprecated("Slow") fun getLineTop(originalLineNumber: Int): Float = lineFirstRowIndices[originalLineNumber] * rowHeight

    @Deprecated("Slow") fun findCharWidth(char: String) = charMeasurer.findCharWidth(char)
}

class BigTextSimpleLayoutResult(
    val text: BigTextLayoutable,
    val rowHeight: Float
) {
    fun getTopOfRow(rowIndex: Int): Float = rowIndex * rowHeight
    fun getBottomOfRow(rowIndex: Int): Float = (rowIndex + 1) * rowHeight

    val top: Float
        get() = 0f

    val bottom: Float
        get() = getBottomOfRow(text.lastRowIndex)
}

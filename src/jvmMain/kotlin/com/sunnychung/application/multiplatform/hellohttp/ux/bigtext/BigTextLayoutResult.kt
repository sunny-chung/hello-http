package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.annotation.TemporaryApi
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast

@OptIn(TemporaryApi::class)
class BigTextLayoutResult(
    /** Number of transformed row spans of non-transformed lines */
    @property:TemporaryApi val lineRowSpans: List<Int>, // O(L)
    /** First transformed row index of non-transformed lines */
    @property:TemporaryApi val lineFirstRowIndices: List<Int>, // O(L)
    /** Transformed start char index of transformed rows */
    internal val rowStartCharIndices: List<Int>, // O(R)
    val rowHeight: Float,
    val totalLines: Int,
    val totalRows: Int,
    /** Total number of lines before transformation */ val totalLinesBeforeTransformation: Int,
) {
    fun findLineNumberByRowNumber(rowNumber: Int): Int {
        return lineFirstRowIndices.binarySearchForMinIndexOfValueAtLeast(rowNumber)
    }

    fun getLineTop(originalLineNumber: Int): Float = lineFirstRowIndices[originalLineNumber] * rowHeight
}

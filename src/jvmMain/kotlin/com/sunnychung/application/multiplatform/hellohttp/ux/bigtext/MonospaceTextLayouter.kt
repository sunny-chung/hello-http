package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.input.TransformedText
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.util.log

private val LINE_BREAK_REGEX = "\n".toRegex()

class MonospaceTextLayouter {
    fun layout(text: String, transformedText: TransformedText, lineHeight: Float, numOfCharsPerLine: Int): BigTextLayoutResult {
        if (numOfCharsPerLine < 1) {
            return BigTextLayoutResult(
                lineRowSpans = listOf(1),
                lineFirstRowIndices = listOf(0),
                rowStartCharIndices = listOf(0),
                rowHeight = lineHeight,
                totalLinesBeforeTransformation = 1,
                totalLines = 1,
                totalRows = 1,
            )
        }
        val originalLineStartIndices = (
                sequenceOf(0) +
                        LINE_BREAK_REGEX.findAll(text).sortedBy { it.range.last }.map { it.range.last + 1 }
                ).toList()
        val transformedLineStartIndices = (
                sequenceOf(0) +
                        LINE_BREAK_REGEX.findAll(transformedText.text).sortedBy { it.range.last }.map { it.range.last + 1 }
                ).toList()
        val lineRowSpans = MutableList(originalLineStartIndices.size) { 1 }
        val lineRowIndices = MutableList(originalLineStartIndices.size + 1) { 0 }
        val transformedRowStartCharIndices = transformedLineStartIndices.flatMapIndexed { index, it ->
            if (index + 1 <= transformedLineStartIndices.lastIndex) {
                val numCharsInThisLine = transformedLineStartIndices[index + 1] - it - (if (transformedText.text[transformedLineStartIndices[index + 1] - 1] == '\n') 1 else 0)
                val numOfRows = numCharsInThisLine divRoundUp numOfCharsPerLine
                (0 until numOfRows).map { j ->
                    (it + j * numOfCharsPerLine).also { k ->
                        log.v { "calc index $index -> $it ($numCharsInThisLine, $numOfCharsPerLine) $k" }
                    }
                }
            } else {
                listOf(it)
            }
        }.also {
            log.v { "rowStartCharIndices = $it" }
        }
        originalLineStartIndices.forEachIndexed { index, it ->
            val transformedStartCharIndex = transformedText.offsetMapping.originalToTransformed(originalLineStartIndices[index])
            val transformedEndCharIndex = if (index + 1 <= originalLineStartIndices.lastIndex) {
                transformedText.offsetMapping.originalToTransformed(originalLineStartIndices[index + 1])
            } else {
                transformedText.text.lastIndex + 1
            }
            val displayRowStart = transformedRowStartCharIndices.binarySearchForMinIndexOfValueAtLeast(transformedStartCharIndex)
            val displayRowEnd = transformedRowStartCharIndices.binarySearchForMinIndexOfValueAtLeast(transformedEndCharIndex)
            val numOfRows = displayRowEnd - displayRowStart
            lineRowSpans[index] = numOfRows
            lineRowIndices[index + 1] = lineRowIndices[index] + numOfRows
            log.v { "lineRowSpans[$index] = ${lineRowSpans[index]} ($transformedStartCharIndex ..< $transformedEndCharIndex) (L $displayRowStart ..< $displayRowEnd)" }
        }
        log.v { "totalLinesBeforeTransformation = ${originalLineStartIndices.size}" }
        log.v { "totalLines = ${transformedLineStartIndices.size}" }
        log.v { "totalRows = ${transformedRowStartCharIndices.size}" }
        return BigTextLayoutResult(
            lineRowSpans = lineRowSpans.toList(),
            lineFirstRowIndices = lineRowIndices.toList(),
            rowStartCharIndices = transformedRowStartCharIndices,
            rowHeight = lineHeight,
            totalLines = transformedLineStartIndices.size,
            totalRows = transformedRowStartCharIndices.size,
            totalLinesBeforeTransformation = originalLineStartIndices.size,
        )
    }
}

private infix fun Int.divRoundUp(other: Int): Int {
    val div = this / other
    val remainder = this % other
    return if (remainder == 0) {
        div
    } else {
        div + 1
    }
}

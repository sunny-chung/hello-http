package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TransformedText
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.util.CharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.util.ComposeUnicodeCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.util.log

private val LINE_BREAK_REGEX = "\n".toRegex()

class MonospaceTextLayouter : TextLayouter {
    val charMeasurer: CharMeasurer

    constructor(charMeasurer: CharMeasurer) {
        this.charMeasurer = charMeasurer
    }

    constructor(textMeasurer: TextMeasurer, textStyle: TextStyle) : this(ComposeUnicodeCharMeasurer(textMeasurer, textStyle))

    fun layout(text: String, transformedText: TransformedText, lineHeight: Float, contentWidth: Float): BigTextLayoutResult {
        log.v { "layout cw=$contentWidth" }
        if (contentWidth < 1) {
            return BigTextLayoutResult(
                lineRowSpans = listOf(1),
                lineFirstRowIndices = listOf(0),
                rowStartCharIndices = listOf(0),
                rowHeight = lineHeight,
                totalLinesBeforeTransformation = 1,
                totalLines = 1,
                totalRows = 1,
                charMeasurer = charMeasurer,
            )
        }
        charMeasurer.measureFullText(text) // O(S lg C)

        val originalLineStartIndices = ( // O(L lg L)
                sequenceOf(0) +
                        LINE_BREAK_REGEX.findAll(text).sortedBy { it.range.last }.map { it.range.last + 1 }
                ).toList()
        val transformedLineStartIndices = ( // O(L lg L)
                sequenceOf(0) +
                        LINE_BREAK_REGEX.findAll(transformedText.text).sortedBy { it.range.last }.map { it.range.last + 1 }
                ).toList()
        val lineRowSpans = MutableList(originalLineStartIndices.size) { 1 }
        val lineRowIndices = MutableList(originalLineStartIndices.size + 1) { 0 }
        val transformedRowStartCharIndices = listOf(0) + transformedLineStartIndices.flatMapIndexed { index, lineStartIndex ->
            if (index + 1 <= transformedLineStartIndices.lastIndex) {
                val numCharsInThisLine = transformedLineStartIndices[index + 1] - lineStartIndex - (if (transformedText.text[transformedLineStartIndices[index + 1] - 1] == '\n') 1 else 0)
                // O(line string length * lg C)
                val charWidths = text.substring(lineStartIndex, lineStartIndex + numCharsInThisLine).map { charMeasurer.findCharWidth(it.toString()) }
//                val numOfRows = maxOf(1, numCharsInThisLine divRoundUp numOfCharsPerLine)
//                (0 until numOfRows).map { j ->
//                    (it + j * numOfCharsPerLine).also { k ->
//                        log.v { "calc index $index -> $it ($numCharsInThisLine, $numOfCharsPerLine) $k" }
//                    }
//                }
                var numCharsPerRow = mutableListOf<Int>()
                var currentRowOccupiedWidth = 0f
                var numCharsInCurrentRow = 0
                charWidths.forEachIndexed { i, w -> // O(line string length)
                    if (currentRowOccupiedWidth + w > contentWidth && numCharsInCurrentRow > 0) {
                        numCharsPerRow += numCharsInCurrentRow
                        numCharsInCurrentRow = 0
                        currentRowOccupiedWidth = 0f
                    }
                    currentRowOccupiedWidth += w
                    ++numCharsInCurrentRow
                }
                if (numCharsInCurrentRow > 0) {
                    numCharsPerRow += numCharsInCurrentRow
                }
                if (numCharsPerRow.isEmpty()) {
                    numCharsPerRow += 0
                }
                var s = 0
                numCharsPerRow.mapIndexed { index, it ->
                    s += it
                    minOf(
                        lineStartIndex + s + if (index >= numCharsPerRow.lastIndex) 1 else 0 /* skip the last char '\n' */,
                        text.length
                    )
                }
            } else {
                emptyList()
            }.also {
                log.v { "transformedLineStartIndices flatMap $index -> $it" }
            }
        }.also {
            log.v { "transformedRowStartCharIndices = $it" }
        }
        originalLineStartIndices.forEachIndexed { index, it ->
            val transformedStartCharIndex = transformedText.offsetMapping.originalToTransformed(originalLineStartIndices[index])
            val transformedEndCharIndex = if (index + 1 <= originalLineStartIndices.lastIndex) {
                transformedText.offsetMapping.originalToTransformed(originalLineStartIndices[index + 1])
            } else {
                transformedText.text.lastIndex + 1
            }
            val displayRowStart = transformedRowStartCharIndices.binarySearchForMaxIndexOfValueAtMost(transformedStartCharIndex)
            val displayRowEnd = transformedRowStartCharIndices.binarySearchForMaxIndexOfValueAtMost(transformedEndCharIndex)
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
            charMeasurer = charMeasurer,
        )
    }

    override fun indexCharWidth(text: String) {
        charMeasurer.measureFullText(text)
    }

    override fun layoutOneLine(line: CharSequence, contentWidth: Float, firstRowOccupiedWidth: Float, offset: Int): Pair<List<Int>, Float> {
        val charWidths = line.map { charMeasurer.findCharWidth(it.toString()) }
        val isOffsetLastLine = line.endsWith('\n')
        var numCharsPerRow = mutableListOf<Int>()
        var currentRowOccupiedWidth = firstRowOccupiedWidth
        var numCharsInCurrentRow = 0
        charWidths.forEachIndexed { i, w -> // O(line string length)
            if (currentRowOccupiedWidth + w > contentWidth && (numCharsInCurrentRow > 0 || currentRowOccupiedWidth > 0)) {
                numCharsPerRow += numCharsInCurrentRow
                numCharsInCurrentRow = 0
                currentRowOccupiedWidth = 0f
            }
            currentRowOccupiedWidth += w
            ++numCharsInCurrentRow
        }
//        if (numCharsInCurrentRow > 0) {
//            numCharsPerRow += numCharsInCurrentRow
//        }
//        if (numCharsPerRow.isEmpty()) {
//            numCharsPerRow += 0
//        }
        var s = 0
        return numCharsPerRow.mapIndexed { index, it ->
            s += it
            offset + s + if (index >= numCharsPerRow.lastIndex && isOffsetLastLine) 1 else 0 /* skip the last char '\n' */
        } to currentRowOccupiedWidth
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

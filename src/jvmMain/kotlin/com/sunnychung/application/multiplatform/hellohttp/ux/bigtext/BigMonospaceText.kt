package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.annotation.TemporaryApi
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private val LINE_BREAK_REGEX = "\n".toRegex()

@Composable
fun BigMonospaceText(
    modifier: Modifier = Modifier,
    text: String,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    visualTransformation: VisualTransformation,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onTextLayoutResult: ((BigTextLayoutResult) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val contentWidth = width - with(density) {
        (padding.calculateStartPadding(LayoutDirection.Ltr) + padding.calculateEndPadding(LayoutDirection.Ltr)).toPx()
    }
    var lineHeight by remember { mutableStateOf(0f) }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = color,
    )
    val numOfCharsPerLine = rememberLast(density.density, density.fontScale, fontSize, width) {
        if (width > 0) {
            Paragraph(
                text = "0".repeat(1000),
                style = textStyle,
                constraints = Constraints(maxWidth = contentWidth.toInt()),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
            ).let {
                lineHeight = it.getLineTop(1) - it.getLineTop(0)
                it.getLineEnd(0)
            }
        } else {
            0
        }
    }
    val transformedText = rememberLast(text.length, text.hashCode(), visualTransformation) {
        visualTransformation.filter(AnnotatedString(text)).also {
            log.v { "transformed text = `$it`" }
        }
    }
    // a line may span multiple rows
    val layoutResult = rememberLast(transformedText.text.length, transformedText.hashCode(), numOfCharsPerLine) {
        if (numOfCharsPerLine < 1) {
            return@rememberLast BigTextLayoutResult(
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
        BigTextLayoutResult(
            lineRowSpans = lineRowSpans.toList(),
            lineFirstRowIndices = lineRowIndices.toList(),
            rowStartCharIndices = transformedRowStartCharIndices,
            rowHeight = lineHeight,
            totalLines = transformedLineStartIndices.size,
            totalRows = transformedRowStartCharIndices.size,
            totalLinesBeforeTransformation = originalLineStartIndices.size,
        ).also {
            if (onTextLayoutResult != null) {
                onTextLayoutResult(it)
            }
        }
    }
    val rowStartCharIndices = layoutResult.rowStartCharIndices

    rememberLast(height, rowStartCharIndices.size, lineHeight) {
        scrollState::class.declaredMemberProperties.first { it.name == "maxValue" }
            .apply {
                (this as KMutableProperty<Int>)
                setter.isAccessible = true
                val scrollableHeight = maxOf(
                    0f,
                    rowStartCharIndices.size * lineHeight - height +
                        with (density) {
                            (padding.calculateTopPadding() + padding.calculateBottomPadding()).toPx()
                        }
                )
                setter.call(scrollState, scrollableHeight.roundToInt())
            }
    }

    val coroutineScope = rememberCoroutineScope() // for scrolling
    var scrollOffset by remember { mutableStateOf(0f) }
//    val scrollState =
    val scrollableState = rememberScrollableState { delta ->
        coroutineScope.launch {
            scrollState.scrollBy(-delta)
        }
//        scrollOffset = minOf(maxOf(0f, scrollOffset - delta), maxOf(0f, rowStartCharIndices.size * lineHeight - height))
        delta
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                width = it.size.width
                height = it.size.height
            }
            .clipToBounds()
            .padding(padding)
            .scrollable(scrollableState, orientation = Orientation.Vertical)
    ) {
        val viewportTop = scrollState.value.toFloat()
//        val viewportTop = scrollOffset
        val viewportBottom = viewportTop + height
        if (lineHeight > 0) {
            val firstRowIndex = maxOf(0, (viewportTop / lineHeight).toInt())
            val lastRowIndex = minOf(rowStartCharIndices.lastIndex, (viewportBottom / lineHeight).toInt() + 1)
            log.v { "row index = [$firstRowIndex, $lastRowIndex]; scroll = $viewportTop ~ $viewportBottom; line h = $lineHeight" }
            viewState.firstVisibleRow = firstRowIndex
            viewState.lastVisibleRow = lastRowIndex

            with(density) {
                (firstRowIndex..lastRowIndex).forEach { i ->
                    val startIndex = rowStartCharIndices[i]
                    val endIndex = if (i + 1 > rowStartCharIndices.lastIndex) {
                        transformedText.text.length
                    } else {
                        rowStartCharIndices[i + 1]
                    }
                    log.v { "line #$i: [$startIndex, $endIndex)" }
                    BasicText(
                        text = transformedText.text.subSequence(
                            startIndex = startIndex,
                            endIndex = endIndex,
                        ),
                        style = textStyle,
                        maxLines = 1,
                        modifier = Modifier.offset(y = (- viewportTop + (i/* - firstRowIndex*/) * lineHeight).toDp())
                    )
                }
            }
        }
    }
}

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

class BigTextViewState {
    var firstVisibleRow: Int by mutableStateOf(0)
        internal set

    var lastVisibleRow: Int by mutableStateOf(0)
        internal set
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

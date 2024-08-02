package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.TextUnit
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.AppText
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private val LINE_BREAK_REGEX = "\n".toRegex()

@Composable
fun BigMonospaceText(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    visualTransformation: VisualTransformation,
    scrollState: ScrollState = rememberScrollState(),
) {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
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
                constraints = Constraints(maxWidth = width),
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
    val rowStartCharIndices = rememberLast(transformedText.text.length, transformedText.hashCode(), numOfCharsPerLine) {
        if (numOfCharsPerLine < 1) {
            return@rememberLast listOf(0)
        }
        val lineStartIndices = (
            sequenceOf(0) +
                LINE_BREAK_REGEX.findAll(transformedText.text).sortedBy { it.range.last }.map { it.range.last + 1 }
        ).toList()
        lineStartIndices.flatMapIndexed { index, it ->
            if (index + 1 <= lineStartIndices.lastIndex) {
                val numCharsInThisLine = lineStartIndices[index + 1] - it - (if (transformedText.text[lineStartIndices[index + 1] - 1] == '\n') 1 else 0)
                (0 until (numCharsInThisLine divRoundUp numOfCharsPerLine)).map { j ->
                    (it + j * numOfCharsPerLine).also { k ->
                        log.v { "calc index $index -> $it ($numCharsInThisLine, $numOfCharsPerLine) $k" }
                    }
                }
            } else {
                listOf(it)
            }
        }
    }.also {
//        log.v { "rowStartCharIndices = ${it}" }
    }

//    rememberLast(rowStartCharIndices.size) {
//        scrollState::class.declaredMemberProperties.first { it.name == "maxValue" }
//            .apply {
//                (this as KMutableProperty<Int>)
//                setter.isAccessible = true
//                setter.call(scrollState, ((rowStartCharIndices.size - 1) * lineHeight).roundToInt())
//            }
//    }

    var scrollOffset by remember { mutableStateOf(0f) }
//    val scrollState =
    val scrollableState = rememberScrollableState { delta ->
        scrollOffset = minOf(maxOf(0f, scrollOffset - delta), maxOf(0f, rowStartCharIndices.size * lineHeight - height))
        delta
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                width = it.size.width
                height = it.size.height
            }
            .clipToBounds()
            .scrollable(scrollableState, orientation = Orientation.Vertical)
    ) {
//        val viewportTop = scrollState.value.toFloat()
        val viewportTop = scrollOffset
        val viewportBottom = viewportTop + height
        if (lineHeight > 0) {
            val firstRowIndex = maxOf(0, (viewportTop / lineHeight).toInt())
            val lastRowIndex = minOf(rowStartCharIndices.lastIndex, (viewportBottom / lineHeight).toInt() + 1)
            log.v { "row index = [$firstRowIndex, $lastRowIndex]; scroll = $scrollOffset ~ $viewportBottom; line h = $lineHeight" }
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

//        VerticalScrollbar(
//            modifier = Modifier.align(Alignment.CenterEnd),
//            adapter = rememberScrollbarAdapter(scrollState, ((rowStartCharIndices.size - 1) * lineHeight).roundToInt()),
//        )
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

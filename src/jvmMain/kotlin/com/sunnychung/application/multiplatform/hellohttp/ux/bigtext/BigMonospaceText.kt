package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.isCtrlOrCmdPressed
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@Composable
fun BigMonospaceText(
    modifier: Modifier = Modifier,
    text: String,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    visualTransformation: VisualTransformation,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onTextLayout: ((BigTextLayoutResult) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = InefficientBigText(text),
    padding = padding,
    fontSize = fontSize,
    color = color,
    isSelectable = isSelectable,
    isEditable = false,
    onUpdateText = {},
    visualTransformation = visualTransformation,
    scrollState = scrollState,
    viewState = viewState,
    onTextLayout = onTextLayout,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoreBigMonospaceText(
    modifier: Modifier = Modifier,
    text: BigText,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    isEditable: Boolean = false,
    onUpdateText: (BigText) -> Unit,
    visualTransformation: VisualTransformation,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onTextLayout: ((BigTextLayoutResult) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val textSelectionColors = LocalTextSelectionColors.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val clipboardManager = LocalClipboardManager.current

    val focusRequester = remember { FocusRequester() }
    val textLayouter = remember { MonospaceTextLayouter() }

    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val contentWidth = width - with(density) {
        (padding.calculateStartPadding(LayoutDirection.Ltr) + padding.calculateEndPadding(LayoutDirection.Ltr)).toPx()
    }
    var lineHeight by remember { mutableStateOf(0f) }
    var charWidth by remember { mutableStateOf(0f) }
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
                charWidth = it.width / it.getLineEnd(0)
                it.getLineEnd(0)
            }
        } else {
            0
        }
    }
    val visualTransformationToUse = visualTransformation
    val transformedText = rememberLast(text.length, text.hashCode(), visualTransformationToUse) {
        visualTransformationToUse.filter(AnnotatedString(text.fullString())).also {
            log.v { "transformed text = `$it`" }
        }
    }
    val layoutResult = rememberLast(transformedText.text.length, transformedText.hashCode(), numOfCharsPerLine) {
        textLayouter.layout(
            text = text.fullString(),
            transformedText = transformedText,
            lineHeight = lineHeight,
            numOfCharsPerLine = numOfCharsPerLine,
        ).also {
            if (onTextLayout != null) {
                onTextLayout(it)
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

    rememberLast(viewState.selection.start, viewState.selection.last, visualTransformation) {
        viewState.transformedSelection = transformedText.offsetMapping.originalToTransformed(viewState.selection.start) ..
            transformedText.offsetMapping.originalToTransformed(viewState.selection.last)
    }

    val coroutineScope = rememberCoroutineScope() // for scrolling
    val scrollableState = rememberScrollableState { delta ->
        coroutineScope.launch {
            scrollState.scrollBy(-delta)
        }
        delta
    }
    var draggedPoint by remember { mutableStateOf<Offset>(Offset.Zero) }
    var selectionStart by remember { mutableStateOf<Int>(-1) }
    var selectionEnd by remember { mutableStateOf<Int>(-1) }
    var isHoldingShiftKey by remember { mutableStateOf(false) }

    val viewportTop = scrollState.value.toFloat()

    fun getTransformedCharIndex(x: Float, y: Float): Int {
        val row = ((viewportTop + y) / lineHeight).toInt()
        val col = (x / charWidth).toInt()
        if (row > layoutResult.rowStartCharIndices.lastIndex) {
            return transformedText.text.length - 1
        } else if (row < 0) {
            return 0
        }
        return minOf(
            layoutResult.rowStartCharIndices[row] + col,
            if (row + 1 <= layoutResult.rowStartCharIndices.lastIndex) {
                layoutResult.rowStartCharIndices[row + 1] - 1
            } else {
                transformedText.text.length - 1
            }
        )
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
            .focusRequester(focusRequester)
            .onDrag(
                enabled = isSelectable,
                onDragStart = {
                    log.v { "onDragStart ${it.x} ${it.y}" }
                    draggedPoint = it
                    val selectedCharIndex = getTransformedCharIndex(x = it.x, y = it.y)
                    selectionStart = selectedCharIndex
                    viewState.transformedSelection = selectedCharIndex .. selectedCharIndex
                    viewState.updateSelectionByTransformedSelection(transformedText)
                    focusRequester.requestFocus()
                    focusRequester.captureFocus()
                },
                onDrag = {
                    log.v { "onDrag ${it.x} ${it.y}" }
                    draggedPoint += it
                    val selectedCharIndex = getTransformedCharIndex(x = draggedPoint.x, y = draggedPoint.y)
                    selectionEnd = selectedCharIndex
                    viewState.transformedSelection = minOf(selectionStart, selectionEnd) .. maxOf(selectionStart, selectionEnd)
                    viewState.updateSelectionByTransformedSelection(transformedText)
                }
            )
            .pointerInput(layoutResult, scrollState.value, lineHeight, charWidth, transformedText.text.length, transformedText.text.hashCode()) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val position = event.changes.first().position
                                log.v { "press ${position.x} ${position.y}" }
                                if (isHoldingShiftKey) {
                                    selectionEnd = getTransformedCharIndex(x = position.x, y = position.y)
                                    log.v { "selectionEnd => $selectionEnd" }
                                    viewState.transformedSelection = minOf(selectionStart, selectionEnd) .. maxOf(selectionStart, selectionEnd)
                                    viewState.updateSelectionByTransformedSelection(transformedText)
                                } else {
                                    viewState.transformedSelection = IntRange.EMPTY
                                    focusRequester.freeFocus()
                                }
                            }
                        }
                    }
                }
            }
            .onFocusChanged { log.v { "BigMonospaceText onFocusChanged ${it.isFocused}" } }
            .onPreviewKeyEvent {
                log.v { "BigMonospaceText onPreviewKeyEvent" }
                when {
                    it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.key == Key.C && !viewState.transformedSelection.isEmpty() -> {
                        // Hit Ctrl-C or Cmd-C to copy
                        log.d { "BigMonospaceText hit copy" }
                        val textToCopy = text.substring(
                            viewState.selection.first.. viewState.selection.last
                        )
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        true
                    }
                    it.type == KeyEventType.KeyDown && it.key in listOf(Key.ShiftLeft, Key.ShiftRight) -> {
                        isHoldingShiftKey = true
                        false
                    }
                    it.type == KeyEventType.KeyUp && it.key in listOf(Key.ShiftLeft, Key.ShiftRight) -> {
                        isHoldingShiftKey = false
                        false
                    }
                    else -> false
                }
            }
            .focusable(isSelectable) // `focusable` should be after callback modifiers that use focus
    ) {
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
                    val yOffset = (- viewportTop + (i/* - firstRowIndex*/) * lineHeight).toDp()
                    if (viewState.hasSelection()) {
                        val intersection = viewState.transformedSelection intersect (startIndex .. endIndex - 1)
                        if (!intersection.isEmpty()) {
                            Box(
                                Modifier
                                    .height(lineHeight.toDp())
                                    .width((intersection.length * charWidth).toDp())
                                    .offset(x = ((intersection.start - startIndex) * charWidth).toDp(), y = yOffset)
                                    .background(color = textSelectionColors.backgroundColor) // `background` modifier must be after `offset` in order to take effect
                            )
                        }
                    }
                    BasicText(
                        text = transformedText.text.subSequence(
                            startIndex = startIndex,
                            endIndex = endIndex,
                        ),
                        style = textStyle,
                        maxLines = 1,
                        modifier = Modifier.offset(y = yOffset)
                    )
                }
            }
        }
    }
}

class BigTextViewState {
    var firstVisibleRow: Int by mutableStateOf(0)
        internal set

    var lastVisibleRow: Int by mutableStateOf(0)
        internal set

    internal var transformedSelection: IntRange by mutableStateOf(0 .. -1)

    var selection: IntRange by mutableStateOf(0 .. -1)

    fun hasSelection(): Boolean = !transformedSelection.isEmpty()

    internal fun updateSelectionByTransformedSelection(transformedText: TransformedText) {
        selection = transformedText.offsetMapping.transformedToOriginal(transformedSelection.first) ..
                transformedText.offsetMapping.transformedToOriginal(transformedSelection.last)
    }
}

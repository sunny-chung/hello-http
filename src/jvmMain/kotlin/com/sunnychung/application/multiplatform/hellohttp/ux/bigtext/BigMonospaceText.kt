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
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.isCtrlOrCmdPressed
import com.sunnychung.application.multiplatform.hellohttp.extension.toTextInput
import com.sunnychung.application.multiplatform.hellohttp.util.ComposeUnicodeCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.util.annotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
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
    textTransformation: IncrementalTextTransformation<*>? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = BigText.createFromLargeString(text), //InefficientBigText(text),
    padding = padding,
    fontSize = fontSize,
    color = color,
    isSelectable = isSelectable,
    isEditable = false,
    onTextChange = {},
    visualTransformation = visualTransformation,
    textTransformation = textTransformation,
    scrollState = scrollState,
    viewState = viewState,
    onTextLayout = onTextLayout,
)

@Composable
fun BigMonospaceText(
    modifier: Modifier = Modifier,
    text: BigTextImpl,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    visualTransformation: VisualTransformation,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTransformInit: ((BigTextTransformed) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = text,
    padding = padding,
    fontSize = fontSize,
    color = color,
    isSelectable = isSelectable,
    isEditable = false,
    onTextChange = {},
    visualTransformation = visualTransformation,
    textTransformation = textTransformation,
    textDecorator = textDecorator,
    scrollState = scrollState,
    viewState = viewState,
    onTextLayout = onTextLayout,
    onTransformInit = onTransformInit,
)

@Composable
fun BigMonospaceTextField(
    modifier: Modifier = Modifier,
    textFieldState: BigTextFieldState,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    visualTransformation: VisualTransformation,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
) {
    BigMonospaceTextField(
        modifier = modifier,
        text = textFieldState.text,
        padding = padding,
        fontSize = fontSize,
        color = color,
        onTextChange = {
            textFieldState.emitValueChange(it.changeId)
        },
        visualTransformation = visualTransformation,
        textTransformation = textTransformation,
        textDecorator = textDecorator,
        scrollState = scrollState,
        viewState = textFieldState.viewState,
        onTextLayout = onTextLayout
    )
}

@Composable
fun BigMonospaceTextField(
    modifier: Modifier = Modifier,
    text: BigText,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    onTextChange: (BigTextChangeEvent) -> Unit,
    visualTransformation: VisualTransformation,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember(text) { BigTextViewState() },
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = text as BigTextImpl,
    padding = padding,
    fontSize = fontSize,
    color = color,
    isSelectable = true,
    isEditable = true,
    onTextChange = onTextChange,
    visualTransformation = visualTransformation,
    textTransformation = textTransformation,
    textDecorator = textDecorator,
    scrollState = scrollState,
    viewState = viewState,
    onTextLayout = onTextLayout,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoreBigMonospaceText(
    modifier: Modifier = Modifier,
    text: BigTextImpl,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    isEditable: Boolean = false,
    onTextChange: (BigTextChangeEvent) -> Unit,
    visualTransformation: VisualTransformation,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember(text) { BigTextViewState() },
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTransformInit: ((BigTextTransformed) -> Unit)? = null,
) {
    log.d { "CoreBigMonospaceText recompose" }

    val density = LocalDensity.current
    val textSelectionColors = LocalTextSelectionColors.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val textInputService = LocalTextInputService.current

    val textStyle = LocalTextStyle.current.copy(
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = color,
    )

    val focusRequester = remember { FocusRequester() }
    val textLayouter = remember(density, fontFamilyResolver, textStyle) {
        MonospaceTextLayouter(
            TextMeasurer(
                fontFamilyResolver,
                density,
                LayoutDirection.Ltr,
            ), textStyle
        )
    }

    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val contentWidth = width - with(density) {
        (padding.calculateStartPadding(LayoutDirection.Ltr) + padding.calculateEndPadding(LayoutDirection.Ltr)).toPx()
    }
    var lineHeight by remember { mutableStateOf(0f) }
//    var charWidth by remember { mutableStateOf(0f) }
//    val numOfCharsPerLine = rememberLast(density.density, density.fontScale, fontSize, width) {
//        if (width > 0) {
//            Paragraph(
//                text = "0".repeat(1000),
//                style = textStyle,
//                constraints = Constraints(maxWidth = contentWidth.toInt()),
//                density = density,
//                fontFamilyResolver = fontFamilyResolver,
//            ).let {
//                lineHeight = it.getLineTop(1) - it.getLineTop(0)
//                charWidth = it.width / it.getLineEnd(0)
//                it.getLineEnd(0)
//            }
//        } else {
//            0
//        }
//    }

    val transformedText: BigTextTransformed = remember(text, textTransformation) {
        log.d { "CoreBigMonospaceText recreate BigTextTransformed" }
        BigTextTransformerImpl(text).also {
//            log.d { "transformedText = |${it.buildString()}|" }
            if (log.config.minSeverity <= Severity.Verbose) {
                it.printDebug("transformedText")
            }
        }
    }
    (transformedText as BigTextTransformerImpl).decorator = textDecorator

//    log.v { "text = |${text.buildString()}|" }
//    log.v { "transformedText = |${transformedText.buildString()}|" }

    fun fireOnLayout() {
        lineHeight = (textLayouter.charMeasurer as ComposeUnicodeCharMeasurer).getRowHeight()
        onTextLayout?.let { callback ->
            callback(BigTextSimpleLayoutResult(
//                text = text,
                text = transformedText, // layout is only performed in `transformedText`
                rowHeight = lineHeight,
            ))
        }
    }

    if (width > 0) {
        log.d { "CoreBigMonospaceText set contentWidth = $contentWidth" }
//        text.onLayoutCallback = {
//            fireOnLayout()
//        }
//        text.setLayouter(textLayouter)
//        text.setContentWidth(contentWidth)

        val startInstant = KInstant.now()

        transformedText.onLayoutCallback = {
            fireOnLayout()
        }
        transformedText.setLayouter(textLayouter)
        transformedText.setContentWidth(contentWidth)

        val endInstant = KInstant.now()
        log.d { "BigText layout took ${endInstant - startInstant}" }

        if (log.config.minSeverity <= Severity.Verbose) {
            (transformedText as BigTextImpl).printDebug("after init layout")
        }

        LaunchedEffect(Unit) {
            fireOnLayout()
        }
    }

//    val visualTransformationToUse = visualTransformation
//    val transformedText = rememberLast(text.length, text.hashCode(), visualTransformationToUse) {
//        visualTransformationToUse.filter(AnnotatedString(text.buildString())).also {
//            log.v { "transformed text = `$it`" }
//        }
//    }

//    val layoutResult = rememberLast(transformedText.text.length, transformedText.hashCode(), textStyle, lineHeight, contentWidth, textLayouter) {
//        textLayouter.layout(
//            text = text.fullString(),
//            transformedText = transformedText,
//            lineHeight = lineHeight,
//            contentWidth = contentWidth,
//        ).also {
//            if (onTextLayout != null) {
//                onTextLayout(it)
//            }
//        }
//    }
//    val rowStartCharIndices = layoutResult.rowStartCharIndices

    rememberLast(height, transformedText.numOfRows, lineHeight) {
        scrollState::class.declaredMemberProperties.first { it.name == "maxValue" }
            .apply {
                (this as KMutableProperty<Int>)
                setter.isAccessible = true
                val scrollableHeight = maxOf(
                    0f,
                    transformedText.numOfRows * lineHeight - height +
                        with (density) {
                            (padding.calculateTopPadding() + padding.calculateBottomPadding()).toPx()
                        }
                )
                setter.call(scrollState, scrollableHeight.roundToInt())
            }
    }

//    rememberLast(viewState.selection.start, viewState.selection.last, visualTransformation) {
//        viewState.transformedSelection = transformedText.offsetMapping.originalToTransformed(viewState.selection.start) ..
//            transformedText.offsetMapping.originalToTransformed(viewState.selection.last)
//    }

    val transformedState = remember(text, textTransformation) {
        log.v { "CoreBigMonospaceText text = |${text.buildString()}|" }
        if (textTransformation != null) {
            val startInstant = KInstant.now()
            textTransformation.initialize(text, transformedText).also {
                val endInstant = KInstant.now()
                log.d { "CoreBigMonospaceText init transformedState ${it.hashCode()} took ${endInstant - startInstant}" }
//                (transformedText as BigTextImpl).layout() // FIXME remove
                if (log.config.minSeverity <= Severity.Verbose) {
                    (transformedText as BigTextImpl).printDebug("init transformedState")
                }
                viewState.transformText = transformedText
                onTransformInit?.invoke(transformedText)
            }
        } else {
            null
        }
    }

    remember(text, textDecorator) {
        if (textDecorator != null) {
            val startInstant = KInstant.now()
            textDecorator.initialize(text).also {
                val endInstant = KInstant.now()
                log.d { "CoreBigMonospaceText init textDecorator took ${endInstant - startInstant}" }
            }
        }
    }

    if (textTransformation != null) {
        viewState.pollReapplyTransformCharRanges().forEach {
            log.d { "onReapplyTransform $it" }
            val startInstant = KInstant.now()
            (textTransformation as IncrementalTextTransformation<Any?>)
                .onReapplyTransform(text, it, transformedText, transformedState)
            log.d { "onReapplyTransform done ${KInstant.now() - startInstant}" }
        }
    }

    rememberLast(viewState.selection.start, viewState.selection.last, textTransformation) {
        viewState.transformedSelection = if (viewState.hasSelection()) {
            transformedText.findTransformedPositionByOriginalPosition(viewState.selection.start) ..
                    transformedText.findTransformedPositionByOriginalPosition(maxOf(0, viewState.selection.last))
        } else {
            IntRange.EMPTY
        }
    }

    val coroutineScope = rememberCoroutineScope() // for scrolling
    val scrollableState = rememberScrollableState { delta ->
        coroutineScope.launch {
            scrollState.scrollBy(-delta)
        }
        delta
    }
    var draggedPoint by remember { mutableStateOf<Offset>(Offset.Zero) }
    var selectionEnd by remember { mutableStateOf<Int>(-1) }
    var isHoldingShiftKey by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val viewportTop = scrollState.value.toFloat()

    fun getTransformedCharIndex(x: Float, y: Float, mode: ResolveCharPositionMode): Int {
        val row = ((viewportTop + y) / lineHeight).toInt()
        val maxIndex = maxOf(0, transformedText.length - if (mode == ResolveCharPositionMode.Selection) 1 else 0)
//        val col = (x / charWidth).toInt()
        if (row > transformedText.lastRowIndex) {
            return maxIndex
        } else if (row < 0) {
            return 0
        }
//        val numCharsInThisRow = if (row + 1 <= layoutResult.rowStartCharIndices.lastIndex) {
//            layoutResult.rowStartCharIndices[row + 1] - layoutResult.rowStartCharIndices[row] - 1
//        } else {
//            maxOf(0, transformedText.text.length - layoutResult.rowStartCharIndices[row] - if (mode == ResolveCharPositionMode.Selection) 1 else 0)
//        }
//        val charIndex = (layoutResult.rowStartCharIndices[row] .. layoutResult.rowStartCharIndices[row] + numCharsInThisRow).let { range ->
//            var accumWidth = 0f
//            range.first {
//                if (it < range.last) {
//                    accumWidth += layoutResult.findCharWidth(transformedText.text.substring(it..it))
//                }
//                return@first (x < accumWidth || it >= range.last)
//            }
//        }

        val rowString = transformedText.findRowString(row)
        val rowPositionStart = transformedText.findRowPositionStartIndexByRowIndex(row)
        var accumWidth = 0f
        val charIndex = rowString.indexOfFirst {
            accumWidth += textLayouter.charMeasurer.findCharWidth(it.toString())
            x < accumWidth
        }.takeIf { it >= 0 }
            ?: rowString.length - if (rowString.endsWith('\n')) 1 else 0

        return minOf(maxIndex, rowPositionStart + charIndex)
    }

    fun getTransformedStringWidth(start: Int, endExclusive: Int): Float {
        return (start .. endExclusive - 1)
            .map {
                val char = transformedText.substring(it..it).string()
                if (char == "\n") { // selecting \n shows a narrow width
                    textLayouter.charMeasurer.findCharWidth(" ")
                } else {
                    textLayouter.charMeasurer.findCharWidth(char)
                }
            }
            .sum()
    }

    fun generateChangeEvent(eventType: BigTextChangeEventType, changeStartIndex: Int, changeEndExclusiveIndex: Int) : BigTextChangeEvent {
        return BigTextChangeEvent(
            changeId = viewState.version,
            bigText = text,
            eventType = eventType,
            changeStartIndex = changeStartIndex,
            changeEndExclusiveIndex = changeEndExclusiveIndex,
        )
    }

    fun updateViewState() {
        viewState.lastVisibleRow = minOf(viewState.lastVisibleRow, transformedText.lastRowIndex)
        log.d { "lastVisibleRow = ${viewState.lastVisibleRow}, lastRowIndex = ${transformedText.lastRowIndex}" }
    }

    fun onValuePreChange(eventType: BigTextChangeEventType, changeStartIndex: Int, changeEndExclusiveIndex: Int) {
        viewState.version = Random.nextLong()
        val event = generateChangeEvent(eventType, changeStartIndex, changeEndExclusiveIndex)

        // invoke textTransformation listener before deletion, so that it knows what will be deleted and transform accordingly
        (textTransformation as? IncrementalTextTransformation<Any?>)?.beforeTextChange(
            event,
            transformedText,
            transformedState
        )
        textDecorator?.beforeTextChange(event)
    }

    fun onValuePostChange(eventType: BigTextChangeEventType, changeStartIndex: Int, changeEndExclusiveIndex: Int) {
        updateViewState()

        viewState.version = Random.nextLong()
        val event = generateChangeEvent(eventType, changeStartIndex, changeEndExclusiveIndex)
        (textTransformation as? IncrementalTextTransformation<Any?>)?.afterTextChange(
            event,
            transformedText,
            transformedState
        )
        textDecorator?.afterTextChange(event)
        onTextChange(event)
    }

    fun deleteSelection(isSaveUndoSnapshot: Boolean) {
        if (viewState.hasSelection()) {
            val start = viewState.selection.start
            val endExclusive = viewState.selection.endInclusive + 1
            onValuePreChange(BigTextChangeEventType.Delete, start, endExclusive)
            text.delete(start, endExclusive)
            if (isSaveUndoSnapshot) {
                text.recordCurrentChangeSequenceIntoUndoHistory()
            }
            onValuePostChange(BigTextChangeEventType.Delete, start, endExclusive)

            viewState.selection = EMPTY_SELECTION_RANGE // cannot use IntRange.EMPTY as `viewState.selection.start` is in use
            viewState.transformedSelection = EMPTY_SELECTION_RANGE
            viewState.cursorIndex = start
            viewState.updateTransformedCursorIndexByOriginal(transformedText)
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
        }
    }

    fun onType(textInput: String) {
        log.v { "key in '$textInput'" }
        if (viewState.hasSelection()) {
            deleteSelection(isSaveUndoSnapshot = false)
        }
        val insertPos = viewState.cursorIndex
        onValuePreChange(BigTextChangeEventType.Insert, insertPos, insertPos + textInput.length)
        text.insertAt(insertPos, textInput)
        text.recordCurrentChangeSequenceIntoUndoHistory()
        onValuePostChange(BigTextChangeEventType.Insert, insertPos, insertPos + textInput.length)
//        (transformedText as BigTextImpl).layout() // FIXME remove
        updateViewState()
        if (log.config.minSeverity <= Severity.Verbose) {
            (transformedText as BigTextImpl).printDebug("transformedText onType '${textInput.replace("\n", "\\n")}'")
        }
        // update cursor after invoking listeners, because a transformation or change may take place
        viewState.cursorIndex = minOf(text.length, insertPos + textInput.length)
        viewState.updateTransformedCursorIndexByOriginal(transformedText)
        viewState.transformedSelectionStart = viewState.transformedCursorIndex
        log.v { "set cursor pos 2 => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }
    }

    fun onDelete(direction: TextFBDirection): Boolean {
        val cursor = viewState.cursorIndex

        if (viewState.hasSelection()) {
            deleteSelection(isSaveUndoSnapshot = true)
            updateViewState()
            return true
        }

        when (direction) {
            TextFBDirection.Forward -> {
                if (cursor + 1 <= text.length) {
                    onValuePreChange(BigTextChangeEventType.Delete, cursor, cursor + 1)
                    text.delete(cursor, cursor + 1)
                    text.recordCurrentChangeSequenceIntoUndoHistory()
                    onValuePostChange(BigTextChangeEventType.Delete, cursor, cursor + 1)
                    updateViewState()
                    if (log.config.minSeverity <= Severity.Verbose) {
                        (transformedText as BigTextImpl).printDebug("transformedText onDelete $direction")
                    }
                    return true
                }
            }
            TextFBDirection.Backward -> {
                if (cursor - 1 >= 0) {
                    onValuePreChange(BigTextChangeEventType.Delete, cursor - 1, cursor)
                    text.delete(cursor - 1, cursor)
                    text.recordCurrentChangeSequenceIntoUndoHistory()
                    onValuePostChange(BigTextChangeEventType.Delete, cursor - 1, cursor)
                    updateViewState()
                    if (log.config.minSeverity <= Severity.Verbose) {
                        (transformedText as BigTextImpl).printDebug("transformedText onDelete $direction")
                    }
                    // update cursor after invoking listeners, because a transformation or change may take place
                    viewState.cursorIndex = maxOf(0, cursor - 1)
                    viewState.updateTransformedCursorIndexByOriginal(transformedText)
                    viewState.transformedSelectionStart = viewState.transformedCursorIndex
                    log.v { "set cursor pos 3 => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }
                    return true
                }
            }
        }
        return false
    }

    fun onUndoRedo(operation: (BigTextChangeCallback) -> Unit) {
        var lastChangeEnd = -1
        operation(object : BigTextChangeCallback {
            override fun onValuePreChange(
                eventType: BigTextChangeEventType,
                changeStartIndex: Int,
                changeEndExclusiveIndex: Int
            ) {
                onValuePreChange(eventType, changeStartIndex, changeEndExclusiveIndex)
            }

            override fun onValuePostChange(
                eventType: BigTextChangeEventType,
                changeStartIndex: Int,
                changeEndExclusiveIndex: Int
            ) {
                onValuePostChange(eventType, changeStartIndex, changeEndExclusiveIndex)
                lastChangeEnd = when (eventType) {
                    BigTextChangeEventType.Insert -> changeEndExclusiveIndex
                    BigTextChangeEventType.Delete -> changeStartIndex
                }
            }
        })
        if (lastChangeEnd >= 0) {
            viewState.cursorIndex = lastChangeEnd
            viewState.updateTransformedCursorIndexByOriginal(transformedText)
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
        }
    }

    fun undo() {
        onUndoRedo { text.undo(it) }
    }

    fun redo() {
        onUndoRedo { text.redo(it) }
    }

    fun copySelection() {
        if (!viewState.hasSelection()) {
            return
        }

        val textToCopy = text.substring(
            viewState.selection.first.. viewState.selection.last
        )
        clipboardManager.setText(textToCopy.annotatedString())
    }

    fun cutSelection() {
        if (!viewState.hasSelection()) {
            return
        }

        copySelection()
        deleteSelection(isSaveUndoSnapshot = true)
    }

    val tv = remember { TextFieldValue() } // this value is not used

    LaunchedEffect(transformedText) {
        if (log.config.minSeverity <= Severity.Verbose) {
            (0..text.length).forEach {
                log.v { "findTransformedPositionByOriginalPosition($it) = ${transformedText.findTransformedPositionByOriginalPosition(it)}" }
            }

            (0..transformedText.length).forEach {
                log.v { "findOriginalPositionByTransformedPosition($it) = ${transformedText.findOriginalPositionByTransformedPosition(it)}" }
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                width = it.size.width
                height = it.size.height
                layoutCoordinates = it
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
                    if (!isHoldingShiftKey) {
                        val selectedCharIndex = getTransformedCharIndex(x = it.x, y = it.y, mode = ResolveCharPositionMode.Selection)
                            .let {
                                viewState.roundedTransformedCursorIndex(it, CursorAdjustDirection.Bidirectional, transformedText, it, true)
                            }
                            .also { log.d { "onDragStart selected=$it" } }
                        viewState.transformedSelection = selectedCharIndex..selectedCharIndex
                        viewState.updateSelectionByTransformedSelection(transformedText)
                        viewState.transformedSelectionStart = selectedCharIndex
                    }
                    focusRequester.requestFocus()
//                    focusRequester.captureFocus()
                },
                onDrag = { // onDragStart happens before onDrag
                    log.v { "onDrag ${it.x} ${it.y}" }
                    draggedPoint += it
                    val selectionStart = viewState.transformedSelectionStart
                    val selectedCharIndex = getTransformedCharIndex(x = draggedPoint.x, y = draggedPoint.y, mode = ResolveCharPositionMode.Selection)
                        .let {
                            if (it >= selectionStart) {
                                viewState.roundedTransformedCursorIndex(it, CursorAdjustDirection.Forward, transformedText, it, true)
                            } else {
                                viewState.roundedTransformedCursorIndex(it, CursorAdjustDirection.Backward, transformedText, it, true)
                            }
                        }
                    selectionEnd = selectedCharIndex
                    viewState.transformedSelection = minOf(selectionStart, selectionEnd) .. maxOf(selectionStart, selectionEnd)
                    viewState.updateSelectionByTransformedSelection(transformedText)
                    viewState.transformedCursorIndex = minOf(
                        transformedText.length,
                        selectionEnd + if (selectionEnd == viewState.transformedSelection.last) 1 else 0
                    )
                    viewState.updateCursorIndexByTransformed(transformedText)
                }
            )
            .pointerInput(isEditable, text, transformedText.hasLayouted, viewState, viewportTop, lineHeight, contentWidth, transformedText.length, transformedText.hashCode()) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val position = event.changes.first().position
                                log.v { "press ${position.x} ${position.y} shift=$isHoldingShiftKey" }
                                if (isHoldingShiftKey) {
                                    val selectionStart = viewState.transformedSelectionStart
                                    selectionEnd = getTransformedCharIndex(x = position.x, y = position.y, mode = ResolveCharPositionMode.Selection)
                                        .let {
                                            viewState.roundedTransformedCursorIndex(it, CursorAdjustDirection.Bidirectional, transformedText, it, true)
                                        }
                                    log.v { "selectionEnd => $selectionEnd" }
                                    viewState.transformedSelection = minOf(selectionStart, selectionEnd) .. maxOf(selectionStart, selectionEnd)
                                    viewState.updateSelectionByTransformedSelection(transformedText)
                                } else {
                                    viewState.transformedSelection = IntRange.EMPTY
//                                    focusRequester.freeFocus()
                                }

                                viewState.transformedCursorIndex = getTransformedCharIndex(x = position.x, y = position.y, mode = ResolveCharPositionMode.Cursor)
                                viewState.roundTransformedCursorIndex(CursorAdjustDirection.Bidirectional, transformedText, viewState.transformedCursorIndex, true)
                                viewState.updateCursorIndexByTransformed(transformedText)
                                if (!isHoldingShiftKey) {
                                    // for selection, max possible index is 1 less than that for cursor
                                    viewState.transformedSelectionStart = getTransformedCharIndex(x = position.x, y = position.y, mode = ResolveCharPositionMode.Selection)
                                }
                                log.v { "set cursor pos 1 => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }

                                focusRequester.requestFocus()
                            }
                        }
                    }
                }
            }
            .onFocusChanged {
                log.v { "BigMonospaceText onFocusChanged ${it.isFocused}" }
                isFocused = it.isFocused
                if (isEditable) {
                    if (it.isFocused) {
                        val textInputSession = textInputService?.startInput(
                            tv,
                            ImeOptions.Default,
                            { ed ->
                                log.v { "onEditCommand [$ed] ${ed.joinToString { it::class.simpleName!! }} $tv" }
                                ed.forEach {
                                    when (it) {
                                        is CommitTextCommand -> {
                                            if (it.text.isNotEmpty()) {
                                                onType(it.text)
                                            }
                                        }
                                        is SetComposingTextCommand -> { // temporary text, e.g. SetComposingTextCommand(text='竹戈', newCursorPosition=1)
                                            // TODO
                                        }
                                    }
                                }
                            },
                            { a -> log.v { "onImeActionPerformed $a" } },
                        )
                        textInputSession?.notifyFocusedRect(
                            Rect(
                                layoutCoordinates!!.positionInRoot(),
                                Size(
                                    layoutCoordinates!!.size.width.toFloat(),
                                    layoutCoordinates!!.size.height.toFloat()
                                )
                            )
                        )
                        log.v { "started text input session" }
//                        keyboardController?.show()
                    } else {
//                        keyboardController?.hide()
                    }
                }
            }
            .onPreviewKeyEvent {
                log.v { "BigMonospaceText onPreviewKeyEvent" }
                when {
                    it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.key == Key.C && !viewState.transformedSelection.isEmpty() -> {
                        // Hit Ctrl-C or Cmd-C to copy
                        log.d { "BigMonospaceText hit copy" }
                        copySelection()
                        true
                    }
                    it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.key == Key.X && !viewState.transformedSelection.isEmpty() -> {
                        // Hit Ctrl-X or Cmd-X to cut
                        log.d { "BigMonospaceText hit cut" }
                        cutSelection()
                        true
                    }
                    isEditable && it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.key == Key.V -> {
                        // Hit Ctrl-V or Cmd-V to paste
                        log.d { "BigMonospaceTextField hit paste" }
                        val textToPaste = clipboardManager.getText()?.text
                        if (!textToPaste.isNullOrEmpty()) {
                            onType(textToPaste)
                            true
                        } else {
                            false
                        }
                    }
                    isEditable && it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && !it.isShiftPressed && it.key == Key.Z -> {
                        // Hit Ctrl-Z or Cmd-Z to undo
                        log.d { "BigMonospaceTextField hit undo" }
                        undo()
                        true
                    }
                    isEditable && it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.isShiftPressed && it.key == Key.Z -> {
                        // Hit Ctrl-Shift-Z or Cmd-Shift-Z to redo
                        log.d { "BigMonospaceTextField hit redo" }
                        redo()
                        true
                    }
                    /* selection */
                    it.type == KeyEventType.KeyDown && it.isCtrlOrCmdPressed() && it.key == Key.A -> {
                        // Hit Ctrl-A or Cmd-A to select all
                        if (text.isNotEmpty) {
                            viewState.selection = 0..text.lastIndex
                            viewState.updateTransformedSelectionBySelection(transformedText)
                        }
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
                    /* text input */
                    isEditable && it.isTypedEvent -> {
                        log.v { "key type '${it.key}'" }
                        val textInput = it.toTextInput()
                        if (textInput != null) {
                            onType(textInput)
                            true
                        } else {
                            false
                        }
                    }
                    isEditable && it.type == KeyEventType.KeyDown -> when {
                        it.key == Key.Enter && !it.isShiftPressed && !it.isCtrlPressed && !it.isAltPressed && !it.isMetaPressed -> {
                            onType("\n")
                            true
                        }
                        it.key == Key.Backspace -> {
                            onDelete(TextFBDirection.Backward)
                        }
                        it.key == Key.Delete -> {
                            onDelete(TextFBDirection.Forward)
                        }
                        it.key in listOf(Key.DirectionLeft, Key.DirectionRight) -> {
                            val delta = if (it.key == Key.DirectionRight) 1 else -1
                            viewState.transformedSelection = IntRange.EMPTY // TODO handle Shift key
                            if (viewState.transformedCursorIndex + delta in 0 .. transformedText.length) {
                                viewState.transformedCursorIndex += delta
                                if (delta > 0) {
                                    viewState.roundTransformedCursorIndex(CursorAdjustDirection.Forward, transformedText, viewState.transformedCursorIndex - delta, false)
                                } else {
                                    viewState.roundTransformedCursorIndex(CursorAdjustDirection.Backward, transformedText, viewState.transformedCursorIndex, true)
                                }
                                viewState.updateCursorIndexByTransformed(transformedText)
                                viewState.transformedSelectionStart = viewState.transformedCursorIndex
                                log.v { "set cursor pos LR => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }
                            }
                            true
                        }
                        it.key in listOf(Key.DirectionUp, Key.DirectionDown) -> {
//                            val row = layoutResult.rowStartCharIndices.binarySearchForMaxIndexOfValueAtMost(viewState.transformedCursorIndex)
                            val row = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
                            val newRow = row + if (it.key == Key.DirectionDown) 1 else -1
                            viewState.transformedSelection = IntRange.EMPTY // TODO handle Shift key
                            viewState.transformedCursorIndex = Unit.let {
                                if (newRow < 0) {
                                    0
                                } else if (newRow > transformedText.lastRowIndex) {
                                    transformedText.length
                                } else {
                                    val col = viewState.transformedCursorIndex - transformedText.findRowPositionStartIndexByRowIndex(row)
                                    val newRowLength = if (newRow + 1 <= transformedText.lastRowIndex) {
                                        transformedText.findRowPositionStartIndexByRowIndex(newRow + 1) - 1
                                    } else {
                                        transformedText.length
                                    } - transformedText.findRowPositionStartIndexByRowIndex(newRow)
                                    if (col <= newRowLength) {
                                        transformedText.findRowPositionStartIndexByRowIndex(newRow) + col
                                    } else {
                                        transformedText.findRowPositionStartIndexByRowIndex(newRow) + newRowLength
                                    }
                                }
                            }
                            viewState.roundTransformedCursorIndex(CursorAdjustDirection.Bidirectional, transformedText, viewState.transformedCursorIndex, true)
                            viewState.updateCursorIndexByTransformed(transformedText)
                            viewState.transformedSelectionStart = viewState.transformedCursorIndex
                            true
                        }
                        else -> false
                    }
                    else -> false
                }
            }
//            .then(BigTextInputModifierElement(1))
            .focusable(isSelectable) // `focusable` should be after callback modifiers that use focus
            .semantics {
                log.d { "semantic lambda" }
                if (isEditable) {
//                    editableText = AnnotatedString(text.buildString(), transformedText.text.spanStyles)
                    editableText = AnnotatedString(transformedText.buildString())
                    setText {
                        viewState.selection = 0 .. text.lastIndex
                        onType(it.text)
                        true
                    }
                    insertTextAtCursor {
                        onType(it.text)
                        true
                    }
                } else {
//                    this.text = AnnotatedString(text.buildString(), transformedText.text.spanStyles)
                    this.text = AnnotatedString(transformedText.buildString())
                    setText { false }
                    insertTextAtCursor { false }
                }
            }

    ) {
        val viewportBottom = viewportTop + height
        if (lineHeight > 0 && transformedText.hasLayouted) {
            val firstRowIndex = maxOf(0, (viewportTop / lineHeight).toInt())
            val lastRowIndex = minOf(transformedText.lastRowIndex, (viewportBottom / lineHeight).toInt() + 1)
            log.v { "row index = [$firstRowIndex, $lastRowIndex]; scroll = $viewportTop ~ $viewportBottom; line h = $lineHeight" }
            viewState.firstVisibleRow = firstRowIndex
            viewState.lastVisibleRow = lastRowIndex

            val startInstant = KInstant.now()

            with(density) {
                (firstRowIndex..lastRowIndex).forEach { i ->
                    val startIndex = transformedText.findRowPositionStartIndexByRowIndex(i)
                    val endIndex = if (i + 1 > transformedText.lastRowIndex) {
                        transformedText.length
                    } else {
                        transformedText.findRowPositionStartIndexByRowIndex(i + 1)
                    }
                    val nonVisualEndIndex = minOf(transformedText.length, maxOf(endIndex, startIndex + 1))
                    val cursorDisplayRangeEndIndex = if (i + 1 > transformedText.lastRowIndex) {
                        transformedText.length
                    } else {
                        maxOf(transformedText.findRowPositionStartIndexByRowIndex(i + 1) - 1, startIndex)
                    }
//                    log.v { "line #$i: [$startIndex, $endIndex)" }
                    val yOffset = (- viewportTop + (i/* - firstRowIndex*/) * lineHeight).toDp()
                    if (viewState.hasSelection()) {
                        val intersection = viewState.transformedSelection intersect (startIndex .. nonVisualEndIndex - 1)
                        if (!intersection.isEmpty()) {
                            log.v { "row #$i - intersection: $intersection" }
                            Box(
                                Modifier
                                    .height(lineHeight.toDp())
                                    .width(getTransformedStringWidth(intersection.start, intersection.endInclusive + 1).toDp())
                                    .offset(x = getTransformedStringWidth(startIndex, intersection.start).toDp(), y = yOffset)
                                    .background(color = textSelectionColors.backgroundColor) // `background` modifier must be after `offset` in order to take effect
                            )
                        }
                    }
                    val rowText = transformedText.subSequence(
                        startIndex = startIndex,
                        endIndex = endIndex,
                    )
                    log.v { "text R$i TT $startIndex ..< $endIndex: $rowText" }
                    BasicText(
                        text = rowText.annotatedString(),
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(y = yOffset)
                    )
                    if (isEditable && isFocused && viewState.transformedCursorIndex in startIndex .. cursorDisplayRangeEndIndex) {
                        var x = 0f
                        (startIndex + 1 .. viewState.transformedCursorIndex).forEach {
                            x += textLayouter.charMeasurer.findCharWidth(transformedText.substring(it - 1.. it - 1).string())
                        }
                        BigTextFieldCursor(
                            lineHeight = lineHeight.toDp(),
                            modifier = Modifier.offset(
                                x = x.toDp(),
                                y = yOffset,
                            )
                        )
                    }
                }
            }

            val endInstant = KInstant.now()
            log.d { "Declare BigText content for render took ${endInstant - startInstant}" }
        }
    }
}

private enum class ResolveCharPositionMode {
    Selection, Cursor
}

enum class TextFBDirection {
    Forward, Backward
}

enum class CursorAdjustDirection {
    Forward, Backward, Bidirectional
}

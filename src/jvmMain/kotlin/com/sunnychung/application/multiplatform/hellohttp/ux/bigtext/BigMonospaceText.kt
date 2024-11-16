package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.contains
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.isCtrlOrCmdPressed
import com.sunnychung.application.multiplatform.hellohttp.extension.toTextInput
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.util.ComposeUnicodeCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.util.annotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.ContextMenuView
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownDivider
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownKeyValue
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
    text: BigText,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    fontFamily: FontFamily = FontFamily.Monospace,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    inputFilter: BigTextInputFilter? = null,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember { BigTextViewState() },
    onPointerEvent: ((event: PointerEvent, tag: String?) -> Unit)? = null,
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTransformInit: ((BigTextTransformed) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = text,
    padding = padding,
    fontSize = fontSize,
    fontFamily = fontFamily,
    color = color,
    isSelectable = isSelectable,
    isEditable = false,
    onTextChange = {},
    inputFilter = inputFilter,
    textTransformation = textTransformation,
    textDecorator = textDecorator,
    scrollState = scrollState,
    viewState = viewState,
    onPointerEvent = onPointerEvent,
    onTextLayout = onTextLayout,
    onTransformInit = onTransformInit,
)

@Composable
fun BigMonospaceTextField(
    modifier: Modifier = Modifier,
    textFieldState: BigTextFieldState,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    fontFamily: FontFamily = FontFamily.Monospace,
    color: Color = LocalColor.current.text,
    inputFilter: BigTextInputFilter? = null,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    keyboardInputProcessor: BigTextKeyboardInputProcessor? = null,
    onPointerEvent: ((event: PointerEvent, tag: String?) -> Unit)? = null,
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTextManipulatorReady: ((BigTextManipulator) -> Unit)? = null,
) {
    BigMonospaceTextField(
        modifier = modifier,
        text = textFieldState.text,
        padding = padding,
        fontSize = fontSize,
        fontFamily = fontFamily,
        color = color,
        onTextChange = {
            textFieldState.emitValueChange(it.changeId)
        },
        inputFilter = inputFilter,
        textTransformation = textTransformation,
        textDecorator = textDecorator,
        scrollState = scrollState,
        viewState = textFieldState.viewState,
        keyboardInputProcessor = keyboardInputProcessor,
        onPointerEvent = onPointerEvent,
        onTextLayout = onTextLayout,
        onTextManipulatorReady = onTextManipulatorReady,
    )
}

@Composable
fun BigMonospaceTextField(
    modifier: Modifier = Modifier,
    text: BigText,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    fontFamily: FontFamily = FontFamily.Monospace,
    color: Color = LocalColor.current.text,
    onTextChange: (BigTextChangeEvent) -> Unit,
    inputFilter: BigTextInputFilter? = null,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember(text) { BigTextViewState() },
    keyboardInputProcessor: BigTextKeyboardInputProcessor? = null,
    onPointerEvent: ((event: PointerEvent, tag: String?) -> Unit)? = null,
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTextManipulatorReady: ((BigTextManipulator) -> Unit)? = null,
) = CoreBigMonospaceText(
    modifier = modifier,
    text = text,
    padding = padding,
    fontSize = fontSize,
    fontFamily = fontFamily,
    color = color,
    isSelectable = true,
    isEditable = true,
    onTextChange = onTextChange,
    inputFilter = inputFilter,
    textTransformation = textTransformation,
    textDecorator = textDecorator,
    scrollState = scrollState,
    viewState = viewState,
    keyboardInputProcessor = keyboardInputProcessor,
    onPointerEvent = onPointerEvent,
    onTextLayout = onTextLayout,
    onTextManipulatorReady = onTextManipulatorReady,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun CoreBigMonospaceText(
    modifier: Modifier = Modifier,
    text: BigText,
    padding: PaddingValues = PaddingValues(4.dp),
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    fontFamily: FontFamily = FontFamily.Monospace,
    color: Color = LocalColor.current.text,
    isSelectable: Boolean = false,
    isEditable: Boolean = false,
    onTextChange: (BigTextChangeEvent) -> Unit,
    inputFilter: BigTextInputFilter? = null,
    textTransformation: IncrementalTextTransformation<*>? = null,
    textDecorator: BigTextDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    viewState: BigTextViewState = remember(text) { BigTextViewState() },
    keyboardInputProcessor: BigTextKeyboardInputProcessor? = null,
    onPointerEvent: ((event: PointerEvent, tag: String?) -> Unit)? = null,
    onTextLayout: ((BigTextSimpleLayoutResult) -> Unit)? = null,
    onTextManipulatorReady: ((BigTextManipulator) -> Unit)? = null,
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
        fontFamily = fontFamily,
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
    var layoutResult by remember(textLayouter, width) { mutableStateOf<BigTextSimpleLayoutResult?>(null) }

    val transformedText: BigTextTransformed = remember(text, textTransformation) {
        log.d { "CoreBigMonospaceText recreate BigTextTransformed $text $textTransformation" }
        BigTextTransformerImpl(text)
            .let {
                if (text.isThreadSafe) {
                    ConcurrentBigTextTransformed(it)
                } else {
                    it
                }
            }
            .also {
//                log.d { "transformedText = |${it.buildString()}|" }
                if (log.config.minSeverity <= Severity.Verbose) {
                    it.printDebug("transformedText")
                }
            }
    }
    transformedText.decorator = textDecorator

//    log.v { "text = |${text.buildString()}|" }
//    log.v { "transformedText = |${transformedText.buildString()}|" }

    remember(text, viewState) {
        text.undoMetadataSupplier = {
            BigTextUndoMetadata(
                cursor = viewState.cursorIndex,
                selection = viewState.selection,
            )
        }
    }

    fun fireOnLayout() {
        lineHeight = (textLayouter.charMeasurer as ComposeUnicodeCharMeasurer).getRowHeight()
        onTextLayout?.let { callback ->
            callback(BigTextSimpleLayoutResult(
                text = transformedText, // layout is only performed in `transformedText`
                rowHeight = lineHeight,
            ).also { layoutResult = it })
        }
    }

    if (width > 0) {
        log.d { "CoreBigMonospaceText set contentWidth = $contentWidth" }

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

        scrollState::class.declaredMemberProperties.first { it.name == "viewportSize" }
            .apply {
                (this as KMutableProperty<Int>)
                setter.isAccessible = true
                setter.call(scrollState, height)
            }
    }

    val transformedState = remember(text, textTransformation) {
        log.v { "CoreBigMonospaceText text = |${text.buildString()}|" }
        if (textTransformation != null) {
            val startInstant = KInstant.now()
            textTransformation.initialize(text, transformedText).also {
                val endInstant = KInstant.now()
                log.d { "CoreBigMonospaceText init transformedState ${it.hashCode()} took ${endInstant - startInstant}" }
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

    var isShowContextMenu by remember { mutableStateOf(false) }

    val viewportTop = scrollState.value.toFloat()

    fun getTransformedCharIndex(x: Float, y: Float, mode: ResolveCharPositionMode): Int {
        val row = ((viewportTop + y) / lineHeight).toInt()
        val maxIndex = maxOf(0, transformedText.length - if (mode == ResolveCharPositionMode.Selection) 1 else 0)
        if (row > transformedText.lastRowIndex) {
            return maxIndex
        } else if (row < 0) {
            return 0
        }

        val rowString = transformedText.findRowString(row)
        val rowPositionStart = transformedText.findRowPositionStartIndexByRowIndex(row)
        var accumWidth = 0f
        val charIndex = rowString.indexOfFirst {
            accumWidth += textLayouter.charMeasurer.findCharWidth(it.toString())
            x < accumWidth
        }.takeIf { it >= 0 }
            ?: (rowString.length - if (rowString.endsWith('\n')) 1 else 0)

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

    fun scrollToCursor() {
        val layoutResult = layoutResult ?: return

        // scroll to cursor position if out of visible range
        val visibleVerticalRange = scrollState.value .. scrollState.value + height
        val row = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
        val rowVerticalRange = layoutResult.getTopOfRow(row).toInt() .. layoutResult.getBottomOfRow(row).toInt()
        if (rowVerticalRange !in visibleVerticalRange) {
            val scrollToPosition = if (rowVerticalRange.start < visibleVerticalRange.start) {
                rowVerticalRange.start
            } else {
                // scroll to a position that includes the bottom of the row + a little space
                minOf(layoutResult.bottom.toInt(), maxOf(0, rowVerticalRange.endInclusive + maxOf(2, (layoutResult.rowHeight * 0.5).toInt()) - height))
            }
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollToPosition)
            }
        }
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
        log.d { "call onTextChange for ${event.changeId}" }
        onTextChange(event)
    }

    fun delete(start: Int, endExclusive: Int) {
        if (start >= endExclusive) {
            return
        }
        onValuePreChange(BigTextChangeEventType.Delete, start, endExclusive)
        text.delete(start, endExclusive)
        onValuePostChange(BigTextChangeEventType.Delete, start, endExclusive)
    }

    fun deleteSelection(isSaveUndoSnapshot: Boolean) {
        if (viewState.hasSelection()) {
            val start = viewState.selection.start
            val endExclusive = viewState.selection.endInclusive + 1
            delete(start, endExclusive)

            viewState.selection = EMPTY_SELECTION_RANGE // cannot use IntRange.EMPTY as `viewState.selection.start` is in use
            viewState.transformedSelection = EMPTY_SELECTION_RANGE
            viewState.cursorIndex = start
            viewState.updateTransformedCursorIndexByOriginal(transformedText)
            viewState.transformedSelectionStart = viewState.transformedCursorIndex

            if (isSaveUndoSnapshot) {
                text.recordCurrentChangeSequenceIntoUndoHistory()
            }
        }
    }

    fun insertAt(insertPos: Int, textInput: CharSequence) {
        val textInput = inputFilter?.filter(textInput) ?: textInput
        onValuePreChange(BigTextChangeEventType.Insert, insertPos, insertPos + textInput.length)
        text.insertAt(insertPos, textInput)
        onValuePostChange(BigTextChangeEventType.Insert, insertPos, insertPos + textInput.length)
    }

    fun onType(textInput: CharSequence, isSaveUndoSnapshot: Boolean = true) {
        log.i { "$text key in '$textInput' ${viewState.hasSelection()} ${viewState.selection} ${viewState.transformedSelection}" }
        if (viewState.hasSelection()) {
            deleteSelection(isSaveUndoSnapshot = false)
        }
        val insertPos = viewState.cursorIndex
        insertAt(insertPos, textInput)
        updateViewState()
        if (log.config.minSeverity <= Severity.Verbose) {
            (transformedText as BigTextImpl).printDebug("transformedText onType '${textInput.string().replace("\n", "\\n")}'")
        }
        // update cursor after invoking listeners, because a transformation or change may take place
        viewState.cursorIndex = minOf(text.length, insertPos + textInput.length)
        viewState.updateTransformedCursorIndexByOriginal(transformedText)
        viewState.transformedSelectionStart = viewState.transformedCursorIndex
        log.v { "set cursor pos 2 => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }
        if (isSaveUndoSnapshot) {
            text.recordCurrentChangeSequenceIntoUndoHistory()
        }
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
                    onValuePostChange(BigTextChangeEventType.Delete, cursor, cursor + 1)
                    updateViewState()
                    if (log.config.minSeverity <= Severity.Verbose) {
                        (transformedText as BigTextImpl).printDebug("transformedText onDelete $direction")
                    }
                    text.recordCurrentChangeSequenceIntoUndoHistory()
                    return true
                }
            }
            TextFBDirection.Backward -> {
                if (cursor - 1 >= 0) {
                    onValuePreChange(BigTextChangeEventType.Delete, cursor - 1, cursor)
                    text.delete(cursor - 1, cursor)
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
                    text.recordCurrentChangeSequenceIntoUndoHistory()
                    return true
                }
            }
        }
        return false
    }

    fun onUndoRedo(operation: (BigTextChangeCallback) -> Pair<Boolean, Any?>) {
        var lastChangeEnd = -1
        val stateToBeRestored = operation(object : BigTextChangeCallback {
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
        updateViewState()
        (stateToBeRestored.second as? BigTextUndoMetadata)?.let { state ->
            viewState.selection = state.selection
            viewState.updateTransformedSelectionBySelection(transformedText)
            viewState.cursorIndex = state.cursor
            viewState.updateTransformedCursorIndexByOriginal(transformedText)
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
            scrollToCursor()
            return
        }
        if (lastChangeEnd >= 0) { // this `if` should never execute
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

    fun paste(): Boolean {
        val textToPaste = clipboardManager.getText()?.text
        return if (!textToPaste.isNullOrEmpty()) {
            onType(textToPaste)
            true
        } else {
            false
        }
    }

    fun selectAll() {
        if (text.isNotEmpty) {
            viewState.selection = 0..text.lastIndex
            viewState.updateTransformedSelectionBySelection(transformedText)
        }
    }

    fun findPreviousWordBoundaryPositionFromCursor(isIncludeCursorPosition: Boolean = false): Int {
        val currentRowIndex = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
        val transformedRowStart = transformedText.findRowPositionStartIndexByRowIndex(currentRowIndex)
        val rowStart = transformedText.findOriginalPositionByTransformedPosition(transformedRowStart)
        val end = minOf(text.length, viewState.cursorIndex + if (isIncludeCursorPosition) 1 else 0)
        val substringFromRowStartToCursor = text.substring(rowStart, end)
        if (substringFromRowStartToCursor.isEmpty()) {
            return maxOf(0, rowStart - 1)
        }
        val wordBoundaryAt = "\\b".toRegex().findAll(substringFromRowStartToCursor)
            .filter { it.range.start < substringFromRowStartToCursor.length }
            .lastOrNull()?.range?.start ?: 0
        return rowStart + wordBoundaryAt
    }

    fun findNextWordBoundaryPositionFromCursor(): Int {
        val currentRowIndex = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
        val transformedRowEnd = if (currentRowIndex + 1 <= transformedText.lastRowIndex) {
            transformedText.findRowPositionStartIndexByRowIndex(currentRowIndex + 1)
        } else {
            transformedText.length
        }
        val rowEnd = transformedText.findOriginalPositionByTransformedPosition(transformedRowEnd)
        val substringFromCursorToRowEnd = text.substring(viewState.cursorIndex, rowEnd)
        if (substringFromCursorToRowEnd.isEmpty()) {
            return minOf(text.length, rowEnd)
        }
        val wordBoundaryAt = "\\b".toRegex().findAll(substringFromCursorToRowEnd)
            .filter { it.range.start > 0 }
            .firstOrNull()?.range?.start ?: substringFromCursorToRowEnd.length
        return viewState.cursorIndex + wordBoundaryAt
    }

    fun updateOriginalCursorOrSelection(newPosition: Int, isSelection: Boolean) {
        val oldCursorPosition = viewState.cursorIndex
        viewState.cursorIndex = newPosition
        viewState.updateTransformedCursorIndexByOriginal(transformedText)
        if (isSelection) {
            val selectionStart = if (viewState.hasSelection()) {
                transformedText.findOriginalPositionByTransformedPosition(viewState.transformedSelectionStart)
            } else {
                oldCursorPosition
            }
            viewState.selection = minOf(selectionStart, newPosition) until maxOf(selectionStart, newPosition)
            viewState.updateTransformedSelectionBySelection(transformedText)
        } else {
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
            viewState.transformedSelection = IntRange.EMPTY
        }
        scrollToCursor()
    }

    fun updateTransformedCursorOrSelection(newTransformedPosition: Int, isSelection: Boolean) {
        val oldTransformedCursorPosition = viewState.transformedCursorIndex
        viewState.transformedCursorIndex = newTransformedPosition
        viewState.updateCursorIndexByTransformed(transformedText)
        if (isSelection) {
            val selectionTransformedStart = if (viewState.hasSelection()) {
                viewState.transformedSelectionStart
            } else {
                oldTransformedCursorPosition
            }
            log.d { "select T $selectionTransformedStart ~ $newTransformedPosition" }
            viewState.transformedSelection = minOf(selectionTransformedStart, newTransformedPosition) until maxOf(selectionTransformedStart, newTransformedPosition)
            viewState.updateSelectionByTransformedSelection(transformedText)
        } else {
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
            viewState.transformedSelection = IntRange.EMPTY
        }
        scrollToCursor()
    }

    fun processKeyboardInput(it: KeyEvent): Boolean {
        return when {
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
                paste()
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
                selectAll()
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
                it.key == Key.Backspace -> when {
                    (currentOS() == MacOS && it.isAltPressed) ||
                        (currentOS() != MacOS && it.isCtrlPressed) -> {
                            // delete previous word
                            val previousWordPosition = findPreviousWordBoundaryPositionFromCursor()
                            if (previousWordPosition >= viewState.cursorIndex) {
                                return false
                            }
                            delete(previousWordPosition, viewState.cursorIndex)
                            updateViewState()
                            // update cursor after invoking listeners, because a transformation or change may take place
                            viewState.cursorIndex = previousWordPosition
                            viewState.updateTransformedCursorIndexByOriginal(transformedText)
                            viewState.transformedSelectionStart = viewState.transformedCursorIndex
                            text.recordCurrentChangeSequenceIntoUndoHistory()
                            true
                        }
                    else -> onDelete(TextFBDirection.Backward)
                }
                it.key == Key.Delete -> {
                    onDelete(TextFBDirection.Forward)
                }
                /* text navigation */
                (currentOS() == MacOS && it.isMetaPressed && it.key == Key.DirectionUp) ||
                        (currentOS() != MacOS && it.isCtrlPressed && it.key == Key.MoveHome) -> {
                    updateOriginalCursorOrSelection(newPosition = 0, isSelection = it.isShiftPressed)
                    true
                }
                (currentOS() == MacOS && it.isMetaPressed && it.key == Key.DirectionDown) ||
                        (currentOS() != MacOS && it.isCtrlPressed && it.key == Key.MoveEnd) -> {
                    updateOriginalCursorOrSelection(newPosition = text.length, isSelection = it.isShiftPressed)
                    true
                }
                (currentOS() == MacOS && it.isMetaPressed && it.key in listOf(Key.DirectionLeft, Key.DirectionRight)) ||
                        it.key in listOf(Key.MoveHome, Key.MoveEnd) -> {
                    // use `transformedText` as basis because `text` does not perform layout
                    val currentRowIndex = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
                    val newTransformedPosition = if (it.key in listOf(Key.DirectionLeft, Key.MoveHome)) {
                        // home -> move to start of row
                        log.d { "move to start of row $currentRowIndex" }
                        transformedText.findRowPositionStartIndexByRowIndex(currentRowIndex)
                    } else {
                        // end -> move to end of row
                        log.d { "move to end of row $currentRowIndex" }
                        if (currentRowIndex + 1 <= transformedText.lastRowIndex) {
                            transformedText.findRowPositionStartIndexByRowIndex(currentRowIndex + 1) - /* the '\n' char */ 1
                        } else {
                            transformedText.length
                        }
                    }
                    updateTransformedCursorOrSelection(
                        newTransformedPosition = newTransformedPosition,
                        isSelection = it.isShiftPressed,
                    )
                    true
                }
                it.key == Key.DirectionLeft && (
                        (currentOS() == MacOS && it.isAltPressed) ||
                                (currentOS() != MacOS && it.isCtrlPressed)
                        ) -> {
                    val newPosition = findPreviousWordBoundaryPositionFromCursor()
                    updateOriginalCursorOrSelection(newPosition = newPosition, isSelection = it.isShiftPressed)
                    true
                }
                it.key == Key.DirectionRight && (
                        (currentOS() == MacOS && it.isAltPressed) ||
                                (currentOS() != MacOS && it.isCtrlPressed)
                        ) -> {
                    val newPosition = findNextWordBoundaryPositionFromCursor()
                    updateOriginalCursorOrSelection(newPosition = newPosition, isSelection = it.isShiftPressed)
                    true
                }
                it.key in listOf(Key.DirectionLeft, Key.DirectionRight) -> {
                    val delta = if (it.key == Key.DirectionRight) 1 else -1
                    if (viewState.transformedCursorIndex + delta in 0 .. transformedText.length) {
                        var newTransformedPosition = viewState.transformedCursorIndex + delta
                        newTransformedPosition = if (delta > 0) {
                            viewState.roundedTransformedCursorIndex(newTransformedPosition, CursorAdjustDirection.Forward, transformedText, viewState.transformedCursorIndex /* FIXME IndexOutOfBoundsException */, false)
                        } else {
                            viewState.roundedTransformedCursorIndex(newTransformedPosition, CursorAdjustDirection.Backward, transformedText, newTransformedPosition, true)
                        }
                        updateTransformedCursorOrSelection(
                            newTransformedPosition = newTransformedPosition,
                            isSelection = it.isShiftPressed,
                        )
                        log.v { "set cursor pos LR => ${viewState.cursorIndex} t ${viewState.transformedCursorIndex}" }
                    }
                    true
                }
                it.key in listOf(Key.DirectionUp, Key.DirectionDown) -> {
                    val row = transformedText.findRowIndexByPosition(viewState.transformedCursorIndex)
                    val newRow = row + if (it.key == Key.DirectionDown) 1 else -1
                    var newTransformedPosition = Unit.let {
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
                            val pos = if (col <= newRowLength) {
                                transformedText.findRowPositionStartIndexByRowIndex(newRow) + col
                            } else {
                                transformedText.findRowPositionStartIndexByRowIndex(newRow) + newRowLength
                            }
                            viewState.roundedTransformedCursorIndex(pos, CursorAdjustDirection.Bidirectional, transformedText, viewState.transformedCursorIndex, true)
                        }
                    }
                    updateTransformedCursorOrSelection(
                        newTransformedPosition = newTransformedPosition,
                        isSelection = it.isShiftPressed,
                    )
                    true
                }
                else -> false
            }
            else -> false
        }
    }

    class BigTextManipulatorImpl(val onTextManipulated: (() -> Unit)? = null) : BigTextManipulator {
        var hasManipulatedText = false
            private set

        override fun append(text: CharSequence) {
            hasManipulatedText = true
            insertAt(text.length, text)
            onTextManipulated?.invoke()
        }

        override fun insertAt(pos: Int, text: CharSequence) {
            hasManipulatedText = true
            insertAt(pos, text)
            onTextManipulated?.invoke()
        }

        override fun replaceAtCursor(text: CharSequence) {
            hasManipulatedText = true
            onType(text, isSaveUndoSnapshot = false) // save undo snapshot at the end
            onTextManipulated?.invoke()
        }

        override fun delete(range: IntRange) {
            hasManipulatedText = true
            delete(range.start, range.endInclusive + 1)
            onTextManipulated?.invoke()
        }

        override fun replace(range: IntRange, text: CharSequence) {
            hasManipulatedText = true
            delete(range.start, range.endInclusive + 1)
            insertAt(range.start, text)
            onTextManipulated?.invoke()
        }

        override fun setCursorPosition(position: Int) {
            require(position in 0 .. text.length) { "Cursor position $position is out of range. Text length: ${text.length}" }
            viewState.cursorIndex = position
            viewState.updateTransformedCursorIndexByOriginal(transformedText)
            viewState.transformedSelectionStart = viewState.transformedCursorIndex
            scrollToCursor()
        }

        override fun setSelection(range: IntRange) {
            require(range.start in 0 .. text.length) { "Range start ${range.start} is out of range. Text length: ${text.length}" }
            require(range.endInclusive + 1 in 0 .. text.length) { "Range end ${range.endInclusive} is out of range. Text length: ${text.length}" }

            viewState.selection = range
            viewState.updateTransformedSelectionBySelection(transformedText)
        }
    }

    fun onProcessKeyboardInput(keyEvent: KeyEvent): Boolean {
        val textManipulator = BigTextManipulatorImpl()

        try {
            if (keyboardInputProcessor?.beforeProcessInput(keyEvent, viewState, textManipulator) == true) {
                return true
            }
            var result = processKeyboardInput(keyEvent)
            if (keyboardInputProcessor?.afterProcessInput(keyEvent, viewState, textManipulator) == true) {
                result = true
            }
            return result

        } finally {
            if (textManipulator.hasManipulatedText) {
                updateViewState()
                text.recordCurrentChangeSequenceIntoUndoHistory()
            }
        }
    }

    remember(text, onTextManipulatorReady) {
        onTextManipulatorReady?.invoke(BigTextManipulatorImpl {
            updateViewState()
            text.recordCurrentChangeSequenceIntoUndoHistory()
        })
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
                    if (transformedText.isEmpty) {
                        viewState.transformedSelection = IntRange.EMPTY
                        viewState.selection = EMPTY_SELECTION_RANGE
                        viewState.transformedCursorIndex = 0
                        viewState.cursorIndex = 0
                        return@onDrag
                    }
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
                    viewState.transformedSelection = minOf(selectionStart, selectionEnd) until maxOf(selectionStart, selectionEnd)
                    log.d { "t sel = ${viewState.transformedSelection}" }
                    viewState.updateSelectionByTransformedSelection(transformedText)
                    viewState.transformedCursorIndex = minOf(
                        transformedText.length,
                        selectionEnd + if (selectionEnd == viewState.transformedSelection.last) 1 else 0
                    )
                    viewState.updateCursorIndexByTransformed(transformedText)
                }
            )
            .pointerInput(isEditable, text, transformedText.hasLayouted, viewState, viewportTop, lineHeight, contentWidth, transformedText.length, transformedText.hashCode(), onPointerEvent) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        if (onPointerEvent != null) {
                            val position = event.changes.first().position
                            val transformedCharIndex = getTransformedCharIndex(x = position.x, y = position.y, mode = ResolveCharPositionMode.Cursor)
                            val tag = if (transformedCharIndex in 0 .. transformedText.lastIndex) {
                                val charSequenceUnderPointer = transformedText.subSequence(transformedCharIndex, transformedCharIndex + 1)
                                (charSequenceUnderPointer as? AnnotatedString)?.spanStyles?.firstOrNull { it.tag.isNotEmpty() }?.tag
                            } else null
                            onPointerEvent(event, tag)
                        }

                        when (event.type) {
                            PointerEventType.Press -> {
                                val position = event.changes.first().position
                                log.v { "press ${position.x} ${position.y} shift=$isHoldingShiftKey" }

                                if (event.button == PointerButton.Secondary) {
                                    isShowContextMenu = !isShowContextMenu
                                    continue
                                }

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
                                    viewState.selection = EMPTY_SELECTION_RANGE
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
            .pointerInput(transformedText, transformedText.hasLayouted, viewportTop, lineHeight, contentWidth, viewState) {
                detectTapGestures(onDoubleTap = {
                    val wordStart = findPreviousWordBoundaryPositionFromCursor(isIncludeCursorPosition = true)
                    val wordEndExclusive = findNextWordBoundaryPositionFromCursor()
                    viewState.selection = wordStart until wordEndExclusive
                    viewState.updateTransformedSelectionBySelection(transformedText)
                    viewState.cursorIndex = wordEndExclusive
                    viewState.updateTransformedCursorIndexByOriginal(transformedText)
                })
            }
            .onFocusChanged {
                log.v { "BigMonospaceText onFocusChanged ${it.isFocused} ${it.hasFocus} ${it.isCaptured}" }
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
                log.v { "BigMonospaceText onPreviewKeyEvent ${it.type} ${it.key} ${it.key.nativeKeyCode} ${it.key.keyCode}" }
                onProcessKeyboardInput(it)
            }
//            .then(BigTextInputModifierElement(1))
            .focusable(isSelectable) // `focusable` should be after callback modifiers that use focus
            .semantics {
                log.d { "semantic lambda" }
                if (isEditable) {
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

        ContextMenuView(
            isShowContextMenu = isShowContextMenu,
            onDismissRequest = { isShowContextMenu = false },
            colors = LocalColor.current,
            testTagParts = null,
            populatedItems = listOf(
                DropDownKeyValue(ContextMenuItem.Copy, "Copy", viewState.hasSelection()),
                DropDownKeyValue(ContextMenuItem.Paste, "Paste", clipboardManager.hasText()),
                DropDownKeyValue(ContextMenuItem.Cut, "Cut", viewState.hasSelection()),
                DropDownDivider,
                DropDownKeyValue(ContextMenuItem.Undo, "Undo", text.isUndoable()),
                DropDownKeyValue(ContextMenuItem.Redo, "Redo", text.isRedoable()),
                DropDownDivider,
                DropDownKeyValue(ContextMenuItem.SelectAll, "Select All", text.isNotEmpty),
            ),
            onClickItem = {
                when (it.key as ContextMenuItem) {
                    ContextMenuItem.Copy -> copySelection()
                    ContextMenuItem.Paste -> paste()
                    ContextMenuItem.Cut -> cutSelection()
                    ContextMenuItem.Undo -> undo()
                    ContextMenuItem.Redo -> redo()
                    ContextMenuItem.SelectAll -> selectAll()
                }
                true
            },
            selectedItem = null,
            isClickable = true,
        )
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

private enum class ContextMenuItem {
    Copy, Paste, Cut, Undo, Redo, SelectAll
}

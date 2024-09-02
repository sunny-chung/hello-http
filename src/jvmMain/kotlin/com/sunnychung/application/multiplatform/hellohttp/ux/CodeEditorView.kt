package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.annotation.TemporaryApi
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForInsertionPoint
import com.sunnychung.application.multiplatform.hellohttp.extension.contains
import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigMonospaceText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigMonospaceTextField
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextLayoutResult
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextSimpleLayoutResult
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextViewState
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.rememberBigTextFieldState
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.CollapseTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.FunctionTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.MultipleVisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.SearchHighlightTransformation
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import kotlin.random.Random

val MAX_TEXT_FIELD_LENGTH = 4 * 1024 * 1024 // 4 MB

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    cacheKey: String,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    collapsableLines: List<IntRange> = emptyList(),
    collapsableChars: List<IntRange> = emptyList(),
    textColor: Color = LocalColor.current.text,
    transformations: List<VisualTransformation> = emptyList(),
    isEnableVariables: Boolean = false,
    knownVariables: Set<String> = setOf(),
    testTag: String? = null,
) {
    val colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField
    )
    val themeColours = LocalColor.current
    val coroutineScope = rememberCoroutineScope()

    // Replace "\r\n" by "\n" because to workaround the issue:
    // https://github.com/JetBrains/compose-multiplatform/issues/3877
    fun String.filterForTextField() = replace("\r\n", "\n")

    var textValue by remember { mutableStateOf(TextFieldValue(text = text.filterForTextField())) }
    var cursorDelta by remember { mutableStateOf(0) }
    val newText = text.filterForTextField().let {
        if (isReadOnly && it.length > MAX_TEXT_FIELD_LENGTH) {
            it.substring(0 .. MAX_TEXT_FIELD_LENGTH - 1) + "\n... (trimmed. total ${it.length} bytes)"
        } else {
            it
        }
    }
    var textLayoutResult by rememberLast(newText) { mutableStateOf<TextLayoutResult?>(null) }
    var lineTops by rememberLast(newText, textLayoutResult) { mutableStateOf<List<Float>?>(null) }
    log.d { "len newText ${newText.length}, textValue.text ${textValue.text.length}, text ${text.length}" }
    if (newText != textValue.text) {
        log.d { "CodeEditorView replace text len ${textValue.text.length} -> ${newText.length}" }
        textValue = textValue.copy(text = newText)
        lineTops = null // recalculate
        textLayoutResult = null
    }
    if (cursorDelta > 0) {
        textValue = textValue.copy(
            selection = TextRange(
                textValue.selection.start + cursorDelta,
                textValue.selection.end + cursorDelta
            )
        )
        cursorDelta = 0
    }

    var collapsedLines = rememberLast(newText) { mutableStateMapOf<IntRange, IntRange>() }
    var collapsedChars = rememberLast(newText) { mutableStateMapOf<IntRange, IntRange>() }

    log.d { "CodeEditorView recompose" }

    fun onPressEnterAddIndent() {
        val cursorPos = textValue.selection.min
        assert(textValue.selection.length == 0)

        log.d { "onPressEnterAddIndent" }

        val text = textValue.text
        var lastLineStart = getLineStart(text, cursorPos)
        var spacesMatch = "^(\\s+)".toRegex().matchAt(text.substring(lastLineStart, cursorPos), 0)
        val newSpaces = "\n" + (spacesMatch?.groups?.get(1)?.value ?: "")
        log.d { "onPressEnterAddIndent add ${newSpaces.length} spaces. current cursor $cursorPos" }
//        textValue = textValue.copy(selection = TextRange(cursorPos + newSpaces.length)) // no use
        cursorDelta += newSpaces.length
        onTextChange?.invoke(text.insert(cursorPos, newSpaces))
    }

    log.v { "cursor at ${textValue.selection}" }
    fun onPressTab(isShiftPressed: Boolean) {
        val selection = textValue.selection
        val text = textValue.text
        if (selection.length == 0) {
            val cursorPos = selection.min
            val newSpaces = " ".repeat(4)
            textValue = textValue.copy(selection = TextRange(cursorPos + newSpaces.length))
            onTextChange?.invoke(text.insert(cursorPos, newSpaces))
        } else if (!isShiftPressed) { // select text and press tab to insert 1-level indent to lines
            val lineStarts = getAllLineStartsInRegion(
                text = text,
                from = selection.min,
                to = selection.max - 1,
            )
            log.v { "lineStarts = $lineStarts" }
            val newSpaces = " ".repeat(4)
            var s = text
            for (i in lineStarts.size - 1 downTo 0) {
                val it = lineStarts[i]
                s = s.insert(it, newSpaces)
            }

            val (minOffset, maxOffset) = Pair(newSpaces.length, newSpaces.length * lineStarts.size)
            log.d { "off = $minOffset, $maxOffset" }
            textValue = textValue.copy(
                text = s,
                selection = TextRange(
                    start = selection.start + if (!selection.reversed) minOffset else maxOffset,
                    end = selection.end + if (!selection.reversed) maxOffset else minOffset,
                )
            )

            onTextChange?.invoke(s)
        } else { // select text and press shift+tab to remove 1-level indent from lines
            val lineStarts = getAllLineStartsInRegion(
                text = text,
                from = selection.min,
                to = selection.max - 1,
            )
            log.v { "lineStarts R = $lineStarts" }
            var s = text
            var firstLineSpaces = 0
            var numSpaces = 0
            for (i in lineStarts.size - 1 downTo 0) {
                val it = lineStarts[i]
                // at most remove 4 spaces
                val spaceRange = "^ {1,4}".toRegex().matchAt(s.substring(it, minOf(it + 4, s.length)), 0)?.range
                if (spaceRange != null) {
                    s = s.removeRange(it + spaceRange.start..it + spaceRange.endInclusive)
                    val spaceLength = spaceRange.endInclusive + 1 - spaceRange.start
                    numSpaces += spaceLength
                    if (i == 0) firstLineSpaces = spaceLength
                }
            }

            val (minOffset, maxOffset) = Pair(- firstLineSpaces, - numSpaces)
            log.d { "off = $minOffset, $maxOffset" }
            textValue = textValue.copy(
                text = s,
                selection = TextRange(
                    start = maxOf(0, selection.start + if (!selection.reversed) minOffset else maxOffset),
                    end = selection.end + if (!selection.reversed) maxOffset else minOffset,
                )
            )

            onTextChange?.invoke(s)
        }
    }

    var isSearchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchOptions by remember { mutableStateOf(SearchOptions(
        isRegex = false,
        isCaseSensitive = false,
        isWholeWord = false
    )) }
    var searchPattern by rememberLast(searchText, searchOptions) { mutableStateOf<Regex?>(null) }
    val scrollState = rememberScrollState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val textFieldFocusRequester = remember { FocusRequester() }

    var searchResultViewIndex by rememberLast(text) { mutableStateOf(0) }
    var lastSearchResultViewIndex by rememberLast(text) { mutableStateOf(0) }
    var searchResultRanges by rememberLast(text, searchPattern) { mutableStateOf<List<IntRange>?>(null) }
    var textFieldSize by remember { mutableStateOf<IntSize?>(null) }

    if (searchText.isNotEmpty() && searchPattern == null) {
        val regexOption = if (searchOptions.isCaseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE)
        try {
            val pattern = if (searchOptions.isRegex) {
                searchText.toRegex(regexOption)
            } else if (searchOptions.isWholeWord) {
                "\\b${Pattern.quote(searchText)}\\b".toRegex(regexOption)
            } else {
                Pattern.quote(searchText).toRegex(regexOption)
            }
            searchPattern = pattern
            searchResultViewIndex = 0
            lastSearchResultViewIndex = -1
        } catch (_: Throwable) {}
    }
    var searchResultSummary = if (!searchResultRanges.isNullOrEmpty()) {
        "${searchResultViewIndex + 1}/${searchResultRanges?.size}"
    } else {
        ""
    }

    var visualTransformations = transformations +
            if (isEnableVariables) {
                listOf(
                    EnvironmentVariableTransformation(
                        themeColors = themeColours,
                        knownVariables = knownVariables
                    ),
                    FunctionTransformation(themeColours),
                )
            } else {
                emptyList()
            } +
            if (isReadOnly) {
                listOf(CollapseTransformation(themeColours, collapsedChars.values.toList()))
            } else {
                emptyList()
            }

    textLayoutResult?.let { tl ->
        (0..minOf(10, tl.lineCount - 1)).forEach {
            log.d { "> TL Line $it top=${tl.getLineTop(it)} bottom=${tl.getLineBottom(it)} h=${tl.getLineBottom(it) - tl.getLineTop(it)}" }
        }
    }

    if (isSearchVisible) {
        if (!searchResultRanges.isNullOrEmpty() && searchPattern != null) {
            visualTransformations += SearchHighlightTransformation(
                searchPattern = searchPattern!!,
                currentIndex = searchResultViewIndex,
                colours = themeColours,
            )
        }

        if (searchPattern != null && searchResultRanges == null) {
            try {
                searchResultRanges = searchPattern!!
                    .findAll(
                        MultipleVisualTransformation(visualTransformations)
                            .filter(AnnotatedString(textValue.text))
                            .text.text
                    )
                    .map { it.range }
                    .sortedBy { it.start }
                    .toList()
            } catch (_: Throwable) {}
        }

        if (lastSearchResultViewIndex != searchResultViewIndex && textLayoutResult != null && textFieldSize != null && searchResultRanges != null) {
            lastSearchResultViewIndex = searchResultViewIndex
            val index = searchResultRanges!!.getOrNull(searchResultViewIndex)?.start
            index?.let {
                val visibleVerticalRange = scrollState.value .. scrollState.value + textFieldSize!!.height
                val lineIndex = textLayoutResult!!.getLineForOffset(it)
                val lineVerticalRange = textLayoutResult!!.getLineTop(lineIndex).toInt() .. textLayoutResult!!.getLineBottom(lineIndex).toInt()
                if (lineVerticalRange !in visibleVerticalRange) {
                    coroutineScope.launch {
                        log.d { "CEV scroll l=$lineIndex r=$lineVerticalRange v=$visibleVerticalRange" }
                        scrollState.animateScrollTo(lineVerticalRange.start)
                    }
                }
            }
        }
    }

    fun onClickSearchNext() {
        val size = searchResultRanges?.size ?: 0
        if (size < 1) return
        searchResultViewIndex = (searchResultViewIndex + 1) % size
    }

    fun onClickSearchPrev() {
        val size = searchResultRanges?.size ?: 0
        if (size < 1) return
        searchResultViewIndex = (searchResultViewIndex - 1 + size) % size
    }

    val visualTransformationToUse = visualTransformations.let {
        if (newText.length > 1 * 1024 * 1024 /* 1 MB */) {
            // disable all styles to avoid hanging
            return@let VisualTransformation.None
        }
        if (it.size > 1) {
            MultipleVisualTransformation(it)
        } else if (it.size == 1) {
            it.first()
        } else {
            VisualTransformation.None
        }
    }

    log.d { "lineTops ${lineTops != null}, textLayoutResult ${textLayoutResult != null}" }

    if (lineTops == null && textLayoutResult != null) {
        log.d { "lineTops recalc start" }
        val charOffsetMapping = visualTransformationToUse.filter(AnnotatedString(textValue.text)).offsetMapping
        val lineOffsets = listOf(0) + "\n".toRegex().findAll(textValue.text).map { charOffsetMapping.originalToTransformed(it.range.endInclusive + 1) }
        log.v { "lineOffsets = $lineOffsets" }
        lineTops = lineOffsets.map { textLayoutResult!!.getLineTop(textLayoutResult!!.getLineForOffset(it)) } + // O(l * L * 1)
                (Float.POSITIVE_INFINITY)
        log.d { "lineTops recalc end" }
    }

    Column(modifier = modifier.onPreviewKeyEvent {
        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        if (it.key == Key.F && (it.isMetaPressed || it.isCtrlPressed)) {
            isSearchVisible = !isSearchVisible
            if (!isSearchVisible) {
                textFieldFocusRequester.requestFocus()
            } else {
                lastSearchResultViewIndex = -1 // force scroll
            }
            true
        } else if (it.key == Key.Escape) {
            isSearchVisible = false
            textFieldFocusRequester.requestFocus()
            true
        } else {
            false
        }
    }) {
        if (isSearchVisible) {
            TextSearchBar(
                text = searchText,
                onTextChange = { searchText = it },
                statusText = searchResultSummary,
                searchOptions = searchOptions,
                onToggleRegex = { searchOptions = searchOptions.copy(isRegex = it) },
                onToggleCaseSensitive = { searchOptions = searchOptions.copy(isCaseSensitive = it) },
                onToggleWholeWord = { searchOptions = searchOptions.copy(isWholeWord = it) },
                onClickNext = { onClickSearchNext() },
                onClickPrev = { onClickSearchPrev() },
                modifier = Modifier.focusRequester(searchBarFocusRequester)
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (it.key == Key.Enter) {
                            if (it.isShiftPressed) {
                                onClickSearchPrev()
                            } else if (it.isAltPressed || it.isCtrlPressed) {
                                // TODO add new line character
                            } else {
                                onClickSearchNext()
                            }
                            true
                        } else {
                            false
                        }
                    },
            )

            LaunchedEffect(Unit) {
                searchBarFocusRequester.requestFocus()
            }
        }
        Box(modifier = Modifier.weight(1f).onGloballyPositioned { textFieldSize = it.size }) {
//        log.v { "CodeEditorView text=$text" }
            Row {
                val onCollapseLine = { i: Int ->
                    val index = collapsableLines.indexOfFirst { it.start == i }
                    collapsedLines[collapsableLines[index]] = collapsableLines[index]
                    collapsedChars[collapsableChars[index]] = collapsableChars[index]
                }
                val onExpandLine = { i: Int ->
                    val index = collapsableLines.indexOfFirst { it.start == i }
                    collapsedLines -= collapsableLines[index]
                    collapsedChars -= collapsableChars[index]
                }

                var layoutResult by remember { mutableStateOf<BigTextSimpleLayoutResult?>(null) }

//                BigLineNumbersView(
//                    scrollState = scrollState,
//                    bigTextViewState = bigTextViewState,
//                    textLayout = layoutResult,
//                    collapsableLines = collapsableLines,
//                    collapsedLines = collapsedLines.values.toList(),
//                    onCollapseLine = onCollapseLine,
//                    onExpandLine = onExpandLine,
//                    modifier = Modifier.fillMaxHeight(),
//                )

                val bigTextFieldState = rememberBigTextFieldState(cacheKey, textValue.text)
                val bigTextValue = bigTextFieldState.text
                var bigTextValueId by remember(textValue.text.length, textValue.text.hashCode()) { mutableStateOf<Long>(Random.nextLong()) }

                BigTextLineNumbersView(
                    scrollState = scrollState,
                    bigTextViewState = bigTextFieldState.viewState,
                    bigTextValueId = bigTextValueId,
                    bigText = bigTextValue as BigTextImpl,
                    layoutResult = layoutResult,
                    collapsableLines = collapsableLines,
                    collapsedLines = collapsedLines.values.toList(),
                    onCollapseLine = onCollapseLine,
                    onExpandLine = onExpandLine,
                    modifier = Modifier.fillMaxHeight(),
                )

                if (isReadOnly) {
                    BigMonospaceText(
                        text = bigTextValue as BigTextImpl,
                        padding = PaddingValues(4.dp),
                        visualTransformation = visualTransformationToUse,
                        fontSize = LocalFont.current.codeEditorBodyFontSize,
                        isSelectable = true,
                        scrollState = scrollState,
                        viewState = bigTextFieldState.viewState,
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier.fillMaxSize()
                            .run {
                                if (testTag != null) {
                                    testTag(testTag)
                                } else {
                                    this
                                }
                            }
                    )
//                    return@Row // compose bug: return here would crash
                } else {
                    /*LineNumbersView(
                        scrollState = scrollState,
                        textLayoutResult = textLayoutResult,
                        lineTops = lineTops,
                        collapsableLines = collapsableLines,
                        collapsedLines = collapsedLines.values.toList(),
                        onCollapseLine = onCollapseLine,
                        onExpandLine = onExpandLine,
                        modifier = Modifier.fillMaxHeight(),
                    )

                    AppTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            log.d { "CEV sel ${textValue.selection.start}" }
                            onTextChange?.invoke(it.text)
                        },
                        visualTransformation = visualTransformationToUse,
                        readOnly = isReadOnly,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = LocalFont.current.codeEditorBodyFontSize,
                        ),
                        colors = colors,
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                            .focusRequester(textFieldFocusRequester)
                            .run {
                                if (!isReadOnly) {
                                    this.onPreviewKeyEvent {
                                        if (it.type == KeyEventType.KeyDown) {
                                            when (it.key) {
                                                Key.Enter -> {
                                                    if (!it.isShiftPressed
                                                        && !it.isAltPressed
                                                        && !it.isCtrlPressed
                                                        && !it.isMetaPressed
                                                    ) {
                                                        onPressEnterAddIndent()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }

                                                Key.Tab -> {
                                                    onPressTab(it.isShiftPressed)
                                                    true
                                                }

                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                } else {
                                    this
                                }
                            }
                            .run {
                                if (testTag != null) {
                                    testTag(testTag)
                                } else {
                                    this
                                }
                            }
                    )*/

//                    var bigTextValue by remember(textValue.text.length, textValue.text.hashCode()) { mutableStateOf<BigText>(BigText.createFromLargeString(text)) } // FIXME performance

                    bigTextFieldState.valueChangesFlow
                        .debounce(100.milliseconds().toMilliseconds())
                        .onEach {
                            log.d { "bigTextFieldState change ${it.changeId}" }
                            onTextChange?.let { onTextChange ->
                                onTextChange(it.bigText.buildString())
                            }
                            bigTextValueId = it.changeId
                        }
                        .launchIn(CoroutineScope(Dispatchers.Main))

                    BigMonospaceTextField(
                        textFieldState = bigTextFieldState,
                        visualTransformation = visualTransformationToUse,
                        fontSize = LocalFont.current.codeEditorBodyFontSize,
//                        textStyle = LocalTextStyle.current.copy(
//                            fontFamily = FontFamily.Monospace,
//                            fontSize = LocalFont.current.codeEditorBodyFontSize,
//                        ),
//                        colors = colors,
                        scrollState = scrollState,
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier.fillMaxSize()
                            .focusRequester(textFieldFocusRequester)
                            .run {
                                if (!isReadOnly) {
                                    this.onPreviewKeyEvent {
                                        if (it.type == KeyEventType.KeyDown) {
                                            when (it.key) {
                                                Key.Enter -> {
                                                    if (!it.isShiftPressed
                                                        && !it.isAltPressed
                                                        && !it.isCtrlPressed
                                                        && !it.isMetaPressed && false // FIXME
                                                    ) {
                                                        onPressEnterAddIndent()
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }

                                                Key.Tab -> {
                                                    onPressTab(it.isShiftPressed)
                                                    true
                                                }

                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                } else {
                                    this
                                }
                            }
                            .run {
                                if (testTag != null) {
                                    testTag(testTag)
                                } else {
                                    this
                                }
                            }
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}

private val SEARCH_OPTION_BUTTON_WIDTH = 20.dp

@Composable
fun TextSearchBar(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    statusText: String,
    searchOptions: SearchOptions,
    onToggleRegex: (Boolean) -> Unit,
    onToggleCaseSensitive: (Boolean) -> Unit,
    onToggleWholeWord: (Boolean) -> Unit,
    onClickPrev: () -> Unit,
    onClickNext: () -> Unit,
) {
    val textSizes = LocalFont.current
    Row(modifier = modifier.padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        AppTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = LocalTextStyle.current.copy(fontSize = textSizes.searchInputSize),
            maxLines = 1,
            singleLine = false, // allow '\n'
            modifier = Modifier.weight(1f),
        )
        AppText(text = statusText, fontSize = textSizes.supplementSize, modifier = Modifier.padding(horizontal = 4.dp))
        AppTextToggleButton(
            text = ".*",
            isSelected = searchOptions.isRegex,
            onToggle = onToggleRegex,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        AppTextToggleButton(
            text = "Aa",
            isSelected = searchOptions.isCaseSensitive,
            onToggle = onToggleCaseSensitive,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        AppTextToggleButton(
            text = "W",
            isSelected = searchOptions.isWholeWord,
            isEnabled = !searchOptions.isRegex,
            onToggle = onToggleWholeWord,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        AppTextToggleButton(
            text = "↑",
            isSelected = false,
            onToggle = { onClickPrev() },
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        AppTextToggleButton(
            text = "↓",
            isSelected = false,
            onToggle = { onClickNext() },
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
    }
}

data class SearchOptions(
    val isRegex: Boolean,
    val isCaseSensitive: Boolean,
    val isWholeWord: Boolean, // ignore if isRegex is true
)

@Composable
fun LineNumbersView(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    textLayoutResult: TextLayoutResult?,
    lineTops: List<Float>?,
    collapsableLines: List<IntRange>,
    collapsedLines: List<IntRange>,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
) = with(LocalDensity.current) {
    val colours = LocalColor.current
    val fonts = LocalFont.current
    var size by remember { mutableStateOf<IntSize?>(null) }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = fonts.codeEditorLineNumberFontSize,
        fontFamily = FontFamily.Monospace,
        color = colours.unimportant,
    )
    log.v { "LineNumbersView ${size != null} && ${textLayoutResult != null} && ${lineTops != null}" }
    var lastTextLayoutResult by remember { mutableStateOf(textLayoutResult) }
    var lastLineTops by remember { mutableStateOf(lineTops) }

    val textLayoutResult = textLayoutResult ?: lastTextLayoutResult
    val lineTops = lineTops ?: lastLineTops

    lastTextLayoutResult = textLayoutResult
    lastLineTops = lineTops

    val collapsedLinesState = CollapsedLinesState(collapsableLines = collapsableLines, collapsedLines = collapsedLines)

    var lineHeight = 0f
    val viewportTop = scrollState.value.toFloat()
    val (firstLine, lastLine) = if (size != null && textLayoutResult != null && lineTops != null) {
        val viewportBottom = viewportTop + size!!.height
        log.d { "LineNumbersView before calculation" }
        // 0-based line index
        // include the partially visible line before the first line that is entirely visible
        val firstLine = maxOf(0, lineTops.binarySearchForInsertionPoint { if (it >= viewportTop) 1 else -1 } - 1)
        val lastLine = lineTops.binarySearchForInsertionPoint { if (it > viewportBottom) 1 else -1 }
        log.v { "LineNumbersView $firstLine ~ <$lastLine / $viewportTop ~ $viewportBottom" }
        log.v { "lineTops = $lineTops" }
        log.v { "collapsedLines = $collapsedLines" }
        log.d { "LineNumbersView after calculation" }
        lineHeight = textLayoutResult.getLineBottom(0) - textLayoutResult.getLineTop(0)

        firstLine to lastLine
    } else {
        0 to -1
    }
    CoreLineNumbersView(
        firstLine = firstLine,
        lastLine = minOf(lastLine, (lineTops?.size ?: 0) - 1),
        totalLines = lineTops?.size ?: 1,
        lineHeight = lineHeight.toDp(),
        getLineOffset = { (lineTops!![it] - viewportTop).toDp() },
        textStyle = textStyle,
        collapsedLinesState = collapsedLinesState,
        onCollapseLine = onCollapseLine,
        onExpandLine = onExpandLine,
        modifier = modifier.onGloballyPositioned { size = it.size }
    )
}

@OptIn(TemporaryApi::class)
@Composable
fun BigLineNumbersView(
    modifier: Modifier = Modifier,
    bigTextViewState: BigTextViewState,
    textLayout: BigTextLayoutResult?,
    scrollState: ScrollState,
    collapsableLines: List<IntRange>,
    collapsedLines: List<IntRange>,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
) = with(LocalDensity.current) {
    val colours = LocalColor.current
    val fonts = LocalFont.current

    val textStyle = LocalTextStyle.current.copy(
        fontSize = fonts.codeEditorLineNumberFontSize,
        fontFamily = FontFamily.Monospace,
        color = colours.unimportant,
    )
    val collapsedLinesState = CollapsedLinesState(collapsableLines = collapsableLines, collapsedLines = collapsedLines)

    val viewportTop = scrollState.value
    val firstLine = textLayout?.findLineNumberByRowNumber(bigTextViewState.firstVisibleRow) ?: 0
    val lastLine = (textLayout?.findLineNumberByRowNumber(bigTextViewState.lastVisibleRow) ?: -100) + 1
    log.v { "lastVisibleRow = ${bigTextViewState.lastVisibleRow} (L $lastLine); totalLines = ${textLayout?.totalLinesBeforeTransformation}" }
    CoreLineNumbersView(
        firstLine = firstLine,
        lastLine = minOf(lastLine, textLayout?.totalLinesBeforeTransformation ?: 1),
        totalLines = textLayout?.totalLinesBeforeTransformation ?: 1,
        lineHeight = (textLayout?.rowHeight ?: 0f).toDp(),
        getLineOffset = { (textLayout!!.getLineTop(it) - viewportTop).toDp() },
        textStyle = textStyle,
        collapsedLinesState = collapsedLinesState,
        onCollapseLine = onCollapseLine,
        onExpandLine = onExpandLine,
        modifier = modifier
    )
}

/**
 * The purpose of this class is to avoid unnecessary heavy computations of cache keys.
 * It must be wrapped by another @Composable with collapsableLines and collapsedLines as parameters.
 */
class CollapsedLinesState(val collapsableLines: List<IntRange>, collapsedLines: List<IntRange>) {
    val collapsableLinesMap = collapsableLines.associateBy { it.start }
    val collapsedLines = collapsedLines.associateBy { it.first }.toSortedMap() // TODO optimize using range tree
}

@Composable
fun BigTextLineNumbersView(
    modifier: Modifier = Modifier,
    bigTextViewState: BigTextViewState,
    bigTextValueId: Long,
    bigText: BigTextImpl,
    layoutResult: BigTextSimpleLayoutResult?,
    scrollState: ScrollState,
    collapsableLines: List<IntRange>,
    collapsedLines: List<IntRange>,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
) = with(LocalDensity.current) {
    val colours = LocalColor.current
    val fonts = LocalFont.current

    val textStyle = LocalTextStyle.current.copy(
        fontSize = fonts.codeEditorLineNumberFontSize,
        fontFamily = FontFamily.Monospace,
        color = colours.unimportant,
    )
    val collapsedLinesState = CollapsedLinesState(collapsableLines = collapsableLines, collapsedLines = collapsedLines)

    var prevHasLayouted by remember { mutableStateOf(false) }
    prevHasLayouted = bigText.hasLayouted
    prevHasLayouted

    val viewportTop = scrollState.value
    val firstLine = bigText.findLineIndexByRowIndex(bigTextViewState.firstVisibleRow) ?: 0
    val lastLine = (bigText.findLineIndexByRowIndex(bigTextViewState.lastVisibleRow) ?: -100) + 1
    log.d { "firstVisibleRow = ${bigTextViewState.firstVisibleRow} (L $firstLine); lastVisibleRow = ${bigTextViewState.lastVisibleRow} (L $lastLine); totalLines = ${bigText.numOfLines}" }
    val rowHeight = layoutResult?.rowHeight ?: 0f
    CoreLineNumbersView(
        firstLine = firstLine,
        lastLine = minOf(lastLine, bigText.numOfLines ?: 1),
        totalLines = bigText.numOfLines ?: 1,
        lineHeight = (rowHeight).toDp(),
//        getLineOffset = { (textLayout!!.getLineTop(it) - viewportTop).toDp() },
        getLineOffset = { ( bigText.findFirstRowIndexOfLine(it) * rowHeight - viewportTop).toDp() },
        textStyle = textStyle,
        collapsedLinesState = collapsedLinesState,
        onCollapseLine = onCollapseLine,
        onExpandLine = onExpandLine,
        modifier = modifier
    )
}

@Composable
private fun CoreLineNumbersView(
    modifier: Modifier = Modifier,
    firstLine: Int,
    lastLine: Int,
    totalLines: Int,
    lineHeight: Dp,
    getLineOffset: (Int) -> Dp,
    textStyle: TextStyle,
//    collapsableLines: List<IntRange>,
//    collapsedLines: List<IntRange>,
    collapsedLinesState: CollapsedLinesState,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
) = with(LocalDensity.current) {
    val colours = LocalColor.current
    val fonts = LocalFont.current

    val collapsableLines = collapsedLinesState.collapsableLines
    val collapsableLinesMap = collapsedLinesState.collapsableLinesMap
    val collapsedLines = collapsedLinesState.collapsedLines

    val textMeasurer = rememberTextMeasurer()
    val lineNumDigits = "$totalLines".length
    val width = rememberLast(lineNumDigits, collapsableLines.isEmpty()) {
        maxOf(textMeasurer.measure("8".repeat(lineNumDigits), textStyle, maxLines = 1).size.width.toDp(), 20.dp) +
                4.dp + (if (collapsableLines.isNotEmpty()) 24.dp else 0.dp) + 4.dp
    }

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clipToBounds()
            .background(colours.backgroundLight)
            .padding(top = 6.dp, start = 4.dp, end = 4.dp), // see AppTextField
    ) {

        var ii: Int = firstLine
        while (ii < lastLine) {
            val i: Int = ii // `ii` is passed by ref

            if (i > firstLine && getLineOffset(i).value - getLineOffset(i - 1).value < 1) {
                // optimization: there is an instant that collapsedLines is empty but lineTops = [0, 0, ..., 0, 1234]
                // skip drawing if there are duplicated lineTops
            } else {
                Row(
                    modifier = Modifier
                        .height(lineHeight)
                        .offset(y = getLineOffset(i)),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        AppText(
                            text = "${i + 1}",
                            style = textStyle,
                            fontSize = fonts.codeEditorLineNumberFontSize,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            color = colours.unimportant,
                        )
                    }
                    if (collapsableLinesMap.contains(i)) {
                        AppImageButton(
                            resource = if (collapsedLines.containsKey(i)) "expand.svg" else "collapse.svg",
                            size = 12.dp,
                            innerPadding = PaddingValues(horizontal = 4.dp),
                            onClick = {
                                if (collapsedLines.containsKey(i)) {
                                    onExpandLine(i)
                                } else {
                                    onCollapseLine(i)
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .padding(start = 4.dp),
                        )
                    } else if (collapsableLines.isNotEmpty()) {
                        Spacer(Modifier.fillMaxHeight().width(24.dp))
                    }
                }
            }
            collapsedLines.headMap(i + 1).forEach {
                if (it.value.contains(i)) {
                    ii = maxOf(ii, it.value.last)
                }
            }
            ++ii
        }
    }
}

fun getLineStart(text: String, position: Int): Int {
    for (i in (position - 1) downTo 0) {
        if (text[i] == '\n') {
            return i + 1
        }
    }
    return 0
}

fun getAllLineStartsInRegion(text: String, from: Int, to: Int): List<Int> {
    return listOf(getLineStart(text, from)) +
            "\n".toRegex().findAll(text.substring(from, to + 1), 0)
                .map { from + it.range.endInclusive + 1 }
}

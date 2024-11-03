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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.collect.TreeRangeMap
import com.sunnychung.application.multiplatform.hellohttp.annotation.TemporaryApi
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForInsertionPoint
import com.sunnychung.application.multiplatform.hellohttp.extension.contains
import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.model.SyntaxHighlight
import com.sunnychung.application.multiplatform.hellohttp.util.ObjectRef
import com.sunnychung.application.multiplatform.hellohttp.util.TreeRangeMaps
import com.sunnychung.application.multiplatform.hellohttp.util.chunkedLatest
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigMonospaceText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigMonospaceTextField
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextFieldState
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextInputFilter
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextKeyboardInputProcessor
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextLayoutResult
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextManipulator
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextSimpleLayoutResult
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformed
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextViewState
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.abbr
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.rememberAnnotatedBigTextFieldState
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.CollapseIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.FunctionIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.GraphqlSyntaxHighlightDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.JsonSyntaxHighlightDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.KotlinSyntaxHighlightSlowDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MultipleIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MultipleTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.SearchHighlightDecorator
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import kotlin.random.Random

val MAX_TEXT_FIELD_LENGTH = 4 * 1024 * 1024 // 4 MB

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    collapsableLines: List<IntRange> = emptyList(),
    collapsableChars: List<IntRange> = emptyList(),
    textColor: Color = LocalColor.current.text,
    syntaxHighlight: SyntaxHighlight,
    isEnableVariables: Boolean = false,
    knownVariables: Set<String> = setOf(),
    testTag: String? = null,
) {
    val themeColours = LocalColor.current
    val coroutineScope = rememberCoroutineScope()

    val inputFilter = BigTextInputFilter { it.replace("\r\n".toRegex(), "\n") }

    var layoutResult by remember { mutableStateOf<BigTextSimpleLayoutResult?>(null) }

    val (secondCacheKey, bigTextFieldMutableState) = rememberAnnotatedBigTextFieldState(initialValue = text)
    val bigTextFieldState: BigTextFieldState = bigTextFieldMutableState.value
    val bigTextValue: BigTextImpl = bigTextFieldState.text
    var bigTextValueId by remember(bigTextFieldState) { mutableStateOf<Long>(Random.nextLong()) }

    var collapsedLines = rememberLast(bigTextFieldState) { mutableStateMapOf<IntRange, IntRange>() }
    var collapsedChars = rememberLast(bigTextFieldState) { mutableStateMapOf<IntRange, IntRange>() }

    log.d { "CodeEditorView recompose" }

    fun onPressEnterAddIndent(textManipulator: BigTextManipulator) {
        log.d { "onPressEnterAddIndent" }

        val lineIndex = bigTextValue.findLineAndColumnFromRenderPosition(bigTextFieldState.viewState.cursorIndex).first
        val previousLineString = bigTextValue.findLineString(lineIndex) // as '\n' is not yet inputted, current line is the "previous line"
        var spacesMatch = "^(\\s+)".toRegex().matchAt(previousLineString, 0)
        val newSpaces = "\n" + (spacesMatch?.groups?.get(1)?.value ?: "")
        textManipulator.replaceAtCursor(newSpaces)
    }

    fun onPressTab(textManipulator: BigTextManipulator, isShiftPressed: Boolean) {
        val vs = bigTextFieldState.viewState
        val text = bigTextFieldState.text
        if (!isShiftPressed && !vs.hasSelection()) {
            val newSpaces = " ".repeat(4)
            textManipulator.replaceAtCursor(newSpaces)
            return
        }

        val lineRange = if (vs.hasSelection()) {
            text.findLineAndColumnFromRenderPosition(vs.selection.start).first ..
                text.findLineAndColumnFromRenderPosition(vs.selection.endInclusive).first
        } else {
            val currentLineIndex = text.findLineAndColumnFromRenderPosition(vs.cursorIndex).first
            currentLineIndex .. currentLineIndex
        }

        val isCursorAtSelectionStart = !vs.hasSelection() || vs.cursorIndex == vs.selection.start
        var selectionStartChange = 0
        var selectionEndChange = 0

        log.d { "tab line range = $lineRange" }

        lineRange.reversed().forEach {
            val linePosStart = text.findPositionStartOfLine(it)
            log.d { "tab line $it pos $linePosStart" }
            if (!isShiftPressed) { // increase indent
                val newSpaces = " ".repeat(4)
                selectionStartChange = newSpaces.length
                selectionEndChange += newSpaces.length
                textManipulator.insertAt(linePosStart, newSpaces)
            } else { // decrease indent
                val line = text.findLineString(it)
                val textToBeDeleted = "^( {1,4}|\t)".toRegex().matchAt(line, 0) ?: return@forEach
                val rangeToBeDeleted = linePosStart until linePosStart + textToBeDeleted.groups[1]!!.range.length
                textManipulator.delete(rangeToBeDeleted)

                if (it == lineRange.first) {
                    selectionStartChange -= textToBeDeleted.groups[1]!!.range.length
                } else {
                    val intersectionWithSelectionRange = vs.selection intersect rangeToBeDeleted
                    log.d { "tab selectionEndChange -= ${intersectionWithSelectionRange.length}" }
                    selectionEndChange -= intersectionWithSelectionRange.length
                }
            }
        }

        if (isShiftPressed) {
            selectionEndChange += selectionStartChange
        }

        log.d { "tab ∆sel = $selectionStartChange, $selectionEndChange" }
        if (selectionStartChange != 0 || selectionEndChange != 0) {
            if (vs.hasSelection()) {
                textManipulator.setSelection(vs.selection.start + selectionStartChange..vs.selection.endInclusive + selectionEndChange)
            }
            if (isCursorAtSelectionStart) {
                textManipulator.setCursorPosition(vs.cursorIndex + selectionStartChange)
            } else {
                textManipulator.setCursorPosition(vs.cursorIndex + selectionEndChange)
            }
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
    val searchPatternLatest by rememberUpdatedState(searchPattern)
    val scrollState = rememberScrollState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val textFieldFocusRequester = remember { FocusRequester() }

    var searchResultViewIndex by rememberLast(bigTextValue) { mutableStateOf(0) }
    var lastSearchResultViewIndex by rememberLast(bigTextValue) { mutableStateOf(0) }
    val searchResultRangesState = rememberLast(bigTextValue) { MutableStateFlow<List<IntRange>?>(null) } //= rememberLast(text, searchPattern) { mutableStateOf<List<IntRange>?>(null) }
    val searchResultRanges by searchResultRangesState.collectAsState()
    var textFieldSize by remember { mutableStateOf<IntSize?>(null) }
    val searchResultRangeTreeState = rememberLast(bigTextValue) { MutableStateFlow<TreeRangeMap<Int, Int>?>(null) } //= rememberLast(text, searchPattern) { mutableStateOf<TreeRangeMap<Int, Int>?>(null) }
    val searchResultRangeTree by searchResultRangeTreeState.collectAsState()

    val searchTrigger = remember { Channel<Unit>() }

    remember(searchOptions) {
        searchTrigger.trySend(Unit)
    }

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
            log.d { "set search pattern ${searchPattern?.pattern}" }
        } catch (_: Throwable) {}
    }
    log.d { "get search pattern ${searchPattern?.pattern}" }

    LaunchedEffect(bigTextValue) {
        searchTrigger.receiveAsFlow()
            .debounce(210L)
            .filter { isSearchVisible }
            .collectLatest {
                log.d { "search triggered ${searchPatternLatest?.pattern}" }
                if (searchPatternLatest != null) {
                    try {
                        val fullText = bigTextValue.buildString()
                        val r = searchPatternLatest!!
                            .findAll(fullText)
                            .map { it.range }
                            .sortedBy { it.start }
                            .toList()
                        searchResultRangesState.value = r
                        searchResultRangeTreeState.value = TreeRangeMaps.from(r)
                        log.d { "search r ${r.size}" }
                    } catch (e: Throwable) {
                        log.d(e) { "search error" }
                    }
                } else {
                    searchResultRangesState.value = null
                    searchResultRangeTreeState.value = null
                }
            }
    }
    var searchResultSummary = if (!searchResultRanges.isNullOrEmpty()) {
        "${searchResultViewIndex + 1}/${searchResultRanges?.size}"
    } else {
        ""
    }

    val variableTransformations = remember(bigTextFieldState, themeColours, isEnableVariables) {
        if (isEnableVariables) {
            listOf(
                EnvironmentVariableIncrementalTransformation(),
                FunctionIncrementalTransformation(themeColours)
            )
        } else {
            emptyList()
        }
    }

    val variableDecorators = remember(bigTextFieldState, themeColours, isEnableVariables, knownVariables) {
        if (isEnableVariables) {
            listOf(
                EnvironmentVariableDecorator(themeColours, knownVariables),
            )
        } else {
            emptyList()
        }
    }

    val syntaxHighlightDecorators = rememberLast(bigTextFieldState, themeColours) {
        when (syntaxHighlight) {
            SyntaxHighlight.None -> emptyList()
            SyntaxHighlight.Json -> listOf(JsonSyntaxHighlightDecorator(themeColours))
            SyntaxHighlight.Graphql -> listOf(GraphqlSyntaxHighlightDecorator(themeColours))
            SyntaxHighlight.Kotlin -> listOf(KotlinSyntaxHighlightSlowDecorator(themeColours))
        }
    }

    val searchDecorators = rememberLast(bigTextFieldState, themeColours, searchResultRangeTree, searchResultViewIndex) {
        listOf(
            SearchHighlightDecorator(searchResultRangeTree ?: TreeRangeMap.create(), searchResultViewIndex, themeColours),
        )
    }


    if (isSearchVisible) {
        if (lastSearchResultViewIndex != searchResultViewIndex && layoutResult != null && textFieldSize != null && searchResultRanges != null) {
            lastSearchResultViewIndex = searchResultViewIndex
            searchResultRanges!!.getOrNull(searchResultViewIndex)?.start?.let { position ->
                if (position > layoutResult!!.text.length) return@let

                val visibleVerticalRange = scrollState.value .. scrollState.value + textFieldSize!!.height
                val rowIndex = layoutResult!!.text.findRowIndexByPosition(position)
                val rowVerticalRange = layoutResult!!.getTopOfRow(rowIndex).toInt()  .. layoutResult!!.getBottomOfRow(rowIndex).toInt()
                if (rowVerticalRange !in visibleVerticalRange) {
                    coroutineScope.launch {
                        log.d { "CEV scroll l=$rowIndex r=$rowVerticalRange v=$visibleVerticalRange" }
                        scrollState.animateScrollTo(rowVerticalRange.start)
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
                onTextChange = {
                    searchText = it
                    log.d { "searchTrigger send" }
                    searchTrigger.trySend(Unit)
                },
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
                    val collapseIncrementalTransformation = remember(bigTextFieldState) {
                        CollapseIncrementalTransformation(themeColours, collapsedChars.values.toList())
                    }
                    var transformedText by remember(bigTextFieldState) { mutableStateOf<BigTextTransformed?>(null) }

                    transformedText?.let { transformedText ->
                        collapseIncrementalTransformation.update(collapsedChars.values.toList(), bigTextFieldState.viewState)
                    }

                    BigMonospaceText(
                        text = bigTextValue as BigTextImpl,
                        padding = PaddingValues(4.dp),
                        inputFilter = inputFilter,
                        textTransformation = rememberLast(bigTextFieldState, collapseIncrementalTransformation) {
                            MultipleIncrementalTransformation(listOf(
                                collapseIncrementalTransformation,
                            ))
                        },
                        textDecorator = //rememberLast(bigTextFieldState, syntaxHighlightDecorators, searchDecorators) {
                            MultipleTextDecorator(syntaxHighlightDecorators + searchDecorators)
                        //},
                        ,
                        fontSize = LocalFont.current.codeEditorBodyFontSize,
                        isSelectable = true,
                        scrollState = scrollState,
                        viewState = bigTextFieldState.viewState,
                        onTextLayout = { layoutResult = it },
                        onTransformInit = { transformedText = it },
                        modifier = Modifier.fillMaxSize()
                            .focusRequester(textFieldFocusRequester)
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
                    LaunchedEffect(bigTextFieldState, onTextChange) {
                        bigTextFieldState.valueChangesFlow
                            .chunkedLatest(200.milliseconds())
                            .collect {
                                log.d { "bigTextFieldState change ${it.changeId} ${it.bigText.buildString()}" }
                                onTextChange?.let { onTextChange ->
                                    val string = it.bigText.buildCharSequence() as AnnotatedString
                                    log.d { "${bigTextFieldState.text} : ${it.bigText} onTextChange(${string.text.abbr()})" }
                                    onTextChange(string.text)
                                    secondCacheKey.value = ObjectRef(string.text)
                                }
                                bigTextValueId = it.changeId
                                searchTrigger.trySend(Unit)
                            }
                    }

                    BigMonospaceTextField(
                        textFieldState = bigTextFieldState,
                        inputFilter = inputFilter,
                        textTransformation = remember(variableTransformations) {
                            MultipleIncrementalTransformation(
                                variableTransformations
                            )
                        },
                        textDecorator = //rememberLast(bigTextFieldState, themeColours, searchResultRangeTree, searchResultViewIndex, syntaxHighlightDecorator) {
                            MultipleTextDecorator(syntaxHighlightDecorators + variableDecorators + searchDecorators)
                        //},
                        ,
                        fontSize = LocalFont.current.codeEditorBodyFontSize,
                        scrollState = scrollState,
                        onTextLayout = { layoutResult = it },
                        keyboardInputProcessor = object : BigTextKeyboardInputProcessor {
                            override fun beforeProcessInput(
                                it: KeyEvent,
                                viewState: BigTextViewState,
                                textManipulator: BigTextManipulator
                            ): Boolean {
                                return if (it.type == KeyEventType.KeyDown) {
                                    when (it.key) {
                                        Key.Enter -> {
                                            if (!it.isShiftPressed
                                                && !it.isAltPressed
                                                && !it.isCtrlPressed
                                                && !it.isMetaPressed
                                            ) {
                                                onPressEnterAddIndent(textManipulator)
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.Tab -> {
                                            onPressTab(textManipulator, it.isShiftPressed)
                                            true
                                        }

                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                            .focusRequester(textFieldFocusRequester)
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

    // Note that layoutResult.text != bigText
    val layoutText = layoutResult?.text as? BigTextTransformerImpl

    var prevHasLayouted by remember { mutableStateOf(false) }
    prevHasLayouted = layoutText?.hasLayouted ?: false
    prevHasLayouted

    val viewportTop = scrollState.value
    val firstLine = layoutText?.findOriginalLineIndexByRowIndex(bigTextViewState.firstVisibleRow) ?: 0
    val lastLine = (layoutText?.findOriginalLineIndexByRowIndex(bigTextViewState.lastVisibleRow) ?: -100) + 1
    log.d { "firstVisibleRow = ${bigTextViewState.firstVisibleRow} (L $firstLine); lastVisibleRow = ${bigTextViewState.lastVisibleRow} (L $lastLine); totalLines = ${layoutText?.numOfOriginalLines}" }
    val rowHeight = layoutResult?.rowHeight ?: 0f
    CoreLineNumbersView(
        firstLine = firstLine,
        lastLine = minOf(lastLine, layoutText?.numOfOriginalLines ?: 1),
        totalLines = layoutText?.numOfOriginalLines ?: 1,
        lineHeight = (rowHeight).toDp(),
        getLineOffset = {
            ((layoutText?.findFirstRowIndexByOriginalLineIndex(it).also { r ->
                log.v { "layoutText.findFirstRowIndexOfLine($it) = $r" }
            }
                ?: 0) * rowHeight - viewportTop).toDp()
        },
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
    /* exclusive */ lastLine: Int,
    totalLines: Int,
    lineHeight: Dp,
    getLineOffset: (Int) -> Dp,
    textStyle: TextStyle,
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

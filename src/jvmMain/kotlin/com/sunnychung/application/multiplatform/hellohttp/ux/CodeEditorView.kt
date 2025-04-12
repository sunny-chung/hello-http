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
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.collect.TreeRangeMap
import com.sunnychung.application.multiplatform.hellohttp.extension.contains
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.model.SyntaxHighlight
import com.sunnychung.application.multiplatform.hellohttp.util.TreeRangeMaps
import com.sunnychung.application.multiplatform.hellohttp.util.chunkedLatest
import com.sunnychung.application.multiplatform.hellohttp.util.let
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.AppUX.ENV_VAR_VALUE_MAX_DISPLAY_LENGTH
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.abbr
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.CollapseIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.FunctionIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.GraphqlSyntaxHighlightDecorator
//import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.GraphqlSyntaxHighlightSlowDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.JsonSyntaxHighlightLinearDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.JsonSyntaxHighlightSlowDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.KotlinSyntaxHighlightSlowDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MultipleIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MultipleTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.SearchHighlightDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.transform.BigTextTransformed
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextField
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextInputFilter
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextKeyboardInputProcessor
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextLabel
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextSimpleLayoutResult
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextViewState
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberConcurrentLargeAnnotatedBigTextFieldState
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.random.Random

val MAX_TEXT_FIELD_LENGTH = 4 * 1024 * 1024 // 4 MB
val INDENT_SPACES = 2

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    cacheKey: String,
    isReadOnly: Boolean = false,

    /**
     * This argument is only used when there is a cache miss using the cache key {@param cacheKey}.
     */
    initialText: String,
    onTextChange: ((String) -> Unit)? = null,
    collapsableLines: List<IntRange> = emptyList(),
    collapsableChars: List<IntRange> = emptyList(),
    textColor: Color = LocalColor.current.text,
    syntaxHighlight: SyntaxHighlight,
    isEnableVariables: Boolean = false,
    knownVariables: Map<String, String> = mutableMapOf(),
    onMeasured: ((textFieldPositionTop: Float) -> Unit)? = null,
    onTextManipulatorReady: ((BigTextFieldState) -> Unit)? = null,
    testTag: String? = null,
) {
    log.d { "CodeEditorView start. cache key = '$cacheKey'" }

    val themeColours = LocalColor.current
    val fonts = LocalFont.current
    val coroutineScope = rememberCoroutineScope()

    val inputFilter = BigTextInputFilter { it.replace("\r\n".toRegex(), "\n") }

    var layoutResult by remember { mutableStateOf<BigTextSimpleLayoutResult?>(null) }

    val bigTextFieldState: BigTextFieldState by rememberConcurrentLargeAnnotatedBigTextFieldState(initialValue = initialText, cacheKey) {
        log.d { "init BigText disable layout. lines = ${it.text.numOfLines}" }
        it.viewState.isLayoutDisabled = true
    }
    val bigTextValue: BigText = bigTextFieldState.text
    var bigTextValueId by remember(bigTextFieldState) { mutableStateOf<Long>(Random.nextLong()) }

    val collapsedLines = rememberLast(bigTextFieldState) { mutableStateMapOf<IntRange, IntRange>() }
    val collapsedChars = rememberLast(bigTextFieldState) { mutableStateMapOf<IntRange, IntRange>() }

    log.d { "CodeEditorView recompose" }
    onTextManipulatorReady?.invoke(bigTextFieldState)

    fun onPressEnterAddIndent() {
        log.d { "onPressEnterAddIndent" }

        val lineIndex = bigTextValue.findLineAndColumnFromRenderPosition(bigTextFieldState.viewState.cursorIndex).first
        val previousLineString = bigTextValue.findLineString(lineIndex) // as '\n' is not yet inputted, current line is the "previous line"
        val spacesMatch = "^([ \t]+)".toRegex().matchAt(previousLineString, 0)
        val newSpaces = "\n" + (spacesMatch?.groups?.get(1)?.value ?: "")
        bigTextFieldState.replaceTextAtCursor(newSpaces)
    }

    fun onPressTab(isShiftPressed: Boolean) {
        val vs = bigTextFieldState.viewState
        val text = bigTextFieldState.text
        if (!isShiftPressed && !vs.hasSelection()) {
            val newSpaces = " ".repeat(INDENT_SPACES)
            bigTextFieldState.replaceTextAtCursor(newSpaces)
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
                val newSpaces = " ".repeat(INDENT_SPACES)
                selectionStartChange = newSpaces.length
                selectionEndChange += newSpaces.length
                bigTextValue.insertAt(linePosStart, newSpaces)
            } else { // decrease indent
                val line = text.findLineString(it)
                val textToBeDeleted = "^( {1,$INDENT_SPACES}|\t)".toRegex().matchAt(line, 0) ?: return@forEach
                val rangeToBeDeleted = linePosStart until linePosStart + textToBeDeleted.groups[1]!!.range.length
                bigTextValue.delete(rangeToBeDeleted)

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
                vs.setSelection(vs.selection.start + selectionStartChange..vs.selection.endInclusive + selectionEndChange)
            }
            if (isCursorAtSelectionStart) {
                vs.setCursorIndex(vs.cursorIndex + selectionStartChange)
            } else {
                vs.setCursorIndex(vs.cursorIndex + selectionEndChange)
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

    var isSyntaxHighlightDisabled = false

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
            log.d { "set search pattern ${searchPattern?.pattern}" }
        } catch (_: Throwable) {}
    }
    log.d { "get search pattern ${searchPattern?.pattern}" }

    LaunchedEffect(bigTextValue) {
        withContext(Dispatchers.IO) {
            searchTrigger.receiveAsFlow()
                .debounce(410L)
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
                            val treeRangeMap = TreeRangeMaps.from(r)
                            withContext(Dispatchers.Main) {
                                searchResultRangesState.value = r
                                searchResultRangeTreeState.value = treeRangeMap
                                log.d { "search r ${r.size}" }
                                searchResultViewIndex = 0
                                lastSearchResultViewIndex = -1
                            }
                        } catch (e: Throwable) {
                            log.d(e) { "search error" }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            searchResultRangesState.value = null
                            searchResultRangeTreeState.value = null
                            searchResultViewIndex = 0
                            lastSearchResultViewIndex = -1
                        }
                    }
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
                EnvironmentVariableIncrementalTransformation(fonts),
                FunctionIncrementalTransformation(themeColours, fonts)
            )
        } else {
            emptyList()
        }
    }

    val variableDecorators = remember(bigTextFieldState, themeColours, isEnableVariables, knownVariables) {
        if (isEnableVariables) {
            listOf(
                EnvironmentVariableDecorator(themeColours, fonts, knownVariables.keys),
            )
        } else {
            emptyList()
        }
    }

    log.d { "before syntaxHighlightDecorators" }

    val syntaxHighlightDecorators = if (
        (isReadOnly && bigTextValue.length > 200 * 1024 * 1024)
        || (!isReadOnly && bigTextValue.length > 32 * 1024 * 1024)
    ) {
        // data too large, syntax highlighter cannot handle quickly, so disable syntax highlighting.
        isSyntaxHighlightDisabled = syntaxHighlight != SyntaxHighlight.None

        emptyList()
    } else {
        rememberLast(bigTextFieldState, themeColours) {
            when (syntaxHighlight) {
                SyntaxHighlight.None -> emptyList()
//                SyntaxHighlight.Json -> listOf(JsonSyntaxHighlightDecorator(themeColours))
//                SyntaxHighlight.Json -> listOf(JsonSyntaxHighlightSlowDecorator(themeColours))
                SyntaxHighlight.Json -> listOf(JsonSyntaxHighlightLinearDecorator(themeColours))
                SyntaxHighlight.Graphql -> listOf(GraphqlSyntaxHighlightDecorator(themeColours))
//                SyntaxHighlight.Graphql -> listOf(GraphqlSyntaxHighlightSlowDecorator(themeColours))
                SyntaxHighlight.Kotlin -> listOf(KotlinSyntaxHighlightSlowDecorator(themeColours))
            }
        }
    }

    log.d { "after syntaxHighlightDecorators" }

    val searchDecorators = rememberLast(bigTextFieldState, themeColours, searchResultRangeTree, searchResultViewIndex) {
        listOf(
            SearchHighlightDecorator(searchResultRangeTree ?: TreeRangeMap.create(), searchResultViewIndex, themeColours),
        )
    }

    if (isSearchVisible && layoutResult != null && textFieldSize != null && searchResultRanges != null) {
        remember(searchPattern, searchResultRanges, searchResultViewIndex, lastSearchResultViewIndex /* force scroll trigger */) {
            lastSearchResultViewIndex = searchResultViewIndex
            let(searchResultRanges!!.getOrNull(searchResultViewIndex)?.start, layoutResult?.text) { position, text ->
                if (position > text.length) return@let

                val visibleVerticalRange = scrollState.value .. scrollState.value + textFieldSize!!.height
                val rowIndex = text.findRowIndexByPosition(position)
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
                key = cacheKey,
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

        if (isSyntaxHighlightDisabled) {
            AppText("Syntax highlighting has been disabled for better performance due to large content size.",
                color = themeColours.primary,
                modifier = Modifier
                .background(themeColours.backgroundTooltip)
                .padding(4.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).onGloballyPositioned {
            textFieldSize = it.size
            log.v { "text field pos = ${it.positionInParent().y}" }
            onMeasured?.invoke(it.positionInParent().y)
        }) {
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
                    bigText = bigTextValue,
                    layoutResult = layoutResult,
                    collapsableLines = collapsableLines,
                    collapsedLines = collapsedLines.values.toList(),
                    onCollapseLine = onCollapseLine,
                    onExpandLine = onExpandLine,
                    onCorrectMeasured = {
                        log.v { "change isLayoutDisabled from ${bigTextFieldState.viewState.isLayoutDisabled} to false" }
                        bigTextFieldState.viewState.isLayoutDisabled = false
                    },
                    modifier = Modifier.fillMaxHeight()
                )

                if (isReadOnly) {
                    val collapseIncrementalTransformation = remember(bigTextFieldState) {
                        CollapseIncrementalTransformation(themeColours, collapsedChars.values.toList())
                    }
                    var transformedText by remember(bigTextFieldState) { mutableStateOf<BigTextTransformed?>(null) }

                    transformedText?.let { transformedText ->
                        collapseIncrementalTransformation.update(collapsedChars.values.toList(), bigTextFieldState.viewState)
                    }

                    BigTextLabel(
                        text = bigTextValue,
                        color = textColor,
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
                        fontSize = fonts.codeEditorBodyFontSize,
                        fontFamily = fonts.monospaceFontFamily,
                        isSoftWrapEnabled = true,
                        isSelectable = true,
                        scrollState = scrollState,
                        viewState = bigTextFieldState.viewState,
                        onTextLayout = { layoutResult = it },
                        onTransformInit = { transformedText = it },
                        contextMenu = AppBigTextFieldContextMenu,
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
                    LaunchedEffect(bigTextFieldState, onTextChange) { // FIXME the flow is frequently recreated
                        log.i { "CEV recreate change collection flow $bigTextFieldState ${onTextChange.hashCode()}" }
                        withContext(Dispatchers.IO) {
                            bigTextFieldState.valueChangesFlow
                                .onEach { log.d { "bigTextFieldState change each ${it.changeId}" } }
                                .chunkedLatest(200.milliseconds())
                                .collect {
                                    log.d { "bigTextFieldState change collect ${it.changeId} ${it.bigText.length} ${it.bigText.buildString()}" }
                                    withContext(NonCancellable) { // continue to complete the current collect block even the flow is cancelled
                                        onTextChange?.let { onTextChange ->
                                            val string = it.bigText.buildCharSequence() as AnnotatedString
                                            withContext(Dispatchers.Main) {
                                                log.d { "${bigTextFieldState.text} : ${it.bigText} ${it.changeId} onTextChange(${string.text.abbr()} | ${string.text.length})" }
                                                onTextChange(string.text)
                                                log.d { "${bigTextFieldState.text} : ${it.bigText} ${it.changeId} called onTextChange(${string.text.abbr()} | ${string.text.length})" }
                                            }
                                        }
                                        bigTextValueId = it.changeId
                                        searchTrigger.trySend(Unit)

                                        bigTextFieldState.markConsumed(it.sequence)
                                    }
                                }
                        }
                    }

                    var mouseHoverVariable by remember(bigTextFieldState) { mutableStateOf<String?>(null) }
                    AppTooltipArea(
                        isVisible = mouseHoverVariable != null && mouseHoverVariable in knownVariables,
                        tooltipText = mouseHoverVariable?.let {
                            val s = knownVariables[it] ?: return@let null
                            if (s.length > ENV_VAR_VALUE_MAX_DISPLAY_LENGTH) {
                                s.substring(0, ENV_VAR_VALUE_MAX_DISPLAY_LENGTH) + " ..."
                            } else {
                                s
                            }
                        } ?: "",
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        BigTextField(
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
                            color = textColor,
                            cursorColor = themeColours.cursor,
                            fontSize = fonts.codeEditorBodyFontSize,
                            fontFamily = fonts.monospaceFontFamily,
                            isSoftWrapEnabled = true,
                            scrollState = scrollState,
                            onTextLayout = { layoutResult = it },
                            contextMenu = AppBigTextFieldContextMenu,
                            keyboardInputProcessor = object : BigTextKeyboardInputProcessor {
                                override fun beforeProcessInput(
                                    it: KeyEvent,
                                    viewState: BigTextViewState
                                ): Boolean {
                                    return if (it.type == KeyEventType.KeyDown) {
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
                            },
                            onPointerEvent = { event, tag ->
                                log.v { "onPointerEventOnAnnotatedTag $tag $event" }
                                mouseHoverVariable = if (tag?.startsWith(EnvironmentVariableIncrementalTransformation.TAG_PREFIX) == true) {
                                    tag.replaceFirst(EnvironmentVariableIncrementalTransformation.TAG_PREFIX, "")
                                } else {
                                    null
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
    key: String,
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
        val focusRequester = remember { FocusRequester() }
        AppTextField(
            key = "$key/SearchText",
            value = text,
            onValueChange = onTextChange,
            textStyle = LocalTextStyle.current.copy(fontSize = textSizes.searchInputSize),
            maxLines = 1,
            singleLine = true, // TODO allow '\n'
            onFinishInit = {
                focusRequester.requestFocus()
            },
            modifier = Modifier.weight(1f)
                .focusRequester(focusRequester),
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
    bigText: BigText,
    layoutResult: BigTextSimpleLayoutResult?,
    scrollState: ScrollState,
    collapsableLines: List<IntRange>,
    collapsedLines: List<IntRange>,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
    onCorrectMeasured: () -> Unit,
) = with(LocalDensity.current) {
    val colours = LocalColor.current
    val fonts = LocalFont.current

    val textStyle = LocalTextStyle.current.copy(
        fontSize = fonts.codeEditorLineNumberFontSize,
        fontFamily = LocalFont.current.monospaceFontFamily,
        color = colours.unimportant,
    )
    val collapsedLinesState = CollapsedLinesState(collapsableLines = collapsableLines, collapsedLines = collapsedLines)

    // Note that layoutResult.text != bigText
    val layoutText = layoutResult?.text as? BigTextTransformed

    var prevHasLayouted by remember { mutableStateOf(false) }
    prevHasLayouted = layoutText?.hasLayouted ?: false
    prevHasLayouted

    val viewportTop = scrollState.value
    val visibleRows = bigTextViewState.calculateVisibleRowRange(viewportTop)
    log.d { "scroll = $viewportTop; visibleRows = $visibleRows; totalLines = ${layoutText?.numOfOriginalLines}" }
    val rowHeight = layoutResult?.rowHeight ?: 0f
    // Note: it is possible that `visibleRows.first` > `visibleRows.last`, when the text covered by this range is removed.
    CoreLineNumbersView(
        firstRow = visibleRows.first,
        lastRow = visibleRows.endInclusive + 1,
        rowToLineIndex = { layoutText?.findOriginalLineIndexByRowIndex(it) ?: 0 },
        totalLines = bigText.numOfLines,
        lineHeight = (rowHeight).toDp(),
        getRowOffset = {
            (it * rowHeight - viewportTop).toDp()
        },
        textStyle = textStyle,
        collapsedLinesState = collapsedLinesState,
        onCollapseLine = onCollapseLine,
        onExpandLine = onExpandLine,
        onCorrectMeasured = onCorrectMeasured,
        modifier = modifier
    )
}

@Composable
private fun CoreLineNumbersView(
    modifier: Modifier = Modifier,
    firstRow: Int,
    /* exclusive */ lastRow: Int,
    rowToLineIndex: (Int) -> Int,
    totalLines: Int,
    lineHeight: Dp,
    getRowOffset: (Int) -> Dp,
    textStyle: TextStyle,
    collapsedLinesState: CollapsedLinesState,
    onCollapseLine: (Int) -> Unit,
    onExpandLine: (Int) -> Unit,
    onCorrectMeasured: () -> Unit,
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
    log.v { "totalLines = $totalLines, width = ${width.toPx()}" }

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clipToBounds()
            .background(colours.backgroundLight)
            .onGloballyPositioned { // need to be put before padding modifiers so that measured size includes padding
                if (abs(it.size.width - width.toPx()) < 0.1 /* equivalent to it.size.width == width.toPx() */) {
                    log.v { "correct width ${it.size.width}. expected = ${width.toPx()}" }
                    onCorrectMeasured()
                } else {
                    log.v { "reject width ${it.size.width}. expected = ${width.toPx()}" }
                }
            }
            .padding(top = 6.dp, start = 4.dp, end = 4.dp) // see AppTextField
    ) {
        var ii: Int = firstRow
        var lastLineIndex = -1
        if (firstRow > 0 && firstRow < lastRow) {
            val lineBeforeFirstRow = rowToLineIndex(firstRow - 1)
            lastLineIndex = lineBeforeFirstRow
        }
        while (ii < lastRow) {
            val i: Int = ii // `ii` is passed by ref
            val lineIndex = rowToLineIndex(i)

            if (lineIndex > lastLineIndex) {
                Row(
                    modifier = Modifier
                        .height(lineHeight)
                        .offset(y = getRowOffset(i)),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        AppText(
                            text = (rowToLineIndex(i) + 1).toString(),
                            style = textStyle,
                            fontSize = fonts.codeEditorLineNumberFontSize,
                            maxLines = 1,
                            color = colours.unimportant,
                        )
                    }
                    if (collapsableLinesMap.contains(lineIndex)) {
                        AppImageButton(
                            resource = if (collapsedLines.containsKey(lineIndex)) "expand.svg" else "collapse.svg",
                            size = 12.dp,
                            innerPadding = PaddingValues(horizontal = 4.dp),
                            onClick = {
                                if (collapsedLines.containsKey(lineIndex)) {
                                    onExpandLine(lineIndex)
                                } else {
                                    onCollapseLine(lineIndex)
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
            lastLineIndex = lineIndex
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

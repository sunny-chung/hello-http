package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolVersion
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.describeTransportLayer
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset

private val DATE_TIME_FORMAT = KDateTimeFormat("HH:mm:ss.lll")
private val TIMESTAMP_COLUMN_WIDTH_DP = 110.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TransportTimelineView(modifier: Modifier = Modifier, protocol: ProtocolVersion?, exchange: RawExchange, response: UserResponse) {
    val timestampColumnWidthDp = TIMESTAMP_COLUMN_WIDTH_DP
    val density = LocalDensity.current
    val clipboardManager = LocalClipboardManager.current

    log.d { "TransportTimelineView recompose" }

    val streamDigits = if (protocol?.isHttp2() == true) {
        minOf(
            exchange.exchanges
                .filter { it.streamId != null }
                .maxOfOrNull { it.streamId!! }
                ?.toString()
                ?.length ?: 1,
            2
        )
    } else {
        0
    }

    // --- for copy button start
    data class PosContainer(val top: Float, val bottom: Float = top, val index: Int, val localLine: Int) : Comparable<PosContainer> {
        override fun compareTo(other: PosContainer): Int {
            return compareValuesBy(this, other, { it.top }, { it.index }, { it.localLine })
        }
    }
    class LineKey(val index: Int, val type: LineType, val localLine: Int, val text: String) : Comparable<LineKey> {
        override fun compareTo(other: LineKey): Int {
            return compareValuesBy(this, other, { it.index }, { it.localLine })
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LineKey) return false

            if (index != other.index) return false
            if (localLine != other.localLine) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + localLine
            return result
        }

        override fun toString(): String {
            return "LineKey(index=$index, type=$type, localLine=$localLine)"
        }
    }

    var copyButtonHeight by remember { mutableStateOf(1) }
    var containerXPos by remember { mutableStateOf(0f) }
    var containerYPos by remember { mutableStateOf(0f) }
    var mouseRelativePos by remember { mutableStateOf<Offset?>(null) }
    var showCopyButtonAtYPos by remember { mutableStateOf<Float?>(null) }
    var showCopyButtonForText by remember { mutableStateOf("") }
    var showCopyButtonLeftBound by remember { mutableStateOf(0f) }
    val childPositions = remember { sortedMapOf<LineKey, PosContainer>() }
    val childPositionToKeys = remember { sortedMapOf<PosContainer, LineKey>() }

    val onPositionLine = { itemIndex: Int, xLeftAbsPos: Float, yTopAbsPos: Float, yBottomAbsPos: Float, localLine: Int, lineType: LineType, eventText: String ->
        val key = LineKey(itemIndex, lineType, localLine, eventText)
        synchronized(childPositions) {
            childPositions[key]?.also { v ->
//                log.v { "Removing $key $v" }
                childPositionToKeys.remove(v) //?: throw RuntimeException("Cannot remove $key")
            }
            val posContainer = PosContainer(top = yTopAbsPos, bottom = yBottomAbsPos, index = itemIndex, localLine = localLine)
//            log.v { "Inserting $key $posContainer" }
            childPositions[key] = posContainer
            childPositionToKeys[posContainer] = key

            showCopyButtonLeftBound = xLeftAbsPos
        }
    }
    val onDisposeLine = { index: Int, localLine: Int ->
        val key = LineKey(index, LineType.FIRST_LINE, localLine, "")
        val posContainer = childPositions[key] //?: throw RuntimeException("Not released $index $localLine")
        if (posContainer != null) {
            childPositions.remove(key) //?: throw RuntimeException("Not released $index $localLine")
            childPositionToKeys.remove(posContainer) //?: throw RuntimeException("Not released $index $localLine")
        }
    }
    val onMouseMove = onMouseMove@ { mousePos: Offset ->
        mouseRelativePos = mousePos

        if (mousePos.x + containerXPos < showCopyButtonLeftBound) {
            showCopyButtonAtYPos = null
            return@onMouseMove
        }

        val yPos = mousePos.y + containerYPos

        val beforeKey = childPositionToKeys.headMap(PosContainer(yPos + 0.1f, index = -1, localLine = -1)).let { map ->
            if (map.isEmpty()) {
                null
            } else {
                val index = map[map.lastKey()]?.index ?: return@let null
                // find the first visible line for this item index
                childPositions.tailMap(LineKey(index, LineType.FIRST_LINE, -1, "")).firstKey()
            }
        }
        val afterKey = childPositionToKeys.tailMap(PosContainer(yPos - 0.1f, index = -1, localLine = -1)).let { map ->
            if (map.isEmpty()) {
                null
            } else {
                val key = map[map.firstKey()]
                if (key != null && beforeKey != null && key.index > beforeKey.index) {
                    childPositions.headMap(LineKey(key.index, LineType.FIRST_LINE, -1, "")).lastKey()
                } else {
                    key
                }
            }
        }
//        log.v { "move at $beforeKey $afterKey" }

        var showYPos = (beforeKey?.let { childPositions[it] } ?: run {
            showCopyButtonAtYPos = null
            return@onMouseMove
        }).top - containerYPos
        log.v { "first line pos $showYPos" }
        showYPos = maxOf(with (density) { 4.dp.toPx() }, showYPos) // stay in visible area
        if (afterKey?.type == LineType.LAST_LINE) {
            // but let it go if the text is going to leave the visible area
            childPositions[afterKey]?.let { lastLinePosContainer ->
                log.v { "bottom ${lastLinePosContainer.bottom} - $copyButtonHeight - ${containerYPos} = ${lastLinePosContainer.bottom - copyButtonHeight - containerYPos}" }
                showYPos = minOf(lastLinePosContainer.bottom - copyButtonHeight - containerYPos, showYPos)
            }
        }
        showCopyButtonAtYPos = showYPos
        showCopyButtonForText = beforeKey.text

        log.v { "move at $beforeKey $afterKey $showYPos" }
        log.v { "childPositions size ${childPositions.size} ${childPositionToKeys.size}" }
    }
    // --- for copy button end

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppTextButton(
                text = "Copy All",
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 4.dp, horizontal = 8.dp),
            ) {
                val textToCopy = response.describeTransportLayer()
                clipboardManager.setText(AnnotatedString(textToCopy))
                AppContext.ErrorMessagePromptViewModel.showSuccessMessage("Copied text")
            }
        }

        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    containerXPos = it.positionInRoot().x
                    containerYPos = it.positionInRoot().y
                }
                .onPointerEvent(PointerEventType.Move) {
                    onMouseMove(it.changes.first().position)
                }
                .onPointerEvent(PointerEventType.Exit) {
                    showCopyButtonAtYPos = null
                }
                .clipToBounds()
        ) {
            Box(
                Modifier
                    .width(timestampColumnWidthDp + 6.dp)
                    .fillMaxHeight()
                    .background(LocalColor.current.backgroundSemiLight)
            )
            Box(
                Modifier
                    .offset(x = timestampColumnWidthDp + 6.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(LocalColor.current.line)
            )

            var contentWidthInPx by remember { mutableStateOf<Int?>(null) }
            val textStyle = LocalTextStyle.current.copy(
                fontSize = LocalFont.current.bodyFontSize,
                fontFamily = FontFamily.Monospace,
            )
            val fontFamilyResolver = LocalFontFamilyResolver.current

            val numCharsInALine = if (contentWidthInPx != null) {
                remember(contentWidthInPx) {
                    Paragraph(
                        text = "0".repeat(1000),
                        style = textStyle,
                        constraints = Constraints(maxWidth = contentWidthInPx!!),
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                    ).getLineEnd(0)
                }
            } else {
                0
            }
            log.d { "numCharsInALine = $numCharsInALine" }

            var totalNumLines by rememberLast(exchange.uiVersion) { mutableStateOf<Int?>(null) }

            val scrollbarAdapter: ScrollbarAdapter
            /*
                Large number of Text elements freezes scrolling and rendering. In this situation LazyColumn helps.
                But `SelectionContainer { LazyColumn { ... } }` cannot select text elements span over a screen.
                (See https://github.com/JetBrains/compose-multiplatform/issues/3550)

                So, Column is still being used when the number of Text elements is small.
             */
            if ((totalNumLines ?: Int.MAX_VALUE) > 1000) {
                val scrollState = rememberLazyListState()
                scrollbarAdapter = rememberScrollbarAdapter(scrollState)

                if (scrollState.isScrollInProgress) {
                    mouseRelativePos?.let { onMouseMove(it) }
                }

                log.d { "TransportTimelineView adopts LazyColumn $totalNumLines" }

                SelectionContainer {
                    LazyColumn(state = scrollState) {
                        synchronized(exchange.exchanges) {
//                        items(items = exchange.exchanges) {
                            TransportTimelineContentView(
                                scope = this,
                                exchange = exchange,
                                contentWidthInPx = contentWidthInPx,
                                numCharsInALine = numCharsInALine,
                                protocol = protocol,
                                streamDigits = streamDigits,
                                onMeasureContentWidth = { contentWidthInPx = it },
                                onMeasureContentLines = { totalNumLines = it.totalNumLines },
                                onPrepareComposable = {},
                                onPositionLine = onPositionLine,
                                onDisposeLine = onDisposeLine,
                            )
//                        }
                        }
                    }
                }
            } else {
                val scrollState = rememberScrollState()
                scrollbarAdapter = rememberScrollbarAdapter(scrollState)

                if (scrollState.isScrollInProgress) {
                    mouseRelativePos?.let { onMouseMove(it) }
                }

                log.d { "TransportTimelineView adopts Column $totalNumLines" }

                SelectionContainer {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        val composables = mutableListOf<@Composable () -> Unit>()
                        synchronized(exchange.exchanges) {
                            TransportTimelineContentView(
                                scope = this,
                                exchange = exchange,
                                contentWidthInPx = contentWidthInPx,
                                numCharsInALine = numCharsInALine,
                                protocol = protocol,
                                streamDigits = streamDigits,
                                onMeasureContentWidth = { contentWidthInPx = it },
                                onMeasureContentLines = { totalNumLines = it.totalNumLines },
                                onPrepareComposable = { composables += it },
                                onPositionLine = onPositionLine,
                                onDisposeLine = onDisposeLine,
                            )
                        }
                        composables.forEach { it() }
                    }
                }
            }

            showCopyButtonAtYPos?.let { showCopyButtonAtYPos ->
                FloatingCopyButton(
                    textToCopy = showCopyButtonForText,
                    size = 16.dp,
                    innerPadding = 2.dp,
                    modifier = Modifier
                        .onGloballyPositioned { copyButtonHeight = it.size.height }
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .offset(y = with(density) { showCopyButtonAtYPos.toDp() })
                )
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = scrollbarAdapter,
            )
        }
    }
}

enum class LineType {
    FIRST_LINE, MIDDLE_LINE, LAST_LINE
}

private fun TransportTimelineContentView(
    scope: Any,
    exchange: RawExchange,
    contentWidthInPx: Int?,
    numCharsInALine: Int,
    protocol: ProtocolVersion?,
    streamDigits: Int,
    onMeasureContentWidth: (Int) -> Unit,
    onMeasureContentLines: (TransportTimelineContentMeasureResult) -> Unit,
    onPrepareComposable: (@Composable () -> Unit) -> Unit,

    // all the below events are for copy buttons
    onPositionLine: (itemIndex: Int, xLeftAbsPos: Float, yTopAbsPos: Float, yBottomAbsPos: Float, localLine: Int, lineType: LineType, eventText: String) -> Unit,
    onDisposeLine: (itemIndex: Int, localLine: Int) -> Unit,
) {
    var totalLines = 0
    exchange.exchanges.forEachIndexed { index, it ->
        var text = it.detail ?: it.payload?.decodeToString()
        ?: it.payloadBuilder?.toByteArray()
            ?.decodeToString() ?: "<Payload Lost>"
//      text = text.substring(0 .. minOf(1000, text.length - 1)) + " (${text.length} length)"

        // workaround this Compose bug:
        // https://github.com/JetBrains/compose-multiplatform/issues/2420
        val (textsSplitted, numTextLines) = if (contentWidthInPx != null && numCharsInALine > 0) {
//        remember(text, numCharsInALine) {
            val splitted = text.split('\n')
                .flatMap {
                    if (it.isNotEmpty()) {
                        it.chunked(numCharsInALine)
                    } else {
                        listOf("")
                    }.toMutableList().apply {
                        this[lastIndex] += "\n"
                    }
                }
                .withIndex()
//                .groupBy { it.index / 100 }
                .groupBy { it.index / 1 } // word wrap breaks maxLines of grouped strings

            Pair(
                splitted.map { it.value.joinToString("") { it.value } },
                splitted.map { it.value.size },
            )
//        }
        } else {
            Pair(listOf(" "), listOf(1))
        }
        totalLines += textsSplitted.size
        log.v { "max chars = " + textsSplitted.maxOf { it.length } }
        log.v { "size = ${textsSplitted.size}" }

        textsSplitted.forEachIndexed { textIndex, textChunk ->
            val viewKey = "$protocol-$index-$textIndex-${textChunk.hashCode()}"
            lazyOrNormalItem(scope = scope, key = viewKey, onPrepareComposable = onPrepareComposable) {
                // Not using `height(IntrinsicSize.Min)` because it is buggy.
                Row(
                    modifier = Modifier.padding(
                        start = 6.dp,
                        end = 6.dp,
                        top = if (textIndex == 0) 2.dp else 0.dp,
                        bottom = if (textIndex == textsSplitted.lastIndex) 2.dp else 0.dp
                    )
                ) {
                    DisableSelection {
                        if (textIndex == 0) {
                            TimestampColumn(
                                createTime = it.instant,
                                lastUpdateTime = it.lastUpdateInstant,
                                modifier = Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP)
                                    .padding(end = 1.dp)
                            )
                            AppText(
                                text = when (it.direction) {
                                    RawExchange.Direction.Outgoing -> "> "
                                    RawExchange.Direction.Incoming -> "< "
                                    else -> "= "
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = LocalFont.current.transportTimelineBodyFontSize,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            // cannot filter by it.direction != RawExchange.Direction.Unspecified
                            // otherwise contentWidthInPx keeps changing and causes infinite recompose loops and view overlapping
                            if (protocol?.isHttp2() == true) {
                                AppText(
                                    text = "{${(it.streamId?.toString() ?: "*")}}".padStart(
                                        2 + streamDigits,
                                        ' '
                                    ) + " ",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = LocalFont.current.transportTimelineBodyFontSize,
                                )
                            }
                        } else {
                            Spacer(
                                Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP)
                                    .padding(end = 1.dp)
                            )
                            AppText(
                                text = "  ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = LocalFont.current.transportTimelineBodyFontSize,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            // cannot filter by it.direction != RawExchange.Direction.Unspecified
                            // otherwise contentWidthInPx keeps changing and causes infinite recompose loops and view overlapping
                            if (protocol?.isHttp2() == true) {
                                AppText(
                                    text = " ".repeat("{} ".length + streamDigits),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = LocalFont.current.transportTimelineBodyFontSize,
                                )
                            }
                        }
                    }
                    AppText(
                        text = textChunk,
                        fontFamily = FontFamily.Monospace,
                        fontSize = LocalFont.current.transportTimelineBodyFontSize,
                        maxLines = numTextLines[textIndex],
                        softWrap = false, // since maxLines is always 1
                        modifier = Modifier.weight(1f).onGloballyPositioned {
                            onMeasureContentWidth(it.size.width)
//                            log.v { "pos $index, $textIndex -> ${ it.positionInRoot().y }, ${ it.positionInWindow().y }, ${ it.positionInParent().y }" }
                            val absPos = it.positionInRoot()
                            onPositionLine(
                                index,
                                absPos.x,
                                absPos.y,
                                absPos.y + it.size.height,
                                textIndex,
                                when (textIndex) {
                                    0 -> LineType.FIRST_LINE
                                    textsSplitted.lastIndex -> LineType.LAST_LINE
                                    else -> LineType.MIDDLE_LINE
                                },
                                text,
                            )
                        },
                    )
                    DisposableEffect(viewKey) {
                        onDispose {
                            onDisposeLine(index, textIndex)
                        }
                    }
                }
            }
        }

    }

    if (contentWidthInPx != null && numCharsInALine > 0) {
        onMeasureContentLines(
            TransportTimelineContentMeasureResult(
                measureInputWidthPx = contentWidthInPx,
                totalNumLines = totalLines
            )
        )
    }
}

data class TransportTimelineContentMeasureResult(val measureInputWidthPx: Int?, val totalNumLines: Int)

fun lazyOrNormalItem(
    scope: Any,
    key: Any?,
    onPrepareComposable: (@Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    when (scope) {
        is LazyListScope -> scope.item(key = key) {
            content()
        }
        is ColumnScope -> onPrepareComposable(content)
        else -> throw UnsupportedOperationException()
    }
}

@Composable
fun TimestampColumn(modifier: Modifier = Modifier, createTime: KInstant, lastUpdateTime: KInstant?) {
    var text = DATE_TIME_FORMAT.format(createTime.atZoneOffset(KZoneOffset.local()))
    if (lastUpdateTime != null && lastUpdateTime != createTime) {
        text = "$text ~ ${DATE_TIME_FORMAT.format(lastUpdateTime.atZoneOffset(KZoneOffset.local()))}"
    }

    // sometimes copy button is not working, due to Compose bug:
    // https://github.com/JetBrains/compose-multiplatform/issues/1450
    CopyableContentContainer(textToCopy = text,
        size = 10.dp,
        innerPadding = 2.dp,
        outerPadding = PaddingValues(end = 2.dp)
    ) {
        AppText(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = LocalFont.current.transportTimelineBodyFontSize,
            textAlign = TextAlign.Right,
            modifier = modifier.padding(end = 4.dp)
        )
    }
}

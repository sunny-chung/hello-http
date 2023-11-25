package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolVersion
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset

private val DATE_TIME_FORMAT = KDateTimeFormat("HH:mm:ss.lll")
private val TIMESTAMP_COLUMN_WIDTH_DP = 130.dp

@Composable
fun TransportTimelineView(modifier: Modifier = Modifier, protocol: ProtocolVersion?, exchange: RawExchange) {
    val timestampColumnWidthDp = TIMESTAMP_COLUMN_WIDTH_DP
    val scrollState = rememberLazyListState()
//    val scrollState = rememberScrollState()

    log.d { "TransportTimelineView recompose" }

    val streamDigits = if (protocol?.isHttp2() == true) {
        minOf(
            exchange.exchanges
                .filter { it.streamId != null }
                .maxOf { it.streamId!! }
                .toString()
                .length,
            2
        )
    } else {
        0
    }

    Box(modifier = modifier) {
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
        val density = LocalDensity.current
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

        SelectionContainer {
            LazyColumn(state = scrollState) {
                synchronized(exchange.exchanges) {
//                    items(items = exchange.exchanges) {
//            Column(modifier = Modifier.verticalScroll(scrollState)) {
//                synchronized(exchange.exchanges) {
                        exchange.exchanges.forEachIndexed { index, it ->
                            var text = it.detail ?: it.payload?.decodeToString()
                            ?: it.payloadBuilder?.toByteArray()
                                ?.decodeToString() ?: "<Payload Lost>"
//                            text = text.substring(0 .. minOf(1000, text.length - 1)) + " (${text.length} length)"

                            // workaround this Compose bug:
                            // https://github.com/JetBrains/compose-multiplatform/issues/2420
                            val (textsSplitted, numTextLines) = if (contentWidthInPx != null && numCharsInALine > 0) {
//                                remember(text, numCharsInALine) {
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
//                                        .groupBy { it.index / 100 }
                                        .groupBy { it.index / 1 } // word wrap breaks maxLines of grouped strings

                                    Pair(
                                        splitted.map { it.value.joinToString("") { it.value } },
                                        splitted.map { it.value.size },
                                    )
//                                }
                            } else {
                                Pair(listOf(" "), listOf(1))
                            }
                            log.d { "max chars = " + textsSplitted.maxOf { it.length } }

                            textsSplitted.forEachIndexed { textIndex, textChunk ->
                                item(key = "$protocol-$index-$textIndex-${textChunk.hashCode()}") {
                                    // Not using `height(IntrinsicSize.Min)` because it is buggy.
                                    Row(modifier = Modifier.padding(
                                        start = 6.dp,
                                        end = 6.dp,
                                        top = if (textIndex == 0) 2.dp else 0.dp,
                                        bottom = if (textIndex == textsSplitted.lastIndex) 2.dp else 0.dp
                                    )) {
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
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                                // cannot filter by it.direction != RawExchange.Direction.Unspecified
                                                // otherwise contentWidthInPx keeps changing and causes infinite recompose loops and view overlapping
                                                if (protocol?.isHttp2() == true) {
                                                    AppText(
                                                        text = "{${(it.streamId?.toString() ?: "*")}}".padStart(2 + streamDigits, ' ') + " ",
                                                        fontFamily = FontFamily.Monospace,
                                                    )
                                                }
                                            } else {
                                                Spacer(Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP)
                                                    .padding(end = 1.dp))
                                                AppText(
                                                    text = "  ",
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                                // cannot filter by it.direction != RawExchange.Direction.Unspecified
                                                // otherwise contentWidthInPx keeps changing and causes infinite recompose loops and view overlapping
                                                if (protocol?.isHttp2() == true) {
                                                    AppText(
                                                        text = " ".repeat("{} ".length + streamDigits),
                                                        fontFamily = FontFamily.Monospace,
                                                    )
                                                }
                                            }
                                        }
                                        AppText(
                                            text = textChunk,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = numTextLines[textIndex],
                                            modifier = Modifier.weight(1f).onGloballyPositioned {
                                                contentWidthInPx = it.size.width
                                            },
                                        )
                                    }
                                }
                            }

//                        }
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

@Composable
fun TimestampColumn(modifier: Modifier = Modifier, createTime: KInstant, lastUpdateTime: KInstant?) {
    var text = DATE_TIME_FORMAT.format(createTime.atZoneOffset(KZoneOffset.local()))
    if (lastUpdateTime != null) {
        text = "$text ~ ${DATE_TIME_FORMAT.format(lastUpdateTime.atZoneOffset(KZoneOffset.local()))}"
    }

    AppText(text = text, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Right, modifier = modifier.padding(end = 4.dp))
}

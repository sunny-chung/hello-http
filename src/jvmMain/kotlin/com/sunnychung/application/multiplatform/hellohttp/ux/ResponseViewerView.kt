package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.manager.Prettifier
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.JsonSyntaxHighlightTransformation
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds

@Composable
fun ResponseViewerView(response: UserResponse) {
    val colors = LocalColor.current

    var selectedTabIndex by remember { mutableStateOf(0) }

    log.d { "ResponseViewerView recompose ${response.errorMessage}" }

    val responseViewModel = AppContext.ResponseViewModel
    responseViewModel.setEnabled(response.isCommunicating)
    val updateTime by responseViewModel.subscribe()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            StatusLabel(response = response)
            DurationLabel(response = response, updateTime = updateTime)
            ResponseSizeLabel(response = response)
        }

        val tabs = if (response.application == ProtocolApplication.WebSocket && response.statusCode == 101) {
            listOf(ResponseTab.Stream, ResponseTab.Header, ResponseTab.Raw)
        } else {
            listOf(ResponseTab.Body, ResponseTab.Header, ResponseTab.Raw)
        }
        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            selectedIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it },
            contents = tabs.map {
                { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
            }
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tabs[selectedTabIndex]) {
                ResponseTab.Body -> if (response.body != null || response.errorMessage != null) {
                    ResponseBodyView(response = response)
                } else {
                    ResponseEmptyView(type = "body", isCommunicating = response.isCommunicating, modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                ResponseTab.Stream -> ResponseStreamView(response)

                ResponseTab.Header -> if (response.headers != null) {
                    KeyValueTableView(keyValues = response.headers!!, modifier = Modifier.fillMaxSize().padding(8.dp))
                } else {
                    ResponseEmptyView(type = "header", isCommunicating = response.isCommunicating, modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                ResponseTab.Raw ->
                    TransportTimelineView(protocol = response.protocol, exchange = response.rawExchange.copy(), modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private enum class ResponseTab {
    Body, Stream, Header, Raw
}

@Composable
@Preview
fun ResponseViewerViewPreview() {
    ResponseViewerView(
        UserResponse(id = "preview", requestId = "preview", requestExampleId = "preview").apply {
            startAt = KInstant.now() - KDuration.of(2346, KFixedTimeUnit.MilliSecond)
            endAt = KInstant.now()
            isCommunicating = false
            statusCode = 200
            statusText = "OK"
            responseSizeInBytes = 1234
            body = "{\"OK\"}".toByteArray()
            headers = listOf(
                "Content-Type" to "application/json",
                "Date" to KZonedInstant.nowAtLocalZoneOffset().format(KDateTimeFormat.ISO8601_DATETIME.pattern)
            )
            rawExchange = RawExchange(
                mutableListOf(
                    RawExchange.Exchange(
                        instant = KInstant.now() + KDuration.of(-1, KFixedTimeUnit.MilliSecond),
                        direction = RawExchange.Direction.Outgoing,
                        detail = "Start"
                    ), RawExchange.Exchange(
                        instant = KInstant.now(),
                        direction = RawExchange.Direction.Incoming,
                        detail = "End"
                    )
                )
            )
        }
    )
}

@Composable
@Preview
fun ResponseViewerViewPreview_EmptyBody() {
    ResponseViewerView(
        UserResponse(id = "preview", requestId = "preview", requestExampleId = "preview").apply {
            startAt = KInstant.now() - KDuration.of(2346, KFixedTimeUnit.MilliSecond)
            endAt = KInstant.now()
            isCommunicating = false
            statusCode = 200
            statusText = "OK"
            responseSizeInBytes = 1234
            body = null
            headers = listOf("Content-Type" to "application/json")
            rawExchange = RawExchange(mutableListOf())
        }
    )
}

@Composable
@Preview
fun WebSocketResponseViewerViewPreview() {
    ResponseViewerView(
        UserResponse(id = "preview", requestId = "preview", requestExampleId = "preview").apply {
            startAt = KInstant.now() - KDuration.of(2346, KFixedTimeUnit.MilliSecond)
            endAt = KInstant.now()
            isCommunicating = true
            application = ProtocolApplication.WebSocket
            statusCode = 101
            statusText = "Switching Protocols"
            responseSizeInBytes = 1234
            body = null
            headers = listOf(
                "Content-Type" to "application/json",
                "Date" to KZonedInstant.nowAtLocalZoneOffset().format(KDateTimeFormat.ISO8601_DATETIME.pattern)
            )
            rawExchange = RawExchange(mutableListOf())
            payloadExchanges =
                mutableListOf(

                    PayloadMessage(
                        instant = KInstant.now() - 1234.milliseconds(),
                        type = PayloadMessage.Type.Connected,
                        data = null
                    ),
                    PayloadMessage(
                        instant = KInstant.now() - 605.milliseconds(),
                        type = PayloadMessage.Type.OutgoingData,
                        data = "hi".encodeToByteArray()
                    ),
                    PayloadMessage(
                        instant = KInstant.now() - 599.milliseconds(),
                        type = PayloadMessage.Type.IncomingData,
                        data = "bye".encodeToByteArray()
                    ),
                    PayloadMessage(
                        instant = KInstant.now(),
                        type = PayloadMessage.Type.Disconnected,
                        data = null
                    ),
                )
        }
    )
}

@Composable
fun DataLabel(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color = LocalColor.current.backgroundLight,
    textColor: Color = LocalColor.current.text,
) {
    AppText(
        text = text,
        color = textColor,
        modifier = modifier.background(color = backgroundColor).padding(8.dp)
    )
}

@Composable
fun StatusLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val colors = LocalColor.current
    val (text, backgroundColor) = if (response.isCommunicating && response.statusCode == null) {
        Pair("Communicating", colors.pendingResponseBackground)
    } else if (response.isError) {
        Pair("Error", colors.errorResponseBackground)
    } else {
        val colour = when (response.application) {
            ProtocolApplication.WebSocket -> when (response.statusCode) {
                null -> return
                101 -> colors.successfulResponseBackground
                in 100..199 -> colors.pendingResponseBackground
                else -> colors.errorResponseBackground
            }
            else -> when (response.statusCode) {
                null -> return
                in 100..199 -> colors.pendingResponseBackground
                in 200..399 -> colors.successfulResponseBackground
                else -> colors.errorResponseBackground
            }
        }
        Pair("${response.statusCode} ${response.statusText}", colour)
    }
    DataLabel(modifier = modifier, text = text, backgroundColor = backgroundColor, textColor = colors.bright)
}

@Composable
fun DurationLabel(modifier: Modifier = Modifier, response: UserResponse, updateTime: KInstant) {
    val startAt = response.startAt ?: return
    val timerAt = response.endAt ?: if (response.isCommunicating) KInstant.now() else return
    val duration = timerAt - startAt
    val text = if (duration >= KDuration.of(10, KFixedTimeUnit.Second)) {
        "${"%.1f".format(duration.toMilliseconds() / 1000.0)} s"
    } else {
        "${duration.toMilliseconds()} ms"
    }
    DataLabel(modifier = modifier, text = text)
}

@Composable
fun ResponseSizeLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val size = response.responseSizeInBytes ?: return
    val text = if (size >= 10 * 1024L * 1024L) {
        "${"%.1f".format(size / 1024.0 / 1024.0)} MB"
    } else if (size >= 10 * 1024L) {
        "${"%.1f".format(size / 1024.0)} KB"
    } else {
        "${size} B"
    }
    DataLabel(modifier = modifier, text = text)
}

@Composable
fun ResponseEmptyView(modifier: Modifier = Modifier, type: String, isCommunicating: Boolean) {
    val colours = LocalColor.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppText(
            text = if (!isCommunicating) {
                "No response $type available"
            } else {
                "Waiting for response"
            },
            fontSize = LocalFont.current.largeInfoSize,
            color = if (!isCommunicating) colours.primary else colours.placeholder,
            textAlign = TextAlign.Center
        )
    }
}

class PrettifierDropDownValue(val name: String, val prettifier: Prettifier?) : DropDownable {
    override val displayText: String
        get() = name
}

private val ORIGINAL = "UTF-8 String"
private val CLIENT_ERROR = "Client Error"

@Composable
fun BodyViewerView(
    modifier: Modifier = Modifier,
    content: ByteArray,
    errorMessage: String?,
    prettifiers: List<PrettifierDropDownValue>,
    selectedPrettifierState: MutableState<PrettifierDropDownValue> = remember { mutableStateOf(prettifiers.first()) }
) {
    var selectedView by selectedPrettifierState
    if (selectedView.name !in prettifiers.map { it.name }) {
        selectedView = prettifiers.first()
    }

    log.d { "BodyViewerView recompose" }

    Column(modifier = modifier) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            AppText(text = "View: ")
            DropDownView(items = prettifiers, selectedItem = selectedView, onClickItem = { selectedView = it; true })
        }

        val modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)
        if (selectedView.name != CLIENT_ERROR) {
            CodeEditorView(
                isReadOnly = true,
                text = try {
                    selectedView.prettifier!!.prettify(content)
                } catch (e: Throwable) {
                    content.decodeToString() ?: ""
                },
                transformations = if (selectedView.prettifier!!.formatName.contains("JSON")) {
                    listOf(JsonSyntaxHighlightTransformation(colours = LocalColor.current))
                } else {
                    emptyList()
                },
                modifier = modifier,
            )
        } else {
            CodeEditorView(
                isReadOnly = true,
                text = errorMessage ?: "",
                textColor = LocalColor.current.warning,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun ResponseBodyView(response: UserResponse) {
    val prettifiers = if (!response.isError) {
        val contentType = response.headers
            ?.filter { it.first.lowercase() == "content-type" }
            ?.map { it.second }
            ?.firstOrNull()
        if (contentType != null) {
            AppContext.PrettifierManager.matchPrettifiers(contentType)
        } else {
            emptyList()
        }
            .map { PrettifierDropDownValue(it.formatName, it) } +
                PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { it.decodeToString() })
    } else {
        listOf(PrettifierDropDownValue(CLIENT_ERROR, null))
    }

    log.d { "ResponseBodyView recompose" }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        BodyViewerView(
            content = response.body ?: byteArrayOf(),
            prettifiers = prettifiers,
            errorMessage = response.errorMessage,
            selectedPrettifierState = rememberLast(response.requestExampleId) { mutableStateOf(prettifiers.first()) }
        )

        if (response.postFlightErrorMessage?.isNotEmpty() == true) {
            AppText(text = "Post-flight Error", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            CodeEditorView(
                isReadOnly = true,
                text = response.postFlightErrorMessage ?: "",
                textColor = LocalColor.current.warning,
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
        }
    }
}

private val DATE_TIME_FORMAT = KDateTimeFormat("HH:mm:ss.lll")
private val TIMESTAMP_COLUMN_WIDTH_DP = 120.dp
private val TYPE_COLUMN_WIDTH_DP = 20.dp

@Composable
fun ResponseStreamView(response: UserResponse) {
    var selectedMessage by remember(response.id) { mutableStateOf<PayloadMessage?>(null) }
    val prettifiers = if (response.isError) {
        listOf(PrettifierDropDownValue(CLIENT_ERROR, null))
    } else if (selectedMessage?.type in setOf(PayloadMessage.Type.Connected, PayloadMessage.Type.Disconnected)) {
        listOf(PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { it.decodeToString() }))
    } else {
        AppContext.PrettifierManager.allPrettifiers()
            .map { PrettifierDropDownValue(it.formatName, it) } +
                PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { it.decodeToString() })
    }
    val detailData = selectedMessage?.data

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        BodyViewerView(
            modifier = Modifier.weight(0.5f),
            content = detailData ?: byteArrayOf(),
            prettifiers = prettifiers,
            selectedPrettifierState = remember(
                response.requestExampleId,
                when (selectedMessage?.type) {
                    PayloadMessage.Type.Connected, PayloadMessage.Type.Disconnected -> 0
                    PayloadMessage.Type.IncomingData, PayloadMessage.Type.OutgoingData, null -> 1
                }
            ) { mutableStateOf(prettifiers.first()) },
            errorMessage = null,
        )

        Box(modifier = Modifier.weight(0.5f)) {
            Box(
                Modifier
                    .width(TIMESTAMP_COLUMN_WIDTH_DP + TYPE_COLUMN_WIDTH_DP)
                    .fillMaxHeight()
                    .background(LocalColor.current.backgroundSemiLight)
            )
            Box(
                Modifier
                    .offset(x = TIMESTAMP_COLUMN_WIDTH_DP + TYPE_COLUMN_WIDTH_DP)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(LocalColor.current.line)
            )

            val scrollState = rememberLazyListState()

            LazyColumn(state = scrollState) {
                items(items = response.payloadExchanges?.reversed() ?: emptyList()) {
                    var modifier: Modifier = Modifier
                    modifier = modifier.clickable { selectedMessage = it }
                    Row(modifier = modifier) {
                        AppText(
                            text = DATE_TIME_FORMAT.format(it.instant.atZoneOffset(KZoneOffset.local())),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP)
                        )
                        AppText(
                            text = when (it.type) {
                                PayloadMessage.Type.IncomingData -> "<"
                                PayloadMessage.Type.OutgoingData -> ">"
                                PayloadMessage.Type.Connected -> "="
                                PayloadMessage.Type.Disconnected -> "="
                            },
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(TYPE_COLUMN_WIDTH_DP)
                        )
                        AppText(
                            text = it.data?.decodeToString()?.replace("\\s+".toRegex(), " ") ?: "",
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

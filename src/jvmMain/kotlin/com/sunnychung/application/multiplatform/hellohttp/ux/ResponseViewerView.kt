package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.manager.Prettifier
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.JsonSyntaxHighlightTransformation
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant

@Composable
fun ResponseViewerView(response: UserResponse) {
    val colors = LocalColor.current

    var selectedTabIndex by remember { mutableStateOf(0) }

    log.d { "ResponseViewerView recompose" }

    val responseViewModel = AppContext.ResponseViewModel
    responseViewModel.setEnabled(response.isCommunicating)
    val updateTime by responseViewModel.subscribe()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            StatusLabel(response = response)
            DurationLabel(response = response, updateTime = updateTime)
            ResponseSizeLabel(response = response)
        }

        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            selectedIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it },
            contents = ResponseTab.values().map {
                { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
            }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (ResponseTab.values()[selectedTabIndex]) {
                ResponseTab.Body -> if (response.body != null || response.errorMessage != null) {
                    ResponseBodyView(response = response)
                } else {
                    ResponseEmptyView(type = "body", isCommunicating = response.isCommunicating, modifier = Modifier.fillMaxSize().padding(8.dp))
                }

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
    Body, Header, Raw
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
    val (text, backgroundColor) = if (response.isCommunicating) {
        Pair("Communicating", colors.pendingResponseBackground)
    } else if (response.isError) {
        Pair("Error", colors.errorResponseBackground)
    } else when (response.statusCode) {
        null -> return
        in 100..399 -> Pair("${response.statusCode} ${response.statusText}", colors.successfulResponseBackground)
        else -> Pair("${response.statusCode} ${response.statusText}", colors.errorResponseBackground)
    }
    DataLabel(modifier = modifier, text = text, backgroundColor = backgroundColor, textColor = colors.bright)
}

@Composable
fun DurationLabel(modifier: Modifier = Modifier, response: UserResponse, updateTime: KInstant) {
    val startAt = response.startAt ?: return
    val timerAt = response.endAt ?: KInstant.now()
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

@Composable
fun ResponseBodyView(response: UserResponse) {
    class PrettifierDropDownValue(val name: String, val prettifier: Prettifier?) : DropDownable {
        override val displayText: String
            get() = name
    }
    val ORIGINAL = "UTF-8 String"
    val CLIENT_ERROR = "Client Error"

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
    var selectedView by remember { mutableStateOf(prettifiers.first()) }
    if (selectedView.name !in prettifiers.map { it.name }) {
        selectedView = prettifiers.first()
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            AppText(text = "View: ")
            DropDownView(items = prettifiers, selectedItem = selectedView, onClickItem = { selectedView = it; true })
        }

        val modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)
        if (selectedView.name != CLIENT_ERROR) {
            CodeEditorView(
                isReadOnly = true,
                text = try {
                    selectedView.prettifier!!.prettify(response.body ?: byteArrayOf())
                } catch (e: Throwable) {
                    response.body?.decodeToString() ?: ""
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
                text = response.errorMessage ?: "",
                textColor = LocalColor.current.warning,
                modifier = modifier,
            )
        }

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

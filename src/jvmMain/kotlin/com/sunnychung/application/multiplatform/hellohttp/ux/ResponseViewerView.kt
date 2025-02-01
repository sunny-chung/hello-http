package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.manager.Prettifier
import com.sunnychung.application.multiplatform.hellohttp.model.Certificate
import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurityType
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.PrettifyResult
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.SyntaxHighlight
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.describeApplicationLayer
import com.sunnychung.application.multiplatform.hellohttp.model.hasSomethingToCopy
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.util.debouncedStateOf
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.formatByteSize
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import java.io.ByteArrayInputStream

@Composable
fun ResponseViewerView(response: UserResponse, connectionStatus: ConnectionStatus) {
    val colors = LocalColor.current

    var selectedTabIndex by remember { mutableStateOf(0) }

    log.d { "ResponseViewerView recompose ${response.requestExampleId} $connectionStatus ${response.errorMessage}" }

    val responseViewModel = AppContext.ResponseViewModel
    responseViewModel.setEnabled(connectionStatus.isNotIdle())
    val updateTime by responseViewModel.subscribe()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(IntrinsicSize.Max)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).weight(1f)) {
                StatusLabel(response = response, connectionStatus = connectionStatus)
                DurationLabel(response = response, updateTime = updateTime, connectionStatus = connectionStatus)
                ResponseSizeLabel(response = response)
            }
            response.connectionSecurity?.let {
                AppTooltipArea(
                    tooltipText = "",
                    tooltipContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 480.dp)) {
                            AppText(text = when (it.security) {
                                ConnectionSecurityType.Unencrypted -> "Not encrypted"
                                ConnectionSecurityType.InsecureEncrypted -> "Unverified TLS"
                                ConnectionSecurityType.VerifiedEncrypted -> "One-way TLS"
                                ConnectionSecurityType.MutuallyVerifiedEncrypted -> "mTLS"
                            })
                            CertificateView(title = "Client Certificate", it.clientCertificatePrincipal)
                            CertificateView(title = "Server Certificate", it.peerCertificatePrincipal)
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight()) {
                        when (it.security) {
                            ConnectionSecurityType.Unencrypted -> AppImage(
                                resource = "insecure.svg",
                                color = colors.placeholder,
                                size = 24.dp,
                            )

                            ConnectionSecurityType.InsecureEncrypted -> AppImage(
                                resource = "questionable-secure.svg",
                                color = colors.placeholder,
                                size = 20.dp,
                                modifier = Modifier.padding(2.dp),
                            )

                            ConnectionSecurityType.VerifiedEncrypted -> AppImage(
                                resource = "secure.svg",
                                color = colors.successful,
                                size = 24.dp,
                            )

                            ConnectionSecurityType.MutuallyVerifiedEncrypted -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppImage(
                                    resource = "secure.svg",
                                    color = colors.successful,
                                    size = 24.dp,
                                )
                                AppText(
                                    text = "mTLS",
                                    isFitContent = true,
                                    maxLines = 1,
                                    color = colors.successful,
                                    modifier = Modifier.width(32.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }

        val tabs = if (
            // TODO these conditions are poorly written. any better semantics?
            (response.application == ProtocolApplication.WebSocket && response.statusCode == 101)
            || (response.application == ProtocolApplication.Grpc && response.payloadExchanges != null)
        ) {
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
                    ResponseEmptyView(
                        type = "body",
                        isCommunicating = connectionStatus.isConnectionActive(),
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                            .testTag(TestTag.ResponseBodyEmpty.name)
                    )
                }

                ResponseTab.Stream -> ResponseStreamView(response)

                ResponseTab.Header -> if (response.headers != null) {
                    KeyValueTableView(key = "Response/${response.id}/Headers", keyValues = response.headers!!, isCopyable = true, modifier = Modifier.fillMaxSize().padding(8.dp))
                } else {
                    ResponseEmptyView(type = "header", isCommunicating = connectionStatus.isConnectionActive(), modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                ResponseTab.Raw ->
                    TransportTimelineView(protocol = response.protocol, exchange = response.rawExchange.copy(), response = response, modifier = Modifier.fillMaxSize())
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
        },
        connectionStatus = ConnectionStatus.DISCONNECTED
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
        },
        connectionStatus = ConnectionStatus.DISCONNECTED
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
        },
        connectionStatus = ConnectionStatus.OPEN_FOR_STREAMING
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
fun StatusLabel(modifier: Modifier = Modifier, response: UserResponse, connectionStatus: ConnectionStatus) {
    val colors = LocalColor.current
    val (text, backgroundColor) = if (connectionStatus.isNotIdle() && response.statusCode == null) {
        Pair("Communicating", colors.pendingResponseBackground)
    } else if (response.statusCode != null || !response.statusText.isNullOrEmpty()) {
        val colour = when (response.application) {
            ProtocolApplication.WebSocket -> when (response.statusCode) {
                null -> return
                101 -> colors.successfulResponseBackground
                in 100..199 -> colors.pendingResponseBackground
                else -> colors.errorResponseBackground
            }
            ProtocolApplication.Grpc -> when (response.statusCode) {
                0, null -> colors.successfulResponseBackground
                else -> colors.errorResponseBackground
            }
            else -> when (response.statusCode) {
                null -> return
                in 100..199 -> colors.pendingResponseBackground
                in 200..399 -> colors.successfulResponseBackground
                else -> colors.errorResponseBackground
            }
        }
        Pair("${response.statusCode ?: ""} ${response.statusText}".trim(), colour)
    } else if (response.isError) {
        Pair("Error", colors.errorResponseBackground)
    } else {
        Pair("", colors.errorResponseBackground)
    }
    if (text.isNotEmpty()) {
        DataLabel(
            modifier = modifier.testTag(TestTag.ResponseStatus.name),
            text = text,
            backgroundColor = backgroundColor,
            textColor = colors.bright,
        )
    }
}

@Composable
fun DurationLabel(modifier: Modifier = Modifier, response: UserResponse, updateTime: KInstant, connectionStatus: ConnectionStatus) {
    val startAt = response.startAt ?: return
    val timerAt = response.endAt ?: if (connectionStatus.isConnectionActive()) KInstant.now() else return
    val duration = timerAt - startAt
    updateTime // subscribe to timer update
    log.v { "response duration update to $duration" }
    val text = if (duration >= KDuration.of(10, KFixedTimeUnit.Second)) {
        "${"%.1f".format(duration.toMilliseconds() / 1000.0)} s"
    } else {
        "${duration.toMilliseconds()} ms"
    }
    DataLabel(modifier = modifier.testTag(TestTag.ResponseDuration.name), text = text)
}

@Composable
fun ResponseSizeLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val size = response.responseSizeInBytes ?: return
    val text = formatByteSize(size)
    DataLabel(modifier = modifier, text = text)
}

@Composable
fun CertificateView(title: String, cert: Certificate?) {
    val headerColumnWidth = 136.dp
    val indentWidth = 16.dp
    Column {
        Row {
            AppText(text = title, modifier = Modifier.width(headerColumnWidth))
            if (cert == null) {
                AppText("N/A")
            }
        }
        if (cert != null) {
            Column(modifier = Modifier.padding(start = indentWidth)) {
                val columnWidth = headerColumnWidth - indentWidth
                Row {
                    AppText(text = "Subject", modifier = Modifier.width(columnWidth))
                    AppText(text = cert.principal)
                }
                if (!cert.subjectAlternativeNames.isNullOrEmpty()) {
                    Row {
                        AppText(text = "Alt. Names", modifier = Modifier.width(columnWidth))
                        AppText(text = cert.subjectAlternativeNames.joinToString(", ") {
                            val key = when (it.first) {
                                0 -> "otherName"
                                1 -> "rfc822Name"
                                2 -> "DNS"
                                3 -> "x400Address"
                                4 -> "dirName"
                                5 -> "ediPartyName"
                                6 -> "URI"
                                7 -> "IP"
                                8 -> "RID"
                                else -> return@joinToString "?"
                            }
                            "$key: ${it.second}"
                        })
                    }
                }
                Row {
                    AppText(text = "Issuer", modifier = Modifier.width(columnWidth))
                    AppText(text = cert.issuerPrincipal)
                }
                Row {
                    AppText(text = "Expiry at", modifier = Modifier.width(columnWidth))
                    AppText(text = cert.notAfter.atZoneOffset(KZoneOffset.local()).format(KDateTimeFormat.ISO8601_DATETIME.pattern))
                }
                Row {
                    AppText(text = "Issued at", modifier = Modifier.width(columnWidth))
                    AppText(text = cert.notBefore.atZoneOffset(KZoneOffset.local()).format(KDateTimeFormat.ISO8601_DATETIME.pattern))
                }
            }
        }
    }
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
    override val key: String
        get() = name

    override val displayText: String
        get() = name
}

private val ORIGINAL = "UTF-8 String"
private val CLIENT_ERROR = "Client Error"

private val jsonEncoder = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

@Composable
fun BodyViewerView(
    modifier: Modifier = Modifier,
    key: String,
    content: ByteArray,
    errorMessage: String?,
    prettifiers: List<PrettifierDropDownValue>,
    selectedPrettifierState: MutableState<PrettifierDropDownValue> = remember { mutableStateOf(prettifiers.first()) },
    hasTopCopyButton: Boolean,
    onTopCopyButtonClick: () -> Unit,
) {
    val colours = LocalColor.current
    val fonts = LocalFont.current

    var selectedView by selectedPrettifierState
    if (selectedView.name !in prettifiers.map { it.name }) {
        selectedView = prettifiers.first()
    }

    log.d { "BodyViewerView recompose" }

    val isEnableJsonPath = selectedView.name.contains("json", ignoreCase = true)
    var jsonPathExpression by rememberLast(key) { mutableStateOf("") }
    var isJsonPathError by rememberLast(key) { mutableStateOf(false) }
    val (debouncedJsonPathExpression, _) = debouncedStateOf(400.milliseconds()) { jsonPathExpression }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                AppText(text = "View: ")
                DropDownView(
                    items = prettifiers,
                    selectedItem = selectedView,
                    onClickItem = { selectedView = it; true }
                )
            }
            if (hasTopCopyButton) {
                AppTextButton(
                    text = "Copy All",
                    onClick = onTopCopyButtonClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(top = 4.dp)
                )
            }
        }

        var textFieldPositionTop by remember { mutableStateOf(0f) }
        val modifier = Modifier.fillMaxWidth().weight(1f).padding(top = if (hasTopCopyButton) 2.dp else 6.dp, bottom = 8.dp)
        if (selectedView.name != CLIENT_ERROR) {
            var hasError = false
            var isRaw = true
            val contentToUse = if (isEnableJsonPath && debouncedJsonPathExpression.isNotEmpty()) {
                try {
                    log.d { "jsonpath exp = $debouncedJsonPathExpression" }
                    val data = JsonPath.read<Any?>(ByteArrayInputStream(content), debouncedJsonPathExpression)
                    log.v { "jsonpath data type ${data.javaClass.name} $data" }
                    when (data) {
                        is Map<*, *>, is List<*>, is Set<*> -> { jsonEncoder.writeValueAsBytes(data) }
                        else -> {
                            isRaw = false
                            data.toString().encodeToByteArray()
                        }
                    }
                } catch (e: Throwable) {
                    log.d { "jsonpath err = ${e.message}" }
                    hasError = true
                    content
                }
            } else {
                content
            }
            isJsonPathError = hasError

            val prettifyResult = remember(contentToUse, selectedView) {
                try {
                    if (isRaw) {
                        selectedView.prettifier!!.prettify(contentToUse)
                    } else {
                        PrettifyResult(contentToUse.decodeToString())
                    }
                } catch (e: Throwable) {
                    PrettifyResult(contentToUse.decodeToString() ?: "")
                }
            }

            CopyableContentContainer(
                textToCopy = prettifyResult.prettyString,
                outerPadding = PaddingValues(
                    top = 6.dp + with(LocalDensity.current) { textFieldPositionTop.toDp() },
                    end = 12.dp
                ),
                modifier = modifier
            ) {
                CodeEditorView(
                    cacheKey = "$key/View:${selectedView.key};Query:${
                        debouncedJsonPathExpression
                            .emptyToNull()
                            ?.takeIf { !isJsonPathError }
                            ?: ""
                    }",
                    isReadOnly = true,
                    initialText = prettifyResult.prettyString,
                    collapsableLines = prettifyResult.collapsableLineRange,
                    collapsableChars = prettifyResult.collapsableCharRange,
                    syntaxHighlight = if (selectedView.prettifier!!.formatName.contains("JSON")) SyntaxHighlight.Json else SyntaxHighlight.None,
                    onMeasured = { textFieldPositionTop = it },
                    testTag = TestTag.ResponseBody.name,
                )
            }
        } else {
            val text = errorMessage ?: content.decodeToString()
            CopyableContentContainer(
                textToCopy = text,
                outerPadding = PaddingValues(
                    top = 6.dp + with(LocalDensity.current) { textFieldPositionTop.toDp() },
                    end = 12.dp
                ),
                modifier = modifier
            ) {
                CodeEditorView(
                    cacheKey = "$key/Error",
                    isReadOnly = true,
                    initialText = text,
                    textColor = colours.warning,
                    syntaxHighlight = SyntaxHighlight.None,
                    onMeasured = { textFieldPositionTop = it },
                    testTag = TestTag.ResponseError.name,
                )
            }
        }
        if (isEnableJsonPath) {
            AppTextFieldWithPlaceholder(
                key = "$key/JSONPath",
                value = jsonPathExpression,
                onValueChange = { jsonPathExpression = it },
                textColor = if (!isJsonPathError) colours.text else colours.warning,
                placeholder = {
                    AppText(
                        text = "JSON Path, e.g. $.items.length()",
                        fontFamily = fonts.monospaceFontFamily,
                        fontSize = fonts.codeEditorBodyFontSize,
                        color = colours.placeholder
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = fonts.monospaceFontFamily,
                    fontSize = fonts.codeEditorBodyFontSize,
                ),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
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
            AppContext.PrettifierManager.matchPrettifiers(response.application, contentType)
        } else {
            emptyList()
        }
            .map { PrettifierDropDownValue(it.formatName, it) } +
                PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { PrettifyResult(it.decodeToString()) })
    } else {
        listOf(PrettifierDropDownValue(CLIENT_ERROR, null))
    }

    val clipboardManager = LocalClipboardManager.current

    log.d { "ResponseBodyView recompose" }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        BodyViewerView(
            key = "Response:${response.id}/Body",
            content = response.body ?: byteArrayOf(),
            prettifiers = prettifiers,
            errorMessage = response.errorMessage,
            selectedPrettifierState = rememberLast(response.requestExampleId) { mutableStateOf(prettifiers.first()) },
            hasTopCopyButton = response.hasSomethingToCopy(),
            onTopCopyButtonClick = {
                val textToCopy = response.describeApplicationLayer()
                clipboardManager.setText(AnnotatedString(textToCopy))
                AppContext.ErrorMessagePromptViewModel.showSuccessMessage("Copied text")
            }
        )

        if (response.postFlightErrorMessage?.isNotEmpty() == true) {
            AppText(text = "Post-flight Error", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            CodeEditorView(
                cacheKey = "Response:${response.id}/PostflightError",
                isReadOnly = true,
                initialText = response.postFlightErrorMessage ?: "",
                textColor = LocalColor.current.warning,
                syntaxHighlight = SyntaxHighlight.None,
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun CopyableContentContainer(
    modifier: Modifier = Modifier,
    textToCopy: String,
    isEnabled: Boolean = true,
    size: Dp = 20.dp,
    innerPadding: Dp = 4.dp,
    outerPadding: PaddingValues = PaddingValues(top = 4.dp, end = 12.dp),
    contentView: @Composable () -> Unit
) {
    if (!isEnabled) {
        Box(modifier = modifier) {
            contentView()
        }
        return
    }

    var isShowCopyButton by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) {
                isShowCopyButton = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isShowCopyButton = false
            }
    ) {
        contentView()
        if (isShowCopyButton) {
            FloatingCopyButton(
                textToCopy = textToCopy,
                size = size,
                innerPadding = innerPadding,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(outerPadding)
            )
        }
    }
}

private val DATE_TIME_FORMAT = KDateTimeFormat("HH:mm:ss.lll")
private val TIMESTAMP_COLUMN_WIDTH_DP = 100.dp
private val TYPE_COLUMN_WIDTH_DP = 20.dp

@Composable
fun ResponseStreamView(response: UserResponse) {
    val colours = LocalColor.current
    val fonts = LocalFont.current
    val clipboardManager = LocalClipboardManager.current

    var selectedMessage by rememberLast(response.id) { mutableStateOf<PayloadMessage?>(null) }
    val displayMessage = selectedMessage ?: response.payloadExchanges?.lastOrNull { it.type in setOf(PayloadMessage.Type.IncomingData, PayloadMessage.Type.Error) } // last -> largest timestamp
    val prettifiers = if ((response.isError && displayMessage == null) || displayMessage?.type == PayloadMessage.Type.Error) {
        listOf(PrettifierDropDownValue(CLIENT_ERROR, null))
    } else if (displayMessage?.type in setOf(PayloadMessage.Type.Connected, PayloadMessage.Type.Disconnected)) {
        listOf(PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { PrettifyResult(it.decodeToString()) }))
    } else {
        AppContext.PrettifierManager.allPrettifiers()
            .map { PrettifierDropDownValue(it.formatName, it) } +
                PrettifierDropDownValue(ORIGINAL, Prettifier(ORIGINAL) { PrettifyResult(it.decodeToString()) })
    }
    val detailData = displayMessage?.data

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        BodyViewerView(
            modifier = Modifier.weight(0.6f),
            key = "Response:${response.id}/Stream:${displayMessage?.id}/Body",
            content = detailData ?: byteArrayOf(),
            prettifiers = prettifiers,
            selectedPrettifierState = remember(
                response.requestExampleId,
                when (displayMessage?.type) { // categorize prettifiers as cache keys
                    PayloadMessage.Type.Connected, PayloadMessage.Type.Disconnected -> 0
                    PayloadMessage.Type.IncomingData, PayloadMessage.Type.OutgoingData, null -> 1
                    PayloadMessage.Type.Error -> 2
                }
            ) { mutableStateOf(prettifiers.first()) },
            errorMessage = null,
            hasTopCopyButton = response.hasSomethingToCopy(),
            onTopCopyButtonClick = {
                val textToCopy = response.describeApplicationLayer()
                clipboardManager.setText(AnnotatedString(textToCopy))
                AppContext.ErrorMessagePromptViewModel.showSuccessMessage("Copied text")
            }
        )

        Box(modifier = Modifier.weight(0.4f).testTag(TestTag.ResponseStreamLog.name)) {
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
                synchronized(response.payloadExchanges ?: Any()) {
                    items(items = response.payloadExchanges?.reversed() ?: emptyList()) {
                        var modifier: Modifier = Modifier
                        modifier = modifier.clickable { selectedMessage = it }
                        Row(modifier = modifier) {
                            val textColour = if (displayMessage?.id == it.id) {
                                colours.highlight
                            } else {
                                colours.primary
                            }
                            AppText(
                                text = DATE_TIME_FORMAT.format(it.instant.atZoneOffset(KZoneOffset.local())),
                                color = textColour,
                                fontFamily = fonts.monospaceFontFamily,
                                fontSize = fonts.streamFontSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP)
                                    .testTag(TestTag.ResponseStreamLogItemTime.name)
                            )
                            AppText(
                                text = when (it.type) {
                                    PayloadMessage.Type.IncomingData -> "<"
                                    PayloadMessage.Type.OutgoingData -> ">"
                                    PayloadMessage.Type.Connected -> "="
                                    PayloadMessage.Type.Disconnected -> "="
                                    PayloadMessage.Type.Error -> "x"
                                },
                                color = textColour,
                                fontFamily = fonts.monospaceFontFamily,
                                fontSize = fonts.streamFontSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(TYPE_COLUMN_WIDTH_DP)
                            )
                            AppText(
                                text = it.data?.decodeToString()?.replace("\\s+".toRegex(), " ") ?: "",
                                color = textColour,
                                isDisableWordWrap = true,
                                softWrap = false,
                                maxLines = 1,
                                fontFamily = fonts.monospaceFontFamily,
                                fontSize = fonts.streamFontSize,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                            )
                        }
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

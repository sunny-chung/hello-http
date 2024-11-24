package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.extension.countNotBlank
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcMethod
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.SyntaxHighlight
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.isValidHttpMethod
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.hostFromUrl
import com.sunnychung.application.multiplatform.hellohttp.parser.JsonParser
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithIndexedChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextManipulator
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import graphql.language.OperationDefinition
import graphql.language.OperationDefinition.Operation
import java.io.File

@Composable
fun RequestEditorView(
    modifier: Modifier = Modifier,
    request: UserRequestTemplate,
    selectedExampleId: String,
    editExampleNameViewModel: EditNameViewModel,
    grpcApiSpecs: List<GrpcApiSpec>,
    environment: Environment?,
    onSelectExample: (UserRequestExample) -> Unit,
    onClickSend: () -> Unit,
    onClickCancel: () -> Unit,
    onClickCopyCurl: () -> Boolean,
    onClickCopyGrpcurl: (selectedPayloadExampleId: String, method: GrpcMethod) -> Boolean,
    onClickCopyPowershellInvokeWebrequest: () -> Boolean,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    connectionStatus: ConnectionStatus,
    onClickConnect: () -> Unit,
    onClickDisconnect: () -> Unit,
    onClickSendPayload: (String) -> Unit,
    onClickCompleteStream: () -> Unit,
    onClickFetchApiSpec: () -> Unit,
    onClickCancelFetchApiSpec: () -> Unit,
    isFetchingApiSpec: Boolean,
) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    var selectedExample = request.examples.firstOrNull { it.id == selectedExampleId }
    val baseExample = request.examples.first()

    if (selectedExample == null) {
        selectedExample = baseExample
        onSelectExample(baseExample)
    } // do not use `selectedExampleId` directly, because selectedExample can be changed

    var selectedRequestTabIndex by remember { mutableStateOf(0) }

    val environmentVariables = environment?.variables?.filter { it.isEnabled }?.map { it.key to it.value }?.toMap() ?: emptyMap()
    val exampleVariables = request.getExampleVariablesOnly(selectedExample.id)
    val mergedVariables = request.getAllVariables(selectedExample.id, environment)

    val currentGraphqlOperation = if (request.application == ProtocolApplication.Graphql) {
        (selectedExample.body as? GraphqlBody)?.getOperation(isThrowError = false)
    } else {
        null
    }
    val currentGrpcMethod = grpcApiSpecs.firstOrNull { it.id == request.grpc?.apiSpecId }
        ?.methods
        ?.firstOrNull { it.serviceFullName == request.grpc?.service && it.methodName == request.grpc?.method }
    val hasPayloadEditor = (request.application == ProtocolApplication.WebSocket
            || (request.application == ProtocolApplication.Grpc && currentGrpcMethod?.isClientStreaming == true)
            )
    var selectedPayloadExampleId by rememberLast(request.id, request.application) { mutableStateOf(request.payloadExamples?.firstOrNull()?.id) }

    val isEnableSendButton = when (connectionStatus.isConnectionActive()) {
        true -> true
        false -> when (request.application) {
            ProtocolApplication.Grpc -> currentGrpcMethod != null
            else -> true
        }
    }

    var isShowCustomHttpMethodDialog by remember { mutableStateOf(false) }

    log.d { "RequestEditorView recompose $request" }

    CustomHttpMethodDialog(
        isEnabled = isShowCustomHttpMethodDialog,
        onDismiss = { isShowCustomHttpMethodDialog = false },
        onConfirm = { customHttpMethod ->
            if (customHttpMethod.isValidHttpMethod()) {
                onRequestModified(
                    request.copyForApplication(
                        application = ProtocolApplication.Http,
                        method = customHttpMethod
                    )
                )
                isShowCustomHttpMethodDialog = false
            }
        },
    )

    Column(modifier = modifier
        .onKeyEvent { e ->
            if (isEnableSendButton && e.type == KeyEventType.KeyDown && e.key == Key.Enter && !e.isAltPressed && !e.isShiftPressed) {
                val currentOS = currentOS()
                if ( (currentOS != MacOS && e.isCtrlPressed && !e.isMetaPressed) ||
                    (currentOS == MacOS && !e.isCtrlPressed && e.isMetaPressed) ) {
                    onClickSend()
                    return@onKeyEvent true
                }
            }
            false
        }
    ) {
        Row(
            modifier = Modifier
                .background(color = colors.backgroundInputField)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val customHttpMethodOption = DropDownKeyValue(
                key = ProtocolMethod(application = ProtocolApplication.Http, method = "<Custom>"),
                displayText = "Custom"
            )
            val options = DropDownMap(
                listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                    .map { DropDownKeyValue(
                        key = ProtocolMethod(application = ProtocolApplication.Http, method = it),
                        displayText = it
                    ) }
                + listOf(
                    customHttpMethodOption,
                    DropDownKeyValue(
                        key = ProtocolMethod(application = ProtocolApplication.Graphql, method = ""),
                        displayText = "GraphQL"
                    ),
                    DropDownKeyValue(
                        key = ProtocolMethod(application = ProtocolApplication.Grpc, method = ""),
                        displayText = "gRPC"
                    ),
                    DropDownKeyValue(
                        key = ProtocolMethod(application = ProtocolApplication.WebSocket, method = ""),
                        displayText = "WebSocket"
                    ),
                )
            )
            DropDownView(
                selectedItem = options.dropdownables.firstOrNull {
                    it.key.application == request.application && (it.key.method == request.method || it.key.method.isEmpty())
                } ?: customHttpMethodOption,
                items = options.dropdownables,
                contentView = { it, isLabel, isSelected, isClickable ->
                    val (text, color) = if (isLabel && it!!.key.application == ProtocolApplication.Http && it!!.key.method == "<Custom>") {
                        Pair(request.method, colors.httpRequestOthers)
                    } else {
                        when (it!!.key.application) {
                            ProtocolApplication.Http -> Pair(
                                it.displayText,
                                when (it.displayText) {
                                    "GET" -> colors.httpRequestGet
                                    "POST" -> colors.httpRequestPost
                                    "PUT" -> colors.httpRequestPut
                                    "PATCH" -> colors.httpRequestPatch
                                    "DELETE" -> colors.httpRequestDelete
                                    else -> colors.httpRequestOthers
                                }
                            )

                            ProtocolApplication.WebSocket -> Pair("WebSocket", colors.websocketRequest)
                            ProtocolApplication.Grpc -> Pair("gRPC", colors.grpcRequest)
                            ProtocolApplication.Graphql -> Pair("GraphQL", colors.graphqlRequest)
                        }
                    }
                    val modifier = if (isLabel) {
                        Modifier
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) colors.backgroundLight else Color.Transparent)
                    }
                    AppText(
                        text = text.emptyToNull() ?: "--",
                        color = color,
                        isFitContent = true,
                        textAlign = TextAlign.Left,
                        maxLines = 1,
                        modifier = modifier.padding(horizontal = 8.dp) //.width(width = 48.dp)
                            .run {
                                if (isLabel) {
                                    testTag(
                                        buildTestTag(
                                            TestTagPart.RequestMethodDropdown,
                                            TestTagPart.DropdownLabel
                                        )!!
                                    )
                                } else {
                                    this
                                }
                            }
                    )
                },
                onClickItem = onClickItem@ {
                    if (request.application != it.key.application) {
                        selectedRequestTabIndex = 0
                    }

                    if (it.key.application == ProtocolApplication.Http && it.key.method == "<Custom>") {
                        isShowCustomHttpMethodDialog = true
                        return@onClickItem true
                    }

                    onRequestModified(request.copyForApplication(application = it.key.application, method = it.key.method))
                    true
                },
                testTagParts = arrayOf(TestTagPart.RequestMethodDropdown),
                modifier = Modifier.fillMaxHeight()
            )

            AppTextField(
                value = request.url,
                onValueChange = {
                    onRequestModified(request.copy(url = it))
                },
                placeholder = { AppText(text = "URL", color = colors.placeholder) },
                visualTransformation = EnvironmentVariableTransformation(
                    themeColors = colors,
                    font = fonts,
                    knownVariables = mergedVariables.keys
                ),
                singleLine = true,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    .testTag(TestTag.RequestUrlTextField.name)
            )

            val isOneOffRequest = when (request.application) {
                ProtocolApplication.WebSocket -> false
                ProtocolApplication.Graphql -> currentGraphqlOperation?.operation in setOf(
                    Operation.QUERY,
                    Operation.MUTATION
                )
                ProtocolApplication.Grpc -> currentGrpcMethod?.isClientStreaming != true
                else -> true
            }
            val dropdownItems: List<SendButtonDropdown> = when (request.application) {
                ProtocolApplication.WebSocket -> emptyList()
                ProtocolApplication.Graphql -> if (isOneOffRequest) listOf(SendButtonDropdown.CurlForLinux, SendButtonDropdown.PowershellInvokeWebrequestForWindows) else emptyList()
                ProtocolApplication.Grpc -> listOf(SendButtonDropdown.GrpcurlForLinux)
                else -> listOf(SendButtonDropdown.CurlForLinux, SendButtonDropdown.PowershellInvokeWebrequestForWindows)
            }
            val (label, backgroundColour) = if (!connectionStatus.isNotIdle()) {
                Pair(if (isOneOffRequest) "Send" else "Connect", colors.backgroundButton)
            } else {
                Pair(if (isOneOffRequest) "Cancel" else "Disconnect", colors.backgroundStopButton)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(backgroundColour).width(IntrinsicSize.Max).widthIn(min = 84.dp).fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxHeight().weight(1f)
                    .run {
                        if (isEnableSendButton) {
                            clickable {
                                if (!connectionStatus.isConnectionActive()) {
                                    onClickSend()
                                } else {
                                    onClickCancel()
                                }
                            }
                        } else {
                            this
                        }
                    }
                    .padding(start = 10.dp, end = if (dropdownItems.isNotEmpty()) 4.dp else 10.dp)
                    .testTag(TestTag.RequestFireOrDisconnectButton.name)
                ) {
                    AppText(
                        text = label,
                        color = if (isEnableSendButton) colors.primary else colors.disabled,
                        fontSize = fonts.buttonFontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (dropdownItems.isNotEmpty()) {
                    DropDownView(
                        iconSize = 24.dp,
                        items = dropdownItems.map { DropDownValue(it.displayText) },
                        isShowLabel = false,
                        onClickItem = {
                            var isSuccess = true
                            when (it.displayText) {
                                SendButtonDropdown.CurlForLinux.displayText -> {
                                    isSuccess = onClickCopyCurl()
                                }
                                SendButtonDropdown.PowershellInvokeWebrequestForWindows.displayText -> {
                                    isSuccess = onClickCopyPowershellInvokeWebrequest()
                                }
                                SendButtonDropdown.GrpcurlForLinux.displayText -> {
                                    isSuccess = try {
                                        onClickCopyGrpcurl(selectedPayloadExampleId!!, currentGrpcMethod!!)
                                    } catch (e: Throwable) {
                                        log.d(e) { "Cannot copy grpcurl command" }
                                        false
                                    }
                                }
                            }
                            isSuccess
                        },
                        arrowPadding = PaddingValues(end = 4.dp),
                        modifier = Modifier.fillMaxHeight(),
                    )
                }
            }
        }
        if (request.application == ProtocolApplication.Grpc) {
            if (request.grpc?.apiSpecId.emptyToNull() == null && grpcApiSpecs.isNotEmpty()) {
                val name = hostFromUrl(request.url)
                grpcApiSpecs.firstOrNull { it.name == name }
                    ?.let { onRequestModified(request.copy(grpc = (request.grpc ?: UserGrpcRequest()).copy(apiSpecId = it.id))) }
            }
            RequestServiceMethodSelector(
                modifier = Modifier.fillMaxWidth(),
                service = request.grpc?.service ?: "",
                method = request.grpc?.method ?: "",
                onSelectService = { onRequestModified(request.copy(grpc = (request.grpc ?: UserGrpcRequest()).copy(service = it))) },
                onSelectMethod = { onRequestModified(request.copy(grpc = (request.grpc ?: UserGrpcRequest()).copy(method = it))) },
                apiSpec = grpcApiSpecs.firstOrNull { it.id == request.grpc?.apiSpecId },
                onSelectApiSpec = { onRequestModified(request.copy(grpc = (request.grpc ?: UserGrpcRequest()).copy(apiSpecId = it))) },
                apiSpecList = grpcApiSpecs,
                onClickFetchApiSpec = onClickFetchApiSpec,
                onClickCancelFetchApiSpec = onClickCancelFetchApiSpec,
                isFetchingApiSpec = isFetchingApiSpec,
            )
        }

//        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            var isEditing = editExampleNameViewModel.isEditing.collectAsState().value

            AppText(text = "Examples: ")

            TabsView(
                modifier = Modifier.weight(1f).background(color = colors.backgroundLight),
                selectedIndex = request.examples.indexOfFirst { it.id == selectedExample.id },
                onSelectTab = {
                    val selectedExample = request.examples[it]
                    onSelectExample(selectedExample)
//                    selectedContentType = selectedExample.contentType
                },
                onDoubleClickTab = {
                    log.d { "req ex onDoubleClickTab $it" }
                    val selectedExample = request.examples[it]
                    onSelectExample(selectedExample)
                    if (it > 0) { // the "Base" example cannot be renamed
                        editExampleNameViewModel.onStartEdit(selectedExample.id)
                    }
                },
                contents = request.examples.mapIndexed { index, it ->
                    {
                        if (isEditing && selectedExample.id == it.id) {
                            log.d { "req ex edit ${selectedExample.id}" }
                            val focusRequester = remember { FocusRequester() }
                            val focusManager = LocalFocusManager.current
                            var textFieldState by remember { mutableStateOf(TextFieldValue(it.name, selection = TextRange(0, it.name.length))) }
                            AppTextField(
                                value = textFieldState,
                                onValueChange = { textFieldState = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { f ->
                                        log.d { "RequestListView onFocusChanged ${f.hasFocus} ${f.isFocused}" }
                                        if (!f.hasFocus && editExampleNameViewModel.isInvokeModelUpdate()) {
                                            onRequestModified(request.copy(examples = request.examples.copyWithChange(it.copy(name = textFieldState.text))))
                                        }
                                        editExampleNameViewModel.onTextFieldFocusChange(f)
                                    }
                                    .onKeyEvent { e -> // TODO refactor to reduce code duplication
                                        when (e.key) {
                                            Key.Enter -> {
                                                log.d { "key enter" }
                                                focusManager.clearFocus()
                                            }
                                            Key.Escape -> {
                                                log.d { "key escape" }
                                                editExampleNameViewModel.onUserCancelEdit()
                                                focusManager.clearFocus()
                                            }
                                            else -> {
                                                return@onKeyEvent false
                                            }
                                        }
                                        true
                                    }
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                AppText(text = it.name)
                                AppTooltipArea(tooltipText = "Duplicate") {
                                    AppImageButton(
                                        resource = "duplicate.svg",
                                        size = 16.dp,
                                        color = colors.placeholder
                                    ) {
                                        val copiedExample = it.deepCopyWithNewId()
                                        log.d { "copied example -> ${copiedExample.id}" }
                                        onRequestModified(
                                            request.copy(examples = request.examples.let { examples ->
                                                val r = examples.toMutableList()
                                                r.add(index + 1, copiedExample)
                                                log.d { "new examples = [\n${r.joinToString { "  ${it.id} -> ${it.name}\n" }}]" }
                                                r
                                            })
                                        )
                                        onSelectExample(copiedExample)
                                        log.d { "onStartEdit ${copiedExample.id}" }
                                        editExampleNameViewModel.onStartEdit(copiedExample.id)
                                    }
                                }
                                if (index > 0) { // "Base" example cannot be deleted
                                    AppDeleteButton {
                                        onRequestModified(
                                            request.copy(examples = request.examples.copyWithRemovedIndex(index))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )

            AppImageButton(
                resource = "add.svg",
                size = 24.dp,
                onClick = {
                    val newExample = UserRequestExample.create(request.application)
                    onRequestModified(
                        request.copy(examples = request.examples + newExample)
                    )
                    onSelectExample(newExample)
                    editExampleNameViewModel.onStartEdit(newExample.id)
                },
                modifier = Modifier.padding(4.dp)
            )

            DropDownView(
                iconResource = "list-ul-alt.svg",
                iconSize = 24.dp,
                items = request.examples.map { DropDownKeyValue(it.id, it.name) },
                isShowLabel = false,
                onClickItem = { clicked ->
                    request.examples.firstOrNull { it.id == clicked.key }?.let {
                        onSelectExample(it)
                        true
                    } ?: false
                },
                modifier = Modifier.padding(4.dp)
            )
        }

        val tabs = when (request.application) {
            ProtocolApplication.WebSocket -> listOf(RequestTab.Query, RequestTab.Header, RequestTab.Variable)
            ProtocolApplication.Grpc -> listOfNotNull(
                if (currentGrpcMethod?.isClientStreaming != true) RequestTab.Body else null,
                RequestTab.Header, RequestTab.Variable, RequestTab.PostFlight
            )
            else -> listOf(RequestTab.Body, RequestTab.Query, RequestTab.Header, RequestTab.Variable, RequestTab.PreFlight, RequestTab.PostFlight)
        }

        fun isApplicable(property: (UserRequestExample) -> Boolean?): Int {
            return if (selectedExample.id == baseExample.id) {
                1
            } else if (property(selectedExample) == true) {
                1
            } else {
                0
            }
        }

        fun List<UserKeyValuePair>.countActive() = count { it.isEnabled }

        val tabBadgeNum = { tab: RequestTab -> when (tab) {
            RequestTab.Body -> when (val body = selectedExample.body) {
                is FileBody -> isApplicable { it.overrides?.isOverrideBody } * body.filePath.countNotBlank()
                is FormUrlEncodedBody -> body.value.countActive()
                is MultipartBody -> body.value.countActive()
                is GraphqlBody -> isApplicable { it.overrides?.isOverrideBodyContent } * body.document.countNotBlank() +
                        isApplicable { it.overrides?.isOverrideBodyVariables } * body.variables.countNotBlank()
                is StringBody -> isApplicable { it.overrides?.isOverrideBody } * body.value.countNotBlank()
                null -> 0
            }
            RequestTab.Query -> selectedExample.queryParameters.countActive()
            RequestTab.Header -> selectedExample.headers.countActive()
            RequestTab.PreFlight -> isApplicable { it.overrides?.isOverridePreFlightScript } *
                selectedExample.preFlight.executeCode.countNotBlank()
            RequestTab.PostFlight -> selectedExample.postFlight.updateVariablesFromHeader.countActive() +
                selectedExample.postFlight.updateVariablesFromBody.countActive()
            RequestTab.Variable -> selectedExample.variables.countActive()
        } }

        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            selectedIndex = selectedRequestTabIndex,
            onSelectTab = { selectedRequestTabIndex = it },
            contents = tabs.map {
                {
                    val badgeNum = tabBadgeNum(it)
                    Row(Modifier.padding(8.dp)) {
                        AppTabLabel(text = it.displayText, isIncludePadding = false)
                        if (badgeNum != 0) {
                            var size by remember(LocalDensity.current) { mutableStateOf(IntSize(0, 0)) }
                            val longerEdgeLengthDp = with (LocalDensity.current) {
                                maxOf(size.width, size.height).toDp()
                            }
                            Box(modifier = Modifier.padding(start = 1.dp)
                                .background(colors.backgroundTooltip, CircleShape)
                                .align(Alignment.Top)
                                .onGloballyPositioned { size = it.size }
                                .sizeIn(minWidth = longerEdgeLengthDp, minHeight = longerEdgeLengthDp)) {
                                AppText(
                                    text = badgeNum.toString(),
                                    fontSize = fonts.badgeFontSize,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            },
            testTag = TestTag.RequestParameterTypeTab.name,
        )
        Box(
            modifier = if (!hasPayloadEditor) {
                Modifier.weight(1f)
            } else {
                Modifier.weight(0.3f)
            }.fillMaxWidth()
        ) {
            when (tabs[selectedRequestTabIndex]) {
                RequestTab.Body -> RequestBodyEditor(
                    request = request,
                    onRequestModified = onRequestModified,
                    selectedExample = selectedExample,
                    environmentVariables = mergedVariables,
                    currentGraphqlOperation = currentGraphqlOperation,
                )

                RequestTab.Header ->
                    RequestKeyValueEditorView(
                        value = selectedExample.headers,
                        onValueUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.copy(
                                            headers = it
                                        )
                                    )
                                )
                            )
                        },
                        baseValue = if (selectedExample.id != baseExample.id) baseExample.headers else null,
                        baseDisabledIds = selectedExample.overrides?.disabledHeaderIds ?: emptySet(),
                        onDisableUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.run {
                                            copy(overrides = overrides!!.copy(disabledHeaderIds = it))
                                        }
                                    )
                                )
                            )
                        },
                        knownVariables = mergedVariables,
                        isSupportFileValue = false,
                        testTagPart = TestTagPart.RequestHeader,
                        modifier = Modifier.fillMaxWidth(),
                    )

                RequestTab.Query ->
                    RequestKeyValueEditorView(
                        value = selectedExample.queryParameters,
                        onValueUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.copy(
                                            queryParameters = it
                                        )
                                    )
                                )
                            )
                        },
                        baseValue = if (selectedExample.id != baseExample.id) baseExample.queryParameters else null,
                        baseDisabledIds = selectedExample.overrides?.disabledQueryParameterIds ?: emptySet(),
                        onDisableUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.run {
                                            copy(overrides = overrides!!.copy(disabledQueryParameterIds = it))
                                        }
                                    )
                                )
                            )
                        },
                        knownVariables = mergedVariables,
                        isSupportFileValue = false,
                        testTagPart = TestTagPart.RequestQueryParameter,
                        modifier = Modifier.fillMaxWidth(),
                    )

                RequestTab.Variable -> {
                    val testTagPart = TestTagPart.ExampleVariable
                    val baseDisabledIds = selectedExample.overrides?.disabledVariables ?: emptySet()
                    val onDisableUpdate: (Set<String>) -> Unit = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.run {
                                        copy(overrides = overrides!!.copy(disabledVariables = it))
                                    }
                                )
                            )
                        )
                    }
                    val onValueUpdate: (List<UserKeyValuePair>) -> Unit = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.copy(
                                        variables = it
                                    )
                                )
                            )
                        )
                    }

                    val data = selectedExample.variables
                    val activeValueKeys = data.filter { it.isEnabled }.map { it.key }.toSet()
                    val baseValues = if (selectedExample.id != baseExample.id) baseExample.variables else emptyList()
                    val activeBaseValues = baseValues.filter { it.isEnabled }
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState())) {
                        if (environmentVariables.isNotEmpty() || activeBaseValues.isNotEmpty()) {
                            InputFormHeader(text = "This Example")
                        }

                        KeyValueEditorView(
                            keyValues = data,
                            isSupportVariables = true,
                            knownVariables = environmentVariables,
                            isInheritedView = false,
                            disabledIds = emptySet(),
                            onItemChange = { index, item ->
                                log.d { "onItemChange" }
                                onValueUpdate(data.copyWithIndexedChange(index, item))
                            },
                            onItemAddLast = { item ->
                                log.d { "onItemAddLast" }
                                onValueUpdate(data + item)
                            },
                            onItemDelete = { index ->
                                log.d { "onItemDelete" }
                                onValueUpdate(data.copyWithRemovedIndex(index))
                            },
                            onDisableChange = {_ ->},
                            testTagPart1 = testTagPart,
                            testTagPart2 = TestTagPart.Current,
                        )

                        if (activeBaseValues.isNotEmpty()) {
                            InputFormHeader(text = "Inherited from Base", modifier = Modifier.padding(top = 12.dp))
                            KeyValueEditorView(
                                keyValues = activeBaseValues.map {
                                    it.copy(isEnabled = it.isEnabled && it.key !in activeValueKeys)
                                },
                                isSupportVariables = true,
                                isSupportDisable = false,
                                knownVariables = environmentVariables,
                                disabledIds = baseDisabledIds,
                                isInheritedView = true,
                                onItemChange = {_, _ ->},
                                onItemAddLast = {_ ->},
                                onItemDelete = {_ ->},
                                onDisableChange = onDisableUpdate,
                                testTagPart1 = testTagPart,
                                testTagPart2 = TestTagPart.Inherited,
                            )
                        }

                        if (environmentVariables.isNotEmpty()) {
                            InputFormHeader(
                                text = "Inherited from Environment",
                                modifier = Modifier.padding(top = 12.dp)
                            )

                            KeyValueEditorView(
                                keyValues = environmentVariables.map {
                                    UserKeyValuePair(
                                        uuidString(),
                                        it.key,
                                        it.value,
                                        FieldValueType.String,
                                        it.key !in exampleVariables
                                    )
                                },
                                keyPlaceholder = "",
                                valuePlaceholder = "",
                                isSupportFileValue = false,
                                isSupportVariables = false,
                                isSupportDisable = false,
                                knownVariables = emptyMap(),
                                disabledIds = emptySet(),
                                isInheritedView = true,
                                onItemChange = { _, _ -> },
                                onItemAddLast = { _ -> },
                                onItemDelete = { _ -> },
                                onDisableChange = { _ -> },
                                testTagPart1 = TestTagPart.ExampleVariable,
                                testTagPart2 = TestTagPart.InheritedFromEnvironment,
                            )
                        }
                    }
                }

                RequestTab.PreFlight ->
                    PreFlightEditorView(
                        selectedExample = selectedExample,
                        onRequestModified = onRequestModified,
                        request = request,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )

                RequestTab.PostFlight -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppText(
                        text = "Update environment variables according to response headers.",
                        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                    )
                    RequestKeyValueEditorView(
                        keyPlaceholder = "Variable",
                        valuePlaceholder = "Header",
                        value = selectedExample.postFlight.updateVariablesFromHeader,
                        onValueUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.copy(
                                            postFlight = selectedExample.postFlight.copy(
                                                updateVariablesFromHeader = it
                                            )
                                        )
                                    )
                                )
                            )
                        },
                        baseValue = if (selectedExample.id != baseExample.id) baseExample.postFlight.updateVariablesFromHeader else null,
                        baseDisabledIds = selectedExample.overrides?.disablePostFlightUpdateVarIds ?: emptySet(),
                        onDisableUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.run {
                                            copy(overrides = overrides!!.copy(disablePostFlightUpdateVarIds = it))
                                        }
                                    )
                                )
                            )
                        },
                        knownVariables = mergedVariables,
                        isSupportFileValue = false,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    )

                    AppText(
                        text = "Update environment variables according to response bodies.",
                        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp + 12.dp)
                    )
                    RequestKeyValueEditorView(
                        keyPlaceholder = "Variable",
                        valuePlaceholder = "Field's JSON Path",
                        value = selectedExample.postFlight.updateVariablesFromBody,
                        onValueUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.copy(
                                            postFlight = selectedExample.postFlight.copy(
                                                updateVariablesFromBody = it
                                            )
                                        )
                                    )
                                )
                            )
                        },
                        baseValue = if (selectedExample.id != baseExample.id) baseExample.postFlight.updateVariablesFromBody else null,
                        baseDisabledIds = selectedExample.overrides?.disablePostFlightUpdateVarIds ?: emptySet(),
                        onDisableUpdate = {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.run {
                                            copy(overrides = overrides!!.copy(disablePostFlightUpdateVarIds = it))
                                        }
                                    )
                                )
                            )
                        },
                        knownVariables = mergedVariables,
                        isSupportFileValue = false,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    )
                }
            }
        }

        if (hasPayloadEditor) {
            StreamingPayloadEditorView(
                modifier = Modifier.weight(0.7f).fillMaxWidth(),
                request = request,
                onRequestModified = onRequestModified,
                selectedPayloadExampleId = selectedPayloadExampleId!!,
                onSelectExample = { selectedPayloadExampleId = it.id },
                hasCompleteButton = request.application == ProtocolApplication.Grpc && currentGrpcMethod?.isClientStreaming == true,
                knownVariables = mergedVariables,
                onClickSendPayload = onClickSendPayload,
                onClickCompleteStream = onClickCompleteStream,
                connectionStatus = connectionStatus,
            )
        }
    }
}

@Composable
private fun PreFlightEditorView(
    modifier: Modifier = Modifier,
    selectedExample: UserRequestExample,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    request: UserRequestTemplate
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            AppText("Execute code before sending request", modifier = Modifier.weight(1f).padding(end = 8.dp))
            if (!request.isExampleBase(selectedExample)) {
                OverrideCheckboxWithLabel(
                    selectedExample = selectedExample,
                    onRequestModified = onRequestModified,
                    request = request,
                    translateToValue = { overrides ->
                        overrides.isOverridePreFlightScript
                    },
                    translateToNewOverrides = { isChecked, overrides ->
                        overrides.copy(isOverridePreFlightScript = isChecked)
                    },
                )
            }
        }
        val isEnabled = request.isExampleBase(selectedExample) || (selectedExample.overrides?.isOverridePreFlightScript == true)
        val example = if (!request.isExampleBase(selectedExample) && (selectedExample.overrides?.isOverridePreFlightScript == false)) {
            request.examples.first()
        } else {
            selectedExample
        }
        KotliteCodeEditorView(
            cacheKey = "Request:${request.id}/Example:${example.id}/Preflight/Script",
            text = example.preFlight.executeCode,
            onTextChange = {
                onRequestModified(
                    request.copy(
                        examples = request.examples.copyWithChange(
                            example.copy(
                                preFlight = example.preFlight.copy(
                                    executeCode = it
                                )
                            )
                        )
                    )
                )
            },
            isEnabled = isEnabled,
            isReadOnly = !isEnabled,
            testTag = TestTag.RequestPreFlightScriptTextField.name,
            modifier = Modifier.padding(top = 4.dp).fillMaxSize(),
        )
    }
}

@Composable
private fun RequestServiceMethodSelector(
    modifier: Modifier = Modifier,
    service: String,
    onSelectService: (String) -> Unit,
    method: String,
    onSelectMethod: (String) -> Unit,
    apiSpec: GrpcApiSpec?,
    onSelectApiSpec: (String) -> Unit,
    apiSpecList: List<GrpcApiSpec>,
    onClickFetchApiSpec: () -> Unit,
    onClickCancelFetchApiSpec: () -> Unit,
    isFetchingApiSpec: Boolean,
) {
    val serviceList = apiSpec?.methods
        ?.map {
            it.serviceFullName
        }
        ?.distinct()
        ?.sorted()
        ?: emptyList()

    fun getMethodList(service: String) =
        apiSpec?.methods
            ?.filter {
                it.serviceFullName == service
            }
            ?.sortedBy { it.methodName }
            ?: emptyList()

    val methodList = getMethodList(service)

    if (service.isEmpty() && serviceList.isNotEmpty()) {
        val firstService = serviceList.first()
        onSelectService(firstService)

        // don't update method at this moment, or changes in `onSelectMethod` would overwrite that in `onSelectService`
//        getMethodList(firstService)
//            .firstOrNull()
//            ?.let { onSelectMethod(it.methodName) }
    } else if (method.isEmpty() && methodList.isNotEmpty()) {
        onSelectMethod(methodList.first().methodName)
    }

    Row(modifier = modifier.height(IntrinsicSize.Max)) {
        DropDownView(
            items = apiSpecList.map { DropDownKeyValue(it.id, it.name) },
            selectedItem = DropDownKeyValue(apiSpec?.id ?: "", apiSpec?.name ?: ""),
            onClickItem = { onSelectApiSpec(it.key); true },
            isLabelFillMaxWidth = true,
            testTagParts = arrayOf(TestTagPart.RequestApiSpecDropdown),
            modifier = Modifier.weight(0.2f).padding(vertical = 4.dp).fillMaxHeight(),
        )
        DropDownView(
            items = serviceList.map { DropDownValue(it) },
            selectedItem = DropDownValue(service),
            onClickItem = { onSelectService(it.displayText); true },
            isLabelFillMaxWidth = true,
            contentView = { it, isLabel, isSelected, isClickable ->
                AppText(
                    text = if (isLabel) it?.displayText?.split('.')?.last().emptyToNull() ?: "--" else it!!.displayText,
                    color = if (!isLabel && isSelected) LocalColor.current.highlight else if (isClickable) LocalColor.current.primary else LocalColor.current.disabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                        .run {
                            if (isLabel) {
                                testTag(
                                    buildTestTag(
                                        TestTagPart.RequestGrpcServiceDropdown,
                                        TestTagPart.DropdownLabel
                                    )!!
                                )
                            } else {
                                this
                            }
                        }
                )
            },
            testTagParts = arrayOf(TestTagPart.RequestGrpcServiceDropdown),
            modifier = Modifier.weight(0.4f).padding(vertical = 4.dp).fillMaxHeight(),
        )
        DropDownView(
            items = methodList.map { DropDownValue(it.methodName) },
            selectedItem = DropDownValue(method),
            onClickItem = { onSelectMethod(it.displayText); true },
            isLabelFillMaxWidth = true,
            testTagParts = arrayOf(TestTagPart.RequestGrpcMethodDropdown),
            modifier = Modifier.weight(0.4f).padding(vertical = 4.dp).fillMaxHeight(),
        )

        if (!isFetchingApiSpec) {
            AppTooltipArea(tooltipText = "Fetch gRPC API Spec") {
                AppImageButton(
                    resource = "download-list.svg",
                    size = 24.dp,
                    onClick = onClickFetchApiSpec,
                    modifier = Modifier.padding(4.dp)
                        .testTag(TestTag.RequestFetchApiSpecButton.name),
                )
            }
        } else {
            AppTooltipArea(tooltipText = "Cancel fetching gRPC API Spec") {
                AppImageButton(
                    resource = "close.svg",
                    size = 24.dp,
                    onClick = onClickCancelFetchApiSpec,
                    modifier = Modifier.padding(4.dp)
                        .testTag(TestTag.RequestCancelFetchApiSpecButton.name),
                )
            }
        }
    }
}

@Composable
private fun RequestKeyValueEditorView(
    modifier: Modifier,
    value: List<UserKeyValuePair>?,
    baseValue: List<UserKeyValuePair>?,
    baseDisabledIds: Set<String>,
    knownVariables: Map<String, String>,
    onValueUpdate: (List<UserKeyValuePair>) -> Unit,
    onDisableUpdate: (Set<String>) -> Unit,
    isSupportFileValue: Boolean,
    keyPlaceholder: String = "Key",
    valuePlaceholder: String = "Value",
    testTagPart: TestTagPart? = null,
) {
    val data = value ?: listOf()
    val activeBaseValues = baseValue?.filter { it.isEnabled }
    Column(modifier = modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
        if (activeBaseValues?.isNotEmpty() == true) {
            InputFormHeader(text = "This Example")
        }

        KeyValueEditorView(
            keyValues = data,
            keyPlaceholder = keyPlaceholder,
            valuePlaceholder = valuePlaceholder,
            isSupportFileValue = isSupportFileValue,
            isSupportVariables = true,
            knownVariables = knownVariables,
            isInheritedView = false,
            disabledIds = emptySet(),
            onItemChange = { index, item ->
                log.d { "onItemChange" }
                onValueUpdate(data.copyWithIndexedChange(index, item))
            },
            onItemAddLast = { item ->
                log.d { "onItemAddLast" }
                onValueUpdate(data + item)
            },
            onItemDelete = { index ->
                log.d { "onItemDelete" }
                onValueUpdate(data.copyWithRemovedIndex(index))
            },
            onDisableChange = {_ ->},
            testTagPart1 = testTagPart,
            testTagPart2 = TestTagPart.Current,
        )

        if (activeBaseValues?.isNotEmpty() == true) {
            val isShowInheritedValues by remember { mutableStateOf(true) }
            InputFormHeader(text = "Inherited from Base", modifier = Modifier.padding(top = 12.dp))
            KeyValueEditorView(
                keyValues = activeBaseValues,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                isSupportFileValue = isSupportFileValue,
                isSupportVariables = true,
                knownVariables = knownVariables,
                disabledIds = baseDisabledIds,
                isInheritedView = true,
                onItemChange = {_, _ ->},
                onItemAddLast = {_ ->},
                onItemDelete = {_ ->},
                onDisableChange = onDisableUpdate,
                testTagPart1 = testTagPart,
                testTagPart2 = TestTagPart.Inherited,
            )
        }
    }
}

@Composable
private fun RequestBodyEditor(
    modifier: Modifier = Modifier,
    request: UserRequestTemplate,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    selectedExample: UserRequestExample,
    environmentVariables: Map<String, String>,
    currentGraphqlOperation: OperationDefinition?,
) {
    val colors = LocalColor.current
    val baseExample = request.examples.first()

    var selectedContentType by rememberLast(selectedExample.id, request.application) { mutableStateOf(selectedExample.contentType) }

    Column(modifier = modifier) {
        val requestBody = selectedExample.body
        Row(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.weight(1f)) {
                if (request.application != ProtocolApplication.Graphql) {
                    AppText("Content Type: ")
                    if (request.application != ProtocolApplication.Grpc) {
                        DropDownView(
                            items = listOf(
                                ContentType.Json,
                                ContentType.Multipart,
                                ContentType.FormUrlEncoded,
                                ContentType.Raw,
                                ContentType.BinaryFile,
                                ContentType.None
                            ).map { DropDownKeyValue(it, it.displayText) },
                            selectedItem = DropDownKeyValue(selectedContentType, selectedContentType.displayText),
                            onClickItem = { item ->
                                val it = item.key
                                if (selectedContentType == it) return@DropDownView true
                                selectedContentType = it
                                val newBody = when (it) {
                                    ContentType.None -> null
                                    ContentType.Json, ContentType.Raw -> StringBody("")
                                    ContentType.Multipart -> MultipartBody(emptyList())
                                    ContentType.FormUrlEncoded -> FormUrlEncodedBody(emptyList())
                                    ContentType.BinaryFile -> FileBody(null)
                                    else -> throw UnsupportedOperationException()
                                }
                                onRequestModified(
                                    request.copy(
                                        examples = request.examples.copyWithChange(
                                            selectedExample.copy(
                                                contentType = selectedContentType,
                                                body = newBody
                                            )
                                        )
                                    )
                                )
                                true
                            },
                            testTagParts = arrayOf(TestTagPart.RequestBodyTypeDropdown),
                        )
                    } else {
                        AppText(selectedContentType.displayText)
                    }
                } else {
                    AppText("Operation: ")
                    val body = selectedExample.body as? GraphqlBody
                    val isSelectedOperationValid = body?.getOperation(isThrowError = false) != null
                    AppTooltipArea(
                        isVisible = !isSelectedOperationValid,
                        tooltipText = "Invalid operation name or query document syntax.\nClick to correct the operation name.",
                        delayMillis = 0,
                    ) {
                        DropDownView(
                            modifier = if (isSelectedOperationValid) Modifier else Modifier.background(colors.errorResponseBackground),
                            items = listOf(),
                            populateItems = {
                                body?.getAllOperations(isThrowError = false)
                                    ?.map { DropDownValue(it.name ?: "") }
                                    ?.let {
                                        if (it.size <= 1) {
                                            listOf(DropDownValue("")) + it
                                        } else {
                                            it
                                        }
                                    }
                                    ?.distinct()
                                    ?: listOf(DropDownValue(""))
                            },
                            selectedItem = DropDownValue(body?.operationName ?: ""),
                            onClickItem = {
                                onRequestModified(
                                    request.copy(
                                        examples = request.examples.copyWithChange(
                                            selectedExample.copy(
                                                contentType = selectedContentType,
                                                body = body!!.copy(
                                                    operationName = it.displayText
                                                )
                                            )
                                        )
                                    )
                                )
                                true
                            },
                            testTagParts = arrayOf(TestTagPart.RequestGraphqlOperationName.name),
                        )
                    }
                }
            }

            if (selectedExample.id != baseExample.id && selectedContentType in setOf(
                    ContentType.Json,
                    ContentType.Raw,
                    ContentType.BinaryFile,
                    ContentType.Graphql,
                )
            ) {
                OverrideCheckboxWithLabel(
                    selectedExample = selectedExample,
                    onRequestModified = onRequestModified,
                    request = request,
                    translateToValue = { overrides ->
                        if (selectedContentType != ContentType.Graphql) {
                            overrides.isOverrideBody
                        } else {
                            overrides.isOverrideBodyContent
                        }
                    },
                    translateToNewOverrides = { isChecked, overrides ->
                        if (selectedContentType != ContentType.Graphql) {
                            overrides.copy(isOverrideBody = isChecked)
                        } else {
                            overrides.copy(isOverrideBodyContent = isChecked)
                        }
                    },
                )
            }
        }
        val remainModifier = Modifier.fillMaxWidth().weight(1f)
        when (selectedContentType) {
            ContentType.Json, ContentType.Raw -> {
                RequestBodyTextEditor(
                    cacheKey = "Request:${request.id}/Example:${selectedExample.id}/RequestBody/JsonOrRaw",
                    request = request,
                    contentType = selectedContentType,
                    onRequestModified = onRequestModified,
                    environmentVariables = environmentVariables,
                    selectedExample = selectedExample,
                    overridePredicate = { it?.isOverrideBody != false },
                    translateToText = { (it.body as? StringBody)?.value },
                    translateTextChangeToNewUserRequestExample = {
                        selectedExample.copy(
                            contentType = selectedContentType,
                            body = StringBody(it)
                        )
                    },
                    syntaxHighlight = if (selectedContentType == ContentType.Json) SyntaxHighlight.Json else SyntaxHighlight.None,
                    modifier = remainModifier,
                )
            }

            ContentType.FormUrlEncoded ->
                RequestKeyValueEditorView(
                    value = (requestBody as? FormUrlEncodedBody)?.value,
                    onValueUpdate = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.copy(
                                        contentType = selectedContentType,
                                        body = FormUrlEncodedBody(it)
                                    )
                                )
                            )
                        )
                    },
                    baseValue = if (selectedExample.id != baseExample.id) (baseExample.body as? FormUrlEncodedBody)?.value else null,
                    baseDisabledIds = selectedExample.overrides?.disabledBodyKeyValueIds ?: emptySet(),
                    onDisableUpdate = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.run {
                                        copy(overrides = overrides!!.copy(disabledBodyKeyValueIds = it))
                                    }
                                )
                            )
                        )
                    },
                    knownVariables = environmentVariables,
                    isSupportFileValue = false,
                    testTagPart = TestTagPart.RequestBodyFormUrlEncodedForm,
                    modifier = remainModifier,
                )

            ContentType.Multipart ->
                RequestKeyValueEditorView(
                    value = (requestBody as? MultipartBody)?.value,
                    onValueUpdate = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.copy(
                                        contentType = selectedContentType,
                                        body = MultipartBody(it)
                                    )
                                )
                            )
                        )
                    },
                    baseValue = if (selectedExample.id != baseExample.id) (baseExample.body as? MultipartBody)?.value else null,
                    baseDisabledIds = selectedExample.overrides?.disabledBodyKeyValueIds ?: emptySet(),
                    onDisableUpdate = {
                        onRequestModified(
                            request.copy(
                                examples = request.examples.copyWithChange(
                                    selectedExample.run {
                                        copy(overrides = overrides!!.copy(disabledBodyKeyValueIds = it))
                                    }
                                )
                            )
                        )
                    },
                    knownVariables = environmentVariables,
                    isSupportFileValue = true,
                    testTagPart = TestTagPart.RequestBodyMultipartForm,
                    modifier = remainModifier,
                )

            ContentType.BinaryFile -> {
                BinaryFileInputView(
                    modifier = remainModifier,
                    filePath = (selectedExample.body as? FileBody)?.filePath,
                    onFilePathUpdate = {
                        if (it != null) {
                            onRequestModified(
                                request.copy(
                                    examples = request.examples.copyWithChange(
                                        selectedExample.copy(
                                            contentType = selectedContentType,
                                            body = FileBody(it)
                                        )
                                    )
                                )
                            )
                        }
                    },
                )
            }

            ContentType.Graphql -> {
                RequestBodyTextEditor(
                    cacheKey = "Request:${request.id}/Example:${selectedExample.id}/GraphQLBody",
                    request = request,
                    contentType = selectedContentType,
                    onRequestModified = onRequestModified,
                    environmentVariables = environmentVariables,
                    selectedExample = selectedExample,
                    overridePredicate = { it?.isOverrideBodyContent != false },
                    translateToText = { (it.body as? GraphqlBody)?.document },
                    translateTextChangeToNewUserRequestExample = {
                        selectedExample.copy(
                            body = (selectedExample.body as GraphqlBody).copy(
                                document = it
                            )
                        )
                    },
                    syntaxHighlight = SyntaxHighlight.Graphql,
                    testTag = TestTag.RequestGraphqlDocumentTextField.name,
                    modifier = Modifier.fillMaxWidth().weight(0.62f),
                )

                Row(modifier = Modifier.padding(top = 10.dp, start = 8.dp, bottom = 6.dp)) {
                    AppText("Variables")
                    if (selectedExample.id != baseExample.id) {
                        Spacer(Modifier.weight(1f))
                        OverrideCheckboxWithLabel(
                            selectedExample = selectedExample,
                            onRequestModified = onRequestModified,
                            request = request,
                            translateToValue = { overrides -> overrides.isOverrideBodyVariables },
                            translateToNewOverrides = { isChecked, overrides ->
                                overrides.copy(isOverrideBodyVariables = isChecked)
                            },
                        )
                    }
                }
                RequestBodyTextEditor(
                    cacheKey = "Request:${request.id}/Example:${selectedExample.id}/GraphQLVariables",
                    request = request,
                    contentType = ContentType.Json,
                    onRequestModified = onRequestModified,
                    environmentVariables = environmentVariables,
                    selectedExample = selectedExample,
                    overridePredicate = { it?.isOverrideBodyVariables != false },
                    translateToText = { (it.body as? GraphqlBody)?.variables },
                    translateTextChangeToNewUserRequestExample = {
                        selectedExample.copy(
                            body = (selectedExample.body as GraphqlBody).copy(
                                variables = it
                            )
                        )
                    },
                    syntaxHighlight = SyntaxHighlight.Json,
                    testTag = TestTag.RequestGraphqlVariablesTextField.name,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp).weight(0.38f),
                )
            }

            ContentType.None -> {}
        }
    }
}

@Composable
private fun OverrideCheckboxWithLabel(
    modifier: Modifier = Modifier,
    request: UserRequestTemplate,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    selectedExample: UserRequestExample,
    translateToValue: (UserRequestExample.Overrides) -> Boolean,
    translateToNewOverrides: (Boolean, UserRequestExample.Overrides) -> UserRequestExample.Overrides,
) {
    Row(modifier) {
        AppText("Is Override Base? ")
        AppCheckbox(
            checked = translateToValue(selectedExample.overrides!!),
            onCheckedChange = {
                onRequestModified(
                    request.copy(
                        examples = request.examples.copyWithChange(
                            selectedExample.run {
                                copy(overrides = translateToNewOverrides(it, overrides!!))
                            }
                        )
                    )
                )
            },
            size = 16.dp,
        )
    }
}

@Composable
private fun RequestBodyTextEditor(
    modifier: Modifier,
    cacheKey: String,
    contentType: ContentType,
    request: UserRequestTemplate,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    environmentVariables: Map<String, String>,
    selectedExample: UserRequestExample,
    overridePredicate: (UserRequestExample.Overrides?) -> Boolean,
    translateToText: (UserRequestExample) -> String?,
    translateTextChangeToNewUserRequestExample: (String) -> UserRequestExample,
    syntaxHighlight: SyntaxHighlight,
    testTag: String? = null,
) {
    val colors = LocalColor.current
    val baseExample = request.examples.first()

    var textManipulator by remember(cacheKey) { mutableStateOf<BigTextManipulator?>(null) }
    val changeText = { it: String ->
        onRequestModified(
            request.copy(
                examples = request.examples.copyWithChange(
                    translateTextChangeToNewUserRequestExample(it)
                )
            )
        )
        Unit
    }

    val prettifyHandler = when (contentType) {
        ContentType.Json -> { code: String ->
            try {
                val prettified = JsonParser(code).prettify().prettyString
                changeText(prettified)
                textManipulator?.replace(0 until code.length, prettified)
            } catch (e: Throwable) {
                AppContext.ErrorMessagePromptViewModel.showErrorMessage(e.message ?: "Error while prettifying as JSON")
            }
        }

        else -> null
    }

    var textFieldPositionTop by remember { mutableStateOf(0f) }

    if (overridePredicate(selectedExample.overrides)) {
        val content = translateToText(selectedExample) ?: ""
        val focusRequester = remember { FocusRequester() }
        FloatingButtonContainer(
            buttonImage = "prettier.svg",
            tooltip = "Prettify",
            isEnabled = prettifyHandler != null,
            onClickButton = {
                prettifyHandler!!(content)
                focusRequester.requestFocus()
            },
            outerPadding = PaddingValues(
                top = 4.dp + with(LocalDensity.current) { textFieldPositionTop.toDp() },
                end = 8.dp,
            ),
            modifier = modifier,
        ) {
            CodeEditorView(
                cacheKey = cacheKey,
                isReadOnly = false,
                isEnableVariables = true,
                knownVariables = environmentVariables,
                initialText = content,
                onTextChange = changeText,
                onTextManipulatorReady = { textManipulator = it },
                onMeasured = { textFieldPositionTop = it },
                syntaxHighlight = syntaxHighlight,
                testTag = testTag ?: TestTag.RequestStringBodyTextField.name,
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    } else {
        CodeEditorView(
            modifier = modifier,
            cacheKey = cacheKey,
            isReadOnly = true,
            isEnableVariables = true,
            knownVariables = environmentVariables,
            initialText = translateToText(baseExample) ?: "",
            onTextChange = {},
            textColor = colors.placeholder,
            syntaxHighlight = SyntaxHighlight.None // intended to have no syntax highlighting
        )
    }
}

@Composable
fun InputFormHeader(modifier: Modifier = Modifier, text: String) {
    val colors = LocalColor.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Surface(color = colors.placeholder, modifier = Modifier.height(1.dp).padding(horizontal = 4.dp).offset(y = 1.dp).width(20.dp)) {}
        AppText(text = text)
        Surface(color = colors.placeholder, modifier = Modifier.height(1.dp).padding(horizontal = 4.dp).offset(y = 1.dp).weight(1f)) {}
    }
}

@Composable
fun BinaryFileInputView(modifier: Modifier = Modifier, filePath: String?, onFilePathUpdate: (String?) -> Unit) {
    val colours = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    val fileDialogState = rememberFileDialogState()

    if (isShowFileDialog) {
        FileDialog(state = fileDialogState) {
            if (it != null && it.isNotEmpty()) {
                onFilePathUpdate(it.first().absolutePath)
            }
            isShowFileDialog = false
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(200.dp)
            .background(colours.backgroundLight)
        ) {
            AppTextButton(
                text = filePath?.let { File(it).name } ?: "Choose a File",
                onClick = { isShowFileDialog = true },
                modifier = Modifier
                    .align(Alignment.Center)
                    .border(width = 1.dp, color = colours.placeholder)
                    .testTag(buildTestTag(TestTagPart.RequestBodyFileForm, TestTagPart.FileButton)!!),
            )
        }
    }
}

@Composable
fun StreamingPayloadEditorView(
    modifier: Modifier = Modifier,
    editExampleNameViewModel: EditNameViewModel = remember { EditNameViewModel() },
    request: UserRequestTemplate,
    onRequestModified: (UserRequestTemplate?) -> Unit,
    selectedPayloadExampleId: String,
    onSelectExample: (PayloadExample) -> Unit,
    hasCompleteButton: Boolean,
    knownVariables: Map<String, String>,
    onClickSendPayload: (String) -> Unit,
    onClickCompleteStream: () -> Unit,
    connectionStatus: ConnectionStatus,
) {
    val colors = LocalColor.current

    var selectedExample = request.payloadExamples!!.firstOrNull { it.id == selectedPayloadExampleId }

    if (selectedExample == null) {
        request.payloadExamples.first().let {
            onSelectExample(it)
            selectedExample = it
        }
    }

    val isEnableSend = connectionStatus == ConnectionStatus.OPEN_FOR_STREAMING

    fun triggerSendPayload() {
        if (isEnableSend) {
            onClickSendPayload(selectedExample!!.body)
        }
    }

    Column(modifier
        .onPreviewKeyEvent { e ->
            if (isEnableSend && e.type == KeyEventType.KeyDown && e.key == Key.Enter && !e.isAltPressed && !e.isShiftPressed) {
                val currentOS = currentOS()
                if ( (currentOS != MacOS && e.isCtrlPressed && !e.isMetaPressed) ||
                    (currentOS == MacOS && !e.isCtrlPressed && e.isMetaPressed) ) {
                    triggerSendPayload()
                    return@onPreviewKeyEvent true
                }
            }
            false
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Payload")
            Spacer(modifier = Modifier.weight(1f))
            AppTextButton(
                text = "Send",
                isEnabled = isEnableSend,
                modifier = Modifier.testTag(TestTag.RequestSendPayloadButton.name),
            ) {
                triggerSendPayload()
            }
            if (hasCompleteButton) {
                AppTextButton(
                    text = "Complete",
                    isEnabled = connectionStatus == ConnectionStatus.OPEN_FOR_STREAMING,
                    modifier = Modifier.padding(start = 4.dp)
                        .testTag(TestTag.RequestCompleteStreamButton.name),
                ) {
                    onClickCompleteStream()
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var isEditing = editExampleNameViewModel.isEditing.collectAsState().value

            AppText(text = "Examples: ")

            TabsView(
                modifier = Modifier.weight(1f).background(color = colors.backgroundLight),
                selectedIndex = request.payloadExamples!!.indexOfFirst { it.id == selectedExample!!.id },
                onSelectTab = {
                    onSelectExample(request.payloadExamples!![it])
                },
                onDoubleClickTab = {
                    onSelectExample(request.payloadExamples!![it])
                    editExampleNameViewModel.onStartEdit(request.payloadExamples!![it].id)
                },
                contents = (request.payloadExamples ?: emptyList()).mapIndexed { index, it ->
                    {
                        val isThisTabEditing = isEditing && selectedExample!!.id == it.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = if (!isThisTabEditing) Modifier.padding(8.dp) else Modifier
                        ) {
                            LabelOrTextField(
                                isEditing = isThisTabEditing,
                                value = it.name,
                                onValueUpdate = { new ->
                                    onRequestModified(request.copy(payloadExamples = request.payloadExamples!!.copyWithChange(it.copy(name = new))))
                                },
                                editNameViewModel = editExampleNameViewModel,
                            )
                            if (!isThisTabEditing && request.payloadExamples.size > 1) {
                                AppDeleteButton {
                                    onRequestModified(
                                        request.copy(payloadExamples = request.payloadExamples.copyWithRemovedIndex(index))
                                    )
                                }
                            }
                        }
                    }
                }
            )

            AppImageButton(
                resource = "add.svg",
                size = 24.dp,
                onClick = {
                    val newExample = PayloadExample(
                        id = uuidString(),
                        name = "New Payload",
                        body = "",
                    )
                    onRequestModified(
                        request.copy(payloadExamples = (request.payloadExamples ?: emptyList()) + newExample)
                    )
                    onSelectExample(newExample)
                    editExampleNameViewModel.onStartEdit(newExample.id)
                },
                modifier = Modifier.padding(4.dp)
                    .testTag(TestTag.RequestAddPayloadExampleButton.name)
            )
        }

        CodeEditorView(
            modifier = Modifier.weight(1f),
            cacheKey = "Request:${request.id}/PayloadExample:${selectedExample!!.id}/PayloadBody",
            isReadOnly = false,
            isEnableVariables = true,
            knownVariables = knownVariables,
            initialText = selectedExample!!.body,
            onTextChange = {
                onRequestModified(
                    request.copy(
                        payloadExamples = request.payloadExamples!!.copyWithChange(
                            selectedExample!!.copy(
                                body = it
                            )
                        )
                    )
                )
            },
            syntaxHighlight = SyntaxHighlight.Json,
            testTag = TestTag.RequestPayloadTextField.name,
        )
    }
}

private enum class RequestTab(val displayText: String) {
    Body("Body"), /* Authorization, */ Query("Query"), Header("Header"), PreFlight("Pre Flight"), PostFlight("Post Flight"), Variable("Variable")
}

private data class ProtocolMethod(val application: ProtocolApplication, val method: String)

private enum class SendButtonDropdown(val displayText: String) {
    CurlForLinux("Copy as cURL command (for Linux / macOS)"),
    GrpcurlForLinux("Copy as grpcurl command (for Linux / macOS)"),
    PowershellInvokeWebrequestForWindows("Copy as PowerShell v6+ Invoke-WebRequest command (for Windows pwsh.exe)")
}

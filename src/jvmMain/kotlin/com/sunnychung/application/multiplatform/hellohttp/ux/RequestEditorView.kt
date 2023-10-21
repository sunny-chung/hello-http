package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithIndexedChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel

@Composable
fun RequestEditorView(
    modifier: Modifier = Modifier,
    request: UserRequest,
    selectedExampleId: String,
    editExampleNameViewModel: EditNameViewModel,
    environment: Environment?,
    onSelectExample: (UserRequestExample) -> Unit,
    onClickSend: () -> Unit,
    onRequestModified: (UserRequest?) -> Unit,
) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    var selectedExample = request.examples.firstOrNull { it.id == selectedExampleId }
    val baseExample = request.examples.first()

    if (selectedExample == null) {
        selectedExample = baseExample
        onSelectExample(baseExample)
    } // do not use `selectedExampleId` directly, because selectedExample can be changed

    var previousRequest by remember { mutableStateOf("") }
    var selectedRequestTabIndex by remember { mutableStateOf(0) }

    var selectedContentType by remember { mutableStateOf(selectedExample.contentType) }

    if (previousRequest != request.id) { // any better way to renew cache?
        selectedContentType = selectedExample.contentType
        previousRequest = request.id
    }

    val environmentVariableKeys = environment?.variables?.filter { it.isEnabled }?.map { it.key }?.toSet() ?: emptySet()

    log.d { "RequestEditorView recompose $request" }

    @Composable
    fun RequestKeyValueEditorView(
        modifier: Modifier,
        value: List<UserKeyValuePair>?,
        baseValue: List<UserKeyValuePair>?,
        baseDisabledIds: Set<String>,
        knownVariables: Set<String>,
        onValueUpdate: (List<UserKeyValuePair>) -> Unit,
        onDisableUpdate: (Set<String>) -> Unit,
        isSupportFileValue: Boolean
    ) {
        val data = value ?: listOf()
        val activeBaseValues = baseValue?.filter { it.isEnabled }
        Column(modifier = modifier.padding(8.dp)) {
            if (activeBaseValues?.isNotEmpty() == true) {
                val isShowInheritedValues by remember { mutableStateOf(true) }
                InputFormHeader(text = "Inherited from Base")
                KeyValueEditorView(
                    keyValues = activeBaseValues,
                    isSupportFileValue = isSupportFileValue,
                    isSupportVariables = true,
                    knownVariables = knownVariables,
                    disabledIds = baseDisabledIds,
                    isInheritedView = true,
                    onItemChange = {_, _ ->},
                    onItemAddLast = {_ ->},
                    onItemDelete = {_ ->},
                    onDisableChange = onDisableUpdate,
                )

                InputFormHeader(text = "This Example", modifier = Modifier.padding(top = 12.dp))
            }

            KeyValueEditorView(
                keyValues = data,
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
//                modifier = modifier,
            )
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(color = colors.backgroundInputField)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropDownView(
                selectedItem = DropDownValue(request.method),
                items = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD").map { DropDownValue(it) },
                contentView = {
                    val (text, color) = when (request.protocol) {
                        Protocol.Http -> Pair(
                            it?.displayText, when (it?.displayText) {
                                "GET" -> colors.httpRequestGet
                                "POST" -> colors.httpRequestPost
                                "PUT" -> colors.httpRequestPut
                                "DELETE" -> colors.httpRequestDelete
                                else -> colors.httpRequestOthers
                            }
                        )

                        Protocol.Grpc -> Pair("gRPC", colors.grpcRequest)
                        Protocol.Graphql -> Pair("GQL", colors.graphqlRequest)
                    }
                    AppText(
                        text = text.emptyToNull() ?: "--",
                        color = color,
                        isFitContent = true,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(width = 48.dp)
                    )
                },
                onClickItem = {
                    val newMethod = it.displayText
                    onRequestModified(request.copy(method = newMethod))
                    true
                },
                modifier = Modifier.fillMaxHeight()
            )

            AppTextField(
                value = request.url,
                onValueChange = {
                    onRequestModified(request.copy(url = it))
                },
                visualTransformation = EnvironmentVariableTransformation(
                    themeColors = colors,
                    knownVariables = environmentVariableKeys
                ),
                singleLine = true,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(colors.backgroundButton).width(width = 90.dp).fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxHeight().weight(1f).clickable { onClickSend() }) {
                    AppText(
                        text = "Send",
                        fontSize = fonts.buttonFontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp).align(Alignment.Center)
                    )
                }
                AppImageButton(resource = "down-small.svg", size = 24.dp, onClick = { /* TODO */}, modifier = Modifier.fillMaxHeight().padding(end = 4.dp))
            }
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
                    selectedContentType = selectedExample.contentType
                },
                onDoubleClickTab = {
                    log.d { "req ex onDoubleClickTab $it" }
                    val selectedExample = request.examples[it]
                    onSelectExample(selectedExample)
                    if (it > 0) { // the "Base" example cannot be renamed
                        editExampleNameViewModel.onStartEdit()
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
                    val newExample = UserRequestExample(
                        id = uuidString(),
                        name = "New Example",
                        overrides = UserRequestExample.Overrides(),
                    )
                    onRequestModified(
                        request.copy(examples = request.examples + newExample)
                    )
                    onSelectExample(newExample)
                    editExampleNameViewModel.onStartEdit()
                },
                modifier = Modifier.padding(4.dp)
            )
        }

        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            selectedIndex = selectedRequestTabIndex,
            onSelectTab = { selectedRequestTabIndex = it },
            contents = RequestTab.values().map {
                { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
            }
        )
        when (RequestTab.values()[selectedRequestTabIndex]) {
            RequestTab.Body -> {
                val requestBody = selectedExample.body
                Row(modifier = Modifier.padding(8.dp)) {
                    AppText("Content Type: ")
                    DropDownView(
                        items = ContentType.values().toList(),
                        selectedItem = selectedContentType,
                        onClickItem = {
                            selectedContentType = it
                            if (it == ContentType.None) {
                                onRequestModified(
                                    request.copy(
                                        examples = request.examples.copyWithChange(
                                            selectedExample.copy(
                                                contentType = selectedContentType,
                                                body = null
                                            )
                                        )
                                    )
                                )
                            }
                            true
                        }
                    )

                    if (selectedExample.id != baseExample.id) {
                        Spacer(modifier.weight(1f))
                        AppText("Is Override Base? ")
                        AppCheckbox(
                            checked = selectedExample.overrides!!.isOverrideBody,
                            onCheckedChange = {
                                onRequestModified(
                                    request.copy(
                                        examples = request.examples.copyWithChange(
                                            selectedExample.run {
                                                copy(overrides = overrides!!.copy(isOverrideBody = it))
                                            }
                                        )
                                    )
                                )
                            },
                            size = 16.dp,
                        )
                    }
                }
                val remainModifier = Modifier.weight(1f).fillMaxWidth()
                when (selectedContentType) {
                    ContentType.Json, ContentType.Raw ->
                        if (selectedExample.overrides?.isOverrideBody != false) {
                            CodeEditorView(
                                modifier = remainModifier,
                                isReadOnly = false,
                                isEnableVariables = true,
                                knownVariables = environmentVariableKeys,
                                text = (selectedExample.body as? StringBody)?.value ?: "",
                                onTextChange = {
                                    onRequestModified(
                                        request.copy(
                                            examples = request.examples.copyWithChange(
                                                selectedExample.copy(
                                                    contentType = selectedContentType,
                                                    body = StringBody(it)
                                                )
                                            )
                                        )
                                    )
                                }
                            )
                        } else {
                            CodeEditorView(
                                modifier = remainModifier,
                                isReadOnly = true,
                                isEnableVariables = true,
                                knownVariables = environmentVariableKeys,
                                text = (baseExample.body as? StringBody)?.value ?: "",
                                onTextChange = {},
                                textColor = colors.placeholder,
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
                            knownVariables = environmentVariableKeys,
                            isSupportFileValue = false,
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
                            knownVariables = environmentVariableKeys,
                            isSupportFileValue = true,
                            modifier = remainModifier,
                        )

                    ContentType.None -> {}
                }
            }

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
                    knownVariables = environmentVariableKeys,
                    isSupportFileValue = false,
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
                    knownVariables = environmentVariableKeys,
                    isSupportFileValue = false,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
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

private enum class RequestTab {
    Body, /* Authorization, */ Query, Header
}

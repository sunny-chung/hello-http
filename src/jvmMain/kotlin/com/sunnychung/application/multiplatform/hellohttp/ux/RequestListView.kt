package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RequestListView(
    requests: List<UserRequest>,
    selectedRequest: UserRequest?,
    editRequestNameViewModel: EditNameViewModel,
    onSelectRequest: (UserRequest) -> Unit,
    onAddRequest: () -> UserRequest,
    onUpdateRequest: (UserRequest) -> Unit,
    onDeleteRequest: (UserRequest) -> Unit,
    onFocusRequestNameTextField: () -> Unit,
    onUnfocusRequestNameTextField: () -> Unit,
) {
    val colors = LocalColor.current

    var searchText by remember { mutableStateOf("") }

    var isEditing = editRequestNameViewModel.isEditing.collectAsState().value

    log.v { "RequestListView $requests" }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            AppTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = { AppImage(resource = "search.svg", size = 16.dp, modifier = Modifier.padding(horizontal = 6.dp)) },
                modifier = Modifier.weight(1f).border(width = 1.dp, color = colors.placeholder, shape = RoundedCornerShape(2.dp))
            )
            AppImageButton(
                resource = "add.svg",
                size = 24.dp,
                onClick = {
                    val newRequest = onAddRequest()
                    editRequestNameViewModel.onStartEdit()
                },
                modifier = Modifier.padding(4.dp)
            )
        }

        LazyColumn {
            items(items = requests) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.combinedClickable(
                        onClick = { onSelectRequest(it) },
                        onDoubleClick = {
                            onSelectRequest(it)
                            editRequestNameViewModel.onStartEdit()
                        }
                    )
                ) {
                    AppDeleteButton { onDeleteRequest(it) }

                    val (text, color) = when (it.protocol) {
                        Protocol.Http -> Pair(
                            it.method, when (it.method) {
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
                        text = text,
                        color = color,
                        isFitContent = true,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(width = 36.dp).padding(end = 4.dp)
                    )
                    if (!isEditing || selectedRequest?.id != it.id) {
                        AppText(
                            text = it.name,
                            maxLines = 1,
                            color = if (selectedRequest?.id == it.id) colors.highlight else colors.primary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val focusRequester = remember { FocusRequester() }
                        val focusManager = LocalFocusManager.current
                        var textFieldState by remember { mutableStateOf(TextFieldValue(it.name, selection = TextRange(0, it.name.length))) }
                        log.d { "RequestListView AppTextField recompose" }
                        AppTextField(
                            value = textFieldState,
                            onValueChange = { v ->
                                textFieldState = v
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { f ->
                                    log.d { "RequestListView onFocusChanged ${f.hasFocus} ${f.isFocused}" }
                                    if (f.hasFocus) {
                                        onFocusRequestNameTextField()
                                    } else {
                                        onUnfocusRequestNameTextField()
                                        if (editRequestNameViewModel.isInvokeModelUpdate()) {
                                            onUpdateRequest(it.copy(name = textFieldState.text))
                                        }
                                    }
                                    editRequestNameViewModel.onTextFieldFocusChange(f)
                                }
                                .onKeyEvent { e ->
                                    when (e.key) {
                                        Key.Enter -> {
                                            log.d { "key enter" }
                                            focusManager.clearFocus()
                                        }
                                        Key.Escape -> {
                                            log.d { "key escape" }
                                            editRequestNameViewModel.onUserCancelEdit()
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
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun RequestListViewPreview() {
    val dummyRequests = listOf(
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "GET", url = "", examples = emptyList()),
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "POST", url = "", examples = emptyList()),
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "PUT", url = "", examples = emptyList()),
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "DELETE", url = "", examples = emptyList()),
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Grpc, method = "", url = "", examples = emptyList()),
        UserRequest(id = uuidString(), name = "abc", protocol = Protocol.Graphql, method = "POST", url = "", examples = emptyList()),
    )
    RequestListView(
        requests = dummyRequests,
        selectedRequest = dummyRequests[2],
        editRequestNameViewModel = EditNameViewModel(),
        onSelectRequest = {},
        onAddRequest = { UserRequest(id = uuidString()) },
        onUpdateRequest = {},
        onDeleteRequest = {},
        onFocusRequestNameTextField = {},
        onUnfocusRequestNameTextField = {},
    )
}

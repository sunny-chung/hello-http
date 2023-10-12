package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.util.log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request

@Composable
fun RequestEditorView(modifier: Modifier = Modifier, request: UserRequest, onClickSend: (Request) -> Unit, onRequestModified: (UserRequest?) -> Unit) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    var selectedExample by remember { mutableStateOf(request.examples.first()) }
    var selectedExampleIndex by remember { mutableStateOf(0) }
    var selectedRequestTab by remember { mutableStateOf(RequestTab.values().first()) }

    log.d { "RequestEditorView recompose $request" }

    fun sendRequest() {
        // TODO merge with "Base" request

        var b = Request.Builder()
            .url(request.url.toHttpUrl()
                .newBuilder()
                .run {
                    var b = this
                    selectedExample.queryParameters.filter { it.isEnabled }.forEach { b = b.addQueryParameter(it.key, it.value) }
                    b
                }
                .build())
            .method(
                method = request.method,
                body = selectedExample.body?.toOkHttpBody(selectedExample.contentType.headerValue?.toMediaType()!!)
            )
        selectedExample.headers.filter { it.isEnabled }.forEach { b = b.addHeader(it.key, it.value) }
        val request = b.build()
        onClickSend(request)
    }

    @Composable
    fun RequestKeyValueEditorView(modifier: Modifier, initial: MutableList<UserKeyValuePair>?, setter: (MutableList<UserKeyValuePair>) -> Unit, isSupportFileValue: Boolean) {
        val keyValues = remember { mutableStateListOf<UserKeyValuePair>().apply {
            initial?.let { addAll(it) }
        } }
        val data = initial ?: mutableListOf()
        KeyValueEditorView(
            keyValues = keyValues,
            isSupportFileValue = isSupportFileValue,
            onItemChange = { index, item ->
                log.d { "onItemChange" }
                setter(data)
                data.set(index, item)
                keyValues[index] = item
                onRequestModified(null) // TODO use copied object instead of null
            },
            onItemAddLast = { item ->
                log.d { "onItemAddLast" }
                setter(data)
                data.add(item)
                keyValues += item
                onRequestModified(null) // TODO use copied object instead of null
            },
            onItemDelete = { index ->
                log.d { "onItemDelete" }
                setter(data)
                data.removeAt(index)
                keyValues.removeAt(index)
                onRequestModified(null) // TODO use copied object instead of null
            },
            modifier = modifier,
        )
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
//                    request.method = newMethod
//                    requestMethod = newMethod
                    onRequestModified(request.copy(method = newMethod))
                    true
                },
                modifier = Modifier.fillMaxHeight()
            )
//            }

            AppTextField(
                value = request.url,
                onValueChange = {
//                    request.url = it
//                    requestUrl = it
                    onRequestModified(request.copy(url = it))
                },
                singleLine = true,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(colors.backgroundButton).width(width = 90.dp).fillMaxHeight()
            ) {
                AppText(
                    text = "Send",
                    fontSize = fonts.buttonFontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { sendRequest() }
                )
                AppImageButton(resource = "down-small.svg", size = 24.dp, onClick = { /* TODO */}, modifier = Modifier.padding(end = 4.dp))
            }
        }
//        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Examples: ")

            TabsView(
                modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
                onSelectTab = { selectedExample = request.examples[it]; selectedExampleIndex = it },
                contents = request.examples.map {
                    { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
                }
            )
        }

        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            onSelectTab = { selectedRequestTab = RequestTab.values()[it] },
            contents = RequestTab.values().map {
                { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
            }
        )
        when (selectedRequestTab) {
            RequestTab.Body -> {
                var selectedContentType by remember { mutableStateOf(selectedExample.contentType) }
                var requestBody by remember { mutableStateOf(selectedExample.body) }
                Row(modifier = Modifier.padding(8.dp)) {
                    AppText("Content Type: ")
                    DropDownView(
                        items = ContentType.values().toList(),
                        selectedItem = selectedContentType,
                        onClickItem = {
                            selectedContentType = it
                            if (it == ContentType.None) {
                                selectedExample.contentType = selectedContentType
                            }
                            true
                        }
                    )
                }
                val remainModifier = Modifier.weight(1f).fillMaxWidth()
                when (selectedContentType) {
                    ContentType.Json, ContentType.Raw ->
                        CodeEditorView(
                            modifier = remainModifier,
                            isReadOnly = false,
                            text = (requestBody as? StringBody)?.value ?: "",
                            onTextChange = {
                                selectedExample.contentType = selectedContentType
                                selectedExample.body = StringBody(it)
                                requestBody = selectedExample.body
                                onRequestModified(null) // TODO use copied object instead of null
                            }
                        )

                    ContentType.FormUrlEncoded ->
                        RequestKeyValueEditorView(
                            initial = (requestBody as? FormUrlEncodedBody)?.value,
                            setter = {
                                request
                                selectedExample.contentType = selectedContentType
                                selectedExample.body = FormUrlEncodedBody(it)
                                requestBody = selectedExample.body
                            },
                            isSupportFileValue = false,
                            modifier = remainModifier,
                        )

                    ContentType.Multipart ->
                        RequestKeyValueEditorView(
                            initial = (requestBody as? MultipartBody)?.value,
                            setter = {
                                selectedExample.contentType = selectedContentType
                                selectedExample.body = MultipartBody(it)
                                requestBody = selectedExample.body
                            },
                            isSupportFileValue = true,
                            modifier = remainModifier,
                        )

                    ContentType.None -> {}
                }
            }

            RequestTab.Header ->
                RequestKeyValueEditorView(
                    initial = selectedExample.headers,
                    setter = { selectedExample.headers = it },
                    isSupportFileValue = false,
                    modifier = Modifier.fillMaxWidth(),
                )

            RequestTab.Query ->
                RequestKeyValueEditorView(
                    initial = selectedExample.queryParameters,
                    setter = { selectedExample.queryParameters = it },
                    isSupportFileValue = false,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

private enum class RequestTab {
    Body, /* Authorization, */ Query, Header
}

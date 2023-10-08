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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request

@Composable
fun RequestEditorView(modifier: Modifier = Modifier, request: UserRequest, onClickSend: (Request) -> Unit) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    var selectedExample by remember { mutableStateOf(request.examples.first()) }
    var selectedExampleIndex by remember { mutableStateOf(0) }
    var selectedRequestTab by remember { mutableStateOf(RequestTab.values().first()) }

    fun sendRequest() {
        // TODO merge with "Base" request

        var b = Request.Builder()
            .url(request.url.toHttpUrl()
                .newBuilder()
                .run {
                    var b = this
                    selectedExample.queryParameters.forEach { b = b.addQueryParameter(it.key, it.value) }
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

    fun onRequestModified() {
        /* TODO */
    }

    @Composable
    fun RequestKeyValueEditorView(modifier: Modifier, data: MutableList<UserKeyValuePair>?, isSupportFileValue: Boolean) {
        val keyValues = remember { mutableStateListOf<UserKeyValuePair>().apply {
            data?.let { addAll(it) }
        } }
        KeyValueEditorView(
            keyValues = keyValues,
            isSupportFileValue = isSupportFileValue,
            onItemChange = { index, item ->
                data?.set(index, item)
                keyValues[index] = item
                onRequestModified()
            },
            onItemAddLast = { item ->
                data?.add(item)
                keyValues += item
                onRequestModified()
            },
            onItemDelete = { index ->
                data?.removeAt(index)
                keyValues.removeAt(index)
                onRequestModified()
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
            var requestMethod by remember { mutableStateOf(request.method) }
            var requestUrl by remember { mutableStateOf(request.url) }

            DropDownView(
                selectedItem = DropDownValue(requestMethod),
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
                        text = text ?: "--",
                        color = color,
                        isFitContent = true,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(width = 48.dp)
                    )
                },
                onClickItem = {
                    val newMethod = it.displayText
                    request.method = newMethod
                    requestMethod = newMethod
                    onRequestModified()
                    true
                },
                modifier = Modifier.fillMaxHeight()
            )
//            }

            AppTextField(
                value = requestUrl,
                onValueChange = {
                    request.url = it
                    requestUrl = it
                    onRequestModified()
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
                var requestContentType by remember { mutableStateOf(selectedExample.contentType) }
                var requestBody by remember { mutableStateOf(selectedExample.body) }
                Row(modifier = Modifier.padding(8.dp)) {
                    AppText("Content Type: ")
                    DropDownView(
                        items = ContentType.values().toList(),
                        selectedItem = requestContentType,
                        onClickItem = {
                            selectedExample.contentType = it
                            requestContentType = it
                            true
                        }
                    )
                }
                val remainModifier = Modifier.weight(1f).fillMaxWidth()
                when (requestContentType) {
                    ContentType.Json, ContentType.Raw ->
                        CodeEditorView(
                            modifier = remainModifier,
                            isReadOnly = false,
                            text = (requestBody as? StringBody)?.value ?: "",
                            onTextChange = {
                                selectedExample.body = StringBody(it)
                                requestBody = selectedExample.body
                                onRequestModified()
                            }
                        )

                    ContentType.FormUrlEncoded ->
                        RequestKeyValueEditorView(
                            data = (requestBody as? FormUrlEncodedBody)?.value,
                            isSupportFileValue = false,
                            modifier = remainModifier,
                        )

                    ContentType.Multipart ->
                        RequestKeyValueEditorView(
                            data = (requestBody as? MultipartBody)?.value,
                            isSupportFileValue = true,
                            modifier = remainModifier,
                        )

                    ContentType.None -> {}
                }
            }

            RequestTab.Header ->
                RequestKeyValueEditorView(
                    data = selectedExample.headers,
                    isSupportFileValue = false,
                    modifier = Modifier.fillMaxWidth(),
                )

            RequestTab.Query ->
                RequestKeyValueEditorView(
                    data = selectedExample.queryParameters,
                    isSupportFileValue = false,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

private enum class RequestTab {
    Body, /* Authorization, */ Query, Header
}

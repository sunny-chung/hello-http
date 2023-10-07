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
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest

@Composable
fun RequestEditorView(modifier: Modifier = Modifier, request: UserRequest) {
    val colors = LocalColor.current
    val fonts = LocalFont.current

    var selectedExample by remember { mutableStateOf(request.examples.first()) }
    var selectedRequestTab by remember { mutableStateOf(RequestTab.values().first()) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(color = colors.backgroundInputField)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Row(modifier = Modifier.padding(horizontal = 8.dp).clickable { /* TODO */ }) {
//                AppImage(resource = "down-small.svg", size = 16.dp)
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
                        text = text ?: "--",
                        color = color,
                        isFitContent = true,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(width = 48.dp)
                    )
                },
                onClickItem = { /* TODO */ true },
                modifier = Modifier.fillMaxHeight()
            )
//            }
            AppTextField(
                value = request.url,
                onValueChange = { /* TODO */ },
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
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { /* TODO */ }
                )
                AppImageButton(resource = "down-small.svg", size = 24.dp, onClick = { /* TODO */}, modifier = Modifier.padding(end = 4.dp))
            }
        }
//        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Examples: ")

            TabsView(
                modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
                onSelectTab = { selectedExample = request.examples[it] },
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
                Row(modifier = Modifier.padding(8.dp)) {
                    AppText("Content Type: ")
                    DropDownView(items = ContentType.values().toList(), selectedItem = selectedExample.contentType, onClickItem = { /* FIXME */ selectedExample = selectedExample.copy(contentType = it); true })
                }
                val remainModifier = Modifier.weight(1f).fillMaxWidth()
                when (selectedExample.contentType) {
                    ContentType.Json, ContentType.Raw ->
                        CodeEditorView(
                            modifier = remainModifier,
                            isReadOnly = false,
                            text = (selectedExample.body as? StringBody)?.value ?: ""
                        )

                    ContentType.FormUrlEncoded ->
                        KeyValueEditorView(keyValues = (selectedExample.body as? FormUrlEncodedBody)?.value ?: emptyList(), modifier = remainModifier)

                    ContentType.Multipart ->
                        KeyValueEditorView(keyValues = (selectedExample.body as? MultipartBody)?.value ?: emptyList(), isSupportFileValue = true, modifier = remainModifier)

                    ContentType.None -> {}
                }
            }

            RequestTab.Header ->
                KeyValueEditorView(keyValues = selectedExample.headers, modifier = Modifier.fillMaxWidth())

            RequestTab.Query ->
                KeyValueEditorView(keyValues = selectedExample.queryParameters, modifier = Modifier.fillMaxWidth())
        }
    }
}

private enum class RequestTab {
    Body, /* Authorization, */ Query, Header
}

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RequestListView(requests: List<UserRequest>, onSelectRequest: (UserRequest) -> Unit) {
    val colors = LocalColor.current

    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            AppTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = { AppImage(resource = "search.svg", size = 16.dp, modifier = Modifier.padding(horizontal = 6.dp)) },
                modifier = Modifier.weight(1f).border(width = 1.dp, color = colors.placeholder, shape = RoundedCornerShape(2.dp))
            )
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { /* TODO */ }, modifier = Modifier.padding(4.dp))
        }

        LazyColumn {
            items(items = requests) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.combinedClickable(onClick = { onSelectRequest(it) })
                ) {
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
                    AppText(text = it.name, maxLines = 1)
                }
            }
        }
    }
}

@Composable
@Preview
fun RequestListViewPreview() {
    RequestListView(
        requests = listOf(
            UserRequest(name = "abc", protocol = Protocol.Http, method = "GET", url = "", examples = emptyList()),
            UserRequest(name = "abc", protocol = Protocol.Http, method = "POST", url = "", examples = emptyList()),
            UserRequest(name = "abc", protocol = Protocol.Http, method = "PUT", url = "", examples = emptyList()),
            UserRequest(name = "abc", protocol = Protocol.Http, method = "DELETE", url = "", examples = emptyList()),
            UserRequest(name = "abc", protocol = Protocol.Grpc, method = "", url = "", examples = emptyList()),
            UserRequest(name = "abc", protocol = Protocol.Graphql, method = "POST", url = "", examples = emptyList()),
        ),
        onSelectRequest = {}
    )
}

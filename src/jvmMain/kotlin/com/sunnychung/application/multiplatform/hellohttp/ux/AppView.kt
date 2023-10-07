package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse

@Composable
@Preview
fun AppView() {
    CompositionLocalProvider(LocalColor provides darkColorScheme()) {
        val colors = LocalColor.current
        CompositionLocalProvider(LocalScrollbarStyle provides defaultScrollbarStyle().copy(
            unhoverColor = colors.scrollBarUnhover,
            hoverColor = colors.scrollBarHover,
        )) {
            Box(modifier = Modifier.background(colors.background).fillMaxSize()) {
                AppContentView()
            }
        }
    }
}

@Composable
fun AppContentView() {
    val networkManager = AppContext.NetworkManager

    var selectedSubproject by remember { mutableStateOf<Subproject?>(null) }
    var activeCallId by remember { mutableStateOf<String?>(null) }
    var callDataUpdates = activeCallId?.let { networkManager.getCallData(it) }?.events?.collectAsState(null)?.value
    val response = activeCallId?.let { networkManager.getCallData(it) }?.response

    Row {
        Column(modifier = Modifier.width(150.dp)) {
//        ProjectAndEnvironmentView()
//            ProjectAndEnvironmentViewPreview()

            ProjectAndEnvironmentViewV2(
                projects = listOf(Project(id = "p1", name = "Project A", listOf(Subproject("a1", "Subproject A1", listOf()), Subproject("a2", "Subproject A2", listOf()))), Project(id = "p2", name = "Project B", listOf()), Project(id = "p3", name = "Project C", listOf())),
                environments = listOf(Environment(name = "Environment A"), Environment(name = "Environment B"), Environment(name = "Environment C")),
                onSelectEnvironment = {},
                onSelectSubproject = { selectedSubproject = it },
                modifier = if (selectedSubproject == null) Modifier.fillMaxHeight() else Modifier
            )

            if (selectedSubproject != null) {
                RequestListViewPreview()
            }
        }
        RequestEditorView(modifier = Modifier.width(300.dp), UserRequest("Example", Protocol.Http, "POST", "https://www.google.com/", examples = listOf(
            UserRequestExample(name = "Base", contentType = ContentType.Json, headers = listOf(UserKeyValuePair("a", "b", FieldValueType.String, true)), queryParameters = emptyList(), body = StringBody("{\n  \"abc\": \"d\"\n}\n")),
            UserRequestExample(name = "Example 1", contentType = ContentType.Multipart, headers = listOf(UserKeyValuePair("a1", "b1", FieldValueType.String, false)), queryParameters = emptyList(), body = MultipartBody(
                listOf(UserKeyValuePair("a2", "b2", FieldValueType.File, false), UserKeyValuePair("a3", "b3", FieldValueType.File, true))
            )),
        )),
            onClickSend = {
                activeCallId = networkManager.sendRequest(it).id
            }
            )
        ResponseViewerView(response = response ?: UserResponse())
    }
}

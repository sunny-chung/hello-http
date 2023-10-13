package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditRequestNameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
@Preview
fun AppView() {
    CompositionLocalProvider(LocalColor provides darkColorScheme()) {
        val colors = LocalColor.current
        CompositionLocalProvider(LocalScrollbarStyle provides defaultScrollbarStyle().copy(
            unhoverColor = colors.scrollBarUnhover,
            hoverColor = colors.scrollBarHover,
        )) {
            val dialogViewModel = AppContext.DialogViewModel
            val dialogUpdate = dialogViewModel.stateUpdateFlow.collectAsState(null).value // needed for updating UI by flow
            Box(modifier = Modifier.background(colors.background).fillMaxSize()) {
                AppContentView()

                dialogViewModel.state?.let { dialog ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.backgroundDialogOverlay)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                dialogViewModel.updateState(null)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .border(1.dp, color = colors.highlight)
                            .background(colors.background)
                            .padding(40.dp)
                            .align(
                                Alignment.Center
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // no-op
                            }
                    ) {
                        dialog.content()
                    }
                }
            }
        }
    }
}

val IS_DEV = true

@Composable
fun AppContentView() {
    val networkManager = AppContext.NetworkManager
    val persistenceManager = AppContext.PersistenceManager
    val requestCollectionRepository = AppContext.RequestCollectionRepository
    val projectCollectionRepository = AppContext.ProjectCollectionRepository
    val projectCollection = remember {
        runBlocking { projectCollectionRepository.read(ProjectAndEnvironmentsDI())!! }
    }

    var selectedSubproject by remember { mutableStateOf<Subproject?>(if (IS_DEV) Subproject(id = "dev", name = "DEV only") else null) }
    var requestCollection by remember { mutableStateOf<RequestCollection?>(null) }
    var requestsState by remember { mutableStateOf(requestCollection?.requests?.toList() ?: emptyList()) }
    var request by remember {
        mutableStateOf(
            runBlocking { // FIXME
                requestCollectionRepository.read(RequestsDI(subprojectId = selectedSubproject!!.id))?.requests?.firstOrNull()
                    ?: UserRequest(id = "-")
            }
        )
    }
    var activeCallId by remember { mutableStateOf<String?>(null) }
    var callDataUpdates = activeCallId?.let { networkManager.getCallData(it) }?.events?.collectAsState(null)?.value
    val activeResponse = activeCallId?.let { networkManager.getCallData(it) }?.response
    var response by remember { mutableStateOf<UserResponse?>(null) }
    if (activeResponse != null) {
        response = activeResponse
    }

    fun loadRequestsForSubproject(subproject: Subproject) {
        CoroutineScope(Dispatchers.IO).launch {
            val r = requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = subproject.id)) { id ->
                RequestCollection(id = id, requests = mutableListOf())
            }
            requestCollection = r
            requestsState = r.requests.toList()
        }
    }

    var isParentClearInputFocus by remember { mutableStateOf(false) }

    var modifier: Modifier = Modifier
    if (isParentClearInputFocus) {
        modifier = modifier.clearFocusOnTap()
    }

    val editRequestNameViewModel = remember { EditRequestNameViewModel() }

    Row(modifier = modifier) {
        Column(modifier = Modifier.width(150.dp)) {
            ProjectAndEnvironmentViewV2(
                projects = projectCollection.projects,
                environments = emptyList(),
                onAddProject = {
                    projectCollection.projects += it
                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                },
                onAddSubproject = { project, newSubproject ->
                    project.subprojects += newSubproject
                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                },
                onSelectEnvironment = {},
                onSelectSubproject = {
                    selectedSubproject = it
                    loadRequestsForSubproject(it)
                },
                modifier = if (selectedSubproject == null) Modifier.fillMaxHeight() else Modifier
            )

            if (selectedSubproject != null) {
                RequestListView(
                    requests = requestsState,
                    selectedRequest = request,
                    editRequestNameViewModel = editRequestNameViewModel,
                    onSelectRequest = { request = it },
                    onAddRequest = {
                        requestCollection!!.requests += it
                        requestsState = requestsState.toMutableList() + it
                        requestCollectionRepository.notifyUpdated(requestCollection!!.id)
                        request = it
                        isParentClearInputFocus = true
                    },
                    onUpdateRequest = { update ->
                        // TODO avoid the loop, refactor to use one state only and no duplicated code
                        requestsState = requestsState.toMutableList().mapIndexed { index, it ->
                            if (it.id == update.id) {
                                requestCollection!!.requests[index] = update
                                update.copy()
                            } else {
                                it
                            }
                        }
                        requestCollectionRepository.notifyUpdated(requestCollection!!.id)
                    },
                    onFocusRequestNameTextField = {
                        isParentClearInputFocus = true
                    },
                    onUnfocusRequestNameTextField = {
                        isParentClearInputFocus = false
                    }
                )
            }
        }
        RequestEditorView(
            modifier = Modifier.width(300.dp),
            request = request,
            onClickSend = { request, error ->
                if (request != null) {
                    activeCallId = networkManager.sendRequest(request).id
                } else {
                    activeCallId = null
                    response = UserResponse(isError = true, errorMessage = error?.message)
                }
            },
            onRequestModified = {
                log.d { "onRequestModified" }
                it?.let { update ->
                    request = update
                    // TODO avoid the loop, refactor to use one state only and no duplicated code
                    requestsState = requestsState.toMutableList().mapIndexed { index, it ->
                        if (it.id == update.id) {
                            requestCollection!!.requests[index] = update
                            update.copy()
                        } else {
                            it
                        }
                    }
                }
                requestCollectionRepository.notifyUpdated(RequestsDI(subprojectId = selectedSubproject!!.id))
            }
        )
        ResponseViewerView(response = response?.copy() ?: UserResponse())
    }
}

fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null) {
                focusManager.clearFocus()
            }
        }
    }
}

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
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

@Composable
fun AppContentView() {
    val networkManager = AppContext.NetworkManager
    val persistResponseManager = AppContext.PersistResponseManager
    val requestCollectionRepository = AppContext.RequestCollectionRepository
    val projectCollectionRepository = AppContext.ProjectCollectionRepository
    val responseCollectionRepository = AppContext.ResponseCollectionRepository
    val projectCollection = remember {
        runBlocking { projectCollectionRepository.read(ProjectAndEnvironmentsDI())!! }
    }

    var selectedSubproject by remember { mutableStateOf<Subproject?>(null) }
    var requestCollection by remember { mutableStateOf<RequestCollection?>(null) }
    var requestsState by remember { mutableStateOf(requestCollection?.requests?.toList() ?: emptyList()) }
    var request by remember { mutableStateOf<UserRequest?>(null) }
    var activeCallId by remember { mutableStateOf<String?>(null) }
    var callDataUpdates = activeCallId?.let { networkManager.getCallData(it) }?.events?.collectAsState(null)?.value // needed for invalidating compose caches
    val activeResponse = activeCallId?.let { networkManager.getCallData(it) }?.response
    var response by remember { mutableStateOf<UserResponse?>(null) }
    if (activeResponse != null && activeResponse.requestId == request?.id) {
        response = activeResponse
    }

    fun loadRequestsForSubproject(subproject: Subproject) {
        CoroutineScope(Dispatchers.IO).launch {
            val r = requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = subproject.id)) { id ->
                RequestCollection(id = id, requests = mutableListOf())
            }
            persistResponseManager.loadResponseCollection(ResponsesDI(subprojectId = subproject.id))
            requestCollection = r
            requestsState = r.requests.toList()
        }
    }

    fun displayRequest(req: UserRequest) {
        request = req
        response = runBlocking { // should be fast as it is retrieved from memory
            responseCollectionRepository.read(ResponsesDI(subprojectId = selectedSubproject!!.id))
                ?.responsesByRequestId?.get(req.id)
        } ?: UserResponse(id = "-", requestId = "-")
    }

    var isParentClearInputFocus by remember { mutableStateOf(false) }

    var modifier: Modifier = Modifier
    if (isParentClearInputFocus) {
        modifier = modifier.clearFocusOnTap()
    }

    val editRequestNameViewModel = remember { EditNameViewModel() }
    val editExampleNameViewModel = remember { EditNameViewModel() }

    fun createRequestForCurrentSubproject(): UserRequest {
        val newRequest = UserRequest(id = uuidString(), name = "New Request", method = "GET")
        requestCollection!!.requests += newRequest
        requestsState = requestsState.toMutableList() + newRequest
        requestCollectionRepository.notifyUpdated(requestCollection!!.id)
        request = newRequest
        isParentClearInputFocus = true
        return newRequest
    }

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
                    request = null
                    response = UserResponse("-", "-")
                },
                modifier = if (selectedSubproject == null) Modifier.fillMaxHeight() else Modifier
            )

            if (selectedSubproject != null) {
                RequestListView(
                    requests = requestsState,
                    selectedRequest = request,
                    editRequestNameViewModel = editRequestNameViewModel,
                    onSelectRequest = { displayRequest(it) },
                    onAddRequest = {
                        createRequestForCurrentSubproject()
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

                        if (request?.id == update.id) {
                            request = update.copy()
                        }
                    },
                    onDeleteRequest = { delete ->
                        // TODO avoid the loop, refactor to use one state only and no duplicated code
                        val index = requestsState.indexOfFirst { it.id == delete.id }
                        if (index >= 0) {
                            requestsState = requestsState.toMutableList().apply { removeAt(index) }
                            requestCollectionRepository.notifyUpdated(requestCollection!!.id)

                            if (request?.id == delete.id) {
                                request = null
                            }
                        }
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

        val requestEditorModifier = Modifier.width(300.dp)
        request?.let { requestNonNull ->
            RequestEditorView(
                modifier = requestEditorModifier,
                request = requestNonNull,
                editExampleNameViewModel = editExampleNameViewModel,
                onClickSend = { networkRequest, error ->
                    if (networkRequest != null) {
                        val callData = networkManager.sendRequest(
                            request = networkRequest,
                            requestId = requestNonNull.id,
                            subprojectId = selectedSubproject!!.id
                        )
                        activeCallId = callData.id
                        persistResponseManager.registerCall(callData.id)
                        callData.isPrepared = true
                    } else {
                        activeCallId = null
                        response = UserResponse(
                            id = uuidString(),
                            requestId = requestNonNull.id,
                            isError = true,
                            errorMessage = error?.message
                        )
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
        } ?: RequestEditorEmptyView(modifier = requestEditorModifier, isShowCreateRequest = selectedSubproject != null && requestCollection != null) {
            val newRequest = createRequestForCurrentSubproject()
            editRequestNameViewModel.onStartEdit()
        }
        ResponseViewerView(response = response?.copy() ?: UserResponse(id = "-", requestId = "-"))
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

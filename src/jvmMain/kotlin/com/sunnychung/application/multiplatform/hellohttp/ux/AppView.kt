package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.extension.toCurlCommand
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.MoveDirection
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.replaceIf
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.ux.local.lightColorScheme
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@Composable
@Preview
fun AppView() {
    val colourTheme by AppContext.UserPreferenceViewModel.colourTheme.collectAsState()
    val colourScheme = when (colourTheme) {
        ColourTheme.Dark -> darkColorScheme()
        ColourTheme.Light -> lightColorScheme()
    }
    CompositionLocalProvider(LocalColor provides colourScheme) {
        val colors = LocalColor.current
        CompositionLocalProvider(LocalScrollbarStyle provides defaultScrollbarStyle().copy(
            unhoverColor = colors.scrollBarUnhover,
            hoverColor = colors.scrollBarHover,
        )) {
            val dialogViewModel = AppContext.DialogViewModel
            val dialogState = dialogViewModel.state.collectAsState().value // needed for updating UI by flow
            log.d { "Dialog State = $dialogState" }
            Box(modifier = Modifier.background(colors.background).fillMaxSize()) {
                AppContentView()

                dialogState?.let { dialog ->
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
                        AppImageButton(
                            resource = "close.svg",
                            size = 24.dp,
                            onClick = { dialogViewModel.updateState(null) },
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = -28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun AppContentView() {
    val networkClientManager = AppContext.NetworkClientManager
    val persistResponseManager = AppContext.PersistResponseManager
    val requestCollectionRepository = AppContext.RequestCollectionRepository
    val projectCollectionRepository = AppContext.ProjectCollectionRepository
    val responseCollectionRepository = AppContext.ResponseCollectionRepository
    val projectCollection = remember {
        runBlocking { projectCollectionRepository.read(ProjectAndEnvironmentsDI())!! }
    }
    val clipboardManager = LocalClipboardManager.current

    val coroutineScope = rememberCoroutineScope()
    var selectedSubproject by remember { mutableStateOf<Subproject?>(null) }
    var selectedSubprojectState by remember { mutableStateOf<Subproject?>(null) }
    var selectedEnvironment by remember { mutableStateOf<Environment?>(null) }
    var requestCollection by remember { mutableStateOf<RequestCollection?>(null) }
    var requestCollectionState by remember { mutableStateOf<RequestCollection?>(null) }
    var request by remember { mutableStateOf<UserRequestTemplate?>(null) }
    var selectedRequestExampleId by remember { mutableStateOf<String?>(null) }

    // purpose of this variable is to force refresh UI once when there is new request
    // so that `callDataUpdates` resolves to a new and correct flow.
    // the value itself is not useful
    val activeCallId = networkClientManager.subscribeToNewRequests().value // `val xxx by yyy` VS `val xxx = yyy.value`: `.value` is called only if `xxx` is accessed

    val callDataUpdates by networkClientManager.subscribeToRequestExampleCall(selectedRequestExampleId)
        ?: run {
            log.d { "callDataUpdates no flow" }
            mutableStateOf(null)
        }
    val activeResponse = networkClientManager.getResponseByRequestExampleId(selectedRequestExampleId)
    var response by remember { mutableStateOf<UserResponse?>(null) }
    if (activeResponse != null && activeResponse.requestId == request?.id && activeResponse.requestExampleId == selectedRequestExampleId) {
        response = activeResponse
    }

    log.d { "callDataUpdates $activeCallId ${callDataUpdates?.event}; status = ${response?.isCommunicating}" }

    fun loadRequestsForSubproject(subproject: Subproject) {
        CoroutineScope(Dispatchers.Main).launch {
            val r = requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = subproject.id)) { id ->
                RequestCollection(id = id, requests = mutableListOf())
            }
            persistResponseManager.loadResponseCollection(ResponsesDI(subprojectId = subproject.id))
            requestCollection = r
            requestCollectionState = requestCollection?.copy()
        }
    }

    fun updateResponseView() {
        response = runBlocking { // should be fast as it is retrieved from memory
            responseCollectionRepository.read(ResponsesDI(subprojectId = selectedSubproject!!.id))
                ?.responsesByRequestExampleId?.get(selectedRequestExampleId)
        } ?: UserResponse(id = "-", requestExampleId = "-", requestId = "-")
    }

    fun displayRequest(req: UserRequestTemplate) {
        request = req
        selectedRequestExampleId = req.examples.first().id
        updateResponseView()
    }

    var isParentClearInputFocus by remember { mutableStateOf(false) }

    var modifier: Modifier = Modifier
    if (isParentClearInputFocus) {
        modifier = modifier.clearFocusOnTap()
    }

    val editRequestNameViewModel = remember { EditNameViewModel() }
    val editExampleNameViewModel = remember { EditNameViewModel() }

    fun createRequestForCurrentSubproject(parentId: String?): UserRequestTemplate {
        val newRequest = UserRequestTemplate(id = uuidString(), name = "New Request", method = "GET")
        requestCollection!!.requests += newRequest
        requestCollectionState = requestCollection?.copy()
        requestCollectionRepository.notifyUpdated(requestCollection!!.id)

        val newTreeRequest = TreeRequest(id = newRequest.id)
        if (parentId == null) {
            selectedSubproject!!.treeObjects += newTreeRequest
        } else {
            (selectedSubproject!!.findParentAndItem(parentId).second as TreeFolder).childs += newTreeRequest
        }
        selectedSubprojectState = selectedSubproject?.deepCopy()
        projectCollectionRepository.notifyUpdated(projectCollection.id)

        displayRequest(newRequest)
        isParentClearInputFocus = true
        return newRequest
    }

    // TODO refactor to a better location
    fun deleteSubprojectRelatedData(subproject: Subproject) {
        coroutineScope.launch {
            requestCollectionRepository.delete(RequestsDI(subprojectId = subproject.id))
            responseCollectionRepository.delete(ResponsesDI(subprojectId = subproject.id))
        }
    }

    log.d { "AppContentView recompose" }

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f)) {
            HorizontalSplitPane(splitPaneState = rememberSplitPaneState(initialPositionPercentage = 180f/800f)) {
                first(minSize = 150.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val projectUpdates = projectCollectionRepository.subscribeUpdates().collectAsState(null).value
                        log.d { "projectUpdates $projectUpdates" }
                        ProjectAndEnvironmentViewV2(
                            projects = projectCollection.projects.toList(),
                            selectedSubproject = selectedSubprojectState,
                            selectedEnvironment = selectedEnvironment,
//                environments = emptyList(),
                            onAddProject = {
                                projectCollection.projects += it
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onAddSubproject = { project, newSubproject ->
                                project.subprojects += newSubproject
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onSelectEnvironment = { selectedEnvironment = it },
                            onSelectSubproject = {
                                selectedSubproject = it
                                selectedSubprojectState = it
                                it?.let { loadRequestsForSubproject(it) }
                                request = null
                                selectedRequestExampleId = null
                                response = UserResponse("-", "-", "-")
                            },
                            onUpdateSubproject = {
                                assert(it.id == selectedSubproject!!.id)
                                with(selectedSubproject!!) {
                                    environments = it.environments
                                    name = it.name
//                        log.d { "Updated subproject ${environments}" }
                                }
                                selectedSubprojectState = selectedSubproject!!.deepCopy()
                                projectCollectionRepository.notifyUpdated(projectCollection.id)

                                selectedEnvironment = it.environments.firstOrNull { it.id == selectedEnvironment?.id }
                            },
                            onUpdateProject = { project ->
                                projectCollection.projects.replaceIf(project) { it.id == project.id }
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onDeleteProject = { project ->
                                project.subprojects.forEach {
                                    deleteSubprojectRelatedData(it)
                                }

                                projectCollection.projects.removeIf { it.id == project.id }
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onDeleteSubproject = { subproject ->
                                deleteSubprojectRelatedData(subproject)

                                // TODO: lower time complexity from O(n^2) to O(n)
                                val project = projectCollection.projects.first { it.subprojects.any { it.id == subproject.id } }
                                project.subprojects.removeIf { it.id == subproject.id }
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            modifier = if (selectedSubproject == null) Modifier.fillMaxHeight() else Modifier
                        )

                        if (selectedSubproject != null && requestCollectionState?.id?.subprojectId == selectedSubproject!!.id) {
                            RequestTreeView(
                                selectedSubproject = selectedSubprojectState!!,
//                    treeObjects = selectedSubprojectState?.treeObjects ?: emptyList(),
                                requests = requestCollectionState?.requests?.associateBy { it.id } ?: emptyMap(),
                                selectedRequest = request,
                                editTreeObjectNameViewModel = editRequestNameViewModel,
                                onSelectRequest = { displayRequest(it) },
                                onAddRequest = {
                                    createRequestForCurrentSubproject(parentId = it)
                                },
                                onUpdateRequest = { update ->
                                    // TODO avoid the loop, refactor to use one state only and no duplicated code
                                    requestCollection!!.requests.replaceIf(update) { it.id == update.id }
                                    requestCollectionState = requestCollection?.copy()
                                    requestCollectionRepository.notifyUpdated(requestCollection!!.id)

                                    if (request?.id == update.id) {
                                        request = update.copy()
                                    }
                                },
                                onDeleteRequest = { delete ->
                                    // TODO avoid the loop, refactor to use one state only and no duplicated code
                                    val hasRemoved = requestCollection!!.requests.removeIf { it.id == delete.id }
                                    if (hasRemoved) {
                                        requestCollectionState = requestCollection?.copy()
                                        requestCollectionRepository.notifyUpdated(requestCollection!!.id)

                                        selectedSubproject?.removeTreeObjectIf { it.id == delete.id }
                                        selectedSubprojectState = selectedSubproject?.deepCopy()
                                        projectCollectionRepository.notifyUpdated(projectCollection.id)

                                        if (request?.id == delete.id) {
                                            request = null
                                            selectedRequestExampleId = null
                                        }
                                    }
                                },
                                onFocusNameTextField = {
                                    isParentClearInputFocus = true
                                },
                                onUnfocusNameTextField = {
                                    isParentClearInputFocus = false
                                },
                                onAddFolder = { parentId ->
                                    val new = TreeFolder(id = uuidString(), name = "New Folder", childs = mutableListOf())
                                    if (parentId == null) {
                                        selectedSubproject!!.treeObjects += new
                                    } else {
                                        (selectedSubproject!!.findParentAndItem(parentId).second as TreeFolder).childs += new
                                    }
                                    selectedSubprojectState = selectedSubproject!!.deepCopy()
                                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                                    new
                                },
                                onUpdateFolder = { newFolder ->
                                    val parent = selectedSubproject!!.findParentAndItem(newFolder.id).first as? TreeFolder
                                    if (parent == null) {
                                        selectedSubproject!!.treeObjects.replaceIf(newFolder) { it.id == newFolder.id }
                                    } else {
                                        parent.childs.replaceIf(newFolder) { it.id == newFolder.id }
                                    }
                                    selectedSubprojectState = selectedSubproject!!.deepCopy()
                                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                                },
                                onDeleteFolder = { folder ->
                                    selectedSubproject!!.removeTreeObjectIf { it.id == folder.id }
                                    selectedSubprojectState = selectedSubproject!!.deepCopy()
                                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                                },
                                onMoveTreeObject = { treeObjectId, direction, destination ->
                                    if (direction == MoveDirection.Inside) {
                                        selectedSubproject!!.moveInto(treeObjectId, destination as TreeFolder?)
                                    } else {
                                        selectedSubproject!!.moveNear(treeObjectId, direction, destination!!.id)
                                    }
                                    selectedSubprojectState = selectedSubproject!!.deepCopy()
                                    projectCollectionRepository.notifyUpdated(projectCollection.id)
                                },
                            )
                        }
                    }
                }

                second(minSize = 400.dp) {
                    HorizontalSplitPane(splitPaneState = rememberSplitPaneState(initialPositionPercentage = 0.5f)) {
                        first(minSize = 200.dp) {
                            val requestEditorModifier = Modifier.fillMaxWidth()
                            request?.let { requestNonNull ->
                                RequestEditorView(
                                    modifier = requestEditorModifier,
                                    request = requestNonNull,
                                    selectedExampleId = selectedRequestExampleId!!,
                                    editExampleNameViewModel = editExampleNameViewModel,
                                    environment = selectedEnvironment,
                                    onSelectExample = {
                                        selectedRequestExampleId = it.id
                                        updateResponseView()
                                    },
                                    onClickSend = {
                                        networkClientManager.fireRequest(
                                            request = requestNonNull,
                                            requestExampleId = selectedRequestExampleId!!,
                                            environment = selectedEnvironment,
                                            subprojectId = selectedSubproject!!.id
                                        )
                                    },
                                    onClickCancel = {
                                        networkClientManager.getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.cancel() }
                                    },
                                    onClickCopyCurl = {
                                        try {
                                            val curl = requestNonNull.toCurlCommand(
                                                exampleId = selectedRequestExampleId!!,
                                                environment = selectedEnvironment
                                            )
                                            log.d { "curl: $curl" }
                                            clipboardManager.setText(AnnotatedString(curl))
                                            true
                                        } catch (e: Throwable) {
                                            log.w(e) { "Cannot convert request" }
                                            false
                                        }
                                    },
                                    onRequestModified = {
                                        log.d { "onRequestModified" }
                                        it?.let { update ->
                                            request = update
                                            // TODO avoid the loop, refactor to use one state only and no duplicated code
                                            requestCollection!!.requests.replaceIf(update) { it.id == update.id }
                                            requestCollectionRepository.notifyUpdated(RequestsDI(subprojectId = selectedSubproject!!.id))
                                        }
                                    },
                                    isConnecting = networkClientManager.getResponseByRequestExampleId(selectedRequestExampleId)?.isCommunicating ?: false,
                                    onClickConnect = {
                                        networkClientManager.fireRequest(
                                            request = requestNonNull,
                                            requestExampleId = selectedRequestExampleId!!,
                                            environment = selectedEnvironment,
                                            subprojectId = selectedSubproject!!.id
                                        )
                                    },
                                    onClickDisconnect = {
                                        networkClientManager.getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.cancel() }
                                    },
                                    onClickSendPayload = { payload ->
                                        networkClientManager.getCallDataByRequestExampleId(selectedRequestExampleId)?.let { it.sendPayload(payload) }
                                    }
                                )
                            } ?: RequestEditorEmptyView(
                                modifier = requestEditorModifier,
                                isShowCreateRequest = selectedSubproject != null && requestCollection != null
                            ) {
                                val newRequest = createRequestForCurrentSubproject(parentId = null)
                                editRequestNameViewModel.onStartEdit()
                            }
                        }
                        second(minSize = 200.dp) {
                            ResponseViewerView(
                                response = response?.copy() ?: UserResponse(
                                    id = "-",
                                    requestId = "-",
                                    requestExampleId = "-"
                                )
                            )
                        }
                    }
                }
            }
        }
        StatusBarView()
    }
}

fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
//    Modifier.pointerInput(Unit) {
//        awaitEachGesture {
//            awaitFirstDown(pass = PointerEventPass.Initial)
//            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
//            if (upEvent != null) {
//                log.d { "clearFocusOnTap clearFocus" }
//                focusManager.clearFocus()
//            }
//        }
//    }
    Modifier
}

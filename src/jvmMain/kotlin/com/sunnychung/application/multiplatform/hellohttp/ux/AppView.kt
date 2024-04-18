package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.extension.CommandGenerator
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.MoveDirection
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.platform.LinuxOS
import com.sunnychung.application.multiplatform.hellohttp.platform.WindowsOS
import com.sunnychung.application.multiplatform.hellohttp.util.let
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.replaceIf
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.ux.local.lightColorScheme
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.ErrorMessagePromptViewModel
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@OptIn(ExperimentalComposeUiApi::class)
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
                    val focusRequester = remember { FocusRequester() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.backgroundDialogOverlay)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                dialogViewModel.updateState(null) // close the dialog
                            }
                    )
                    Box(
                        modifier = Modifier
                            .padding(60.dp)
                            .border(1.dp, color = colors.highlight)
                            .background(colors.background)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // no-op
                            }
                            .onKeyEvent {
                                log.d { "Dialog key ${it.type} ${it.key}" }
                                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                                    dialogViewModel.updateState(null) // close the dialog
                                    true
                                } else {
                                    false
                                }
                            }
                            .focusRequester(focusRequester)
                            .padding(40.dp)
                            .align(
                                Alignment.Center
                            )
                    ) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        dialog.content()
                        AppImageButton(
                            resource = "close.svg",
                            size = 24.dp,
                            onClick = { dialogViewModel.updateState(null) },
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = -28.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(all = 40.dp)
                        .width(IntrinsicSize.Max)
                        .height(IntrinsicSize.Max)
                ) {
                    val errorMessageVM = AppContext.ErrorMessagePromptViewModel
                    val errorMessageState = errorMessageVM.state.collectAsState().value
                    val visible = errorMessageVM.isVisible()
                    AnimatedVisibility(
                        visible,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(1000))
                    ) {
                        AppText(
                            text = errorMessageState.message,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(
                                    when (errorMessageState.type) {
                                        ErrorMessagePromptViewModel.MessageType.Error -> colors.errorResponseBackground
                                        ErrorMessagePromptViewModel.MessageType.Success -> colors.successfulResponseBackground
                                    }
                                )
                                .widthIn(min = 200.dp, max = 600.dp)
                                .onPointerEvent(PointerEventType.Enter) {
                                    errorMessageVM.lockDismissTime()
                                }
                                .onPointerEvent(PointerEventType.Exit) {
                                    errorMessageVM.unlockDismissTime(1.seconds())
                                }
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { errorMessageVM.dismiss() }
                                .padding(12.dp),
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
    val apiSpecificationCollectionRepository = AppContext.ApiSpecificationCollectionRepository
    val projectCollection = remember {
        runBlocking { projectCollectionRepository.read(ProjectAndEnvironmentsDI())!! }
    }
    val clipboardManager = LocalClipboardManager.current
    val errorMessageVM = AppContext.ErrorMessagePromptViewModel

    val coroutineScope = rememberCoroutineScope()
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedSubprojectId by remember { mutableStateOf<String?>(null) }
    val selectedSubproject = selectedSubprojectId?.let { projectCollectionRepository.subscribeLatestSubproject(ProjectAndEnvironmentsDI(), it).collectAsState(null, Dispatchers.Main.immediate).value?.first }
    var selectedEnvironment by remember { mutableStateOf<Environment?>(null) }
    var requestCollectionDI by remember { mutableStateOf<RequestsDI?>(null) }
    val requestCollection = requestCollectionDI?.let { di -> requestCollectionRepository.subscribeLatestCollection(di).collectAsState(null).value?.first }
    var selectedRequestId by rememberLast(selectedSubprojectId) { mutableStateOf<String?>(null) }
    val request = let(requestCollection?.id, selectedRequestId) { di, selectedRequestId ->
        // collect immediately, or fast typing would lead to race conditions
        requestCollectionRepository.subscribeLatestRequest(di, selectedRequestId)
            .collectAsState(null, Dispatchers.Main.immediate).value
    }?.let {
        // Bug: https://issuetracker.google.com/issues/205590513
//        if (it.id == selectedRequestId) {
            it
//        } else {
//            null
//        }
    }
    var selectedRequestExampleId by rememberLast(request?.id) { mutableStateOf<String?>(request?.examples?.first()?.id) }
    log.d { "selectedSubprojectId=$selectedSubprojectId selectedRequestId=$selectedRequestId request=${request?.id} selectedRequestExampleId=$selectedRequestExampleId" }

    val projectApiSpecCollection = selectedProject?.let { apiSpecificationCollectionRepository.subscribeLatestCollection(
        ApiSpecDI(it.id)
    ).collectAsState(null).value?.first }
    val projectGrpcSpecs = projectApiSpecCollection?.grpcApiSpecs?.associateBy { it.id }

    // purpose of this variable is to force refresh UI once when there is new request
    // so that `callDataUpdates` resolves to a new and correct flow.
    // the value itself is not useful
    val activeCallId = networkClientManager.subscribeToNewRequests().value // `val xxx by yyy` VS `val xxx = yyy.value`: `.value` is called only if `xxx` is accessed

    val callDataUpdates by selectedRequestExampleId?.let { networkClientManager.subscribeToRequestExampleCall(it) }
        ?: run {
            log.d { "callDataUpdates no flow" }
            mutableStateOf(null)
        }
    val activeResponse = selectedRequestExampleId?.let { networkClientManager.getResponseByRequestExampleId(it) }
//    var response by remember { mutableStateOf<UserResponse?>(null) }
//    if (activeResponse != null && activeResponse.requestId == request?.id && activeResponse.requestExampleId == selectedRequestExampleId) {
//        response = activeResponse
//    }
    val response = runBlocking { // should be fast as it is retrieved from memory
        if (selectedSubproject == null || selectedRequestExampleId == null) return@runBlocking null
        val di = ResponsesDI(subprojectId = selectedSubproject!!.id)
        val resp = responseCollectionRepository.read(di)
            ?.responsesByRequestExampleId?.get(selectedRequestExampleId)
        log.d { "updateResponseView $selectedRequestExampleId" }
        resp
    } ?: UserResponse(id = "-", requestExampleId = "-", requestId = "-")

    var forceRecompose by remember { mutableStateOf("") }
    val needThisForForceRecomposeToWork = forceRecompose

    log.d { "callDataUpdates $activeCallId ${callDataUpdates?.event}; status = ${response?.isCommunicating}" }

    fun forceUpdateUI() {
        forceRecompose = uuidString()
        log.d { "forceUpdateUI $forceRecompose" }
    }

    fun loadRequestsForSubproject(subproject: Subproject) {
        CoroutineScope(Dispatchers.Main).launch {
            val r = requestCollectionRepository.readOrCreate(RequestsDI(subprojectId = subproject.id)) { id ->
                RequestCollection(id = id, requests = mutableListOf())
            }
            persistResponseManager.loadResponseCollection(ResponsesDI(subprojectId = subproject.id))
            requestCollectionDI = RequestsDI(subprojectId = subproject.id)
        }
    }

    fun updateResponseView() {
//        response = runBlocking { // should be fast as it is retrieved from memory
//            if (selectedSubproject == null || selectedRequestExampleId == null) return@runBlocking null
//            val di = ResponsesDI(subprojectId = selectedSubproject!!.id)
//            val resp = responseCollectionRepository.read(di)
//                ?.responsesByRequestExampleId?.get(selectedRequestExampleId)
//            log.d { "updateResponseView $selectedRequestExampleId" }
//            resp
//        } ?: UserResponse(id = "-", requestExampleId = "-", requestId = "-")
    }

    fun displayRequest(req: UserRequestTemplate) {
        selectedRequestId = req.id
//        selectedRequestExampleId = req.examples.first().id // this line is needed because `updateResponseView()` depends on its immediate result
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
        requestCollectionRepository.notifyUpdated(requestCollection!!.id)

        val newTreeRequest = TreeRequest(id = newRequest.id)
        if (parentId == null) {
            selectedSubproject!!.treeObjects += newTreeRequest
        } else {
            (selectedSubproject!!.findParentAndItem(parentId).second as TreeFolder).childs += newTreeRequest
        }
        projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)

        displayRequest(newRequest)
        isParentClearInputFocus = true
        return newRequest
    }

    // TODO refactor to a better location
    fun deleteSubprojectRelatedData(subproject: Subproject) {
        CoroutineScope(Dispatchers.IO).launch {
            requestCollectionRepository.delete(RequestsDI(subprojectId = subproject.id))
            responseCollectionRepository.delete(ResponsesDI(subprojectId = subproject.id))
        }
    }

    // TODO refactor to a better location
    fun deleteProjectRelatedData(project: Project) {
        CoroutineScope(Dispatchers.IO).launch {
            project.subprojects.forEach {
                deleteSubprojectRelatedData(it)
            }
            apiSpecificationCollectionRepository.delete(ApiSpecDI(projectId = project.id))
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
                            selectedProject = selectedProject,
                            selectedSubproject = selectedSubproject,
                            selectedEnvironment = selectedEnvironment,
                            onAddProject = {
                                projectCollection.projects += it
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onAddSubproject = { project, newSubproject ->
                                project.subprojects += newSubproject
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onSelectEnvironment = { selectedEnvironment = it },
                            onSelectProject = { selectedProject = it },
                            onSelectSubproject = {
                                selectedSubprojectId = it?.id
                                it?.let { loadRequestsForSubproject(it) }
//                                response = UserResponse("-", "-", "-")
                            },
                            onUpdateSubproject = {
                                assert(it.id == selectedSubproject!!.id)
                                with(selectedSubproject!!) {
                                    environments = it.environments
                                    name = it.name
//                        log.d { "Updated subproject ${environments}" }
                                }
                                projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)

                                selectedEnvironment = it.environments.firstOrNull { it.id == selectedEnvironment?.id }
                            },
                            onUpdateProject = { project ->
                                projectCollection.projects.replaceIf(project) { it.id == project.id }
                                projectCollectionRepository.notifyUpdated(projectCollection.id)
                            },
                            onDeleteProject = { project ->
                                deleteProjectRelatedData(project)

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

                        if (selectedSubproject != null && requestCollection?.id?.subprojectId == selectedSubproject!!.id) {
                            RequestTreeView(
                                selectedSubproject = selectedSubproject!!,
//                    treeObjects = selectedSubprojectState?.treeObjects ?: emptyList(),
                                requests = requestCollection?.requests?.associateBy { it.id } ?: emptyMap(),
                                selectedRequest = request,
                                editTreeObjectNameViewModel = editRequestNameViewModel,
                                onSelectRequest = { displayRequest(it) },
                                onAddRequest = {
                                    createRequestForCurrentSubproject(parentId = it)
                                },
                                onUpdateRequest = { update ->
                                    requestCollectionRepository.updateRequest(requestCollection!!.id, update)
                                },
                                onDeleteRequest = { delete ->
                                    // TODO avoid the loop, refactor to use one state only and no duplicated code
                                    val hasRemoved = requestCollectionRepository.deleteRequest(requestCollection!!.id, delete.id)
                                    if (hasRemoved) {
                                        selectedSubproject?.removeTreeObjectIf { it.id == delete.id }
                                        projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)

                                        if (request?.id == delete.id) {
                                            selectedRequestId = null
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
                                    projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)
                                    new
                                },
                                onUpdateFolder = { newFolder ->
                                    val parent = selectedSubproject!!.findParentAndItem(newFolder.id).first as? TreeFolder
                                    if (parent == null) {
                                        selectedSubproject!!.treeObjects.replaceIf(newFolder) { it.id == newFolder.id }
                                    } else {
                                        parent.childs.replaceIf(newFolder) { it.id == newFolder.id }
                                    }
                                    projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)
                                },
                                onDeleteFolder = { folder ->
                                    selectedSubproject!!.removeTreeObjectIf { it.id == folder.id }
                                    projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)
                                },
                                onMoveTreeObject = { treeObjectId, direction, destination ->
                                    if (direction == MoveDirection.Inside) {
                                        selectedSubproject!!.moveInto(treeObjectId, destination as TreeFolder?)
                                    } else {
                                        selectedSubproject!!.moveNear(treeObjectId, direction, destination!!.id)
                                    }
                                    projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)
                                },
                                onCopyRequest = { treeObjectId, direction, destination ->
                                    val request = requestCollection?.requests?.firstOrNull { it.id == treeObjectId } ?: return@RequestTreeView
                                    val copiedRequest = request.deepCopyWithNewId()
                                    requestCollection!!.requests += copiedRequest
                                    requestCollectionRepository.notifyUpdated(requestCollection!!.id)

                                    selectedSubproject!!.addNear(TreeRequest(copiedRequest.id), direction, destination!!.id)
                                    projectCollectionRepository.updateSubproject(projectCollection.id, selectedSubproject!!)

                                    selectedRequestId = copiedRequest.id
                                    editRequestNameViewModel.onStartEdit(copiedRequest.id)
                                }
                            )
                        }
                    }
                }

                second(minSize = 400.dp) {
                    HorizontalSplitPane(splitPaneState = rememberSplitPaneState(initialPositionPercentage = 0.5f)) {
                        first(minSize = 200.dp) {
                            val requestEditorModifier = Modifier.fillMaxWidth()
                            request?.let { requestNonNull ->
                                fun onClickSendOrConnect() {
                                    networkClientManager.fireRequest(
                                        request = requestNonNull,
                                        requestExampleId = selectedRequestExampleId!!,
                                        environment = selectedEnvironment,
                                        projectId = selectedProject!!.id,
                                        subprojectId = selectedSubproject!!.id,
                                        fireType = UserResponse.Type.Regular,
                                    )
                                }

                                RequestEditorView(
                                    modifier = requestEditorModifier,
                                    request = requestNonNull,
                                    selectedExampleId = selectedRequestExampleId!!,
                                    editExampleNameViewModel = editExampleNameViewModel,
                                    environment = selectedEnvironment,
                                    grpcApiSpecs = selectedSubproject?.grpcApiSpecIds?.mapNotNull {
                                        projectGrpcSpecs?.get(it)
                                    } ?: emptyList(),
                                    onSelectExample = {
                                        selectedRequestExampleId = it.id
                                        updateResponseView()
                                    },
                                    onClickSend = {
                                        onClickSendOrConnect()
                                    },
                                    onRequestLoadTest = { loadTestInput ->
                                        networkClientManager.fireLoadTestRequests(
                                            request = requestNonNull,
                                            requestExampleId = selectedRequestExampleId!!,
                                            environment = selectedEnvironment,
                                            projectId = selectedProject!!.id,
                                            subprojectId = selectedSubproject!!.id,
                                            input = loadTestInput,
                                        )
                                    },
                                    onClickCancel = {
                                        networkClientManager.cancel(selectedRequestExampleId!!)
                                    },
                                    onClickCopyCurl = {
                                        try {
                                            val curl = with (CommandGenerator(LinuxOS)) {
                                                requestNonNull.toCurlCommand(
                                                    exampleId = selectedRequestExampleId!!,
                                                    environment = selectedEnvironment
                                                )
                                            }
                                            log.d { "curl: $curl" }
                                            clipboardManager.setText(AnnotatedString(curl))
                                            errorMessageVM.showSuccessMessage("Copied command")
                                            true
                                        } catch (e: Throwable) {
                                            log.w(e) { "Cannot convert request" }
                                            errorMessageVM.showErrorMessage(e.message ?: e.javaClass.name)
                                            false
                                        }
                                    },
                                    onClickCopyPowershellInvokeWebrequest = {
                                        try {
                                            val cmd = with (CommandGenerator(WindowsOS)) {
                                                requestNonNull.toPowerShellInvokeWebRequestCommand(
                                                    exampleId = selectedRequestExampleId!!,
                                                    environment = selectedEnvironment
                                                )
                                            }
                                            log.d { "cmd: $cmd" }
                                            clipboardManager.setText(AnnotatedString(cmd))
                                            errorMessageVM.showSuccessMessage("Copied command")
                                            true
                                        } catch (e: Throwable) {
                                            log.w(e) { "Cannot convert request" }
                                            errorMessageVM.showErrorMessage(e.message ?: e.javaClass.name)
                                            false
                                        }
                                    },
                                    onClickCopyGrpcurl = { payloadExampleId, grpcMethod ->
                                        try {
                                            val cmd = with (CommandGenerator(LinuxOS)) {
                                                requestNonNull.toGrpcurlCommand(
                                                    exampleId = selectedRequestExampleId!!,
                                                    environment = selectedEnvironment,
                                                    payloadExampleId = payloadExampleId,
                                                    method = grpcMethod,
                                                )
                                            }
                                            log.d { "grpcurl: $cmd" }
                                            clipboardManager.setText(AnnotatedString(cmd))
                                            errorMessageVM.showSuccessMessage("Copied command")
                                            true
                                        } catch (e: Throwable) {
                                            errorMessageVM.showErrorMessage(e.message ?: e.javaClass.name)
                                            throw e
                                        }
                                    },
                                    onRequestModified = {
                                        log.d { "onRequestModified" }
                                        it?.let { update ->
                                            requestCollectionRepository.updateRequest(requestCollection!!.id, update)
                                        }
                                    },
                                    connectionStatus = selectedRequestExampleId?.let { networkClientManager.getStatusByRequestExampleId(it) } ?: ConnectionStatus.DISCONNECTED ,
                                    onClickConnect = {
                                        onClickSendOrConnect()
                                    },
                                    onClickDisconnect = {
                                        networkClientManager.cancel(selectedRequestExampleId!!)
                                    },
                                    onClickSendPayload = { payload ->
                                        networkClientManager.sendPayload(
                                            selectedRequestExampleId = selectedRequestExampleId!!,
                                            payload = payload,
                                            environment = selectedEnvironment,
                                        )
                                    },
                                    onClickCompleteStream = {
                                        networkClientManager.sendEndOfStream(
                                            selectedRequestExampleId = selectedRequestExampleId!!,
                                        )
                                    },
                                    onClickFetchApiSpec = {
                                        if (requestNonNull.application == ProtocolApplication.Grpc) {
                                            try {
                                                networkClientManager.fetchGrpcApiSpec(
                                                    url = requestNonNull.url,
                                                    environment = selectedEnvironment,
                                                    projectId = selectedProject!!.id,
                                                    subprojectId = selectedSubprojectId!!
                                                )
                                                forceUpdateUI() // load the "Connecting" icon
                                            } catch (e: Throwable) {
                                                errorMessageVM.showErrorMessage(e.message ?: e.javaClass.name)
                                            }
                                        }
                                    },
                                    onClickCancelFetchApiSpec = {
                                        if (requestNonNull.application == ProtocolApplication.Grpc) {
                                            networkClientManager.cancelFetchingGrpcApiSpec(
                                                url = requestNonNull.url,
                                                environment = selectedEnvironment,
                                                subprojectId = selectedSubprojectId!!
                                            )
                                        }
                                    },
                                    isFetchingApiSpec = run {
                                        val selectedSubprojectId = selectedSubprojectId ?: return@run false
                                        val r = if (requestNonNull.application == ProtocolApplication.Grpc) {
                                            networkClientManager.subscribeGrpcApiSpecFetchingStatus(
                                                url = requestNonNull.url,
                                                environment = selectedEnvironment,
                                                subprojectId = selectedSubprojectId
                                            ).collectAsState().value
                                        } else {
                                            false
                                        }
                                        log.d { "isFetchingApiSpec = $r" }
                                        r
                                    },
                                )
                            } ?: RequestEditorEmptyView(
                                modifier = requestEditorModifier,
                                isShowCreateRequest = selectedSubproject != null && requestCollection != null
                            ) {
                                val newRequest = createRequestForCurrentSubproject(parentId = null)
                                editRequestNameViewModel.onStartEdit(newRequest.id)
                            }
                        }
                        second(minSize = 200.dp) {
                            ResponseViewerView(
                                response = response?.copy() ?: UserResponse(
                                    id = "-",
                                    requestId = "-",
                                    requestExampleId = "-"
                                ),
                                connectionStatus = selectedRequestExampleId
                                    ?.let(networkClientManager::getStatusByRequestExampleId)
                                    ?: ConnectionStatus.DISCONNECTED,
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

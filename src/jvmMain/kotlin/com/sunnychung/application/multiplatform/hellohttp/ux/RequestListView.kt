package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RequestListView(
    selectedSubproject: Subproject,
//    treeObjects: List<TreeObject>,
    requests: Map<String, UserRequest>,
    selectedRequest: UserRequest?,
    editTreeObjectNameViewModel: EditNameViewModel,
    onSelectRequest: (UserRequest) -> Unit,
    onAddRequest: () -> UserRequest,
    onUpdateRequest: (UserRequest) -> Unit,
    onDeleteRequest: (UserRequest) -> Unit,
    onFocusNameTextField: () -> Unit,
    onUnfocusNameTextField: () -> Unit,
    onAddFolder: () -> TreeFolder,
    onUpdateFolder: (TreeFolder) -> Unit,
    onDeleteFolder: (TreeFolder) -> Unit,
    onMoveTreeObject: (treeObjectId: String, destination: TreeFolder?) -> Unit,
) {
    val colors = LocalColor.current

    var searchText by remember { mutableStateOf("") }
    var selectedTreeObjectId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var treeParentBound by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val treeObjectBounds = remember { mutableStateMapOf<String, DropTargetInfo>() }
    var draggingOverTreeObjectId by remember { mutableStateOf<String?>(null) }

    var isEditing = editTreeObjectNameViewModel.isEditing.collectAsState().value

    val treeObjects = selectedSubproject.treeObjects

    log.d { "RequestListView recompose ${treeObjects.size} ${requests.size}" }

    @Composable
    fun RequestLeafView(it: UserRequest) {
        var myBound by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
        var draggedPoint by remember { mutableStateOf<Offset?>(null) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.combinedClickable(
                onClick = { onSelectRequest(it) },
                onDoubleClick = {
                    selectedTreeObjectId = it.id
                    onSelectRequest(it)
                    editTreeObjectNameViewModel.onStartEdit()
                }
            )
                .onGloballyPositioned { treeParentBound?.let { p -> myBound = p.localBoundingBoxOf(it); /*log.d { "req rect $myBound" }*/ } }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.type == PointerEventType.Release) {
                                log.d { "Pointer release ${it.name}" }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            draggedPoint = Offset(myBound.left, myBound.top) + it
                        },
                        onDrag = { offset ->
                            draggedPoint = draggedPoint!! + offset
                            var intersect: DropTargetInfo? = null
                            for ((key, value) in treeObjectBounds) {
                                if (draggedPoint!! in value.bounds) {
                                    intersect = value
                                    break
                                }
                            }
//                            log.d { "onDrag o=$offset b=$draggedPoint intersects $intersect"}
                            draggingOverTreeObjectId = intersect?.folder?.id
                        },
                        onDragCancel = {
                            draggingOverTreeObjectId = null
                            draggedPoint = null
                        },
                        onDragEnd = {
                            log.d { "Dragged into ${treeObjectBounds[draggingOverTreeObjectId]?.folder?.name}" }

                            if (draggingOverTreeObjectId != null) {
                                val (dropTargetParent, dropTarget) = selectedSubproject.findParentAndItem(draggingOverTreeObjectId!!)
                                val destination = if (dropTarget is TreeFolder) {
                                    dropTarget
                                } else {
                                    dropTargetParent as TreeFolder?
                                }
                                onMoveTreeObject(it.id, destination)
                            }

                            draggingOverTreeObjectId = null
                            draggedPoint = null
                        },
                    )
                }
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
            LabelOrTextField(
                isEditing = isEditing && selectedTreeObjectId == it.id,
                editNameViewModel = editTreeObjectNameViewModel,
                labelColor = if (selectedRequest?.id == it.id) colors.highlight else colors.primary,
                value = it.name,
                onValueUpdate = { v -> onUpdateRequest(it.copy(name = v)) },
                onFocus = onFocusNameTextField,
                onUnfocus = onUnfocusNameTextField,
                modifier = Modifier.weight(1f),
            )
            AppDeleteButton { onDeleteRequest(it) }
        }
    }

    @Composable
    fun FolderView(folder: TreeFolder,
                   isExpanded: Boolean,
                   onExpandUnexpand: (isExpanded: Boolean) -> Unit,
                   onDelete: () -> Unit
    ) {
        var modifier = Modifier.combinedClickable(
            onClick = { onExpandUnexpand(!isExpanded) },
            onDoubleClick = {
                selectedTreeObjectId = folder.id
                editTreeObjectNameViewModel.onStartEdit()
            }
        )
            .onGloballyPositioned {
                treeParentBound?.let { treeParentBound ->
                    treeObjectBounds[folder.id] = DropTargetInfo(treeParentBound.localBoundingBoxOf(it), folder)
                }
            }
        if (draggingOverTreeObjectId == folder.id) {
            modifier = modifier.background(Color(0f, 0f, 0.3f))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            AppImage(
                resource = if (isExpanded) "folder-open.svg" else "folder.svg",
                size = 16.dp,
            )
            LabelOrTextField(
                isEditing = isEditing && selectedTreeObjectId == folder.id,
                editNameViewModel = editTreeObjectNameViewModel,
                value = folder.name,
                onValueUpdate = { v -> onUpdateFolder(folder.copy(name = v)) },
                onFocus = onFocusNameTextField,
                onUnfocus = onUnfocusNameTextField,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            AppDeleteButton { onDelete() }
        }
    }

    @Composable
//    fun LazyListScope.TreeObjectView(obj: TreeObject) {
    fun ColumnScope.TreeObjectView(obj: TreeObject) {
        when (obj) {
            is TreeRequest -> {
                    RequestLeafView(requests[obj.id]!!)
            }

            is TreeFolder -> {
                var isExpanded by remember { mutableStateOf(false) }

                    FolderView(
                        folder = obj,
                        isExpanded = isExpanded, onExpandUnexpand = { isExpanded = it },
                        onDelete = { onDeleteFolder(obj) }
                    )
                if (isExpanded) {
                    Column {
                        obj.childs.forEach {
                            log.d { "expanded to show ${it}" }
                            this@Column.TreeObjectView(it)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            AppTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = { AppImage(resource = "search.svg", size = 16.dp, modifier = Modifier.padding(horizontal = 6.dp)) },
                modifier = Modifier.weight(1f).border(width = 1.dp, color = colors.placeholder, shape = RoundedCornerShape(2.dp))
            )
            DropDownView(
                iconResource = "add.svg",
                iconSize = 24.dp,
                items = listOf(DropDownValue("Request"), DropDownValue("Folder")),
                isShowLabel = false,
                onClickItem = {
                    selectedTreeObjectId = when (it.displayText) {
                        "Request" -> onAddRequest().id
                        "Folder" -> {
                            onAddFolder().id
                        }
                        else -> null
                    }
                    editTreeObjectNameViewModel.onStartEdit()
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                    true
                },
                modifier = Modifier.padding(4.dp)
            )
        }

//        LazyColumn { // LazyColumn does not work properly with nested items
//            items(items = treeObjects) {
//                this@LazyColumn.TreeObjectView(it)
//            }
//        }

        Column(modifier = Modifier.verticalScroll(scrollState).onGloballyPositioned { treeParentBound = it }) {
            treeObjects.forEach {
                Column(/*verticalAlignment = Alignment.CenterVertically,*/ modifier = Modifier.defaultMinSize(minHeight = 28.dp)) {
                    TreeObjectView(it)
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
        selectedSubproject = Subproject(id = "preview", name = "", treeObjects = dummyRequests.map { TreeRequest(it.id) }.toMutableList()),
//        treeObjects = dummyRequests.map { TreeRequest(it.id) },
        requests = dummyRequests.associateBy { it.id },
        selectedRequest = dummyRequests[2],
        editTreeObjectNameViewModel = EditNameViewModel(),
        onSelectRequest = {},
        onAddRequest = { UserRequest(id = uuidString()) },
        onUpdateRequest = {},
        onDeleteRequest = {},
        onFocusNameTextField = {},
        onUnfocusNameTextField = {},
        onAddFolder = { TreeFolder(id = uuidString(), name = "", childs = mutableListOf()) },
        onUpdateFolder = {},
        onDeleteFolder = {},
        onMoveTreeObject = {_, _ ->},
    )
}

data class DropTargetInfo(var bounds: Rect, val folder: TreeFolder?)

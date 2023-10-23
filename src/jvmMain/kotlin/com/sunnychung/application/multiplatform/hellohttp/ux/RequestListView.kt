package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.MoveDirection
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RequestTreeView(
    selectedSubproject: Subproject,
//    treeObjects: List<TreeObject>,
    requests: Map<String, UserRequestTemplate>,
    selectedRequest: UserRequestTemplate?,
    editTreeObjectNameViewModel: EditNameViewModel,
    onSelectRequest: (UserRequestTemplate) -> Unit,
    onAddRequest: () -> UserRequestTemplate,
    onUpdateRequest: (UserRequestTemplate) -> Unit,
    onDeleteRequest: (UserRequestTemplate) -> Unit,
    onFocusNameTextField: () -> Unit,
    onUnfocusNameTextField: () -> Unit,
    onAddFolder: () -> TreeFolder,
    onUpdateFolder: (TreeFolder) -> Unit,
    onDeleteFolder: (TreeFolder) -> Unit,
    onMoveTreeObject: (treeObjectId: String, direction: MoveDirection, destination: TreeObject?) -> Unit,
) {
    val colors = LocalColor.current

    var searchText by remember { mutableStateOf("") }
    var selectedTreeObjectId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var treeParentBound by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val treeObjectBounds = remember { mutableStateMapOf<String, DropTargetInfo>() }
    var draggingOverDropTarget by remember { mutableStateOf<DropTargetInfo?>(null) }

    var isEditing = editTreeObjectNameViewModel.isEditing.collectAsState().value
    val isDraggable = searchText.isEmpty()

    val treeObjects = filterTreeObjects(
        rootObjects = selectedSubproject.treeObjects,
        containText = searchText,
        requests = requests
    )

    log.d { "RequestListView recompose ${treeObjects.size} ${requests.size} isDraggable=$isDraggable" }

    @Composable
    fun Draggable(modifier: Modifier = Modifier, isEnableDrag: Boolean, id: String, onDrop: (DropTargetInfo) -> Unit, content: @Composable () -> Unit) {
        var myBound by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
        var draggedPoint by remember { mutableStateOf<Offset?>(null) }
        var dragIsActive by remember { mutableStateOf(false) }
        Box(
            modifier = modifier
                .onGloballyPositioned { treeParentBound?.let { p -> myBound = p.localBoundingBoxOf(it); /*log.d { "req rect $myBound" }*/ } }
                .pointerInput(isEnableDrag, id) { // https://stackoverflow.com/questions/72299963/value-of-mutablestate-inside-modifier-pointerinput-doesnt-change-after-remember
                    detectDragGestures(
                        onDragStart = {
                            draggedPoint = Offset(myBound.left, myBound.top) + it
                            log.d { "drag start $isEnableDrag" }
                            dragIsActive = isEnableDrag
                        },
                        onDrag = { offset ->
                            if (dragIsActive) {
                                draggedPoint = draggedPoint!! + offset
                                var intersect: DropTargetInfo? = null
                                for ((key, value) in treeObjectBounds) {
//                                    log.v { "onDrag e $value" }
                                    // when there are overlapping regions, "Before" and "After" bars have higher priority than "Inside" folders
                                    if (draggedPoint!! in value.bounds && (intersect == null || value.direction != MoveDirection.Inside)) {
                                        intersect = value
//                                        log.d { "onDrag replace ${intersect?.direction} ${intersect?.item}"}
                                    }
                                }
//                            log.v { "onDrag o=$offset b=$draggedPoint intersects $intersect"}
//                            log.d { "onDrag intersects ${intersect?.direction} ${intersect?.item}"}
                                draggingOverDropTarget = intersect

                                log.v { "onDrag point=$draggedPoint intersects ${intersect?.direction}/${intersect?.item?.id?.let { requests[it]?.name }}"}
                            }
                        },
                        onDragCancel = {
                            dragIsActive = false
                            draggingOverDropTarget = null
                            draggedPoint = null
                        },
                        onDragEnd = {
//                            log.d { "Dragged into ${draggingOverDropTarget?.item?.name}" }

                            if (dragIsActive && draggingOverDropTarget != null) {
                                onDrop(draggingOverDropTarget!!)
                            }

                            dragIsActive = false
                            draggingOverDropTarget = null
                            draggedPoint = null
                        },
                    )
                }
                .onKeyEvent {
                    if (it.key == Key.Escape) {
                        log.d { "Detected ESC to cancel drag" }
                        dragIsActive = false
                        draggingOverDropTarget = null
                        true
                    } else {
                        false
                    }
                }
        ) {
            content()
        }
    }

    fun Modifier.droppable(direction: MoveDirection, item: TreeObject?): Modifier {
        return this.onGloballyPositioned {
            treeParentBound?.let { treeParentBound ->
//                log.v { "registered - $direction $item" }
                treeObjectBounds["$direction:${item?.id ?: "none"}"] = DropTargetInfo(
                    bounds = treeParentBound.localBoundingBoxOf(it),
                    item = item,
                    direction = direction
                )
                log.v { "registered [$direction:${item?.id ?: "none"}] = ${treeObjectBounds["$direction:${item?.id ?: "none"}"]}" }
            } // ?: log.v { "not registered - $direction $item" }
        }
    }

    fun TreeObject.describe(): String {
        return when (this) {
            is TreeFolder -> name
            is TreeRequest -> requests[id]?.name ?: "unknown request"
        }
    }

    fun handleDropAction(draggable: TreeObject, droppedAt: DropTargetInfo) {
        // TODO handle reorder as well
        if (droppedAt.direction == MoveDirection.Inside) {
            val (dropTargetParent, dropTarget) = selectedSubproject.findParentAndItem(droppedAt.item!!.id)
            val destination = if (dropTarget is TreeFolder) {
                dropTarget
            } else {
                dropTargetParent as TreeFolder? // TODO can remove?
            }
            log.d { "dropped ${draggable.describe()} in ${destination?.describe()}" }
            onMoveTreeObject(draggable.id, droppedAt.direction, destination)
        } else {
            log.d { "dropped ${draggable.describe()} ${droppedAt.direction} ${droppedAt.item?.describe()}" }
            onMoveTreeObject(draggable.id, droppedAt.direction, droppedAt.item)
        }
    }

    @Composable
    fun RequestLeafView(modifier: Modifier = Modifier, it: UserRequestTemplate) {
        Draggable(
            isEnableDrag = isDraggable,
            id = it.id,
            onDrop = { destination -> handleDropAction(draggable = TreeRequest(id = it.id), droppedAt = destination) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.combinedClickable(
                    onClick = { onSelectRequest(it) },
                    onDoubleClick = {
                        selectedTreeObjectId = it.id
                        onSelectRequest(it)
                        editTreeObjectNameViewModel.onStartEdit()
                    }
                )
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
    }

    @Composable
    fun FolderView(
        modifier: Modifier = Modifier,
        folder: TreeFolder,
        isExpanded: Boolean,
        onExpandUnexpand: (isExpanded: Boolean) -> Unit,
        onDelete: () -> Unit
    ) {
        Draggable(
            isEnableDrag = isDraggable,
            id = folder.id,
            onDrop = { destination -> handleDropAction(draggable = folder, droppedAt = destination) }
        ) {
            var modifierToUse = modifier.combinedClickable(
                onClick = { onExpandUnexpand(!isExpanded) },
                onDoubleClick = {
                    selectedTreeObjectId = folder.id
                    editTreeObjectNameViewModel.onStartEdit()
                }
            )
                .droppable(MoveDirection.Inside, folder)
            if (draggingOverDropTarget?.direction == MoveDirection.Inside && draggingOverDropTarget?.item?.id == folder.id) {
                modifierToUse = modifierToUse.background(colors.backgroundHoverDroppable)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifierToUse
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
    }

    @Composable
//    fun LazyListScope.TreeObjectView(obj: TreeObject) {
    fun ColumnScope.TreeObjectView(indentLevel: Int, obj: TreeObject) {
        val leftPadding = 16.dp * indentLevel
        Box {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                val modifier = Modifier.defaultMinSize(minHeight = 28.dp).padding(start = leftPadding)
                when (obj) {
                    is TreeRequest -> {
                        RequestLeafView(modifier, requests[obj.id]!!)
                    }

                    is TreeFolder -> {
                        var isExpanded by remember { mutableStateOf(false) }

                        FolderView(
                            modifier = modifier,
                            folder = obj,
                            isExpanded = isExpanded,
                            onExpandUnexpand = { isExpanded = it },
                            onDelete = { onDeleteFolder(obj) }
                        )
                        if (isExpanded) {
                            Column {
                                obj.childs.forEach {
                                    this@Column.TreeObjectView(indentLevel = indentLevel + 1, obj = it)
                                }
                            }
                            if (obj.childs.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .droppable(MoveDirection.After, obj.childs.last())
                                ) {
                                    if (draggingOverDropTarget?.direction == MoveDirection.After && draggingOverDropTarget?.item?.id == obj.childs.last().id) {
                                        Surface(
                                            color = colors.backgroundHoverDroppable,
                                            modifier = Modifier.fillMaxSize().padding(start = 16.dp * (indentLevel + 1))
                                        ) {}
                                    }
                                }
                                Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            } else {
                                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(4.dp)
                .droppable(MoveDirection.Before, obj)
            ) {
                if (draggingOverDropTarget?.direction == MoveDirection.Before && draggingOverDropTarget?.item?.id == obj.id) {
                    Surface(
                        color = colors.backgroundHoverDroppable,
                        modifier = Modifier.fillMaxSize().padding(start = leftPadding)
                    ) {}
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

        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .onGloballyPositioned { treeParentBound = it }
            .padding(end = 8.dp)
        ) {
            treeObjects.forEach {
                TreeObjectView(indentLevel = 0, obj = it)
            }
        }
    }
}

fun filterTreeObjects(rootObjects: MutableList<TreeObject>, containText: String, requests: Map<String, UserRequestTemplate>): MutableList<TreeObject> {
    fun transverse(current: TreeObject): TreeObject? {
        return when (current) {
            is TreeRequest -> if (requests[current.id]?.name?.contains(containText, ignoreCase = true) == true) {
                current.copy()
            } else {
                null
            }

            is TreeFolder -> if (current.name.contains(containText, ignoreCase = true)) {
                current.copy()
            } else {
                val childs = current.childs.mapNotNull { transverse(it) }
                if (childs.isNotEmpty()) {
                    current.copy(childs = childs.toMutableList())
                } else {
                    null
                }
            }
        }
    }
    val root = TreeFolder(id = "root", name = "", childs = rootObjects)
    return (transverse(root) as TreeFolder?)?.childs ?: mutableListOf()
}

@Composable
@Preview
fun RequestListViewPreview() {
    val dummyRequests = listOf(
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "GET", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "POST", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "PUT", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Http, method = "DELETE", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Grpc, method = "", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", protocol = Protocol.Graphql, method = "POST", url = "", examples = emptyList()),
    )
    RequestTreeView(
        selectedSubproject = Subproject(id = "preview", name = "", treeObjects = dummyRequests.map { TreeRequest(it.id) }.toMutableList(), mutableListOf()),
//        treeObjects = dummyRequests.map { TreeRequest(it.id) },
        requests = dummyRequests.associateBy { it.id },
        selectedRequest = dummyRequests[2],
        editTreeObjectNameViewModel = EditNameViewModel(),
        onSelectRequest = {},
        onAddRequest = { UserRequestTemplate(id = uuidString()) },
        onUpdateRequest = {},
        onDeleteRequest = {},
        onFocusNameTextField = {},
        onUnfocusNameTextField = {},
        onAddFolder = { TreeFolder(id = uuidString(), name = "", childs = mutableListOf()) },
        onUpdateFolder = {},
        onDeleteFolder = {},
        onMoveTreeObject = {_, _, _ ->},
    )
}

data class DropTargetInfo(var bounds: Rect, val direction: MoveDirection, val item: TreeObject?)

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.extension.isCtrlOrCmdPressed
import com.sunnychung.application.multiplatform.hellohttp.model.MoveDirection
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RequestTreeView(
    selectedSubproject: Subproject,
//    treeObjects: List<TreeObject>,
    requests: Map<String, UserRequestTemplate>,
    selectedRequest: UserRequestTemplate?,
    editTreeObjectNameViewModel: EditNameViewModel,
    onSelectRequest: (UserRequestTemplate) -> Unit,
    onAddRequest: (parentId: String?) -> UserRequestTemplate,
    onUpdateRequest: (UserRequestTemplate) -> Unit,
    onDeleteRequest: (UserRequestTemplate) -> Unit,
    onFocusNameTextField: () -> Unit,
    onUnfocusNameTextField: () -> Unit,
    onAddFolder: (parentId: String?) -> TreeFolder,
    onUpdateFolder: (TreeFolder) -> Unit,
    onDeleteFolder: (TreeFolder) -> Unit,
    onMoveTreeObject: (treeObjectId: String, direction: MoveDirection, destination: TreeObject?) -> Unit,
    onCopyRequest: (treeObjectId: String, direction: MoveDirection, destination: TreeObject?) -> Unit,
    onImportCurlRequest: (String) -> String?,
    onImportJsonRequest: (ImportJsonRequestInput, Boolean) -> ImportJsonRequestResult,
    onExportRequestsToClipboard: (List<UserRequestTemplate>) -> Unit,
    onExportRequestsToFile: (List<UserRequestTemplate>, File) -> Unit,
) {
    val colors = LocalColor.current

    var searchText by remember { mutableStateOf("") }
    var expandedFolderIds = remember { mutableStateMapOf<String, Unit>() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var treeParentBound by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val requestRowBringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }
    val treeObjectBounds = remember { mutableStateMapOf<String, DropTargetInfo>() }
    var draggingOverDropTarget by remember { mutableStateOf<DropTargetInfo?>(null) }

    var isEditing = editTreeObjectNameViewModel.isEditing.collectAsState().value
    val isDraggable = searchText.isEmpty()

    val selectedRequestIds = remember(selectedSubproject.id) { mutableStateMapOf<String, Unit>() }
    var selectedRangeAnchorRequestId by remember(selectedSubproject.id) { mutableStateOf<String?>(selectedRequest?.id) }
    var isShowContextMenu by remember { mutableStateOf(false) }
    var contextMenuAtItemId by remember { mutableStateOf<String?>(null) }
    var contextMenuType by remember { mutableStateOf(RequestTreeContextMenuType.None) }
    var isShowImportCurlDialog by remember { mutableStateOf(false) }
    var isShowImportJsonDialog by remember { mutableStateOf(false) }
    var isShowExportFileDialog by remember { mutableStateOf(false) }
    var requestsToExport by remember { mutableStateOf<List<UserRequestTemplate>>(emptyList()) }
    var exportFilename by remember { mutableStateOf("request.json") }
    val exportFileDialogState = rememberFileDialogState()

    val treeObjects = filterTreeObjects(
        rootObjects = selectedSubproject.treeObjects,
        containText = searchText,
        requests = requests
    )

    LaunchedEffect(
        selectedRequest?.id,
        requestRowBringIntoViewRequesters[selectedRequest?.id],
    ) {
        val selectedRequestId = selectedRequest?.id ?: return@LaunchedEffect
        val bringIntoViewRequester = requestRowBringIntoViewRequesters[selectedRequestId] ?: return@LaunchedEffect
        bringIntoViewRequester.bringIntoView()
    }

    LaunchedEffect(requests.keys) {
        selectedRequestIds.keys.toList()
            .filterNot { it in requests.keys }
            .forEach(selectedRequestIds::remove)
    }

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

    fun collectRequestIdsInTreeOrder(treeObjects: List<TreeObject>): List<String> {
        val requestIds = mutableListOf<String>()
        fun traverse(current: TreeObject) {
            when (current) {
                is TreeRequest -> requestIds += current.id
                is TreeFolder -> current.childs.forEach(::traverse)
            }
        }
        treeObjects.forEach(::traverse)
        return requestIds
    }

    fun collectVisibleRequestIds(treeObjects: List<TreeObject>): List<String> {
        val requestIds = mutableListOf<String>()
        fun traverse(current: TreeObject) {
            when (current) {
                is TreeRequest -> requestIds += current.id
                is TreeFolder -> if (expandedFolderIds.containsKey(current.id)) {
                    current.childs.forEach(::traverse)
                }
            }
        }
        treeObjects.forEach(::traverse)
        return requestIds
    }

    fun getSelectedRequestsForExport(): List<UserRequestTemplate> {
        val selectedIds = selectedRequestIds.keys.toSet()
        if (selectedIds.isEmpty()) {
            return emptyList()
        }

        val orderedTreeRequestIds = collectRequestIdsInTreeOrder(selectedSubproject.treeObjects)
        val requestsInTreeOrder = orderedTreeRequestIds
            .asSequence()
            .filter { it in selectedIds }
            .mapNotNull { requests[it] }
            .toList()

        val selectedIdsInTreeOrder = requestsInTreeOrder.map { it.id }.toSet()
        val remainingRequests = selectedIds
            .asSequence()
            .filterNot { it in selectedIdsInTreeOrder }
            .mapNotNull { requests[it] }
            .toList()

        return requestsInTreeOrder + remainingRequests
    }

    fun createDefaultExportFilename(selectedRequests: List<UserRequestTemplate>): String {
        val rawRequestName = selectedRequests.firstOrNull()?.name.orEmpty()
        val sanitizedName = rawRequestName
            .replace("[\\\\/:*?\"<>|]".toRegex(), "-")
            .replace("[\\u0000-\\u001F]".toRegex(), "")
            .trim()
            .trim('.')
            .ifBlank { "request" }
        return "$sanitizedName.json"
    }

    fun clearAndSelectRequest(request: UserRequestTemplate) {
        selectedRequestIds.clear()
        selectedRequestIds[request.id] = Unit
        selectedRangeAnchorRequestId = request.id
    }

    fun selectRequestByMouseClick(
        request: UserRequestTemplate,
        isShiftPressed: Boolean,
        isCtrlOrCmdPressed: Boolean,
    ) {
        if (isShiftPressed) {
            val visibleRequestIds = collectVisibleRequestIds(treeObjects)
            val anchorRequestId = selectedRangeAnchorRequestId?.takeIf { it in visibleRequestIds }
                ?: selectedRequest?.id?.takeIf { it in visibleRequestIds }
                ?: request.id
            val anchorIndex = visibleRequestIds.indexOf(anchorRequestId)
            val clickedIndex = visibleRequestIds.indexOf(request.id)
            if (anchorIndex >= 0 && clickedIndex >= 0) {
                if (!isCtrlOrCmdPressed) {
                    selectedRequestIds.clear()
                }
                visibleRequestIds.subList(
                    fromIndex = minOf(anchorIndex, clickedIndex),
                    toIndex = maxOf(anchorIndex, clickedIndex) + 1,
                ).forEach {
                    selectedRequestIds[it] = Unit
                }
                selectedRangeAnchorRequestId = anchorRequestId
            } else {
                clearAndSelectRequest(request)
            }
            onSelectRequest(request)
            return
        }

        if (isCtrlOrCmdPressed) {
            if (selectedRequestIds.containsKey(request.id)) {
                selectedRequestIds.remove(request.id)
            } else {
                selectedRequestIds[request.id] = Unit
            }
            selectedRangeAnchorRequestId = request.id
            onSelectRequest(request)
            return
        }

        clearAndSelectRequest(request)
        onSelectRequest(request)
    }

    fun handleRequestSecondaryClick(request: UserRequestTemplate) {
        if (!selectedRequestIds.containsKey(request.id)) {
            clearAndSelectRequest(request)
        }
        onSelectRequest(request)
        contextMenuType = RequestTreeContextMenuType.RequestExport
        isShowContextMenu = true
    }

    @Composable
    fun RequestLeafView(modifier: Modifier = Modifier, it: UserRequestTemplate) {
        val requestBringIntoViewRequester = remember { BringIntoViewRequester() }
        DisposableEffect(it.id) {
            requestRowBringIntoViewRequesters[it.id] = requestBringIntoViewRequester
            onDispose {
                if (requestRowBringIntoViewRequesters[it.id] === requestBringIntoViewRequester) {
                    requestRowBringIntoViewRequesters.remove(it.id)
                }
            }
        }
        Draggable(
            isEnableDrag = isDraggable,
            id = it.id,
            onDrop = { destination -> handleDropAction(draggable = TreeRequest(id = it.id), droppedAt = destination) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .background(
                        color = if (selectedRequestIds.containsKey(it.id)) {
                            colors.backgroundInputFieldHighlight.copy(alpha = 0.6f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(2.dp),
                    )
                    .testTag(
                        if (selectedRequest?.id == it.id) {
                            buildTestTag(TestTag.RequestTreeRequestRow.name, it.id, "selected")!!
                        } else {
                            buildTestTag(TestTag.RequestTreeRequestRow.name, it.id)!!
                        }
                    )
                    .semantics {
                        // Compose UI tests occasionally fail to find row text reliably from the same node that is
                        // later used for interaction (merged/unmerged semantics behavior differs by API).
                        // Expose row metadata explicitly so tests can query deterministic fields.
                        requestTreeRowRequestId = it.id
                        requestTreeRowMethod = it.method
                        requestTreeRowName = it.name
                        requestTreeRowUrl = it.url
                        // Expose explicit semantics click action.
                        // In tests, pointer-based click can be flaky for scrolled/off-screen rows in non-lazy trees,
                        // while semantics action is significantly more stable.
                        onClick(label = "Select request") {
                            clearAndSelectRequest(it)
                            onSelectRequest(it)
                            true
                        }
                    }
                    .bringIntoViewRequester(requestBringIntoViewRequester)
                    .onPointerEvent(PointerEventType.Press) { event ->
                        when (event.button) {
                            PointerButton.Primary -> {
                                selectRequestByMouseClick(
                                    request = it,
                                    isShiftPressed = event.keyboardModifiers.isShiftPressed,
                                    isCtrlOrCmdPressed = event.keyboardModifiers.isCtrlOrCmdPressed(),
                                )
                            }

                            PointerButton.Secondary -> {
                                handleRequestSecondaryClick(it)
                            }
                        }
                    }
                    .combinedClickable(
                        onClick = {}, // not using this because there will be a significant delay
                        onDoubleClick = {
                            onSelectRequest(it)
                            editTreeObjectNameViewModel.onStartEdit(it.id)
                        }
                    )
            ) {
                val (text, color) = when (it.application) {
                    ProtocolApplication.Http -> Pair(
                        it.method, when (it.method) {
                            "GET" -> colors.httpRequestGet
                            "POST" -> colors.httpRequestPost
                            "PUT" -> colors.httpRequestPut
                            "PATCH" -> colors.httpRequestPatch
                            "DELETE" -> colors.httpRequestDelete
                            else -> colors.httpRequestOthers
                        }
                    )

                    ProtocolApplication.WebSocket -> Pair("WS", colors.websocketRequest)
                    ProtocolApplication.Grpc -> Pair("gRPC", colors.grpcRequest)
                    ProtocolApplication.Graphql -> Pair("GQL", colors.graphqlRequest)
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
                    isEditing = isEditing && editTreeObjectNameViewModel.editingItemId.value == it.id,
                    editNameViewModel = editTreeObjectNameViewModel,
                    labelColor = if (selectedRequest?.id == it.id) colors.highlight else colors.primary,
                    value = it.name,
                    onValueUpdate = { v -> onUpdateRequest(it.copy(name = v)) },
                    onFocus = onFocusNameTextField,
                    onUnfocus = onUnfocusNameTextField,
                    modifier = Modifier.weight(1f),
                )
                AppTooltipArea(
                    tooltipText = "Duplicate",
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    AppImageButton(
                        resource = "duplicate.svg",
                        size = 16.dp,
                        color = colors.placeholder,
                        onClick = {
                            onCopyRequest(it.id, MoveDirection.After, TreeRequest(id = it.id))
                        },
                    )
                }
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
            var modifierToUse = modifier.onClick(
                matcher = PointerMatcher.Primary,
                onClick = {}, // not using this because there will be a significant delay
                onDoubleClick = {
                    editTreeObjectNameViewModel.onStartEdit(folder.id)
                }
            )
                .onPointerEvent(PointerEventType.Press) {
                    when (it.button) {
                        PointerButton.Primary -> {
                            onExpandUnexpand(!isExpanded)
                        }
                        PointerButton.Secondary -> {
                            contextMenuAtItemId = folder.id
                            contextMenuType = RequestTreeContextMenuType.Folder
                            isShowContextMenu = true
                        }
                    }
                }
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
                    isEditing = isEditing && editTreeObjectNameViewModel.editingItemId.value == folder.id,
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
                        requests[obj.id]?.let { request ->
                            RequestLeafView(modifier, request)
                        }
                    }

                    is TreeFolder -> {
                        val isExpanded = expandedFolderIds.containsKey(obj.id)

                        FolderView(
                            modifier = modifier,
                            folder = obj,
                            isExpanded = isExpanded,
                            onExpandUnexpand = { when(it) {
                                true -> expandedFolderIds[obj.id] = Unit
                                false -> expandedFolderIds.remove(obj.id)
                            } },
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

    fun createRequestOrFolder(resourceType: ResourceType, parentId: String?) {
        val itemId = when (resourceType) {
            ResourceType.Request -> onAddRequest(parentId).id
            ResourceType.Folder -> {
                onAddFolder(parentId).id
            }
            ResourceType.ImportCurlCommand -> {
                throw UnsupportedOperationException("Use import dialog for cURL command import")
            }
            ResourceType.ImportJson -> {
                throw UnsupportedOperationException("Use import dialog for JSON import")
            }
        }
        editTreeObjectNameViewModel.onStartEdit(itemId)
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    CursorDropdownMenu(
        expanded = isShowContextMenu,
        onDismissRequest = {
            isShowContextMenu = false
            contextMenuType = RequestTreeContextMenuType.None
        },
        modifier = Modifier.background(colors.backgroundContextMenu)
    ) {
        when (contextMenuType) {
            RequestTreeContextMenuType.Folder -> {
                listOf("New Request", "New Folder").forEach { item ->
                    Column(
                        modifier = Modifier.clickable {
                            expandedFolderIds[contextMenuAtItemId!!] = Unit
                            createRequestOrFolder(
                                resourceType = when (item) {
                                    "New Request" -> ResourceType.Request
                                    "New Folder" -> ResourceType.Folder
                                    else -> throw UnsupportedOperationException()
                                },
                                parentId = contextMenuAtItemId,
                            )
                            isShowContextMenu = false
                            contextMenuType = RequestTreeContextMenuType.None
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AppText(text = item)
                    }
                }
            }

            RequestTreeContextMenuType.RequestExport -> {
                listOf("Export as JSON to clipboard", "Export as JSON to file").forEach { item ->
                    Column(
                        modifier = Modifier.clickable {
                            val selectedRequests = getSelectedRequestsForExport()
                            when (item) {
                                "Export as JSON to clipboard" -> {
                                    if (selectedRequests.isNotEmpty()) {
                                        onExportRequestsToClipboard(selectedRequests)
                                    }
                                }

                                "Export as JSON to file" -> {
                                    requestsToExport = selectedRequests
                                    exportFilename = createDefaultExportFilename(selectedRequests)
                                    isShowExportFileDialog = selectedRequests.isNotEmpty()
                                }
                            }
                            isShowContextMenu = false
                            contextMenuType = RequestTreeContextMenuType.None
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AppText(text = item)
                    }
                }
            }

            RequestTreeContextMenuType.None -> Unit
        }
    }

    if (isShowExportFileDialog) {
        FileDialog(
            state = exportFileDialogState,
            mode = java.awt.FileDialog.SAVE,
            title = "Export Selected Requests",
            filename = exportFilename,
        ) {
            isShowExportFileDialog = false
            val file = it?.firstOrNull()
            val requestsForExport = requestsToExport
            requestsToExport = emptyList()
            if (file != null && requestsForExport.isNotEmpty()) {
                onExportRequestsToFile(requestsForExport, file)
            }
        }
    }

    ImportCurlCommandDialog(
        isEnabled = isShowImportCurlDialog,
        onDismiss = { isShowImportCurlDialog = false },
        onImportCommand = { command ->
            val importedRequestId = onImportCurlRequest(command)
            if (importedRequestId == null) {
                false
            } else {
                selectedRequestIds.clear()
                selectedRequestIds[importedRequestId] = Unit
                selectedRangeAnchorRequestId = importedRequestId
                true
            }
        },
    )

    ImportJsonRequestDialog(
        isEnabled = isShowImportJsonDialog,
        onDismiss = { isShowImportJsonDialog = false },
        onImportJson = { input, isAcceptDataLossRisk ->
            when (val result = onImportJsonRequest(input, isAcceptDataLossRisk)) {
                is ImportJsonRequestResult.Success -> {
                    selectedRequestIds.clear()
                    selectedRequestIds[result.selectedRequestId] = Unit
                    selectedRangeAnchorRequestId = result.selectedRequestId
                    result
                }

                else -> result
            }
        },
    )

    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            AppTextField(
                key = "RequestTree/SearchText",
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = { AppImage(resource = "search.svg", size = 16.dp, modifier = Modifier.padding(start = 2.dp, end = 6.dp)) },
                modifier = Modifier.weight(1f).border(width = 1.dp, color = colors.placeholder, shape = RoundedCornerShape(2.dp))
            )
            DropDownView(
                iconResource = "add.svg",
                iconSize = 24.dp,
                items = ResourceType.values().map { DropDownKeyValue(it, it.displayText) },
                isShowLabel = false,
                onClickItem = {
                    when (it.key) {
                        ResourceType.Request -> {
                            createRequestOrFolder(resourceType = ResourceType.Request, parentId = null)
                        }

                        ResourceType.Folder -> {
                            createRequestOrFolder(resourceType = ResourceType.Folder, parentId = null)
                        }

                        ResourceType.ImportCurlCommand -> {
                            isShowImportCurlDialog = true
                        }

                        ResourceType.ImportJson -> {
                            isShowImportJsonDialog = true
                        }
                    }
                    true
                },
                modifier = Modifier.padding(4.dp)
                    .testTag(TestTag.CreateRequestOrFolderButton.name)
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
            .testTag(TestTag.RequestTreeScrollContainer.name)
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
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Http, method = "GET", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Http, method = "POST", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Http, method = "PUT", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Http, method = "DELETE", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Grpc, method = "", url = "", examples = emptyList()),
        UserRequestTemplate(id = uuidString(), name = "abc", application = ProtocolApplication.Graphql, method = "POST", url = "", examples = emptyList()),
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
        onCopyRequest = {_, _, _ ->},
        onImportCurlRequest = { null },
        onImportJsonRequest = { _, _ -> ImportJsonRequestResult.Error },
        onExportRequestsToClipboard = {},
        onExportRequestsToFile = { _, _ -> },
    )
}

data class DropTargetInfo(var bounds: Rect, val direction: MoveDirection, val item: TreeObject?)

private enum class RequestTreeContextMenuType {
    None,
    Folder,
    RequestExport,
}

private enum class ResourceType {
    Request,
    Folder,
    ImportCurlCommand,
    ImportJson,
    ;

    val displayText: String
        get() = when (this) {
            Request -> "Request"
            Folder -> "Folder"
            ImportCurlCommand -> "Import cURL command (Linux / macOS)"
            ImportJson -> "Import from JSON"
        }
}

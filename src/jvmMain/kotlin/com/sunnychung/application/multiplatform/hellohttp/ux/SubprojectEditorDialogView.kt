package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ApiSpecDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.replaceIf
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import kotlinx.coroutines.Dispatchers

@Composable
fun SubprojectEditorDialogView(
    modifier: Modifier = Modifier,
    projectId: String,
    subprojectId: String,
) {
    val colours = LocalColor.current

    val projectCollectionRepository = AppContext.ProjectCollectionRepository
    val apiSpecificationCollectionRepository = AppContext.ApiSpecificationCollectionRepository

    log.d { "SubprojectEditorDialogView recompose" }

    val focusRequester = remember { FocusRequester() }
    var cachedSubproject by rememberLast(subprojectId) { mutableStateOf<Subproject?>(null) }
    val subproject = projectCollectionRepository.subscribeLatestSubproject(
        ProjectAndEnvironmentsDI(), subprojectId
    ).collectAsState(null, Dispatchers.Main.immediate).value?.first /*?: cachedSubproject*/ ?: return
    cachedSubproject = subproject

    val projectApiSpecCollection = apiSpecificationCollectionRepository.subscribeLatestCollection(
        ApiSpecDI(projectId)
    ).collectAsState(null, Dispatchers.Main.immediate).value?.first
    val projectGrpcSpecs = projectApiSpecCollection?.grpcApiSpecs?.associateBy { it.id }
    val grpcApiSpecs = subproject.grpcApiSpecIds.mapNotNull {
        projectGrpcSpecs?.get(it)
    }

    fun onSubprojectUpdate() {
        cachedSubproject = subproject
        projectCollectionRepository.updateSubproject(ProjectAndEnvironmentsDI(), subproject)
    }

    fun onProjectApiSpecUpdate() {
        apiSpecificationCollectionRepository.notifyUpdated(ApiSpecDI(projectId))
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AppText(text = "Subproject Name:  ")
            AppTextFieldWithPlaceholder(
                key = "SubprojectEditorDialogView/${subproject.id}/SubprojectName",
                value = subproject.name,
                onValueChange = {
                    subproject.name = it
                    onSubprojectUpdate()
                },
                placeholder = { AppText(text = "Subproject Name", color = colours.placeholder) },
                singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
        }

        TabsView(
            selectedIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it },
            contents = SubprojectEditorTab.values().map { { AppTabLabel(text = it.displayText) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        when (SubprojectEditorTab.values()[selectedTabIndex]) {
            SubprojectEditorTab.Configuration -> ConfigurationEditor(
                subproject = subproject,
                onSubprojectUpdate = { onSubprojectUpdate() },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            SubprojectEditorTab.GrpcApiSpec -> GrpcApiSpecEditor(
                grpcApiSpecs = grpcApiSpecs,
                projectApiSpecCollection = projectApiSpecCollection,
                subproject = subproject,
                onSubprojectUpdate = { onSubprojectUpdate() },
                onProjectApiSpecUpdate = { onProjectApiSpecUpdate() },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private val HEADER_COLUMN_WIDTH = 250.dp

@Composable
private fun ConfigurationEditor(
    modifier: Modifier = Modifier,
    subproject: Subproject,
    onSubprojectUpdate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = modifier.verticalScroll(rememberScrollState())) {
        ConfigurationRow(
            title = "Is Cookie Enabled",
        ) {
            AppCheckbox(
                checked = subproject.configuration.isCookieEnabled(),
                onCheckedChange = {
                    subproject.configuration.isCookieEnabled = it
                    onSubprojectUpdate()
                },
                size = 24.dp,
                modifier = Modifier.testTag(TestTag.SubprojectEditorCookieCheckbox.name)
            )
        }
        PayloadLimitEditorView(
            title = "Display Limit per Outbound Payload in Raw Transport Log",
            hint = "Maximum no. of bytes to be displayed and persisted for each outbound payload in Raw Transport Timeline. If the limit is exceeded, the payload would be truncated to this limit.\n\nIn HTTP/2, it is common to have multiple payloads if the request data size is large, whereas in HTTP/1 it is usually only maximum one payload per direction.\n\nThis setting only applies to new calls, and does not affect data transmission.",
            defaultValue = "512 KB",
            propertyGetter = { subproject.configuration.outboundPayloadStorageLimit },
            propertySetter = {
                subproject.configuration.outboundPayloadStorageLimit = it
                onSubprojectUpdate()
            },
            subproject = subproject,
        )
        PayloadLimitEditorView(
            title = "Display Limit per Inbound Payload in Raw Transport Log",
            hint = "Maximum no. of bytes to be displayed and persisted for each inbound payload in Raw Transport Timeline. If the limit is exceeded, the payload would be truncated to this limit.\n\nIn HTTP/2, it is common to have multiple payloads if the response data size is large, whereas in HTTP/1 it is usually only maximum one payload per direction.\n\nThis setting only applies to new calls, and does not affect data transmission.",
            defaultValue = "512 KB",
            propertyGetter = { subproject.configuration.inboundPayloadStorageLimit },
            propertySetter = {
                subproject.configuration.inboundPayloadStorageLimit = it
                onSubprojectUpdate()
            },
            subproject = subproject,
        )
        PayloadLimitEditorView(
            title = "Total Displayed HTTP/2 Outbound Data Size Limit per Call in Raw Transport Log",
            hint = "Maximum accumulated no. of body bytes of outbound DATA frames in a HTTP/2 call to be displayed and persisted in Raw Transport Timeline, excluding frame headers.\n\nThis setting only applies to new calls, and does not affect data transmission.",
            defaultValue = "2 MB",
            propertyGetter = { subproject.configuration.accumulatedOutboundDataStorageLimitPerCall },
            propertySetter = {
                subproject.configuration.accumulatedOutboundDataStorageLimitPerCall = it
                onSubprojectUpdate()
            },
            subproject = subproject,
        )
        PayloadLimitEditorView(
            title = "Total Displayed HTTP/2 Inbound Data Size Limit per Call in Raw Transport Log",
            hint = "Maximum accumulated no. of body bytes of inbound DATA frames in a HTTP/2 call to be displayed and persisted in Raw Transport Timeline, excluding frame headers.\n\nThis setting only applies to new calls, and does not affect data transmission.",
            defaultValue = "2 MB",
            propertyGetter = { subproject.configuration.accumulatedInboundDataStorageLimitPerCall },
            propertySetter = {
                subproject.configuration.accumulatedInboundDataStorageLimitPerCall = it
                onSubprojectUpdate()
            },
            subproject = subproject,
        )
    }
}

@Composable
private fun PayloadLimitEditorView(
    modifier: Modifier = Modifier,
    title: String,
    hint: String? = null,
    defaultValue: String,
    propertyGetter: () -> Long,
    propertySetter: (Long) -> Unit,
    subproject: Subproject,
) {
    var textValue by rememberLast(subproject.id) {
        mutableStateOf(propertyGetter().toString())
    }
    ConfigurationRow(
        modifier = modifier,
        title = title,
        hint = hint,
        remark = "Acceptable range: 8192 (8 KB) ~ 104857600 (100 MB);\nNegative = use default ($defaultValue)",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                key = "Subproject/${subproject.id}/Configuration/PayloadLimit/$title",
                value = textValue,
                onValueChange = {
                    if (it.length > "104857600".length) return@AppTextField
                    if (!it.matches("-?[0-9]*".toRegex())) return@AppTextField

                    textValue = it
                    it.toLongOrNull()?.let {
                        if (it < 0 || it in (8 * 1024)..(100 * 1024 * 1024)) {
                            propertySetter(it)
                        }
                    }
                }
            )
            AppText("bytes")
        }
    }
}

@Composable
private fun ConfigurationRow(
    modifier: Modifier = Modifier,
    title: String,
    hint: String? = null,
    remark: String? = null,
    content: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(HEADER_COLUMN_WIDTH)) {
            AppText(text = title, modifier = Modifier.weight(1f))
            if (!hint.isNullOrEmpty()) {
                AppTooltipArea(tooltipText = hint) {
                    AppImage(resource = "question.svg", size = 16.dp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Column {
            content()
            if (remark != null) {
                AppText(remark)
            }
        }
    }
}

@Composable
private fun GrpcApiSpecEditor(
    modifier: Modifier = Modifier,
    grpcApiSpecs: List<GrpcApiSpec>,
    projectApiSpecCollection: ApiSpecCollection?,
    subproject: Subproject,
    onSubprojectUpdate: () -> Unit,
    onProjectApiSpecUpdate: () -> Unit,
) {
    val colours = LocalColor.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        var selectedGrpcApiSpecId by remember { mutableStateOf<String?>(null) }
        val selectedGrpcApiSpec = grpcApiSpecs.firstOrNull { it.id == selectedGrpcApiSpecId }

        LazyColumn(
            modifier = Modifier
                .background(color = colours.backgroundInputField)
                .weight(0.3f)
                .defaultMinSize(minWidth = 160.dp)
                .fillMaxHeight()
        ) {
            items(items = grpcApiSpecs) {
                AppText(
                    text = it.name,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp).defaultMinSize(minHeight = 24.dp).fillMaxWidth().clickable {
                        selectedGrpcApiSpecId = it.id
                    }
                )
            }
        }
        Column(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
            selectedGrpcApiSpec?.let { selectedGrpcApiSpec ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AppText("Spec Name")
                    AppTextFieldWithPlaceholder(
                        key = "SubprojectEditorDialogView/${subproject.id}/gRPCAPISpec/${selectedGrpcApiSpec.id}",
                        value = selectedGrpcApiSpec.name,
                        onValueChange = { newName ->
                            // name should be unique within a Subproject
                            if (grpcApiSpecs.any { it.name == newName }) {
                                // TODO show some error hints?
                                return@AppTextFieldWithPlaceholder
                            }

                            projectApiSpecCollection!!.grpcApiSpecs.replaceIf(
                                selectedGrpcApiSpec.copy(name = newName)
                            ) { it.id == selectedGrpcApiSpec.id }
                            onProjectApiSpecUpdate()
                        },
                        placeholder = { AppText(text = "API Spec Name", color = colours.placeholder) },
                        modifier = Modifier.weight(1f),
                    )
                    AppDeleteButton(size = 24.dp) {
                        if (subproject.grpcApiSpecIds.remove(selectedGrpcApiSpec.id)) {
                            onSubprojectUpdate()
                        }
                        if (projectApiSpecCollection?.grpcApiSpecs?.removeIf { it.id == selectedGrpcApiSpec.id } == true) {
                            onProjectApiSpecUpdate()
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 4.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                    AppText(text = "Source: ${selectedGrpcApiSpec.source}", modifier = Modifier.padding(top = 12.dp))
                    AppText(
                        text = "Last Updated: ${
                            selectedGrpcApiSpec.updateTime.atZoneOffset(KZoneOffset.local())
                                .format("yyyy-MM-dd HH:mm:ss (Z)")
                        }", modifier = Modifier.padding(top = 12.dp)
                    )
                    AppText(text = "Methods:", modifier = Modifier.padding(top = 12.dp))
                    selectedGrpcApiSpec.methods.sortedBy { "${it.serviceFullName}/${it.methodName}" }.forEach {
                        AppText(
                            text = "${it.serviceFullName}/${it.methodName}",
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 0.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class SubprojectEditorTab(override val key: Any?, override val displayText: String) : DropDownable {
    Configuration(key = "Configuration", displayText = "Configuration"),
    GrpcApiSpec(key = "GrpcApiSpec", displayText = "gRPC API Spec")
}

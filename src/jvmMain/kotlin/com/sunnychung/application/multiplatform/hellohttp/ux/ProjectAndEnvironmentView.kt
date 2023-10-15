package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont

@Composable
fun ProjectAndEnvironmentViewV2(
    modifier: Modifier = Modifier,
    projects: List<Project>,
    onAddProject: (Project) -> Unit,
    onAddSubproject: (project: Project, newSubproject: Subproject) -> Unit,
    onSelectSubproject: (Subproject) -> Unit,
    environments: List<Environment>,
    onSelectEnvironment: (Environment) -> Unit
) {
    val colors = LocalColor.current

    var expandedSection by remember { mutableStateOf(ExpandedSection.Project) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedSubproject by remember { mutableStateOf<Subproject?>(null) }

    var showDialogType by remember { mutableStateOf(EditDialogType.None) }
    var dialogTextFieldValue by remember { mutableStateOf("") }

    MainWindowDialog(
        isEnabled = showDialogType != EditDialogType.None,
        onDismiss = { showDialogType = EditDialogType.None }) {
        val focusRequester = remember { FocusRequester() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppTextFieldWithPlaceholder(
                value = dialogTextFieldValue,
                onValueChange = { dialogTextFieldValue = it },
                placeholder = {
                    AppText(
                        text = if (showDialogType == EditDialogType.Project) "Project name" else "Subproject name",
                        color = colors.placeholder
                    )
                },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )
            AppTextButton(text = "Done", onClick = {
                when (showDialogType) {
                    EditDialogType.Project -> {
                        val project = Project(id = uuidString(), name = dialogTextFieldValue, subprojects = mutableListOf())
                        onAddProject(project)
                        if (selectedProject == null) {
                            selectedProject = project
                            expandedSection = ExpandedSection.Subproject
                        }
                    }
                    EditDialogType.Subproject -> {
                        val subproject = Subproject(id = uuidString(), name = dialogTextFieldValue, treeObjects = mutableListOf()) // TODO refactor to AppView
                        onAddSubproject(selectedProject!!, subproject)
                        onSelectSubproject(subproject)
                        selectedSubproject = subproject
                        expandedSection = ExpandedSection.None
                    }
                    EditDialogType.Environment -> TODO()
                    EditDialogType.None -> {}
                }
                showDialogType = EditDialogType.None
            })
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Project", modifier = Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { showDialogType = EditDialogType.Project; dialogTextFieldValue = "" })
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            if (expandedSection == ExpandedSection.Project) {
                if (projects.isEmpty()) {
                    AppText(
                        text = "Click to Create a Project",
                        fontSize = LocalFont.current.createLabelSize,
                        modifier = Modifier
                            .clickable { showDialogType = EditDialogType.Project; dialogTextFieldValue = "" }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = projects) { it ->
                            AppText(
                                text = it.name,
                                hasHoverHighlight = true,
                                modifier = Modifier
                                    .clickable { selectedProject = it; expandedSection = ExpandedSection.Subproject }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                DropDownView(
                    modifier = Modifier.fillMaxWidth(),
                    items = projects,
                    selectedItem = selectedProject,
                    isLabelFillMaxWidth = true,
                    onClickItem = { selectedProject = it; true })
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Subproject", modifier = Modifier.weight(1f))
            AppImageButton(
                resource = "add.svg",
                size = 24.dp,
                onClick = { showDialogType = EditDialogType.Subproject; dialogTextFieldValue = "" },
                enabled = selectedProject != null
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            if (expandedSection == ExpandedSection.Subproject) {
                val subprojects = selectedProject?.subprojects ?: emptyList()
                if (subprojects.isEmpty()) {
                    AppText(
                        text = "Click to Create a Subproject",
                        fontSize = LocalFont.current.createLabelSize,
                        modifier = Modifier
                            .clickable { showDialogType = EditDialogType.Subproject; dialogTextFieldValue = "" }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = subprojects) { it ->
                            AppText(
                                text = it.name,
                                hasHoverHighlight = true,
                                modifier = Modifier
                                    .clickable { onSelectSubproject(it); selectedSubproject = it; expandedSection = ExpandedSection.None }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropDownView(
                        modifier = Modifier.fillMaxWidth(),
                        selectedItem = selectedSubproject,
                        items = selectedProject?.subprojects ?: emptyList(),
                        isLabelFillMaxWidth = true,
                        onClickItem = { onSelectSubproject(it); selectedSubproject = it; true })
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Environment", modifier = Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { /* TODO */ })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DropDownView(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                items = environments,
                isLabelFillMaxWidth = true,
                onClickItem = { true })
        }
    }
}

private enum class ExpandedSection {
    Project, Subproject, None
}

private enum class EditDialogType {
    Project, Subproject, Environment, None
}

@Composable
@Preview
fun ProjectAndEnvironmentViewV2Preview() {
    ProjectAndEnvironmentViewV2(
        projects = listOf(Project(id = "p1", name = "Project A", mutableListOf(Subproject("a1", "Subproject A1", mutableListOf()), Subproject("a2", "Subproject A2", mutableListOf()))), Project(id = "p2", name = "Project B", mutableListOf()), Project(id = "p3", name = "Project C", mutableListOf())),
        environments = listOf(Environment(name = "Environment A"), Environment(name = "Environment B"), Environment(name = "Environment C")),
        onSelectEnvironment = {},
        onSelectSubproject = {},
        onAddProject = {},
        onAddSubproject = {_, _ ->},
    )
}

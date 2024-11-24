package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    selectedProject: Project?,
    selectedSubproject: Subproject?,
    selectedEnvironment: Environment?,
    onAddProject: (Project) -> Unit,
    onAddSubproject: (project: Project, newSubproject: Subproject) -> Unit,
    onSelectProject: (Project?) -> Unit,
    onSelectSubproject: (Subproject?) -> Unit,
//    environments: List<Environment>,
    onSelectEnvironment: (Environment?) -> Unit,
    onUpdateProject: (Project) -> Unit,
    onUpdateSubproject: (Subproject) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onDeleteSubproject: (Subproject) -> Unit,
) {
    val colors = LocalColor.current

    var expandedSection by remember { mutableStateOf(ExpandedSection.Project) }

    var showDialogType by remember { mutableStateOf(EditDialogType.None) }
    var dialogTextFieldValue by remember { mutableStateOf("") }
    var dialogIsCreate by remember { mutableStateOf<Boolean>(true) }

    log.v { "P&E selectedProject = ${selectedProject?.id}" }

    if (selectedProject == null && projects.size == 1) {
        // invoking `onSelectProject()` directly doesn't trigger recomposition
        // what is worse, manually re-assigning the value would not trigger recomposition as well
        LaunchedEffect(projects.first().id) {
            onSelectProject(projects.first())
        }
        expandedSection = ExpandedSection.Subproject
    }
    if (selectedSubproject == null && selectedProject != null && selectedProject!!.subprojects.size == 1) {
        // but this works
        onSelectSubproject(selectedProject!!.subprojects.first())
        expandedSection = ExpandedSection.None
    }

    MainWindowDialog(
        key = "ProjectNameAndSubprojectName",
        isEnabled = showDialogType in setOf(EditDialogType.Project, EditDialogType.CreateSubproject),
        onDismiss = { showDialogType = EditDialogType.None }) {
        val focusRequester = remember { FocusRequester() }

        fun onDone() {
            when (showDialogType) {
                EditDialogType.Project -> {
                    if (dialogIsCreate) {
                        val project = Project(id = uuidString(), name = dialogTextFieldValue, subprojects = mutableListOf())
                        onAddProject(project)
                        onSelectSubproject(null)
                        onSelectEnvironment(null)
                        if (selectedProject == null) {
                            expandedSection = ExpandedSection.Subproject
                        }
                        onSelectProject(project)
                    } else {
                        val updated = selectedProject!!.copy(name = dialogTextFieldValue)
                        onUpdateProject(updated)
                        onSelectProject(updated)
                    }
                }
                EditDialogType.CreateSubproject -> {
                    if (dialogIsCreate) {
                        val subproject = Subproject(
                            id = uuidString(),
                            name = dialogTextFieldValue,
                            treeObjects = mutableListOf(),
                            environments = mutableListOf()
                        ) // TODO refactor to AppView
                        onAddSubproject(selectedProject!!, subproject)
                        onSelectSubproject(subproject)
                        onSelectEnvironment(null)
//                        selectedSubproject = subproject
                        expandedSection = ExpandedSection.None
                    } else {
                        val updated = selectedSubproject!!.copy(name = dialogTextFieldValue)
                        onUpdateSubproject(updated)
                    }
                }
                else -> {}
            }
            showDialogType = EditDialogType.None
        }

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
                    .onPreviewKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                            onDone()
                            true
                        } else {
                            false
                        }
                    }
                    .defaultMinSize(minWidth = 200.dp)
                    .testTag(TestTag.ProjectNameAndSubprojectNameDialogTextField.name),
            )
            AppTextButton(
                text = "Done",
                onClick = { onDone() },
                modifier = Modifier.padding(top = 4.dp)
                    .testTag(TestTag.ProjectNameAndSubprojectNameDialogDoneButton.name),
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    MainWindowDialog(
        key = "EditSubproject",
        isEnabled = showDialogType in setOf(EditDialogType.EditSubproject),
        onDismiss = { showDialogType = EditDialogType.None }
    ) {
        SubprojectEditorDialogView(
            projectId = selectedProject!!.id,
            subprojectId = selectedSubproject!!.id,
            modifier = Modifier.padding(12.dp).fillMaxSize(),
        )
    }

    MainWindowDialog(
        key = "Environment",
        isEnabled = showDialogType in setOf(EditDialogType.Environment),
        onDismiss = { showDialogType = EditDialogType.None }
    ) {
        SubprojectEnvironmentsEditorDialogView(
            subproject = selectedSubproject!!,
            onSubprojectUpdate = { onUpdateSubproject(it) },
            initialEnvironment = selectedEnvironment,
            modifier = Modifier.padding(12.dp).fillMaxSize(),
        )
    }


    Column(modifier = modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Project", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = {
                showDialogType = EditDialogType.Project
                dialogTextFieldValue = ""
                dialogIsCreate = true
            })
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            if (expandedSection == ExpandedSection.Project) {
                if (projects.isEmpty()) {
                    AppText(
                        text = "Click to Create a Project",
                        fontSize = LocalFont.current.createLabelSize,
                        modifier = Modifier
                            .clickable {
                                showDialogType = EditDialogType.Project
                                dialogTextFieldValue = ""
                                dialogIsCreate = true
                            }
                            .testTag(TestTag.FirstTimeCreateProjectButton.name)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(items = projects) { it ->
                            AppText(
                                text = it.name,
                                hasHoverHighlight = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProject(it); expandedSection = ExpandedSection.Subproject }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropDownView(
                        modifier = Modifier.weight(1f),
                        items = projects,
                        selectedItem = selectedProject,
                        isLabelFillMaxWidth = true,
                        onClickItem = {
                            onSelectProject(it)
                            onSelectSubproject(null)
                            onSelectEnvironment(null)
                            true
                        }
                    )
                    AppImageButton(
                        resource = "edit.svg",
                        size = 16.dp,
                        onClick = {
                            showDialogType = EditDialogType.Project
                            dialogTextFieldValue = selectedProject!!.name
                            dialogIsCreate = false
                        }
                    )
                    AppDeleteButton {
                        onDeleteProject(selectedProject!!)
                        val anotherProject = projects.firstOrNull { it.id != selectedProject!!.id }
                        if (anotherProject != null) {
                            onSelectProject(anotherProject)
                        } else {
                            onSelectProject(null)
                            expandedSection = ExpandedSection.Project
                        }
                        onSelectSubproject(null)
                        onSelectEnvironment(null)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (selectedProject != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = "Subproject", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AppImageButton(
                    resource = "add.svg",
                    size = 24.dp,
                    onClick = {
                        showDialogType = EditDialogType.CreateSubproject
                        dialogTextFieldValue = ""
                        dialogIsCreate = true
                    },
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
                                .clickable {
                                    showDialogType = EditDialogType.CreateSubproject
                                    dialogTextFieldValue = ""
                                    dialogIsCreate = true
                                }
                                .testTag(TestTag.FirstTimeCreateSubprojectButton.name)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(items = subprojects) { it ->
                                AppText(
                                    text = it.name,
                                    hasHoverHighlight = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectSubproject(it); /*selectedSubproject = it;*/
                                            onSelectEnvironment(null)
                                            expandedSection = ExpandedSection.None
                                        }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DropDownView(
                            modifier = Modifier.weight(1f),
                            selectedItem = selectedSubproject,
                            items = selectedProject?.subprojects ?: emptyList(),
                            isLabelFillMaxWidth = true,
                            onClickItem = {
                                onSelectSubproject(it); /*selectedSubproject = it;*/
                                onSelectEnvironment(null)
                                true
                            }
                        )
                        AppImageButton(
                            resource = "edit.svg",
                            size = 16.dp,
                            onClick = {
                                selectedSubproject ?: return@AppImageButton

                                showDialogType = EditDialogType.EditSubproject
                                dialogTextFieldValue = selectedSubproject!!.name
                                dialogIsCreate = false
                            },
                        )
                        AppDeleteButton {
                            selectedSubproject ?: return@AppDeleteButton

                            onDeleteSubproject(selectedSubproject!!)
                            val anotherSubproject =
                                selectedProject!!.subprojects.firstOrNull { it.id != selectedSubproject.id }
                            if (anotherSubproject != null) {
                                onSelectSubproject(anotherSubproject)
                            } else {
                                onSelectSubproject(null)
                                expandedSection = ExpandedSection.Subproject
                            }
                            onSelectEnvironment(null)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (selectedSubproject != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = "Environment", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AppImageButton(
                    resource = "edit.svg",
                    size = 20.dp,
                    onClick = { showDialogType = EditDialogType.Environment },
                    modifier = Modifier.testTag(TestTag.EditEnvironmentsButton.name)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropDownView(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    selectedItem = selectedEnvironment,
                    items = selectedSubproject.environments,
                    isLabelFillMaxWidth = true,
                    onClickItem = { onSelectEnvironment(it); true },
                    testTagParts = arrayOf(TestTagPart.EnvironmentDropdown),
                )
            }
        }
    }
}

private enum class ExpandedSection {
    Project, Subproject, None
}

private enum class EditDialogType {
    Project, CreateSubproject, EditSubproject, Environment, None
}

@Composable
@Preview
fun ProjectAndEnvironmentViewV2Preview() {
    ProjectAndEnvironmentViewV2(
        projects = listOf(Project(id = "p1", name = "Project A", mutableListOf(Subproject("a1", "Subproject A1", mutableListOf(), mutableListOf()), Subproject("a2", "Subproject A2", mutableListOf(), mutableListOf()))), Project(id = "p2", name = "Project B", mutableListOf()), Project(id = "p3", name = "Project C", mutableListOf())),
        selectedProject = null,
        selectedSubproject = null,
        selectedEnvironment = null,
//        environments = listOf(Environment(name = "Environment A", id = "A", variables = emptyList()), Environment(name = "Environment B", id = "B", variables = emptyList()), Environment(name = "Environment C", id = "C", variables = emptyList())),
        onSelectEnvironment = {},
        onSelectProject = {},
        onSelectSubproject = {},
        onAddProject = {},
        onAddSubproject = {_, _ ->},
        onUpdateSubproject = { _->},
        onUpdateProject = {_ ->},
        onDeleteProject = {_ ->},
        onDeleteSubproject = {_ ->},
    )
}

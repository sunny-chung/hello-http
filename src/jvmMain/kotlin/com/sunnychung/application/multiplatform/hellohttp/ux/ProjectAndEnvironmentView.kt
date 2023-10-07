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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont

@Composable
fun ProjectAndEnvironmentView(projects: List<Project>, environments: List<Environment>) {

    Column(modifier = Modifier/*.width(IntrinsicSize.Max)*/.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Project")
            DropDownView(modifier = Modifier.fillMaxWidth(), items = projects, hasSpacer = true, onClickItem = { true })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Environment")
            DropDownView(modifier = Modifier.fillMaxWidth(), items = environments, hasSpacer = true, onClickItem = { true })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProjectAndEnvironmentViewV2(modifier: Modifier = Modifier, projects: List<Project>, onSelectSubproject: (Subproject) -> Unit, environments: List<Environment>, onSelectEnvironment: (Environment) -> Unit) {
    val colors = LocalColor.current

    var expandedSection by remember { mutableStateOf(ExpandedSection.Project) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Project")
            Spacer(Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { /* TODO */ })
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            if (expandedSection == ExpandedSection.Project) {
                if (projects.isEmpty()) {
                    AppText(
                        text = "Click to Create a Project",
                        fontSize = LocalFont.current.createLabelSize,
                        modifier = Modifier
                            .clickable { /* TODO */ }
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
                    hasSpacer = true,
                    onClickItem = { selectedProject = it; true })
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Subproject")
            Spacer(Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { /* TODO */ })
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            if (expandedSection == ExpandedSection.Subproject) {
                val subprojects = selectedProject?.subprojects ?: emptyList()
                if (subprojects.isEmpty()) {
                    AppText(
                        text = "Click to Create a Subproject",
                        fontSize = LocalFont.current.createLabelSize,
                        modifier = Modifier
                            .clickable { /* TODO */ }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = subprojects) { it ->
                            AppText(
                                text = it.name,
                                hasHoverHighlight = true,
                                modifier = Modifier
                                    .clickable { onSelectSubproject(it); expandedSection = ExpandedSection.None }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropDownView(
                        modifier = Modifier.fillMaxWidth(),
                        items = selectedProject?.subprojects ?: emptyList(),
                        hasSpacer = true,
                        onClickItem = { onSelectSubproject(it); true })
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Environment")
            Spacer(Modifier.weight(1f))
            AppImageButton(resource = "add.svg", size = 24.dp, onClick = { /* TODO */ })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DropDownView(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                items = environments,
                hasSpacer = true,
                onClickItem = { true })
        }
    }
}

private enum class ExpandedSection {
    Project, Subproject, None
}

@Composable
@Preview
fun ProjectAndEnvironmentViewPreview() {
    ProjectAndEnvironmentView(
        projects = listOf(Project(id = "p1", name = "Project A", listOf(Subproject("a1", "Subproject A1", listOf()), Subproject("a2", "Subproject A2", listOf()))), Project(id = "p2", name = "Project B", listOf()), Project(id = "p3", name = "Project C", listOf())),
        environments = listOf(Environment(name = "Environment A"), Environment(name = "Environment B"), Environment(name = "Environment C")),
    )
}

@Composable
@Preview
fun ProjectAndEnvironmentViewV2Preview() {
    ProjectAndEnvironmentViewV2(
        projects = listOf(Project(id = "p1", name = "Project A", listOf(Subproject("a1", "Subproject A1", listOf()), Subproject("a2", "Subproject A2", listOf()))), Project(id = "p2", name = "Project B", listOf()), Project(id = "p3", name = "Project C", listOf())),
        environments = listOf(Environment(name = "Environment A"), Environment(name = "Environment B"), Environment(name = "Environment C")),
        onSelectEnvironment = {},
        onSelectSubproject = {},
    )
}

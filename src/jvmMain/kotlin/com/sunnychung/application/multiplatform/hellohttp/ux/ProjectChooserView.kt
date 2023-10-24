package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectChooserView(modifier: Modifier = Modifier, selectedProjectIds: Set<String>, onUpdateSelection: (Set<String>) -> Unit) {
    val projectCollectionRepository = AppContext.ProjectCollectionRepository
    val colours = LocalColor.current
    val updates by projectCollectionRepository.subscribeUpdates().collectAsState(null)
    val projects = runBlocking {
        projectCollectionRepository.read(ProjectAndEnvironmentsDI())!!.projects
    }
    val allProjectIds = projects.map { it.id }.toSet()
    val haveAllProjectsSelected = selectedProjectIds.containsAll(allProjectIds)

    LazyColumn(modifier = modifier.background(colours.backgroundInputField)) {
        stickyHeader {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(colours.backgroundInputField)) {
                    AppCheckbox(
                        checked = haveAllProjectsSelected,
                        onCheckedChange = {
                            onUpdateSelection(
                                if (it) {
                                    allProjectIds
                                } else {
                                    emptySet()
                                }
                            )
                        },
                        size = 24.dp,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                    )
                    AppText(text = "Project")
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = colours.line))
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
        items(items = projects) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppCheckbox(
                    checked = selectedProjectIds.contains(it.id),
                    onCheckedChange = { isChecked ->
                        onUpdateSelection(
                            if (isChecked) {
                                selectedProjectIds + it.id
                            } else {
                                selectedProjectIds - it.id
                            }
                        )
                    },
                    size = 24.dp,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                )
                AppText(text = it.name, modifier = Modifier.weight(1f).padding(end = 4.dp))
            }
        }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.importer.InsomniaV4Importer
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import kotlinx.coroutines.runBlocking
import java.io.File

@Composable
fun SettingDialogView(closeDialog: () -> Unit) {
    val tabs = listOf("Data")
    var selectedTabIndex by remember { mutableStateOf(0) }
    Column(modifier = Modifier.size(width = 480.dp, height = 300.dp)) {
        TabsView(
            selectedIndex = selectedTabIndex,
            contents = tabs.map { { AppText(text = it) } },
            onSelectTab = { selectedTabIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.padding(8.dp)) {
            when (tabs[selectedTabIndex]) {
                "Data" -> {
                    DataTab(closeDialog = closeDialog)
                }
            }
        }
    }
}

private val COLUMN_HEADER_WIDTH = 160.dp

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalColor.current
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = colors.placeholder,
                modifier = Modifier.height(1.dp).padding(horizontal = 4.dp).offset(y = 1.dp).width(20.dp)
            ) {}
            AppText(text = title)
            Surface(
                color = colors.placeholder,
                modifier = Modifier.height(1.dp).padding(horizontal = 4.dp).offset(y = 1.dp).weight(1f)
            ) {}
        }
        Column(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}

@Composable
private fun DataTab(modifier: Modifier = Modifier, closeDialog: () -> Unit) {
    val colors = LocalColor.current
    val formats = listOf("Insomnia v4 JSON")

    var isShowFileDialog by remember { mutableStateOf(false) }
    var file by remember { mutableStateOf<File?>(null) }
    var fileFormat by remember { mutableStateOf(formats.first()) }

    if (isShowFileDialog) {
        FileDialog {
            println("File Dialog result = $it")
            file = it.firstOrNull()
            isShowFileDialog = false
        }
    }

    Column {
        Section("Import Projects") {
            var projectName by remember { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "File", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                    AppTextButton(
                        text = file?.name ?: "Choose a File",
                        onClick = { isShowFileDialog = true },
                        modifier = Modifier.border(width = 1.dp, color = colors.placeholder)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "Choose a Format", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                    DropDownView(
                        selectedItem = DropDownValue(fileFormat),
                        items = formats.map { DropDownValue(it) },
                        onClickItem = { fileFormat = it.displayText; true },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "New Project Name", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                    AppTextFieldWithPlaceholder(
                        value = projectName,
                        onValueChange = { projectName = it },
                        placeholder = { AppText(text = "New Project Name", color = colors.placeholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            AppTextButton(
                text = "Import",
                onClick = {
                    runBlocking { // TODO change to suspend in background
                        // FIXME error handling
                        InsomniaV4Importer().importAsProject(file = file!!, projectName = projectName)
                    }
                    closeDialog()
                }
            )
        }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.exporter.DataDumpExporter
import com.sunnychung.application.multiplatform.hellohttp.exporter.InsomniaV4Exporter
import com.sunnychung.application.multiplatform.hellohttp.extension.`if`
import com.sunnychung.application.multiplatform.hellohttp.importer.DataDumpImporter
import com.sunnychung.application.multiplatform.hellohttp.importer.InsomniaV4Importer
import com.sunnychung.application.multiplatform.hellohttp.importer.PostmanV2JsonImporter
import com.sunnychung.application.multiplatform.hellohttp.importer.PostmanV2ZipImporter
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import kotlinx.coroutines.runBlocking
import java.io.File

@Composable
fun SettingDialogView(closeDialog: () -> Unit) {
    val colors = LocalColor.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    Column(modifier = Modifier.size(width = 480.dp, height = 300.dp)) {
        TabsView(
            selectedIndex = selectedTabIndex,
            contents = SettingTab.values().map { { AppTabLabel(text = it.name) } },
            onSelectTab = { selectedTabIndex = it },
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
        )

        Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
            when (SettingTab.values()[selectedTabIndex]) {
                SettingTab.Data -> {
                    DataTab(closeDialog = closeDialog)
                }
                SettingTab.Appearance -> {
                    AppearanceTab()
                }
            }
        }
    }
}

private enum class SettingTab {
    Data, Appearance
}

private val COLUMN_HEADER_WIDTH = 140.dp

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

enum class ImportFormat {
    `Hello HTTP Data Dump`, `Insomnia v4 JSON`, `Postman v2 ZIP Data Dump`, `Postman v2 JSON Single Collection`
}

enum class ExportFormat {
    `Hello HTTP Data Dump`, `Insomnia v4 JSON (One File per Project)`
}

@Composable
private fun DataTab(modifier: Modifier = Modifier, closeDialog: () -> Unit) {
    val colors = LocalColor.current

    Column {
        Section("Import Projects") {
            var importFileFormat by remember { mutableStateOf(ImportFormat.values().first()) }
            var projectName by remember { mutableStateOf("") }

            var isShowFileDialog by remember { mutableStateOf(false) }
            var file by remember { mutableStateOf<File?>(null) }
            if (isShowFileDialog) {
                FileDialog {
                    println("File Dialog result = $it")
                    file = it.firstOrNull()
                    isShowFileDialog = false
                }
            }

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
                        selectedItem = DropDownValue(importFileFormat.name),
                        items = ImportFormat.values().map { DropDownValue(it.name) },
                        onClickItem = { importFileFormat = ImportFormat.valueOf(it.displayText); true },
                    )
                }
                if (importFileFormat in setOf(ImportFormat.`Insomnia v4 JSON`, ImportFormat.`Postman v2 JSON Single Collection`)) {
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
            }
            AppTextButton(
                text = "Import",
                onClick = {
                    runBlocking { // TODO change to suspend in background
                        // FIXME error handling
                        when (importFileFormat) {
                            ImportFormat.`Hello HTTP Data Dump` -> {
                                DataDumpImporter().importAsProjects(file!!)
                            }
                            ImportFormat.`Insomnia v4 JSON` -> {
                                InsomniaV4Importer().importAsProject(file = file!!, projectName = projectName)
                            }
                            ImportFormat.`Postman v2 ZIP Data Dump` -> {
                                PostmanV2ZipImporter().importAsProjects(file!!)
                            }
                            ImportFormat.`Postman v2 JSON Single Collection` -> {
                                PostmanV2JsonImporter().importAsProject(file = file!!, projectName = projectName)
                            }
                        }
                    }
                    closeDialog()
                }
            )
        }

        Section("Export Projects") {
            val selectedProjectIds = remember { mutableStateListOf<String>() }
            var exportFileFormat by remember { mutableStateOf(ExportFormat.values().first()) }
            var isShowDirectoryPicker by remember { mutableStateOf(false) }
            var isShowFileDialog by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(text = "Choose project(s) to export")
                ProjectChooserView(
                    selectedProjectIds = selectedProjectIds.toSet(),
                    onUpdateSelection = {
                        selectedProjectIds.clear()
                        selectedProjectIds.addAll(it.toList())
                    },
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(start = 8.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "Choose a Format", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                    DropDownView(
                        selectedItem = DropDownValue(exportFileFormat.name),
                        items = ExportFormat.values().map { DropDownValue(it.name) },
                        onClickItem = { exportFileFormat = ExportFormat.valueOf(it.displayText); true },
                    )
                }
                AppTextButton(
                    text = "Export",
                    onClick = {
                        when (exportFileFormat) {
                            ExportFormat.`Hello HTTP Data Dump` -> isShowFileDialog = true
                            ExportFormat.`Insomnia v4 JSON (One File per Project)` -> isShowDirectoryPicker = true
                        }
                    }
                )
            }

            DirectoryPicker(isShowDirectoryPicker) { dir ->
                log.d { "Chosen dir $dir" }
                isShowDirectoryPicker = false

                dir?.let { File(dir) }?.`if` { it.isDirectory }
                    ?.takeIf { exportFileFormat == ExportFormat.`Insomnia v4 JSON (One File per Project)` }
                    ?.let { dirFile ->
                        // TODO suspend instead of runBlocking
                        runBlocking {
                            val exporter = InsomniaV4Exporter()
                            val dateTimeString = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
                            AppContext.ProjectCollectionRepository.read(ProjectAndEnvironmentsDI())!!
                                .projects
                                .filter { it.id in selectedProjectIds }
                                .forEach {
                                    val exportFile = File(
                                        dirFile,
                                        "${it.name.replace("[\\s\\p{Punct}]".toRegex(), "-")}_$dateTimeString.json"
                                    )
                                    exporter.exportToFile(it, exportFile)
                                }
                        }
                        closeDialog()
                    }
            }
            if (isShowFileDialog) {
                val dateTimeString = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
                FileDialog(mode = java.awt.FileDialog.SAVE, filename = "HelloHTTP_dump_$dateTimeString.dump") {
                    isShowFileDialog = false
                    val file = it.firstOrNull()

                    file?.takeIf { exportFileFormat == ExportFormat.`Hello HTTP Data Dump` }?.let { file ->
                        // TODO suspend instead of runBlocking
                        runBlocking {
                            val selectedProjects =
                                AppContext.ProjectCollectionRepository.read(ProjectAndEnvironmentsDI())!!
                                    .projects
                                    .filter { it.id in selectedProjectIds }

                            DataDumpExporter().exportToFile(selectedProjects, file)
                        }
                        closeDialog()
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceTab() {
    val currentColourTheme by AppContext.UserPreferenceViewModel.colourTheme.collectAsState()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Colour Theme", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
            DropDownView(
                selectedItem = DropDownValue(currentColourTheme.name),
                items = ColourTheme.values().map { DropDownValue(it.name) },
                onClickItem = {
                    val newColourTheme = ColourTheme.valueOf(it.displayText)
                    AppContext.UserPreferenceViewModel.setColorTheme(newColourTheme)

                    runBlocking {
                        val userPreferenceRepository = AppContext.UserPreferenceRepository
                        val userPreference = userPreferenceRepository.read(UserPreferenceDI())!!.preference
                        userPreference.colourTheme = newColourTheme
                        userPreferenceRepository.notifyUpdated(UserPreferenceDI())
                    }

                    true
                },
            )
        }
    }
}

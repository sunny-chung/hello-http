package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.error.ApplicationException
import com.sunnychung.application.multiplatform.hellohttp.exporter.DataDumpExporter
import com.sunnychung.application.multiplatform.hellohttp.exporter.InsomniaV4Exporter
import com.sunnychung.application.multiplatform.hellohttp.exporter.PostmanV2MultiFileExporter
import com.sunnychung.application.multiplatform.hellohttp.extension.`if`
import com.sunnychung.application.multiplatform.hellohttp.importer.DataDumpImporter
import com.sunnychung.application.multiplatform.hellohttp.importer.InsomniaV4Importer
import com.sunnychung.application.multiplatform.hellohttp.importer.PostmanV2JsonImporter
import com.sunnychung.application.multiplatform.hellohttp.importer.PostmanV2ZipImporter
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_BACKUP_RETENTION_DAYS
import com.sunnychung.application.multiplatform.hellohttp.model.RenderingApi
import com.sunnychung.application.multiplatform.hellohttp.model.getApplicableRenderingApiList
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.io.File
import java.io.IOException

@Composable
fun SettingDialogView(closeDialog: () -> Unit) {
    val colors = LocalColor.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
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

private val COLUMN_HEADER_WIDTH = 170.dp

@Composable
private fun Section(title: CharSequence, content: @Composable ColumnScope.() -> Unit) {
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
    `Hello HTTP Data Dump`, `Insomnia v4 JSON (One File per Project)`, `Postman v2 Data Dump (One File per Project or Env)`
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
            val fileDialogState = rememberFileDialogState()
            if (isShowFileDialog) {
                FileDialog(state = fileDialogState) {
                    println("File Dialog result = $it")
                    if (it != null) {
                        file = it.firstOrNull()
                    }
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
                            key = "SettingDialog/DataTab/NewpProjectName",
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
                    try {
                        runBlocking { // TODO change to suspend in background
                            // FIXME error handling
                            val file = file ?: throw ApplicationException("Please select a file to continue.")
                            if (!file.isFile) throw ApplicationException("The selected file is not a regular file.")
                            if (!file.canRead()) throw ApplicationException("The selected file is not readable.")
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
                    } catch (e: Throwable) {
                        val errorMessage = if (e is ApplicationException) {
                            e.message!!
                        } else if (e is IOException) {
                            "Fail to import due to an I/O error.\n\nDetail: ${e.message ?: "-"}"
                        } else {
                            "Fail to import. The file is not in a supported format.\n\nDetail: ${e.message ?: "-"}"
                        }
                        AppContext.ErrorMessagePromptViewModel.showErrorMessage(errorMessage)
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Section("Export Projects") {
            val selectedProjectIds = remember { mutableStateListOf<String>() }
            var exportFileFormat by remember { mutableStateOf(ExportFormat.values().first()) }
            var isShowDirectoryPicker by remember { mutableStateOf(false) }
            var isShowFileDialog by remember { mutableStateOf(false) }
            val fileDialogState = rememberFileDialogState()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(text = "Choose project(s) to export")
                ProjectChooserView(
                    selectedProjectIds = selectedProjectIds.toSet(),
                    onUpdateSelection = {
                        selectedProjectIds.clear()
                        selectedProjectIds.addAll(it.toList())
                    },
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(start = 8.dp, end = 80.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "Choose a Format", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                    DropDownView(
                        selectedItem = DropDownValue(exportFileFormat.name),
                        items = ExportFormat.values().map { DropDownValue(it.name) },
                        onClickItem = { exportFileFormat = ExportFormat.valueOf(it.displayText); true },
                    )
                }
                if (exportFileFormat == ExportFormat.`Postman v2 Data Dump (One File per Project or Env)`) {
                    AppText(
                        text = "WARNING: Postman can only import plain HTTP and GraphQL APIs, i.e. WebSockets, GraphQL subscriptions and gRPC are not supported.",
                        color = colors.highlight
                    )
                }
                AppTextButton(
                    text = "Export",
                    onClick = {
                        when (exportFileFormat) {
                            ExportFormat.`Hello HTTP Data Dump` -> isShowFileDialog = true
                            ExportFormat.`Insomnia v4 JSON (One File per Project)` -> isShowDirectoryPicker = true
                            ExportFormat.`Postman v2 Data Dump (One File per Project or Env)` -> isShowDirectoryPicker = true
                        }
                    }
                )
            }

            DirectoryPicker(isShowDirectoryPicker) { dir ->
                log.d { "Chosen dir $dir" }
                isShowDirectoryPicker = false

                dir?.let { File(dir) }?.`if` { it.isDirectory }
                    ?.let { dirFile ->
                        // TODO suspend instead of runBlocking
                        val hasDoneSomthing = when (exportFileFormat) {
                            ExportFormat.`Insomnia v4 JSON (One File per Project)` -> runBlocking {
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
                                true
                            }

                            ExportFormat.`Postman v2 Data Dump (One File per Project or Env)` -> runBlocking {
                                val exporter = PostmanV2MultiFileExporter()
                                val projects = AppContext.ProjectCollectionRepository.read(ProjectAndEnvironmentsDI())!!
                                    .projects
                                    .filter { it.id in selectedProjectIds }

                                exporter.exportToFile(projects, dirFile)

                                true
                            }

                            else -> false
                        }

                        if (hasDoneSomthing) {
                            closeDialog()
                        }
                    }
            }
            if (isShowFileDialog) {
                val dateTimeString = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
                FileDialog(state = fileDialogState, mode = java.awt.FileDialog.SAVE, filename = "Hello-HTTP_dump_$dateTimeString.dump") {
                    isShowFileDialog = false
                    val file = it?.firstOrNull()

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

        Section("Automatic Backup") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val userPreferenceRepository = AppContext.UserPreferenceRepository
                userPreferenceRepository.subscribeUpdates().collectAsState(null).value
                val userPreference = runBlocking { // TODO don't use runBlocking
                    userPreferenceRepository.read(UserPreferenceDI())!!.preference
                }
                var inputState by remember { mutableStateOf(userPreference.backupRetentionDays) }
                inputState = userPreference.backupRetentionDays

                AppText(text = "Keep backups for", modifier = Modifier.width(COLUMN_HEADER_WIDTH))
                AppTextField(
                    key = "Setting/Data/AutomaticBackup/KeepBackupsForHowManyDays",
                    value = (inputState ?: DEFAULT_BACKUP_RETENTION_DAYS).toString(),
                    onValueChange = {
                        val days = if (it.isEmpty()) {
                            null
                        } else {
                            it.toIntOrNull() ?: -1
                        }
                        if (days == null || days >= 0) {
                            userPreference.backupRetentionDays = days
                            userPreferenceRepository.notifyUpdated(UserPreferenceDI())
                            inputState = days
                        }
                    },
                )
                AppText(text = " days")
            }
            AppTextButton(
                text = "Open Backup Directory",
                onClick = {
                    Desktop.getDesktop().open(AppContext.AutoBackupManager.backupDir())
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun AppearanceTab() {
    val currentColourTheme by AppContext.UserPreferenceViewModel.colourTheme.collectAsState()

    val userPreferenceRepository = AppContext.UserPreferenceRepository
    userPreferenceRepository.subscribeUpdates().collectAsState(null).value
    val userPreference = runBlocking { // TODO don't use runBlocking
        userPreferenceRepository.read(UserPreferenceDI())!!.preference
    }

    Column {
        Section("Theme") {
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

        Spacer(modifier = Modifier.height(12.dp))

        Section(buildAnnotatedString {
            append("Experimental ")
            withStyle(SpanStyle(color = LocalColor.current.warning)) {
                append("(Warning: Changing may cause something VERY BAD!)")
            }
        }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(text = "Preferred Rendering (Requires restarting the app to change) (Current: ${AppContext.instance.renderingApi})", modifier = Modifier.width(COLUMN_HEADER_WIDTH).padding(6.dp))
                    DropDownView(
                        selectedItem = run {
                            val item = userPreference.preferredRenderingApi_Experimental ?: RenderingApi.Default
                            DropDownKeyValue(item, item.name)
                        },
                        items = getApplicableRenderingApiList(currentOS()).map {
                            DropDownKeyValue(it, it.name)
                        },
                        onClickItem = {
                            userPreference.preferredRenderingApi_Experimental = it.key.takeIf { it != RenderingApi.Default }
                            userPreferenceRepository.notifyUpdated(UserPreferenceDI())
                            true
                        },
                    )
                }
            }
        }
    }
}

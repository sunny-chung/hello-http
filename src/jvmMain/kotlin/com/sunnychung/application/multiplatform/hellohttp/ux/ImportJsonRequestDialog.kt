package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.SyntaxHighlight
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import java.io.File

private val JSON_IMPORT_PLACEHOLDER = """
    {"app":{"name":"Hello HTTP","version":"1.9.0"},"requests":[{"id":"...","name":"My Request","application":"Http","method":"GET","url":"https://example.com","examples":[{"id":"...","name":"Base","contentType":"None","headers":[],"cookies":[],"queryParameters":[],"body":null,"variables":[],"preFlight":{"executeCode":"","updateVariablesFromHeader":[],"updateVariablesFromQueryParameters":[],"updateVariablesFromBody":[],"updateVariablesFromGraphqlVariables":[]},"postFlight":{"updateVariablesFromHeader":[],"updateVariablesFromBody":[]},"overrides":null}],"payloadExamples":null}]}
""".trimIndent()

sealed interface ImportJsonRequestResult {
    data object Success : ImportJsonRequestResult
    data object Error : ImportJsonRequestResult
    data class RequireVersionConfirmation(
        val sourceVersion: String,
        val currentVersion: String,
    ) : ImportJsonRequestResult
}

sealed interface ImportJsonRequestInput {
    data class RawText(val json: String) : ImportJsonRequestInput
    data class JsonFile(val file: File) : ImportJsonRequestInput
}

@Composable
fun ImportJsonRequestDialog(
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onImportJson: (ImportJsonRequestInput, Boolean) -> ImportJsonRequestResult,
) {
    var jsonText by remember { mutableStateOf("") }
    var isShowFileDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var isShowVersionConfirmationDialog by remember { mutableStateOf(false) }
    var confirmationSourceVersion by remember { mutableStateOf("") }
    var confirmationCurrentVersion by remember { mutableStateOf("") }
    val fileDialogState = rememberFileDialogState()
    val isUsingSelectedFile = selectedFile != null

    fun dismiss() {
        jsonText = ""
        selectedFile = null
        isShowVersionConfirmationDialog = false
        confirmationSourceVersion = ""
        confirmationCurrentVersion = ""
        onDismiss()
    }

    fun currentInputOrNull(): ImportJsonRequestInput? {
        selectedFile?.let {
            return ImportJsonRequestInput.JsonFile(it)
        }
        if (jsonText.isBlank()) {
            return null
        }
        return ImportJsonRequestInput.RawText(jsonText)
    }

    fun import() {
        val currentInput = currentInputOrNull() ?: run {
            return
        }
        when (val result = onImportJson(currentInput, false)) {
            is ImportJsonRequestResult.Success -> dismiss()
            is ImportJsonRequestResult.Error -> Unit
            is ImportJsonRequestResult.RequireVersionConfirmation -> {
                confirmationSourceVersion = result.sourceVersion
                confirmationCurrentVersion = result.currentVersion
                isShowVersionConfirmationDialog = true
            }
        }
    }

    if (isShowFileDialog) {
        FileDialog(
            state = fileDialogState,
            title = "Choose a JSON file",
        ) {
            isShowFileDialog = false
            val file = it?.firstOrNull() ?: return@FileDialog
            selectedFile = file
        }
    }

    BinaryDialog(
        key = "ImportJsonRequestDataLossRiskDialog",
        isVisible = isShowVersionConfirmationDialog,
        content = "The source JSON was exported by app version $confirmationSourceVersion, " +
                "which is newer than your current app version $confirmationCurrentVersion. " +
                "Importing may lose some fields. Continue?",
        positiveButtonCaption = "Import Anyway",
        positiveButtonColor = LocalColor.current.warning,
        onClickPositiveButton = {
            val currentInput = currentInputOrNull() ?: run {
                return@BinaryDialog
            }
            when (onImportJson(currentInput, true)) {
                is ImportJsonRequestResult.Success -> dismiss()
                else -> Unit
            }
        },
        onDismiss = {
            isShowVersionConfirmationDialog = false
        },
    )

    MainWindowDialog(
        key = "ImportJsonRequestDialog",
        isEnabled = isEnabled,
        onDismiss = { dismiss() },
    ) {
        Column(modifier = Modifier.width(760.dp)) {
            AppText(text = "Import request(s) from JSON")
            CodeEditorView(
                cacheKey = "ImportJsonRequestDialog/Json",
                isReadOnly = isUsingSelectedFile,
                isEnabled = !isUsingSelectedFile,
                initialText = jsonText,
                onTextChange = { jsonText = it },
                syntaxHighlight = SyntaxHighlight.Json,
                isAutoFocusOnInit = true,
                placeholderText = JSON_IMPORT_PLACEHOLDER,
                testTag = TestTag.ImportJsonRequestDialogTextField.name,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(240.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    AppTextButton(
                        text = "Browse file",
                        onClick = { isShowFileDialog = true },
                        modifier = Modifier.testTag(TestTag.ImportJsonRequestDialogBrowseFileButton.name),
                    )
                    if (isUsingSelectedFile) {
                        AppText(
                            text = "Selected: ${selectedFile?.name.orEmpty()}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AppImageButton(
                            resource = "delete.svg",
                            size = 24.dp,
                        ) {
                            selectedFile = null
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextButton(
                        text = "Cancel",
                        onClick = { dismiss() },
                    )
                    AppTextButton(
                        text = "Import",
                        isEnabled = isUsingSelectedFile || jsonText.isNotBlank(),
                        onClick = { import() },
                        modifier = Modifier.testTag(TestTag.ImportJsonRequestDialogImportButton.name),
                    )
                }
            }
        }
    }
}

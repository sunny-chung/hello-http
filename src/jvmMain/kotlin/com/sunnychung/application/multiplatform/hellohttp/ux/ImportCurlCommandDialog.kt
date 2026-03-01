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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun ImportCurlCommandDialog(
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onImportCommand: (String) -> Boolean,
) {
    val colors = LocalColor.current
    var command by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun dismiss() {
        command = ""
        onDismiss()
    }

    fun import() {
        if (command.isBlank()) {
            return
        }
        val isSuccess = onImportCommand(command)
        if (isSuccess) {
            dismiss()
        }
    }

    MainWindowDialog(
        key = "ImportCurlCommandDialog",
        isEnabled = isEnabled,
        onDismiss = { dismiss() },
    ) {
        Column(modifier = Modifier.width(680.dp)) {
            AppText(text = "Import a request from a Linux / macOS curl command")
            AppTextFieldWithPlaceholder(
                key = "ImportCurlCommandDialog/Command",
                value = command,
                onValueChange = { command = it },
                placeholder = {
                    AppText(
                        text = "curl --request \"GET\" --url \"https://example.com\"",
                        color = colors.placeholder,
                    )
                },
                singleLine = false,
                onFinishInit = {
                    focusRequester.requestFocus()
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .focusRequester(focusRequester)
                    .testTag(TestTag.ImportCurlCommandDialogCommandTextField.name),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.End),
            ) {
                AppTextButton(
                    text = "Cancel",
                    onClick = { dismiss() },
                )
                AppTextButton(
                    text = "Import",
                    isEnabled = command.isNotBlank(),
                    onClick = { import() },
                    modifier = Modifier.testTag(TestTag.ImportCurlCommandDialogImportButton.name),
                )
            }
        }
    }
}

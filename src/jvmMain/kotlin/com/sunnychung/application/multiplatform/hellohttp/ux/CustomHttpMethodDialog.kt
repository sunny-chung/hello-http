package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.isValidHttpMethod
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun CustomHttpMethodDialog(isEnabled: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val colours = LocalColor.current
    val errorMessageVM = AppContext.ErrorMessagePromptViewModel
    var text by remember { mutableStateOf("") }

    fun onDone() {
        if (!text.isValidHttpMethod()) {
            errorMessageVM.showErrorMessage("Provided input is not a valid HTTP method.")
        } else {
            onConfirm(text)
        }
    }

    MainWindowDialog(
        key = "CustomHttpMethodDialog",
        isEnabled = isEnabled,
        onDismiss = onDismiss) {

        val focusRequester = remember { FocusRequester() }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppTextFieldWithPlaceholder(
                key = "CustomHttpMethodDialog/HttpMethod",
                value = text,
                onValueChange = {
                    if (it.isEmpty() || it.isValidHttpMethod()) {
                        text = it
                    }
                },
                placeholder = {
                    AppText(
                        text = "HTTP method name",
                        color = colours.placeholder
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
                    .defaultMinSize(minWidth = 200.dp),
            )
            AppTextButton(
                text = "Done",
                onClick = { onDone() },
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

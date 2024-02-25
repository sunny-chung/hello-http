package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestInput
import com.sunnychung.application.multiplatform.hellohttp.util.let
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds

private val FIRST_COLUMN_WIDTH = 120.dp

@Composable
fun LoadTestDialog(isEnabled: Boolean, onDismiss: () -> Unit, onConfirm: (LoadTestInput) -> Unit) {
    var numConcurrent by remember { mutableStateOf<Int?>(null) }
    var durationNumber by remember { mutableStateOf<Int?>(null) }
    var timeoutSeconds by remember { mutableStateOf<Int?>(null) }

    fun onDone() {
        let(numConcurrent, durationNumber, timeoutSeconds) { numConcurrent, durationNumber, timeoutSeconds ->
            onConfirm(
                LoadTestInput(
                    numConcurrent = numConcurrent,
                    intendedDuration = durationNumber.seconds(),
                    timeout = timeoutSeconds.seconds(),
                )
            )
        }
    }

    MainWindowDialog(
        key = "LoadTestDialog",
        isEnabled = isEnabled,
        onDismiss = onDismiss
    ) {
        val focusRequester = remember { FocusRequester() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        onDone()
                        true
                    } else {
                        false
                    }
                }
                .width(IntrinsicSize.Max)
        ) {
            AppText(
                text = "Load Test Parameters",
                fontSize = LocalFont.current.dialogTitleSize,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = "No. of concurrent users", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppTextFieldWithPlaceholder(
                    value = numConcurrent?.toString() ?: "",
                    onValueChange = {
                        if (it.isEmpty()) {
                            numConcurrent = null
                        } else {
                            it.toIntOrNull()?.let { numConcurrent = it }
                        }
                    },
                    placeholder = {
                        PlaceholderText("e.g. 100")
                    },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester)
                        .defaultMinSize(minWidth = 200.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = "Duration", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppTextFieldWithPlaceholder(
                    value = durationNumber?.toString() ?: "",
                    onValueChange = {
                        if (it.isEmpty()) {
                            durationNumber = null
                        } else {
                            it.toIntOrNull()?.let { durationNumber = it }
                        }
                    },
                    placeholder = {
                        PlaceholderText("e.g. 20")
                    },
                    singleLine = true,
                    modifier = Modifier.defaultMinSize(minWidth = 200.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText(text = "Timeout", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppTextFieldWithPlaceholder(
                    value = timeoutSeconds?.toString() ?: "",
                    onValueChange = {
                        if (it.isEmpty()) {
                            timeoutSeconds = null
                        } else {
                            it.toIntOrNull()?.let { timeoutSeconds = it }
                        }
                    },
                    placeholder = {
                        PlaceholderText("e.g. 3")
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                AppText("seconds")
            }

            AppTextButton(
                text = "Start",
                onClick = { onDone() },
                isEnabled = listOfNotNull(numConcurrent, durationNumber, timeoutSeconds).isNotEmpty(),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

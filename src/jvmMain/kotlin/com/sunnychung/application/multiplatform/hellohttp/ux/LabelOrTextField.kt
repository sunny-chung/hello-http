package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel

@Composable
fun LabelOrTextField(
    modifier: Modifier,
    isEditing: Boolean,
    value: String,
    onValueUpdate: (String) -> Unit,
    labelColor: Color = LocalColor.current.primary,
    editNameViewModel: EditNameViewModel,
    onFocus: () -> Unit,
    onUnfocus: () -> Unit,
) {
    if (!isEditing) {
        AppText(
            text = value.replace(' ', '\u00A0'), // disable breaking by words
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = labelColor,
            modifier = modifier
        )
    } else {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        var textFieldState by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(0, value.length))) }
        log.d { "RequestListView AppTextField recompose" }
        AppTextField(
            value = textFieldState,
            onValueChange = { v ->
                textFieldState = v
            },
            singleLine = true,
            modifier = modifier // modifier is immutable
                .focusRequester(focusRequester)
                .onFocusChanged { f ->
                    log.d { "RequestListView onFocusChanged $value ${f.hasFocus} ${f.isFocused}" }
                    if (f.hasFocus) {
                        onFocus()
                    } else {
                        onUnfocus()
                        if (editNameViewModel.isInvokeModelUpdate()) {
                            onValueUpdate(textFieldState.text)
                        }
                    }
                    editNameViewModel.onTextFieldFocusChange(f)
                }
                .onKeyEvent { e ->
                    when (e.key) {
                        Key.Enter -> {
                            log.d { "key enter" }
                            focusManager.clearFocus()
                        }
                        Key.Escape -> {
                            log.d { "key escape" }
                            editNameViewModel.onUserCancelEdit()
                            focusManager.clearFocus()
                        }
                        else -> {
                            return@onKeyEvent false
                        }
                    }
                    true
                }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditNameViewModel
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.createFromTinyString
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextViewState

@Composable
fun LabelOrTextField(
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    value: String,
    onValueUpdate: (String) -> Unit,
    labelColor: Color = LocalColor.current.primary,
    editNameViewModel: EditNameViewModel,
    onFocus: () -> Unit = {},
    onUnfocus: () -> Unit = {},
) {
    if (!isEditing) {
        AppText(
            text = value,
            isDisableWordWrap = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = labelColor,
            modifier = modifier
        )
    } else {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val textFieldState = remember(editNameViewModel.editingItemId.value) {
            BigTextFieldState(
                BigText.createFromTinyString(value),
                BigTextViewState()
            ).also {
                it.viewState.setSelection(0 ..< it.text.length)
            }
        }
        log.d { "RequestListView AppTextField recompose" }
        AppTextField(
            textState = textFieldState,
            onValueChange = {},
            singleLine = true,
            onFinishInit = {
                focusRequester.requestFocus()
            },
            modifier = modifier // modifier is immutable
                .focusRequester(focusRequester)
                .onFocusChanged { f ->
                    log.d { "RequestListView onFocusChanged $value ${f.hasFocus} ${f.isFocused}" }
                    if (f.hasFocus) {
                        onFocus()
                    } else {
                        onUnfocus()
                        if (editNameViewModel.isInvokeModelUpdate()) {
                            onValueUpdate(textFieldState.text.buildString())
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

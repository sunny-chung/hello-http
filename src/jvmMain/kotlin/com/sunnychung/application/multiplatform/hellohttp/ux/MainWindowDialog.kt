package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.DialogState

@Composable
fun MainWindowDialog(key: String, isEnabled: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val dialogViewModel = AppContext.DialogViewModel
    if (isEnabled) {
        dialogViewModel.updateState(
            DialogState(
                key = key,
                content = content,
                onDismiss = onDismiss,
            )
        )
    } else if (key == dialogViewModel.state.value?.key) {
        dialogViewModel.updateState(null)
    }
}

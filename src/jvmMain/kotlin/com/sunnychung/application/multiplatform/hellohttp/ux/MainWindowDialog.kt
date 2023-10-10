package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.DialogState

@Composable
fun MainWindowDialog(isEnabled: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AppContext.DialogViewModel.updateState(if (isEnabled) DialogState(content = content, onDismiss = onDismiss) else null)
}

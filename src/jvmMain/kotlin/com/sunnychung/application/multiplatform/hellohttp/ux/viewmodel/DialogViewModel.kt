package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DialogViewModel {
    private var stateFlow: MutableStateFlow<DialogState?> = MutableStateFlow(null)
    val state: StateFlow<DialogState?> = stateFlow

    fun updateState(state: DialogState?) {
        log.d { "DialogViewModel updateState $state" }
        if (state == null) {
            this.stateFlow.value?.onDismiss?.invoke()
        }
        this.stateFlow.value = state
    }
}

data class DialogState(val key: String, val content: @Composable () -> Unit, val onDismiss: () -> Unit)

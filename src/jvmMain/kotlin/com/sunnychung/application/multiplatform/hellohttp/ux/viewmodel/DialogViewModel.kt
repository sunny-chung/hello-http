package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DialogViewModel {
    private var stateFlow: MutableStateFlow<List<DialogState>> = MutableStateFlow(emptyList())
    val state: StateFlow<List<DialogState>> = stateFlow

//    fun updateState(state: DialogState?) {
//        log.d { "DialogViewModel updateState $state" }
//        if (state == null) {
//            this.stateFlow.value?.onDismiss?.invoke()
//        }
//        this.stateFlow.value = state
//    }

    fun showDialogIfNotExist(state: DialogState) {
        log.d { "DialogViewModel showDialogIfNotExist $state" }
        synchronized(this) {
            val all = this.stateFlow.value
            if (all.none { it.key == state.key }) {
                log.d { "DialogViewModel add $state" }
                this.stateFlow.value = all + state
            }
        }
    }

    fun removeDialog(key: String) {
        synchronized(this) {
            val all = this.stateFlow.value
            val afterRemove = all.filter { it.key != key }
            if (all.size != afterRemove.size) {
                all.filter { it.key == key }.forEach {
                    it.onDismiss()
                }
                this.stateFlow.value = afterRemove
            }
        }
    }

    fun popDialog() {
        synchronized(this) {
            val all = this.stateFlow.value
            if (all.isNotEmpty()) {
                all.last().onDismiss()
                this.stateFlow.value = all.copyWithRemovedIndex(all.lastIndex)
            }
        }
    }
}

data class DialogState(val key: String, val content: @Composable () -> Unit, val onDismiss: () -> Unit)

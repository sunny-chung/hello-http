package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class DialogViewModel {
    // TODO: refactor the boilerplate to MutableStateFlow
    var state: DialogState? = null
    private val stateUpdateChannel = Channel<DialogState?>()
    val stateUpdateFlow =
        stateUpdateChannel.receiveAsFlow()
            .distinctUntilChanged()
            .onEach { log.d { "DialogViewModel onEach" } }
            .shareIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly)

    fun updateState(state: DialogState?) {
        log.d { "DialogViewModel updateState $state" }
        if (state == null) {
            this.state?.onDismiss?.invoke()
        }
        this.state = state
        CoroutineScope(Dispatchers.Default).launch {
            stateUpdateChannel.send(state)
        }
    }
}

data class DialogState(val content: @Composable () -> Unit, val onDismiss: () -> Unit)

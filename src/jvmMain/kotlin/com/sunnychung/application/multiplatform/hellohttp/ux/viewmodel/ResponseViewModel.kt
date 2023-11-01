package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.yield

class ResponseViewModel {
    private val isEnabledState = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val timerFlow = isEnabledState.flatMapLatest { isEnabled ->
        if (!isEnabled) {
            return@flatMapLatest flow {}
        }
        flow {
            while (isEnabledState.value) {
                emit(KInstant.now())
                delay(100L)
                yield()
            }
        }
    }

    private val nowState = MutableStateFlow(KInstant.now())

    init {
        timerFlow.onEach { nowState.value = it }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    fun setEnabled(isEnabled: Boolean) {
        isEnabledState.value = isEnabled
    }

    @Composable
    fun subscribe(): State<KInstant> = nowState.collectAsState()
}

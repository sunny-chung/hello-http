package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val SHOW_ERROR_INITIAL_DURATION = 3.seconds()
private val SHOW_SUCCESS_INITIAL_DURATION = 1.seconds()

class ErrorMessagePromptViewModel {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State>
        get() = _state

    private var timerCoroutine: Job? = null

    fun isVisible(): Boolean {
        if (!state.value.isVisible) return false
        state.value.dismissInstant?.let {
            if (KInstant.now() > it) {
                _state.value = _state.value.copy(
                    isVisible = false
                )
                return false
            }
        }
        return true
    }

    fun showErrorMessage(message: String) {
        timerCoroutine?.cancel()
        _state.value = State(
            type = MessageType.Error,
            isVisible = message.isNotEmpty(),
            dismissInstant = KInstant.now() + SHOW_ERROR_INITIAL_DURATION,
            message = message
        )
        timerCoroutine = CoroutineScope(Dispatchers.Main).launch {
            delay(SHOW_ERROR_INITIAL_DURATION.toMilliseconds())
            dismiss()
        }
    }

    fun showSuccessMessage(message: String) {
        timerCoroutine?.cancel()
        _state.value = State(
            type = MessageType.Success,
            isVisible = message.isNotEmpty(),
            dismissInstant = KInstant.now() + SHOW_SUCCESS_INITIAL_DURATION,
            message = message
        )
        timerCoroutine = CoroutineScope(Dispatchers.Main).launch {
            delay(SHOW_SUCCESS_INITIAL_DURATION.toMilliseconds())
            dismiss()
        }
    }

    fun lockDismissTime() {
        timerCoroutine?.cancel()
        _state.value = _state.value.copy(
            dismissInstant = null
        )
    }
    fun unlockDismissTime(delayDuration: KDuration) {
        timerCoroutine?.cancel()
        val dismissInstant = maxOf(_state.value.dismissInstant ?: KInstant.now(), KInstant.now() + delayDuration)
        _state.value = _state.value.copy(
            dismissInstant = dismissInstant
        )
        timerCoroutine = CoroutineScope(Dispatchers.Main).launch {
            delay((dismissInstant - KInstant.now()).toMilliseconds())
            dismiss()
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(isVisible = false)
    }

    data class State(
        val type: MessageType = MessageType.Error,
        val isVisible: Boolean = false,
        val dismissInstant: KInstant? = null,
        val message: String = "",
    )

    enum class MessageType {
        Error, Success
    }
}

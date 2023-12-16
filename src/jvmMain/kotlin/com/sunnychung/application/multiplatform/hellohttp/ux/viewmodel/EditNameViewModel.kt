package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow

class EditNameViewModel {

    val isEditing = MutableStateFlow(false)
    val isCancelled = MutableStateFlow(false)
    val editingItemId = MutableStateFlow<String?>(null)

    @Deprecated("unused")
    val textFieldValue = MutableStateFlow(TextFieldValue(""))

    val hasReachedEditingState = MutableStateFlow(false)

    fun onStartEdit(id: String) {
        isEditing.value = true
        editingItemId.value = id
        hasReachedEditingState.value = false
//        textFieldValue.value = TextFieldValue(request.name, selection = TextRange(0, request.name.length))
        isCancelled.value = false
    }

    fun onTextFieldValueChange(newState: TextFieldValue) {
        textFieldValue.value = newState
    }

    fun onTextFieldFocusChange(state: FocusState) {
        if (state.hasFocus) {
            if (!hasReachedEditingState.value) {
                hasReachedEditingState.value = true
            }
        } else {
            if (hasReachedEditingState.value) {
                isEditing.value = false
                editingItemId.value = null
            }
        }
    }

    fun isInvokeModelUpdate(): Boolean {
        return hasReachedEditingState.value && !isCancelled.value
    }

    fun onUserCancelEdit() {
        isCancelled.value = true
        editingItemId.value = null
    }
}

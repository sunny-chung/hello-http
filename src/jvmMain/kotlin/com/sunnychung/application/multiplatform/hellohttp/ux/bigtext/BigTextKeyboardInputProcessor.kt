package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.input.key.KeyEvent

interface BigTextKeyboardInputProcessor {

    fun beforeProcessInput(keyEvent: KeyEvent, viewState: BigTextViewState, textManipulator: BigTextManipulator): Boolean = false

    fun afterProcessInput(keyEvent: KeyEvent, viewState: BigTextViewState, textManipulator: BigTextManipulator): Boolean = false
}

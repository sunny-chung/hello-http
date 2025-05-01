package com.sunnychung.application.multiplatform.hellohttp.ux

import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState

class BigTextFieldStateScope(private val textState: BigTextFieldState) {
    fun moveCursorToEnd() {
        textState.viewState.setCursorIndex(textState.text.length)
    }

    fun selectAll() {
        textState.viewState.setSelection(0 ..< textState.text.length)
    }
}

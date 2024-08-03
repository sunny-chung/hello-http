package com.sunnychung.application.multiplatform.hellohttp.extension

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS

fun KeyEvent.isCtrlOrCmdPressed(): Boolean {
    return if (currentOS() == MacOS) {
        isMetaPressed
    } else {
        isCtrlPressed
    }
}

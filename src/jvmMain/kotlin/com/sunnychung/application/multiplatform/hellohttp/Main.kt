package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
    Window(title = "Hello HTTP", onCloseRequest = ::exitApplication) {
        AppView()
    }
    }
}

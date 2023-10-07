package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView

fun main() = application {
    Window(title = "Hello HTTP", onCloseRequest = ::exitApplication) {
        AppView()
    }
}

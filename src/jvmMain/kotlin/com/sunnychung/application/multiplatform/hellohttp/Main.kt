package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    println(AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null))
    runBlocking {
        AppContext.PersistenceManager.initialize()
    }
    application {
        Window(title = "Hello HTTP", onCloseRequest = ::exitApplication) {
            AppView()
        }
    }
}

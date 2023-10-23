package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import java.awt.Dimension

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    println(AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null))
    runBlocking {
        AppContext.PersistenceManager.initialize()
        val preference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
        AppContext.UserPreferenceViewModel.setColorTheme(preference.colourTheme)
    }
    application {
        Window(title = "Hello HTTP", onCloseRequest = ::exitApplication, state = rememberWindowState(width = 1024.dp, height = 768.dp)) {
            window.minimumSize = Dimension(800, 600)
            AppView()
        }
    }
}

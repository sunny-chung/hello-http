package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.error.MultipleProcessError
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import java.awt.Dimension
import java.io.File
import kotlin.system.exitProcess

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    val appDir = AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null)
    println("appDir = $appDir")
    runBlocking {
        try {
            AppContext.SingleInstanceProcessService.apply { dataDir = File(appDir) }.enforce()
        } catch (e: MultipleProcessError) {
            application {
                Window(title = "Hello HTTP", onCloseRequest = { exitProcess(1) } ) {
                    AlertDialog(
                        onDismissRequest = { exitProcess(1) },
                        text = { Text("Another instance of Hello HTTP is running. Please close that process before starting another one.") },
                        confirmButton = { Text(text = "Close", modifier = Modifier.clickable { exitProcess(1) }.padding(10.dp)) },
                    )
                }
            }
            exitProcess(1)
        }
        println("Preparing to start")
        AppContext.PersistenceManager.initialize()
        AppContext.AutoBackupManager.backupNow()
        val preference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
        AppContext.UserPreferenceViewModel.setColorTheme(preference.colourTheme)
    }
    application {
        Window(
            title = "Hello HTTP",
            onCloseRequest = ::exitApplication,
            icon = painterResource("image/appicon.svg"),
            state = rememberWindowState(width = 1024.dp, height = 560.dp)
        ) {
            with(LocalDensity.current) {
                window.minimumSize = if (isMacOs()) {
                    Dimension(800, 450)
                } else {
                    Dimension(800.dp.roundToPx(), 450.dp.roundToPx())
                }
            }
            AppView()
        }
    }
}

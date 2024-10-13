package com.sunnychung.application.multiplatform.hellohttp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.error.MultipleProcessError
import com.sunnychung.application.multiplatform.hellohttp.model.Version
import com.sunnychung.application.multiplatform.hellohttp.platform.LinuxOS
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.WindowsOS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.platform.isMacOs
import com.sunnychung.application.multiplatform.hellohttp.ux.AppView
import com.sunnychung.application.multiplatform.hellohttp.ux.DataLossWarningDialogWindow
import io.github.treesitter.ktreesitter.json.TreeSitterJson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    val appDir = AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null)
    println("appDir = $appDir")
    AppContext.dataDir = File(appDir)
    runBlocking {
        try {
            AppContext.SingleInstanceProcessService.apply { dataDir = File(appDir) }.enforce()
        } catch (e: MultipleProcessError) {
            application {
                Window(title = "Hello HTTP", onCloseRequest = { exitProcess(1) }) {
                    AlertDialog(
                        onDismissRequest = { exitProcess(1) },
                        text = { Text("Another instance of Hello HTTP is running. Please close that process before starting another one.") },
                        confirmButton = {
                            Text(
                                text = "Close",
                                modifier = Modifier.clickable { exitProcess(1) }.padding(10.dp)
                            )
                        },
                    )
                }
            }
            exitProcess(1)
        }
        loadNativeLibraries()
        println("Preparing to start")
        AppContext.PersistenceManager.initialize()

        val dataVersion = AppContext.OperationalRepository.read(OperationalDI())!!.data.appVersion.let { Version(it) }
        val appVersion = AppContext.MetadataManager.version.let { Version(it) }

        val prepareCounter = AtomicInteger(0)

        application {
            var isContinue by remember { mutableStateOf<Boolean?>(null) }
            var isPrepared by remember { mutableStateOf(false) }
            if (isContinue == null) {
                if (dataVersion > appVersion) {
                    DataLossWarningDialogWindow(
                        dataVersion = dataVersion.versionName,
                        appVersion = appVersion.versionName
                    ) {
                        isContinue = it
                    }
                } else {
                    isContinue = true
                }
            }
            if (isContinue == false) {
                println("Exit")
                exitApplication()
            } else if (isContinue == true) {
                LaunchedEffect(Unit) {
                    println("Preparing after continue")
                    if (prepareCounter.addAndGet(1) > 1) {
                        throw RuntimeException("Prepare more than once")
                    }

                    AppContext.OperationalRepository.read(OperationalDI())
                        .also {
                            it!!.data.appVersion = appVersion.versionName
                            AppContext.OperationalRepository.awaitUpdate(OperationalDI())
                        }
                    AppContext.AutoBackupManager.backupNow()
                    val preference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
                    AppContext.UserPreferenceViewModel.setColorTheme(preference.colourTheme)

                    // json path initialization
                    Configuration.setDefaults(object : Configuration.Defaults {
                        override fun jsonProvider(): JsonProvider = JacksonJsonProvider()
                        override fun options(): MutableSet<Option> = mutableSetOf()
                        override fun mappingProvider(): MappingProvider = JacksonMappingProvider()
                    })

                    delay(500L)
                    isPrepared = true
                }

                if (isPrepared) {
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
                } else {
                    // dummy window to prevent application exit before the main window is loaded
                    Window(
                        title = "Hello HTTP",
                        icon = painterResource("image/appicon.svg"),
                        visible = false,
                        onCloseRequest = {}
                    ) {}
                }
            }
        }
    }
}

fun loadNativeLibraries() {
    val libraries = listOf("tree-sitter-json" to TreeSitterJson)
    val systemArch = if (currentOS() == WindowsOS) {
        "x64"
    } else {
        getSystemArchitecture()
    }.uppercase()
    libraries.forEach { (name, enclosingClazz) ->
        val libFileName = when (currentOS()) {
            LinuxOS -> "lib${name}-${systemArch}.so"
            MacOS -> "lib${name}-${systemArch}.dylib"
            else -> "${name}-${systemArch}.dll"
        }
        println("Loading native lib $libFileName")
        val dest = File(File(AppContext.dataDir, "lib"), libFileName)
        dest.parentFile.mkdirs()
        enclosingClazz.javaClass.classLoader.getResourceAsStream(libFileName).use { `is` ->
            `is` ?: throw RuntimeException("Lib $libFileName not found")
            FileOutputStream(dest).use { os ->
                `is`.copyTo(os)
            }
        }
        System.load(dest.absolutePath)
    }
}

fun getSystemArchitecture(): String {
    return exec("uname", "-m").trim()
}

fun exec(vararg components: String): String {
    val pb = ProcessBuilder(*components)
    val process = pb.start()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw RuntimeException("${components.first()} Process finished with exit code $exitCode")
    }
    return output
}

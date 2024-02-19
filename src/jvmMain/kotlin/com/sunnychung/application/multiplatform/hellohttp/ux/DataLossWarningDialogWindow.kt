package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState

@Composable
fun DataLossWarningDialogWindow(dataVersion: String, appVersion: String, onUserRespond: (Boolean) -> Unit) {
    Window(
        title = "Hello HTTP",
        icon = painterResource("image/appicon.svg"),
        onCloseRequest = { onUserRespond(false) },
        resizable = false,
        state = rememberWindowState(position = WindowPosition(Alignment.Center), width = 500.dp, height = 240.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "The version of this application ($appVersion) is older than your user data version " +
                            "($dataVersion). Continue using this application may lose some of your data (e.g. data " +
                            "for features that are only available in newer versions). Please upgrade this " +
                            "application if possible.\n\nContinue to use this old version?"
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Button(onClick = { onUserRespond(true) }) { Text("Continue. Accept possible data loss.") }
                    Button(onClick = { onUserRespond(false) }) { Text("Exit") }
                }
            }
        }
    }
}

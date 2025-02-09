package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun StatusBarView(modifier: Modifier = Modifier) {
    val colors = LocalColor.current
    val metadataManager = AppContext.MetadataManager

    var showDialog by remember { mutableStateOf<DialogName?>(null) }
    var openUrl by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxWidth().height(22.dp).background(colors.backgroundLight).padding(horizontal = 4.dp)) {
        AppText(
            text = "v${metadataManager.version} (${metadataManager.gitCommitHash})",
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.align(Alignment.CenterEnd)) {
            ClickableIconAndText("question.svg", "User Manual") {
                openUrl = "https://sunny-chung.github.io/hello-http/"
                showDialog = DialogName.OpenExternalLink
            }
            ClickableIconAndText("report-issue.svg", "Bug Report or Feature Request") {
                openUrl = "https://github.com/sunny-chung/hello-http/issues"
                showDialog = DialogName.OpenExternalLink
            }
            ClickableIconAndText("setting.svg", "Setting & Data") { showDialog = DialogName.Setting }
        }
    }

    MainWindowDialog(key = "Setting", isEnabled = showDialog == DialogName.Setting, onDismiss = { showDialog = null }) {
        SettingDialogView(closeDialog = { showDialog = null })
    }

    MainWindowDialog(key = "OpenExternalLink", isEnabled = showDialog == DialogName.OpenExternalLink, onDismiss = { showDialog = null }) {
        OpenExternalLinkDialogView(url = openUrl, onDismiss = { showDialog = null })
    }
}

@Composable
fun ClickableIconAndText(imageResource: String, text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.clickable { onClick() }) {
        AppImage(resource = imageResource, size = 16.dp, modifier = Modifier.padding(horizontal = 4.dp))
        AppText(text = text, fontSize = 12.sp)
    }
}

private enum class DialogName {
    Setting, OpenExternalLink
}

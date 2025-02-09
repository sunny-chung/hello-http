package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun OpenExternalLinkDialogView(url: String, onDismiss: () -> Unit) {
    val colour = LocalColor.current
    val clipboardManager = LocalClipboardManager.current
    val localUriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppText("An external link is going to be opened.")

        AppText(
            text = url,
            color = colour.highlight,
            modifier = Modifier
                .background(colour.backgroundInputField, RoundedCornerShape(corner = CornerSize(8.dp)))
                .padding(6.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AppTextButton(text = "Copy URL") {
                clipboardManager.setText(AnnotatedString(url))
                AppContext.ErrorMessagePromptViewModel.showSuccessMessage("Copied text")
            }
            AppTextButton(text = "Open in Web Browser") {
                localUriHandler.openUri(url)
                onDismiss()
            }
            Spacer(Modifier.height(4.dp)) // 8 dp + 4 dp + 8 dp
            AppTextButton(text = "Close", backgroundColor = colour.backgroundStopButton) {
                onDismiss()
            }
        }
    }
}

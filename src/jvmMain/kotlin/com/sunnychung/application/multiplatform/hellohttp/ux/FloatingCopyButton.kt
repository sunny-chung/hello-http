package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun FloatingCopyButton(textToCopy: String, size: Dp = 20.dp, innerPadding: Dp = 4.dp, modifier: Modifier = Modifier) {
    FloatingCopyButton(textProvider = { textToCopy }, size = size, innerPadding = innerPadding, modifier = modifier)
}

@Composable
fun FloatingCopyButton(textProvider: () -> String, size: Dp = 20.dp, innerPadding: Dp = 4.dp, color: Color = LocalColor.current.copyButton, modifier: Modifier = Modifier) {
    val colours = LocalColor.current
    val clipboardManager = LocalClipboardManager.current
    AppImageButton(
        resource = "copy-to-clipboard.svg",
        size = size + innerPadding * 2,
        innerPadding = PaddingValues(innerPadding),
        color = color,
        onClick = {
            val textToCopy = textProvider()
            log.d { "Copied $textToCopy" }
            clipboardManager.setText(AnnotatedString(textToCopy))
            AppContext.ErrorMessagePromptViewModel.showSuccessMessage("Copied text")
        },
        modifier = modifier
            .background(colours.backgroundFloatingButton, RoundedCornerShape(4.dp))
    )
}

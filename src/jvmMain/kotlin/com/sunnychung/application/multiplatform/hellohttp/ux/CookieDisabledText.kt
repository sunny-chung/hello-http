package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CookieDisabledText(modifier: Modifier = Modifier) {
    AppText(
        text = "Cookie is disabled. Edit the Subproject to enable.",
        modifier = modifier.fillMaxWidth(),
    )
}

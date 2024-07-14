package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun PlaceholderText(text: String) {
    val colours = LocalColor.current
    AppText(
        text = text,
        color = colours.placeholder
    )
}

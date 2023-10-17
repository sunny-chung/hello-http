package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppTextButton(modifier: Modifier = Modifier, text: String, color: Color = LocalColor.current.text, onClick: (() -> Unit)?) {
    AppText(
        text = text,
        color = color,
        modifier = modifier
            .background(LocalColor.current.backgroundButton)
            .run {
                if (onClick != null) {
                    this.clickable { onClick() }
                } else {
                    this
                }
            }
            .padding(all = 8.dp)
    )
}

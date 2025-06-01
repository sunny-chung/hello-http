package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppTextButton(
    modifier: Modifier = Modifier,
    text: String,
    image: String? = null,
    isEnabled: Boolean = true,
    color: Color = LocalColor.current.text,
    backgroundColor: Color = if (isEnabled) LocalColor.current.backgroundButton else LocalColor.current.disabled,
    onClick: (() -> Unit)?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(backgroundColor)
            .run {
                if (isEnabled && onClick != null) {
                    this.clickable { onClick() }
                } else {
                    this
                }
            }
            .padding(all = 8.dp)
    ) {
        if (image != null) {
            AppImage(
                resource = image,
                size = 16.dp,
            )
        }
        AppText(
            text = text,
            color = color,
        )
    }
}

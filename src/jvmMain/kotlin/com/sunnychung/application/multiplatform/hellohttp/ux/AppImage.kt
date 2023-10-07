package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppImage(
    modifier: Modifier = Modifier,
    resource: String,
    size: Dp = 32.dp,
    color: Color = LocalColor.current.image,
    enabled: Boolean = true
) {
    val colorToUse = if (enabled) {
        color
    } else {
        color.copy(alpha = color.alpha / 2f)
    }
    var modifierToUse = modifier.size(size)
    Image(
        painter = painterResource("image/$resource"),
        colorFilter = ColorFilter.tint(colorToUse),
        contentDescription = null,
        modifier = modifierToUse
    )
}
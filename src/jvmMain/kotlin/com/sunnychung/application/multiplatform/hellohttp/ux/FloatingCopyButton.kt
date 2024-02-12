package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun FloatingCopyButton(size: Dp = 20.dp, innerPadding: Dp = 4.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colours = LocalColor.current
    AppImageButton(
        resource = "copy-to-clipboard.svg",
        size = size + innerPadding * 2,
        innerPadding = PaddingValues(innerPadding),
        color = colours.copyButton,
        onClick = onClick,
        modifier = modifier
            .background(colours.backgroundFloatingButton, RoundedCornerShape(4.dp))
    )
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltipArea(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    tooltipText: String,
    delayMillis: Int = 100,
    tooltipContent: @Composable () -> Unit = { AppText(text = tooltipText) },
    content: @Composable () -> Unit,
) {
    val colours = LocalColor.current
    TooltipArea(
        tooltip = {
            if (isVisible) {
                Surface(
                    modifier = Modifier.shadow(4.dp).widthIn(max = 500.dp),
                    color = colours.backgroundTooltip,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                        tooltipContent()
                    }
                }
            }
        },
        delayMillis = delayMillis,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            offset = DpOffset(x = 16.dp, y = 8.dp)
        ),
        modifier = modifier,
    ) {
        content()
    }
}

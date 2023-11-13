package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppDeleteButton(
    modifier: Modifier = Modifier,
    initialContentView: @Composable ((Modifier) -> Unit)? = null,
    size: Dp = 16.dp,
    isShowTextLabel: Boolean = false,
    onClickDelete: () -> Unit,
) {
    val colors = LocalColor.current
    var isShowingConfirmButtonState by remember { mutableStateOf(false) }
    var lastClickInstant by remember { mutableStateOf(KInstant.now()) }

    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = colors.backgroundTooltip,
                shape = RoundedCornerShape(8.dp)
            ) {
                AppText(text = "Click to Delete", modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
            }
        },
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            offset = DpOffset(x = 16.dp, y = 0.dp)
        )
    ) {
        Crossfade(
            targetState = isShowingConfirmButtonState,
            animationSpec = tween(
                durationMillis = if (isShowingConfirmButtonState) 1100 else 500,
                easing = LinearOutSlowInEasing
            ),
            modifier = modifier
        ) { isShowingConfirmButton ->
            if (!isShowingConfirmButton) {
                if (initialContentView != null) {
                    initialContentView(Modifier.clickable { isShowingConfirmButtonState = true })
                } else {
                    AppImageButton(
                        resource = "delete.svg",
                        color = colors.placeholder,
                        size = size,
                        modifier = Modifier.focusProperties { canFocus = false },
                        onClick = {
                            isShowingConfirmButtonState = true
                            lastClickInstant = KInstant.now()
                        },
                    )
                }
            } else {
                Row(modifier = Modifier
                    .clickable {
                        if (KInstant.now() - lastClickInstant >= 500.milliseconds()) {
                            onClickDelete()
                            isShowingConfirmButtonState = false
                        }
                    }
                    .onPointerEvent(eventType = PointerEventType.Exit, onEvent = { isShowingConfirmButtonState = false })
                    .focusProperties { canFocus = false }
                ) {
                    AppImage(resource = "warning-sharp.svg", color = colors.highlight, size = size)
                    if (isShowTextLabel) {
                        AppText(text = "Click to Confirm", color = colors.highlight)
                    }
                }
            }
        }
    }
}

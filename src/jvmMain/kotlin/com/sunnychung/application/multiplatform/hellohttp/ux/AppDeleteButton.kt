package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppDeleteButton(
    modifier: Modifier = Modifier,
    initialContentView: @Composable ((Modifier) -> Unit)? = null,
    size: Dp = 16.dp,
    isShowTextLabel: Boolean = false,
    onClickDelete: () -> Unit,
) {
    val colors = LocalColor.current
    var isShowingConfirmButton by remember { mutableStateOf(false) }

    if (!isShowingConfirmButton) {
        if (initialContentView != null) {
            initialContentView(modifier.clickable { isShowingConfirmButton = true })
        } else {
            AppImageButton(resource = "delete.svg", size = size, modifier = modifier, onClick = { isShowingConfirmButton = true })
        }
    } else {
        Row(modifier = modifier.clickable { onClickDelete() }.onPointerEvent(eventType = PointerEventType.Exit, onEvent = { isShowingConfirmButton = false })) {
            AppImage(resource = "warning-sharp.svg", color = colors.highlight, size = size)
            if (isShowTextLabel) {
                AppText(text = "Click to Confirm", color = colors.highlight)
            }
        }
    }
}

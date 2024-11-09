package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun FloatingButtonContainer(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    size: Dp = 20.dp,
    innerPadding: Dp = 4.dp,
    outerPadding: PaddingValues = PaddingValues(top = 4.dp, end = 12.dp),
    buttonImage: String,
    tooltip: String? = null,
    onClickButton: (String) -> Unit,
    contentView: @Composable () -> Unit,
) {
    if (!isEnabled) {
        Box(modifier = modifier) {
            contentView()
        }
        return
    }

    var isShowFloatingButton by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) {
                isShowFloatingButton = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isShowFloatingButton = false
            }
    ) {
        contentView()
        if (isShowFloatingButton) {
            AppTooltipArea(
                isVisible = tooltip != null,
                tooltipText = tooltip ?: "",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(outerPadding)
            ) {
                FloatingButton(
                    image = buttonImage,
                    onClick = onClickButton,
                    size = size,
                    innerPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
fun FloatingButton(image: String, size: Dp = 20.dp, innerPadding: Dp = 4.dp, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    val colours = LocalColor.current
    AppImageButton(
        resource = image,
        size = size + innerPadding * 2,
        innerPadding = PaddingValues(innerPadding),
        color = colours.copyButton,
        onClick = {
            onClick(image)
        },
        modifier = modifier
            .background(colours.backgroundFloatingButton, RoundedCornerShape(4.dp))
    )
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppTextToggleButton(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    val colours = LocalColor.current
    AppText(
        text = text,
        color = if (isEnabled) colours.primary else colours.disabled,
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        modifier = modifier
            .background(if (isSelected && isEnabled) colours.backgroundInputField else colours.backgroundLight)
            .run {
                if (isEnabled) {
                    clickable { onToggle(!isSelected) }
                } else {
                    this
                }
            }
            .padding(horizontal = 2.dp, vertical = 4.dp)
    )
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    uncheckedColor: Color = LocalColor.current.placeholder,
    checkedColor: Color = LocalColor.current.highlight,
    size: Dp,
) {
    AppImageButton(
        resource = if (checked) {
            "checkbox-checked.svg"
        } else {
            "checkbox.svg"
        },
        color = if (checked) {
            checkedColor
        } else {
            uncheckedColor
        },
        size = size,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
    )
}

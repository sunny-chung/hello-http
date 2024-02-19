package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun AppCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isFocusable: Boolean = true,
    uncheckedColor: Color = LocalColor.current.placeholder,
    checkedColor: Color = LocalColor.current.highlight,
    size: Dp,
    spacingWithLabel: Dp = 2.dp,
    label: (@Composable () -> Unit)? = null,
) {
    val clickAction = { onCheckedChange(!checked) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacingWithLabel),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(onClick = clickAction),
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
            onClick = null, // use parent's clickable
            modifier = Modifier.let {
                if (!isFocusable) {
                    it.focusable(false)
                } else {
                    it
                }
            }
        )
        label?.invoke()
    }
}

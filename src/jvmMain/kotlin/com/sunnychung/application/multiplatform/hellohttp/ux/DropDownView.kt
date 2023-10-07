package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

/**
 * @param onClickItem return true to dismiss menu
 */
@Composable
fun <T: DropDownable> DropDownView(
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    items: List<T>,
    contentView: @Composable ((T?) -> Unit) = { AppText(text = it?.displayText ?: "--") },
    isShowLabel: Boolean = true,
    hasSpacer: Boolean = false,
    clickableArea: DropDownClickableArea = DropDownClickableArea.All,
    selectedItem: T? = null,
    onClickItem: (T) -> Boolean
) {
    val colors = LocalColor.current

    var isShowContextMenu by remember { mutableStateOf(false) }

    CursorDropdownMenu(
        expanded = isShowContextMenu,
        onDismissRequest = { isShowContextMenu = false },
        modifier = Modifier.background(colors.backgroundContextMenu)
    ) {
        items.forEach { item ->
//            DropdownMenuItem(
//                onClick = {
//                    if (onClickItem(item)) {
//                        isShowContextMenu = false
//                    }
//                },
//                contentPadding = PaddingValues(horizontal = 8.dp)
//            ) {
//                contentView(item)
//            }

            Column(modifier = Modifier.clickable {
                if (onClickItem(item)) {
                    isShowContextMenu = false
                }
            }.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()) {
                contentView(item)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { isShowContextMenu = !isShowContextMenu }
    ) {
        if (isShowLabel) {
            contentView(selectedItem)
        }
        if (hasSpacer) {
            Spacer(modifier = Modifier.weight(1f))
        }
        AppImage(resource = "down-small.svg", size = iconSize)
    }
}

interface DropDownable {
    val displayText: String
}

data class DropDownValue(override val displayText: String) : DropDownable

enum class DropDownClickableArea {
    All, ArrowOnly
}

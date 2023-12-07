package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

/**
 * @param onClickItem return true to dismiss menu
 */
@Composable
fun <T: DropDownable> DropDownView(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    iconResource: String = "down-small.svg",
    iconSize: Dp = 16.dp,
    items: List<T>,
    populateItems: (List<T>) -> List<T> = { it },
    maxLines: Int = 1,
    isLabelFillMaxWidth: Boolean = false,
    isShowLabel: Boolean = true,
    contentView: @Composable (RowScope.(it: T?, isLabel: Boolean, isSelected: Boolean, isClickable: Boolean) -> Unit) = {it, isLabel, isSelected, isClickable ->
        AppText(
            text = it?.displayText.emptyToNull() ?: "--",
            color = if (!isLabel && isSelected) {
                LocalColor.current.highlight
            } else if (isClickable) {
                LocalColor.current.primary
            } else {
                LocalColor.current.disabled
            },
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = if (isLabelFillMaxWidth) Modifier.weight(1f) else Modifier.weight(1f, fill = false)
        )
    },
    arrowPadding: PaddingValues = PaddingValues(0.dp),
    selectedItem: T? = null,
    onClickItem: (T) -> Boolean
) {
    val colors = LocalColor.current
    val populatedItems = populateItems(items)
    val isClickable = isEnabled && populatedItems.isNotEmpty()

    var isShowContextMenu by remember { mutableStateOf(false) }

    CursorDropdownMenu(
        expanded = isShowContextMenu,
        onDismissRequest = { isShowContextMenu = false },
        modifier = Modifier.background(colors.backgroundContextMenu)
    ) {
        populatedItems.forEach { item ->
            Column(modifier = Modifier.clickable {
                if (onClickItem(item)) {
                    isShowContextMenu = false
                }
            }.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()) {
                Row {
                    contentView(item, false, item.key == selectedItem?.key, isClickable)
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.run {
            if (isClickable) {
                clickable {
                    isShowContextMenu = !isShowContextMenu
                }
            } else {
                this
            }
        }
    ) {
        if (isShowLabel) {
            contentView(selectedItem, true, false, isClickable)
        }
        AppImage(
            resource = iconResource,
            size = iconSize,
            color = if (isClickable) colors.primary else colors.disabled,
            modifier = Modifier.padding(arrowPadding),
        )
    }
}

interface DropDownable {
    val key: Any?
    val displayText: String
}

data class DropDownValue(override val displayText: String) : DropDownable {
    override val key: String
        get() = displayText
}

data class DropDownKeyValue<T>(override val key: T, override val displayText: String) : DropDownable

data class DropDownMap<T>(private val values: List<DropDownKeyValue<T>>) {
    private val mapByKey = values.associateBy { it.key }

    val dropdownables = values

    operator fun get(key: T) = mapByKey[key]

}

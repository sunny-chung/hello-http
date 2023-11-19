package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun TabsView(modifier: Modifier, selectedIndex: Int, onSelectTab: (Int) -> Unit, onDoubleClickTab: ((Int) -> Unit)? = null, contents: List<@Composable (() -> Unit)>) {
    val colors = LocalColor.current
    var lastSelectedIndex by remember { mutableStateOf(-1) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // needed for scrolling
    LazyRow(modifier = modifier.background(color = colors.backgroundLight), state = scrollState) {
        items(count = contents.size) { i ->
            TabItem(
                isSelected = (selectedIndex == i),
                key = "$i", // using `i` does not work
                onClick = {
                    log.d { "Tab onClick $i" }
                    onSelectTab(i)
                },
                onDoubleClickTab = onDoubleClickTab?.let { { log.d { "Tab onDoubleClickTab $i" }; it(i) } },
            ) {
                contents[i]()
            }
        }
    }
    if (lastSelectedIndex != selectedIndex) {
        log.d { "Tab $lastSelectedIndex -> $selectedIndex" }
        lastSelectedIndex = selectedIndex
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                log.d { "Tab scroll to $selectedIndex" }
//                delay(200)
                scrollState.animateScrollToItem(selectedIndex)
            }
        }
    }

}

@Composable
@Deprecated("Use TabItem instead.")
fun TabItemOld(modifier: Modifier = Modifier, isSelected: Boolean, onClick: () -> Unit, content: @Composable (() -> Unit)) {
    val colors = LocalColor.current

    fun adjust(original: Int, adjustment: Int): Int {
        if (original == Constraints.Infinity) return original
        return original + adjustment
    }

    Layout({
        Box(modifier = Modifier.clickable { onClick() }) {
            content()
        }
        VerticalLine(if (isSelected) colors.line else Color.Transparent)
        VerticalLine(if (isSelected) colors.line else Color.Transparent)
        HorizontalLine(if (!isSelected) colors.line else Color.Transparent)
    }, modifier) { measurables, constraints ->
        require(measurables.size == 4)

        val content = measurables[0].measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = adjust(constraints.maxWidth, -2),
                minHeight = constraints.minHeight,
                maxHeight = adjust(constraints.maxHeight, -2),
            )
        )

        val verticalLines = (1..2).map { i ->
            measurables[i].measure(
                Constraints(
                    minWidth = 1,
                    maxWidth = 1,
                    minHeight = constraints.minHeight,
                    maxHeight = min(constraints.maxHeight, content.height + 2),
                )
            )
        }

        val horizontalLines = (3..3).map { i ->
            measurables[i].measure(
                Constraints(
                    minWidth = constraints.minWidth,
                    maxWidth = min(constraints.maxWidth, content.width + 2),
                    minHeight = 1,
                    maxHeight = 1,
                )
            )
        }

        layout(width = content.width + 2, height = content.height + 2) {
            content.place(x = 1, y = 1)
            verticalLines[0].place(x = 0, y = 0)
            verticalLines[1].place(x = 1 + content.width, y = 0)
            horizontalLines[0].place(x = 0, y = 1 + content.height)
        }
    }
}

@Composable
fun VerticalLine(color: Color = LocalColor.current.line) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(color = color)
    )
}

@Composable
fun HorizontalLine(color: Color = LocalColor.current.line) {
    Box(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(color = color)
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TabItem(modifier: Modifier = Modifier, key: String, isSelected: Boolean, onClick: () -> Unit, onDoubleClickTab: (() -> Unit)? = null, content: @Composable (() -> Unit)) {
    val colors = LocalColor.current
    var modifierToUse = modifier
        .background(if (isSelected) colors.backgroundInputField else Color.Transparent)
    if (onDoubleClickTab != null) {
        modifierToUse = modifierToUse
            .onPointerEvent(PointerEventType.Press) {
                onClick()
            }
            .combinedClickable(
                onDoubleClick = onDoubleClickTab,
                onClick = {} // not using this because there will be a significant delay
            )
    } else {
        modifierToUse = modifierToUse.clickable { onClick() }
    }
    Box(modifier = modifierToUse) {
        content()
    }
}

package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.delay

@Composable
fun BigTextFieldCursor(modifier: Modifier = Modifier, lineHeight: Dp) {
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        Box(
            modifier = modifier
                .width(2.dp)
                .height(lineHeight)
                .background(LocalColor.current.cursor)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(700.milliseconds().millis)
            isVisible = !isVisible
        }
    }
}

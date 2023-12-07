package com.sunnychung.application.multiplatform.hellohttp.ux.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
inline fun <T> rememberLast(vararg keys: Any?, crossinline calculation: @DisallowComposableCalls () -> T): T {
    var lastState by remember { mutableStateOf(keys) }
    var lastCalculation by remember { mutableStateOf(calculation()) }
    if (!lastState.contentEquals(keys)) {
        lastCalculation = calculation()
        lastState = keys
    }
    return lastCalculation
}

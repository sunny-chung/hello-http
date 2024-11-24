package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
@Composable
fun <T> debouncedStateOf(interval: KDuration, stateProducer: () -> T): T {
    val currentState = stateProducer()
    val coroutineScope = rememberCoroutineScope()
    val flow = remember { MutableSharedFlow<T>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ) }
    var stateRecord by remember { mutableStateOf(currentState) }
    if (stateRecord != currentState) {
        flow.tryEmit(currentState)
    }
    flow.debounce(interval.toMilliseconds())
        .onEach {
            stateRecord = it
        }
        .launchIn(scope = coroutineScope)
    return stateRecord
}

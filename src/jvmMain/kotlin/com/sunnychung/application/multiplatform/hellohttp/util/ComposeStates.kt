package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * @return pair of debounced state and "is the state latest"
 */
@OptIn(FlowPreview::class)
@Composable
fun <T> debouncedStateOf(interval: KDuration, tolerateCount: Int = 0, vararg cacheKeys: Any?, stateProducer: () -> T): Pair<T, Boolean> {
    val currentState = stateProducer()
    val flow = remember(*cacheKeys) { MutableSharedFlow<T>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ) }
    var stateRecord by remember(*cacheKeys) { mutableStateOf(currentState) }
    var stateCount by remember(*cacheKeys) { mutableStateOf(0) }
    if (stateRecord != currentState) {
        ++stateCount
        flow.tryEmit(currentState).also {
            log.v { "ds tryEmit($currentState) = $it. old = $stateRecord" }
        }
        if (stateCount <= tolerateCount) {
            stateRecord = currentState
        }
    }
    LaunchedEffect(flow) {
        log.v { "ds launch flow" }
        flow.distinctUntilChanged()
            .debounce(interval.toMilliseconds())
            .collect {
                log.v { "ds collect state $it" }
                stateRecord = it
            }
    }
    return (stateRecord to (stateRecord == currentState)).also {
        log.v { "ds state = $it, count = $stateCount" }
    }
}

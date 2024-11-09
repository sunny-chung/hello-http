package com.sunnychung.application.multiplatform.hellohttp.util

import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

// Modified from https://blog.shreyaspatil.dev/collecting-items-from-the-flow-in-chunks
private class TimeChunkedLatestFlow<T>(
    private val upstream: Flow<T>,
    private val duration: KDuration
) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = coroutineScope<Unit> {
        val mutex = Mutex()

        // Holds the un-emitted items
        var latestValue: T? = null
        var hasValue = false

        // Flag to know the status of upstream flow whether it has been completed or not
        var isFlowCompleted = false

        launch {
            try {
                while (true) {
                    delay(duration.toMilliseconds())
                    mutex.withLock {
                        // If the upstream flow has been completed and there are no values
                        // pending to emit in the collector, just break this loop.
                        if (isFlowCompleted && !hasValue) {
                            return@launch
                        }
                        if (hasValue) {
                            collector.emit(latestValue!!)
                            hasValue = false
                        }
                    }
                }
            } catch (e: CancellationException) {
                mutex.withLock {
                    if (hasValue) {
                        collector.emit(latestValue!!)
                        hasValue = false
                    }
                }
                throw e
            }
        }

        // Collect the upstream flow and add the items to the above `values` list
        upstream.collect {
            mutex.withLock {
                latestValue = it
                hasValue = true
            }
        }

        // If we reach here it means the upstream flow has been completed and won't
        // produce any values anymore. So set the flag as flow is completed so that
        // child coroutine will break its loop
        isFlowCompleted = true
    }
}

fun <T> Flow<T>.chunkedLatest(duration: KDuration): Flow<T> = TimeChunkedLatestFlow(this, duration)

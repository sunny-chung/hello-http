package com.sunnychung.application.multiplatform.hellohttp.helper

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CountDownLatch(private var value: Int) {
    private val valueLock = Mutex()
    private val globalLock = Mutex()

    init {
        runBlocking {
            globalLock.lock()
        }
    }

    suspend fun decrement() {
        valueLock.withLock {
            if (--value <= 0) {
                globalLock.unlock()
            }
        }
    }

    suspend fun await() {
        globalLock.withLock { /* do nothing */ }
    }
}

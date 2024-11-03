package com.sunnychung.application.multiplatform.hellohttp.test.util

import com.sunnychung.application.multiplatform.hellohttp.util.chunkedLatest
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkedLatestFlowTest {

    @Test
    fun receiveOnlyLatestValues() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    (0..10).forEach {
                        emit(it)
                        delay(145)
                    }
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)
            }

            assertEquals(listOf(3, 6, 10), results)
        }
    }

    @Test
    fun receiveValuesEmittedAtCompletion1() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    (0..10).forEach {
                        emit(it)
                        delay(145)
                    }
                    emit(11)
                    emit(12)
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)
            }

            assertEquals(listOf(3, 6, 10, 12), results)
        }
    }

    @Test
    fun receiveValuesEmittedAtCompletion2() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    (0..12).forEach {
                        emit(it)
                        delay(145)
                    }
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)
            }

            assertEquals(listOf(3, 6, 10, 12), results)
        }
    }

    @Test
    fun emptyFlow() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    delay(1000)
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)
            }

            assertEquals(listOf(), results)
        }
    }

    @Test
    fun singleValueWithoutDelay() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    emit(10)
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)
            }

            assertEquals(listOf(10), results)
        }
    }
}

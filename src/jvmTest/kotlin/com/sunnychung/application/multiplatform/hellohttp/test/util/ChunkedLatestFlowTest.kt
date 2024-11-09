package com.sunnychung.application.multiplatform.hellohttp.test.util

import com.sunnychung.application.multiplatform.hellohttp.util.chunkedLatest
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkedLatestFlowTest {

    @Test
    fun receiveOnlyLatestValues() {
        runBlocking(Dispatchers.IO) {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    val startTime = System.currentTimeMillis()
                    (0..10).forEach {
                        emit(it)
                        val currTime = System.currentTimeMillis()
                        println("t=${currTime - startTime}: $it")
                        delay((it + 1) * 145 - (currTime - startTime))
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
        runBlocking(Dispatchers.IO) {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    val startTime = System.currentTimeMillis()
                    (0..10).forEach {
                        emit(it)
                        val currTime = System.currentTimeMillis()
                        println("t=${currTime - startTime}: $it")
                        delay((it + 1) * 145 - (currTime - startTime))
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
        runBlocking(Dispatchers.IO) {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                flow<Int> {
                    val startTime = System.currentTimeMillis()
                    (0..12).forEach {
                        emit(it)
                        val currTime = System.currentTimeMillis()
                        println("t=${currTime - startTime}: $it")
                        delay((it + 1) * 145 - (currTime - startTime))
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

    @Test
    fun collectAfterCancel() {
        runBlocking {
            val results = Collections.synchronizedList(mutableListOf<Int>())

            coroutineScope {
                val flow = flow<Int> {
                    (0..12).forEach {
                        emit(it)
                        delay(145)
                    }
                }
                    .chunkedLatest(500.milliseconds())
                    .onEach { results += it }
                    .launchIn(this)

                launch {
                    delay(410)
                    flow.cancel()
                }
            }

            assertEquals(listOf(2), results)
        }
    }
}

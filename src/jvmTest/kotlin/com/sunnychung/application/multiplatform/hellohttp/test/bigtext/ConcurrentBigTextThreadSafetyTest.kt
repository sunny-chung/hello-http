package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.ConcurrentBigText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentBigTextThreadSafetyTest {

    @Test
    fun concurrentAppend() {
        val NUM_ITERATIONS_PER_THREAD = 1000000
        val NUM_THREADS = 10

        fun CoroutineScope.testOp(text: BigText) {
            (0 until NUM_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        text.append("_")
                    }
                }
            }
        }

        // first, check BigText is thread-unsafe
        BigTextImpl().let { text ->
            try {
                runBlocking(Dispatchers.IO) {
                    testOp(text)
                }
                assert(text.length < NUM_THREADS * NUM_ITERATIONS_PER_THREAD)
            } catch (_: Exception) {
                println("Caught exception while running thread-unsafe test. It is safe to ignore.")
            }
        }

        // next, check ConcurrentBigText is thread-safe
        ConcurrentBigText(BigTextImpl()).let { text ->
            runBlocking(Dispatchers.IO) {
                testOp(text)
            }
            assertEquals(NUM_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
        }
    }

    @Test
    fun concurrentInsertAndLength() {
        val NUM_ITERATIONS_PER_THREAD = 100000
        val NUM_THREADS = 10
        val random = Random(12345)

        fun CoroutineScope.testOp(text: BigText) {
            (0 until NUM_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        text.insertAt(random.nextInt(0, text.length + 1),"_")
                    }
                }
            }
        }

        // first, check BigText is thread-unsafe
        BigTextImpl().let { text ->
            try {
                runBlocking(Dispatchers.IO) {
                    withContext(CoroutineExceptionHandler { context, e ->
                        println("Caught exception in coroutine while running thread-unsafe test. It is safe to ignore.")
                    }) {
                        testOp(text)
                    }
                }
                assert(text.length < NUM_THREADS * NUM_ITERATIONS_PER_THREAD)
            } catch (_: Exception) {
                println("Caught exception while running thread-unsafe test. It is safe to ignore.")
            }
        }

        // next, check ConcurrentBigText is thread-safe
        ConcurrentBigText(BigTextImpl()).let { text ->
            runBlocking(Dispatchers.IO) {
                testOp(text)
            }
            assertEquals(NUM_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
        }
    }

    @Test
    fun concurrentDeleteAndLength() {
        val NUM_ITERATIONS_PER_THREAD = 100000
        val NUM_THREADS = 10
        val random = Random(12345)
        val initialText = (0 until 1200000).joinToString("") { (it % 10).toString() }

        fun CoroutineScope.testOp(text: BigText) {
            (0 until NUM_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        fun op() {
                            val start = random.nextInt(0, text.length)
                            text.delete(start, start + 1)
                        }
                        if (text is ConcurrentBigText) {
                            text.withWriteLock {
                                op()
                            }
                        } else {
                            op()
                        }
                    }
                }
            }
        }

        // first, check BigText is thread-unsafe
        BigTextImpl().let { text ->
            try {
                runBlocking(Dispatchers.IO) {
                    text.append(initialText)
                    withContext(CoroutineExceptionHandler { context, e ->
                        println("Caught exception in coroutine while running thread-unsafe test. It is safe to ignore.")
                    }) {
                        testOp(text)
                    }
                }
                assert(text.length > initialText.length - NUM_THREADS * NUM_ITERATIONS_PER_THREAD)
            } catch (_: Exception) {
                println("Caught exception while running thread-unsafe test. It is safe to ignore.")
            }
        }

        // next, check ConcurrentBigText is thread-safe
        ConcurrentBigText(BigTextImpl()).let { text ->
            runBlocking(Dispatchers.IO) {
                text.append(initialText)
                testOp(text)
            }
            assertEquals(initialText.length - NUM_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
        }
    }
}

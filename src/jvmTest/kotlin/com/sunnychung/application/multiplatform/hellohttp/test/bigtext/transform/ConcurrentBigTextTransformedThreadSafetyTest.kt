package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformed
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.ConcurrentBigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.ConcurrentBigTextTransformed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentBigTextTransformedThreadSafetyTest {

    @Test
    fun concurrentAppendBothOriginalAndTransformed() {
        val NUM_ITERATIONS_PER_THREAD = 50000
        val NUM_ORIGINAL_THREADS = 8
        val NUM_TRANSFORMED_THREADS = 12

        fun CoroutineScope.testOp(text: BigText, transformed: BigTextTransformed) {
            (0 until NUM_TRANSFORMED_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        transformed.append("_")
                    }
                }
            }
            (0 until NUM_ORIGINAL_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        text.append("-")
                    }
                }
            }
        }
        val expectedCount = (NUM_ORIGINAL_THREADS + NUM_TRANSFORMED_THREADS) * NUM_ITERATIONS_PER_THREAD

        // first, check BigTextTransformerImpl is thread-unsafe
        // update: Don't check. It forms a cycle in RB-tree and thus an infinite loop.
//        BigTextImpl().let { text ->
//            val transformed = BigTextTransformerImpl(text)
//            try {
//                runBlocking(Dispatchers.IO) {
//                    testOp(text, transformed)
//                }
//                assert(text.length < expectedCount)
//            } catch (_: Exception) {
//                println("Caught exception while running thread-unsafe test. It is safe to ignore.")
//            }
//        }

        // next, check ConcurrentBigText is thread-safe
        ConcurrentBigText(BigTextImpl()).let { text ->
            val transformed = ConcurrentBigTextTransformed(BigTextTransformerImpl(text))
            runBlocking(Dispatchers.IO) {
                testOp(text, transformed)
            }
            assertEquals(NUM_ORIGINAL_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
            assertEquals(expectedCount, transformed.length)
        }
    }

    @Test
    fun concurrentInsertBothOriginalAndTransformed() {
        val NUM_ITERATIONS_PER_THREAD = 25000
        val NUM_ORIGINAL_THREADS = 8
        val NUM_TRANSFORMED_THREADS = 12

        val random = Random(123456)

        fun CoroutineScope.testOp(text: BigText, transformed: BigTextTransformed) {
            (0 until NUM_TRANSFORMED_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        transformed.insertAt(random.nextInt(0, text.length + 1),"_")
                    }
                }
            }
            (0 until NUM_ORIGINAL_THREADS).forEach {
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach {
                        text.insertAt(random.nextInt(0, text.length + 1),"-")
                    }
                }
            }
        }
        val expectedCount = (NUM_ORIGINAL_THREADS + NUM_TRANSFORMED_THREADS) * NUM_ITERATIONS_PER_THREAD
        ConcurrentBigText(BigTextImpl()).let { text ->
            val transformed = ConcurrentBigTextTransformed(BigTextTransformerImpl(text))
            runBlocking(Dispatchers.IO) {
                testOp(text, transformed)
            }
            assertEquals(NUM_ORIGINAL_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
            assertEquals(expectedCount, transformed.length)
        }
    }

    @Test
    fun concurrentDeleteBothOriginalAndTransformed() {
        val NUM_ITERATIONS_PER_THREAD = 25000
        val NUM_ORIGINAL_THREADS = 8
        val NUM_TRANSFORMED_THREADS = 12

        val initialText = (0 until 600000).joinToString("") { (it % 10).toString() }

        fun CoroutineScope.testOp(text: BigText, transformed: BigTextTransformed) {
            (0 until NUM_TRANSFORMED_THREADS).forEach { t ->
                launch {
                    (0 until NUM_ITERATIONS_PER_THREAD).forEach { i ->
                        transformed.delete(NUM_ITERATIONS_PER_THREAD * t + i, NUM_ITERATIONS_PER_THREAD * t + i + 1)
                    }
                }
            }
            (0 until NUM_ORIGINAL_THREADS).forEach { t ->
                launch {
                    (NUM_ITERATIONS_PER_THREAD - 1 downTo 0).forEach { i ->
                        text.delete(NUM_ITERATIONS_PER_THREAD * (NUM_TRANSFORMED_THREADS + 1) + i, NUM_ITERATIONS_PER_THREAD * (NUM_TRANSFORMED_THREADS + 1) + i + 1)
                    }
                }
            }
        }
        val expectedCount = initialText.length - (NUM_ORIGINAL_THREADS + NUM_TRANSFORMED_THREADS) * NUM_ITERATIONS_PER_THREAD
        ConcurrentBigText(BigTextImpl()).let { text ->
            text.append(initialText)
            val transformed = ConcurrentBigTextTransformed(BigTextTransformerImpl(text))
            runBlocking(Dispatchers.IO) {
                testOp(text, transformed)
            }
            assertEquals(initialText.length - NUM_ORIGINAL_THREADS * NUM_ITERATIONS_PER_THREAD, text.length)
            assertEquals(expectedCount, transformed.length)
        }
    }
}

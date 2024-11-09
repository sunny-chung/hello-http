package com.sunnychung.application.multiplatform.hellohttp.test.util

import com.sunnychung.application.multiplatform.hellohttp.util.CircularList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CircularListTest {

    @Test
    fun pushAndRemoveHead() {
        val l = CircularList<Int>(100)

        assertEquals(0, l.size)

        (0 until 80).forEach {
            l.push(it)
            println("h = ${l.head}")
            assertEquals(it + 1, l.size)
        }

        (79 downTo 0).forEach {
            val removed = l.removeHead()
            println("h = ${l.head}")
            assertEquals(it, removed)
            assertEquals(it, l.size)
        }

        assertEquals(null, l.removeHead())
        assertEquals(0, l.size)
        println("h = ${l.head}")

        (80 until 90).forEach {
            l.push(it)
            println("h = ${l.head}")
            assertEquals(it + 1 - 80, l.size)
        }

        (89 downTo 80).forEach {
            val removed = l.removeHead()
            assertEquals(it, removed)
            assertEquals(it - 80, l.size)
        }

        assertEquals(null, l.removeHead())
        assertEquals(0, l.size)
    }

    @Test
    fun pushAndRemoveTail() {
        val l = CircularList<Int>(100)

        assertEquals(0, l.size)

        (0 until 80).forEach {
            l.push(it)
            assertEquals(it + 1, l.size)
        }

        (0 until 80).forEach {
            val removed = l.removeTail()
            assertEquals(it, removed)
            assertEquals(79 - it, l.size)
        }

        assertEquals(null, l.removeTail())
        assertEquals(0, l.size)

        (80 until 90).forEach {
            l.push(it)
            assertEquals(it + 1 - 80, l.size)
        }

        (80 until 90).forEach {
            val removed = l.removeTail()
            assertEquals(it, removed)
            assertEquals(89 - it, l.size)
        }

        assertEquals(null, l.removeTail())
        assertEquals(0, l.size)
    }

    @Test
    fun pushAndRemoveHeadRotate() {
        val l = CircularList<Int>(100)

        assertEquals(0, l.size)

        (0 until 280).forEach {
            l.push(it)
            println("h = ${l.head}, t = ${l.tail}")
            assertEquals(minOf(100, it + 1), l.size)
        }

        (279 downTo 180).forEach {
            val removed = l.removeHead()
            assertEquals(it, removed)
            assertEquals(it - 180, l.size)
        }

        assertEquals(null, l.removeHead())
        assertEquals(0, l.size)

        (280 until 330).forEach {
            l.push(it)
            assertEquals(it + 1 - 280, l.size)
        }

        (329 downTo 280).forEach {
            val removed = l.removeHead()
            assertEquals(it, removed)
            assertEquals(it - 280, l.size)
        }

        assertEquals(null, l.removeHead())
        assertEquals(0, l.size)
    }

    @Test
    fun pushAndRemoveTailRotate() {
        val l = CircularList<Int>(100)

        assertEquals(0, l.size)

        (0 until 280).forEach {
            l.push(it)
            assertEquals(minOf(100, it + 1), l.size)
        }

        (180 until 280).forEach {
            val removed = l.removeTail()
            assertEquals(it, removed)
            assertEquals(279 - it, l.size)
        }

        assertEquals(null, l.removeTail())
        assertEquals(0, l.size)

        (280 until 330).forEach {
            l.push(it)
            assertEquals(it + 1 - 280, l.size)
        }

        (280 until 330).forEach {
            val removed = l.removeTail()
            assertEquals(it, removed)
            assertEquals(329 - it, l.size)
        }

        assertEquals(null, l.removeTail())
        assertEquals(0, l.size)
    }

    @Test
    fun mixed() {
        val l = CircularList<Int>(100)

        assertEquals(0, l.size)

        (0 until 70).forEach {
            l.push(it)
            assertEquals(minOf(100, it + 1), l.size)
        }

        (69 downTo 40).forEach {
            val removed = l.removeHead()
            assertEquals(it, removed)
            assertEquals(it, l.size)
        }

        (100 until 160).forEach {
            l.push(it)
            assertEquals(minOf(100, it - 60 + 1), l.size)
        }

        (0 until 20).forEach {
            val removed = l.removeTail()
            assertEquals(it, removed)
            assertEquals(99 - it, l.size)
        }

        (159 downTo 150).forEach {
            val removed = l.removeHead()
            assertEquals(it, removed)
            assertEquals(it - 80, l.size)
        }

        (200 until 230).forEach {
            l.push(it)
            assertEquals(minOf(100, it - 200 + 70 + 1), l.size)
        }

        assertEquals(229, l.removeHead())
        assertEquals(99, l.size)

        assertEquals(20, l.removeTail())
        assertEquals(98, l.size)

        val random = Random(100)

        (97 downTo 0).forEach {
            if (random.nextBoolean()) {
                l.removeHead()
            } else {
                l.removeTail()
            }
            assertEquals(it, l.size)
        }

        assertEquals(null, l.removeTail())
        assertEquals(0, l.size)
    }
}

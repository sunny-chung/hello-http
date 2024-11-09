package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import kotlin.test.Test
import kotlin.test.assertEquals

class BinarySearchTest {

    @Test
    fun binarySearchForMaxIndexOfValueAtMost() {
        // [0, 2, 37, 57, 72, 85, 91, 113]
        // f(-1) = -1, f(0) = 0, f(72) = 4, f(73) = 4, f(84) = 4, f(85) = 5, f(113) = 7, f(999) = 7
        val l = listOf(0, 2, 37, 57, 72, 85, 91, 113)
        assertEquals(-1, l.binarySearchForMaxIndexOfValueAtMost(-1))
        assertEquals(0, l.binarySearchForMaxIndexOfValueAtMost(0))
        assertEquals(4, l.binarySearchForMaxIndexOfValueAtMost(72))
        assertEquals(4, l.binarySearchForMaxIndexOfValueAtMost(73))
        assertEquals(4, l.binarySearchForMaxIndexOfValueAtMost(84))
        assertEquals(5, l.binarySearchForMaxIndexOfValueAtMost(85))
        assertEquals(7, l.binarySearchForMaxIndexOfValueAtMost(113))
        assertEquals(7, l.binarySearchForMaxIndexOfValueAtMost(999))
    }

    @Test
    fun binarySearchForMinIndexOfValueAtLeast() {
        // [0, 2, 37, 57, 72, 85, 91, 113]
        // f(-1) = 0, f(0) = 0, f(72) = 4, f(73) = 5, f(84) = 5, f(85) = 5, f(113) = 7, f(999) = 8
        val l = listOf(0, 2, 37, 57, 72, 85, 91, 113)
        assertEquals(0, l.binarySearchForMinIndexOfValueAtLeast(-1))
        assertEquals(0, l.binarySearchForMinIndexOfValueAtLeast(0))
        assertEquals(4, l.binarySearchForMaxIndexOfValueAtMost(72))
        assertEquals(5, l.binarySearchForMinIndexOfValueAtLeast(73))
        assertEquals(5, l.binarySearchForMinIndexOfValueAtLeast(84))
        assertEquals(5, l.binarySearchForMinIndexOfValueAtLeast(85))
        assertEquals(7, l.binarySearchForMinIndexOfValueAtLeast(113))
        assertEquals(8, l.binarySearchForMinIndexOfValueAtLeast(999))
    }
}

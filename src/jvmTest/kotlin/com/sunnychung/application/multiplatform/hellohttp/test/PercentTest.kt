package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.extension.atPercent
import kotlin.test.Test
import kotlin.test.assertEquals

class PercentTest {
    @Test
    fun atPercentEvenSize() {
        val list = (1..100).toList()
        assertEquals(50.5, list.atPercent(50))
        assertEquals(100.0, list.atPercent(100))
        assertEquals(99.0, list.atPercent(99))
        assertEquals(1.0, list.atPercent(0))
    }

    @Test
    fun atPercentOldSize() {
        val list = (1..99).toList()
        assertEquals(50.0, list.atPercent(50))
        assertEquals(99.0, list.atPercent(100))
        assertEquals(98.0, list.atPercent(99))
        assertEquals(1.0, list.atPercent(0))
    }

    @Test
    fun atPercentInTwoElements() {
        val list = (1..2).toList()
        assertEquals(1.5, list.atPercent(50))
        assertEquals(2.0, list.atPercent(100))
        assertEquals(2.0, list.atPercent(99))
        assertEquals(1.0, list.atPercent(0))
    }

    @Test
    fun atPercentInOneElement() {
        val list = (1..1).toList()
        assertEquals(1.0, list.atPercent(50))
        assertEquals(1.0, list.atPercent(100))
        assertEquals(1.0, list.atPercent(99))
        assertEquals(1.0, list.atPercent(0))
    }

    @Test
    fun atPercentInEmptyList() {
        val list = emptyList<Long>()
        assertEquals(0.0, list.atPercent(50))
        assertEquals(0.0, list.atPercent(100))
        assertEquals(0.0, list.atPercent(99))
        assertEquals(0.0, list.atPercent(0))
    }
}

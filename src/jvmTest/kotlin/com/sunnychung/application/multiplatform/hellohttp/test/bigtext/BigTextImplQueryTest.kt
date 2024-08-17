package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class BigTextImplQueryTest {

    @Test
    fun findLineString() {
        val lines = ((1..12) + (0 .. 23) + (13 .. 1029)).map { "${('0' + it % 10).toString().repeat(it)}\n" }
        val s = lines.joinToString("")
        val t = BigTextImpl(chunkSize = 64).apply {
            append(s)
        }
        lines.forEachIndexed { i, line ->
            val result = t.findLineString(i)
            assertEquals(line, result)
        }
    }

    @Test
    fun findLineStringInGiantString() {
        listOf(16391, 32781).forEach { upperBound ->
            val lines = ((1..12) + (0..23) + (13..upperBound)).map { "${('0' + it % 10).toString().repeat(it)}\n" }
            val s = lines.joinToString("")
            val t = BigTextImpl(chunkSize = 2 * 1024 * 1024).apply {
                append(s)
            }
            lines.forEachIndexed { i, line ->
                val result = t.findLineString(i)
                assertEquals(line, result)
            }
        }
    }

    @Test
    fun findLineStringInEvenSizedLines() {
        // each line has exactly 4 characters including '\n'
        val lines = (0 .. 10_000_004).map { "${(it % 1000).toString().padStart(3, '0')}\n" }
        val s = lines.joinToString("")
        val t = BigTextImpl(chunkSize = 64).apply {
            append(s)
        }
        lines.forEachIndexed { i, line ->
            val result = t.findLineString(i)
            assertEquals(line, result)
        }
    }

    @Test
    fun findLineStringWithoutNLAtTheEnd() {
        // each line has exactly 4 characters including '\n'
        val s = "abcdefgh\nijk\nlm"
        val t = BigTextImpl(chunkSize = 64).apply {
            append(s)
        }
        s.split("\n").forEachIndexed { i, line ->
            val result = t.findLineString(i)
            assertEquals("$line${if (i < 2) "\n" else "" }", result)
        }
    }

    @Test
    fun findLineStringWithNLAtTheEnd() {
        // each line has exactly 4 characters including '\n'
        val s = "abcdefgh\nijk\nlm\n"
        val t = BigTextImpl(chunkSize = 64).apply {
            append(s)
        }
        (0..3).forEachIndexed { i, line ->
            val result = t.findLineString(i)
            val expected = when (i) {
                0 -> "abcdefgh\n"
                1 -> "ijk\n"
                2 -> "lm\n"
                3 -> ""
                else -> throw IllegalArgumentException()
            }
            assertEquals(expected, result)
        }
    }

    @Test
    fun findLineStringInFullOfEmptyLines() {
        // each line has exactly 4 characters including '\n'
        val s = "\n".repeat(10_000_009)
        val t = BigTextImpl(chunkSize = 64).apply {
            append(s)
        }
        println("[findLineStringInFullOfEmptyLines] initialized")
        (0 .. 10_000_009).forEachIndexed { i, line ->
            val result = t.findLineString(i)
            assertEquals(if (i < 10_000_009) "\n" else "", result)
        }
    }
}

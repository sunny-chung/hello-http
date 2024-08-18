package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random
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

    @Test
    fun findLineStringAfterInserts() {
        val lines1 = ((1..12) + (0 .. 23) + (13 .. 1029)).map { "${('0' + it % 10).toString().repeat(it)}\n" }.joinToString("")
        val lines2 = ((1..200)).map { "${('a' + it % 10).toString().repeat(it)}\n" }.joinToString("")
        val lines3 = "AAAA\nB\nCCC\n"
        val t = BigTextImpl(chunkSize = 64).apply {
            append(lines1)
            insertAt((lines1.length * 0.4 - 1).toInt(), lines2)
            insertAt(0, lines3)
        }
        var s = lines1
        s = s.insert((lines1.length * 0.4 - 1).toInt(), lines2)
        s = s.insert(0, lines3)
        val splitted = s.split("\n")
        splitted.forEachIndexed { i, line ->
            val result = t.findLineString(i)
            assertEquals(if (i == splitted.lastIndex) line else "$line\n", result)
        }
    }

    @Test
    fun findLineStringAfterRemoves() {
        val lines1 = ((1..12) + (0 .. 23) + (13 .. 1029)).map { "${('0' + it % 10).toString().repeat(it)}\n" }.joinToString("")
        val pos1 = (lines1.length * 0.4 - 1).toInt()
        val pos2 = (lines1.length * 0.7).toInt()
        val pos3 = (lines1.length * 0.3).toInt()
        val t = BigTextVerifyImpl(chunkSize = 64).apply {
            append(lines1)
            delete(pos1, pos1 + 100000)
            delete(pos2, pos2 + 58888)
            delete(0, 37777)
            delete(pos3, pos3 + 45678)
            delete(pos1, pos1 + 19)
        }
        val s = t.stringImpl.fullString()
        println("len = ${s.length}")
        val splitted = s.split("\n")
        splitted.forEachIndexed { i, line ->
            val result = t.bigTextImpl.findLineString(i)
            assertEquals(if (i == splitted.lastIndex) line else "$line\n", result)
        }
    }

    @Test
    fun findLineStringAfterRemoves2() {
        val lines = ((1..12)/* + (0 .. 23) + (13 .. 1029)*/).map { "${('0' + it % 10).toString().repeat(it)}\n" }.joinToString("")
        val t = BigTextVerifyImpl(chunkSize = 65536).apply {
            append(lines)
            delete(0, 18)
        }
        val s = t.stringImpl.fullString()
        println("len = ${s.length}")
        val splitted = s.split("\n")
        splitted.forEachIndexed { i, line ->
            val result = t.bigTextImpl.findLineString(i)
            assertEquals(if (i == splitted.lastIndex) line else "$line\n", result)
        }
    }

    fun generateString(length: Int): String {
        return "${('0' + length % 10).toString().repeat(length)}\n"
    }

    @Test
    fun findLineStringAfterSomeInsertsAndRemoves() {
        val lines1 = ((1..12) + (0 .. 23) + (13 .. 1029)).map { generateString(it) }.joinToString("")
        val pos1 = (lines1.length * 0.4 - 1).toInt()
        val pos2 = (lines1.length * 0.7).toInt()
        val pos3 = (lines1.length * 0.3).toInt()
        val t = BigTextVerifyImpl(chunkSize = 64).apply {
            append(lines1)
            delete(pos1, pos1 + 100000)
            delete(pos2, pos2 + 58888)
            insertAt(pos2 - 9, generateString(50))
            delete(0, 37777)
            delete(pos3, pos3 + 45678)
            insertAt(pos3 - 9, generateString(50))
            insertAt(0, generateString(29))
            delete(pos1, pos1 + 19)
        }
        val s = t.stringImpl.fullString()
        println("len = ${s.length}")
        val splitted = s.split("\n")
        splitted.forEachIndexed { i, line ->
            val result = t.bigTextImpl.findLineString(i)
            assertEquals(if (i == splitted.lastIndex) line else "$line\n", result)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 64 * 1024, 2 * 1024 * 1024])
    fun findLineStringAfterMoreInsertsAndRemoves(chunkSize: Int) {
        val t = BigTextVerifyImpl(chunkSize = chunkSize)
        t.append(generateString(12_345_678))
        repeat(4000) {
            val len = when (random(0, 100)) {
                in 0 .. 49 -> random(0, 20)
                in 50 .. 74 -> random(20, 100)
                in 75 .. 84 -> random(100, 1000)
                in 85 .. 94 -> random(1000, 10000)
                in 95 .. 98 -> random(10000, 100000)
                in 99 .. 99 -> random(100000, 1000000)
                else -> throw IllegalStateException()
            }
            when (random(0, 6)) {
                0 -> t.append(generateString(len))
                1 -> t.insertAt(0, generateString(len))
                2 -> t.insertAt(random(0, t.length), generateString(len))
                3 -> t.delete(0, minOf(len, t.length))
                4 -> t.delete(maxOf(t.length - minOf(len, t.length), 0), t.length)
                5 -> {
                    val len = minOf(len, maxOf(t.length - minOf(len, t.length), 0))
                    val start = random(0, len)
                    t.delete(start, start + len)
                }
            }
        }
        val splitted = t.stringImpl.fullString().split("\n")
        splitted.forEachIndexed { i, line ->
            val result = t.bigTextImpl.findLineString(i)
            assertEquals(if (i == splitted.lastIndex) line else "$line\n", result)
        }
    }
}

private fun random(from: Int, toExclusive: Int): Int {
    if (toExclusive == from) {
        return 0
    }
    return Random.nextInt(from, toExclusive)
}

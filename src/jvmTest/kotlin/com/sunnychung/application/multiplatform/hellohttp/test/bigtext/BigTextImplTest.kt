package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.isD
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Some cases in this test may look very specific, but they had consistently failed before.
 */
class BigTextImplTest {

    companion object {
        @JvmStatic
        fun argumentsOfMultipleRandomDeletes(): Array<IntArray>
            = arrayOf(64, 1024, 16 * 1024, 64 * 1024, 128 * 1024, 512 * 1024) // chunk size
                .flatMap { chunkSize ->
                    arrayOf(64, 100, 200, 257, 1024, 64 * 1024, 1024 * 1024, 5 * 1024 * 1024, 12 * 1024 * 1024 + 3) // initial length
                        .map { initialLength ->
                            intArrayOf(chunkSize, initialLength)
                        }
                }
                .toTypedArray()
    }

    @Test
    fun appendMultipleShort() {
        val t = BigTextVerifyImpl()
        t.append("abc")
        t.append("defgh")
        t.append("ijk")
        t.printDebug()
        assertEquals(1, t.tree.size())
    }

    @Test
    fun appendMultipleLong() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(60))
        t.append("b".repeat(50))
        t.append("c".repeat(100))
        t.printDebug()
        assertEquals(4, t.tree.size())
    }

    @Test
    fun insertBetweenChunks() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64))
        t.append("b".repeat(64))
        t.insertAt(64, "1".repeat(64))
        t.append("c".repeat(100))
        t.printDebug()
        assertEquals(5, t.tree.size())
    }

    @Test
    fun insertMultipleBetweenChunks() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64))
        t.append("b".repeat(64))
        t.insertAt(64, "1".repeat(128))
        t.append("c".repeat(100))
        t.insertAt(64 * 4, "2".repeat(128))
        t.insertAt(t.length, "3".repeat(70))
        t.printDebug()
        assertEquals(11, t.tree.size())
    }

    @Test
    fun insertWithinChunks1() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64))
        t.append("b".repeat(64))
        t.insertAt(50, "1".repeat(20))
        t.append("c".repeat(30))
        t.printDebug()
        assertEquals(5, t.tree.size())
    }

    @Test
    fun insertWithinChunks2() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64))
        t.append("b".repeat(64))
        t.insertAt(70, "1".repeat(20))
        t.append("c".repeat(30))
        t.printDebug()
        assertEquals(5, t.tree.size())
    }

    @Test
    fun insertMultipleWithinChunks1() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64)) // 0 ..< 50, 50 ..< 64
        t.append("b".repeat(64)) // 64 ..< 128
        t.insertAt(50, "1".repeat(150)) // 128 ..< 192, 192 ..< 256, 256 ..< 278
        t.append("c".repeat(30)) // 278 ..< 308
        t.printDebug()
        assertEquals(7, t.tree.size())
    }

    @Test
    fun insertMultipleWithinChunks2() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(64))
        t.append("b".repeat(64))
        t.insertAt(70, "1".repeat(150))
        t.append("c".repeat(30))
        t.printDebug()
        assertEquals(7, t.tree.size())
    }

    @Test
    fun empty() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.printDebug()
        assertEquals(0, t.tree.size())
        assertEquals(0, t.buildString().length)
        assertEquals(0, t.length)
    }

    @Test
    fun insertEmpty() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("abcd")
        t.append("")
        t.insertAt(2, "")
        t.insertAt(1, "")
        t.insertAt(1, "")
        t.insertAt(4, "")
        t.printDebug()
        assertEquals(1, t.tree.size())
        assertEquals(4, t.buildString().length)
        assertEquals(4, t.length)
    }

    @Test
    fun insertLonger() {
        val t = BigTextVerifyImpl(chunkSize = 16)
        t.append((0..99).map { 'a' + (it % 26) }.joinToString(""))
        t.insertAt(60, (0..99).map { 'A' + (it % 26) }.joinToString(""))
        t.append((0..39).map { '0' + (it % 10) }.joinToString(""))
        t.printDebug()
        assertEquals(240 / 16, t.buffers.size)
        assertEquals((100 / 16 + 1) + 1 + (100 / 16 + 1) + (40 / 16 + 1), t.tree.size())
        assertEquals(240, t.length)
        assertEquals(240, t.buildString().length)
    }

    @Test
    fun insertALotLonger() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append((0..9999).map { 'a' + (it % 26) }.joinToString(""))
        t.insertAt(4600, (0..9999).map { 'A' + (it % 26) }.joinToString(""))
        t.append((0..9999).map { '0' + (it % 10) }.joinToString(""))
        t.printDebug()
        assertEquals(30000 / 64 + 1, t.buffers.size)
//        assertEquals((30000 / 64 + 1) + 1 + (30000 / 64 + 1) + (30000 / 64 + 1), t.tree.size())
        assertEquals(30000, t.length)
        assertEquals(30000, t.buildString().length)
    }

    @Test
    fun insertStringOfMillionChars() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(1000000))
        t.insertAt(300000, "b".repeat(1000000))
        t.append("c".repeat(3000000))
//        t.printDebug()
        assertEquals(5000000 / 64, t.buffers.size)
        assertEquals((1000000 / 64 + 1) + (1000000 / 64 + 1) + (3000000 / 64 + 1) - 2, t.tree.size())
        assertEquals(5000000, t.length)
        assertEquals(5000000, t.buildString().length)
    }

//    @Test
//    fun appendNearEndOfChunk() {
//        val t = BigTextVerifyImpl(chunkSize = 64)
//        t.append("-".repeat(6400))
//        t.append("a".repeat(64 + 41))
//        t.append("b".repeat(21))
//        t.append("c".repeat(7))
////        t.printDebug()
//        val len = 6400 + 60 + 7
//        assertEquals(len / 64 + 1, t.buffers.size)
//        assertEquals(len, t.length)
//        assertEquals(len, t.fullString().length)
//    }

    @Test
    fun insertAtStart() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(339))
        t.insertAt(0, "b".repeat(46))
        t.printDebug()
        val len = 339 + 46
        assertEquals(len / 64 + 1, t.buffers.size)
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun insertMoreAtStart() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(150))
        t.insertAt(0, "B".repeat(13))
        t.insertAt(0, "C".repeat(62))
        t.insertAt(0, "D".repeat(58))
        t.insertAt(0, "E".repeat(7))
        t.insertAt(0, "F".repeat(90))
        t.insertAt(0, "G".repeat(64))
        t.insertAt(0, "H".repeat(129))
        t.printDebug()
        val len = 150 + 13 + 62 + 58 + 7 + 90 + 64 + 129
        assertEquals(len / 64 + 1, t.buffers.size)
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun insertAtFixedPosition() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append("a".repeat(339))
        t.insertAt(29, "E".repeat(46))
        t.insertAt(29, "D".repeat(46))
        t.insertAt(29, "C".repeat(46))
        t.insertAt(29, "B".repeat(46))
        t.printDebug()
        val len = 339 + 46 * 4
        assertEquals(len / 64 + 1, t.buffers.size)
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun insertAtVariousPositions() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append((0..653).map { 'a' + (it % 26) }.joinToString(""))
        t.insertAt(80, (0..79).map { 'A' + (it % 26) }.joinToString(""))
        t.insertAt(0, (0..25).map { '0' + (it % 10) }.joinToString(""))
        t.printDebug()
        val len = 654 + 80 + 26
        assertEquals(len / 64 + 1, t.buffers.size)
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    /**
     * Benchmark:
     *
     *  Chunk Size  | Completion Time (s)
     *  64B         | 13.3
     *  1KB         | 6.9
     *  16KB        | 9.2
     *  64KB        | 12.9
     *  128KB       | 16.0
     *  512KB       | 49.5
     *  2MB         | 194
     */
    @ParameterizedTest
    @ValueSource(ints = [64, 1024, 16 * 1024, 64 * 1024, 128 * 1024, 512 * 1024])
    fun multipleRandomInserts(chunkSize: Int) {
        val t = BigTextVerifyImpl(chunkSize = chunkSize)
        var totalLength = 0
        repeat(2000) {
            println("it #$it")
            val length = when (random(0, 6)) {
                0 -> 0
                1 -> random(1, 20)
                2 -> random(20, 100)
                3 -> random(100, 400)
                4 -> random(400, 4000)
                5 -> random(4000, 100000)
                else -> throw IllegalStateException()
            }
            val newString = if (length > 0) {
                val startChar: Char = if (it % 2 == 0) 'A' else 'a'
                (0 until length - 1).asSequence().map { (startChar + it % 26).toString() }.joinToString("") + "|"
            } else {
                ""
            }
            when (random(0, 6)) {
                0 -> t.append(newString)
                1 -> t.insertAt(t.length, newString)
                2 -> t.insertAt(0, newString)
                in 3..5 -> t.insertAt(if (t.length > 0) random(0, t.length) else 0, newString)
                else -> throw IllegalStateException()
            }
            totalLength += length
            assertEquals(totalLength, t.length)
        }
    }

    @Test
    fun deleteWithinChunk() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append((0 until 64 * 3).map { 'a' + (it % 26) }.joinToString(""))
        t.delete(64 * 2 + 10, 64 * 2 + 30)
        t.delete(64 * 1 + 10, 64 * 1 + 30)
        t.delete(64 * 0 + 10, 64 * 0 + 30)
        val len = 64 * 3 - 20 * 3
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun deleteAmongChunks() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append((0 until 64 * 10).map { 'a' + (it % 26) }.joinToString(""))
        val d1range = 64 * 4 + 10 until 64 * 7 + 30
        isD = true
        t.delete(d1range)
        val d2range = 64 * 8 + 10 - d1range.length until 64 * 9 + 47 - d1range.length
        t.delete(d2range)
        val d3range = 64 * 1 + 10 until 64 * 3 + 30
        t.delete(d3range)
        val len = 64 * 10 - d1range.length - d2range.length - d3range.length
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun deleteAtBeginning() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        t.append((0 until 654).map { 'a' + (it % 26) }.joinToString(""))
        t.delete(0 .. 19)
        t.delete(0 .. 19)
        t.delete(0 .. 19)
        t.delete(0 .. 19)
        t.delete(0 .. 29)
        val len = 654 - 20 * 4 - 30
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @ParameterizedTest
    @ValueSource(ints = [60, 64, 67, 128, 192, 640, 6483, 64_000_000, 64_000_001, 64 * 1024 * 1024])
    fun deleteWholeThing(length: Int) {
//        val t = BigTextVerifyImpl(chunkSize = 64)
        val t = BigTextImpl(chunkSize = 64)
        t.append("X".repeat(length))
        assertEquals(length, t.length)
        if (length == 640) isD = true
        t.delete(0 until t.length)
        assertEquals(0, t.length)
        assertEquals(0, t.buildString().length)
        assertEquals(0, t.tree.size())
    }

    @Test
    fun deleteWithinLongerChunk() {
        val t = BigTextVerifyImpl(chunkSize = 1024)
        t.append((0 until 1024).map { 'a' + (it % 26) }.joinToString(""))
        t.delete(300 .. 319)
        t.delete(300 .. 800)
        isD = true
        t.delete(0 .. 279)
        t.delete(13 .. 69)
        val len = 1024 - 20 - 501 - 280 - 57
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @Test
    fun deleteSomeLeftChunks() {
        val t = BigTextVerifyImpl(chunkSize = 64)
//        t.append((0 until 16384).map { 'a' + (it % 26) }.joinToString(""))
//        t.delete(3443 .. 4568)
        t.append((0 until 1024).map { 'a' + (it % 26) }.joinToString(""))
        t.delete(343 .. 456)
        val len = 1024 - (457 - 343)
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 12 * 1024, 6 * 1024 * 1024])
    fun deleteRepeatedlyAtBeginning(initialLength: Int) {
        val t = BigTextVerifyImpl(chunkSize = 64)
        var len = initialLength
//        t.append((0 until len).map { 'a' + (it % 26) }.joinToString(""))
        t.append((0 until len).map { 'a' + Random.nextInt(26) }.joinToString(""))
        repeat(1200) {
            if (t.length >= 12) {
                len += t.delete(0..11)
                if (t.length == 76) isD = true
            }
        }
        assertEquals(len, t.length)
        assertEquals(len, t.buildString().length)
    }

    @ParameterizedTest
    @MethodSource("argumentsOfMultipleRandomDeletes")
    fun multipleRandomDeletes(arguments: IntArray) {
        val chunkSize = arguments[0]
        val initialLength = arguments[1]

        val t = BigTextVerifyImpl(chunkSize = chunkSize)
        t.append((0 until initialLength).map { 'a' + (it % 26) }.joinToString(""))
        var totalLength = initialLength
        repeat(3000) {
            println("it #$it")
            val length = when (random(0, 6)) {
                0 -> 0
                1 -> random(1, 20)
                2 -> random(20, 100)
                3 -> random(100, 400)
                4 -> random(400, if (initialLength <= 1024 * 1024) 2000 else 4000)
                5 -> random(
                    if (initialLength <= 1024 * 1024) 2000 else 4000,
                    if (initialLength <= 1024 * 1024) 12000 else 60000
                )
                else -> throw IllegalStateException()
            }
            val textLengthChange = when (random(0, 5)) {
                in 0 .. 2 -> if (t.length > 0) {
                    val p1 = random(0, t.length)
                    val p2 = p1 + minOf(length, t.length - p1) // p1 + p2 <= t.length
                    t.delete(minOf(p1, p2), maxOf(p1, p2))
                } else {
                    t.delete(0, 0)
                }
                3 -> t.delete(0, random(0, minOf(length, t.length))) // delete from start
                4 -> t.delete(t.length - random(0, minOf(length, t.length)), t.length) // delete from end
                else -> throw IllegalStateException()
            }
            totalLength += textLengthChange
            assertEquals(totalLength, t.length)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 1024, 16 * 1024, 64 * 1024, 512 * 1024, 2 * 1024 * 1024])
    fun multipleRandomOperations(chunkSize: Int) {
        val t = BigTextVerifyImpl(chunkSize = chunkSize)
        var totalLength = 0
        repeat(5000) {
            println("it #$it")
            val length = when (random(0, 6)) {
                0 -> 0
                1 -> random(1, 20)
                2 -> random(20, 100)
                3 -> random(100, 400)
                4 -> random(400, 4000)
                5 -> random(4000, 100000)
                else -> throw IllegalStateException()
            }
            val newString = if (length > 0) {
                val startChar: Char = if (it % 2 == 0) 'A' else 'a'
                (0 until length - 1).asSequence().map { (startChar + it % 26).toString() }.joinToString("") + "|"
            } else {
                ""
            }
            val textLengthChange = when (random(0, 15)) {
                in 0 .. 1 -> t.append(newString)
                2 -> t.insertAt(t.length, newString)
                3 -> t.insertAt(0, newString)
                in 4..8 -> t.insertAt(random(0, t.length), newString)
                in 9..11 -> if (t.length > 0) {
                    val p1 = random(0, t.length)
                    val p2 = minOf(t.length, p1 + random(0, t.length - p1)) // p1 + p2 <= t.length
                    t.delete(minOf(p1, p2), maxOf(p1, p2))
                } else {
                    t.delete(0, 0)
                }
                12 -> t.delete(0, random(0, minOf(length, t.length))) // delete from start
                13 -> t.delete(t.length - random(0, minOf(length, t.length)), t.length) // delete from end
                14 -> t.delete(0, t.length) // delete whole string
                else -> throw IllegalStateException()
            }
            totalLength += textLengthChange
            assertEquals(totalLength, t.length)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 1024, 64 * 1024])
    fun exhaustSubstring(chunkSize: Int) {
        (0..1025).forEach { length -> // O(length ^ 3) * O(verify), where O(verify) = O(length)
            val t = BigTextVerifyImpl(chunkSize = chunkSize)
            t.append((0 until length).map { 'a' + Random.nextInt(26) }.joinToString(""))
            (0 .. length - 1).forEach { i ->
                (i .. length).forEach { j ->
//                    println("substring $i, $j")
                    val ss = t.substring(i, j) // the substring content is verified by BigTextVerifyImpl
                    assertEquals(j - i, ss.length)
                }
            }
        }
    }

    /**
     * Benchmark:
     *
     *  Chunk Size  | Completion Time (s)
     *  64B         | 86
     *  1KB         | 49.0
     *  64KB        | 41.4
     *  512KB       | 41.7
     *  2MB         | 38.2
     */
    @ParameterizedTest
    @ValueSource(ints = [64, 1024, 64 * 1024, 512 * 1024, 2 * 1024 * 1024])
    fun randomLongSubstring(chunkSize: Int) {
        repeat(500) { // 500 * O(length) * 1000
            println("it #$it")
            val length = when (random(0, 5)) {
                0 -> random(1026, 4097)
                1 -> random(4097, 65537)
                2 -> random(65537, 100 * 1024 + 1)
                3 -> random(100 * 1024 + 1, 1 * 1024 * 1024 + 1)
                4 -> random(1 * 1024 * 1024 + 1, 16 * 1024 * 1024)
                else -> throw IllegalStateException()
            }
            val t = BigTextVerifyImpl(chunkSize = chunkSize)
            t.append((0 until length).map { 'a' + Random.nextInt(26) }.joinToString(""))

            repeat(1000) {
                val p1 = random(0, length)
                val pl: Int = when (random(0, 6)) {
                    0 -> t.length * 5 / 100
                    1 -> t.length * 16 / 100
                    2 -> t.length * 34 / 100
                    3 -> t.length * 63 / 100
                    4 -> t.length * 87 / 100
                    5 -> t.length
                    else -> throw IllegalStateException()
                }
                val p2 = minOf(length, p1 + random(0, maxOf(2, pl - p1))) // p1 + p2 <= t.length
//                println("L = $length. ss $p1 ..< $p2")
                val ss = t.substring(p1, p2)
                assertEquals(p2 - p1, ss.length)
            }
        }
    }
}

private fun random(from: Int, toExclusive: Int): Int {
    if (toExclusive == from) {
        return 0
    }
    return Random.nextInt(from, toExclusive)
}

package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextVerifyImpl
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BigTextImplTest {

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
        assertEquals(0, t.fullString().length)
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
        assertEquals(4, t.fullString().length)
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
        assertEquals(240, t.fullString().length)
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
        assertEquals(30000, t.fullString().length)
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
        assertEquals(5000000, t.fullString().length)
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
        assertEquals(len, t.fullString().length)
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
        assertEquals(len, t.fullString().length)
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
        assertEquals(len, t.fullString().length)
    }

    @Test
    fun multipleRandomInserts() {
        val t = BigTextVerifyImpl(chunkSize = 64)
        var totalLength = 0
        repeat(2000) {
            println("it #$it")
            val length = when (Random.nextInt(0, 6)) {
                0 -> 0
                1 -> Random.nextInt(1, 20)
                2 -> Random.nextInt(20, 100)
                3 -> Random.nextInt(100, 400)
                4 -> Random.nextInt(400, 4000)
                5 -> Random.nextInt(4000, 100000)
                else -> throw IllegalStateException()
            }
            val newString = if (length > 0) {
                val startChar: Char = if (it % 2 == 0) 'A' else 'a'
                (0 until length - 1).asSequence().map { (startChar + it % 26).toString() }.joinToString("") + "|"
            } else {
                ""
            }
            when (Random.nextInt(0, 6)) {
                0 -> t.append(newString)
                1 -> t.insertAt(t.length, newString)
                2 -> t.insertAt(0, newString)
                in 3..5 -> t.insertAt(if (t.length > 0) Random.nextInt(0, t.length) else 0, newString)
                else -> throw IllegalStateException()
            }
            totalLength += length
            assertEquals(totalLength, t.length)
        }
    }
}

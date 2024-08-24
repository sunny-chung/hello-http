package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.MonospaceTextLayouter
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.isD
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.logL
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private var random: Random = Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BigTextImplLayoutTest {

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun layoutOneLineJustFit(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }

        assertEquals(testString.count { it == '<' } + 1, t.numOfRows)
        testString.chunked(10).forEachIndexed { index, s ->
            assertEquals(s, t.findRowString(index))
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun layoutOneLineWithExtraSpace(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10 + 13.4567f)
        }

        assertEquals(testString.count { it == '<' } + 1, t.numOfRows)
        testString.chunked(10).forEachIndexed { index, s ->
            assertEquals(s, t.findRowString(index))
        }
    }

    fun verifyBigTextImplAgainstTestString(testString: String, bigTextImpl: BigTextImpl, softWrapAt: Int = 10) {
        val splitted = testString.split("\n")
        val expectedRows = splitted.flatMapIndexed { index: Int, str: String ->
//            val str = if (index < splitted.lastIndex) "$s\n" else s
            str.chunked(softWrapAt).let { ss ->
                val ss = if (ss.isEmpty()) listOf(str) else ss
                if (index < splitted.lastIndex) {
                    ss.mapIndexed { i, s ->
                        if (i == ss.lastIndex) {
                            "$s\n"
                        } else {
                            s
                        }
                    }
                } else {
                    ss
                }
            }
        }
//        println("exp $expectedRows")
        try {
            assertEquals(expectedRows.size, bigTextImpl.numOfRows)
            expectedRows.forEachIndexed { index, s ->
                assertEquals(s, bigTextImpl.findRowString(index))
            }
        } catch (e: Throwable) {
            bigTextImpl.printDebug("ERROR")
            println("exp $expectedRows")
            throw e
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun layoutMultipleLines1(chunkSize: Int) {
        val testString = "abcd\n1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.\nABCDEFGHIJ<BCDEFGHIJ<xyz\nabcd"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = testString, bigTextImpl = t)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun layoutMultipleLines2(chunkSize: Int) {
        val testString = "abcd\n1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.\nABCDEFGHIJ<BCDEFGHIJ\nabcd\n\n"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = testString, bigTextImpl = t)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun layoutMultipleLines3(chunkSize: Int) {
        val testString = "\n\n\n\nabcdefghij<\n1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.\nABCDEFGHIJ<BCDEFGHIJ\nabcd\n\n"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = testString, bigTextImpl = t)
    }

    @Test
    fun oneRowOnly() {
        val testString = "1234567890"
        val t = BigTextImpl(chunkSize = 64).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }

        assertEquals(1, t.numOfRows)
        assertEquals(testString, t.findRowString(0))
    }

    @Test
    fun oneRowAcrossMultipleChunks() {
        val testString = "1234567890".repeat(10)
        val t = BigTextImpl(chunkSize = 16).apply {
            append(testString)
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 100)
        }

        assertEquals(1, t.numOfRows)
        assertEquals(testString, t.findRowString(0))
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64])
    fun insertTriggersRelayout1(chunkSize: Int) {
        val initial = "abcd\nABCDEFGHIJ<BCDEFGHIJ<xyz\nabcd"
        val add = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.\n"
        val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
            append(initial)
            bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            bigTextImpl.setContentWidth(16f * 10 + 1.23f)
            printDebug("after 1st layout")
        }
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(5, add)
        t.printDebug("after relayout")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64])
    fun insertTriggersRelayout2(chunkSize: Int) {
        val initial = "abcd\n1234567ABCDEFGHIJ<BCDEFGHIJ<xyz\nabcd"
        val add = "890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.\n"
        val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
            append(initial)
            bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            bigTextImpl.setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(12, add)
        t.printDebug("after relayout")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun insertTriggersRelayout3(chunkSize: Int) {
        val initial = "abcd\nwww\n\n\n1234567890<234567890<bcdefghEFGHIJ<BCDEFGHIJ<xyz\nabcd\nXYZ\n\n"
        val add = "ij<BCDEFGHIJ<row break< should h<appen her<e.\nABCD"
        val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
            append(initial)
            bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            bigTextImpl.setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(39, add)
        t.printDebug("after relayout")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 64, 16, 65536, 1 * 1024 * 1024])
    fun insertLongLines(chunkSize: Int) {
        random = Random(123) // use a fixed seed for easier debug
        val initial = randomString(10000, isAddNewLine = true)
        val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
            append(initial)
            bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            bigTextImpl.setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(1677, randomString(1000, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 1")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(4989, randomString(2000, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 2")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(8912, randomString(1000, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 3")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 64, 16, 65536, 1 * 1024 * 1024])
    fun insertMultipleTimesWithOverlaps(chunkSize: Int) {
        random = Random(123) // use a fixed seed for easier debug
        val initial = randomString(999, isAddNewLine = true)
        val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
            append(initial)
            bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            bigTextImpl.setContentWidth(16f * 10 + 1.23f)
        }
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(467, randomString(30, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 1")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(491, randomString(35, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 2")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(112, randomString(500, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 3")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
        t.insertAt(480, randomString(399, isAddNewLine = false) + "\n")
        t.printDebug("after relayout 4")
        verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl)
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 64, 16, 65536, 1 * 1024 * 1024])
    @Order(Integer.MAX_VALUE - 300)
    fun insertAtBeginning(chunkSize: Int) {
        random = Random(123456) // use a fixed seed for easier debug
        listOf(10, 25, 37, 100, 1000, 10000).forEach { softWrapAt ->
            val initial = randomString(666, isAddNewLine = true)
            val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
                append(initial)
                bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
                bigTextImpl.setContentWidth(16f * softWrapAt + 1.23f)
            }
            listOf(15, 4, 1, 1, 2, 8, 16, 19, 200, 1235, 2468, 10001, 257).forEachIndexed { i, it ->
                t.insertAt(0, randomString(it, isAddNewLine = false) + "\n")
                t.printDebug("after relayout $softWrapAt, $i")
                verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl, softWrapAt = softWrapAt)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 64, 16, 65536, 1 * 1024 * 1024])
    @Order(Integer.MAX_VALUE - 200) // This test is time-consuming. Run at 2nd last!
    fun insertNewLines(chunkSize: Int) {
        random = Random(1234566) // use a fixed seed for easier debug
        listOf(10, 37, 100, 1000).forEach { softWrapAt ->
            val initial = randomString(666, isAddNewLine = true)
            val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
                append(initial)
                bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
                bigTextImpl.setContentWidth(16f * softWrapAt + 1.23f)
            }
            repeat(1000) { i ->
                println("Iterate $softWrapAt, $i")
                val length = when (random.nextInt(100)) {
                    in 0..9 -> 0
                    in 10..39 -> 1
                    in 40..59 -> random.nextInt(2, 6)
                    in 60..74 -> random.nextInt(6, 15)
                    in 75..86 -> random.nextInt(15, 100)
                    in 87..95 -> random.nextInt(100, 350)
                    in 96..98 -> random.nextInt(350, 1000)
                    in 99..99 -> random.nextInt(1000, 10000)
                    else -> throw IllegalStateException()
                }
                val pos = when (random.nextInt(10)) {
                    in 0..1 -> 0
                    in 2..3 -> t.length
                    else -> random.nextInt(t.length + 1)
                }
                t.insertAt(pos, "\n".repeat(length))
                verifyBigTextImplAgainstTestString(testString = t.stringImpl.fullString(), bigTextImpl = t.bigTextImpl, softWrapAt = softWrapAt)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [256, 64, 16, 65536, 1 * 1024 * 1024])
    @Order(Integer.MAX_VALUE - 100) // This test is pretty time-consuming. Run at the last!
    fun manyInserts(chunkSize: Int) { //if (chunkSize != 256) return
        random = Random(1234567) // use a fixed seed for easier debug
        repeat(10) { repeatIt ->
            listOf(10, 37, 100, 1000, 10000).forEach { softWrapAt ->
                val initial = randomString(random.nextInt(1000), isAddNewLine = true)
                val t = BigTextVerifyImpl(chunkSize = chunkSize).apply {
                    append(initial)
                    bigTextImpl.setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
                    bigTextImpl.setContentWidth(16f * softWrapAt + 1.23f)
                }
                val numInsertTimes = if (repeatIt > 0) {
                    when (softWrapAt) {
                        in 0 .. 30 -> 407
                        in 31 .. 200 -> 607
                        else -> 1007
                    }
                } else {
                    1007
                }
                repeat(numInsertTimes) { i ->
                    val length = when (random.nextInt(100)) {
                        in 0..44 -> random.nextInt(10)
                        in 45..69 -> random.nextInt(10, 100)
                        in 70..87 -> random.nextInt(100, 1000)
                        in 88..97 -> random.nextInt(1000, 10000)
                        in 98..99 -> random.nextInt(10000, 80000)
                        else -> throw IllegalStateException()
                    }
                    val pos = when (random.nextInt(10)) {
                        in 0..1 -> 0
                        in 2..3 -> t.length
                        else -> random.nextInt(t.length + 1)
                    }
                    t.insertAt(pos, randomString(length, isAddNewLine = random.nextBoolean()) + "\n")
                    logL.d { t.inspect("after relayout $repeatIt $softWrapAt, $i") }
                    println("Iterate $repeatIt, $softWrapAt, $i")
                    if (i >= 43) {
                        isD = true
                    }
                    verifyBigTextImplAgainstTestString(
                        testString = t.stringImpl.fullString(),
                        bigTextImpl = t.bigTextImpl,
                        softWrapAt = softWrapAt
                    )
                }
            }
        }
    }

    @BeforeTest
    fun beforeEach() {
        random = Random
    }
}

fun randomString(length: Int, isAddNewLine: Boolean): String = (0 until length).joinToString("") {
    when {
        isAddNewLine && random.nextInt(100) == 0 -> "\n"
        else -> ('a' + random.nextInt(26)).toString()
    }
}

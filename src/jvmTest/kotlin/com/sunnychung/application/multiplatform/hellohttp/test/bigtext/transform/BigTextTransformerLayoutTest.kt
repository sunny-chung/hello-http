package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.BigTextVerifyImpl
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.FixedWidthCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.random
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.randomString
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.verifyBigTextImplAgainstTestString
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.MonospaceTextLayouter
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BigTextTransformerLayoutTest {

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun noTransformation(chunkSize: Int) { if (chunkSize != 16) return
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        verifyBigTextImplAgainstTestString(testString = testString, bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun inserts(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(58, "[INSERT0]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(16, "[INSERT1]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(47, "[INSERT2]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun insertLines(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(58, "[IN\nSERT0\n]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(16, "\n[INSER\nT1]")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.insertAt(47, "[INSERT2]\n")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun insertBeforeLineBreak(chunkSize: Int) {
        val testString = "1234567890<234\n<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.insertAt(14, "567890")
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    @Order(Integer.MAX_VALUE - 100) // This test is pretty time-consuming. Run at the last!
    fun manyInserts(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        random = Random(23456)

        repeat(600) {
            val pos = when (random.nextInt(10)) {
                in 0 .. 1 -> 0
                in 2 .. 3 -> v.originalLength
                else -> random.nextInt(1, v.originalLength)
            }
            val length = when (random.nextInt(10)) {
                in 0 .. 2 -> 1 + random.nextInt(3)
                in 3 .. 4 -> random.nextInt(4, 11)
                in 5 .. 6 -> random.nextInt(11, 300)
                7 -> random.nextInt(300, 1000)
                8 -> random.nextInt(1000, 10000)
                9 -> random.nextInt(10000, 100000)
                else -> throw IllegalStateException()
            }
            v.insertAt(pos, randomString(length, isAddNewLine = true))
            verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun deletes(chunkSize: Int) {
        val testString = "1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(46 .. 68)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(10 .. 19)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(30 .. 43)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun deletesLines1(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(v.length - 12 until v.length)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(20 .. 21)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(41 .. 46)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun deletesLines2(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        v.delete(42 .. 43)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(44 .. 45)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(21 .. 21)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun deletesAtTheEnd(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(originalLength - 3 until originalLength)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 5 until originalLength - 3)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 9 until originalLength - 5)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(originalLength - 10 until originalLength - 9)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(23 until originalLength - 10)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64, 16])
    fun deletesAtTheBeginning(chunkSize: Int) {
        val testString = "1234567890<234567890\n<bcdefghij<B\nCD\nE\nFGH\n\n\n\nIJ<row break< should h<appe\nn he\nr<e."
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(testString)
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)

        val originalLength = testString.length
        v.delete(0 .. 1)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(2 .. 18)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(19 .. 22)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(23 .. 41)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.delete(42 until originalLength - 7)
        verifyBigTextImplAgainstTestString(testString = v.stringImpl.buildString(), bigTextImpl = tt)

        v.bigTextImpl.printDebug()
    }
}

package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.MonospaceTextLayouter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals

class BigTextImplLayoutTest {

    @ParameterizedTest
    @ValueSource(ints = [65536, 64])
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
    @ValueSource(ints = [65536, 64])
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

    fun verifyBigTextImplAgainstTestString(testString: String, bigTextImpl: BigTextImpl) {
        val splitted = testString.split("\n")
        val expectedRows = splitted.flatMapIndexed { index: Int, str: String ->
//            val str = if (index < splitted.lastIndex) "$s\n" else s
            str.chunked(10).let { ss ->
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
        println("exp $expectedRows")
        assertEquals(expectedRows.size, bigTextImpl.numOfRows)
        expectedRows.forEachIndexed { index, s ->
            assertEquals(s, bigTextImpl.findRowString(index))
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [65536, 64])
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
    @ValueSource(ints = [65536, 64])
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
    @ValueSource(ints = [65536, 64])
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
}

package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.BigTextVerifyImpl
import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.FixedWidthCharMeasurer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.MonospaceTextLayouter
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.isD
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BigTextTransformPositionCalculatorTest {

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun noTransformation(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun transformInserts(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength
        v.insertAt(56, "AAA")
        v.verifyPositionCalculation()
        v.insertAt(7, "abcd")
        v.verifyPositionCalculation()
        v.insertAt(22, "!@#$%")
//        if (chunkSize == 64) {
//            isD = true
//        }
        v.verifyPositionCalculation()
        isD = true
        v.insertAt(21, "xyzxxyyzz")
//        if (chunkSize == 16) {
//            isD = true
//        }
        v.verifyPositionCalculation()
        v.insertAt(0, "[head]")
        v.verifyPositionCalculation()
        v.insertAt(originalLength,"[tail]")
        if (chunkSize == 16) {
            isD = true
        }
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun transformDeletes(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength
        v.delete(56 .. 63)
        v.verifyPositionCalculation()
        v.delete(65 .. 65)
        v.verifyPositionCalculation()
        v.delete(7 .. 16)
        v.verifyPositionCalculation()
        v.delete(22 .. 49)
        v.verifyPositionCalculation()
        v.delete(50 .. 55)
        v.verifyPositionCalculation()
        v.delete(0 .. 2)
        v.verifyPositionCalculation()
        v.delete(originalLength - 2 until originalLength)
        isD = true
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun transformDeleteWholeText(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength
        v.delete(0 until originalLength)
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun simpleIncrementalTransformReplaces(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

        v.replace(45 .. 52, "-+-+-")
        v.verifyPositionCalculation()

        v.replace(55 .. 63, "-+-+-")
        v.verifyPositionCalculation()

        v.replace(65 .. 68, "some relatively long string that is longer than a chunk")
        if (chunkSize == 64) isD = true
        v.verifyPositionCalculation()

        v.replace(24 .. 29, "1-to-1")
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun incrementalTransformReplacesAtEdges(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

        v.replace(0 .. 2, "-+-+-")
        v.verifyPositionCalculation()

        v.replace(originalLength - 12 until originalLength, "*-*-*")
        v.verifyPositionCalculation()

        v.replace(3 .. 3, "some relatively long string that is longer than a chunk")
        v.verifyPositionCalculation()

        v.replace(4 .. 5, "=")
        v.bigTextImpl.printDebug("After replacement '='")
        if (chunkSize == 64) isD = true
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun simpleBlockTransformReplaces(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

        v.replace(45 .. 52, "-+-+-", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        v.replace(55 .. 63, "-+-+-", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        if (chunkSize == 64) isD = true
        v.replace(65 .. 68, "some relatively long string that is longer than a chunk", BigTextTransformOffsetMapping.WholeBlock)
        if (chunkSize == 64) isD = true
        v.verifyPositionCalculation()

        v.replace(24 .. 29, "1-to-1", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun blockTransformReplacesAtEdges(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

        v.replace(0 .. 2, "-+-+-", BigTextTransformOffsetMapping.WholeBlock)
        isD = true
        v.verifyPositionCalculation()

        v.replace(originalLength - 12 until originalLength, "*-*-*", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        v.replace(3 .. 3, "some relatively long string that is longer than a chunk", BigTextTransformOffsetMapping.WholeBlock)
        isD = true
        v.verifyPositionCalculation()

        v.replace(4 .. 5, "=", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()
    }

    private fun testInsertAfterTransformReplaceAtSamePosition(chunkSize: Int, replaceMapping: BigTextTransformOffsetMapping) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

        v.replace(32 .. 36, "long replacement", replaceMapping)
        v.verifyPositionCalculation()
        v.insertAt(32, "inserted text 32")
//        isD = true
        v.verifyPositionCalculation()

        v.replace(15 .. 23, "!?", replaceMapping)
        v.verifyPositionCalculation()
        v.insertAt(15, "inserted text 15")
        isD = true
        v.verifyPositionCalculation()

        v.replace(0 .. 2, "-+-+-", replaceMapping)
        v.verifyPositionCalculation()
        v.insertAt(0, "inserted text 0")
        v.verifyPositionCalculation()

        v.replace(originalLength - 12 until originalLength, "*-*-*", replaceMapping)
        v.verifyPositionCalculation()
        v.insertAt(originalLength - 12, "inserted text ${originalLength - 12}")
        v.verifyPositionCalculation()

        v.replace(3 .. 3, "some relatively long string that is longer than a chunk", replaceMapping)
        if (chunkSize == 64) {
            isD = true
        }
        v.verifyPositionCalculation()
        v.insertAt(3, "inserted text 3")
        v.verifyPositionCalculation()

        v.replace(4 .. 5, "=", replaceMapping)
        v.verifyPositionCalculation()
        v.insertAt(4, "inserted text 4")
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun insertAfterIncrementalTransformReplaceAtSamePosition(chunkSize: Int) {
        testInsertAfterTransformReplaceAtSamePosition(
            chunkSize = chunkSize,
            replaceMapping = BigTextTransformOffsetMapping.Incremental
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun insertAfterBlockTransformReplaceAtSamePosition(chunkSize: Int) {
        testInsertAfterTransformReplaceAtSamePosition(
            chunkSize = chunkSize,
            replaceMapping = BigTextTransformOffsetMapping.WholeBlock
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun mixedTransformReplaces(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength

//        isD = true
        v.replace(32 .. 36, "long replacement", BigTextTransformOffsetMapping.Incremental)
        tt.printDebug("after replacement")
        v.verifyPositionCalculation()

//        isD = true
        v.replace(15 .. 23, "!?", BigTextTransformOffsetMapping.WholeBlock)
        tt.printDebug("after replacement !?")
        v.verifyPositionCalculation()

        v.replace(0 .. 2, "-+-+-", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

//        isD = true
        v.replace(originalLength - 12 until originalLength, "*-*-*", BigTextTransformOffsetMapping.Incremental)
//        isD = true
        v.verifyPositionCalculation()

//        if (chunkSize == 64) isD = true
        v.replace(3 .. 3, "some relatively long string that is longer than a chunk", BigTextTransformOffsetMapping.Incremental)
//        if (chunkSize == 64) isD = true
        v.verifyPositionCalculation()

        v.replace(4 .. 5, "=", BigTextTransformOffsetMapping.WholeBlock)
        if (chunkSize == 64) isD = true
        v.verifyPositionCalculation()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun mixedTransforms(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append("1234567890<234567890<bcdefghij<BCDEFGHIJ<row break< should h<appen her<e.")
        }
        val tt = BigTextTransformerImpl(t).apply {
            setLayouter(MonospaceTextLayouter(FixedWidthCharMeasurer(16f)))
            setContentWidth(16f * 10)
        }
        val v = BigTextVerifyImpl(tt)
        val originalLength = v.originalLength // 73

        v.insertAt(8, "inserted text 8")
        v.verifyPositionCalculation()

        v.delete(40 .. 52)
//        isD = true
        v.verifyPositionCalculation()

        v.replace(32 .. 36, "long replacement", BigTextTransformOffsetMapping.Incremental)
        v.verifyPositionCalculation()

        v.delete(26 .. 28)
        v.verifyPositionCalculation()

        v.insertAt(29, "XYZ")
//        isD = true
        v.verifyPositionCalculation()

        v.delete(24 .. 24)
        v.verifyPositionCalculation()

        v.replace(15 .. 23, "!?", BigTextTransformOffsetMapping.WholeBlock)
        isD = true
        v.verifyPositionCalculation()

        v.replace(0 .. 2, "-+-+-", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        v.insertAt(0, "inserted text at the beginning")
        v.verifyPositionCalculation()

        v.replace(originalLength - 12 until originalLength - 8, "*-*-*", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        v.delete(originalLength - 8 until originalLength)
        v.verifyPositionCalculation()

        v.replace(3 .. 3, "some relatively long string that is longer than a chunk", BigTextTransformOffsetMapping.Incremental)
        v.verifyPositionCalculation()

        v.insertAt(3, "inserted text 3")
        v.verifyPositionCalculation()

        v.replace(4 .. 5, "=", BigTextTransformOffsetMapping.WholeBlock)
        v.verifyPositionCalculation()

        v.insertAt(53, "^")
        v.verifyPositionCalculation()

        v.delete(54 .. 54)
        v.verifyPositionCalculation()

        v.replace(55 .. 55, "_")
        v.verifyPositionCalculation()

        v.delete(56 .. 59)
        v.verifyPositionCalculation()

        v.insertAt(56, "qwerty")
        v.verifyPositionCalculation()
    }
}

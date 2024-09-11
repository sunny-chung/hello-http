package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.isD
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class BigTextTransformerImplTest {

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsert(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(14, "ABCDEFG")

        transformed.printDebug()

        assertEquals("12345678901234ABCDEFG567890", transformed.buildString())
        assertAllSubstring("12345678901234ABCDEFG567890", transformed)
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsertMultipleAtDifferentPos(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(14, "ABCDEFG")
        transformed.transformInsert(7,  "KJI")
        transformed.transformInsert(16, "WXYZ")

        transformed.printDebug()

        assertEquals("1234567KJI8901234ABCDEFG56WXYZ7890", transformed.buildString())
        assertAllSubstring("1234567KJI8901234ABCDEFG56WXYZ7890", transformed)
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsertMultipleAtSamePos(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        if (chunkSize == 16) { isD = true }
        original.append("12345678901234567890")
        isD = false
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(14, "WXYZ")
        if (chunkSize == 16) { isD = true }
        transformed.transformInsert(14,  "KJI")

        transformed.printDebug()

        assertEquals("12345678901234KJIWXYZ567890", transformed.buildString())
        assertAllSubstring("12345678901234KJIWXYZ567890", transformed)
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsertMultipleAtBeginning(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(0, "ABCDEFG")
        transformed.transformInsert(0, "WXYZ")
        transformed.transformInsert(0,  "KJI")

        transformed.printDebug()

        assertEquals("KJIWXYZABCDEFG12345678901234567890", transformed.buildString())
        assertAllSubstring("KJIWXYZABCDEFG12345678901234567890", transformed)
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsertMultipleAtEnd(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(20, "ABCDEFG")
        transformed.transformInsert(20, "WXYZ")
        transformed.transformInsert(20,  "KJI")

        transformed.printDebug()

        "12345678901234567890KJIWXYZABCDEFG".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformInsertLongString(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(14, "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopABCDEFG")
        transformed.transformInsert(7,  "KJI")
        if (chunkSize == 16) { isD = true }
        transformed.transformInsert(16, "WXYZ")
        transformed.transformInsert(0,  "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopBCDEFGH")

        transformed.printDebug()

        "qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopBCDEFGH1234567KJI8901234qwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopqwertyuiopABCDEFG56WXYZ7890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDelete(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(14 .. 18)

        transformed.printDebug()

        "12345678901234012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteAtSamePosition1(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(14 .. 16)
        transformed.transformDelete(14 .. 15)

        transformed.printDebug()

        "12345678901234890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteAtSamePosition2(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(14 .. 15)
        transformed.transformDelete(14 .. 16)

        transformed.printDebug()

        "12345678901234890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteMultiple(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(14 .. 18)
        transformed.transformDelete(3 .. 6)
        transformed.transformDelete(10 .. 11)

        transformed.printDebug()

        "123890340".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("12345678901234567890", original.buildString())
        assertAllSubstring("12345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteAtBeginning(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(0 .. 11)

        transformed.printDebug()

        "3456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteAtEnd(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(36 .. 39)

        transformed.printDebug()

        "123456789012345678901234567890123456".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformDeleteWholeThing(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformDelete(0 .. 39)

        transformed.printDebug()

        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformReplace(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformReplace(13 .. 27, "abc")

        transformed.printDebug()

        "1234567890123abc901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformReplaceMultiple(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformReplace(36 .. 38, "A")
        transformed.transformReplace(13 .. 27, "abc")
        transformed.transformReplace(4 .. 4, "def")
        transformed.transformReplace(29 .. 30, "XY")

        transformed.printDebug()

        "1234def67890123abc9XY23456A0".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformReplaceAtBeginning(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformReplace(0 .. 33, "A")
        transformed.transformReplace(36 .. 37, "abc")

        transformed.printDebug()

        "A56abc90".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [64, 16])
    fun initialTransformReplaceAtEnd(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformReplace(25 .. 39, "qwertyuiop".repeat(3))

        transformed.printDebug()

        "1234567890123456789012345${"qwertyuiop".repeat(3)}".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [1024 * 1024, 64, 16])
    fun initialTransformReplaceByLongString(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformReplace(6 .. 25, "qwertyuiop".repeat(10))
        transformed.transformReplace(32 .. 35, "qwertyuiop".repeat(7))

        transformed.printDebug()

        "123456${"qwertyuiop".repeat(10)}789012${"qwertyuiop".repeat(7)}7890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [1024 * 1024, 64, 16])
    fun initialTransformReplaceConsecutive(chunkSize: Int) {
        val originalString = "1234567890123456789012345678901234567890"
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append(originalString)
        val transformed = BigTextTransformerImpl(original)
        try {
            var start = 0
            var counter = 0
            var expected = originalString
            while (start < 40) {
                var length = ((counter++) % 3) + 1
                val endExclusive = minOf(40, start + length)
                length = endExclusive - start
                val replacement = (counter % 10).digitToChar().toString().repeat(length)

//                if (chunkSize == 16 && counter == 3) { isD = true }
                if (chunkSize == 16 && counter == 10) { isD = true }
                expected = expected.replaceRange(start until endExclusive, replacement)
                transformed.transformReplace(start until endExclusive, replacement)

                assertEquals(expected, transformed.buildString())
                assertAllSubstring(expected, transformed)

                start = endExclusive
            }
        } finally {
            transformed.printDebug()
        }

        assertEquals("1234567890123456789012345678901234567890", original.buildString())
        assertAllSubstring("1234567890123456789012345678901234567890", original)
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun insertToOriginalThenTransformInsert(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        original.insertAt(15, "ABCDEFGHIJabcdefghij!@#$%")
        assertEquals(40 + 25, original.length)
        transformed.transformInsert(8, "abcd")
        transformed.transformInsert(63, "qwertyuiop".repeat(2))
        transformed.transformInsert(15, "XX")
        isD = true
        transformed.transformInsert(16, "OO")
        isD = false

        transformed.printDebug()

        "12345678abcd9012345XXAOOBCDEFGHIJabcdefghij!@#\$%67890123456789012345678${"qwertyuiop".repeat(2)}90".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun insertToOriginalThenTransformDelete(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        original.insertAt(15, "ABCDEFGHIJabcdefghij!@#\$%")
        assertEquals(40 + 25, original.length)
        transformed.transformDelete(24 .. 30)
        transformed.transformDelete(6 .. 9)
        transformed.transformDelete(58 .. 61)

        transformed.printDebug()

        "12345612345ABCDEFGHIghij!@#\$%678901234567890123890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun insertToOriginalThenTransformReplace(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        original.insertAt(15, "ABCDEFGHIJabcdefghij!@#\$%")
        assertEquals(40 + 25, original.length)
        transformed.transformReplace(24 .. 30, "")
        transformed.transformReplace(6 .. 9, "----")
        transformed.transformReplace(31 .. 31, "XYZXYZxyz")
        transformed.transformReplace(39 .. 41, "??")
        transformed.transformReplace(32 .. 38, ".")
        transformed.transformReplace(58 .. 61, " something longer")

        transformed.printDebug()

        "123456----12345ABCDEFGHIXYZXYZxyz.??8901234567890123 something longer890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun insertMultipleBetweenOriginalAndTransform(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(8, "abcd")
        original.insertAt(15, "ABCDEFGHIJabcdefghij!@#\$%")
        assertEquals(40 + 25, original.length)
        "12345678abcd9012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsert(63, "qwertyuiop")
        original.insertAt(62, "aa")
        "12345678abcd9012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa8qwertyuiop90".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsert(67, "(end)")
        transformed.transformInsert(64, "!")
        "12345678abcd9012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa!8qwertyuiop90(end)".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.insertAt(0, "prepend")
        original.insertAt(3, "PRE")
        transformed.transformInsert(6, "XOXO")
        transformed.transformInsert(12, "...")
        original.insertAt(14, "/")
        "prePREXOXOpend12...34/5678abcd9012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa!8qwertyuiop90(end)".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "prePREpend1234/56789012345ABCDEFGHIJabcdefghij!@#\$%6789012345678901234567aa890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun insertOriginalAtEndMultipleBetweenOriginalAndTransform(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(8, "abcd")
        original.append("ABCDEFGHIJabcdefghij!@#\$%")
        assertEquals(40 + 25, original.length)
        "12345678abcd90123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1234567890123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsertAtOriginalEnd("qwertyuiop")
        original.append("a")
        original.append("a")
        original.append("bb")
        "12345678abcd90123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabbqwertyuiop".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1234567890123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabb".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsertAtOriginalEnd("(end)")
        transformed.transformInsert(69, "!")
        "12345678abcd90123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabb!(end)qwertyuiop".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1234567890123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabb".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @BeforeEach
    fun beforeEach() {
        isD = false
    }
}

fun assertAllSubstring(expected: String, text: BigText) {
    (0 .. expected.length).forEach { i ->
        (i .. expected.length).forEach { j ->
            assertEquals(expected.substring(i, j), text.substring(i, j))
        }
    }
}

package com.sunnychung.application.multiplatform.hellohttp.test.bigtext.transform

import com.sunnychung.application.multiplatform.hellohttp.test.bigtext.randomString
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
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
        transformed.transformInsert(14,  "KJI")
        if (chunkSize == 16) { isD = true }
        transformed.transformInsert(14, "WXYZ")

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
        transformed.transformInsert(0,  "KJI")
        transformed.transformInsert(0, "WXYZ")
        transformed.transformInsert(0, "ABCDEFG")

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
        transformed.transformInsert(20,  "KJI")
        transformed.transformInsert(20, "WXYZ")
        transformed.transformInsert(20, "ABCDEFG")

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
        "12345678abcd90123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabbqwertyuiop(end)!".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1234567890123456789012345678901234567890ABCDEFGHIJabcdefghij!@#\$%aabb".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun deleteMultipleBetweenOriginalAndTransform(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        original.delete(31, 33)
        original.delete(35, 37)
        transformed.transformInsert(8, "abcd")
        original.delete(15, 18)
        assertEquals(40 - 2 - 2 - 3, original.length)
        "12345678abcd9012345901234567890145670".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123456789012345901234567890145670".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformDelete(22 .. 26)
        transformed.transformInsert(29, "qwertyuiop")
        original.delete(4 .. 7)
        "1234abcd9012345901234514qwertyuiop5670".let { expected ->
            assertEquals(expected, transformed.buildString()) //
            assertAllSubstring(expected, transformed)
        }
        "12349012345901234567890145670".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformDelete(5 .. 8)
        transformed.transformDelete(26 .. 28)
        "1234abcd945901234514qwertyuiop5".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "12349012345901234567890145670".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.delete(10 .. 15)
        "1234abcd944514qwertyuiop5".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "12349012344567890145670".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.insertAt(10, "_")
        original.insertAt(9, "ABC")
        "1234abcd9ABC4_4514qwertyuiop5".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123490123ABC4_4567890145670".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.delete(3 .. 26)
        "123".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformDelete(0 .. 2)
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "123".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.delete(0 .. 1)
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "3".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.delete(0 .. 0)
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsert(0, "Zz")
        "Zz".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun replaceMultipleBetweenOriginalAndTransform(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("1234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        original.replace(31 .. 33, "zxcvb")
        original.replace(5 .. 7, "ZXCV")
        "12345ZXCV90123456789012345678901zxcvb567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }
        transformed.transformReplace(8 .. 11, "ab")
        original.delete(15, 18)
        "12345ZXCab23489012345678901zxcvb567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "12345ZXCV90123489012345678901zxcvb567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformReplace(13 .. 33, "qwerty")
        "12345ZXCab2qwerty567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "12345ZXCV90123489012345678901zxcvb567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        original.replace(1 .. 35, "#######")
        "1#######7890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1#######7890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformReplace(5 .. 9, "-")
        "1####-90".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1#######7890".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformInsert(12, "*")
        original.replace(0 .. 10, "x")
        "x0*".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "x0".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.transformReplace(0 .. 1, "@")
        original.replace(0 .. 1, "")
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1024, 64, 16])
    fun replaceLongString(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        val initial = "1234567890223456789032345678904234567890_234567890223456789032345678904234567890"
        original.append(initial)
        assertEquals(80, original.length)
        val transformed = BigTextTransformerImpl(original)
        val s1 = randomString(72 - 6 + 1, false)
        transformed.transformReplace(6 .. 72, s1)
        "123456${s1}4567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        initial.let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        val s2 = randomString(77 - 4 + 1, false)
        transformed.transformReplace(1 .. 2, "!")
        original.replace(4 .. 77, s2)
        "1!4${s2}90".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        "1234${s2}90".let { expected ->
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, original)
        }

        transformed.printDebug()
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deleteOverlap(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890123456789012345678901234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.delete(15 .. 33)
        transformed.delete(12 .. 37)
        "123456789012901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(12 .. 52)
        "123456789012456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(9 .. 52)
        "123456789456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(4 .. 78)
        "12340".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(14 .. 66) // no effect
        "12340".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(50 .. 68) // no effect
        "12340".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.delete(0 .. 79)
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun replaceOverlap(chunkSize: Int) {
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append("12345678901234567890123456789012345678901234567890123456789012345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.replace(15 .. 33, "AAA")
        transformed.replace(12 .. 37, "BB")
        "123456789012BB901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
//        transformed.replace(21 .. 34, "CCCCCC") // TODO prevent this operation to succeed
//        "123456789012BB901234567890123456789012345678901234567890".let { expected ->
//            assertEquals(expected, transformed.buildString())
//            assertAllSubstring(expected, transformed)
//        }
        transformed.replace(11 .. 11, "www")
        "12345678901wwwBB901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(12 .. 52, "DDDD")
        "12345678901wwwDDDD456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(9 .. 52, "eeeee")
        "123456789eeeee456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(4 .. 78, ".")
        "1234.0".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(0 .. 79, "GG")
        "GG".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(0 .. 79, "hihi")
        "hihi".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        transformed.replace(0 .. 79, "")
        "".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun deleteAndReplaceOverlapped(chunkSize: Int) {
        val initial = "1234567890223456789032345678904234567890_234567890623456789072345678908234567890\n"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(initial)
        }
        val tt = BigTextTransformerImpl(t)
        tt.replace(43 .. 60, "def") // incremental replace
        assertEquals(
            expected = initial
                .replaceRange(43 .. 60, "def"),
            actual = tt.buildString()
        )

        tt.delete(42 .. 43)
        assertEquals(
            expected = initial
                .replaceRange(44 .. 60, "ef")
                .replaceRange(42 .. 43, ""),
            actual = tt.buildString()
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun restoreToOriginal1(chunkSize: Int) {
        val initialText = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append(initialText)
        val transformed = BigTextTransformerImpl(original)

        transformed.replace(11 .. 18, "ABCD", BigTextTransformOffsetMapping.Incremental)
        "12345678901ABCD0123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(initialText, original.buildString())
            assertAllSubstring(expected, transformed)
        }

        transformed.insertAt(61, "EFGHIJ")
        "12345678901ABCD012345678901234567890123456789012345678901EFGHIJ2345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(initialText, original.buildString())
            assertAllSubstring(expected, transformed)
        }

        transformed.restoreToOriginal(0 .. initialText.length - 1)
        initialText.let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(expected, original.buildString())
            assertEquals(expected.length, transformed.length)
            assertEquals(initialText.length, transformed.originalLength)
            assertAllSubstring(expected, transformed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun restoreToOriginal2(chunkSize: Int) {
        val initialText = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append(initialText)
        val transformed = BigTextTransformerImpl(original)

        transformed.replace(11 .. 18, "ABCD", BigTextTransformOffsetMapping.WholeBlock)
        "12345678901ABCD0123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(initialText, original.buildString())
            assertEquals(expected.length, transformed.length)
            assertEquals(initialText.length, transformed.originalLength)
            assertAllSubstring(expected, transformed)
        }

        transformed.restoreToOriginal(11 .. 18)
        initialText.let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(expected, original.buildString())
            assertEquals(expected.length, transformed.length)
            assertEquals(initialText.length, transformed.originalLength)
            assertAllSubstring(expected, transformed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun restoreToOriginalThenOriginalDelete(chunkSize: Int) {
        val initialText = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append(initialText)
        val transformed = BigTextTransformerImpl(original)

        transformed.replace(11 .. 18, "ABCD", BigTextTransformOffsetMapping.WholeBlock)
        "12345678901ABCD0123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(initialText, original.buildString())
            assertAllSubstring(expected, transformed)
        }

        transformed.restoreToOriginal(11 .. 18)
        initialText.let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(expected, original.buildString())
            assertAllSubstring(expected, transformed)
        }

        original.delete(18 .. 18)

        "1234567890123456780123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, original.buildString())
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun transformReplaceThenInsertToOriginalAtMiddle(chunkSize: Int) {
        listOf("EEEEEE", "E".repeat(70)).forEach { insertContent ->
            val initialText = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            val original = BigTextImpl(chunkSize = chunkSize)
            original.append(initialText)
            val transformed = BigTextTransformerImpl(original)

            transformed.replace(3..10, "abcdef", BigTextTransformOffsetMapping.Incremental)
            "123abcdef234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
                assertEquals(expected, transformed.buildString())
                assertEquals(initialText, original.buildString())
                assertAllSubstring(expected, transformed)
            }

            transformed.printDebug("before insert")

            original.insertAt(6, insertContent)

            transformed.printDebug("after insert")

            "123abc${insertContent}def234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
                assertEquals(expected, transformed.buildString())
                assertAllSubstring(expected, transformed)
            }
            assertEquals(
                "123456${insertContent}78901234567890123456789012345678901234567890123456789012345678901234567890",
                original.buildString()
            )
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1048576, 64, 16])
    fun transformReplaceThenDeleteOriginalAtMiddle(chunkSize: Int) {
        val initialText = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        val original = BigTextImpl(chunkSize = chunkSize)
        original.append(initialText)
        val transformed = BigTextTransformerImpl(original)

        transformed.replace(3..10, "abcdef", BigTextTransformOffsetMapping.Incremental)
        "123abcdef234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertEquals(initialText, original.buildString())
            assertAllSubstring(expected, transformed)
        }

        transformed.printDebug("before delete")

        original.delete(6 .. 7)

        transformed.printDebug("after delete")

        "123abcf234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals(
            "123456901234567890123456789012345678901234567890123456789012345678901234567890",
            original.buildString()
        )

        original.delete(5 .. 5)
        "123abf234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals(
            "12345901234567890123456789012345678901234567890123456789012345678901234567890",
            original.buildString()
        )

        original.delete(5 .. 5) // equivalent to initial index 8
        "123ab234567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals(
            "1234501234567890123456789012345678901234567890123456789012345678901234567890",
            original.buildString()
        )

        isD = true
        original.delete(5 .. 5) // equivalent to initial index 9, out of replacement range
        "123ab34567890123456789012345678901234567890123456789012345678901234567890".let { expected ->
            assertEquals(expected, transformed.buildString())
            assertAllSubstring(expected, transformed)
        }
        assertEquals(
            "123451234567890123456789012345678901234567890123456789012345678901234567890",
            original.buildString()
        )
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

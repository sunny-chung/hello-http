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
        original.append("12345678901234567890")
        val transformed = BigTextTransformerImpl(original)
        transformed.transformInsert(14, "WXYZ")
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

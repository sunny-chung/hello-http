package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals

class BigTextUndoRedoTest {

    @ParameterizedTest
    @ValueSource(ints = [6, 64, 2 * 1024 * 1024])
    fun undoRedoSimpleInsertDeleteSingleCharacter(chunkSize: Int) {
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            isUndoEnabled = true
        }
        "abcde".forEach {
            t.append(it.toString())
            t.recordCurrentChangeSequenceIntoUndoHistory()
        }
        t.insertAt(2, "A")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.insertAt(3, "B")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.insertAt(4, "C")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        assertEquals("abABCcde", t.buildString())
        t.delete(3 .. 3)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(5 .. 5)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(5 .. 5)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.append("x")
        assertEquals("abACcx", t.buildString())
        assertUndoRedoUndo(listOf(
            "abACcx",
            "abACc",
            "abACce",
            "abACcde",
            "abABCcde",
            "abABcde",
            "abAcde",
            "abcde",
            "abcd",
            "abc",
            "ab",
            "a",
            "",
        ), t)
    }

    @ParameterizedTest
    @ValueSource(ints = [16, 64, 2 * 1024 * 1024])
    fun undoRedoSimpleInsertDeleteChunkStringWithInitialString(chunkSize: Int) {
        val initial = "0123456789112345678921234567893123456789"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(initial)
            isUndoEnabled = true
        }
        t.delete(14 .. 19)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.insertAt(13, "abcdefg")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.append("xyz")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.insertAt(29, "ABCDEFGH")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(31 .. 33)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(0 .. 5)
//        assertEquals("0123456789112abcdefg321234567ABCDEFGH893123456789xyz", t.buildString())
        assertEquals("6789112abcdefg321234567ABFGH893123456789xyz", t.buildString())

        assertUndoRedoUndo(listOf(
            "6789112abcdefg321234567ABFGH893123456789xyz",
            "0123456789112abcdefg321234567ABFGH893123456789xyz",
            "0123456789112abcdefg321234567ABCDEFGH893123456789xyz",
            "0123456789112abcdefg321234567893123456789xyz",
            "0123456789112abcdefg321234567893123456789",
            "0123456789112321234567893123456789",
            "0123456789112345678921234567893123456789",
        ), t)
    }

    @ParameterizedTest
    @ValueSource(ints = [16, 15, 64, 2 * 1024 * 1024])
    fun undoRedoInsertDeleteLongStringWithInitialString(chunkSize: Int) {
        val initial = "abcd"
        val t = BigTextImpl(chunkSize = chunkSize).apply {
            append(initial)
            isUndoEnabled = true
        }
        t.insertAt(0, "0123456789112345678921234567893123456789412345678951234567896123")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(1..64)
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.insertAt(3, "ABCD")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.append("xyz")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        assertUndoRedoUndo(listOf(
            "0bcABCDdxyz",
            "0bcABCDd",
            "0bcd",
            "0123456789112345678921234567893123456789412345678951234567896123abcd",
            initial
        ), t)
    }

    @Test
    fun noRedoAfterMakingChanges() {
        val t = BigTextImpl(chunkSize = 256).apply {
            append("")
            isUndoEnabled = true
        }
        t.append("abcd")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.append("efg")
        t.recordCurrentChangeSequenceIntoUndoHistory()
        t.delete(6 .. 6)
        assertEquals("abcdef", t.buildString())
        listOf("abcdefg", "abcd").forEach { expected ->
            assertEquals(true, t.undo().first)
            assertEquals(expected, t.buildString())
        }

        // no redo after making a change
        t.append("x")
        assertEquals("abcdx", t.buildString())
        (1..10).forEach {
            assertEquals(false, t.redo().first)
            assertEquals("abcdx", t.buildString())
        }

        assertUndoRedoUndo(listOf("abcdx", "abcd", ""), t)
    }

    fun assertUndoRedoUndo(reversedExpectedStrings: List<String>, t: BigTextImpl) {
        assertEquals(reversedExpectedStrings.first(), t.buildString())
        reversedExpectedStrings.stream().skip(1).forEach { expected ->
            assertEquals(true, t.undo().first)
            assertEquals(expected, t.buildString())
        }
        (1..3).forEach {
            assertEquals(false, t.undo().first)
            assertEquals(reversedExpectedStrings.last(), t.buildString())
        }
        reversedExpectedStrings.asReversed().stream().skip(1).forEach { expected ->
            assertEquals(true, t.redo().first)
            assertEquals(expected, t.buildString())
        }
        (1..10).forEach {
            assertEquals(false, t.redo().first)
            assertEquals(reversedExpectedStrings.first(), t.buildString())
        }
        reversedExpectedStrings.stream().skip(1).forEach { expected ->
            assertEquals(true, t.undo().first)
            assertEquals(expected, t.buildString())
        }
        (1..10).forEach {
            assertEquals(false, t.undo().first)
            assertEquals(reversedExpectedStrings.last(), t.buildString())
        }
    }
}

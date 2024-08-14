package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

/**
 * Manipulates large String. This is NOT thread-safe.
 */
interface BigText {

    val length: Int

    fun fullString(): String

    fun substring(start: Int, endExclusive: Int): String

    fun substring(range: IntRange): String = substring(range.start, range.endInclusive + 1)

    fun append(text: String): Int

    fun insertAt(pos: Int, text: String): Int

    fun delete(start: Int, endExclusive: Int): Int

    fun delete(range: IntRange): Int = delete(range.start, range.endInclusive + 1)

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean
}

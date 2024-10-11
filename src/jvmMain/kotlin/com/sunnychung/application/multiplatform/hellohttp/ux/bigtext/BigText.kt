package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

/**
 * Manipulates large String. This is NOT thread-safe.
 */
interface BigText {

    val length: Int

    fun buildString(): String

    fun buildCharSequence(): CharSequence

    fun substring(start: Int, endExclusive: Int): CharSequence

    fun substring(range: IntRange): CharSequence = substring(range.start, range.endInclusive + 1)

    fun subSequence(startIndex: Int, endIndex: Int) = substring(startIndex, endIndex)

    fun append(text: CharSequence): Int

    fun insertAt(pos: Int, text: CharSequence): Int

    fun delete(start: Int, endExclusive: Int): Int

    fun delete(range: IntRange): Int = delete(range.start, range.endInclusive + 1)

    fun replace(start: Int, endExclusive: Int, text: CharSequence) {
        delete(start, endExclusive)
        insertAt(start, text)
    }

    fun replace(range: IntRange, text: CharSequence) {
        delete(range)
        insertAt(range.start, text)
    }

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean

    companion object
}

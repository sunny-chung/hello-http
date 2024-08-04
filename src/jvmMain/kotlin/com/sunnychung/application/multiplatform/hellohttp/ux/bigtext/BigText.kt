package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

/**
 * Manipulates large String. This is NOT thread-safe.
 */
interface BigText {

    val length: Int

    fun fullString(): String

    fun substring(start: Int, endExclusive: Int): String

    fun substring(range: IntRange): String

    fun append(text: String)

    fun insertAt(pos: Int, text: String)

    fun delete(start: Int, endExclusive: Int)

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean
}

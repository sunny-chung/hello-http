package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.util.string

class InefficientBigText(text: String) : BigText {
    private var string: String = text

    override val length: Int
        get() = string.length

    override fun buildString(): String = string

    override fun buildCharSequence(): CharSequence = string

    override fun substring(start: Int, endExclusive: Int): CharSequence =
        string.substring(start, endExclusive)

    override fun substring(range: IntRange): CharSequence =
        substring(range.first, range.last)

    override fun append(text: CharSequence): Int {
        string += text
        return text.length
    }

    override fun insertAt(pos: Int, text: CharSequence): Int {
        string = string.insert(pos, text.string())
        return text.length
    }

    override fun delete(start: Int, endExclusive: Int): Int {
        string = string.removeRange(start, endExclusive)
        return -(endExclusive - start)
    }

    override fun hashCode(): Int =
        string.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is BigText) {
            return false
        }
        return when(other) {
            is InefficientBigText -> string == other.buildString()
            else -> TODO()
        }
    }
}

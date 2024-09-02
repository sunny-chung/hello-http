package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.insert

class InefficientBigText(text: String) : BigText {
    private var string: String = text

    override val length: Int
        get() = string.length

    override fun buildString(): String = string

    override fun substring(start: Int, endExclusive: Int): String =
        string.substring(start, endExclusive)

    override fun substring(range: IntRange): String =
        substring(range.first, range.last)

    override fun append(text: String): Int {
        string += text
        return text.length
    }

    override fun insertAt(pos: Int, text: String): Int {
        string = string.insert(pos, text)
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

package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.insert

class InefficientBigText(text: String) : BigText {
    private var string: String = text

    override val length: Int
        get() = string.length

    override fun fullString(): String = string

    override fun substring(start: Int, endExclusive: Int): String =
        string.substring(start, endExclusive)

    override fun substring(range: IntRange): String =
        substring(range.first, range.last)

    override fun append(text: String) {
        string += text
    }

    override fun insertAt(pos: Int, text: String) {
        string = string.insert(pos, text)
    }

    override fun delete(start: Int, endExclusive: Int) {
        string = string.removeRange(start, endExclusive)
    }

    override fun hashCode(): Int =
        string.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is BigText) {
            return false
        }
        return when(other) {
            is InefficientBigText -> string == other.fullString()
            else -> TODO()
        }
    }
}

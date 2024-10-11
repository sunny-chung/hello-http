package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

class BigTextAsCharSequence(internal val bigText: BigTextImpl) : CharSequence {
    override val length: Int
        get() = bigText.length

    override fun get(index: Int): Char {
        return bigText.substring(index, index + 1)[0]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return bigText.subSequence(startIndex, endIndex)
    }

    override fun toString(): String = bigText.buildString()
}

fun BigText.Companion.wrap(charSequence: CharSequence): BigTextImpl {
    return when (charSequence) {
        is BigTextAsCharSequence -> charSequence.bigText
        else -> BigText.createFromLargeString(charSequence.toString())
    }
}

fun BigTextImpl.asCharSequence(): CharSequence  = BigTextAsCharSequence(this)

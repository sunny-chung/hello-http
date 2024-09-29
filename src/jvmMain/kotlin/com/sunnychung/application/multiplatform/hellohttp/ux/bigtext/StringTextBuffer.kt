package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

class StringTextBuffer(size: Int) : TextBuffer() {
    private val buffer = StringBuilder(size)

    override val length: Int
        get() = buffer.length

    override fun bufferAppend(text: CharSequence) {
        buffer.append(text)
    }

    override fun bufferSubstring(start: Int, endExclusive: Int): String {
        return buffer.substring(start, endExclusive)
    }

    override fun bufferSubSequence(start: Int, endExclusive: Int): CharSequence {
        return buffer.subSequence(start, endExclusive)
    }
}

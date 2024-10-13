package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import java.util.TreeMap

class AnnotatedStringTextBuffer(size: Int) : TextBuffer() {
    private val buffer = StringBuilder(size)

    // TODO optimize it to use interval tree when styles that change character width are supported
    // otherwise layout would be very slow
    private val spanStyles = TreeMap<Int, MutableList<Pair<IntRange, SpanStyle>>>()

    override val length: Int
        get() = buffer.length

    override fun bufferAppend(text: CharSequence) {
        if (text is AnnotatedString) {
            val baseStart = buffer.length
            text.spanStyles.forEach {
                val start = it.start + baseStart
                val endExclusive = it.end + baseStart
                spanStyles.getOrPut(start) { mutableListOf() } += (start until endExclusive) to it.item
            }
            buffer.append(text)
            return
        }
        buffer.append(text)
    }

    override fun bufferSubstring(start: Int, endExclusive: Int): String {
        return buffer.substring(start, endExclusive)
    }

    override fun bufferSubSequence(start: Int, endExclusive: Int): CharSequence {
        val queryRange = start until endExclusive
        return AnnotatedString(
            text = buffer.substring(start, endExclusive),
            spanStyles = spanStyles.subMap(0, endExclusive)
                .flatMap { e ->
                    e.value.filter {
                        queryRange hasIntersectWith it.first
                    }
                        .map {
                            AnnotatedString.Range(
                                item = it.second,
                                start = maxOf(0, it.first.start - start),
                                end = minOf(endExclusive - start, it.first.endInclusive + 1 - start)
                            )
                        }
                },
            paragraphStyles = emptyList()
        )
    }
}

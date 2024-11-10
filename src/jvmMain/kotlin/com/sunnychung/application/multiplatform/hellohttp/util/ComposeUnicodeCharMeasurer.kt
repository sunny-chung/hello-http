package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import java.util.LinkedHashMap

class ComposeUnicodeCharMeasurer(private val measurer: TextMeasurer, private val style: TextStyle) : CharMeasurer {
    private val charWidth: MutableMap<String, Float> = LinkedHashMap<String, Float>(256)
    private val charHeight: Float = measurer.measure("|\n|").let {
        it.getLineTop(1) - it.getLineTop(0)
    }

    /**
     * Time complexity = O(S lg C)
     */
    override fun measureFullText(text: String) {
        val charToMeasure = mutableSetOf<String>()
        var surrogatePairFirst: Char? = null
        text.forEach {
            var s = it.toString()
            if (surrogatePairFirst == null && s[0].isSurrogatePairFirst()) {
                surrogatePairFirst = s[0]
                return@forEach
            } else if (surrogatePairFirst != null) {
                if (s[0].isSurrogatePairSecond()) {
                    s = "$surrogatePairFirst${s[0]}"
                } else {
                    s = s.substring(0, 1)
                }
                surrogatePairFirst = null
            }
            if (!charWidth.containsKey(s) && shouldIndexChar(s)) {
                charToMeasure += s
            }
        }
        measureAndIndex(charToMeasure)
        log.v { "UnicodeCharMeasurer measureFullText cache size ${charWidth.size}" }
    }

    /**
     * Time complexity = O(lg C)
     *
     * TODO: handle surrogate pair correctly
     */
    override fun findCharWidth(char: String): Float {
        if (char[0].isSurrogatePairFirst()) {
            return 0f
        }
        when (char.codePoints().findFirst().asInt) {
            in 0x4E00..0x9FFF,
                in 0x3400..0x4DBF,
                in 0x20000..0x2A6DF,
                in 0xAC00..0xD7AF ->
                    return charWidth[CJK_FULLWIDTH_REPRESENTABLE_CHAR]!!
        }
        return charWidth[char] ?: run {
            measureAndIndex(setOf(char))
            charWidth[char]!!
        }
    }

    private fun measureAndIndex(charSet: Set<String>) {
        val chars = charSet.toList()
        measureExactWidthOf(chars).forEachIndexed { index, r ->
            charWidth[chars[index]] = r
            if (r < 1f) {
                log.i { "measure '${chars[index]}' width = $r" }
            }
        }
    }

    fun getRowHeight(): Float = charHeight

    fun measureExactWidthOf(targets: List<String>): List<Float> {
        val result = measurer.measure(targets.joinToString("") { "$it\n"}, style, softWrap = false)
        return targets.mapIndexed { index, s ->
            result.getLineRight(index) - result.getLineLeft(index)
        }
    }

    inline fun shouldIndexChar(s: String): Boolean {
        val cp = s.codePoints().findFirst().asInt
        return when (cp) {
            in 0x4E00..0x9FFF -> false // CJK Unified Ideographs
            in 0x3400..0x4DBF -> false // CJK Unified Ideographs Extension A
            in 0x20000..0x2A6DF -> false // CJK Unified Ideographs Extension B
            in 0xAC00..0xD7AF -> false // Hangul Syllables
            else -> true
        }
    }

    fun Char.isSurrogatePairFirst(): Boolean {
        return code in (0xD800 .. 0xDBFF)
    }

    fun Char.isSurrogatePairSecond(): Boolean {
        return code in (0xDC00 .. 0xDFFF)
    }

    init {
        measureAndIndex(COMPULSORY_MEASURES)
        // hardcode, because calling TextMeasurer#measure() against below characters returns zero width
        charWidth[" "] = charWidth["_"]!!
        charWidth["\t"] = charWidth[" "]!!
        charWidth["?"] = charWidth["!"]!!
        charWidth["’"] = charWidth["'"]!!
    }

    companion object {
        private const val CJK_FULLWIDTH_REPRESENTABLE_CHAR = "好"
        private val COMPULSORY_MEASURES = (
            (0x20.toChar() .. 0x7E.toChar()).map(Char::toString).toMutableSet() +
                CJK_FULLWIDTH_REPRESENTABLE_CHAR
        ).toSet()
    }
}

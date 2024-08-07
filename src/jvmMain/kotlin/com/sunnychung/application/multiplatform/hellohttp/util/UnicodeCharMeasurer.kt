package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import java.util.LinkedHashMap

class UnicodeCharMeasurer(private val measurer: TextMeasurer, private val style: TextStyle) {
    private val charWidth: MutableMap<String, Float> = LinkedHashMap<String, Float>(256)

    /**
     * Time complexity = O(S lg C)
     */
    fun measureFullText(text: String) {
        val charToMeasure = mutableSetOf<String>()
        text.forEach {
            val s = it.toString()
            if (!charWidth.containsKey(s) && shouldIndexChar(s)) {
                charToMeasure += s
            }
        }
        measureAndIndex(charToMeasure)
        log.v { "UnicodeCharMeasurer measureFullText cache size ${charWidth.size}" }
    }

    /**
     * Time complexity = O(lg C)
     */
    fun findCharWidth(char: String): Float {
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
                log.w { "measure '${chars[index]}' width = $r" }
            }
        }
    }

    fun measureExactWidthOf(targets: List<String>): List<Float> {
        val result = measurer.measure(targets.joinToString("\n"), style, softWrap = false)
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

    init {
        measureAndIndex(COMPULSORY_MEASURES)
        // hardcode, because calling TextMeasurer#measure() against below characters returns zero width
        charWidth[" "] = charWidth["_"]!!
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

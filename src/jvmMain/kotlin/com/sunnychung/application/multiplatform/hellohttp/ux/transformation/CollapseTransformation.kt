package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor

/**
 * This class assumes offset before this filter is not modified
 */
class CollapseTransformation(colours: AppColor, collapsedCharRanges: List<IntRange>) : VisualTransformation {
    val collapsedStyle = SpanStyle(background = colours.backgroundCollapsed)

    // TODO optimize to use binary tree
    val collapsedCharRanges = collapsedCharRanges.sortedBy { it.first }

    override fun filter(text: AnnotatedString): TransformedText {
        var lastIndex = 0
        val modifiedText = buildAnnotatedString {
            collapsedCharRanges.forEach {
                if (it.first < lastIndex) return@forEach
                append(text.subSequence(lastIndex .. it.start))

                append(" ")
                append(AnnotatedString("...", collapsedStyle))
                append(" ")

                append(text.subSequence(it.last .. it.last))

                lastIndex = it.last + 1
            }
            append(text.subSequence(lastIndex, text.length))
        }

        return TransformedText(modifiedText, CollapseTransformationOffsetMapping(collapsedCharRanges))
    }
}

/**
 *                          0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
 *                          a b c { d e f g h i j  }  k  l  {  m  n  }  o  p
 * collapsedCharRanges            [3, 11]                  [14, 17]
 * transformedText          a b c {   . . .   } k  l  {     .  .  .     }  o  p
 * originalToTransformed    0 1 2 3 9 9 9 9 9 9 9  9  10 11 12 18 18 18 18 19 20
 * transformedToOriginal    0 1 2 3 11       11 12 13 14 17             17 18 19
 */
class CollapseTransformationOffsetMapping(collapsedCharRanges: List<IntRange>) : OffsetMapping {
    // TODO optimize to use binary tree
    private val collapsedCharRanges = collapsedCharRanges.sortedBy { it.start }
    override fun originalToTransformed(offset: Int): Int {
        if (collapsedCharRanges.isEmpty() || offset <= collapsedCharRanges.first().first) {
            return offset
        }

        var accumulatedEatenChars = 0
        var lastIndex = 0
        for (it in collapsedCharRanges) {
            if (it.first > offset) break
            if (it.first < lastIndex) continue
            if (offset > it.first) {
                if(offset <= it.last) {
                    val newOffset = it.first + " ... ".length + 1 - accumulatedEatenChars
//                    log.v { "newOffset[$offset] = $newOffset" }
                    return newOffset
                } else {
                    accumulatedEatenChars += (it.last) - (it.first + 1) - " ... ".length
//                    log.v { "accumulatedEatenChars = $accumulatedEatenChars" }
                }
            }
            lastIndex = it.last + 1
        }
        val newOffset = offset - accumulatedEatenChars
//        log.v { "new offset[$offset] = $newOffset" }
        return newOffset
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (collapsedCharRanges.isEmpty() || offset < collapsedCharRanges.first().first + 1) {
            return offset
        }

        var accumulatedEatenChars = 0
        var lastIndex = 0
        for (it in collapsedCharRanges) {
            if (it.first > offset + accumulatedEatenChars) break
            if (it.first < lastIndex) continue
            if (offset + accumulatedEatenChars >= it.first + 1) {
                if (offset + accumulatedEatenChars <= it.first + " ... }".length) {
//                    log.v { "toOrg[$offset] = ${it.last}" }
                    return it.last
                } else {
                    accumulatedEatenChars += it.last - (it.first + 1) - " ... ".length
//                    log.v { "accumulatedEatenChars = $accumulatedEatenChars" }
                }
            }
            lastIndex = it.last + 1
        }
        val oldOffset = offset + accumulatedEatenChars
//        log.v { "toOrg [$offset] = $oldOffset" }
        return oldOffset
    }
}

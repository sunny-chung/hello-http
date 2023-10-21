package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import java.util.*

class EnvironmentVariableTransformation(val themeColors: AppColor, val knownVariables: Set<String>) : VisualTransformation {
    private val variableRegex = "\\$\\{\\{([^{}]+)\\}\\}".toRegex()

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.toString()
        val variables = variableRegex.findAll(originalText)
//        val inlines = mutableMapOf<String, InlineTextContent>()
        val variableRanges = TreeMap<Int, IntRange>()
        variables.forEach {
            variableRanges[it.range.start] = it.range
        }
        val newAnnotated = buildAnnotatedString {
            var start = 0
            variables.forEach {
                append(originalText.substring(start until it.range.start))

//                val inlineId = uuidString()
//                appendInlineContent(inlineId, originalText.substring(it.range))
//                inlines[inlineId] = InlineTextContent(
//                    Placeholder(it.range.count() * 1.sp, 12.sp, PlaceholderVerticalAlign.AboveBaseline)
//                ) { _ ->
//                    AppText(
//                        text = it.groups[1]!!.value,
//                        fontFamily = FontFamily.Monospace,
//                        modifier = Modifier.background(
//                            color = LocalColor.current.backgroundTooltip,
//                            shape = RoundedCornerShape(8.dp)
//                        )
//                    )
//                }

                val variableName = it.groups[1]!!.value
                withStyle(SpanStyle(
                    color = themeColors.variableTextColor,
                    background = if (variableName in knownVariables) themeColors.variableBackgroundColor else themeColors.variableErrorBackgroundColor,
                    fontFamily = FontFamily.Monospace,
                )) {
                    append(variableName)
                }

                start = it.range.last + 1
            }
            append(originalText.substring(start))
        }
        return TransformedText(newAnnotated, EnvironmentVariableTransformationOffsetMapping(variableRanges))
    }
}

private const val PREFIX_LENGTH = 3
private const val POSTFIX_LENGTH = 2

/**
 *                        0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18
 *                        a b c { { v a r } } d  e  {  {  v  }  }  x  y
 * variableRanges               [3, 9       ]       [12, 16     ]
 * originalToTransformed  0 1 2 3 3 3 4 5 5 5 6  7  8  8  8  8  8  9  10
 * transformedToOriginal  0 1 2 5 5 5 6 7 7 7 10 11 14 14 14 14 14 17 18
 */
class EnvironmentVariableTransformationOffsetMapping(val variableRanges: TreeMap<Int, IntRange>) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        val preRanges = variableRanges.headMap(offset, true)
        val lastPreRange = preRanges.lastEntry()
        return offset + maxOf(0, preRanges.size - 1) * -(PREFIX_LENGTH + POSTFIX_LENGTH) + (lastPreRange?.let {
            if (offset > it.value.endInclusive) {
                -(PREFIX_LENGTH + POSTFIX_LENGTH)
            } else if (offset > it.value.endInclusive - POSTFIX_LENGTH) {
                -(PREFIX_LENGTH + (offset - (it.value.endInclusive - POSTFIX_LENGTH)))
            } else if (offset >= it.value.start + PREFIX_LENGTH) {
                -PREFIX_LENGTH
            } else {
                -(offset - it.value.start)
            }
        } ?: 0)
    }

    override fun transformedToOriginal(offset: Int): Int { // TODO: reduce time complexity
        var delta = 0
        for (range in variableRanges) {
            if (offset + delta > range.value.endInclusive - (PREFIX_LENGTH + POSTFIX_LENGTH)) {
                delta += PREFIX_LENGTH + POSTFIX_LENGTH
            } else if (offset + delta >= range.value.start) {
                delta += PREFIX_LENGTH
                break
            } else {
                break
            }
        }
        return offset + delta
    }

}

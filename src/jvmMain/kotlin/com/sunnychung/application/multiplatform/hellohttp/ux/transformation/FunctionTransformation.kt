package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.sunnychung.application.multiplatform.hellohttp.constant.UserFunctions
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import java.util.*

class FunctionTransformation(val themeColors: AppColor) : VisualTransformation {
    private val functionRegex =
        ("\\$\\(\\((" + UserFunctions.keys.joinToString("|") { Regex.escape(it) } + ")\\)\\)").toRegex()

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val functions = functionRegex.findAll(originalText)
//        val inlines = mutableMapOf<String, InlineTextContent>()
        val functionRanges = TreeMap<Int, IntRange>()
        functions.forEach {
            functionRanges[it.range.start] = it.range
        }
        val newAnnotated = buildAnnotatedString {
            var start = 0
            functions.forEach {
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
                    color = themeColors.functionTextColor,
                    background = themeColors.functionBackgroundColor,
                    fontFamily = FontFamily.Monospace,
                )) {
                    append(variableName)
                }

                start = it.range.last + 1
            }
            append(originalText.substring(start))
        }
        val offsetMapping = FunctionTransformationOffsetMapping(functionRanges)
        return TransformedText(
            text = AnnotatedString(text = newAnnotated.text, spanStyles = mergeSpanStylesWithTransformedOffset(text.spanStyles, offsetMapping) + newAnnotated.spanStyles)
                /*.correctSpanStyleRanges()*/,
            offsetMapping = offsetMapping
        )
    }
}

// offset mapping of environment variables and functions are the same
class FunctionTransformationOffsetMapping(variableRanges: TreeMap<Int, IntRange>) : EnvironmentVariableTransformationOffsetMapping(variableRanges)

package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppFont
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator

class EnvironmentVariableDecorator(themeColors: AppColor, font: AppFont, var knownVariables: Set<String>) :
    BigTextDecorator {
    val knownVariableStyle = SpanStyle(
        color = themeColors.variableTextColor,
        background = themeColors.variableBackgroundColor,
        fontFamily = font.monospaceFontFamily,
    )
    val unknownVariableStyle = SpanStyle(
        color = themeColors.variableTextColor,
        background = themeColors.variableErrorBackgroundColor,
        fontFamily = font.monospaceFontFamily,
    )

    override fun onApplyDecorationOnTransformation(text: CharSequence, transformedRange: IntRange): CharSequence {
        if (text is AnnotatedString) {
//            val tagRanges = text.getStringAnnotations(EnvironmentVariableIncrementalTransformation.TAG, 0, text.length)
            val tagRanges = text.spanStyles.filter { it.tag.startsWith(EnvironmentVariableIncrementalTransformation.TAG_PREFIX) }
            if (tagRanges.isNotEmpty()) {
                val previousSpanStyles = text.spanStyles
                val newSpanStyles = tagRanges.map { tagRange ->
                    val name = tagRange.tag.replaceFirst(EnvironmentVariableIncrementalTransformation.TAG_PREFIX, "")
                    val style = if (name in knownVariables) {
                        knownVariableStyle
                    } else {
                        unknownVariableStyle
                    }
                    AnnotatedString.Range(style, tagRange.start, tagRange.end, tagRange.tag)
                }
                return AnnotatedString(text.text, previousSpanStyles + newSpanStyles, text.paragraphStyles)
            }
        }
        return super.onApplyDecorationOnTransformation(text, transformedRange)
    }
}

package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEventType
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.IncrementalTextTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.TextFBDirection

class EnvironmentVariableIncrementalTransformation : IncrementalTextTransformation<Unit> {
    companion object {
        const val TAG_PREFIX = "EnvVar/"
        const val TAG = "EnvVar"
    }
    val processLengthLimit = 30

    private val variableRegex = "\\$\\{\\{([^{}]{1,$processLengthLimit})\\}\\}".toRegex()

    private val variableNameRegex = "[^{}\n\r]{1,$processLengthLimit}".toRegex()

    override fun initialize(text: BigText, transformer: BigTextTransformer) {
//        if (true) return

        // TODO avoid loading building string again which uses double memory
        val variables = variableRegex.findAll(text.buildString())
        variables.forEach {
            val variableName = it.groups[1]!!.value
            transformer.replace(it.range, createSpan(variableName), BigTextTransformOffsetMapping.WholeBlock)
        }
    }

    override fun afterTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {
        // TODO handle multiple matches (e.g. triggered by pasting text)

        val originalText = change.bigText
        when (change.eventType) {
            BigTextChangeEventType.Insert -> {
                // Find if there is pattern match ("\${{" or "}}") in the inserted text.
                // If yes, try to locate the pair within `processLengthLimit`, and make desired replacement.

                originalText.findPositionByPattern(
                    change.changeStartIndex,
                    change.changeEndExclusiveIndex,
                    "}}",
                    TextFBDirection.Forward
                ).also {
                    log.d { "EnvironmentVariableIncrementalTransformation search end end=$it" }
                }?.let {
                    val anotherBracket = originalText.findPositionByPattern(
                        it - processLengthLimit,
                        it - 1,
                        "\${{",
                        TextFBDirection.Backward
                    )
                    log.d { "EnvironmentVariableIncrementalTransformation search end start=$it" }
                    if (anotherBracket != null) {
                        val variableName = originalText.substring(anotherBracket + "\${{".length, it).string()
                        if (isValidVariableName(variableName)) {
                            log.d { "EnvironmentVariableIncrementalTransformation add '$variableName'" }
                            transformer.replace(
                                anotherBracket until it + "}}".length,
                                createSpan(variableName),
                                BigTextTransformOffsetMapping.WholeBlock
                            )
                        } else {
                            log.d { "variableName '$variableName' is invalid" }
                        }
                    }
                }
                originalText.findPositionByPattern(
                    change.changeStartIndex,
                    change.changeEndExclusiveIndex,
                    "\${{",
                    TextFBDirection.Forward
                ).also {
                    log.d { "EnvironmentVariableIncrementalTransformation search start start=$it" }
                }?.let {
                    val anotherBracket = originalText.findPositionByPattern(
                        it + "\${{".length,
                        it + processLengthLimit,
                        "}}",
                        TextFBDirection.Forward
                    )
                    log.d { "EnvironmentVariableIncrementalTransformation search start end=$it" }
                    if (anotherBracket != null) {
                        val variableName = originalText.substring(it + "\${{".length, anotherBracket).string()
                        if (isValidVariableName(variableName)) {
                            log.d { "EnvironmentVariableIncrementalTransformation add '$variableName'" }
                            transformer.replace(
                                it until anotherBracket + "}}".length,
                                createSpan(variableName),
                                BigTextTransformOffsetMapping.WholeBlock
                            )
                        } else {
                            log.d { "variableName '$variableName' is invalid" }
                        }
                    }
                }
            }

            else -> {}
        }
    }

    override fun beforeTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {
        // TODO handle multiple matches (e.g. triggered by pasting text)

        val originalText = change.bigText
        when (change.eventType) {
            BigTextChangeEventType.Delete -> {
                // Find if there is pattern match ("\${{" or "}}") in the inserted text.
                // If yes, try to locate the pair within `processLengthLimit`, and remove the transformation by restoring them to original.

                val changeRange = change.changeStartIndex until change.changeEndExclusiveIndex

                originalText.findPositionByPattern(change.changeStartIndex - processLengthLimit, change.changeEndExclusiveIndex, "}}", TextFBDirection.Backward)
                    ?.takeIf { (it until it + "}}".length) hasIntersectWith changeRange }
                    ?.let {
                        originalText.findPositionByPattern(it - processLengthLimit, it - 1, "\${{", TextFBDirection.Backward)
                            ?.let { anotherStart ->
                                log.d { "EnvironmentVariableIncrementalTransformation delete A" }
                                transformer.restoreToOriginal(anotherStart until it + "}}".length)
                            }
                    }
                originalText.findPositionByPattern(change.changeStartIndex - "\${{".length + 1, change.changeEndExclusiveIndex + "\${{".length, "\${{", TextFBDirection.Forward)
                    ?.takeIf { (it until it + "\${{".length) hasIntersectWith changeRange }
                    ?.let {
                        originalText.findPositionByPattern(it + "\${{".length, it + processLengthLimit, "}}", TextFBDirection.Forward)
                            ?.let { anotherStart ->
                                log.d { "EnvironmentVariableIncrementalTransformation delete B" }
                                transformer.restoreToOriginal(it until anotherStart + "}}".length)
                            }
                    }
            }

            else -> {}
        }


    }

    fun isValidVariableName(name: String): Boolean {
        return name.matches(variableNameRegex)
    }

//    @OptIn(ExperimentalTextApi::class)
    fun createSpan(variableName: String): CharSequence { // TODO change to AnnotatedString
//        return buildAnnotatedString {
//            withAnnotation(TAG, variableName) {
//                append(variableName)
//            }
//        }
        return AnnotatedString(variableName, listOf(AnnotatedString.Range(SpanStyle(), 0, variableName.length, "$TAG_PREFIX$variableName")))
    }
}

fun BigText.findPositionByPattern(fromPosition: Int, toPosition: Int, pattern: String, direction: TextFBDirection): Int? {
    val substringBeginIndex = maxOf(0, fromPosition - pattern.length)
    val substringEndExclusiveIndex = minOf(length, toPosition + pattern.length)
    val substring = substring(substringBeginIndex, substringEndExclusiveIndex)
    val lookupResult = when (direction) {
        TextFBDirection.Forward -> substring.indexOf(pattern)
        TextFBDirection.Backward -> substring.lastIndexOf(pattern)
    }
    return lookupResult.takeIf { it >= 0 }
        ?.let { substringBeginIndex + lookupResult }
        .also { log.d { "findPositionByPattern f=$fromPosition, t=$toPosition, s=$substringBeginIndex, e=$substringEndExclusiveIndex, sub=$substring, d=$direction, p=$pattern, res=$it" } }
        ?.takeIf { (it until it + pattern.length) hasIntersectWith (fromPosition until toPosition) }
}

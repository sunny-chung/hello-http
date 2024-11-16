package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TransformedText

val EMPTY_SELECTION_RANGE = 0 .. -1

class BigTextViewState {
    /**
     * A unique value that changes when the BigText string value is changed.
     *
     * This field is generated randomly and is NOT a sequence number.
     */
    var version: Long by mutableStateOf(0)
        internal set

    var firstVisibleRow: Int by mutableStateOf(0)
        internal set

    var lastVisibleRow: Int by mutableStateOf(0)
        internal set

    internal var transformedSelection: IntRange by mutableStateOf(0..-1)

    /**
     * `transformedSelectionStart` can be different from `transformedSelection.start`.
     * If a text is selected from position 5 to 1, transformedSelection = (1 .. 5) while transformedSelectionStart = 5.
     */
    var transformedSelectionStart: Int by mutableStateOf(0)

    var selection: IntRange by mutableStateOf(0..-1)

    fun hasSelection(): Boolean = !selection.isEmpty() && transformedSelection.start >= 0 && !transformedSelection.isEmpty()

    internal fun updateSelectionByTransformedSelection(transformedText: TransformedText) {
        selection = transformedText.offsetMapping.transformedToOriginal(transformedSelection.first) ..
                transformedText.offsetMapping.transformedToOriginal(transformedSelection.last)
    }

    internal fun updateTransformedSelectionBySelection(transformedText: TransformedText) {
        transformedSelection = transformedText.offsetMapping.originalToTransformed(selection.first) ..
                transformedText.offsetMapping.originalToTransformed(selection.last)
    }

    internal fun updateSelectionByTransformedSelection(transformedText: BigTextTransformed) {
        selection = if (transformedSelection.isEmpty()) {
            EMPTY_SELECTION_RANGE
        } else {
            transformedText.findOriginalPositionByTransformedPosition(transformedSelection.first) ..
                transformedText.findOriginalPositionByTransformedPosition(transformedSelection.last)
        }
    }

    internal fun updateTransformedSelectionBySelection(transformedText: BigTextTransformed) {
        transformedSelection = if (!selection.isEmpty()) {
            transformedText.findTransformedPositionByOriginalPosition(selection.first) ..
                    transformedText.findTransformedPositionByOriginalPosition(selection.last)
        } else {
            IntRange.EMPTY
        }
    }

    internal var transformedCursorIndex by mutableStateOf(0)
    var cursorIndex by mutableStateOf(0)

    internal fun updateCursorIndexByTransformed(transformedText: TransformedText) {
        cursorIndex = transformedText.offsetMapping.transformedToOriginal(transformedCursorIndex)
    }

    internal fun updateTransformedCursorIndexByOriginal(transformedText: TransformedText) {
        transformedCursorIndex = transformedText.offsetMapping.originalToTransformed(cursorIndex)
    }

    internal fun updateCursorIndexByTransformed(transformedText: BigTextTransformed) {
        cursorIndex = transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex).also {
            com.sunnychung.application.multiplatform.hellohttp.util.log.d { "cursorIndex = $it (from T $transformedCursorIndex)" }
        }
    }

    internal fun updateTransformedCursorIndexByOriginal(transformedText: BigTextTransformed) {
        transformedCursorIndex = transformedText.findTransformedPositionByOriginalPosition(cursorIndex).also {
            com.sunnychung.application.multiplatform.hellohttp.util.log.d { "updateTransformedCursorIndexByOriginal = $it (from $cursorIndex)" }
        }
        cursorIndex = transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex)
    }

    internal fun roundTransformedCursorIndex(direction: CursorAdjustDirection, transformedText: BigTextTransformed, compareWithPosition: Int, isOnlyWithinBlock: Boolean) {
        transformedCursorIndex = roundedTransformedCursorIndex(transformedCursorIndex, direction, transformedText, compareWithPosition, isOnlyWithinBlock).also {
            com.sunnychung.application.multiplatform.hellohttp.util.log.d { "roundedTransformedCursorIndex($transformedCursorIndex, $direction, ..., $compareWithPosition) = $it" }
        }
    }

    internal fun roundedTransformedCursorIndex(transformedCursorIndex: Int, direction: CursorAdjustDirection, transformedText: BigTextTransformed, compareWithPosition: Int, isOnlyWithinBlock: Boolean): Int {
        val possibleRange = 0 .. transformedText.length
        val previousMappedPosition = transformedText.findOriginalPositionByTransformedPosition(compareWithPosition)
        when (direction) {
            CursorAdjustDirection.Forward, CursorAdjustDirection.Backward -> {
                val step = if (direction == CursorAdjustDirection.Forward) 1 else -1
                var delta = 0
                while (transformedCursorIndex + delta in possibleRange) {
                    if (transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex + delta) != previousMappedPosition) {
                        return transformedCursorIndex + delta + if (isOnlyWithinBlock) {
                            // for backward, we find the last index that is same as `previousMappedPosition`
                            - step
                        } else {
                            // for forward, we find the first index that is different from `previousMappedPosition`
                            0
                        }
                    }
                    delta += step
                }
                // (transformedCursorIndex + delta) is out of range
                return transformedCursorIndex + delta - step
            }
            CursorAdjustDirection.Bidirectional -> {
                if (transformedCursorIndex >= transformedText.length) {
                    return transformedText.length
                }

                var delta = 0
                while ((transformedCursorIndex + delta in possibleRange || transformedCursorIndex - delta in possibleRange)) {
                    if (transformedCursorIndex + delta + 1 in possibleRange && transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex + delta + 1) != previousMappedPosition) {
                        return transformedCursorIndex + delta + if (transformedCursorIndex + delta - 1 in possibleRange && transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex + delta - 1) == previousMappedPosition) {
                            // position (transformedCursorIndex + delta) is a block,
                            // while position (transformedCursorIndex + delta + 1) is not a block.
                            // so return (transformedCursorIndex + delta + 1)
                            1
                        } else {
                            0
                        }
                    }
                    if (transformedCursorIndex - delta - 1 in possibleRange && transformedText.findOriginalPositionByTransformedPosition(transformedCursorIndex - delta - 1) != previousMappedPosition) {
                        // for backward, we find the last index that is same as `previousMappedPosition`
                        return transformedCursorIndex - delta //+ 1
                    }
                    ++delta
                }
                return transformedCursorIndex + delta - 1
            }
        }
    }

    private val charRangesToReapplyTransforms = mutableSetOf<IntRange>()

    fun requestReapplyTransformation(originalRange: IntRange) {
        com.sunnychung.application.multiplatform.hellohttp.util.log.d { "requestReapplyTransformation $originalRange" }
        charRangesToReapplyTransforms += originalRange
    }

    fun pollReapplyTransformCharRanges(): List<IntRange> {
        val result = charRangesToReapplyTransforms.toList()
        charRangesToReapplyTransforms.clear()
        return result
    }

    var transformText: BigTextTransformed? = null
        internal set
}

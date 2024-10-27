package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.length

data class BigTextInputOperation(
    val changes: List<BigTextInputChange>
)

data class BigTextInputChange(
    val type: BigTextChangeEventType,
    val buffer: TextBuffer,
    val bufferCharIndexes: IntRange,
    val positions: IntRange,
) {
    init {
        require(positions.length == bufferCharIndexes.length)
    }

    override fun toString(): String {
        val substring = buffer.substring(bufferCharIndexes.start, minOf(bufferCharIndexes.endInclusive + 1, bufferCharIndexes.start + 10))
        return "{$type, buf=$bufferCharIndexes, pos=$positions (${substring} ...)}"
    }
}
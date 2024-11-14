package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextTransformed : BigTextTransformer, BigText, BigTextLayoutable {

    val originalText: BigText

    val originalLength: Int

    override fun delete(range: IntRange): Int {
        return super.delete(range)
    }

    fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int

    fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int

    fun findFirstRowIndexByOriginalLineIndex(originalLineIndex: Int): Int

    fun findOriginalLineIndexByRowIndex(rowIndex: Int): Int

    /**
     * Request trigger reapplying transformation in the next UI pass.
     *
     * If BigText is used alone without UI framework, this function does nothing.
     */
    @Deprecated("This function has no use case.")
    fun requestReapplyTransformation(originalRange: IntRange)

    fun insertOriginal(
        pos: Int,
        nodeValue: BigTextNodeValue,
        bufferOffsetStart: Int = nodeValue.bufferOffsetStart,
        bufferOffsetEndExclusive: Int = nodeValue.bufferOffsetEndExclusive,
    )

    fun deleteOriginal(originalRange: IntRange, isReMapPositionNeeded: Boolean = true)
}

package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextTransformed : BigTextTransformer, BigText, BigTextLayoutable {

    override fun delete(range: IntRange): Int {
        return super.delete(range)
    }

    fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int

    fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int

    /**
     * Request trigger reapplying transformation in the next UI pass.
     *
     * If BigText is used alone without UI framework, this function does nothing.
     */
    fun requestReapplyTransformation(originalRange: IntRange)
}

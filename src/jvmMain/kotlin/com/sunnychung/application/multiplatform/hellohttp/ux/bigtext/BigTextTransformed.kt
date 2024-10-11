package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextTransformed : BigTextTransformer, BigText, BigTextLayoutable {

    override fun delete(range: IntRange): Int {
        return super.delete(range)
    }

    fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int

    fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int
}

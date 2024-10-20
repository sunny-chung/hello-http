package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface IncrementalTextTransformation<C> {

    fun initialize(text: BigText, transformer: BigTextTransformer): C

    fun beforeTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: C) = Unit
    fun afterTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: C) = Unit

    fun onReapplyTransform(text: BigText, originalRange: IntRange, transformer: BigTextTransformer, context: C) = Unit
}

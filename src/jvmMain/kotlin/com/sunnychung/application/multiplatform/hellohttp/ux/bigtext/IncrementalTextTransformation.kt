package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface IncrementalTextTransformation<C> {

    fun initialize(text: BigText, transformer: BigTextTransformer): C

    fun onTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: C)
}

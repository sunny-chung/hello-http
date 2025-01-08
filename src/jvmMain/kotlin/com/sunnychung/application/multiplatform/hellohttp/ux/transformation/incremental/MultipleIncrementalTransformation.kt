package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextChangeEvent
import com.sunnychung.lib.multiplatform.bigtext.core.transform.BigTextTransformer
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation

class MultipleIncrementalTransformation(val transformations: List<IncrementalTextTransformation<*>>) : IncrementalTextTransformation<Any?> {
    override fun initialize(text: BigText, transformer: BigTextTransformer): Any? {
        transformations.forEach {
            it.initialize(text, transformer)
        }
        return Unit
    }

    override fun beforeTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Any?) {
        transformations.forEach {
            (it as IncrementalTextTransformation<Any?>).beforeTextChange(change, transformer, context)
        }
    }

    override fun afterTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Any?) {
        transformations.forEach {
            (it as IncrementalTextTransformation<Any?>).afterTextChange(change, transformer, context)
        }
    }

    override fun onReapplyTransform(
        text: BigText,
        originalRange: IntRange,
        transformer: BigTextTransformer,
        context: Any?
    ) {
        transformations.forEach {
            (it as IncrementalTextTransformation<Any?>).onReapplyTransform(text, originalRange, transformer, context)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultipleIncrementalTransformation) return false

        if (transformations != other.transformations) return false

        return true
    }

    override fun hashCode(): Int {
        return transformations.hashCode()
    }

}

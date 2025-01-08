package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextChangeEvent
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator

class MultipleTextDecorator(val decorators: List<BigTextDecorator>): BigTextDecorator {
    override fun initialize(text: BigText) {
        decorators.forEach {
            it.initialize(text)
        }
    }

    override fun beforeTextChange(change: BigTextChangeEvent) {
        decorators.forEach {
            it.beforeTextChange(change)
        }
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        decorators.forEach {
            it.afterTextChange(change)
        }
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        var text = text
        decorators.forEach {
            text = it.onApplyDecorationOnOriginal(text, originalRange)
        }
        return text
    }

    override fun onApplyDecorationOnTransformation(text: CharSequence, transformedRange: IntRange): CharSequence {
        var text = text
        decorators.forEach {
            text = it.onApplyDecorationOnTransformation(text, transformedRange)
        }
        return text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultipleTextDecorator) return false

        if (decorators != other.decorators) return false

        return true
    }

    override fun hashCode(): Int {
        return decorators.hashCode()
    }
}

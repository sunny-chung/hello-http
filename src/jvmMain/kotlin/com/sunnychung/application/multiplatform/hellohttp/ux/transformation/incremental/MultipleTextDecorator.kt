package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextDecorator

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
}

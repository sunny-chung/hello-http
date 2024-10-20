package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextDecorator {

    fun initialize(text: BigText) = Unit

    fun beforeTextChange(change: BigTextChangeEvent) = Unit
    fun afterTextChange(change: BigTextChangeEvent) = Unit

    fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence = text
    fun onApplyDecorationOnTransformation(text: CharSequence, transformedRange: IntRange): CharSequence = text
}

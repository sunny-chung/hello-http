package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextDecorator {

    fun initialize(text: BigText)

    fun beforeTextChange(change: BigTextChangeEvent) = Unit
    fun afterTextChange(change: BigTextChangeEvent) = Unit

    fun onApplyDecoration(text: BigText, range: IntRange): CharSequence
}

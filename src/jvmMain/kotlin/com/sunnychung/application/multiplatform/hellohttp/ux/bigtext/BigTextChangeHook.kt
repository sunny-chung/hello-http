package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextChangeHook {

    fun afterInsertChunk(modifiedText: BigText, position: Int, newValue: BigTextNodeValue)

    fun afterDelete(modifiedText: BigText, position: IntRange)
}

package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextChangeCallback {

    fun onValuePreChange(eventType: BigTextChangeEventType, changeStartIndex: Int, changeEndExclusiveIndex: Int) = Unit

    fun onValuePostChange(eventType: BigTextChangeEventType, changeStartIndex: Int, changeEndExclusiveIndex: Int) = Unit
}

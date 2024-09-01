package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking

@Composable
fun rememberBigTextFieldState(cacheKey: String, bigText: BigTextImpl): BigTextFieldState {
    return remember(cacheKey) {
        BigTextFieldState(cacheKey, bigText, BigTextViewState())
    }
}

@Composable
fun rememberBigTextFieldState(cacheKey: String, initialValue: String = ""): BigTextFieldState {
    return rememberBigTextFieldState(cacheKey, BigText.createFromLargeString(initialValue))
}

class BigTextFieldState(val cacheKey: String, val text: BigTextImpl, val viewState: BigTextViewState) {
    private val valueChangesMutableFlow = MutableSharedFlow<BigTextChangeWithoutDetail>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val valueChangesFlow: SharedFlow<BigTextChangeWithoutDetail> = valueChangesMutableFlow

    internal fun emitValueChange(changeId: Long) {
        logV.v { "BigTextFieldState emitValueChange A $changeId" }
//        logV.v { "BigTextFieldState emitValueChange B $changeId" }
        valueChangesMutableFlow.tryEmit(BigTextChangeWithoutDetail(changeId = changeId, bigText = text))
    }
}

class BigTextChangeWithoutDetail(val changeId: Long, val bigText: BigTextImpl)

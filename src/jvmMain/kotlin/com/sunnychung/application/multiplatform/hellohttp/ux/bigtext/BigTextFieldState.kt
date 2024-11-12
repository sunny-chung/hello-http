package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter

/**
 * Create a BigTextFieldState with a large text buffer.
 *
 * The argument `initialValue` is only used when there is a cache miss using the cache key `cacheKey`.
 */
@Composable
fun rememberLargeAnnotatedBigTextFieldState(initialValue: String = "", vararg cacheKeys: Any?): MutableState<BigTextFieldState> {
    return rememberSaveable(*cacheKeys) {
        log.i { "cache miss 1" }
        mutableStateOf(
            BigTextFieldState(
                BigText.createFromLargeAnnotatedString(AnnotatedString(initialValue)),
                BigTextViewState()
            )
        )
    }
}

fun CharSequence.abbr(): CharSequence {
    return if (this.length > 20) {
        substring(0 .. 19)
    } else {
        this
    }
}

class BigTextFieldState(val text: BigText, val viewState: BigTextViewState) {
    private var lastSequence = -1
    private var lastConsumedSequence = -1

    private val valueChangesMutableFlow = MutableSharedFlow<BigTextChangeWithoutDetail>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val valueChangesFlow: Flow<BigTextChangeWithoutDetail> = valueChangesMutableFlow
        .filter { it.sequence > lastConsumedSequence }

    internal fun emitValueChange(changeId: Long) {
        logV.v { "BigTextFieldState emitValueChange A $changeId" }
//        logV.v { "BigTextFieldState emitValueChange B $changeId" }
        valueChangesMutableFlow.tryEmit(BigTextChangeWithoutDetail(changeId = changeId, bigText = text, sequence = ++lastSequence)).let { isSuccess ->
            if (!isSuccess) {
                logV.w { "BigTextFieldState emitValueChange fail. #Subscribers = ${valueChangesMutableFlow.subscriptionCount.value}" }
            }
        }
    }

    fun markConsumed(sequence: Int) {
        lastConsumedSequence = maxOf(lastConsumedSequence, sequence)
    }
}

class BigTextChangeWithoutDetail(val changeId: Long, val bigText: BigText, val sequence: Int)

package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.MutableObjectRef
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter

@Composable
fun rememberBigTextFieldState(initialValue: String = ""): Pair<MutableState<String>, MutableState<BigTextFieldState>> {
    val secondCacheKey = rememberSaveable { mutableStateOf(initialValue) }
    val state = rememberSaveable {
        log.d { "cache miss 1" }
        mutableStateOf(BigTextFieldState(BigText.createFromLargeString(initialValue), BigTextViewState()))
    }
    if (initialValue !== secondCacheKey.value) {
        log.d { "cache miss. old key2 = ${secondCacheKey.value.abbr()}; new key2 = ${initialValue.abbr()}" }
        secondCacheKey.value = initialValue
        state.value = BigTextFieldState(BigText.createFromLargeString(initialValue), BigTextViewState())
    }
    return secondCacheKey to state
}

@Composable
fun rememberAnnotatedBigTextFieldState(initialValue: AnnotatedString = AnnotatedString("")): Pair<MutableState<AnnotatedString>, MutableState<BigTextFieldState>> {
    val secondCacheKey = rememberSaveable { mutableStateOf(initialValue) }
    val state = rememberSaveable {
        log.d { "cache miss 1" }
        mutableStateOf(BigTextFieldState(BigText.createFromLargeAnnotatedString(initialValue), BigTextViewState()))
    }
    if (initialValue !== secondCacheKey.value) {
        log.d { "cache miss. old key2 = ${secondCacheKey.value.abbr()}; new key2 = ${initialValue.abbr()}" }
        secondCacheKey.value = initialValue
        state.value = BigTextFieldState(BigText.createFromLargeAnnotatedString(initialValue), BigTextViewState())
    }
    return secondCacheKey to state
}

@Composable
fun rememberAnnotatedBigTextFieldState(initialValue: String = ""): Pair<MutableObjectRef<String>, MutableState<BigTextFieldState>> {
    val secondCacheKey by rememberSaveable { mutableStateOf(MutableObjectRef(initialValue)) }
    val state = rememberSaveable {
        log.i { "cache miss 1" }
        mutableStateOf(BigTextFieldState(BigText.createFromLargeAnnotatedString(AnnotatedString(initialValue)), BigTextViewState()))
    }
    if (initialValue !== secondCacheKey.value) {
        log.i { "cache miss. old key2 = ${secondCacheKey.value.abbr()}; new key2 = ${initialValue.abbr()}" }

        secondCacheKey.value = initialValue
        state.value = BigTextFieldState(BigText.createFromLargeAnnotatedString(AnnotatedString(initialValue)), BigTextViewState())
//        log.i { "new view state = ${state.value.viewState}" }
    }
    return secondCacheKey to state
}

fun CharSequence.abbr(): CharSequence {
    return if (this.length > 20) {
        substring(0 .. 19)
    } else {
        this
    }
}

class BigTextFieldState(val text: BigTextImpl, val viewState: BigTextViewState) {
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

class BigTextChangeWithoutDetail(val changeId: Long, val bigText: BigTextImpl, val sequence: Int)

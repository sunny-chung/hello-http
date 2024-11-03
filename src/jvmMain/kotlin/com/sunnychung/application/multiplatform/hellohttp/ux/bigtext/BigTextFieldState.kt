package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import com.sunnychung.application.multiplatform.hellohttp.util.ObjectRef
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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
fun rememberAnnotatedBigTextFieldState(initialValue: String = ""): Pair<MutableState<ObjectRef<String>>, MutableState<BigTextFieldState>> {
    val secondCacheKey = rememberSaveable { mutableStateOf(ObjectRef(initialValue)) }
    val state = rememberSaveable {
        log.i { "cache miss 1" }
        mutableStateOf(BigTextFieldState(BigText.createFromLargeAnnotatedString(AnnotatedString(initialValue)), BigTextViewState()))
    }
    if (ObjectRef(initialValue) != secondCacheKey.value) {
        log.i { "cache miss. old key2 = ${secondCacheKey.value.ref.abbr()}; new key2 = ${initialValue.abbr()}" }

//        // set a value different from initialValue, otherwise the value 'equals' to the old value and would not be set to secondCacheKey.value
//        secondCacheKey.value = if (initialValue.isNotEmpty()) "" else " "
//        secondCacheKey.value = initialValue

        secondCacheKey.value = ObjectRef(initialValue)
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

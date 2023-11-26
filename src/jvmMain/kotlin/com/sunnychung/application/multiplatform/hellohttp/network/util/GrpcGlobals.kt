package com.sunnychung.application.multiplatform.hellohttp.network.util

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface State<T>
data class DataState<T>(val data: T) : State<T>
data class ErrorState<T>(val error: Throwable) : State<T>

fun <T> flowAndStreamObserver(): Pair<Flow<T>, StreamObserver<T>> {
    val scope = CoroutineScope(Dispatchers.IO)
    val channel = Channel<T>()
    val processedFlow = channel.receiveAsFlow()
    val streamObserver = object : StreamObserver<T> {
        override fun onNext(value: T) {
            scope.launch {
                channel.send(value)
            }
        }

        override fun onError(error: Throwable) {
            scope.launch {
                channel.close(error)
            }
        }

        override fun onCompleted() {
            channel.close(null)
        }

    }
    return Pair(processedFlow, streamObserver)
}

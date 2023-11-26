package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.replaceIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer

class RequestCollectionRepository : BaseCollectionRepository<RequestCollection, RequestsDI>(serializer()) {
//    private val index = ConcurrentHashMap<String, UserRequest>()

    private val publishNonPersistedRequestUpdates = MutableSharedFlow<Pair<String, UserRequestTemplate?>>()

    override fun relativeFilePath(id: RequestsDI): String = "requests/req-${id.subprojectId}.db"

    fun updateRequest(di: RequestsDI, update: UserRequestTemplate) {
        // dispatch immediately, or fast typing would lead to race conditions
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val collection = read(di)!!
            collection.requests.replaceIf(update) { it.id == update.id }
            notifyUpdated(di)
            publishNonPersistedRequestUpdates.emit(update.id to update)
        }
    }

    fun deleteRequest(di: RequestsDI, requestId: String): Boolean {
        return runBlocking {
            val collection = read(di)!!
            val hasRemoved = collection.requests.removeIf { it.id == requestId }
            if (hasRemoved) {
                notifyUpdated(di)
                coroutineScope.launch { // deadlock if not emit in another scope
                    publishNonPersistedRequestUpdates.emit(requestId to null)
                }
            }
            hasRemoved
        }
    }

    fun subscribeLatestRequest(di: RequestsDI, requestId: String): Flow<UserRequestTemplate?> = publishNonPersistedRequestUpdates
        .onSubscription {
//            val req = read(di)!!.requests.firstOrNull { it.id == requestId }
//            emit(Pair(requestId, req))
            emit(Pair(requestId, null))
        }
        .filter { it.first == requestId }
        .map {
            // revert to `it.second` if race condition is significant
//            it.second
            read(di)!!.requests.firstOrNull { it.id == requestId }
        }
}
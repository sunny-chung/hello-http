package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.application.multiplatform.hellohttp.network.ConnectionStatus
import com.sunnychung.application.multiplatform.hellohttp.network.NetworkEvent
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PersistResponseManager {

//    private val flows: MutableSet<Flow<NetworkManager.NetworkEvent>> = mutableSetOf()
//    private val networkManager by lazy { AppContext.NetworkManager }
    private val responseCollectionRepository by lazy { AppContext.ResponseCollectionRepository }
    private val requestCollectionRepository by lazy { AppContext.RequestCollectionRepository }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun registerCall(callData: CallData) {
        callData.jobs += callData.events.onEach {
            log.d { "PersistResponseManager receives call ${callData.id} event ${it.event}" }
            persistCallResponse(callData)
        }.launchIn(coroutineScope)
        if (callData.response.isError) {
            coroutineScope.launch {
                persistCallResponse(callData)

                // force refresh UI
                (callData.eventsStateFlow as MutableStateFlow<NetworkEvent?>).value =
                    NetworkEvent("", KInstant.now(), "", callData)
            }
        }
    }

    private suspend fun persistCallResponse(callData: CallData) {
        val documentId = ResponsesDI(subprojectId = callData.subprojectId)
        val record = loadResponseCollection(documentId)
        if (callData.status != ConnectionStatus.DISCONNECTED) { // prevent race condition among different calls of the same request example
            record.responsesByRequestExampleId[callData.response.requestExampleId] = callData.response
        }
        responseCollectionRepository.notifyUpdated(documentId)
    }

    /**
     * This method assumes `requestCollectionRepository.read` has already been called for the same subproject.
     */
    suspend fun loadResponseCollection(documentId: ResponsesDI): ResponseCollection {
        val result = responseCollectionRepository.readOrCreate(documentId) { id ->
            ResponseCollection(id, ConcurrentHashMap())
        }

        // cleanup responses that is not linked to an active request
        requestCollectionRepository.read(RequestsDI(subprojectId = documentId.subprojectId))?.let { requestCollection ->
            val requestExampleIds = requestCollection.requests.flatMap { it.examples }.map { it.id }.toSet()
            val keysOfDetachedResponses = result.responsesByRequestExampleId.keys - requestExampleIds
            if (keysOfDetachedResponses.isNotEmpty()) {
                keysOfDetachedResponses.forEach {
                    result.responsesByRequestExampleId.remove(key = it)
                }
                responseCollectionRepository.notifyUpdated(documentId)
            }
        }

        return result
    }
}

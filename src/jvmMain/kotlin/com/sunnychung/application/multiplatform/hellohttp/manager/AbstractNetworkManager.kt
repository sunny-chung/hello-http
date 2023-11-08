package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractNetworkManager : NetworkManager {

    protected val eventSharedFlow = MutableSharedFlow<NetworkEvent>()

    /**
     * MutableSharedFlow#collectAsState is buggy in Jetpack Compose
     * Copy to MutableStateFlow to make UI updates
     */
    protected val eventStateFlow = MutableStateFlow<NetworkEvent?>(null)

    protected val callData = ConcurrentHashMap<String, CallData>()

    init {
        eventSharedFlow.onEach { eventStateFlow.value = it }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    protected fun emitEvent(callId: String, event: String) {
        val instant = KInstant.now()
        runBlocking {
            eventSharedFlow.emit(
                NetworkEvent(
                    callId = callId,
                    instant = instant,
                    event = event
                )
            )
        }
    }

    override fun getCallData(callId: String) = callData[callId]

    protected fun createCallData(requestBodySize: Int?, requestExampleId: String, requestId: String, subprojectId: String): CallData {
        val outgoingBytesFlow = MutableSharedFlow<Pair<KInstant, ByteArray>>()
        val incomingBytesFlow = MutableSharedFlow<Pair<KInstant, ByteArray>>()
        val optionalResponseSize = AtomicInteger()

        val callId = uuidString()

        val data = CallData(
            id = callId,
            subprojectId = subprojectId,
            events = eventSharedFlow.asSharedFlow()
                .filter { it.callId == callId }
                .flowOn(Dispatchers.IO)
                .shareIn(CoroutineScope(Dispatchers.IO), started = SharingStarted.Eagerly),
            eventsStateFlow = eventStateFlow,
            outgoingBytes = outgoingBytesFlow,
            incomingBytes = incomingBytesFlow,
            optionalResponseSize = optionalResponseSize,
            response = UserResponse(id = uuidString(), requestId = requestId, requestExampleId = requestExampleId),
        )
        callData[callId] = data

        data.events
            .onEach {
                synchronized(data.response.rawExchange.exchanges) {
                    if (true || it.event == "Response completed") { // deadline fighter
                        data.response.rawExchange.exchanges.forEach {
                            it.consumePayloadBuilder()
                        }
                    } else { // lazy
                        val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                        lastExchange?.consumePayloadBuilder()
                    }
                    data.response.rawExchange.exchanges += RawExchange.Exchange(
                        instant = it.instant,
                        direction = RawExchange.Direction.Unspecified,
                        detail = it.event
                    )
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        data.outgoingBytes
            .onEach {
                synchronized(data.response.rawExchange.exchanges) {
                    val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                    if (lastExchange == null || lastExchange.direction != RawExchange.Direction.Outgoing) {
                        data.response.rawExchange.exchanges += RawExchange.Exchange(
                            instant = it.first,
                            direction = RawExchange.Direction.Outgoing,
                            detail = null,
                            payloadBuilder = ByteArrayOutputStream(maxOf(requestBodySize ?: 0, it.second.size + 1 * 1024 * 1024))
                        ).apply {
                            payloadBuilder!!.write(it.second)
                        }
                    } else {
                        lastExchange.payloadBuilder!!.write(it.second)
                        lastExchange.lastUpdateInstant = it.first
                    }
                    log.v { it.second.decodeToString() }
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        data.incomingBytes
            .onEach {
                synchronized(data.response.rawExchange.exchanges) {
                    val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                    if (lastExchange == null || lastExchange.direction != RawExchange.Direction.Incoming) {
                        data.response.rawExchange.exchanges += RawExchange.Exchange(
                            instant = it.first,
                            direction = RawExchange.Direction.Incoming,
                            detail = null,
                            payloadBuilder = ByteArrayOutputStream(maxOf(optionalResponseSize.get(), it.second.size + 1 * 1024 * 1024))
                        ).apply {
                            payloadBuilder!!.write(it.second)
                        }
                    } else {
                        lastExchange.payloadBuilder!!.write(it.second)
                        lastExchange.lastUpdateInstant = it.first
                    }
                    log.v { it.second.decodeToString() }
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        return data
    }

    protected suspend fun CallData.waitForPreparation() {
        val callData = this
        withTimeout(3000) {
            while (!callData.isPrepared) {
                log.d { "Wait for preparation" }
                delay(100)
                yield()
            }
        }
        // withTimeout and while(...) guarantee callData is prepared
        assert(callData.isPrepared)
    }

    fun executePostFlightAction(callId: String, out: UserResponse, postFlightAction: ((UserResponse) -> Unit)) {
        emitEvent(callId, "Executing Post Flight Actions")
        try {
            postFlightAction(out)
            emitEvent(callId, "Post Flight Actions Completed")
        } catch (e: Throwable) {
            out.postFlightErrorMessage = e.message
            emitEvent(callId, "Post Flight Actions Stopped with Error")
        }
    }
}
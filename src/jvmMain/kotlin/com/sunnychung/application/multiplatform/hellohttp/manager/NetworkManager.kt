package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.InspectInputStream
import com.sunnychung.application.multiplatform.hellohttp.network.InspectOutputStream
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.SocketSourceSinkTransformer
import okhttp3.internal.headersContentLength
import okio.Sink
import okio.Source
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

private val socketAsyncTimeoutClass = Class.forName("okio.SocketAsyncTimeout").kotlin
private val socketAsyncTimeoutConstructor = socketAsyncTimeoutClass.primaryConstructor!!.apply {
    isAccessible = true
}
private val socketAsyncTimeoutClassSinkMethod = socketAsyncTimeoutClass.memberFunctions.first { it.name == "sink" }.apply { isAccessible = true }
private val outputStreamSinkConstructor = Class.forName("okio.OutputStreamSink").kotlin.primaryConstructor!!.apply {
    isAccessible = true
}
private val socketAsyncTimeoutClassSourceMethod = socketAsyncTimeoutClass.memberFunctions.first { it.name == "source" }.apply { isAccessible = true }
private val inputStreamSourceConstructor = Class.forName("okio.InputStreamSource").kotlin.primaryConstructor!!.apply {
    isAccessible = true
}

class NetworkManager {
    class NetworkEvent(val callId: String, val instant: KInstant, val event: String)
    class CallData(
        val id: String,
        val subprojectId: String,
        var isPrepared: Boolean = false,

        val events: Flow<NetworkEvent>,
        val outgoingBytes: Flow<Pair<KInstant, ByteArray>>,
        val incomingBytes: Flow<Pair<KInstant, ByteArray>>,
        val optionalResponseSize: AtomicInteger,
        val response: UserResponse,
    )

    private val eventChannel = Channel<NetworkEvent>()
    private val callData = ConcurrentHashMap<String, CallData>()

    fun buildHttpClient(callId: String, outgoingBytesChannel: Channel<Pair<KInstant, ByteArray>>, incomingBytesChannel: Channel<Pair<KInstant, ByteArray>>, responseSize: AtomicInteger): OkHttpClient {

        fun logNetworkEvent(call: Call, event: String) {
            val instant = KInstant.now()
            runBlocking {
                log.d { "Network Event: $event" }
                eventChannel.send(NetworkEvent(callId = callId, instant = instant, event = event))
            }
        }

        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(
                maxIdleConnections = 0,
                keepAliveDuration = 30,
                timeUnit = TimeUnit.SECONDS
            ))
            .protocols(listOf(Protocol.HTTP_1_1)) // TODO support HTTP/2
            .eventListener(object : EventListener() {
                override fun callEnd(call: Call) {
                    logNetworkEvent(call, "Call ended")
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    logNetworkEvent(call, "Call failed")
                }

                override fun callStart(call: Call) {
                    logNetworkEvent(call, "Call starts")
                }

                override fun canceled(call: Call) {
                    logNetworkEvent(call, "Call cancelled")
                }

                override fun connectEnd(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy,
                    protocol: Protocol?
                ) {
                    logNetworkEvent(call, "Connected with $protocol")
                }

                override fun connectFailed(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy,
                    protocol: Protocol?,
                    ioe: IOException
                ) {
                    logNetworkEvent(call, "Connect failed with $protocol. Error: $ioe")
                }

                override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
                    logNetworkEvent(call, "Connect starts to $inetSocketAddress")
                }

                override fun connectionAcquired(call: Call, connection: Connection) {
                    logNetworkEvent(call, "Connection accquired")
                }

                override fun connectionReleased(call: Call, connection: Connection) {
                    logNetworkEvent(call, "Connection released")
                }

                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
                    logNetworkEvent(call, "DNS resolved to $inetAddressList")
                }

                override fun dnsStart(call: Call, domainName: String) {
                    logNetworkEvent(call, "DNS resolution of domain [$domainName] started")
                }

                override fun proxySelectEnd(call: Call, url: HttpUrl, proxies: List<Proxy>) {
                    logNetworkEvent(call, "Proxy enumerated: $proxies")
                }

                override fun proxySelectStart(call: Call, url: HttpUrl) {
                    logNetworkEvent(call, "Proxy selection starts")
                }

                override fun requestBodyEnd(call: Call, byteCount: Long) {

                }

                override fun requestBodyStart(call: Call) {

                }

                override fun requestFailed(call: Call, ioe: IOException) {
                    logNetworkEvent(call, "Send request failed. Error: $ioe")
                }

                override fun requestHeadersEnd(call: Call, request: Request) {

                }

                override fun requestHeadersStart(call: Call) {

                }

                override fun responseBodyEnd(call: Call, byteCount: Long) {

                }

                override fun responseBodyStart(call: Call) {

                }

                override fun responseFailed(call: Call, ioe: IOException) {
                    logNetworkEvent(call, "Receiving response failed. Error: $ioe")
                }

                override fun responseHeadersEnd(call: Call, response: Response) {
                    responseSize.set(response.headersContentLength().toInt())
                }

                override fun responseHeadersStart(call: Call) {

                }

                override fun satisfactionFailure(call: Call, response: Response) {
                    logNetworkEvent(call, "Call failed due to cache rules")
                }

                override fun secureConnectEnd(call: Call, handshake: Handshake?) {
                    logNetworkEvent(call, "Secure connection established")
                }

                override fun secureConnectStart(call: Call) {
                    logNetworkEvent(call, "Secure connection starts")
                }
            })
            .socketSourceSinkTransformer(SocketSourceSinkTransformer(
                mapSink = { socket, _ ->
                    val timeout = socketAsyncTimeoutConstructor.call(socket)
                    val sink = outputStreamSinkConstructor.call(InspectOutputStream(socket.getOutputStream(), outgoingBytesChannel), timeout) as Sink
                    socketAsyncTimeoutClassSinkMethod.call(timeout, sink) as Sink
                },

                mapSource = { socket, _ ->
                    val timeout = socketAsyncTimeoutConstructor.call(socket)
                    val source = inputStreamSourceConstructor.call(InspectInputStream(socket.getInputStream(), incomingBytesChannel), timeout) as Source
                    socketAsyncTimeoutClassSourceMethod.call(timeout, source) as Source
                }
            ))
            .build()
    }

    fun getCallData(callId: String) = callData[callId]

    fun sendRequest(request: Request, requestId: String, subprojectId: String): CallData {
        val outgoingBytesChannel: Channel<Pair<KInstant, ByteArray>> = Channel()
        val incomingBytesChannel: Channel<Pair<KInstant, ByteArray>> = Channel()
        val optionalResponseSize = AtomicInteger()

        val callId = uuidString()

        val httpClient = buildHttpClient(
            callId = callId,
            outgoingBytesChannel = outgoingBytesChannel,
            incomingBytesChannel = incomingBytesChannel,
            responseSize = optionalResponseSize
        )

        val call = NetworkCall(
            id = callId,
            call = httpClient.newCall(request),
            outgoingBytesChannel = outgoingBytesChannel,
            incomingBytesChannel = incomingBytesChannel
        )
        val data = CallData(
            id = call.id,
            subprojectId = subprojectId,
            events = eventChannel.receiveAsFlow()
                .filter { it.callId == call.id }
                .flowOn(Dispatchers.IO)
                .shareIn(CoroutineScope(Dispatchers.IO), started = SharingStarted.Eagerly),
            outgoingBytes = outgoingBytesChannel.receiveAsFlow()
                .flowOn(Dispatchers.IO)
                .shareIn(CoroutineScope(Dispatchers.IO), started = SharingStarted.Eagerly),
            incomingBytes = incomingBytesChannel.receiveAsFlow()
                .flowOn(Dispatchers.IO)
                .shareIn(CoroutineScope(Dispatchers.IO), started = SharingStarted.Eagerly),
            optionalResponseSize = optionalResponseSize,
            response = UserResponse(id = uuidString(), requestId = requestId),
        )
        callData[call.id] = data

        data.events
            .onEach {
                synchronized(data.response.rawExchange.exchanges) {
                    val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                    if (lastExchange?.payloadBuilder != null && lastExchange.payload == null) {
                        lastExchange.payload = lastExchange.payloadBuilder!!.toByteArray()
                        lastExchange.payloadBuilder = null
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
                            payloadBuilder = ByteArrayOutputStream(maxOf(request.body?.contentLength()?.toInt() ?: 0, it.second.size + 1 * 1024 * 1024))
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

        CoroutineScope(Dispatchers.IO).launch {
            val out = callData[call.id]!!.response
            out.startAt = KInstant.now()
            out.isCommunicating = true

            try {
                val response = call.await()

                out.statusCode = response.code
                out.statusText = response.message
                out.headers = response.headers.map { it }
                out.body = response.body?.bytes()
                out.responseSizeInBytes = out.body?.size?.toLong() ?: 0L // response.body?.contentLength() returns -1
            } catch (e: Throwable) {
                log.d { "Call Error: ${e.message}" }
                out.errorMessage = e.message
                out.isError = true
            } finally {
                out.endAt = KInstant.now()
                out.isCommunicating = false
            }

            eventChannel.send(NetworkEvent(call.id, KInstant.now(), "Response completed"))
        }
        return data
    }
}

internal class NetworkCall(
    val id: String,
    private val call: Call,
    val outgoingBytesChannel: Channel<Pair<KInstant, ByteArray>>,
    val incomingBytesChannel: Channel<Pair<KInstant, ByteArray>>,
) : Call by call {

    suspend fun await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isCancelled) return
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            })

            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                    // ignore
                }
            }
        }
    }
}

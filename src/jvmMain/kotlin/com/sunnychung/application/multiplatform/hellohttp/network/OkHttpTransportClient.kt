package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.extension.toOkHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.okhttp.GzipDecompressionInterceptor
import com.sunnychung.application.multiplatform.hellohttp.network.util.InspectInputStream
import com.sunnychung.application.multiplatform.hellohttp.network.util.InspectOutputStream
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
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
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
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

@Deprecated("Use ApacheHttpTransportClient")
class OkHttpTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {
    
    fun buildHttpClient(
        callId: String,
        sslConfig: SslConfig,
        outgoingBytesChannel: MutableSharedFlow<RawPayload>,
        incomingBytesChannel: MutableSharedFlow<RawPayload>,
        responseSize: AtomicInteger
    ): OkHttpClient {

        fun logNetworkEvent(call: Call, event: String) {
            val instant = KInstant.now()
            runBlocking {
                log.d { "Network Event: $event" }
                eventSharedFlow.emit(NetworkEvent(callId = callId, instant = instant, event = event, callData[callId] ?: return@runBlocking))
            }
        }

        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(
                maxIdleConnections = 0,
                keepAliveDuration = 30,
                timeUnit = TimeUnit.SECONDS
            ))
            .protocols(listOf(Protocol.HTTP_1_1)) // TODO support HTTP/2
            .apply {
                if (sslConfig.isInsecure == true) {
                    val (sslContext, keyManager, trustManager) = createSslContext(sslConfig)
                    sslSocketFactory(sslContext.socketFactory, trustManager!!)
                    hostnameVerifier { _, _ -> true }
                }
            }
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
//            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(GzipDecompressionInterceptor())
            .build()
    }

    override fun sendRequest(
        callId: String,
        coroutineScope: CoroutineScope,
        client: Any?,
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        subprojectConfig: SubprojectConfiguration,
        fireType: UserResponse.Type,
        loadTestState: LoadTestState?,
    ): CallData {
        val okHttpRequest = request.toOkHttpRequest()

        val data = createCallData(
            callId = callId,
            coroutineScope = coroutineScope,
            requestBodySize = okHttpRequest.body?.contentLength()?.toInt(),
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
            sslConfig = sslConfig,
            subprojectConfig = subprojectConfig,
            fireType = UserResponse.Type.Regular,
        )
        val callId = data.id

        val httpClient = buildHttpClient(
            callId = callId,
            sslConfig = sslConfig,
            outgoingBytesChannel = data.outgoingBytes as MutableSharedFlow<RawPayload>,
            incomingBytesChannel = data.incomingBytes as MutableSharedFlow<RawPayload>,
            responseSize = data.optionalResponseSize
        )

        val call = NetworkCall(
            id = callId,
            call = httpClient.newCall(okHttpRequest),
        )

        coroutineScope.launch {
            val callData = callData[call.id]!!
            callData.waitForPreparation()
            log.d { "Call ${call.id} is prepared" }

            val out = callData.response
            out.startAt = KInstant.now()
            out.isCommunicating = true
            callData.status = ConnectionStatus.CONNECTING

            try {
                val response = call.await()

                out.statusCode = response.code
                out.statusText = response.message
                out.headers = response.headers.map { it }
                out.body = response.body?.bytes()
                out.responseSizeInBytes = out.body?.size?.toLong() ?: 0L // response.body?.contentLength() returns -1
            } catch (e: Throwable) {
                log.d(e) { "Call Error: ${e.message}" }
                out.errorMessage = e.message
                out.isError = true
            } finally {
                out.endAt = KInstant.now()
                out.isCommunicating = false
                callData.status = ConnectionStatus.DISCONNECTED
            }

            if (!out.isError && postFlightAction != null) {
                executePostFlightAction(callId, out, postFlightAction)
            }

            eventSharedFlow.emit(NetworkEvent(call.id, KInstant.now(), "Response completed", callData))
        }
        return data
    }

    override fun createReusableNonInspectableClient(
        parentCallId: String,
        concurrency: Int,
        request: HttpRequest,
        httpConfig: HttpConfig,
        sslConfig: SslConfig
    ): Any? {
        return null
    }
}

internal class NetworkCall(
    val id: String,
    private val call: Call,
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

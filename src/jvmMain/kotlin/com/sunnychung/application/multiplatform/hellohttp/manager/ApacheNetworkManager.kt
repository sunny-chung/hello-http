package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.extension.toApacheHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.Protocol
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolVersion
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.apache.Http2FrameSerializer
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.config.TlsConfig
import org.apache.hc.client5.http.function.ConnectionListener
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpConnection
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.config.Http1Config
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.AsyncClientExchangeHandler
import org.apache.hc.core5.http.nio.AsyncPushConsumer
import org.apache.hc.core5.http.nio.CapacityChannel
import org.apache.hc.core5.http.nio.DataStreamChannel
import org.apache.hc.core5.http.nio.RequestChannel
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.http2.config.H2Config
import org.apache.hc.core5.http2.frame.FrameType
import org.apache.hc.core5.http2.frame.RawFrame
import org.apache.hc.core5.http2.hpack.HPackInspectHeader
import org.apache.hc.core5.http2.impl.nio.H2InspectListener
import org.apache.hc.core5.reactor.IOReactorConfig
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.Principal
import java.security.cert.Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ApacheNetworkManager(networkClientManager: NetworkClientManager) : AbstractNetworkManager(networkClientManager) {

    private fun buildHttpClient(
        callId: String,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
        outgoingBytesFlow: MutableSharedFlow<RawPayload>,
        incomingBytesFlow: MutableSharedFlow<RawPayload>,
        responseSize: AtomicInteger
    ): MinimalHttpAsyncClient {
        val http2FrameSerializer = Http2FrameSerializer()

        val dnsResolver = object : SystemDefaultDnsResolver() {
            override fun resolve(host: String): Array<InetAddress> {
                emitEvent(callId, "DNS resolution of domain [$host] started")
                val result = super.resolve(host)
                emitEvent(callId, "DNS resolved to ${result.contentToString()}")
                return result
            }
        }

        val httpVersionPolicy = when (httpConfig.protocolVersion) {
            HttpConfig.HttpProtocolVersion.Http1Only -> HttpVersionPolicy.FORCE_HTTP_1
            HttpConfig.HttpProtocolVersion.Http2Only -> HttpVersionPolicy.FORCE_HTTP_2
            HttpConfig.HttpProtocolVersion.Negotiate, null -> HttpVersionPolicy.NEGOTIATE
        }

        val httpClient = HttpAsyncClients.createMinimal(
            H2Config.DEFAULT,
            Http1Config.DEFAULT,
            IOReactorConfig.DEFAULT,
            PoolingAsyncClientConnectionManagerBuilder.create()
                .setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(httpVersionPolicy).build())
                .setDnsResolver(dnsResolver)
                .setTlsStrategy(ClientTlsStrategyBuilder.create()
                    .setSslContext(createSslContext(sslConfig).first)
                    .setHostnameVerifier(createHostnameVerifier(sslConfig))
                    .build())
                .setConnectionListener(object : ConnectionListener {
                    override fun onConnectedHost(remoteAddress: String, protocolVersion: String) {
                        emitEvent(callId, "Connected to $remoteAddress with $protocolVersion")
                    }

                    override fun onTlsUpgraded(
                        protocol: String,
                        cipherSuite: String,
                        applicationProtocol: String,
                        localPrincipal: Principal?,
                        localCertificates: Array<Certificate>?,
                        peerPrincipal: Principal?,
                        peerCertificates: Array<Certificate>?
                    ) {
                        emitEvent(callId, "Established TLS upgrade with protocol '$protocol', cipher suite '$cipherSuite' and application protocol '$applicationProtocol'.\n" +
                                "\n" +
                                "Client principal = $localPrincipal\n" +
//                                "Client certificates = ${localCertificates?.firstOrNull()}\n" +
                                "\n" +
                                "Server principal = $peerPrincipal\n"
//                                "Server certificates = ${peerCertificates?.firstOrNull()}\n"
                        )
                    }

                })
                .build(),
            { bytes, pos, len ->
                println("<< " + bytes.copyOfRange(pos, pos + len).decodeToString())
                runBlocking {
                    incomingBytesFlow.emit(Http1Payload(
                        instant = KInstant.now(),
                        payload = bytes.copyOfRange(pos, pos + len)
                    ))
                }
            },
            { bytes, pos, len ->
                runBlocking {
                    outgoingBytesFlow.emit(Http1Payload(
                        instant = KInstant.now(),
                        payload = bytes.copyOfRange(pos, pos + len)
                    ))
                }
//                println(">> " + bytes.copyOfRange(pos, pos + len).decodeToString())
            },
            object : H2InspectListener {
                val suspendedHeaderFrames = ConcurrentHashMap<Int, String>()

                override fun onHeaderInputDecoded(connection: HttpConnection, streamId: Int?, headers: MutableList<HPackInspectHeader>) {
                    val serialized = http2FrameSerializer.serializeHeaders(headers)
                    val frameHeader = suspendedHeaderFrames[streamId]!!
                    suspendedHeaderFrames.remove(streamId)
                    runBlocking {
                        incomingBytesFlow.emit(
                            Http2Frame(
                                instant = KInstant.now(),
                                streamId = streamId,
                                content = "${frameHeader}${serialized}\n",
                            )
                        )
                    }
                }

                override fun onHeaderOutputEncoded(connection: HttpConnection, streamId: Int?, headers: MutableList<HPackInspectHeader>) {
                    val serialized = http2FrameSerializer.serializeHeaders(headers)
                    val frameHeader = suspendedHeaderFrames[streamId]!!
                    suspendedHeaderFrames.remove(streamId)
                    runBlocking {
                        outgoingBytesFlow.emit(
                            Http2Frame(
                                instant = KInstant.now(),
                                streamId = streamId,
                                content = "${frameHeader}${serialized}\n",
                            )
                        )
                    }
                }

                override fun onFrameInput(connection: HttpConnection, streamId: Int?, frame: RawFrame) {
                    processFrame(streamId = streamId, frame = frame, receiverFlow = incomingBytesFlow)
                }

                override fun onFrameOutput(connection: HttpConnection, streamId: Int?, frame: RawFrame) {
                    processFrame(streamId = streamId, frame = frame, receiverFlow = outgoingBytesFlow)
                }

                fun processFrame(streamId: Int?, frame: RawFrame, receiverFlow: MutableSharedFlow<RawPayload>) {
                    val serialized = http2FrameSerializer.serializeFrame(frame)
                    val type = FrameType.valueOf(frame.type)
                    if (type == FrameType.HEADERS) {
                        suspendedHeaderFrames[streamId!!] = serialized
                    } else {
                        runBlocking {
                            receiverFlow.emit(
                                Http2Frame(
                                    instant = KInstant.now(),
                                    streamId = streamId,
                                    content = serialized,
                                )
                            )
                        }
                    }
                }

            }
        )

        return httpClient
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig
    ): CallData {
        val (apacheHttpRequest, requestBodySize) = request.toApacheHttpRequest()

        val data = createCallData(
            requestBodySize = requestBodySize.toInt(),
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId,
        )
        val callId = data.id

        val httpClient = buildHttpClient(
            callId = callId,
            httpConfig = httpConfig,
            sslConfig = sslConfig,
            outgoingBytesFlow = data.outgoingBytes as MutableSharedFlow<RawPayload>,
            incomingBytesFlow = data.incomingBytes as MutableSharedFlow<RawPayload>,
            responseSize = data.optionalResponseSize
        )
        httpClient.start()

        // TODO: Should we remove push promise support completely?
        httpClient.register("*") {
            object : AsyncPushConsumer {
                override fun releaseResources() {

                }

                override fun updateCapacity(capacityChannel: CapacityChannel?) {

                }

                override fun consume(src: ByteBuffer?) {

                }

                override fun streamEnd(trailers: MutableList<out Header>?) {

                }

                override fun consumePromise(
                    promise: org.apache.hc.core5.http.HttpRequest?,
                    response: HttpResponse?,
                    entityDetails: EntityDetails?,
                    context: HttpContext?
                ) {

                }

                override fun failed(error: java.lang.Exception) {
                    error.printStackTrace()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val callData = callData[callId]!!
            callData.waitForPreparation()
            log.d { "Call $callId is prepared" }

            val consumer = SimpleResponseConsumer.create()
            val producer = apacheHttpRequest

            val out = callData.response
            out.startAt = KInstant.now()
            out.isCommunicating = true

            try {

                val response = suspendCancellableCoroutine<SimpleHttpResponse?> { continuation ->

                    var result: SimpleHttpResponse? = null

                    httpClient.execute(object : AsyncClientExchangeHandler {
                        override fun releaseResources() {
                            println("releaseResources")
                            producer.releaseResources()
                            consumer.releaseResources()
                        }

                        override fun updateCapacity(channel: CapacityChannel) {
                            consumer.updateCapacity(channel)
                        }

                        override fun consume(src: ByteBuffer) {
                            consumer.consume(src)
                        }

                        override fun streamEnd(trailers: MutableList<out Header>?) {
                            println("streamEnd")
                            consumer.streamEnd(trailers)
                            continuation.resume(result, null)
                        }

                        override fun available(): Int {
                            return producer.available()
                        }

                        override fun produce(channel: DataStreamChannel) {
                            producer.produce(channel)
                        }

                        override fun failed(exception: Exception) {
                            println("failed ${exception}")
                            exception.printStackTrace()
                            consumer.failed(exception)
                            continuation.cancel(exception)
                        }

                        override fun produceRequest(channel: RequestChannel, context: HttpContext) {
                            producer.sendRequest(channel, context)
                        }

                        override fun consumeResponse(
                            response: HttpResponse,
                            entityDetails: EntityDetails?,
                            context: HttpContext?
                        ) {
                            println("consumeResponse ${StatusLine(response)}")
                            out.protocol = ProtocolVersion(
                                protocol = Protocol.Http,
                                major = response.version.major,
                                minor = response.version.minor
                            )
                            out.statusCode = response.code
                            out.statusText = response.reasonPhrase
                            out.headers = response.headers?.map { it.name to it.value }
                            consumer.consumeResponse(
                                response,
                                entityDetails,
                                context,
                                object : FutureCallback<SimpleHttpResponse> {
                                    override fun completed(response: SimpleHttpResponse) {
                                        result = response
                                    }

                                    override fun failed(error: Exception) {

                                    }

                                    override fun cancelled() {

                                    }

                                })
                        }

                        override fun consumeInformation(p0: HttpResponse?, p1: HttpContext?) {
                        }

                        override fun cancel() {
                            println("cancel")
                            continuation.cancel()
                        }

                    })
                }

                out.statusCode = response?.code
                out.statusText = response?.reasonPhrase
                out.headers = response?.headers?.map { it.name to it.value }
                out.body = response?.bodyBytes
                out.responseSizeInBytes = out.body?.size?.toLong() ?: 0L

            } catch (e: Throwable) {
                out.errorMessage = e.message
                out.isError = true
            } finally {
                out.endAt = KInstant.now()
                out.isCommunicating = false
            }

            if (!out.isError && postFlightAction != null) {
                executePostFlightAction(callId, out, postFlightAction)
            }

            httpClient.close()
//            httpClient.awaitShutdown(TimeValue.ofSeconds(2))
            data.consumePayloads()

            emitEvent(callId, "Response completed")
        }
        return data
    }
}
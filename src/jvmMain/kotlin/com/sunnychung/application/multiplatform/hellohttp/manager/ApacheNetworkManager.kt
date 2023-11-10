package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.extension.toApacheHttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.config.TlsConfig
import org.apache.hc.client5.http.function.ConnectionListener
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.config.Http1Config
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.AsyncClientExchangeHandler
import org.apache.hc.core5.http.nio.CapacityChannel
import org.apache.hc.core5.http.nio.DataStreamChannel
import org.apache.hc.core5.http.nio.RequestChannel
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder
import org.apache.hc.core5.http.nio.support.BasicRequestProducer
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.http2.config.H2Config
import org.apache.hc.core5.reactor.IOReactorConfig
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.Principal
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicInteger

class ApacheNetworkManager : AbstractNetworkManager() {

    private fun buildHttpClient(
        callId: String,
        sslConfig: SslConfig,
        outgoingBytesFlow: MutableSharedFlow<Pair<KInstant, ByteArray>>,
        incomingBytesFlow: MutableSharedFlow<Pair<KInstant, ByteArray>>,
        responseSize: AtomicInteger
    ): MinimalHttpAsyncClient {
        val dnsResolver = object : SystemDefaultDnsResolver() {
            override fun resolve(host: String): Array<InetAddress> {
                emitEvent(callId, "DNS resolution of domain [$host] started")
                val result = super.resolve(host)
                emitEvent(callId, "DNS resolved to ${result.contentToString()}")
                return result
            }
        }

        val httpClient = HttpAsyncClients.createMinimal(
            H2Config.DEFAULT,
            Http1Config.DEFAULT,
            IOReactorConfig.DEFAULT,
            PoolingAsyncClientConnectionManagerBuilder.create()
                .setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1).build())
                .setDnsResolver(dnsResolver)
                .setConnectionListener(object : ConnectionListener {
                    override fun onConnectedHost(remoteAddress: String, protocolVersion: String) {
                        emitEvent(callId, "Connected to $remoteAddress with $protocolVersion")
                    }

                    override fun onTlsUpgraded(
                        protocol: String,
                        cipherSuite: String,
                        localPrincipal: Principal?,
                        localCertificates: Array<Certificate>?,
                        peerPrincipal: Principal?,
                        peerCertificates: Array<Certificate>?
                    ) {
                        emitEvent(callId, "Established TLS upgrade with protocol $protocol and cipher suite $cipherSuite.\n" +
                                "\n" +
                                "Local principal = $localPrincipal\n" +
                                "Local certificates = ${localCertificates?.firstOrNull()}\n" +
                                "\n" +
                                "Remote principal = $peerPrincipal\n" +
                                "Remote certificates = ${peerCertificates?.firstOrNull()}\n"
                        )
                    }

                })
                .build(),
            { bytes, pos, len ->
//                println("<< " + bytes.copyOfRange(pos, pos + len).decodeToString())
                runBlocking {
                    incomingBytesFlow.emit(KInstant.now() to bytes.copyOfRange(pos, pos + len))
                }
            },
            { bytes, pos, len ->
                runBlocking {
                    outgoingBytesFlow.emit(KInstant.now() to bytes.copyOfRange(pos, pos + len))
                }
//                println(">> " + bytes.copyOfRange(pos, pos + len).decodeToString())
            },
            null
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
            sslConfig = sslConfig,
            outgoingBytesFlow = data.outgoingBytes as MutableSharedFlow<Pair<KInstant, ByteArray>>,
            incomingBytesFlow = data.incomingBytes as MutableSharedFlow<Pair<KInstant, ByteArray>>,
            responseSize = data.optionalResponseSize
        )
        httpClient.start()

        CoroutineScope(Dispatchers.IO).launch {
            val callData = callData[callId]!!
            callData.waitForPreparation()
            log.d { "Call $callId is prepared" }

            val consumer = SimpleResponseConsumer.create()
            val producer = apacheHttpRequest

            val out = callData.response
            out.startAt = KInstant.now()
            out.isCommunicating = true

            val response = suspendCancellableCoroutine<SimpleHttpResponse?> { continuation ->

                var result: SimpleHttpResponse? = null

                httpClient.execute(object : AsyncClientExchangeHandler {
                    override fun releaseResources() {
                        println("releaseResources")
                        producer.releaseResources()
                        consumer.releaseResources()
                        continuation.resume(result, null)
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
                        consumer.failed(exception)
                        continuation.cancel(exception)
                    }

                    override fun produceRequest(channel: RequestChannel, context: HttpContext) {
                        producer.sendRequest(channel, context)
                    }

                    override fun consumeResponse(response: HttpResponse, entityDetails: EntityDetails?, context: HttpContext?) {
                        println("consumeResponse ${StatusLine(response)}")
                        consumer.consumeResponse(response, entityDetails, context, object : FutureCallback<SimpleHttpResponse> {
                            override fun completed(response: SimpleHttpResponse) {
                                result = response
                            }

                            override fun failed(p0: java.lang.Exception?) {

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

            // TODO error handling

            out.endAt = KInstant.now()
            out.isCommunicating = false

            if (!out.isError && postFlightAction != null) {
                executePostFlightAction(callId, out, postFlightAction)
            }

            emitEvent(callId, "Response completed")
        }
        return data
    }
}
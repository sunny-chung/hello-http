package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.extension.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.manager.CallDataStore
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestState
import com.sunnychung.application.multiplatform.hellohttp.model.LongDuplicateContainer
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponseByResponseTime
import com.sunnychung.application.multiplatform.hellohttp.network.util.DenyAllSslCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.network.util.MultipleTrustCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.network.util.TrustAllSslCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
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
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

abstract class AbstractTransportClient internal constructor(callDataStore: CallDataStore) : TransportClient {

    protected val eventSharedFlow = MutableSharedFlow<NetworkEvent>()

    /**
     * MutableSharedFlow#collectAsState is buggy in Jetpack Compose
     * Copy to MutableStateFlow to make UI updates
     */
    protected val eventStateFlow = MutableStateFlow<NetworkEvent?>(null)

    protected val callData = callDataStore.provideCallDataStore()

    init {
        eventSharedFlow.onEach { eventStateFlow.value = it }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    override fun emitEvent(callId: String, event: String, isForce: Boolean) {
        if (!isForce && (callData[callId]?.let { it.fireType == UserResponse.Type.LoadTestChild } != false)) {
            return
        }
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

    protected fun coroutineExceptionHandler() = CoroutineExceptionHandler { context, ex ->
        log.w(ex) { "Uncaught exception in coroutine ${context[CoroutineName.Key] ?: "-"}" }
    }

    internal fun createSslContext(sslConfig: SslConfig): CustomSsl {
        return SSLContext.getInstance("TLS")
            .run {
                val trustManager = if (sslConfig.isInsecure == true) {
                    TrustAllSslCertificateManager()
                } else {
                    val customCaCertificates = sslConfig.trustedCaCertificates.filter { it.isEnabled }
                    Unit.takeIf { customCaCertificates.isNotEmpty() || sslConfig.isDisableSystemCaCertificates == true }?.let {
                        val defaultX509TrustManager = createTrustManager(null)
                        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
                        trustStore.load(null)
                        customCaCertificates.map {
                            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it.content.inputStream())
                            trustStore.setCertificateEntry(it.name, cert)
                        }
                        val customTrustManager = createTrustManager(trustStore)
                        val combinedTrustManager = MultipleTrustCertificateManager(
                            listOfNotNull(
                                defaultX509TrustManager.takeIf { sslConfig.isDisableSystemCaCertificates != true },
                                customTrustManager.takeIf { customCaCertificates.isNotEmpty() }
                            ).emptyToNull() ?: listOf(DenyAllSslCertificateManager())
                        )
                        combinedTrustManager
                    }
                }
                val keyManager = sslConfig.clientCertificateKeyPairs.firstOrNull { it.isEnabled }?.let {
                    val cert = CertificateFactory.getInstance("X.509").generateCertificate(it.certificate.content.inputStream())
                    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(it.privateKey.content))

                    val password = uuidString()
                    val keyStore = KeyStore.getInstance("JKS")
                    keyStore.load(null)
                    keyStore.setCertificateEntry("cert", cert)
                    keyStore.setKeyEntry("key", key, password.toCharArray(), arrayOf(cert))
                    val keyManagers = KeyManagerFactory.getInstance("SunX509")
                        .apply { init(keyStore, password.toCharArray()) }
                        .keyManagers
                    keyManagers.first()
                }
                init(keyManager?.let { arrayOf(it) }, trustManager?.let { arrayOf(it) }, SecureRandom())
                CustomSsl(sslContext = this, keyManager = keyManager, trustManager = trustManager)
            }
    }

    private fun createTrustManager(keystore: KeyStore?): X509TrustManager {
        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply {
                init(keystore)
            }
            .trustManagers
            .filterIsInstance(X509TrustManager::class.java)
            .first()
    }

    protected fun createHostnameVerifier(sslConfig: SslConfig): HostnameVerifier? {
        if (sslConfig.isInsecure == true) {
            return HostnameVerifier { _, _ -> true }
        } else {
            return null
        }
    }

    override fun getCallData(callId: String) = callData[callId]

    override fun createCallData(
        callId: String?,
        requestBodySize: Int?,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        sslConfig: SslConfig,
        fireType: UserResponse.Type,
        loadTestState: LoadTestState?,
    ): CallData {
        val outgoingBytesFlow = MutableSharedFlow<RawPayload>()
        val incomingBytesFlow = MutableSharedFlow<RawPayload>()
        val optionalResponseSize = AtomicInteger()

        val callId = callId ?: uuidString()

        val data = CallData(
            id = callId,
            subprojectId = subprojectId,
            sslConfig = sslConfig.copy(),
            events = eventSharedFlow.asSharedFlow()
                .filter { it.callId == callId }
                .flowOn(Dispatchers.IO)
                .shareIn(CoroutineScope(Dispatchers.IO), started = SharingStarted.Eagerly),
            eventsStateFlow = eventStateFlow,
            outgoingBytes = outgoingBytesFlow,
            incomingBytes = incomingBytesFlow,
            optionalResponseSize = optionalResponseSize,
            response = UserResponse(id = uuidString(), requestId = requestId, requestExampleId = requestExampleId, type = fireType),
            fireType = fireType,
            loadTestState = loadTestState,
            cancel = {}
        )
        callData[callId] = data

        if (fireType != UserResponse.Type.LoadTestChild) {
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
                        if (it is Http2Frame || lastExchange == null || lastExchange.direction != RawExchange.Direction.Outgoing) {
                            data.response.rawExchange.exchanges += RawExchange.Exchange(
                                instant = it.instant,
                                direction = RawExchange.Direction.Outgoing,
                                streamId = if (it is Http2Frame) it.streamId else null,
                                detail = null,
                                payloadBuilder = ByteArrayOutputStream(
                                    maxOf(
                                        requestBodySize ?: 0,
                                        it.payload.size + 1 * 1024 * 1024
                                    )
                                )
                            ).apply {
                                payloadBuilder!!.write(it.payload)
                            }
                        } else {
                            lastExchange.payloadBuilder!!.write(it.payload)
                            lastExchange.lastUpdateInstant = it.instant
                        }
                        log.v { it.payload.decodeToString() }
                    }
                }
                .launchIn(CoroutineScope(Dispatchers.IO))

            data.incomingBytes
                .onEach {
                    synchronized(data.response.rawExchange.exchanges) {
                        val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                        if (it is Http2Frame || lastExchange == null || lastExchange.direction != RawExchange.Direction.Incoming) {
                            data.response.rawExchange.exchanges += RawExchange.Exchange(
                                instant = it.instant,
                                direction = RawExchange.Direction.Incoming,
                                streamId = if (it is Http2Frame) it.streamId else null,
                                detail = null,
                                payloadBuilder = ByteArrayOutputStream(
                                    maxOf(
                                        optionalResponseSize.get(),
                                        it.payload.size + 1 * 1024 * 1024
                                    )
                                )
                            ).apply {
                                payloadBuilder!!.write(it.payload)
                            }
                        } else {
                            lastExchange.payloadBuilder!!.write(it.payload)
                            lastExchange.lastUpdateInstant = it.instant
                        }
                        log.v { it.payload.decodeToString() }
                    }
                }
                .launchIn(CoroutineScope(Dispatchers.IO))
        }

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

    protected fun CallData.consumePayloads() {
        response.rawExchange.exchanges.forEach {
            it.consumePayloadBuilder()
        }
    }

    fun executePostFlightAction(callId: String, out: UserResponse, postFlightAction: ((UserResponse) -> Unit)) {
        if (callData[callId]?.let { it.fireType != UserResponse.Type.Regular } != false) {
            return
        }

        emitEvent(callId, "Executing Post Flight Actions")
        try {
            postFlightAction(out)
            emitEvent(callId, "Post Flight Actions Completed")
        } catch (e: Throwable) {
            out.postFlightErrorMessage = e.message
            emitEvent(callId, "Post Flight Actions Stopped with Error -- ${e.message}")
        }
    }

    fun completeResponse(callId: String, response: UserResponse) {
        val call = callData[callId] ?: return
        call.complete()
    }
}

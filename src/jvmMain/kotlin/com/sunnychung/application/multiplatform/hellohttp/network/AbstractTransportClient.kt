package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.extension.emptyToNull
import com.sunnychung.application.multiplatform.hellohttp.manager.CallDataStore
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_PAYLOAD_STORAGE_SIZE_LIMIT
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.util.DenyAllSslCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.network.util.MultipleTrustCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.network.util.TrustAllSslCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
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
import kotlinx.coroutines.launch
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

    protected fun emitEvent(callId: String, event: String) =
        emitEvent(instant = KInstant.now(), callId = callId, event = event)

    protected fun emitEvent(instant: KInstant, callId: String, event: String) {
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

    fun createCallData(
        requestBodySize: Int?,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        sslConfig: SslConfig,
        subprojectConfig: SubprojectConfiguration,
    ): CallData {
        val outgoingBytesFlow = MutableSharedFlow<RawPayload>()
        val incomingBytesFlow = MutableSharedFlow<RawPayload>()
        val optionalResponseSize = AtomicInteger()

        val callId = uuidString()

        val outboundPayloadStorageLimit = subprojectConfig.outboundPayloadStorageLimit.takeIf { it >= 0 } ?: DEFAULT_PAYLOAD_STORAGE_SIZE_LIMIT
        val inboundPayloadStorageLimit = subprojectConfig.inboundPayloadStorageLimit.takeIf { it >= 0 } ?: DEFAULT_PAYLOAD_STORAGE_SIZE_LIMIT

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
            response = UserResponse(id = uuidString(), requestId = requestId, requestExampleId = requestExampleId),
            cancel = {}
        )
        log.d { "Registering call #$callId" }
        callData[callId] = data

        data.jobs += data.events
            .onEach {
                synchronized(data.response.rawExchange.exchanges) {
                    if (true || it.event == "Response completed") { // deadline fighter
                        data.consumePayloads()
                        if (it.event == "Response completed") {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(3.seconds().millis)
                                data.response.rawExchange.exchanges.lastOrNull()
                                    ?.consumePayloadBuilder(isComplete = true)
                            }
                        }
                    } else { // lazy
                        val lastExchange = data.response.rawExchange.exchanges.lastOrNull()
                        lastExchange?.consumePayloadBuilder(isComplete = false)
                    }
                    if (it.event.isNotEmpty()) { // there are some "empty" events not for display
                        data.response.rawExchange.exchanges += RawExchange.Exchange(
                            instant = it.instant,
                            direction = RawExchange.Direction.Unspecified,
                            detail = it.event
                        )
                    }
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        fun processRawPayload(it: RawPayload, direction: RawExchange.Direction, approximateSize: Int?, storageLimit: Long) {
            synchronized(data.response.rawExchange.exchanges) {
                val lastIndex = data.response.rawExchange.exchanges.lastIndex
                val lastExchange = lastIndex.takeIf { it >= 0 }?.let { i -> data.response.rawExchange.exchanges[i] }
                if (it is Http2Frame || lastExchange == null || lastExchange.direction != direction || (lastExchange.streamId ?: -1) >= 0) {
                    data.response.rawExchange.exchanges += RawExchange.Exchange(
                        instant = it.instant,
                        direction = direction,
                        streamId = if (it is Http2Frame) (it.streamId ?: 0) else null,
                        detail = null,
                        payloadBuilder = ByteArrayOutputStream(minOf(approximateSize ?: Int.MAX_VALUE, it.payload.size + 64 * 1024))
                    ).apply {
                        unsafeWritePayloadBytes(bytes = it.payload, limit = storageLimit)
                    }
                    if (lastExchange?.direction != direction) {
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1.seconds().millis)
                            with(data.response.rawExchange.exchanges) {
                                synchronized(this) {
                                    (0..lastIndex).forEach { i ->
                                        this[i].consumePayloadBuilder(isComplete = true)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    lastExchange.unsafeWritePayloadBytes(bytes = it.payload, limit = storageLimit)
                    lastExchange.lastUpdateInstant = it.instant
                }
                log.v { it.payload.decodeToString() }
            }
        }

        data.jobs += data.outgoingBytes
            .onEach {
                processRawPayload(
                    it = it,
                    direction = RawExchange.Direction.Outgoing,
                    approximateSize = requestBodySize,
                    storageLimit = outboundPayloadStorageLimit,
                )
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        data.jobs += data.incomingBytes
            .onEach {
                processRawPayload(
                    it = it,
                    direction = RawExchange.Direction.Incoming,
                    approximateSize = optionalResponseSize.get(),
                    storageLimit = inboundPayloadStorageLimit,
                )
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        log.d { "Created call #$callId" }

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
            emitEvent(callId, "Post Flight Actions Stopped with Error -- ${e.message}")
        }
    }
}

fun CallData.consumePayloads(isComplete: Boolean = false) {
    synchronized(response.rawExchange.exchanges) {
        response.rawExchange.exchanges.forEachIndexed { index, it ->
            it.consumePayloadBuilder(isComplete = isComplete || index < response.rawExchange.exchanges.lastIndex)
        }
    }
}

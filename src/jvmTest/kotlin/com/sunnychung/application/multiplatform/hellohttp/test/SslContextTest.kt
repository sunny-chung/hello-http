package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.importFrom
import com.sunnychung.application.multiplatform.hellohttp.network.ApacheHttpTransportClient
import java.io.File
import kotlin.test.Test

class SslContextTest {

    val httpClient = ApacheHttpTransportClient(NetworkClientManager())

    @Test
    fun `insecure SSL should still prepare client certificate if available`() {
        val sslConfig = SslConfig(
            isInsecure = true,
            clientCertificateKeyPairs = listOf(
                ClientCertificateKeyPair.importFrom(
                    certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file),
                    keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.der")!!.file),
                    keyPassword = ""
                )
            )
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.keyManager != null)
    }
}
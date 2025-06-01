package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.network.SpringWebClientTransportClient
import com.sunnychung.application.multiplatform.hellohttp.util.importCaCertificates
import com.sunnychung.application.multiplatform.hellohttp.util.importFrom
import com.sunnychung.application.multiplatform.hellohttp.util.parseCaCertificates
//import com.sunnychung.application.multiplatform.hellohttp.network.ApacheHttpTransportClient
import com.sunnychung.application.multiplatform.hellohttp.network.util.DenyAllSslCertificateManager
import com.sunnychung.application.multiplatform.hellohttp.network.util.MultipleTrustCertificateManager
import java.io.File
import java.security.cert.CertificateException
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SslContextTest {

//    val httpClient = ApacheHttpTransportClient(NetworkClientManager())
    val httpClient = SpringWebClientTransportClient(NetworkClientManager())

    private fun verifyGoogleCertificates(trustManager: X509TrustManager) {
        trustManager.checkServerTrusted(
            arrayOf(
                parseCaCertificates(javaClass.classLoader.getResource("ssl/google-com-cert-3.pem")!!.readBytes()).single(),
                parseCaCertificates(javaClass.classLoader.getResource("ssl/google-com-cert-2.pem")!!.readBytes()).single(),
                parseCaCertificates(javaClass.classLoader.getResource("ssl/google-com-cert-1.pem")!!.readBytes()).single(),
            ),
            "RSA"
        )
    }

    private fun verifyCustomServerCertificate(trustManager: X509TrustManager) {
        trustManager.checkServerTrusted(
            arrayOf(
                parseCaCertificates(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.readBytes()).single(),
                parseCaCertificates(javaClass.classLoader.getResource("ssl/serverCert.pem")!!.readBytes()).single(),
            ),
            "RSA"
        )
    }

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

    @Test
    fun `client certificate and CA certificate should be processed if both available, and verifying server certificate against CA certificate should succeed`() {
        val sslConfig = SslConfig(
            clientCertificateKeyPairs = listOf(
                ClientCertificateKeyPair.importFrom(
                    certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file),
                    keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.der")!!.file),
                    keyPassword = ""
                )
            ),
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.keyManager != null)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size >= 2)

        // verify a server cert should pass
        verifyCustomServerCertificate(ssl.trustManager!!)
        verifyGoogleCertificates(ssl.trustManager!!)
    }

    @Test
    fun `client certificate and CA certificate should be processed if both available, and verifying server certificate against CA certificate with system CA disabled should succeed`() {
        val sslConfig = SslConfig(
            clientCertificateKeyPairs = listOf(
                ClientCertificateKeyPair.importFrom(
                    certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file),
                    keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.der")!!.file),
                    keyPassword = ""
                )
            ),
            isDisableSystemCaCertificates = true,
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.keyManager != null)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size == 1)

        // verify a server cert should pass
        verifyCustomServerCertificate(ssl.trustManager!!)

        // this should fail
        assertFailsWith<CertificateException> {
            verifyGoogleCertificates(ssl.trustManager!!)
        }
    }

    @Test
    fun `verifying server certificate against CA certificate should succeed`() {
        val sslConfig = SslConfig(
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size >= 2)

        // verify a server cert should pass
        verifyCustomServerCertificate(ssl.trustManager!!)
        verifyGoogleCertificates(ssl.trustManager!!)
    }

    @Test
    fun `verifying server certificate against CA certificate with system certificates disabled should succeed`() {
        val sslConfig = SslConfig(
            isDisableSystemCaCertificates = true,
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size == 1)

        // verify a server cert should pass
        verifyCustomServerCertificate(ssl.trustManager!!)

        // this should fail
        assertFailsWith<CertificateException> {
            verifyGoogleCertificates(ssl.trustManager!!)
        }
    }

    @Test
    fun `verifying a certificate signed by another CA against a CA certificate should fail`() {
        val sslConfig = SslConfig(
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size >= 2)

        // verify a server cert signed by another CA should fail
        assertFailsWith<CertificateException> {
            ssl.trustManager!!.checkServerTrusted(
                arrayOf(
                    parseCaCertificates(
                        javaClass.classLoader.getResource("ssl/anotherServerCert.pem")!!.readBytes()
                    ).single(),
                    parseCaCertificates(
                        javaClass.classLoader.getResource("ssl/anotherServerCACert.pem")!!.readBytes()
                    ).single(),
                ),
                "RSA"
            )
        }
    }

    @Test
    fun `with custom CA certificates enabled, verifying publicly trusted server certificate should succeed`() {
        val sslConfig = SslConfig(
            trustedCaCertificates =
                importCaCertificates(File(javaClass.classLoader.getResource("ssl/serverCACert.pem")!!.file))
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager != null)

        // verify a server cert should pass
        verifyGoogleCertificates(ssl.trustManager!!)
    }

    @Test
    fun `without custom CA certificates configurations, there is no custom trust manager`() {
        val sslConfig = SslConfig()
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager == null)
    }

    @Test
    fun `with all CA disabled, verifying any server certificate should fail`() {
        val sslConfig = SslConfig(
            isDisableSystemCaCertificates = true
        )
        val ssl = httpClient.createSslContext(sslConfig)
        assert(ssl.trustManager is MultipleTrustCertificateManager)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.size == 1)
        assert((ssl.trustManager as MultipleTrustCertificateManager).trustManagers.first() is DenyAllSslCertificateManager)

        // verify any certificate should fail

        assertFailsWith<CertificateException> {
            verifyCustomServerCertificate(ssl.trustManager!!)
        }

        assertFailsWith<CertificateException> {
            ssl.trustManager!!.checkServerTrusted(
                arrayOf(
                    parseCaCertificates(
                        javaClass.classLoader.getResource("ssl/anotherServerCert.pem")!!.readBytes()
                    ).single(),
                    parseCaCertificates(
                        javaClass.classLoader.getResource("ssl/anotherServerCACert.pem")!!.readBytes()
                    ).single(),
                ),
                "RSA"
            )
        }

        assertFailsWith<CertificateException> {
            verifyGoogleCertificates(ssl.trustManager!!)
        }
    }
}

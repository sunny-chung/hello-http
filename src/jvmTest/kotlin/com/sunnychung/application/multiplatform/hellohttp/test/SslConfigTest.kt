package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.network.SpringWebClientTransportClient
//import com.sunnychung.application.multiplatform.hellohttp.network.ApacheHttpTransportClient
import com.sunnychung.application.multiplatform.hellohttp.util.importFrom
import java.io.File
import java.security.spec.InvalidKeySpecException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SslConfigTest {

    fun verifyCertAndKey(certFile: File, keyFile: File, password: String) {
        val keyPair = ClientCertificateKeyPair.importFrom(
            certFile = certFile,
            keyFile = keyFile,
            keyPassword = password
        )
        val sslConfig = SslConfig(clientCertificateKeyPairs = listOf(keyPair))
//        ApacheHttpTransportClient(NetworkClientManager()).createSslContext(sslConfig)
        SpringWebClientTransportClient(NetworkClientManager()).createSslContext(sslConfig)
    }

    @Test
    fun `read client cert and unencrypted key`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.der")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read client cert and unencrypted key with password should fail`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.der")!!.file)
        val exception = assertFailsWith<Throwable> {
            verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "asdfh")
        }
        assert(exception.cause is InvalidKeySpecException)
    }

    @Test
    fun `read client cert and encrypted key with correct password`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.encrypted.der")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "asdfg")
    }

    @Test
    fun `read client cert and encrypted key with incorrect password should fail`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.encrypted.der")!!.file)
        val exception = assertFailsWith<Throwable> {
            verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "asdfh")
        }
        assert(exception.cause is InvalidKeySpecException)
    }

    @Test
    fun `read client cert and key encrypted by empty password`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.emptyencrypted.der")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read client cert and key encrypted by empty password with other password should fail`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.emptyencrypted.der")!!.file)
        val exception = assertFailsWith<Throwable> {
            verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "asdfh")
        }
        assert(exception.cause is InvalidKeySpecException)
    }

    @Test
    fun `read client cert and PKCS1 DER unencrypted key`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs1.der")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read client cert and PKCS1 PEM unencrypted key`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs1.pem")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read PEM client cert and PEM unencrypted key`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.pem")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.pem")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read client cert and PEM key encrypted by empty password`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.emptyencrypted.pem")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "")
    }

    @Test
    fun `read client cert and PEM key encrypted by password`() {
        val certFile = File(javaClass.classLoader.getResource("ssl/clientCert.der")!!.file)
        val keyFile = File(javaClass.classLoader.getResource("ssl/clientKey.pkcs8.encrypted.pem")!!.file)
        verifyCertAndKey(certFile = certFile, keyFile = keyFile, password = "asdfg")
    }
}

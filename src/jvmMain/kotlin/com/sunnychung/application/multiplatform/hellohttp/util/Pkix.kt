package com.sunnychung.application.multiplatform.hellohttp.util

import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.security.auth.x500.X500Principal

fun ClientCertificateKeyPair.Companion.importFrom(certFile: File, keyFile: File, keyPassword: String): ClientCertificateKeyPair {
    listOf(certFile, keyFile).forEach { file ->
        if (!file.canRead()) {
            throw IllegalArgumentException("File ${file.name} cannot be read.")
        }
    }

    val cert: X509Certificate = try {
        val certBytes = certFile.readBytes()
        parseCaCertificates(certBytes).also {
            if (it.size > 1) {
                throw RuntimeException("There should be only one certificate but ${it.size} were found.")
            } else if (it.isEmpty()) {
                throw RuntimeException("No certificate was found.")
            }
        }.single()
    } catch (e: Throwable) {
        throw RuntimeException("Error while parsing the certificate file -- ${e.message}", e)
    }

    val keyBytes = keyFile.readBytes()
    val privateKey = try {
        parsePrivateKey(keyBytes = keyBytes, keyPassword = keyPassword)
    } catch (e: Throwable) {
        throw RuntimeException("Error while parsing the private key file -- ${e.message}", e)
    }

    val now = KInstant.now()
    return ClientCertificateKeyPair(
        id = uuidString(),
        certificate = ImportedFile(
            id = uuidString(),
            name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${
                        KZonedInstant(
                            cert.notAfter.time,
                            KZoneOffset.local()
                        ).format(KDateTimeFormat.ISO8601_DATETIME.pattern)
                    }",
            originalFilename = certFile.name,
            createdWhen = now,
            isEnabled = true,
            content = cert.encoded,
        ),
        privateKey = ImportedFile(
            id = uuidString(),
            name = "Private Key",
            originalFilename = keyFile.name,
            createdWhen = now,
            isEnabled = true,
            content = privateKey.encoded, // store decrypted bytes
        ),
        createdWhen = now,
        isEnabled = true,
    )
}

private fun parsePrivateKey(keyBytes: ByteArray, keyPassword: String): PrivateKey {
    val securityProvider = BouncyCastleProvider()
    val keyConverter = JcaPEMKeyConverter().setProvider(securityProvider)

    val parsePkcs8AnyUnencryptedPrivateKey = { keyBytes: ByteArray ->
        val keyInfo = PrivateKeyInfo.getInstance(keyBytes)
        keyConverter.getPrivateKey(keyInfo).also {
            // this is an unencrypted key. there should be no password given.
            if (keyPassword.isNotEmpty()) {
                throw InvalidKeySpecException("Parse fail")
            }
        }
    }

    val parsePkcs8AnyEncryptedPrivateKey = { keyBytes: ByteArray ->
        decryptAsPrivateKey(keyBytes = keyBytes, keyPassword)
    }

    val parsePkcs1RsaPrivateKey = { keyBytes: ByteArray ->
        println("parsePkcs1RsaPrivateKey")

        // this implementation does not throw exception for invalid keys
//        val spec: KeySpec = PKCS8EncodedKeySpec(keyBytes)
//        KeyFactory.getInstance("RSA", securityProvider).generatePrivate(spec)

        // this does throw
        val pkcs8 = pkcs1PrivateKeyToPkcs8Encoded(keyBytes)
        parsePkcs8AnyUnencryptedPrivateKey(pkcs8)
    }

    // convert PEM to DER if applicable
    val keyBytes = keyBytes
        .tryToConvertPemToDer("-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----")
        .tryToConvertPemToDer("-----BEGIN ENCRYPTED PRIVATE KEY-----", "-----END ENCRYPTED PRIVATE KEY-----")
        .tryToConvertPemToDer("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----")

    val tryInOrder = listOf(
        parsePkcs8AnyUnencryptedPrivateKey,
        parsePkcs8AnyEncryptedPrivateKey,
        parsePkcs1RsaPrivateKey
    )
    tryInOrder.forEach { proc ->
        try {
            return proc(keyBytes)
        } catch (_: Throwable) { }
    }

    throw InvalidKeySpecException("Cannot parse given private key")
}

fun pkcs1PrivateKeyToPkcs8Encoded(pkcs1Encoded: ByteArray): ByteArray {
    val algId: AlgorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
    val privateKeyInfo = PrivateKeyInfo(algId, ASN1Sequence.getInstance(pkcs1Encoded))

    val pkcs8Encoded = privateKeyInfo.encoded
    return pkcs8Encoded
}

fun decodeUnencryptedPrivateKey(keyBytes: ByteArray): PrivateKey {
    val keySpec = PKCS8EncodedKeySpec(keyBytes)
    return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
}

fun decryptAsPrivateKey(keyBytes: ByteArray, password: String): PrivateKey {
    return EncryptedPrivateKeyInfo(keyBytes)
        .let {
            val secretKey = SecretKeyFactory.getInstance(it.algName)
                .generateSecret(PBEKeySpec(password.toCharArray()))
            val cipher = Cipher.getInstance(it.algName)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, it.algParameters)
            val keySpec = it.getKeySpec(cipher)

            // try if all these work
            KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        }
}

private fun ByteArray.tryToConvertPemToDer(startLine: String, endLine: String): ByteArray {
    val startBytes = startLine.toByteArray()
    if (size <= startBytes.size || !copyOfRange(0, startBytes.size).contentEquals(startBytes)) {
        return this
    }
    InputStreamReader(ByteArrayInputStream(this)).buffered().use { reader ->
        val firstLine = reader.readLine()
        return if (firstLine == startLine) {
            val base64Encoded = buildString {
                while (reader.ready()) {
                    val line = reader.readLine()
                    if (line != endLine) {
                        append(line)
                    } else {
                        break
                    }
                }
            }
            Base64.getDecoder().decode(base64Encoded)
        } else { // der / pem
            this
        }
    }
}

fun ClientCertificateKeyPair.Companion.importFrom(bundleFile: File, keyStorePassword: String, keyPassword: String): ClientCertificateKeyPair {
    if (!bundleFile.canRead()) {
        throw IllegalArgumentException("File ${bundleFile.name} cannot be read.")
    }

    val store = KeyStore.getInstance("PKCS12")
    store.load(FileInputStream(bundleFile), keyStorePassword.toCharArray())

    var cert: X509Certificate? = null
    var privateKey: PrivateKey? = null

    val e = store.aliases()
    while (e.hasMoreElements() && (cert == null || privateKey == null)) {
        val alias = e.nextElement()
        if (store.isCertificateEntry(alias) && cert == null) {
            cert = store.getCertificate(alias) as? X509Certificate
        } else if (store.isKeyEntry(alias) && privateKey == null) {
            cert = store.getCertificate(alias) as? X509Certificate
            privateKey = try {
                store.getKey(alias, keyPassword.toCharArray()) as? PrivateKey
            } catch (e: Throwable) {
                log.w(e) { "The key with alias $alias cannot be retrieved." }
                null
            }
        }
    }

    if (cert == null) {
        throw RuntimeException("No certificate was found.")
    }
    if (privateKey == null) {
        throw RuntimeException("No key was retrieved.")
    }

    val now = KInstant.now()
    return ClientCertificateKeyPair(
        id = uuidString(),
        certificate = ImportedFile(
            id = uuidString(),
            name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${
                        KZonedInstant(
                            cert.notAfter.time,
                            KZoneOffset.local()
                        ).format(KDateTimeFormat.ISO8601_DATETIME.pattern)
                    }",
            originalFilename = bundleFile.name,
            createdWhen = now,
            isEnabled = true,
            content = cert.encoded,
        ),
        privateKey = ImportedFile(
            id = uuidString(),
            name = "Private Key",
            originalFilename = bundleFile.name,
            createdWhen = now,
            isEnabled = true,
            content = privateKey.encoded, // store decrypted bytes
        ),
        createdWhen = now,
        isEnabled = true,
    )
}

fun parseCaCertificates(bytes: ByteArray) : List<X509Certificate> {
    val certBytes = bytes
        .tryToConvertPemToDer(startLine = "-----BEGIN CERTIFICATE-----", endLine = "-----END CERTIFICATE-----")
        .tryToConvertPemToDer(startLine = "-----BEGIN PKCS7-----", endLine = "-----END PKCS7-----")
    return CertificateFactory.getInstance("X.509")
        .generateCertificates(ByteArrayInputStream(certBytes)).map { it as X509Certificate }
}

fun importCaCertificates(file: File): List<ImportedFile> {
    val content = file.readBytes()
    val certs = parseCaCertificates(content)

    return certs.map { cert ->
        ImportedFile(
            id = uuidString(),
            name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${
                        KZonedInstant(
                            cert.notAfter.time,
                            KZoneOffset.local()
                        ).format(KDateTimeFormat.ISO8601_DATETIME.pattern)
                    }" +
                    if (cert.keyUsage?.get(5) != true || cert.basicConstraints < 0) "\n⚠️ Not a CA certificate!" else "",
            originalFilename = file.name,
            createdWhen = KInstant.now(),
            isEnabled = true,
            content = cert.encoded,
        )
    }
}
package com.sunnychung.application.multiplatform.hellohttp.util

import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.security.KeyFactory
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

private fun parsePrivateKey(keyBytes: ByteArray, keyPassword: String): PrivateKey = if (keyPassword.isEmpty()) {
    // try without password. if it's fail, try with empty password
    try {
        decodeUnencryptedPrivateKey(keyBytes)
    } catch (e: InvalidKeySpecException) {
        decryptAsPrivateKey(keyBytes = keyBytes, keyPassword)
    }
} else {
    decryptAsPrivateKey(keyBytes = keyBytes, keyPassword)
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

fun parseCaCertificates(bytes: ByteArray) : List<X509Certificate> {
    InputStreamReader(ByteArrayInputStream(bytes)).buffered().use { reader ->
        val firstLine = reader.readLine()
        val certBytes = if (firstLine == "-----BEGIN PKCS7-----") { // p7b
            val base64Encoded = buildString {
                while (reader.ready()) {
                    val line = reader.readLine()
                    if (line != "-----END PKCS7-----") {
                        append(line)
                    } else {
                        break
                    }
                }
            }
            Base64.getDecoder().decode(base64Encoded)
        } else { // der / pem
            bytes
        }
        return CertificateFactory.getInstance("X.509")
            .generateCertificates(ByteArrayInputStream(certBytes)).map { it as X509Certificate }
    }
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
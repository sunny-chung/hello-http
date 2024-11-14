package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.security.KeyFactory
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

@Persisted
@Serializable
data class Environment(
    val id: String,
    val name: String,
    val variables: MutableList<UserKeyValuePair>,
    val httpConfig: HttpConfig = HttpConfig(),
    val sslConfig: SslConfig = SslConfig(),
    val userFiles: List<ImportedFile> = listOf(),
) : DropDownable {

    override val key: String
        get() = id

    override val displayText: String
        get() = name

    fun deepCopyWithNewId(): Environment {
        fun KInstant.deepCopy() = this + 0.seconds() // create a new copy
        fun List<UserKeyValuePair>.deepCopyWithNewId() = map {
            it.copy(id = uuidString())
        }
        fun ImportedFile.deepCopyWithNewId() = copy(
            id = uuidString(),
            content = content.copyOf(),
            createdWhen = createdWhen.deepCopy(),
        )
        fun List<ImportedFile>.deepCopyWithNewId() = map {
            it.deepCopyWithNewId()
        }

        return copy(
            id = uuidString(),
            variables = variables.deepCopyWithNewId().toMutableList(),
            httpConfig = httpConfig.copy(),
            sslConfig = sslConfig.copy(
                trustedCaCertificates = sslConfig.trustedCaCertificates.deepCopyWithNewId(),
                clientCertificateKeyPairs = sslConfig.clientCertificateKeyPairs.map {
                    it.copy(
                        id = uuidString(),
                        certificate = it.certificate.deepCopyWithNewId(),
                        privateKey = it.privateKey,
                        createdWhen = it.createdWhen.deepCopy(),
                    )
                },
            ),
            userFiles = userFiles.deepCopyWithNewId(),
        )
    }
}

@Persisted
@Serializable
data class HttpConfig(
    val protocolVersion: HttpProtocolVersion? = null
) {
    enum class HttpProtocolVersion {
        Http1Only, Http2Only, Negotiate
    }
}

@Persisted
@Serializable
data class SslConfig(
    val isInsecure: Boolean? = null,
    val trustedCaCertificates: List<ImportedFile> = emptyList(),
    val isDisableSystemCaCertificates: Boolean? = null,
    val clientCertificateKeyPairs: List<ClientCertificateKeyPair> = emptyList(),
) {
    fun hasCustomConfig() = isInsecure == true ||
            trustedCaCertificates.any { it.isEnabled } ||
            isDisableSystemCaCertificates == true ||
            clientCertificateKeyPairs.any { it.isEnabled }
}

@Persisted
@Serializable
data class ClientCertificateKeyPair(
    override val id: String,

    /**
     * In `certificate`, `isEnabled` is a delegate of the outer one
     */
    val certificate: ImportedFile,

    /**
     * In `privateKey`:
     * 1. `isEnabled` is a delegate of the outer one.
     * 2. `content` may not be the original content. It is decrypted PKCS8 key spec bytes.
     */
    val privateKey: ImportedFile,

    val createdWhen: KInstant,

    val isEnabled: Boolean
) : Identifiable {
    val name
        get() = certificate.name

    companion object
}

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

    fun decryptAsRsaKeySpec(keyBytes: ByteArray, password: String): PKCS8EncodedKeySpec {
        return EncryptedPrivateKeyInfo(keyBytes)
            .let {
                val secretKey = SecretKeyFactory.getInstance(it.algName).generateSecret(PBEKeySpec(password.toCharArray()))
                val cipher = Cipher.getInstance(it.algName)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, it.algParameters)
                val keySpec = it.getKeySpec(cipher)

                // try if all these work
                KeyFactory.getInstance("RSA").generatePrivate(keySpec)

                keySpec
            }
    }

    val keyBytes = keyFile.readBytes()
    val keySpec = try {
        if (keyPassword.isEmpty()) {
            // try without password. if it's fail, try with empty password
            try {
                val keySpec = PKCS8EncodedKeySpec(keyBytes)
                KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                keySpec
            } catch (e: InvalidKeySpecException) {
                decryptAsRsaKeySpec(keyBytes = keyBytes, keyPassword)
            }
        } else {
            decryptAsRsaKeySpec(keyBytes = keyBytes, keyPassword)
        }
    } catch (e: Throwable) {
        throw RuntimeException("Error while parsing the private key file -- ${e.message}", e)
    }

    val now = KInstant.now()
    return ClientCertificateKeyPair(
        id = uuidString(),
        certificate = ImportedFile(
            id = uuidString(),
            name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${KZonedInstant(cert.notAfter.time, KZoneOffset.local()).format(KDateTimeFormat.ISO8601_DATETIME.pattern)}",
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
            content = keySpec.encoded, // store decrypted bytes
        ),
        createdWhen = now,
        isEnabled = true,
    )
}

// ----------- TODO refactor to a separate file -----------

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
        return CertificateFactory.getInstance("X.509").generateCertificates(ByteArrayInputStream(certBytes)).map { it as X509Certificate }
    }
}

fun importCaCertificates(file: File): List<ImportedFile> {
    val content = file.readBytes()
    val certs = parseCaCertificates(content)

    return certs.map { cert ->
        ImportedFile(
            id = uuidString(),
            name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${KZonedInstant(cert.notAfter.time, KZoneOffset.local()).format(KDateTimeFormat.ISO8601_DATETIME.pattern)}" +
                    if (cert.keyUsage?.get(5) != true || cert.basicConstraints < 0) "\n⚠️ Not a CA certificate!" else ""
            ,
            originalFilename = file.name,
            createdWhen = KInstant.now(),
            isEnabled = true,
            content = cert.encoded,
        )
    }
}

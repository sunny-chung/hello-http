package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class Environment(
    val id: String,
    val name: String,
    val variables: MutableList<UserKeyValuePair>,
    val httpConfig: HttpConfig = HttpConfig(),
    val sslConfig: SslConfig = SslConfig(),
    val userFiles: List<ImportedFile> = listOf(),
    var cookieJar: CookieJar = CookieJar(),
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

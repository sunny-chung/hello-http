package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class ConnectionSecurity(
    val security: ConnectionSecurityType,
    val clientCertificatePrincipal: Certificate?,
    val peerCertificatePrincipal: Certificate?,
)

@Persisted
@Serializable
data class Certificate(
    val principal: String,
    val issuerPrincipal: String,
    val subjectAlternativeNames: List<Pair<Int, String>>? = null,
    val notAfter: KInstant,
    val notBefore: KInstant,
)

enum class ConnectionSecurityType {
    /**
     * Cleartext HTTP
     */
    Unencrypted,

    /**
     * TLS without verification
     */
    InsecureEncrypted,

    /**
     * TLS with verification. It could be verified with custom trusted certificates.
     */
    VerifiedEncrypted,

    /**
     * mTLS with verification
     */
    MutuallyVerifiedEncrypted
}

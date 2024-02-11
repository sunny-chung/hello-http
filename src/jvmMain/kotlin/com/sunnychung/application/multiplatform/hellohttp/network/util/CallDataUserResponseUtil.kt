package com.sunnychung.application.multiplatform.hellohttp.network.util

import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurity
import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurityType
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.CallData
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

internal object CallDataUserResponseUtil {
    internal fun onConnected(out: UserResponse) {
        synchronized(out) {
            if (out.connectionSecurity == null) {
                out.connectionSecurity = ConnectionSecurity(
                    security = ConnectionSecurityType.Unencrypted,
                    clientCertificatePrincipal = null,
                    peerCertificatePrincipal = null
                )
            }
        }
    }

    internal fun onTlsUpgraded(
        callData: CallData,
        localCertificates: Array<Certificate>?,
        peerCertificates: Array<Certificate>?,
    ) {
        synchronized(callData.response) {
            callData.response.connectionSecurity = ConnectionSecurity(
                security = when {
                    callData.sslConfig.isInsecure == true -> ConnectionSecurityType.InsecureEncrypted
                    !localCertificates.isNullOrEmpty() && !peerCertificates.isNullOrEmpty() -> ConnectionSecurityType.MutuallyVerifiedEncrypted
                    !peerCertificates.isNullOrEmpty() -> ConnectionSecurityType.VerifiedEncrypted
                    else -> ConnectionSecurityType.Unencrypted
                },
                clientCertificatePrincipal = (localCertificates?.firstOrNull() as? X509Certificate)?.toPersistableCertificate(),
                peerCertificatePrincipal = (peerCertificates?.firstOrNull() as? X509Certificate)?.toPersistableCertificate(),
            )
        }
    }

    private fun X509Certificate.toPersistableCertificate() = com.sunnychung.application.multiplatform.hellohttp.model.Certificate(
        principal = subjectX500Principal.getName(X500Principal.RFC1779),
        issuerPrincipal = issuerX500Principal.getName(X500Principal.RFC1779),
        subjectAlternativeNames = subjectAlternativeNames?.filter { it.size >= 2 }
            ?.map {
                Pair(
                    it[0] as? Int,
                    when (it[1]) {
                        is String -> it[1]
                        is ByteArray -> (it[1] as ByteArray).decodeToString()
                        else -> null
                    }
                )
            }
            ?.filter { it.first != null && it.second != null } as List<Pair<Int, String>>?,
        notAfter = KInstant(notAfter.time),
        notBefore = KInstant(notBefore.time),
    )
}

package com.sunnychung.application.multiplatform.hellohttp.network.util

import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

class MultipleTrustCertificateManager(private val trustManagers: List<X509TrustManager>) : X509ExtendedTrustManager() {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
        checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
        checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        trustManagers.forEachIndexed { index, it ->
            try {
                it.checkClientTrusted(chain, authType)
                return // if any manager trusts the cert, accept it
            } catch (e: CertificateException) {
                if (index >= trustManagers.lastIndex) {
                    throw e
                }
            }
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
        checkServerTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
        checkServerTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        trustManagers.forEachIndexed { index, it ->
            try {
                it.checkServerTrusted(chain, authType)
                return // if any manager trusts the cert, accept it
            } catch (e: CertificateException) {
                if (index >= trustManagers.lastIndex) {
                    throw e
                }
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return trustManagers.map { it.acceptedIssuers }
            .fold(mutableListOf<X509Certificate>()) { acc, it -> acc += it; acc }
            .toTypedArray()
    }

}
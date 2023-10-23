package com.sunnychung.application.multiplatform.hellohttp.network

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TrustAllSslCertificateManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}
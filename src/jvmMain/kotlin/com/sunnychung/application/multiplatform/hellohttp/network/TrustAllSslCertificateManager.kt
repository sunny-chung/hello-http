package com.sunnychung.application.multiplatform.hellohttp.network

import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

class TrustAllSslCertificateManager : X509ExtendedTrustManager() {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {

    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {

    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}
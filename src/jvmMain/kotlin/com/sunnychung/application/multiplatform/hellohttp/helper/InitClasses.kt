package com.sunnychung.application.multiplatform.hellohttp.helper

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.SubprojectConfiguration
import com.sunnychung.application.multiplatform.hellohttp.network.RawPayload
import com.sunnychung.application.multiplatform.hellohttp.network.SpringWebClientTransportClient
import kotlinx.coroutines.flow.MutableSharedFlow

class InitClasses {

    init {
        (AppContext.instance.HttpTransportClient as SpringWebClientTransportClient)
            .also { transport ->
                val callData = transport.createCallData(null, "", "", "", SslConfig(), SubprojectConfiguration(subprojectId = ""))
                transport.buildWebClient(
                    callId = "",
                    callData = callData,
                    isSsl = true,
                    httpConfig = HttpConfig(HttpConfig.HttpProtocolVersion.Http2Only),
                    sslConfig = SslConfig(),
                    outgoingBytesFlow = callData.outgoingBytes as MutableSharedFlow<RawPayload>,
                    incomingBytesFlow = callData.incomingBytes as MutableSharedFlow<RawPayload>,
                    http2AccumulatedOutboundDataSerializeLimit = DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT.toInt(),
                    http2AccumulatedInboundDataSerializeLimit = DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT.toInt(),
                    doOnRequestSent = {},
                )
            }
    }
}

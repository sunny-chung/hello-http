package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.ByteArrayOutputStream

const val DEFAULT_PAYLOAD_STORAGE_SIZE_LIMIT: Long = 2 * 1024 * 1024 // 2 MB

@Persisted
@Serializable
data class RawExchange(
    val exchanges: MutableList<Exchange>,
    @Transient var uiVersion: String = uuidString(),
) {
    @Persisted
    @Serializable
    class Exchange(
        val instant: KInstantAsLong,
        var lastUpdateInstant: KInstantAsLong? = null,
        val direction: Direction,
        val detail: String?,
        @Transient var payloadBuilder: ByteArrayOutputStream? = null,
        val streamId: Int? = null,
        var payload: ByteArray? = null,
        var payloadSize: Long? = null,
    ) {
        fun consumePayloadBuilder(isComplete: Boolean) {
            payloadBuilder?.let { payloadBuilder ->
                synchronized(payloadBuilder) {
                    if ((payload?.size ?: 0) < payloadBuilder.size()) {
                        payload = payloadBuilder.toByteArray()
                        if (isComplete) {
                            this.payloadBuilder = null
                        }
                    }
                }
            }
        }

        /**
         * Caller must wrap calls to this function with a lock, e.g.
         * `synchronized(rawExchange.exchanges)`
         */
        fun unsafeWritePayloadBytes(bytes: ByteArray, limit: Long) {
            payloadSize = (payloadSize ?: 0) + bytes.size
            payloadBuilder?.let { payloadBuilder ->
                if (payloadBuilder.size() + bytes.size <= limit) {
                    payloadBuilder.write(bytes)
                } else if (payloadBuilder.size() <= limit && bytes.isNotEmpty()) {
                    payloadBuilder.write(bytes, 0, (limit - payloadBuilder.size()).toInt())
                } else {
                    consumePayloadBuilder()
                    this.payloadBuilder = null
                }
            }
            log.d { "unsafe write ${bytes.size} => $payloadSize" }
        }
    }

    enum class Direction {
        Outgoing, Incoming, Unspecified
    }
}

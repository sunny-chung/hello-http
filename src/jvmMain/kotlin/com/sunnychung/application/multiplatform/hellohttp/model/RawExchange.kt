package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.serializer.SynchronizedListSerializer
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.ByteArrayOutputStream

const val DEFAULT_PAYLOAD_STORAGE_SIZE_LIMIT: Long = 512 * 1024 // 512 KB
const val DEFAULT_ACCUMULATED_DATA_STORAGE_SIZE_LIMIT: Long = 2 * 1024 * 1024 // 2 MB

@Persisted
@Serializable
data class RawExchange(
    @Serializable(with = SynchronizedListSerializer::class) val exchanges: MutableList<Exchange>,
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
                    }
                    if (isComplete) {
                        this.payloadBuilder = null
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
                    consumePayloadBuilder(isComplete = true)
                }
            }
            log.d { "unsafe write ${bytes.size} => $payloadSize" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Exchange) return false

            if (instant != other.instant) return false
            if (lastUpdateInstant != other.lastUpdateInstant) return false
            if (direction != other.direction) return false
            if (detail != other.detail) return false
            if (payloadBuilder != other.payloadBuilder) return false
            if (streamId != other.streamId) return false
            if (payload != null) {
                if (other.payload == null) return false
                if (payload !== other.payload) return false // modified
            } else if (other.payload != null) return false
            if (payloadSize != other.payloadSize) return false

            return true
        }

        override fun hashCode(): Int {
            var result = instant.hashCode()
            result = 31 * result + (lastUpdateInstant?.hashCode() ?: 0)
            result = 31 * result + direction.hashCode()
            result = 31 * result + (detail?.hashCode() ?: 0)
            result = 31 * result + (payloadBuilder?.hashCode() ?: 0)
            result = 31 * result + (streamId ?: 0)
            result = 31 * result + (payload?.contentHashCode() ?: 0)
            result = 31 * result + (payloadSize?.hashCode() ?: 0)
            return result
        }
    }

    enum class Direction {
        Outgoing, Incoming, Unspecified
    }
}

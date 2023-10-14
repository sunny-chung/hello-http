package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.ByteArrayOutputStream

@Persisted
@Serializable
data class RawExchange(
    val exchanges: MutableList<Exchange>,
) {
    @Persisted
    @Serializable
    class Exchange(
        val instant: KInstantAsLong,
        var lastUpdateInstant: KInstantAsLong? = null,
        val direction: Direction,
        val detail: String?,
        @Transient var payloadBuilder: ByteArrayOutputStream? = null,
        var payload: ByteArray? = null
    )

    enum class Direction {
        Outgoing, Incoming, Unspecified
    }
}

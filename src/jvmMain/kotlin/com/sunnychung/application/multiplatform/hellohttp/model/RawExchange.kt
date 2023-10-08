package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.io.ByteArrayOutputStream

data class RawExchange(
    val exchanges: MutableList<Exchange>,
) {
    class Exchange(
        val instant: KInstant,
        var lastUpdateInstant: KInstant? = null,
        val direction: Direction,
        val detail: String?,
        var payloadBuilder: ByteArrayOutputStream? = null,
        var payload: ByteArray? = null
    )

    enum class Direction {
        Outgoing, Incoming, Unspecified
    }
}

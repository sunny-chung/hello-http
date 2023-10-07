package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.lib.multiplatform.kdatetime.KInstant

data class RawExchange(
    val exchanges: List<Exchange>,
) {
    data class Exchange(
        val instant: KInstant,
        val direction: Direction,
        val detail: String,
    )

    enum class Direction {
        Outgoing, Incoming
    }
}

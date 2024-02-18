package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class ProtocolVersion(val protocol: Protocol, val versionName: String?, val major: Int?, val minor: Int?) {
    constructor(protocol: Protocol, major: Int, minor: Int?) : this(
        protocol = protocol,
        versionName = major.toString() + if (minor != null) ".$minor" else "",
        major = major,
        minor = minor
    )

    constructor(protocol: Protocol, versionName: String) : this(
        protocol = protocol,
        versionName = versionName,
        major = versionName.split(".").getOrNull(0)?.toInt(),
        minor = versionName.split(".").getOrNull(1)?.toInt(),
    )

    override fun toString(): String {
        return "${protocol.displayName}/$versionName"
    }

    fun isHttp2() = protocol == Protocol.Http && major == 2
}

enum class Protocol(val displayName: String) {
    Http("HTTP")
}

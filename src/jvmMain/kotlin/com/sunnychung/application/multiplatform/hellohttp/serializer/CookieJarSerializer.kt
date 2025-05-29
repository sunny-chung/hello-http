package com.sunnychung.application.multiplatform.hellohttp.serializer

import com.sunnychung.application.multiplatform.hellohttp.network.util.Cookie
import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CookieJarSerializer : KSerializer<CookieJar> {
    private val listSerializer = ListSerializer(Cookie.serializer())
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: CookieJar
    ) {
        synchronized(value) {
            val persistentCookies = value.getPersistentCookies()
            listSerializer.serialize(encoder, persistentCookies)
        }
    }

    override fun deserialize(decoder: Decoder): CookieJar {
        val cookies: List<Cookie> = listSerializer.deserialize(decoder)
        return CookieJar(cookies)
    }
}

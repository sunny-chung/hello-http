package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.extension.encodeToStream
import com.sunnychung.application.multiplatform.hellohttp.network.util.Cookie
import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.toKZonedDateTime
import kotlinx.serialization.builtins.ListSerializer
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CookieJarSerializerTest {
    @Test
    fun readWritePersistentCookies() {
        val jar = CookieJar()
        jar.store(URI("https://www.example.com"), listOf("bb=101; Max-Age=1000", "aa=102; Expires=01 Jan ${KZonedInstant.nowAtLocalZoneOffset().toKZonedDateTime().year + 1} 00:00:00 GMT"))
        jar.store(URI("https://www.example.com"), listOf("cc=A; Max-Age=2000"))
        val newJar = assertReadWriteCookieJar(jar)
//        println(newJar)
        assertEquals(3, newJar.size)
    }

    @Test
    fun readWriteEmpty() {
        val jar = CookieJar()
        val newJar = assertReadWriteCookieJar(jar)
        assertEquals(0, newJar.size)
    }

    @Test
    fun ignoreSessionCookies() {
        val jar = CookieJar()
        jar.store(URI("https://www.example.com"), listOf("bb=101; Max-Age=1000", "dd=103", "aa=102; Expires=01 Jan ${KZonedInstant.nowAtLocalZoneOffset().toKZonedDateTime().year + 1} 00:00:00 GMT"))
        jar.store(URI("https://www.example.com"), listOf("cc=A; Max-Age=2000", "ee=104"))
        val newJar = assertReadWriteCookieJar(jar)
//        println(newJar)
        assertEquals(3, newJar.size)
    }
}

private fun assertReadWriteCookieJar(value: CookieJar): CookieJar {
    // write via custom CborStream
    val baos = ByteArrayOutputStream()
    com.sunnychung.application.multiplatform.hellohttp.extension.CborStream {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }.encodeToStream(serializer = CookieJar.serializer(), value = value, out = baos)

    // assert
    val baos2 = ByteArrayOutputStream()
    com.sunnychung.application.multiplatform.hellohttp.extension.CborStream {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }.encodeToStream(serializer = ListSerializer(Cookie.serializer()), value = value.getPersistentCookies(), out = baos2)

    assertContentEquals(baos2.toByteArray(), baos.toByteArray())

    // read back via official Cbor
    val dataRead = kotlinx.serialization.cbor.Cbor {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }.decodeFromByteArray(deserializer = CookieJar.serializer(), bytes = baos.toByteArray())

    assertEquals(value.getPersistentCookies().sortedBy { it.name }.toList(), dataRead.getPersistentCookies().sortedBy { it.name }.toList())

    return dataRead
}

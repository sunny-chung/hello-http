package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import com.sunnychung.lib.multiplatform.kdatetime.toKZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookieJarTest {

    private lateinit var jar: CookieJar
    private val baseUrl = URI("https://example.com/index.html")

    @BeforeTest // before each @Test
    fun setup() {
        jar = CookieJar()
    }

    @Test
    fun `store and retrieve a single cookie`() {
        jar.store(baseUrl, listOf("sessionId=abc123; Path=/; HttpOnly"))

        val cookies = jar.getCookiesFor(URI("https://example.com/"))
        assertEquals(1, cookies.size)
        assertEquals("sessionId", cookies[0].name)
        assertEquals("abc123", cookies[0].value)

        assertTrue(jar.getPersistentCookies().isEmpty())
    }

    @Test
    fun `store and retrieve multiple cookies`() {
        jar.store(baseUrl, listOf("sessionId=abc123; Path=/; HttpOnly"))
        jar.store(baseUrl, listOf("name=sunny; HttpOnly", "theme=dark"))

        val cookies = jar.getCookiesFor(URI("https://example.com/"))
        assertEquals(3, cookies.size)
        for (cookie in cookies) {
            when (cookie.name) {
                "sessionId" -> assertEquals("abc123", cookie.value)
                "name" -> assertEquals("sunny", cookie.value)
                "theme" -> assertEquals("dark", cookie.value)
                else -> throw AssertionError("Unexpected cookie: ${cookie.name}")
            }
        }

        assertTrue(jar.getPersistentCookies().isEmpty())
    }

    @Test
    fun `cookie with domain should match subdomain`() {
        jar.store(baseUrl, listOf("theme=dark; Domain=example.com; Path=/"))

        val cookies = jar.getCookiesFor(URI("https://sub.example.com/dashboard"))
        assertEquals(1, cookies.size)
        assertEquals("theme", cookies[0].name)
    }

    @Test
    fun `cookie with domain should not match another domain`() {
        jar.store(baseUrl, listOf("theme=dark; Domain=example.com; Path=/"))

        val cookies = jar.getCookiesFor(URI("https://otherexample.com/dashboard"))
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `cookie with subdomain should not match parent domain`() {
        val subdomainUrl = URI("https://api.example.com")
        jar.store(subdomainUrl, listOf("apiKey=123; Domain=api.example.com"))

        val cookies = jar.getCookiesFor(URI("https://example.com"))
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `cookie with secure flag should not match http`() {
        jar.store(baseUrl, listOf("auth=token; Secure; Path=/"))

        val cookies = jar.getCookiesFor(URI("http://example.com/"))
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `cookie with secure flag should match https`() {
        jar.store(baseUrl, listOf("auth=token; Secure; Path=/"))

        val cookies = jar.getCookiesFor(URI("https://example.com/"))
        assertEquals(1, cookies.size)
        assertEquals("auth", cookies[0].name)
        assertEquals("token", cookies[0].value)
    }

    @Test
    fun `cookie with secure and HttpOnly flag should match https`() {
        jar.store(baseUrl, listOf("auth=token; Secure; HttpOnly; Path=/"))

        val cookies = jar.getCookiesFor(URI("https://example.com/"))
        assertEquals(1, cookies.size)
        assertEquals("auth", cookies[0].name)
        assertEquals("token", cookies[0].value)
    }

    @Test
    fun `cookie with path should only match that path prefix`() {
        jar.store(baseUrl, listOf("lang=en; Path=/account"))

        val match = jar.getCookiesFor(URI("https://example.com/account/settings"))
        val noMatch1 = jar.getCookiesFor(URI("https://example.com/profile"))
        val noMatch2 = jar.getCookiesFor(URI("https://example.com/"))
        val noMatch3 = jar.getCookiesFor(URI("https://example.com"))
        val noMatch4 = jar.getCookiesFor(URI("https://example.com/account2")) // See RFC 6265 Section 5.1.4 the last paragraph
        assertEquals(1, match.size)
        assertTrue(noMatch1.isEmpty())
        assertTrue(noMatch2.isEmpty())
        assertTrue(noMatch3.isEmpty())
        assertTrue(noMatch4.isEmpty())
    }

    @Test
    fun `cookie with Max-Age should expire after time`() {
        jar.store(baseUrl, listOf("temp=123; Max-Age=2"))

        val cookies = jar.getCookiesFor(baseUrl)
        assertEquals(1, cookies.size)

        assertEquals(1, jar.getPersistentCookies().size)

        runBlocking {
            delay(3.seconds().millis)
            val cookies = jar.getCookiesFor(baseUrl)
            assertTrue(cookies.isEmpty())
            assertTrue(jar.getPersistentCookies().isEmpty())
        }
    }

    @Test
    fun `cookie with Max-Age=0 should expire immediately`() {
        jar.store(baseUrl, listOf("temp=123; Max-Age=0"))

        val cookies = jar.getCookiesFor(baseUrl)
        assertTrue(cookies.isEmpty(), "Cookie should be expired immediately")
    }

    @Test
    fun `cookie with invalid Max-Age is treated as session cookie`() {
        jar.store(baseUrl, listOf("temp=123; Max-Age=notanumber"))

        val cookies = jar.getCookiesFor(baseUrl)
        assertEquals(1, cookies.size)

        val persistentCookies = jar.getPersistentCookies()
        assertTrue(persistentCookies.isEmpty())
    }

    @Test
    fun `cookie with a future Expires should be persistable`() {
        jar.store(baseUrl, listOf("future=test; Expires=01 Jan ${KZonedInstant.nowAtLocalZoneOffset().toKZonedDateTime().year + 1} 00:00:00 GMT"))

        val cookies = jar.getCookiesFor(baseUrl)
        println(cookies)
        assertEquals(1, cookies.size)
        assertEquals(1, jar.getPersistentCookies().size)
    }

    @Test
    fun `cookie with Expires in the past should be ignored`() {
        jar.store(baseUrl, listOf("expired=test; Expires=Sat, 01 Jan 2000 00:00:00 GMT"))

        val cookies = jar.getCookiesFor(baseUrl)
        println(cookies)
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `cookie with invalid Expires should be treated as session cookie`() {
        jar.store(baseUrl, listOf("session1=test; Expires=Wed, 01 Jan 2000 00:00:00 GMT", "session2=test; Expires=InvalidDate")) // 01 Jan 2000 is Sat

        val cookies = jar.getCookiesFor(baseUrl)
        assertEquals(2, cookies.size)

        val persistentCookies = jar.getPersistentCookies()
        assertTrue(persistentCookies.isEmpty())
    }

    @Test
    fun `cookie replacements should update existing cookies`() {
        jar.store(baseUrl, listOf("user=alice; Path=/"))
        jar.store(baseUrl, listOf("user=bob; Path=/"))

        val cookies = jar.getCookiesFor(baseUrl)
        assertEquals(1, cookies.size)
        assertEquals("bob", cookies[0].value)

        assertTrue(jar.getPersistentCookies().isEmpty())
    }

    @Test
    fun `invalid Set-Cookie headers should be ignored`() {
        val invalidHeaders = listOf(
            "", // empty string
            "justkey", // no equals sign
            "=", // missing name
//            "a1=b; Expires=InvalidDate", // invalid date
//            "a2=b; Max-Age=notanumber", // invalid max-age
//            "a3=b; Expires=Feb 2025 00:00:00 GMT", // invalid date
//            "a4=b; Expires=01 2025 00:00:00 GMT", // invalid date
//            "a5=b; Expires=01 Feb 00:00:00 GMT", // invalid date
//            "a6=b; Expires=01 Feb 2025", // missing time
//            "a7=b; Expires=00 Feb 2025 00:00:00 GMT", // invalid date
//            "a8=b; Expires=32 Mar 2025 00:00:00 GMT", // invalid date
//            "a9=b; Expires=1 Jul 1600 00:00:00 GMT", // invalid date
//            "a10=b; Expires=1 Jul 2025 24:00:00 GMT", // invalid time
//            "a11=b; Expires=1 Jul 2025 20:60:00 GMT", // invalid time
//            "a12=b; Expires=1 Jul 2025 20:00:60 GMT", // invalid time
        )
        jar.store(baseUrl, invalidHeaders)

        val cookies = jar.getCookiesFor(baseUrl)
        println(cookies)
        assertTrue(cookies.isEmpty())
        assertTrue(jar.getPersistentCookies().isEmpty())
    }

    @Test
    fun `unknown cookie attributes should be ignored but keep the cookie`() {
        jar.store(baseUrl, listOf("user=alice; unknown=attr; unknown2; Path=/; Expires=01 Jan ${KZonedInstant.nowAtLocalZoneOffset().toKZonedDateTime().year + 1} 00:00:00 GMT"))

        val cookies = jar.getCookiesFor(baseUrl)
        println(cookies)
        assertEquals(1, cookies.size)
        assertEquals(1, jar.getPersistentCookies().size)
    }

    @Test
    fun `cookie can be set to empty`() {
        jar.store(baseUrl, listOf("user=alice; Path=/"))
        jar.store(baseUrl, listOf("user=; Path=/"))

        val cookies = jar.getCookiesFor(baseUrl)
        assertEquals(1, cookies.size)
        assertEquals("", cookies[0].value)

        assertTrue(jar.getPersistentCookies().isEmpty())
    }

    @Test
    fun `getCookieHeader should return correct header string`() {
        jar.store(baseUrl, listOf(
            "user=alice; Path=/",
            "session=xyz; Path=/; Secure"
        ))

        val header = jar.getCookieHeader(URI("https://example.com/"))
        assertTrue(header.contains("user=alice"))
        assertTrue(header.contains("session=xyz"))
    }

    @Test
    fun `getCookieHeader should exclude non-matching cookies`() {
        jar.store(baseUrl, listOf(
            "secureOnly=1; Secure; Path=/"
        ))

        val header = jar.getCookieHeader(URI("http://example.com"))
        assertEquals("", header)
    }

    @Test
    fun `cookie without path should default to slash`() {
        jar.store(baseUrl, listOf("token=abc123"))

        val cookies = jar.getCookiesFor(URI("https://example.com/home"))
        assertEquals(1, cookies.size)
    }
}

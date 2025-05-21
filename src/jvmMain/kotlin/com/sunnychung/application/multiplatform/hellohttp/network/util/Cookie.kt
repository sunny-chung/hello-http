package com.sunnychung.application.multiplatform.hellohttp.network.util

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import com.sunnychung.lib.multiplatform.kdatetime.toKInstant
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

/**
 * Reference implementation: RFC 6265
 */
data class Cookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val expires: KInstant? = null
) {
    fun isExpired(): Boolean {
        return expires?.let { it <= KInstant.now() } ?: false
    }

    fun isSessionCookie(): Boolean = expires == null

    fun isPersistable(): Boolean {
        return expires?.let { it > KInstant.now() } ?: false
    }

    fun matches(url: URI): Boolean {
        val host = url.host.lowercase()
        val requestPath = url.path.ifEmpty { "/" }

        val domainMatch = host == domain.lowercase() || host.endsWith(".$domain")
        val pathMatch = requestPath == path || ( // see RFC 6265 section 5.1.4
            requestPath.startsWith(path) &&
                (path.endsWith("/") || requestPath[path.lastIndex + 1] == '/')
        )
        val secureMatch = !secure || url.scheme == "https"

        return domainMatch && pathMatch && secureMatch && !isExpired()
    }

    fun toHeaderString(): String = "$name=$value"
}

/**
 * Reference implementation: RFC 6265
 */
class CookieJar {
    private val cookies = mutableListOf<Cookie>()

    fun store(url: URI, setCookieHeaders: List<String>) {
        for (header in setCookieHeaders) {
            val parts = header.split(";").map { it.trim() }
            if (parts.isEmpty()) continue

            val nameValue = parts[0].split("=", limit = 2)
            if (nameValue.size != 2) continue

            val name = nameValue[0]
            if (name.isEmpty()) continue
            val value = nameValue[1]
            var domain = url.host
            var path = "/"
            var secure = false
            var httpOnly = false
            var expires: KInstant? = null

            for (i in 1 until parts.size) {
                val attr = parts[i]
                val attrParts = attr.split("=", limit = 2)
                val attrName = attrParts[0].trim().lowercase()
                val attrValue = attrParts.getOrNull(1)?.trim()

                when (attrName) {
                    "domain" -> if (attrValue != null) domain = attrValue.trimStart('.')
                    "path" -> if (attrValue != null) path = attrValue
                    "secure" -> secure = true
                    "httponly" -> httpOnly = true
                    "expires" -> {
                        if (attrValue != null) {
                            try {
                                expires = Instant.from(
                                    DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).parse(attrValue)
                                ).toKInstant() // borrow from the Java time library as KDateTime doesn't have strict RFC 1123 parser yet
                            } catch (_: Exception) { }
                        }
                    }
                    "max-age" -> {
                        if (attrValue != null) {
                            val seconds = attrValue.toLongOrNull()
                            if (seconds != null && seconds >= 0) {
                                expires = KInstant.now() + seconds.seconds()
                            }
                        }
                    }
                }
            }

            val cookie = Cookie(name, value, domain, path, secure, httpOnly, expires)

            // Replace existing cookie with same name + domain + path
            cookies.removeIf {
                it.name == cookie.name &&
                    it.domain == cookie.domain &&
                    it.path == cookie.path
            }

            cookies.add(cookie)
        }
    }

    fun getCookiesFor(url: URI): List<Cookie> {
        return cookies.filter { it.matches(url) }
    }

    fun getCookieHeader(url: URI): String {
        return getCookiesFor(url).joinToString("; ") { it.toHeaderString() }
    }

    fun getPersistentCookies(): List<Cookie> {
        return cookies.filter { it.isPersistable() }
    }
}

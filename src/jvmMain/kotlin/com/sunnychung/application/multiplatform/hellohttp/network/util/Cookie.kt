package com.sunnychung.application.multiplatform.hellohttp.network.util

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.extension.distinctMergeBy
import com.sunnychung.application.multiplatform.hellohttp.serializer.CookieJarSerializer
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import com.sunnychung.lib.multiplatform.kdatetime.toKInstant
import kotlinx.serialization.Serializable
import java.net.IDN
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

const val COOKIE_NAME_MAX_LENGTH = 255
const val COOKIE_VALUE_MAX_LENGTH = 4096

// TODO include a longer list
private val publicSuffixes = setOf(
    "com", "org", "net", "edu", "gov", "co.uk", "io", "dev"
)

/**
 * Reference implementation: RFC 6265
 */
@Persisted
@Serializable
data class Cookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val expires: KInstant? = null,

    // non HTTP Cookie fields
    val isEnabled: Boolean = true,
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

    fun toAttributeString(): String = listOfNotNull("Secure".takeIf { secure }, "HttpOnly".takeIf { httpOnly }).joinToString("; ")
}

/**
 * Reference implementation: RFC 6265
 */
@Persisted
@Serializable(with = CookieJarSerializer::class)
class CookieJar(initialCookies: List<Cookie>? = null) {
    private val cookies = initialCookies?.toMutableList() ?: mutableListOf<Cookie>()
    var versionKey: String = uuidString()
        private set

    val size: Int get() = cookies.size

    fun store(url: URI, setCookieHeaders: List<String>, cookieValidator: (Cookie) -> Boolean = { true }) {
        for (header in setCookieHeaders) {
            val parts = header.split(";").map { it.trim() }
            if (parts.isEmpty()) continue

            val nameValue = parts[0].split("=", limit = 2)
            if (nameValue.size != 2) continue

            val name = nameValue[0]
            if (name.isEmpty()) continue
            val value = nameValue[1]
            var domain = url.host
            var path = ""
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

            if (path.isEmpty()) { // RFC 6265 Section 5.1.4
                val lastSlashIndex = url.path.lastIndexOf('/')
                path = if (lastSlashIndex > 0) {
                    url.path.substring(0 ..< lastSlashIndex)
                } else {
                    "/"
                }
            }

            if (!isValidCookieDomain(domain, url)) {
                continue
            }

            val cookie = Cookie(name, value, domain, path, secure, httpOnly, expires)
            if (cookie.name.length > COOKIE_NAME_MAX_LENGTH) continue
            if (cookie.value.length > COOKIE_VALUE_MAX_LENGTH) continue
            if (!cookieValidator(cookie)) {
                continue
            }

            // Replace existing cookie with same name + domain + path
            cookies.removeIf {
                it.name == cookie.name &&
                    it.domain == cookie.domain &&
                    it.path == cookie.path
            }

            cookies.add(cookie)

            updateVersion()
        }
    }

    protected fun updateVersion() {
        versionKey = uuidString()
    }

    fun getCookiesFor(url: URI): List<Cookie> {
        return cookies.filter { it.isEnabled && it.matches(url) }
            .distinctMergeBy(
                key = Cookie::name,
                mergeFunction = { a, b ->
                    if (compareValuesBy(a, b, { it.path.length }, { it.domain.length }) >= 0) {
                        a
                    } else {
                        b
                    }
                },
            )
    }

    fun getCookieHeader(url: URI): String {
        return getCookiesFor(url).toCookieHeader()
    }

    fun getPersistentCookies(): List<Cookie> {
        return cookies.filter { it.isPersistable() }
    }

    fun getAllCookies(): List<Cookie> = cookies

    fun getAllNonExpiredCookies(): List<Cookie> = cookies.filter {
        !it.isExpired()
    }

    override fun toString(): String {
        return cookies.toString()
    }

    fun copy(): CookieJar {
        val cookiesCopy = cookies.map {
            it.copy()
        }
        return CookieJar(cookiesCopy)
    }
}

// Utility to check if a domain is a public suffix
fun isPublicSuffix(domain: String): Boolean {
    val parts = domain.lowercase().split(".")
    return when {
        parts.size >= 2 -> {
//            val suffix1 = parts.takeLast(1).joinToString(".")
            val suffix2 = parts.takeLast(2).joinToString(".")
            publicSuffixes.contains(suffix2) //|| publicSuffixes.contains(suffix1)
        }
        else -> publicSuffixes.contains(domain)
    }
}

// Normalize domains (e.g. IDN to ASCII)
fun normalizeDomain(domain: String): String {
    return IDN.toASCII(domain.trim().lowercase())
}

// Main validation function
fun isValidCookieDomain(cookieDomain: String, requestUrl: URI): Boolean {
    val cookieDomain = try {
        normalizeDomain(cookieDomain)
    } catch (_: Throwable) { return false }
    val requestHost = try {
        normalizeDomain(requestUrl.host ?: return false)
    } catch (_: Throwable) { return false }

    // 1. Cookie domain must not be a public suffix
    if (isPublicSuffix(cookieDomain)) {
        log.d("❌ Cookie domain is a public suffix.")
        return false
    }

    // 2. Cookie domain must domain-match the request host (RFC 6265)
    if (!requestHost.endsWith(cookieDomain)) {
        log.d("❌ Cookie domain does not match request host.")
        return false
    }

    // 3. Prevent setting cookie for parent domains unless it's a valid superdomain
    val hostParts = requestHost.split(".")
    val domainParts = cookieDomain.split(".")

    if (domainParts.size < 2 || domainParts.size > hostParts.size) {
        log.d("❌ Invalid domain depth.")
        return false
    }

    // 4. Prevent setting cookie for TLD only (e.g. ".com")
    if (cookieDomain.count { it == '.' } < 1) {
        log.d("❌ Cookie domain is too shallow (likely a TLD).")
        return false
    }

    return true
}

fun List<Cookie>.toCookieHeader() = joinToString("; ") { it.toHeaderString() }

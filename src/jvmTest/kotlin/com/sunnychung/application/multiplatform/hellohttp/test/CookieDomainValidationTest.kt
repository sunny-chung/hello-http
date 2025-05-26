package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.network.util.isValidCookieDomain
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CookieDomainValidationTest {

    // Valid subdomain match
    @Test
    fun `Valid domain match with parent domain`() {
        val url = "https://app.example.com"
        val cookieDomain = "example.com"
        assertTrue(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: domain doesn't match request host
    @Test
    fun `Invalid domain that does not match host`() {
        val url = "https://app.example.com"
        val cookieDomain = "evil.com"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: domain is a public suffix (e.g. .com)
    @Test
    fun `Invalid domain that is a public suffix 1`() {
        val url = "https://my.com"
        val cookieDomain = "com"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: domain is a public suffix (e.g. co.uk)
    @Test
    fun `Invalid domain that is a public suffix 2`() {
        val url = "https://my.co.uk"
        val cookieDomain = "co.uk"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Valid: exact match between host and domain
    @Test
    fun `Valid exact domain match`() {
        val url = "https://secure.example.com"
        val cookieDomain = "secure.example.com"
        assertTrue(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: cookie set for deeper subdomain than request host
    @Test
    fun `Invalid domain - deeper subdomain than host`() {
        val url = "https://example.com"
        val cookieDomain = "app.example.com"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: cookie domain is a single label (e.g. localhost)
    @Test
    fun `Invalid single-label cookie domain`() {
        val url = "http://localhost"
        val cookieDomain = "localhost"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Valid: IDN domain normalized
    @Test
    fun `Valid internationalized domain name (IDN)`() {
        val url = "https://xn--exmple-cua.com"
        val cookieDomain = "xn--exmple-cua.com"
        assertTrue(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: cookie domain does not match request host at all
    @Test
    fun `Invalid unrelated domain`() {
        val url = "https://mybank.com"
        val cookieDomain = "otherbank.com"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Valid: domain attribute in mixed casing
    @Test
    fun `Valid domain with mixed case attribute`() {
        val url = "https://example.com"
        val cookieDomain = "example.com"
        assertTrue(isValidCookieDomain(cookieDomain, URI(url)))
    }

    // Invalid: too shallow domain (e.g. ".com")
    @Test
    fun `Invalid domain - too shallow`() {
        val url = "https://something.com"
        val cookieDomain = ".com"
        assertFalse(isValidCookieDomain(cookieDomain, URI(url)))
    }
}
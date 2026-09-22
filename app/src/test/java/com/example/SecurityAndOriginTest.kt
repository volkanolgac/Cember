package com.example

import android.net.Uri
import com.example.volkan.config.AppConfig
import com.example.volkan.util.OriginValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndOriginTest {

    @Test
    fun testExactTrustedLocalOriginMatching() {
        // Valid exact origin
        assertTrue(OriginValidator.isTrustedLocalOrigin("https://appassets.androidplatform.net"))
        assertTrue(OriginValidator.isTrustedLocalOrigin("https://appassets.androidplatform.net:443"))
        assertTrue(OriginValidator.isTrustedLocalOrigin("https://appassets.androidplatform.net/assets/index.html"))

        // Security attacks & variations that MUST fail
        assertFalse(OriginValidator.isTrustedLocalOrigin("http://appassets.androidplatform.net"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("https://appassets.androidplatform.net.evil.com"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("https://appassets.androidplatform.net:8080"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("https://evil.com/appassets.androidplatform.net"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("file:///android_asset/web/index.html"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("data:text/html,<html>"))
        assertFalse(OriginValidator.isTrustedLocalOrigin("javascript:alert(1)"))
        assertFalse(OriginValidator.isTrustedLocalOrigin(""))
        assertFalse(OriginValidator.isTrustedLocalOrigin(null as String?))
    }

    @Test
    fun testOriginParser() {
        val parsed = OriginValidator.parseOrigin("https://api.myserver.com:8443/v1/data")
        assertNotNull(parsed)
        assertEquals("https", parsed?.scheme)
        assertEquals("api.myserver.com", parsed?.host)
        assertEquals(8443, parsed?.port)
        assertEquals("https://api.myserver.com:8443", parsed?.toOriginString())

        // Default port normalization
        val defaultHttps = OriginValidator.parseOrigin("https://appassets.androidplatform.net")
        assertEquals(443, defaultHttps?.port)

        val defaultHttp = OriginValidator.parseOrigin("http://insecure.example.com")
        assertEquals(80, defaultHttp?.port)

        // Invalid schemes
        assertNull(OriginValidator.parseOrigin("file:///tmp/test"))
        assertNull(OriginValidator.parseOrigin("data:image/png;base64,..."))
        assertNull(OriginValidator.parseOrigin("blob:https://appassets.androidplatform.net/uuid"))
    }

    @Test
    fun testAllowedRemoteOrigins() {
        val config = AppConfig(
            allowedRemoteOrigins = listOf("https://api.myserver.com", "https://cdn.myserver.com:8080")
        )

        assertTrue(OriginValidator.isOriginAllowed("https://api.myserver.com/users", config))
        assertTrue(OriginValidator.isOriginAllowed("https://cdn.myserver.com:8080/assets/hero.jpg", config))

        // Mismatched origins
        assertFalse(OriginValidator.isOriginAllowed("https://api.myserver.com:9000", config))
        assertFalse(OriginValidator.isOriginAllowed("http://api.myserver.com", config))
        assertFalse(OriginValidator.isOriginAllowed("https://api.myserver.com.attacker.com", config))
        assertFalse(OriginValidator.isOriginAllowed("https://other.com", config))
    }
}

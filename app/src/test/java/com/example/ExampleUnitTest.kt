package com.example

import com.example.volkan.bridge.BridgeResponse
import com.example.volkan.config.AppConfig
import com.example.volkan.config.FullscreenMode
import com.example.volkan.config.OrientationMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testAppConfigJsonParsing() {
        val json = """
            {
                "appName": "Test Web Game",
                "applicationId": "com.volkan.game",
                "versionName": "2.1.0",
                "versionCode": 42,
                "orientationMode": "LANDSCAPE",
                "fullscreenMode": "IMMERSIVE_STICKY",
                "vibrationEnabled": true,
                "allowedRemoteOrigins": ["https://api.game.com"]
            }
        """.trimIndent()

        val config = AppConfig.parse(json)
        assertEquals("Test Web Game", config.appName)
        assertEquals("com.volkan.game", config.applicationId)
        assertEquals("2.1.0", config.versionName)
        assertEquals(42, config.versionCode)
        assertEquals(OrientationMode.LANDSCAPE, config.orientationMode)
        assertEquals(FullscreenMode.IMMERSIVE_STICKY, config.fullscreenMode)
        assertTrue(config.vibrationEnabled)
        assertEquals(listOf("https://api.game.com"), config.allowedRemoteOrigins)
    }

    @Test
    fun testBridgeResponseSerialization() {
        val data = JSONObject().put("status", "ok")
        val res = BridgeResponse(requestId = "123", success = true, data = data)
        val jsonStr = res.toJson()
        assertTrue(jsonStr.contains("123"))
        assertTrue(jsonStr.contains("true"))
        assertTrue(jsonStr.contains("ok"))
    }
}

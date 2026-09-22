package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.volkan.config.AppConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testReadStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertNotNull(appName)
        assertTrue(appName.isNotEmpty())
    }

    @Test
    fun testLoadAppConfigFromAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AppConfig.loadFromAssets(context)
        assertNotNull(config)
        assertNotNull(config.appName)
        assertTrue(config.appName.isNotEmpty())
        assertNotNull(config.applicationId)
        assertTrue(config.applicationId.isNotEmpty())
    }
}

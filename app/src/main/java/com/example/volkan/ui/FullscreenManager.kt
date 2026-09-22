package com.example.volkan.ui

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.volkan.config.AppConfig
import com.example.volkan.config.FullscreenMode

class FullscreenManager(
    private val activity: Activity,
    private val appConfig: AppConfig
) {
    private fun getInsetsController(): WindowInsetsControllerCompat? {
        val window = activity.window ?: return null
        val decorView = window.decorView ?: return null
        return WindowCompat.getInsetsController(window, decorView)
    }

    fun applyConfiguration() {
        when (appConfig.fullscreenMode) {
            FullscreenMode.IMMERSIVE_STICKY -> enterFullscreen()
            FullscreenMode.EDGE_TO_EDGE -> applyEdgeToEdge()
            FullscreenMode.NORMAL -> exitFullscreen()
        }
    }

    fun enterFullscreen() {
        val window = activity.window ?: return

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = getInsetsController() ?: return
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun exitFullscreen() {
        val window = activity.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = getInsetsController() ?: return
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun applyEdgeToEdge() {
        enterFullscreen()
    }
}

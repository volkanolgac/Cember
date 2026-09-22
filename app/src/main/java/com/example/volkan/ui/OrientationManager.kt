package com.example.volkan.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import com.example.volkan.config.AppConfig
import com.example.volkan.config.OrientationMode

class OrientationManager(
    private val activity: Activity,
    private val appConfig: AppConfig
) {
    fun applyInitialOrientation() {
        applyOrientation(appConfig.orientationMode)
    }

    fun applyOrientation(mode: OrientationMode) {
        activity.requestedOrientation = when (mode) {
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.USER -> ActivityInfo.SCREEN_ORIENTATION_USER
            OrientationMode.UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

package com.example.volkan.util

import android.util.Log
import com.example.BuildConfig

/**
 * Production-ready structured logger for Volkan Web2Android.
 * Sanitizes URLs and sensitive parameters, suppressing verbose output in release builds.
 */
object VolkanLogger {
    const val TAG_SHELL = "VOLKAN_SHELL"
    const val TAG_WEBVIEW = "VOLKAN_WEBVIEW"
    const val TAG_BRIDGE = "VOLKAN_BRIDGE"
    const val TAG_NETWORK = "VOLKAN_NETWORK"
    const val TAG_PERMISSION = "VOLKAN_PERMISSION"
    const val TAG_NAVIGATION = "VOLKAN_NAVIGATION"
    const val TAG_DOWNLOAD = "VOLKAN_DOWNLOAD"

    private val isDebug: Boolean = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, sanitize(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, sanitize(message), throwable)
        } else {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, sanitize(message), throwable)
        } else {
            Log.e(tag, sanitize(message))
        }
    }

    /**
     * Strips potentially sensitive query parameters (tokens, passwords, api keys)
     */
    private fun sanitize(message: String): String {
        return message.replace(Regex("(?i)(password|token|key|auth|secret)=[^&\\s]+"), "$1=***")
    }
}

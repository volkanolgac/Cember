package com.example.volkan.bridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.volkan.config.AppConfig
import com.example.volkan.config.FullscreenMode
import com.example.volkan.config.OrientationMode
import com.example.volkan.navigation.NavigationDecision
import com.example.volkan.navigation.NavigationManager
import com.example.volkan.network.NetworkMonitor
import com.example.volkan.permission.PermissionManager
import com.example.volkan.ui.FullscreenManager
import com.example.volkan.ui.OrientationManager
import com.example.volkan.util.OriginValidator
import com.example.volkan.util.VolkanLogger
import org.json.JSONObject

class VolkanBridgeManager(
    private val activity: Activity,
    private val appConfig: AppConfig,
    private val networkMonitor: NetworkMonitor,
    private val permissionManager: PermissionManager,
    private val fullscreenManager: FullscreenManager,
    private val orientationManager: OrientationManager,
    private val navigationManager: NavigationManager,
    private val onNavigateWebView: (String) -> Unit
) {

    fun attachBridge(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            VolkanLogger.e(VolkanLogger.TAG_BRIDGE, "WEB_MESSAGE_LISTENER feature is not supported on this WebKit runtime. Bridge disabled.")
            return
        }

        // Strict allowed origins set: exact local asset origin and configured app origin
        val allowedOrigins = mutableSetOf(OriginValidator.LOCAL_ASSET_ORIGIN)
        val configuredAppOrigin = OriginValidator.parseOrigin(appConfig.webAppOrigin)
        if (configuredAppOrigin != null && configuredAppOrigin.scheme == "https") {
            allowedOrigins.add(configuredAppOrigin.toOriginString())
        }

        VolkanLogger.i(VolkanLogger.TAG_BRIDGE, "Registering WebMessageListener 'VolkanAndroid' with allowed origins: $allowedOrigins")

        WebViewCompat.addWebMessageListener(
            webView,
            "VolkanAndroid",
            allowedOrigins
        ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
            handleIncomingMessage(message, sourceOrigin, isMainFrame, replyProxy)
        }
    }

    private fun handleIncomingMessage(
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        // 1. Reject non-main-frame execution
        if (!isMainFrame) {
            VolkanLogger.w(VolkanLogger.TAG_BRIDGE, "Rejected bridge message from non-main frame (origin=$sourceOrigin)")
            return
        }

        // 2. Reject untrusted or non-matching origins
        if (!OriginValidator.isTrustedAppOrigin(sourceOrigin, appConfig)) {
            VolkanLogger.w(VolkanLogger.TAG_BRIDGE, "Rejected bridge message from unauthorized origin: $sourceOrigin")
            return
        }

        val rawData = message.data ?: return
        try {
            val json = JSONObject(rawData)
            val requestId = json.optString("requestId", "")
            val action = json.optString("action", "")
            val params = json.optJSONObject("params") ?: JSONObject()

            if (requestId.isBlank() || action.isBlank()) {
                VolkanLogger.w(VolkanLogger.TAG_BRIDGE, "Received malformed bridge message (missing requestId or action)")
                return
            }

            processAction(requestId, action, params, replyProxy)
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_BRIDGE, "Failed to parse bridge JSON payload: ${e.message}")
        }
    }

    private fun processAction(
        requestId: String,
        action: String,
        params: JSONObject,
        replyProxy: JavaScriptReplyProxy
    ) {
        when (action) {
            "ping" -> {
                sendSuccess(replyProxy, requestId, JSONObject().apply {
                    put("pong", true)
                    put("timestamp", System.currentTimeMillis())
                })
            }

            "getAppInfo" -> {
                sendSuccess(replyProxy, requestId, JSONObject().apply {
                    put("appName", appConfig.appName)
                    put("applicationId", appConfig.applicationId)
                    put("versionName", appConfig.versionName)
                    put("versionCode", appConfig.versionCode)
                    put("sdkVersion", Build.VERSION.SDK_INT)
                    put("deviceModel", Build.MODEL)
                    put("manufacturer", Build.MANUFACTURER)
                })
            }

            "getCapabilities" -> {
                sendSuccess(replyProxy, requestId, JSONObject().apply {
                    put("camera", appConfig.cameraEnabled)
                    put("microphone", appConfig.microphoneEnabled)
                    put("geolocation", appConfig.geolocationEnabled)
                    put("notifications", appConfig.notificationsEnabled)
                    put("vibration", appConfig.vibrationEnabled)
                    put("share", appConfig.shareEnabled)
                    put("clipboard", appConfig.clipboardEnabled)
                    put("filePicker", appConfig.filePickerEnabled)
                    put("downloads", appConfig.downloadPolicyEnabled)
                    put("uploads", appConfig.uploadPolicyEnabled)
                    put("keepScreenOn", appConfig.keepScreenOn)
                    put("offlineSupport", appConfig.offlineMode)
                })
            }

            "getNetworkState" -> {
                val state = networkMonitor.getCurrentNetworkState()
                sendSuccess(replyProxy, requestId, JSONObject().apply {
                    put("isConnected", state.isConnected)
                    put("isValidated", state.isValidated)
                    put("status", state.status.name)
                    put("type", state.type.name)
                    put("isWifi", state.isWifi)
                    put("isCellular", state.isCellular)
                })
            }

            "vibrate" -> {
                if (!appConfig.vibrationEnabled) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Vibration is disabled in app-config.json")
                    return
                }

                val duration = params.optLong("duration", 100L).coerceIn(10L, 5000L)
                performVibration(duration)
                sendSuccess(replyProxy, requestId, JSONObject().apply { put("vibrated", true) })
            }

            "share" -> {
                if (!appConfig.shareEnabled) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Native sharing is disabled in app-config.json")
                    return
                }

                val title = params.optString("title", appConfig.appName)
                val text = params.optString("text", "")
                val url = params.optString("url", "")
                val fullText = if (url.isNotBlank() && !text.contains(url)) {
                    if (text.isNotBlank()) "$text $url" else url
                } else {
                    text
                }

                activity.runOnUiThread {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, fullText)
                        }
                        activity.startActivity(Intent.createChooser(shareIntent, title))
                        sendSuccess(replyProxy, requestId, JSONObject().apply { put("shared", true) })
                    } catch (e: Exception) {
                        sendError(replyProxy, requestId, "SHARE_FAILED", e.message ?: "Failed to trigger share sheet")
                    }
                }
            }

            "copyToClipboard" -> {
                if (!appConfig.clipboardEnabled) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Clipboard is disabled in app-config.json")
                    return
                }

                val text = params.optString("text", "")
                activity.runOnUiThread {
                    try {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Text", text)
                        clipboard.setPrimaryClip(clip)
                        sendSuccess(replyProxy, requestId, JSONObject().apply { put("copied", true) })
                    } catch (e: Exception) {
                        sendError(replyProxy, requestId, "CLIPBOARD_ERROR", e.message ?: "Failed to copy to clipboard")
                    }
                }
            }

            "getClipboard" -> {
                if (!appConfig.clipboardEnabled) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Clipboard is disabled in app-config.json")
                    return
                }

                activity.runOnUiThread {
                    try {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val item = clipboard.primaryClip?.getItemAt(0)
                        val text = item?.text?.toString() ?: ""
                        sendSuccess(replyProxy, requestId, JSONObject().apply { put("text", text) })
                    } catch (e: Exception) {
                        sendError(replyProxy, requestId, "CLIPBOARD_ERROR", e.message ?: "Failed to read clipboard")
                    }
                }
            }

            "keepScreenOn" -> {
                val enabled = params.optBoolean("enabled", true)
                // Enforce keepScreenOn capability: allow dynamic change if enabled or if requested disable
                if (enabled && !appConfig.keepScreenOn) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "keepScreenOn capability is false in app-config.json")
                    return
                }

                activity.runOnUiThread {
                    if (enabled) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    sendSuccess(replyProxy, requestId, JSONObject().apply { put("keepScreenOn", enabled) })
                }
            }

            "setOrientation" -> {
                val modeStr = params.optString("mode", "PORTRAIT").uppercase()
                val targetMode = try {
                    OrientationMode.valueOf(modeStr)
                } catch (_: Exception) {
                    OrientationMode.PORTRAIT
                }
                activity.runOnUiThread {
                    orientationManager.applyOrientation(targetMode)
                    sendSuccess(replyProxy, requestId, JSONObject().apply { put("orientation", targetMode.name) })
                }
            }

            "setFullscreen" -> {
                if (appConfig.fullscreenMode == FullscreenMode.NORMAL) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Fullscreen mode is set to NORMAL (disabled) in app-config.json")
                    return
                }

                val fullscreen = params.optBoolean("fullscreen", true)
                activity.runOnUiThread {
                    if (fullscreen) {
                        fullscreenManager.enterFullscreen()
                    } else {
                        fullscreenManager.exitFullscreen()
                    }
                    sendSuccess(replyProxy, requestId, JSONObject().apply { put("fullscreen", fullscreen) })
                }
            }

            "requestNotificationPermission" -> {
                if (!appConfig.notificationsEnabled) {
                    sendError(replyProxy, requestId, "CAPABILITY_DISABLED", "Notifications are disabled in app-config.json")
                    return
                }

                activity.runOnUiThread {
                    permissionManager.requestNotificationPermission { granted ->
                        sendSuccess(replyProxy, requestId, JSONObject().apply { put("granted", granted) })
                    }
                }
            }

            "openExternal" -> {
                val url = params.optString("url", "")
                if (url.isBlank()) {
                    sendError(replyProxy, requestId, "INVALID_URL", "URL must not be blank")
                    return
                }

                activity.runOnUiThread {
                    try {
                        when (navigationManager.evaluateUrl(url)) {
                            NavigationDecision.HandledExternally -> {
                                sendSuccess(replyProxy, requestId, JSONObject().apply {
                                    put("outcome", "OPENED_EXTERNALLY")
                                    put("opened", true)
                                })
                            }
                            NavigationDecision.AllowInWebView -> {
                                onNavigateWebView(url)
                                sendSuccess(replyProxy, requestId, JSONObject().apply {
                                    put("outcome", "OPENED_IN_WEBVIEW")
                                    put("opened", true)
                                })
                            }
                            NavigationDecision.Blocked -> {
                                sendError(replyProxy, requestId, "BLOCKED", "Navigation to $url is blocked by app configuration")
                            }
                        }
                    } catch (e: Exception) {
                        sendError(replyProxy, requestId, "ERROR", e.message ?: "Failed to navigate to $url")
                    }
                }
            }

            "exitApp", "closeApp" -> {
                activity.runOnUiThread {
                    activity.finishAffinity()
                }
                sendSuccess(replyProxy, requestId, JSONObject().apply { put("closed", true) })
            }

            else -> {
                sendError(replyProxy, requestId, "UNKNOWN_ACTION", "Unknown action: $action")
            }
        }
    }

    private fun performVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            VolkanLogger.w(VolkanLogger.TAG_BRIDGE, "Vibration failed: ${e.message}")
        }
    }

    private fun sendSuccess(replyProxy: JavaScriptReplyProxy, requestId: String, data: JSONObject) {
        val response = BridgeResponse(requestId = requestId, success = true, data = data)
        try {
            replyProxy.postMessage(response.toJson())
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_BRIDGE, "Failed to post bridge reply: ${e.message}")
        }
    }

    private fun sendError(replyProxy: JavaScriptReplyProxy, requestId: String, errorCode: String, message: String) {
        val response = BridgeResponse(requestId = requestId, success = false, error = errorCode, message = message)
        try {
            replyProxy.postMessage(response.toJson())
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_BRIDGE, "Failed to post bridge reply: ${e.message}")
        }
    }
}

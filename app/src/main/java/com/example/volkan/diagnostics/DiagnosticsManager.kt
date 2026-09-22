package com.example.volkan.diagnostics

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.BuildConfig
import com.example.volkan.config.AppConfig
import com.example.volkan.network.NetworkMonitor

data class DiagnosticsInfo(
    val appName: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val androidVersion: String,
    val sdkInt: Int,
    val webViewPackage: String,
    val webAppOrigin: String,
    val webAppEntry: String,
    val networkState: String,
    val webMessageListenerSupported: Boolean,
    val safeBrowsingSupported: Boolean
)

class DiagnosticsManager(
    private val context: Context,
    private val appConfig: AppConfig,
    private val networkMonitor: NetworkMonitor
) {
    fun collectDiagnostics(): DiagnosticsInfo {
        val webViewPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebViewCompat.getCurrentWebViewPackage(context)?.let {
                "${it.packageName} (${it.versionName})"
            } ?: "System Default"
        } else {
            "System Default"
        }

        val netState = networkMonitor.getCurrentNetworkState()

        return DiagnosticsInfo(
            appName = appConfig.appName,
            applicationId = appConfig.applicationId,
            versionName = appConfig.versionName,
            versionCode = appConfig.versionCode,
            buildType = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE",
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            webViewPackage = webViewPackage,
            webAppOrigin = appConfig.webAppOrigin,
            webAppEntry = appConfig.webAppEntry,
            networkState = if (netState.isConnected) "ONLINE (${netState.type})" else "OFFLINE",
            webMessageListenerSupported = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER),
            safeBrowsingSupported = WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)
        )
    }

    fun clearWebViewData(webView: WebView, onComplete: () -> Unit) {
        webView.clearCache(true)
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            onComplete()
        }
    }
}

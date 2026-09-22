package com.example.volkan.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import com.example.BuildConfig
import com.example.volkan.config.AppConfig
import com.example.volkan.config.MultipleWindowPolicy
import com.example.volkan.util.VolkanLogger

class WebViewManager(
    private val context: Context,
    private val appConfig: AppConfig,
    private val assetLoaderManager: WebViewAssetLoaderManager
) {

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: WebView) {
        VolkanLogger.i(VolkanLogger.TAG_WEBVIEW, "Configuring WebView for high-performance execution and security")

        // Enable hardware acceleration for 60 FPS Canvas and WebGL
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isScrollbarFadingEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.isHapticFeedbackEnabled = true

        val settings = webView.settings

        // 1. JavaScript & Storage APIs
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        // 2. Viewport & Touch
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // 3. Media & Audio autoplay support
        settings.mediaPlaybackRequiresUserGesture = false

        // 4. Strict Security Hardening
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        // 5. Google Safe Browsing
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            settings.safeBrowsingEnabled = true
        }

        // 6. Multi-window / Popups
        val supportPopups = appConfig.multipleWindowPolicy != MultipleWindowPolicy.BLOCK
        settings.setSupportMultipleWindows(supportPopups)
        settings.javaScriptCanOpenWindowsAutomatically = supportPopups

        // 7. Caching Strategy
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 8. Custom User-Agent
        val defaultUa = settings.userAgentString
        if (appConfig.customUserAgentSuffix.isNotBlank()) {
            settings.userAgentString = "$defaultUa ${appConfig.customUserAgentSuffix.trim()}"
        }

        // 9. Cookie Management
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, false)

        // 10. Service Worker Support (if enabled in config and supported by device)
        if (appConfig.serviceWorkerEnabled && WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            try {
                val swController = ServiceWorkerControllerCompat.getInstance()
                swController.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                        return assetLoaderManager.shouldInterceptRequest(request.url)
                    }
                })
                VolkanLogger.i(VolkanLogger.TAG_WEBVIEW, "ServiceWorker controller configured with local asset interceptor")
            } catch (e: Exception) {
                VolkanLogger.w(VolkanLogger.TAG_WEBVIEW, "Could not initialize ServiceWorkerController: ${e.message}")
            }
        }

        // 11. Debugging strictly disabled in release builds
        if (BuildConfig.DEBUG && appConfig.debugToolsEnabled) {
            WebView.setWebContentsDebuggingEnabled(true)
            VolkanLogger.i(VolkanLogger.TAG_WEBVIEW, "WebContentsDebugging ENABLED for debug build")
        } else {
            WebView.setWebContentsDebuggingEnabled(false)
        }
    }
}

package com.example.volkan.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.volkan.config.AppConfig
import com.example.volkan.navigation.NavigationDecision
import com.example.volkan.navigation.NavigationManager
import com.example.volkan.ui.NativeOverlayManager
import com.example.volkan.util.VolkanLogger

class VolkanWebViewClient(
    private val appConfig: AppConfig,
    private val assetLoaderManager: WebViewAssetLoaderManager,
    private val navigationManager: NavigationManager,
    private val overlayManager: NativeOverlayManager,
    private val onFirstFrameCallback: () -> Unit = {},
    private val onPageFinishedCallback: (String) -> Unit,
    private val onRendererCrashCallback: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        VolkanLogger.d(VolkanLogger.TAG_WEBVIEW, "shouldOverrideUrlLoading: $url (isForMainFrame=${request.isForMainFrame})")

        return when (navigationManager.evaluateUrl(url)) {
            NavigationDecision.AllowInWebView -> false
            NavigationDecision.HandledExternally -> true
            NavigationDecision.Blocked -> true
        }
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return assetLoaderManager.shouldInterceptRequest(request.url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        VolkanLogger.d(VolkanLogger.TAG_WEBVIEW, "onPageStarted: $url")
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        super.onPageCommitVisible(view, url)
        VolkanLogger.i(VolkanLogger.TAG_WEBVIEW, "First visible frame committed for: $url")
        onFirstFrameCallback()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        VolkanLogger.d(VolkanLogger.TAG_WEBVIEW, "onPageFinished: $url")
        overlayManager.hide()
        onFirstFrameCallback()
        url?.let { onPageFinishedCallback(it) }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error.description?.toString() ?: "Error code ${error.errorCode}"
            } else {
                "Load error"
            }
            VolkanLogger.e(VolkanLogger.TAG_WEBVIEW, "Main frame load error on ${request.url}: $description")
            overlayManager.showError("Failed to load page", description)
        }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request.isForMainFrame) {
            VolkanLogger.w(VolkanLogger.TAG_WEBVIEW, "HTTP error ${errorResponse.statusCode} for ${request.url}")
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        VolkanLogger.e(VolkanLogger.TAG_WEBVIEW, "SSL Error: ${error.primaryError} on URL ${error.url}")
        // Strictly reject invalid SSL certificates in production
        handler.cancel()
        overlayManager.showError("Security Error", "The SSL certificate for this connection is invalid or untrusted.")
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        val didCrash = detail.didCrash()
        VolkanLogger.e(VolkanLogger.TAG_WEBVIEW, "WebView render process gone. Crashed: $didCrash")
        onRendererCrashCallback()
        // Returning true informs the OS that we have handled the crash, preventing host process termination
        return true
    }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse
    ) {
        VolkanLogger.e(VolkanLogger.TAG_WEBVIEW, "SafeBrowsing hit: threatType=$threatType on ${request.url}")
        callback.backToSafety(true)
        overlayManager.showError("Unsafe Content Blocked", "The page was flagged as unsafe by Google Safe Browsing.")
    }
}

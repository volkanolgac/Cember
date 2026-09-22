package com.example.volkan.webview

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import com.example.BuildConfig
import com.example.volkan.config.AppConfig
import com.example.volkan.config.MultipleWindowPolicy
import com.example.volkan.media.FileChooserManager
import com.example.volkan.navigation.NavigationManager
import com.example.volkan.permission.PermissionManager
import com.example.volkan.util.OriginValidator
import com.example.volkan.util.VolkanLogger

class VolkanWebChromeClient(
    private val activity: Activity,
    private val appConfig: AppConfig,
    private val fileChooserManager: FileChooserManager,
    private val permissionManager: PermissionManager,
    private val navigationManager: NavigationManager,
    private val customViewContainer: FrameLayout,
    private val webView: WebView
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        return fileChooserManager.handleShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        val originStr = request.origin.toString()
        val resources = request.resources

        VolkanLogger.i(VolkanLogger.TAG_PERMISSION, "PermissionRequest for [${resources.joinToString()}] from $originStr")

        permissionManager.requestWebPermissions(originStr, resources) { approvedResources ->
            activity.runOnUiThread {
                if (approvedResources.isNotEmpty()) {
                    VolkanLogger.i(VolkanLogger.TAG_PERMISSION, "Granting approved resources: ${approvedResources.joinToString()}")
                    request.grant(approvedResources)
                } else {
                    VolkanLogger.w(VolkanLogger.TAG_PERMISSION, "Denying permission request for $originStr")
                    request.deny()
                }
            }
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        VolkanLogger.d(VolkanLogger.TAG_PERMISSION, "PermissionRequest canceled by WebView")
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        permissionManager.requestGeolocationPermission(origin, callback)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (resultMsg == null) return false

        when (appConfig.multipleWindowPolicy) {
            MultipleWindowPolicy.BLOCK -> {
                VolkanLogger.d(VolkanLogger.TAG_WEBVIEW, "Popup window creation blocked by configuration")
                return false
            }

            MultipleWindowPolicy.OPEN_EXTERNAL -> {
                val transport = resultMsg.obj as? WebView.WebViewTransport
                if (transport != null) {
                    val tempWebView = WebView(activity)
                    tempWebView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                            navigationManager.launchBrowser(request.url)
                            tempWebView.destroy()
                            return true
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                    return true
                }
                return false
            }

            MultipleWindowPolicy.ALLOW_SAME_ORIGIN -> {
                val transport = resultMsg.obj as? WebView.WebViewTransport
                if (transport != null) {
                    val tempWebView = WebView(activity)
                    tempWebView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                            val targetUrl = request.url.toString()
                            if (OriginValidator.isTrustedAppOrigin(request.url, appConfig)) {
                                webView.loadUrl(targetUrl)
                            } else {
                                navigationManager.launchBrowser(request.url)
                            }
                            tempWebView.destroy()
                            return true
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                    return true
                }
                return false
            }
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback

        webView.visibility = View.GONE
        customViewContainer.visibility = View.VISIBLE
        customViewContainer.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onHideCustomView() {
        if (customView == null) return
        customViewContainer.removeView(customView)
        customView = null
        customViewContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
    }

    fun isCustomViewShowing(): Boolean = customView != null

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (BuildConfig.DEBUG && consoleMessage != null) {
            VolkanLogger.d(
                VolkanLogger.TAG_WEBVIEW,
                "[JS Console] ${consoleMessage.message()} -- line ${consoleMessage.lineNumber()} (${consoleMessage.sourceId()})"
            )
        }
        return true
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        AlertDialog.Builder(activity)
            .setTitle(appConfig.appName)
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        AlertDialog.Builder(activity)
            .setTitle(appConfig.appName)
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        val input = EditText(activity).apply {
            setText(defaultValue ?: "")
        }
        AlertDialog.Builder(activity)
            .setTitle(appConfig.appName)
            .setMessage(message ?: "")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                result?.confirm(input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                result?.cancel()
            }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
    }
}

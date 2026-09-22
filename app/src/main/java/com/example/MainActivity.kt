package com.example

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.volkan.bridge.VolkanBridgeManager
import com.example.volkan.config.AppConfig
import com.example.volkan.config.BackButtonPolicy
import com.example.volkan.diagnostics.DiagnosticsDialog
import com.example.volkan.diagnostics.DiagnosticsManager
import com.example.volkan.download.VolkanDownloadManager
import com.example.volkan.media.FileChooserManager
import com.example.volkan.navigation.NavigationManager
import com.example.volkan.network.NetworkMonitor
import com.example.volkan.permission.PermissionManager
import com.example.volkan.ui.FullscreenManager
import com.example.volkan.ui.NativeOverlayManager
import com.example.volkan.ui.OverlayType
import com.example.volkan.ui.OrientationManager
import com.example.volkan.util.VolkanLogger
import com.example.volkan.webview.VolkanWebChromeClient
import com.example.volkan.webview.VolkanWebViewClient
import com.example.volkan.webview.WebViewAssetLoaderManager
import com.example.volkan.webview.WebViewManager

class MainActivity : ComponentActivity() {

    private lateinit var appConfig: AppConfig
    private var webView: WebView? = null
    private lateinit var rootContainer: FrameLayout
    private lateinit var customViewContainer: FrameLayout
    private lateinit var overlayContainer: FrameLayout

    private lateinit var fullscreenManager: FullscreenManager
    private lateinit var orientationManager: OrientationManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var permissionManager: PermissionManager
    private lateinit var fileChooserManager: FileChooserManager
    private lateinit var downloadManager: VolkanDownloadManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var assetLoaderManager: WebViewAssetLoaderManager
    private lateinit var webViewManager: WebViewManager
    private var webChromeClient: VolkanWebChromeClient? = null
    private var webViewClient: VolkanWebViewClient? = null
    private lateinit var bridgeManager: VolkanBridgeManager
    private lateinit var overlayManager: NativeOverlayManager
    private lateinit var diagnosticsManager: DiagnosticsManager

    private var isAppLoaded = false
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install modern Jetpack Splash Screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            !isAppReady && (!this::overlayManager.isInitialized || overlayManager.currentType == OverlayType.NONE)
        }

        super.onCreate(savedInstanceState)
        VolkanLogger.i(VolkanLogger.TAG_SHELL, "MainActivity onCreate starting")

        // 1. Load Application Configuration
        appConfig = AppConfig.loadFromAssets(this)

        // 2. Setup Layout Containers
        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        customViewContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = android.view.View.GONE
        }

        overlayContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(rootContainer)

        // 3. Setup Overlays
        overlayManager = NativeOverlayManager(
            context = this,
            container = overlayContainer,
            onRetryClicked = { recreateAndLoadWebView() },
            onDiagnosticsClicked = { showDiagnostics() }
        )

        // 4. Setup Single NetworkMonitor Instance FIRST
        networkMonitor = NetworkMonitor(this) { networkState ->
            if (appConfig.networkRequired && !networkState.isConnected && !isAppLoaded) {
                runOnUiThread {
                    overlayManager.showOfflineError()
                }
            }
        }

        // 5. Setup Managers and Inject Single NetworkMonitor Instance
        fullscreenManager = FullscreenManager(this, appConfig)
        orientationManager = OrientationManager(this, appConfig)
        navigationManager = NavigationManager(this, appConfig)
        permissionManager = PermissionManager(this, appConfig)
        fileChooserManager = FileChooserManager(this, appConfig)
        downloadManager = VolkanDownloadManager(this, appConfig)
        assetLoaderManager = WebViewAssetLoaderManager(this, appConfig)
        webViewManager = WebViewManager(this, appConfig, assetLoaderManager)

        bridgeManager = VolkanBridgeManager(
            activity = this,
            appConfig = appConfig,
            networkMonitor = networkMonitor,
            permissionManager = permissionManager,
            fullscreenManager = fullscreenManager,
            orientationManager = orientationManager,
            navigationManager = navigationManager,
            onNavigateWebView = { url ->
                runOnUiThread {
                    webView?.loadUrl(url)
                }
            }
        )

        diagnosticsManager = DiagnosticsManager(this, appConfig, networkMonitor)

        // 6. Apply Fullscreen and Orientation
        fullscreenManager.applyConfiguration()
        orientationManager.applyInitialOrientation()

        // 7. Setup Initial WebView
        setupNewWebView()

        // 8. Setup Back Button Navigation Handler
        setupBackNavigation()

        // 9. Restore or Load Web App
        if (savedInstanceState != null) {
            webView?.restoreState(savedInstanceState)
        } else {
            handleIntent(intent)
            loadApp()
        }
    }

    /**
     * Initializes and attaches a fresh WebView instance into the view tree.
     */
    private fun setupNewWebView() {
        val newWebView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        webViewManager.configureWebView(newWebView)

        newWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadManager.handleDownload(url, userAgent, contentDisposition, mimeType, contentLength)
        }

        val chromeClient = VolkanWebChromeClient(
            activity = this,
            appConfig = appConfig,
            fileChooserManager = fileChooserManager,
            permissionManager = permissionManager,
            navigationManager = navigationManager,
            customViewContainer = customViewContainer,
            webView = newWebView
        )
        webChromeClient = chromeClient
        newWebView.webChromeClient = chromeClient

        val wvClient = VolkanWebViewClient(
            appConfig = appConfig,
            assetLoaderManager = assetLoaderManager,
            navigationManager = navigationManager,
            overlayManager = overlayManager,
            onFirstFrameCallback = {
                if (!isAppReady) {
                    isAppReady = true
                    VolkanLogger.i(VolkanLogger.TAG_SHELL, "First usable frame ready - dismissing system splash screen")
                }
            },
            onPageFinishedCallback = { url ->
                isAppLoaded = true
                isAppReady = true
                VolkanLogger.i(VolkanLogger.TAG_SHELL, "Web app page loaded: $url")
            },
            onRendererCrashCallback = {
                VolkanLogger.e(VolkanLogger.TAG_SHELL, "Handling onRenderProcessGone recovery")
                runOnUiThread {
                    overlayManager.showRendererCrash()
                }
            }
        )
        webViewClient = wvClient
        newWebView.webViewClient = wvClient

        // Register WebMessageListener RPC bridge
        bridgeManager.attachBridge(newWebView)

        // Assemble view hierarchy
        rootContainer.removeAllViews()
        rootContainer.addView(newWebView)
        rootContainer.addView(customViewContainer)
        rootContainer.addView(overlayContainer)

        webView = newWebView
    }

    /**
     * Complete production recovery when renderer crashes or retry is pressed.
     */
    private fun recreateAndLoadWebView() {
        VolkanLogger.i(VolkanLogger.TAG_SHELL, "Recreating WebView instance after crash or reload request")
        destroyCurrentWebView()
        setupNewWebView()
        fullscreenManager.applyConfiguration()
        orientationManager.applyInitialOrientation()
        loadApp()
    }

    private fun destroyCurrentWebView() {
        webView?.let { wv ->
            rootContainer.removeView(wv)
            wv.stopLoading()
            wv.webChromeClient = null
            wv.webViewClient = WebViewClient()
            wv.setDownloadListener(null)
            wv.destroy()
        }
        webView = null
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val chrome = webChromeClient
                if (chrome != null && chrome.isCustomViewShowing()) {
                    chrome.onHideCustomView()
                    return
                }

                val wv = webView
                if (appConfig.backButtonPolicy == BackButtonPolicy.WEB_HISTORY_FIRST && wv != null && wv.canGoBack()) {
                    wv.goBack()
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun loadApp() {
        if (appConfig.networkRequired && !networkMonitor.getCurrentNetworkState().isConnected) {
            overlayManager.showOfflineError()
            return
        }

        val entryUrl = assetLoaderManager.getLocalEntryUrl()
        VolkanLogger.i(VolkanLogger.TAG_SHELL, "Loading web application from: $entryUrl")
        webView?.loadUrl(entryUrl)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        VolkanLogger.i(VolkanLogger.TAG_SHELL, "Handling deep link intent: $data")
    }

    private fun showDiagnostics() {
        val currentWv = webView
        if (currentWv != null) {
            DiagnosticsDialog.show(this, diagnosticsManager, currentWv) {
                recreateAndLoadWebView()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        fullscreenManager.enterFullscreen()
        webView?.onResume()
        networkMonitor.startMonitoring()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            fullscreenManager.enterFullscreen()
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        networkMonitor.stopMonitoring()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyCurrentWebView()
    }
}

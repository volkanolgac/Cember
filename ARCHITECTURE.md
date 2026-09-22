# Architecture Overview

The **Volkan Web2Android** architecture separates the native Android shell runtime from the web application payload.

```
VOLKAN WEB2ANDROID ARCHITECTURE
│
├── MainActivity.kt                <- Entry point, Activity lifecycle, insets, back dispatcher
│
├── config/
│   └── AppConfig.kt              <- Central configuration model & JSON asset loader
│
├── webview/
│   ├── WebViewManager.kt         <- WebSettings, Hardware Acceleration, DOM storage, Cookies
│   ├── WebViewAssetLoaderManager.kt <- WebViewAssetLoader mapping /assets/ to HTTPS origin
│   ├── VolkanWebViewClient.kt    <- Navigation evaluation, SSL checks, crash recovery
│   └── VolkanWebChromeClient.kt  <- Fullscreen custom view, file chooser, media permissions
│
├── bridge/
│   ├── BridgeProtocol.kt         <- Asynchronous JSON RPC schema (request/response)
│   └── VolkanBridgeManager.kt    <- Origin-restricted WebMessageListener & JS interface
│
├── network/
│   └── NetworkMonitor.kt         <- ConnectivityManager state tracker & event dispatcher
│
├── navigation/
│   └── NavigationManager.kt      <- External URLs, allowlists, and Android Intent router
│
├── permission/
│   └── PermissionManager.kt      <- Runtime Android permission requests with origin checks
│
├── media/
│   └── FileChooserManager.kt     <- ActivityResultLauncher bridge for <input type="file">
│
├── download/
│   └── VolkanDownloadManager.kt  <- System DownloadManager integration
│
├── ui/
│   ├── FullscreenManager.kt      <- WindowInsetsControllerCompat immersive controller
│   ├── OrientationManager.kt     <- Sensor/Portrait/Landscape orientation manager
│   └── NativeOverlayManager.kt   <- Native error, loading, and renderer crash overlays
│
└── diagnostics/
    ├── DiagnosticsManager.kt     <- Debug diagnostics inspector
    └── DiagnosticsDialog.kt      <- Runtime developer debug overlay
```

## Security Layer
- **Origin Restriction**: The native bridge is bound strictly to `https://appassets.androidplatform.net` and explicitly declared `allowedRemoteOrigins`.
- **Zero Insecure Schemes**: `file://` access is disabled in WebSettings.
- **Renderer Isolation**: Renderer crashes are trapped via `onRenderProcessGone` and presented with a native recovery view instead of terminating the app.

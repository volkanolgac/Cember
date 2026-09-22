package com.example.volkan.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import com.example.volkan.config.AppConfig
import com.example.volkan.util.VolkanLogger

class WebViewAssetLoaderManager(
    private val context: Context,
    private val appConfig: AppConfig
) {
    private val assetLoader: WebViewAssetLoader by lazy {
        val builder = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(context)
            )

        // If custom webAppOrigin domain is specified and not the default androidplatform domain, map it too
        val customUri = try { Uri.parse(appConfig.webAppOrigin) } catch (_: Exception) { null }
        if (customUri != null && customUri.host != null && customUri.host != "appassets.androidplatform.net") {
            builder.setDomain(customUri.host!!)
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        }

        builder.build()
    }

    fun shouldInterceptRequest(uri: Uri): WebResourceResponse? {
        val response = assetLoader.shouldInterceptRequest(uri)
        if (response != null) {
            VolkanLogger.d(VolkanLogger.TAG_WEBVIEW, "Intercepted local asset: $uri")
        }
        return response
    }

    fun getLocalEntryUrl(): String {
        val assetDir = appConfig.webAppAssetDirectory.trim('/')
        val entry = appConfig.webAppEntry.trimStart('/')
        val path = if (assetDir.isNotEmpty()) "$assetDir/$entry" else entry
        return "https://appassets.androidplatform.net/assets/$path"
    }
}

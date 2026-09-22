package com.example.volkan.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.example.volkan.config.AppConfig
import com.example.volkan.config.ExternalLinkPolicy
import com.example.volkan.util.OriginValidator
import com.example.volkan.util.VolkanLogger

sealed class NavigationDecision {
    object AllowInWebView : NavigationDecision()
    object HandledExternally : NavigationDecision()
    object Blocked : NavigationDecision()
}

class NavigationManager(
    private val activity: Activity,
    private val appConfig: AppConfig
) {
    /**
     * Evaluates whether a URL should be loaded in the internal WebView, opened in external browser/app, or blocked.
     */
    fun evaluateUrl(url: String?): NavigationDecision {
        if (url.isNullOrBlank()) return NavigationDecision.Blocked

        val uri = try {
            Uri.parse(url.trim())
        } catch (_: Exception) {
            return NavigationDecision.Blocked
        }

        val scheme = (uri.scheme ?: "").lowercase()

        // 1. Exact origin matching for local assets and trusted origin
        if (OriginValidator.isTrustedAppOrigin(uri, appConfig)) {
            return NavigationDecision.AllowInWebView
        }

        // 2. Exact origin matching for allowed remote origins
        if (appConfig.allowedRemoteOrigins.isNotEmpty()) {
            val isAllowedRemote = appConfig.allowedRemoteOrigins.any { remoteOrigin ->
                val parsedRemote = OriginValidator.parseOrigin(remoteOrigin)
                val parsedTarget = OriginValidator.parseOrigin(uri)
                parsedRemote != null && parsedTarget != null && parsedRemote == parsedTarget
            }
            if (isAllowedRemote) {
                return NavigationDecision.AllowInWebView
            }
        }

        // 3. Handle allowed external custom schemes (tel, mailto, sms, geo, market, intent)
        val schemeWithColon = if (scheme.isNotEmpty()) "$scheme:" else ""
        if (appConfig.allowedExternalSchemes.contains(schemeWithColon)) {
            return launchExternalScheme(uri)
        }

        // 4. Handle HTTP/HTTPS navigation based on configured externalLinkPolicy
        if (scheme == "http" || scheme == "https") {
            if (appConfig.allowExternalNavigation) {
                return NavigationDecision.AllowInWebView
            }

            return when (appConfig.externalLinkPolicy) {
                ExternalLinkPolicy.OPEN_IN_EXTERNAL_BROWSER -> {
                    launchBrowser(uri)
                }
                ExternalLinkPolicy.ALLOW_LIST_ONLY -> {
                    VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "External URL not in allow list: $url")
                    NavigationDecision.Blocked
                }
                ExternalLinkPolicy.BLOCK -> {
                    VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "External navigation blocked by policy: $url")
                    NavigationDecision.Blocked
                }
            }
        }

        VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "Blocked unsupported scheme/URL: $url")
        return NavigationDecision.Blocked
    }

    fun launchBrowser(uri: Uri): NavigationDecision {
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") {
            return NavigationDecision.Blocked
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
            NavigationDecision.HandledExternally
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_NAVIGATION, "Failed to launch browser for $uri: ${e.message}")
            NavigationDecision.Blocked
        }
    }

    private fun launchExternalScheme(uri: Uri): NavigationDecision {
        return try {
            val intent = if (uri.scheme.equals("intent", ignoreCase = true)) {
                // Securely parse intent URI and strip explicit components / selectors
                val parsedIntent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                parsedIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                parsedIntent.component = null
                parsedIntent.selector = null
                parsedIntent
            } else {
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }

            // Verify that an activity exists to resolve this intent
            val resolveInfo = activity.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                activity.startActivity(intent)
                NavigationDecision.HandledExternally
            } else {
                VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "No activity found to handle intent for $uri")
                NavigationDecision.Blocked
            }
        } catch (e: SecurityException) {
            VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "SecurityException launching intent $uri: ${e.message}")
            NavigationDecision.Blocked
        } catch (e: ActivityNotFoundException) {
            VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "ActivityNotFoundException for $uri: ${e.message}")
            NavigationDecision.Blocked
        } catch (e: Exception) {
            VolkanLogger.w(VolkanLogger.TAG_NAVIGATION, "Error launching intent $uri: ${e.message}")
            NavigationDecision.Blocked
        }
    }
}

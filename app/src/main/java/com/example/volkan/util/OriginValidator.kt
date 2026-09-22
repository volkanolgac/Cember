package com.example.volkan.util

import android.net.Uri
import com.example.volkan.config.AppConfig

/**
 * Robust, exact-match origin validation for security decisions.
 * Completely eliminates startsWith/contains partial matching vulnerabilities.
 */
object OriginValidator {

    const val LOCAL_ASSET_DOMAIN = "appassets.androidplatform.net"
    const val LOCAL_ASSET_ORIGIN = "https://appassets.androidplatform.net"

    data class NormalizedOrigin(
        val scheme: String,
        val host: String,
        val port: Int
    ) {
        fun toOriginString(): String {
            val isStandardPort = (scheme == "https" && port == 443) || (scheme == "http" && port == 80)
            return if (isStandardPort) {
                "$scheme://$host"
            } else {
                "$scheme://$host:$port"
            }
        }
    }

    /**
     * Parses and normalizes a URL or origin string into its exact scheme, host, and port.
     * Rejects opaque, null, file, javascript, data, or malformed URIs.
     */
    fun parseOrigin(rawUrlOrOrigin: String?): NormalizedOrigin? {
        if (rawUrlOrOrigin.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(rawUrlOrOrigin.trim())
            parseOrigin(uri)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses a Uri into a NormalizedOrigin.
     */
    fun parseOrigin(uri: Uri?): NormalizedOrigin? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host?.lowercase() ?: return null
        if (host.isBlank()) return null

        val rawPort = uri.port
        val normalizedPort = if (rawPort != -1) {
            rawPort
        } else {
            if (scheme == "https") 443 else 80
        }

        return NormalizedOrigin(scheme, host, normalizedPort)
    }

    /**
     * Checks if the given URL/origin matches the trusted local asset origin exactly.
     */
    fun isTrustedLocalOrigin(urlOrOrigin: String?): Boolean {
        val parsed = parseOrigin(urlOrOrigin) ?: return false
        return parsed.scheme == "https" &&
                parsed.host == LOCAL_ASSET_DOMAIN &&
                parsed.port == 443
    }

    /**
     * Checks if the given Uri matches the trusted local asset origin exactly.
     */
    fun isTrustedLocalOrigin(uri: Uri?): Boolean {
        val parsed = parseOrigin(uri) ?: return false
        return parsed.scheme == "https" &&
                parsed.host == LOCAL_ASSET_DOMAIN &&
                parsed.port == 443
    }

    /**
     * Checks if the given URL/origin matches either the trusted local origin or the configured webAppOrigin exactly.
     */
    fun isTrustedAppOrigin(urlOrOrigin: String?, appConfig: AppConfig): Boolean {
        if (isTrustedLocalOrigin(urlOrOrigin)) return true

        val parsed = parseOrigin(urlOrOrigin) ?: return false
        val configuredAppOrigin = parseOrigin(appConfig.webAppOrigin) ?: return false

        return parsed == configuredAppOrigin
    }

    /**
     * Checks if the given Uri matches either the trusted local origin or the configured webAppOrigin exactly.
     */
    fun isTrustedAppOrigin(uri: Uri?, appConfig: AppConfig): Boolean {
        if (isTrustedLocalOrigin(uri)) return true

        val parsed = parseOrigin(uri) ?: return false
        val configuredAppOrigin = parseOrigin(appConfig.webAppOrigin) ?: return false

        return parsed == configuredAppOrigin
    }

    /**
     * Checks if the given URL/origin matches either trusted app origin OR an exact remote origin in allowedRemoteOrigins.
     */
    fun isOriginAllowed(urlOrOrigin: String?, appConfig: AppConfig): Boolean {
        if (isTrustedAppOrigin(urlOrOrigin, appConfig)) return true

        val parsed = parseOrigin(urlOrOrigin) ?: return false

        return appConfig.allowedRemoteOrigins.any { remoteOriginStr ->
            val remoteParsed = parseOrigin(remoteOriginStr)
            remoteParsed != null && remoteParsed == parsed
        }
    }

    /**
     * Checks if the given Uri matches either trusted app origin OR an exact remote origin in allowedRemoteOrigins.
     */
    fun isOriginAllowed(uri: Uri?, appConfig: AppConfig): Boolean {
        if (isTrustedAppOrigin(uri, appConfig)) return true

        val parsed = parseOrigin(uri) ?: return false

        return appConfig.allowedRemoteOrigins.any { remoteOriginStr ->
            val remoteParsed = parseOrigin(remoteOriginStr)
            remoteParsed != null && remoteParsed == parsed
        }
    }

    /**
     * Returns true if two URL or origin strings have the exact same origin (scheme, host, port).
     */
    fun isSameOrigin(urlOrOriginA: String?, urlOrOriginB: String?): Boolean {
        val a = parseOrigin(urlOrOriginA) ?: return false
        val b = parseOrigin(urlOrOriginB) ?: return false
        return a == b
    }
}

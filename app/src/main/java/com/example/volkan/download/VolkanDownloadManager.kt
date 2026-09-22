package com.example.volkan.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import com.example.volkan.config.AppConfig
import com.example.volkan.util.VolkanLogger
import java.io.File

class VolkanDownloadManager(
    private val context: Context,
    private val appConfig: AppConfig
) {
    fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        if (!appConfig.downloadPolicyEnabled) {
            VolkanLogger.w(VolkanLogger.TAG_DOWNLOAD, "Downloads are disabled by app configuration")
            Toast.makeText(context, "Downloads disabled by app configuration", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val guessedFilename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            // Sanitize filename to prevent any path traversal or invalid characters
            val sanitizedFilename = File(guessedFilename).name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalFilename = sanitizedFilename.ifBlank { "download_${System.currentTimeMillis()}" }

            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: ""
            if (scheme != "http" && scheme != "https" && scheme != "data" && scheme != "blob") {
                VolkanLogger.w(VolkanLogger.TAG_DOWNLOAD, "Unsupported download scheme: $scheme")
                return
            }

            val request = DownloadManager.Request(uri).apply {
                if (mimeType.isNotBlank()) setMimeType(mimeType)
                if (userAgent.isNotBlank()) addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading $finalFilename")
                setTitle(finalFilename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, finalFilename)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            downloadManager?.enqueue(request)
            Toast.makeText(context, "Downloading $finalFilename…", Toast.LENGTH_SHORT).show()
            VolkanLogger.i(VolkanLogger.TAG_DOWNLOAD, "Enqueued download for $finalFilename ($mimeType)")
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_DOWNLOAD, "Failed to initiate download: ${e.message}")
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

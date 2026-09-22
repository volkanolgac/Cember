package com.example.volkan.media

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.volkan.config.AppConfig
import com.example.volkan.util.VolkanLogger

class FileChooserManager(
    private val activity: ComponentActivity,
    private val appConfig: AppConfig
) {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (filePathCallback == null) return@registerForActivityResult

            var results: Array<Uri>? = null
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val dataString = data.dataString
                    val clipData = data.clipData

                    if (clipData != null && clipData.itemCount > 0) {
                        results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }

            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }

    fun handleShowFileChooser(
        callback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        if (!appConfig.uploadPolicyEnabled || !appConfig.filePickerEnabled) {
            VolkanLogger.w(VolkanLogger.TAG_SHELL, "File upload/picker is disabled by app configuration")
            callback.onReceiveValue(null)
            return false
        }

        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback

        val intent = try {
            fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        } catch (_: Exception) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }

        if (fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        return try {
            fileChooserLauncher.launch(intent)
            true
        } catch (e: Exception) {
            VolkanLogger.e(VolkanLogger.TAG_SHELL, "Failed to launch file chooser intent: ${e.message}")
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            false
        }
    }
}

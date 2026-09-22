package com.example.volkan.diagnostics

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import com.example.R

object DiagnosticsDialog {
    fun show(
        context: Context,
        diagnosticsManager: DiagnosticsManager,
        webView: WebView,
        onReload: () -> Unit
    ) {
        val info = diagnosticsManager.collectDiagnostics()

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(dp(context, 20))
        }

        fun addRow(label: String, value: String) {
            val tv = TextView(context).apply {
                text = "$label: $value"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 13f
                setPadding(0, dp(context, 4), 0, dp(context, 4))
            }
            layout.addView(tv)
        }

        val title = TextView(context).apply {
            text = "⚡ Volkan Web2Android Diagnostics"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(context, 12))
        }
        layout.addView(title)

        addRow("App Name", info.appName)
        addRow("Application ID", info.applicationId)
        addRow("Version", "${info.versionName} (${info.versionCode})")
        addRow("Build Type", info.buildType)
        addRow("Android OS", "${info.androidVersion} (API ${info.sdkInt})")
        addRow("WebView Provider", info.webViewPackage)
        addRow("Web App Origin", info.webAppOrigin)
        addRow("Web App Entry", info.webAppEntry)
        addRow("Network State", info.networkState)
        addRow("WebMessageListener", if (info.webMessageListenerSupported) "Supported" else "Fallback Interface")
        addRow("Safe Browsing", if (info.safeBrowsingSupported) "Enabled" else "Not supported")

        val btnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 16), 0, 0)
        }

        val btnReload = Button(context).apply {
            text = "Reload"
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                onReload()
            }
        }
        btnLayout.addView(btnReload)

        val btnClear = Button(context).apply {
            text = "Clear Cache"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                diagnosticsManager.clearWebViewData(webView) {
                    Toast.makeText(context, "WebView Cache Cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnLayout.addView(btnClear)

        layout.addView(btnLayout)
        scrollView.addView(layout)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.diagnostics_title))
            .setView(scrollView)
            .setPositiveButton(context.getString(R.string.btn_close), null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

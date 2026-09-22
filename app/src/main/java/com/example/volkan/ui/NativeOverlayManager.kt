package com.example.volkan.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.example.R

enum class OverlayType {
    NONE,
    OFFLINE,
    LOAD_ERROR,
    RENDERER_CRASH
}

class NativeOverlayManager(
    private val context: Context,
    private val container: FrameLayout,
    private val onRetryClicked: () -> Unit,
    private val onDiagnosticsClicked: () -> Unit
) {
    private var currentOverlayView: View? = null
    var currentType: OverlayType = OverlayType.NONE
        private set

    fun showError(title: String, description: String, showDiagnostics: Boolean = true) {
        currentType = OverlayType.LOAD_ERROR
        val layout = createBaseLayout()

        val titleView = TextView(context).apply {
            text = title
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(titleView)

        val descView = TextView(context).apply {
            text = description
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(24))
        }
        layout.addView(descView)

        val btnRetry = Button(context).apply {
            text = context.getString(R.string.btn_retry)
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                hide()
                onRetryClicked()
            }
        }
        layout.addView(btnRetry)

        if (showDiagnostics) {
            val btnDiag = Button(context).apply {
                text = context.getString(R.string.btn_diagnostics)
                setBackgroundColor(Color.parseColor("#334155"))
                setTextColor(Color.parseColor("#CBD5E1"))
                setOnClickListener {
                    onDiagnosticsClicked()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            }
            layout.addView(btnDiag)
        }

        replaceOverlay(layout)
    }

    fun showRendererCrash() {
        currentType = OverlayType.RENDERER_CRASH
        showError(
            title = context.getString(R.string.error_renderer_title),
            description = context.getString(R.string.error_renderer_desc)
        )
    }

    fun showOfflineError() {
        currentType = OverlayType.OFFLINE
        showError(
            title = context.getString(R.string.error_offline_title),
            description = context.getString(R.string.error_offline_desc)
        )
    }

    fun hide() {
        currentType = OverlayType.NONE
        currentOverlayView?.let {
            container.removeView(it)
            currentOverlayView = null
        }
    }

    private fun replaceOverlay(newView: View) {
        currentOverlayView?.let { container.removeView(it) }
        currentOverlayView = newView
        container.addView(newView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    private fun createBaseLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(dp(24))
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

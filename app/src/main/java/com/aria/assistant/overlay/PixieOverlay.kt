package com.aria.assistant.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * A small floating card — like Alexa/Google Assistant's on-screen
 * response — shown while WakeWordService is listening or replying.
 * Needs the "draw over other apps" permission, which is a special
 * permission granted via Settings (not a runtime dialog).
 */
class PixieOverlay(
    private val context: Context,
    private val onOverlayError: (String) -> Unit = {}
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun isPermitted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun requestPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun show(text: String, autoHideMillis: Long = 4000) {
        if (!isPermitted()) {
            onOverlayError("overlay permission not granted")
            return
        }
        mainHandler.post {
            hideRunnable?.let { mainHandler.removeCallbacks(it) }

            val view = overlayView ?: createView().also {
                overlayView = it
                addToWindow(it)
            }
            view.text = text

            if (autoHideMillis > 0) {
                val runnable = Runnable { hide() }
                hideRunnable = runnable
                mainHandler.postDelayed(runnable, autoHideMillis)
            }
        }
    }

    fun hide() {
        mainHandler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    // Already removed — safe to ignore.
                }
            }
            overlayView = null
        }
    }

    private fun createView(): TextView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 15f
        setPadding(36, 24, 36, 24)
        gravity = Gravity.CENTER
        maxWidth = (context.resources.displayMetrics.widthPixels * 0.85).toInt()
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#E6141422"))
            cornerRadius = 32f
        }
    }

    private fun addToWindow(view: TextView) {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            overlayView = null
            onOverlayError("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

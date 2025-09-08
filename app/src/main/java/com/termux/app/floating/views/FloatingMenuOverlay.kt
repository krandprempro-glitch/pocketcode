package com.termux.app.floating.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.termux.R

class FloatingMenuOverlay(private val context: Context) {

    interface OnMenuSelectListener {
        fun onSSHConnection()
        fun onRunCommand()
        fun onQuickSettings()
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private var view: View? = null
    private var listener: OnMenuSelectListener? = null

    fun setOnMenuSelectListener(l: OnMenuSelectListener) {
        listener = l
    }

    fun show() {
        if (view != null) return
        val v = LayoutInflater.from(context).inflate(R.layout.overlay_floating_menu, null)
        v.findViewById<View>(R.id.menu_ssh).setOnClickListener {
            listener?.onSSHConnection(); hide()
        }
        v.findViewById<View>(R.id.menu_run).setOnClickListener {
            listener?.onRunCommand(); hide()
        }
        v.findViewById<View>(R.id.menu_settings).setOnClickListener {
            listener?.onQuickSettings(); hide()
        }
        v.findViewById<View>(R.id.menu_close).setOnClickListener { hide() }
        view = v
        windowManager.addView(v, layoutParams)
    }

    fun hide() {
        view?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        view = null
    }
}


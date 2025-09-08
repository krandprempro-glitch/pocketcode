package com.termux.app.floating.views

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.termux.R

class OverlayMenuDialog(
    context: Context
) : Dialog(context, R.style.Theme_FloatingDialog) {

    interface OnMenuSelectListener {
        fun onSSHConnection()
        fun onRunCommand()
        fun onQuickSettings()
    }

    private var listener: OnMenuSelectListener? = null

    fun setOnMenuSelectListener(l: OnMenuSelectListener) {
        listener = l
    }

    override fun show() {
        // Configure as overlay window before showing
        window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                w.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                w.setType(WindowManager.LayoutParams.TYPE_PHONE)
            }
            val metrics = context.resources.displayMetrics
            val desiredWidth = (metrics.widthPixels * 0.8f).toInt()
            w.setLayout(desiredWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.CENTER)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.attributes = w.attributes.apply { dimAmount = 0.35f }
        }

        super.show()
    }

    init {
        setContentView(R.layout.overlay_floating_menu)

        findViewById<View>(R.id.row_ssh)?.setOnClickListener {
            listener?.onSSHConnection(); dismiss()
        }
        findViewById<View>(R.id.row_run)?.setOnClickListener {
            listener?.onRunCommand(); dismiss()
        }
        findViewById<View>(R.id.row_settings)?.setOnClickListener {
            listener?.onQuickSettings(); dismiss()
        }
        findViewById<View>(R.id.btn_close)?.setOnClickListener { dismiss() }
    }
}

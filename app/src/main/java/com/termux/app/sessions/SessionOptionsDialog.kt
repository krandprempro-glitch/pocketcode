package com.termux.app.sessions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import com.termux.R
import com.termux.databinding.DialogSessionOptionsBinding

class SessionOptionsDialog(
    context: Context,
    private val session: SessionInfo,
    private val onRename: () -> Unit,
    private val onClose: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSessionOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSessionOptionsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawableResource(R.color.dialog_window_background_light)
            setDimAmount(0.3f)
            clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val metrics = context.resources.displayMetrics
            val width = (metrics.widthPixels * 0.85f).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        binding.sessionTitle.text = session.name

        binding.btnRename.setOnClickListener {
            dismiss()
            onRename()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
            onClose()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }
}

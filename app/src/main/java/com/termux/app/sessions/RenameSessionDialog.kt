package com.termux.app.sessions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import com.termux.R
import com.termux.databinding.DialogRenameSessionBinding

class RenameSessionDialog(
    context: Context,
    private val currentName: String,
    private val onRename: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogRenameSessionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogRenameSessionBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawableResource(R.color.dialog_window_background_light)
            setDimAmount(0.3f)
            clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val metrics = context.resources.displayMetrics
            val width = (metrics.widthPixels * 0.9f).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        binding.sessionNameInput.setText(currentName)
        binding.sessionNameInput.setSelection(currentName.length)

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val newName = binding.sessionNameInput.text.toString().trim()
            if (newName.isNotEmpty() && newName != currentName) {
                onRename(newName)
            }
            dismiss()
        }
    }
}

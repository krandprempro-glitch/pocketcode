package com.termux.app.sessions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.termux.app.databinding.DialogNewSessionBinding
import com.termux.app.models.SSHConfigManager
import com.termux.app.managers.ProjectWorkspaceManager

class NewSessionDialog(
    context: Context,
    private val onSessionCreate: (name: String, sshConfig: String?, path: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogNewSessionBinding
    private var selectedSshConfig: String? = null
    private var selectedPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogNewSessionBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupSshDropdown()
        setupPathDropdown()
        setupButtons()
    }

    private fun setupSshDropdown() {
        val sshConfigs = try {
            SSHConfigManager.getInstance(context).allConfigs.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, sshConfigs)
        binding.sshDropdown.setAdapter(adapter)
        binding.sshDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedSshConfig = sshConfigs[position]
        }
    }

    private fun setupPathDropdown() {
        val paths = try {
            ProjectWorkspaceManager.getInstance(context)
                .allBookmarks
                .map { it.displayName to it.fullPath }
        } catch (e: Exception) {
            emptyList()
        }

        val displayNames = paths.map { it.first }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, displayNames)
        binding.pathDropdown.setAdapter(adapter)
        binding.pathDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedPath = paths[position].second
        }

        // Default to first path if available
        if (paths.isNotEmpty()) {
            selectedPath = paths[0].second
            binding.pathDropdown.setText(paths[0].first, false)
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener {
            val name = binding.sessionNameInput.text?.toString()?.takeIf { it.isNotBlank() }
                ?: selectedPath.substringAfterLast("/").ifEmpty { "终端" }

            onSessionCreate(name, selectedSshConfig, selectedPath)
            dismiss()
        }
    }
}

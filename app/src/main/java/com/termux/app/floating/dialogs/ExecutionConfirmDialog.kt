package com.termux.app.floating.dialogs

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.models.SSHConfigManager
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.models.SSHConnectionConfig
import com.termux.shared.interact.ShareUtils

class ExecutionConfirmDialog(
    context: Context,
    private val configuration: RunConfiguration
) : Dialog(context, R.style.Theme_FloatingDialog) {
    
    private lateinit var tvConfigName: TextView
    private lateinit var tvSSHConnection: TextView
    private lateinit var tvProjectPath: TextView
    private lateinit var tvCommandPreview: TextView
    private lateinit var swKillPrevious: Switch
    private lateinit var swRunInBackground: Switch
    private lateinit var btnExecute: MaterialButton
    private lateinit var btnCopyCommand: MaterialButton
    private lateinit var btnCancel: MaterialButton
    
    private var actionListener: OnExecutionActionListener? = null
    
    interface OnExecutionActionListener {
        fun onExecuteCommand(config: RunConfiguration, command: String)
        fun onCopyCommand(command: String)
    }
    
    init {
        initDialog()
    }
    
    private fun initDialog() {
        setContentView(R.layout.dialog_execution_confirm)
        
        window?.let { window ->
            val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
        
        initViews()
        populateData()
        setupListeners()
    }
    
    private fun initViews() {
        tvConfigName = findViewById(R.id.tv_config_name)
        tvSSHConnection = findViewById(R.id.tv_ssh_connection)
        tvProjectPath = findViewById(R.id.tv_project_path)
        tvCommandPreview = findViewById(R.id.tv_command_preview)
        swKillPrevious = findViewById(R.id.sw_kill_previous)
        swRunInBackground = findViewById(R.id.sw_run_in_background)
        btnExecute = findViewById(R.id.btn_execute)
        btnCopyCommand = findViewById(R.id.btn_copy_command)
        btnCancel = findViewById(R.id.btn_cancel)
    }
    
    private fun populateData() {
        tvConfigName.text = configuration.name
        
        // 获取SSH配置信息
        val sshManager = SSHConfigManager.getInstance(context)
        val sshConfig = sshManager.getConfigByName(configuration.sshConfigId)
        if (sshConfig != null) {
            tvSSHConnection.text = "${sshConfig.name} (${sshConfig.host})"
        }
        
        tvProjectPath.text = configuration.projectPath
        
        // 设置默认选项
        swKillPrevious.isChecked = true
        swRunInBackground.isChecked = configuration.runInBackground
        
        updateCommandPreview()
    }
    
    private fun setupListeners() {
        btnExecute.setOnClickListener { executeCommand() }
        btnCopyCommand.setOnClickListener { copyCommand() }
        btnCancel.setOnClickListener { dismiss() }
        
        // 监听开关变化，更新命令预览
        swKillPrevious.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
        swRunInBackground.setOnCheckedChangeListener { _, _ -> updateCommandPreview() }
    }
    
    private fun updateCommandPreview() {
        val command = generateCommand()
        tvCommandPreview.text = command
    }
    
    private fun generateCommand(): String {
        // 根据当前选项生成完整命令
        val tempConfig = configuration.copy().apply {
            runInBackground = swRunInBackground.isChecked
        }
        
        return if (swKillPrevious.isChecked) {
            CommandBuilder.buildCompleteCommand(tempConfig)
        } else {
            CommandBuilder.buildBackgroundCommand(tempConfig)
        }
    }
    
    private fun executeCommand() {
        val command = generateCommand()
        actionListener?.onExecuteCommand(configuration, command)
        dismiss()
    }
    
    private fun copyCommand() {
        val command = generateCommand()
        ShareUtils.copyTextToClipboard(context, command, "命令已复制到剪贴板")
        
        actionListener?.onCopyCommand(command)
    }
    
    fun setOnExecutionActionListener(listener: OnExecutionActionListener) {
        this.actionListener = listener
    }
}

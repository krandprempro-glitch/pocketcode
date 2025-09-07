package com.termux.app.floating.extensions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.termux.app.MainTabActivity
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.floating.dialogs.ExecutionConfirmDialog
import com.termux.app.floating.dialogs.ExecutionResultDialog
import com.termux.app.floating.dialogs.RunConfigSelectionDialog
import com.termux.app.floating.dialogs.SshConnectionDialog
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.models.SSHConnectionConfig
import com.termux.shared.interact.ShareUtils

/**
 * FloatingActionButton的命令执行扩展功能
 */
class FloatingActionExtensions(private val context: Context) {
    
    private val executionStateManager = ExecutionStateManager.getInstance(context)
    
    /**
     * 处理运行命令操作
     */
    fun handleRunCommandAction() {
        // 显示运行配置选择对话框
        val selectionDialog = RunConfigSelectionDialog(context)
        selectionDialog.setOnConfigSelectedListener(object : RunConfigSelectionDialog.OnConfigSelectedListener {
            override fun onConfigSelected(config: RunConfiguration) {
                showExecutionConfirmDialog(config)
            }
            
            override fun onNewConfigRequested() {
                // 跳转到配置管理页面
                val intent = Intent(context, MainTabActivity::class.java).apply {
                    putExtra("navigate_to", "run_config_new")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        })
        selectionDialog.show()
    }
    
    /**
     * 处理SSH连接操作
     */
    fun handleSSHConnectionAction() {
        val connectionDialog = SshConnectionDialog(context)
        connectionDialog.setOnConnectionActionListener(object : SshConnectionDialog.OnConnectionActionListener {
            override fun onConnectRequested(config: SSHConnectionConfig) {
                // 执行SSH连接
                performSSHConnection(config)
            }
            
            override fun onManageConfigsRequested() {
                // 跳转到SSH配置管理页面
                val intent = Intent(context, MainTabActivity::class.java).apply {
                    putExtra("navigate_to", "ssh_config_manage")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        })
        connectionDialog.show()
    }
    
    /**
     * 处理快捷设置操作
     */
    fun handleQuickSettingsAction() {
        // 跳转到配置页面
        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "configuration_main")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    private fun showExecutionConfirmDialog(config: RunConfiguration) {
        val confirmDialog = ExecutionConfirmDialog(context, config)
        confirmDialog.setOnExecutionActionListener(object : ExecutionConfirmDialog.OnExecutionActionListener {
            override fun onExecuteCommand(config: RunConfiguration, command: String) {
                executeConfiguration(config)
            }
            
            override fun onCopyCommand(command: String) {
                ShareUtils.copyTextToClipboard(context, command, "命令已复制到剪贴板")
            }
        })
        confirmDialog.show()
    }
    
    private fun executeConfiguration(config: RunConfiguration) {
        // 显示执行结果对话框
        val initialResult = ExecutionResult().apply {
            status = ExecutionResult.Status.EXECUTING
        }
        
        val resultDialog = ExecutionResultDialog(context, initialResult)
        resultDialog.setOnResultActionListener(object : ExecutionResultDialog.OnResultActionListener {
            override fun onViewLogRequested(logFilePath: String) {
                // TODO: 实现日志查看功能
                Toast.makeText(context, "查看日志: $logFilePath", Toast.LENGTH_SHORT).show()
            }
            
            override fun onReExecuteRequested() {
                executeConfiguration(config) // 重新执行
            }
        })
        
        // 监听执行状态变化
        val stateListener = object : ExecutionStateManager.OnExecutionStateChangeListener {
            override fun onExecutionStarted(result: ExecutionResult) {
                // 更新对话框显示
                resultDialog.updateResult(result)
            }
            
            override fun onExecutionProgress(message: String, result: ExecutionResult) {
                // 更新进度
                resultDialog.updateResult(result)
            }
            
            override fun onExecutionCompleted(result: ExecutionResult) {
                // 显示最终结果
                resultDialog.updateResult(result)
                
                // 清理监听器
                executionStateManager.removeStateListener(this)
            }
        }
        
        executionStateManager.addStateListener(stateListener)
        resultDialog.show()
        
        // 开始执行
        executionStateManager.executeConfiguration(config)
    }
    
    private fun performSSHConnection(config: SSHConnectionConfig) {
        // 简单的SSH连接提示，实际实现可以根据需求扩展
        Toast.makeText(
            context,
            "正在连接到 ${config.name} (${config.host}:${config.port})",
            Toast.LENGTH_SHORT
        ).show()
        
        // TODO: 实现实际的SSH连接逻辑
        // 可以启动终端会话、打开SSH连接等
    }
}
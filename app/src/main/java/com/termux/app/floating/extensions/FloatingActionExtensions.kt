package com.termux.app.floating.extensions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.*
import com.termux.app.MainTabActivity
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.floating.dialogs.ExecutionResultDialog
import com.termux.app.floating.dialogs.RunConfigSelectionDialog
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.floating.services.RemoteCommandExecutor
import com.termux.shared.logger.Logger

/**
 * FloatingActionButton的命令执行扩展功能
 * 提供完整的命令执行流程
 */
class FloatingActionExtensions(private val context: Context) {
    
    companion object {
        private const val LOG_TAG = "FloatingActionExtensions"
    }
    
    private val executionStateManager = ExecutionStateManager.getInstance(context)
    private val remoteCommandExecutor = RemoteCommandExecutor.getInstance(context)
    private var currentExecutionDialog: ExecutionResultDialog? = null
    
    /**
     * 处理运行命令操作 - 完整执行流程
     */
    fun handleRunCommandAction() {
        Logger.logInfo(LOG_TAG, "Handling run command action")
        
        // 显示运行配置选择对话框
        val selectionDialog = RunConfigSelectionDialog(context).apply {
            setOnConfigSelectedListener(object : RunConfigSelectionDialog.OnConfigSelectedListener {
                override fun onConfigSelected(config: RunConfiguration) {
                    executeRunConfiguration(config)
                }
                
                override fun onNewConfigRequested() {
                    // 直接跳转到新建运行配置页面
                    navigateToNewRunConfigPage()
                }
            })
        }
        
        selectionDialog.show()
    }
    
    /**
     * 执行运行配置
     */
    private fun executeRunConfiguration(config: RunConfiguration) {
        Logger.logInfo(LOG_TAG, "Executing configuration: ${config.name}")
        
        // 创建初始执行结果
        val initialResult = ExecutionResult(
            taskId = "temp_${System.currentTimeMillis()}",
            status = ExecutionResult.Status.EXECUTING,
            executedCommand = config.command,
            startTime = System.currentTimeMillis()
        )
        
        // 显示执行结果对话框
        showExecutionResultDialog(initialResult) { action ->
            when (action) {
                ExecutionAction.RERUN -> executeRunConfiguration(config)
                ExecutionAction.VIEW_LOG -> showLogDialog(initialResult.output)
            }
        }
        
        // 异步执行命令
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = remoteCommandExecutor.executeConfiguration(config)
                
                // 更新对话框显示结果
                withContext(Dispatchers.Main) {
                    currentExecutionDialog?.updateResult(result)
                }
                
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Execution failed: ${e.message}")
                
                val errorResult = initialResult.copy(
                    status = ExecutionResult.Status.ERROR,
                    errorMessage = e.message ?: "Unknown error",
                    endTime = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.Main) {
                    currentExecutionDialog?.updateResult(errorResult)
                }
            }
        }
    }
    
    /**
     * 显示执行结果对话框
     */
    private fun showExecutionResultDialog(
        result: ExecutionResult,
        onAction: (ExecutionAction) -> Unit
    ) {
        currentExecutionDialog?.dismiss()
        
        currentExecutionDialog = ExecutionResultDialog(context, result).apply {
            setOnResultActionListener(object : ExecutionResultDialog.OnResultActionListener {
                override fun onViewLogRequested(output: String) {
                    onAction(ExecutionAction.VIEW_LOG)
                }
                
                override fun onReExecuteRequested() {
                    onAction(ExecutionAction.RERUN)
                }
            })
            
            show()
        }
    }
    
    /**
     * 显示日志对话框
     */
    private fun showLogDialog(output: String) {
        // 创建一个简单的文本显示对话框来显示日志
        val logDialog = android.app.AlertDialog.Builder(context)
            .setTitle("命令输出")
            .setMessage(output)
            .setPositiveButton("关闭", null)
            .create()
        
        logDialog.show()
        
        // 设置对话框大小
        logDialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.7).toInt()
        )
    }
    
    /**
     * 处理SSH连接操作
     */
    fun handleSSHConnectionAction() {
        Logger.logInfo(LOG_TAG, "Handling SSH connection action")
        
        // 跳转到SSH连接页面
        Toast.makeText(context, "正在打开SSH连接页面...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "ssh_connection")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理快捷设置操作
     */
    fun handleQuickSettingsAction() {
        Logger.logInfo(LOG_TAG, "Handling quick settings action")
        
        navigateToConfigurationPage()
    }
    
    /**
     * 导航到新建运行配置页面
     */
    private fun navigateToNewRunConfigPage() {
        val intent = com.termux.app.configuration.activities.RunConfigDetailActivity.newIntent(context, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 导航到配置页面
     */
    private fun navigateToConfigurationPage() {
        Toast.makeText(context, "正在打开配置页面...", Toast.LENGTH_SHORT).show()

        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "configuration")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        currentExecutionDialog?.dismiss()
        currentExecutionDialog = null
    }
    
    /**
     * 执行操作类型
     */
    private enum class ExecutionAction {
        RERUN, VIEW_LOG
    }
}

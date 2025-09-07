package com.termux.app.floating.managers

import android.content.Context
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.models.SSHConfigManager

import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.floating.services.RemoteCommandService
import com.termux.app.models.SSHConnectionConfig
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ExecutionStateManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ExecutionStateManager? = null
        
        fun getInstance(context: Context): ExecutionStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExecutionStateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val context: Context = context.applicationContext
    private val commandService = RemoteCommandService()
    private val executionHistory = ConcurrentHashMap<String, ExecutionResult>()
    private val stateListeners = mutableListOf<OnExecutionStateChangeListener>()
    
    interface OnExecutionStateChangeListener {
        fun onExecutionStarted(result: ExecutionResult)
        fun onExecutionProgress(message: String, result: ExecutionResult)
        fun onExecutionCompleted(result: ExecutionResult)
    }
    
    /**
     * 执行运行配置
     */
    fun executeConfiguration(config: RunConfiguration) {
        // 获取SSH配置
        val sshManager = SSHConfigManager.getInstance(context)
        val sshConfig = sshManager.getConfigByName(config.sshConfigId)
        
        if (sshConfig == null) {
            notifyExecutionError("找不到对应的SSH配置")
            return
        }
        
        // 生成完整命令
        val command = CommandBuilder.buildCompleteCommand(config)
        
        // 创建执行结果对象
        val result = ExecutionResult().apply {
            executedCommand = command
        }
        
        // 通知开始执行
        notifyExecutionStarted(result)
        
        // 异步执行命令
        val future = commandService.executeCommand(
            sshConfig, command, object : RemoteCommandService.OnExecutionProgressListener {
                override fun onProgress(message: String, currentResult: ExecutionResult) {
                    notifyExecutionProgress(message, currentResult)
                }
                
                override fun onCompleted(completedResult: ExecutionResult) {
                    // 更新配置的最后使用时间
                    updateConfigLastUsed(config)
                    
                    // 保存执行历史
                    executionHistory[completedResult.taskId] = completedResult
                    
                    // 通知执行完成
                    notifyExecutionCompleted(completedResult)
                }
            })
        
        // 保存任务引用
        executionHistory[result.taskId] = result
    }
    
    /**
     * 执行自定义命令
     */
    fun executeCustomCommand(sshConfig: SSHConnectionConfig, command: String) {
        val result = ExecutionResult().apply {
            executedCommand = command
        }
        
        notifyExecutionStarted(result)
        
        val future = commandService.executeCommand(
            sshConfig, command, object : RemoteCommandService.OnExecutionProgressListener {
                override fun onProgress(message: String, currentResult: ExecutionResult) {
                    notifyExecutionProgress(message, currentResult)
                }
                
                override fun onCompleted(completedResult: ExecutionResult) {
                    executionHistory[completedResult.taskId] = completedResult
                    notifyExecutionCompleted(completedResult)
                }
            })
        
        executionHistory[result.taskId] = result
    }
    
    private fun updateConfigLastUsed(config: RunConfiguration) {
        val configManager = RunConfigurationManager.getInstance(context)
        configManager.updateLastUsed(config.id)
    }
    
    private fun notifyExecutionStarted(result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                listener.onExecutionStarted(result)
            }
        }
    }
    
    private fun notifyExecutionProgress(message: String, result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                listener.onExecutionProgress(message, result)
            }
        }
    }
    
    private fun notifyExecutionCompleted(result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                listener.onExecutionCompleted(result)
            }
        }
    }
    
    private fun notifyExecutionError(errorMessage: String) {
        val result = ExecutionResult().apply {
            status = ExecutionResult.Status.ERROR
            this.errorMessage = errorMessage
            endTime = System.currentTimeMillis()
        }
        
        notifyExecutionCompleted(result)
    }
    
    /**
     * 获取执行历史
     */
    fun getExecutionHistory(): List<ExecutionResult> {
        return ArrayList(executionHistory.values)
    }
    
    /**
     * 清理执行历史
     */
    fun clearHistory() {
        executionHistory.clear()
    }
    
    /**
     * 添加状态监听器
     */
    fun addStateListener(listener: OnExecutionStateChangeListener) {
        synchronized(stateListeners) {
            if (!stateListeners.contains(listener)) {
                stateListeners.add(listener)
            }
        }
    }
    
    /**
     * 移除状态监听器
     */
    fun removeStateListener(listener: OnExecutionStateChangeListener) {
        synchronized(stateListeners) {
            stateListeners.remove(listener)
        }
    }
    
    /**
     * 获取正在运行的任务数量
     */
    fun getRunningTaskCount(): Int = commandService.getRunningTaskCount()
    
    fun shutdown() {
        commandService.shutdown()
    }
}

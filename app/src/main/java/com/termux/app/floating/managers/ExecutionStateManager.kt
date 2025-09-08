package com.termux.app.floating.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.models.SSHConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.floating.services.RemoteCommandExecutor
import com.termux.app.models.SSHConnectionConfig
import com.termux.shared.logger.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ExecutionStateManager private constructor(context: Context) {
    
    companion object {
        private const val LOG_TAG = "ExecutionStateManager"
        private const val PREFS_NAME = "execution_state"
        private const val KEY_EXECUTION_HISTORY = "execution_history"
        private const val MAX_HISTORY_SIZE = 100
        
        @Volatile
        private var INSTANCE: ExecutionStateManager? = null
        
        fun getInstance(context: Context): ExecutionStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExecutionStateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 执行历史和状态
    private val executionHistory = ConcurrentHashMap<String, ExecutionResult>()
    private val currentExecutions = ConcurrentHashMap<String, ExecutionResult>()
    
    // 状态流
    private val _executionStateFlow = MutableStateFlow<Map<String, ExecutionResult>>(emptyMap())
    val executionStateFlow: StateFlow<Map<String, ExecutionResult>> = _executionStateFlow.asStateFlow()
    
    // 监听器
    private val stateListeners = mutableListOf<OnExecutionStateChangeListener>()
    
    interface OnExecutionStateChangeListener {
        fun onExecutionStarted(result: ExecutionResult)
        fun onExecutionProgress(result: ExecutionResult)
        fun onExecutionCompleted(result: ExecutionResult)
        fun onExecutionStateChanged(executionId: String, status: ExecutionResult.Status)
    }
    
    init {
        loadExecutionHistory()
    }
    
    /**
     * 更新执行状态
     */
    fun updateExecutionState(executionId: String, status: ExecutionResult.Status) {
        val currentResult = currentExecutions[executionId]
        if (currentResult != null) {
            val updatedResult = currentResult.copy(status = status)
            currentExecutions[executionId] = updatedResult
            updateStateFlow()
            
            notifyStateChanged(executionId, status)
        }
    }
    
    /**
     * 保存执行结果
     */
    fun saveExecutionResult(executionId: String, result: ExecutionResult) {
        // 保存到当前执行
        currentExecutions[executionId] = result
        
        // 如果执行完成，移动到历史记录
        if (isStatusFinished(result.status)) {
            currentExecutions.remove(executionId)
            executionHistory[executionId] = result
            
            // 限制历史记录大小
            trimExecutionHistory()
            
            // 持久化历史记录
            saveExecutionHistory()
        }
        
        updateStateFlow()
        
        when (result.status) {
            ExecutionResult.Status.EXECUTING,
            ExecutionResult.Status.RUNNING -> notifyExecutionStarted(result)
            ExecutionResult.Status.SUCCESS, 
            ExecutionResult.Status.ERROR,
            ExecutionResult.Status.FAILED, 
            ExecutionResult.Status.TIMEOUT,
            ExecutionResult.Status.CANCELLED -> notifyExecutionCompleted(result)
        }
    }
    
    /**
     * 更新实时输出
     */
    fun updateLiveOutput(executionId: String, result: ExecutionResult) {
        currentExecutions[executionId] = result
        updateStateFlow()
        notifyExecutionProgress(result)
    }
    
    /**
     * 取消执行
     */
    fun cancelExecution(executionId: String): Boolean {
        val executor = RemoteCommandExecutor.getInstance(context)
        return executor.cancelExecution(executionId)
    }
    
    /**
     * 获取当前执行的任务
     */
    fun getCurrentExecutions(): Map<String, ExecutionResult> {
        return currentExecutions.toMap()
    }
    
    /**
     * 获取执行历史
     */
    fun getExecutionHistory(): List<ExecutionResult> {
        return executionHistory.values.sortedByDescending { it.startTime }
    }
    
    /**
     * 获取特定执行的结果
     */
    fun getExecutionResult(executionId: String): ExecutionResult? {
        return currentExecutions[executionId] ?: executionHistory[executionId]
    }
    
    /**
     * 清理执行历史
     */
    fun clearHistory() {
        executionHistory.clear()
        saveExecutionHistory()
    }
    
    /**
     * 加载执行历史
     */
    private fun loadExecutionHistory() {
        try {
            val historyJson = preferences.getString(KEY_EXECUTION_HISTORY, null)
            if (historyJson != null) {
                val type = object : TypeToken<Map<String, ExecutionResult>>() {}.type
                val history: Map<String, ExecutionResult> = gson.fromJson(historyJson, type)
                executionHistory.putAll(history)
                Logger.logInfo(LOG_TAG, "Loaded ${history.size} execution history records")
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load execution history: ${e.message}")
        }
    }
    
    /**
     * 保存执行历史
     */
    private fun saveExecutionHistory() {
        try {
            val historyJson = gson.toJson(executionHistory.toMap())
            preferences.edit()
                .putString(KEY_EXECUTION_HISTORY, historyJson)
                .apply()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to save execution history: ${e.message}")
        }
    }
    
    /**
     * 限制历史记录大小
     */
    private fun trimExecutionHistory() {
        if (executionHistory.size > MAX_HISTORY_SIZE) {
            val sortedEntries = executionHistory.entries
                .sortedByDescending { it.value.startTime }
                .take(MAX_HISTORY_SIZE)
            
            executionHistory.clear()
            sortedEntries.forEach { (key, value) ->
                executionHistory[key] = value
            }
        }
    }
    
    /**
     * 更新状态流
     */
    private fun updateStateFlow() {
        val allExecutions = mutableMapOf<String, ExecutionResult>()
        allExecutions.putAll(currentExecutions)
        allExecutions.putAll(executionHistory)
        _executionStateFlow.value = allExecutions.toMap()
    }
    
    /**
     * 通知监听器
     */
    private fun notifyExecutionStarted(result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                try {
                    listener.onExecutionStarted(result)
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error in execution started listener: ${e.message}")
                }
            }
        }
    }
    
    private fun notifyExecutionProgress(result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                try {
                    listener.onExecutionProgress(result)
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error in execution progress listener: ${e.message}")
                }
            }
        }
    }
    
    private fun notifyExecutionCompleted(result: ExecutionResult) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                try {
                    listener.onExecutionCompleted(result)
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error in execution completed listener: ${e.message}")
                }
            }
        }
    }
    
    private fun notifyStateChanged(executionId: String, status: ExecutionResult.Status) {
        synchronized(stateListeners) {
            stateListeners.forEach { listener ->
                try {
                    listener.onExecutionStateChanged(executionId, status)
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error in state change listener: ${e.message}")
                }
            }
        }
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
    fun getRunningTaskCount(): Int {
        return currentExecutions.count { 
            it.value.status == ExecutionResult.Status.EXECUTING || 
            it.value.status == ExecutionResult.Status.RUNNING 
        }
    }
    
    /**
     * 获取活跃的执行ID列表
     */
    fun getActiveExecutionIds(): List<String> {
        return currentExecutions.keys.toList()
    }
    
    /**
     * 清理资源
     */
    fun shutdown() {
        val executor = RemoteCommandExecutor.getInstance(context)
        executor.cleanup()
        
        // 保存当前状态
        saveExecutionHistory()
        
        // 清理监听器
        synchronized(stateListeners) {
            stateListeners.clear()
        }
    }
    
    // 辅助方法：检查状态是否已完成
    private fun isStatusFinished(status: ExecutionResult.Status): Boolean {
        return status in listOf(
            ExecutionResult.Status.SUCCESS, 
            ExecutionResult.Status.ERROR, 
            ExecutionResult.Status.FAILED, 
            ExecutionResult.Status.TIMEOUT, 
            ExecutionResult.Status.CANCELLED
        )
    }
}

package com.termux.app.bridge.managers

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.PathBookmarkHelper
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.managers.FloatingWindowManager
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.models.DirectoryBookmark
import com.termux.app.models.SSHConfigManager
import com.termux.shared.logger.Logger
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Configuration和Floating模块之间的桥接管理器
 * 负责数据同步、状态一致性和配置管理
 */
class ConfigFloatingBridge private constructor(private val context: Context) {
    
    companion object {
        @JvmStatic
        private var instance: ConfigFloatingBridge? = null
        
        @JvmStatic
        fun getInstance(context: Context): ConfigFloatingBridge {
            return instance ?: synchronized(this) {
                instance ?: ConfigFloatingBridge(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val configManager: RunConfigurationManager = RunConfigurationManager.getInstance(context)
    private val executionManager: ExecutionStateManager = ExecutionStateManager.getInstance(context)
    private val floatingManager: FloatingWindowManager = FloatingWindowManager.getInstance(context)
    private val dataChangeListeners = mutableListOf<OnDataChangeListener>()
    
    interface OnDataChangeListener {
        fun onRunConfigChanged()
        fun onSSHConfigChanged() 
        fun onExecutionStateChanged()
    }
    
    init {
        setupDataSyncListeners()
    }
    
    private fun setupDataSyncListeners() {
        // TODO: 实现数据同步监听器，当相关管理器支持监听器时
        Logger.logInfo("ConfigFloatingBridge", "Data sync listeners setup completed")
        
        // TODO: 监听执行状态变化，当ExecutionStateManager支持监听器时
    }
    
    /**
     * 获取最近使用的运行配置
     */
    fun getRecentRunConfigs(limit: Int): List<RunConfiguration> {
        return configManager.getRecentConfigurations(limit)
    }
    
    /**
     * 获取按SSH分组的运行配置
     */
    fun getConfigsBySSHGroup(): Map<String, List<RunConfiguration>> {
        val allConfigs = configManager.getAllConfigurations()
        val groupedConfigs = HashMap<String, MutableList<RunConfiguration>>()
        
        for (config in allConfigs) {
            val sshConfigId = config.sshConfigId
            if (!groupedConfigs.containsKey(sshConfigId)) {
                groupedConfigs[sshConfigId] = ArrayList()
            }
            groupedConfigs[sshConfigId]?.add(config)
        }
        
        return groupedConfigs
    }
    
    /**
     * 验证运行配置的完整性
     */
    fun validateRunConfiguration(config: RunConfiguration): ValidationResult {
        val result = ValidationResult()
        
        // 检查SSH配置是否存在
        val sshManager = SSHConfigManager.getInstance(context)
        val sshConfig = sshManager.getConfigByName(config.sshConfigId)
        if (sshConfig == null) {
            result.addError("SSH配置不存在或已被删除")
        }
        
        // 检查项目路径是否仍在收藏中
        val bookmarks = PathBookmarkHelper.getBookmarksBySSHConfig(context, config.sshConfigId)
        val pathExists = bookmarks.any { bookmark -> 
            bookmark.fullPath == config.projectPath 
        }
            
        if (!pathExists) {
            result.addWarning("项目路径可能已从收藏中移除")
        }
        
        // 检查命令格式
        if (TextUtils.isEmpty(config.command)) {
            result.addError("运行命令不能为空")
        }
        
        return result
    }
    
    /**
     * 清理无效的运行配置
     */
    fun cleanupInvalidConfigurations(): Int {
        val allConfigs = configManager.getAllConfigurations()
        var cleanedCount = 0
        
        for (config in allConfigs) {
            val validation = validateRunConfiguration(config)
            if (validation.hasErrors()) {
                configManager.deleteConfiguration(config.id)
                cleanedCount++
            }
        }
        
        return cleanedCount
    }
    
    /**
     * 导出配置数据
     */
    fun exportConfigurations(): String? {
        return try {
            val exportData = HashMap<String, Any>()
            
            // 导出运行配置
            val runConfigs = configManager.getAllConfigurations()
            exportData["runConfigurations"] = runConfigs
            
            // 导出SSH配置
            val sshManager = SSHConfigManager.getInstance(context)
            val sshConfigs = sshManager.getAllConfigs()
            exportData["sshConfigurations"] = sshConfigs
            
            // 导出设置
            val settings = HashMap<String, Any>()
            settings["floatingEnabled"] = floatingManager.isFloatingEnabled()
            exportData["settings"] = settings
            
            // 添加导出信息
            val metadata = HashMap<String, Any>()
            metadata["exportTime"] = System.currentTimeMillis()
            metadata["version"] = "1.0"
            exportData["metadata"] = metadata
            
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(exportData)
            
        } catch (e: Exception) {
            Logger.logError("ConfigFloatingBridge",  e.toString())
            null
        }
    }
    
    /**
     * 导入配置数据
     */
    fun importConfigurations(jsonData: String): ImportResult {
        val result = ImportResult()
        
        try {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any>>(){}.type
            val importData: Map<String, Any> = gson.fromJson(jsonData, mapType)
            
            // 导入运行配置
            if (importData.containsKey("runConfigurations")) {
                @Suppress("UNCHECKED_CAST")
                val runConfigMaps = importData["runConfigurations"] as? List<Map<String, Any>>
                    
                runConfigMaps?.forEach { configMap ->
                    try {
                        val config = gson.fromJson(
                            gson.toJson(configMap), RunConfiguration::class.java)
                        configManager.saveConfiguration(config)
                        result.incrementSuccessCount()
                    } catch (e: Exception) {
                        result.addError("导入运行配置失败: ${e.message}")
                    }
                }
            }
            
            // 导入SSH配置
            if (importData.containsKey("sshConfigurations")) {
                @Suppress("UNCHECKED_CAST")
                val sshConfigMaps = importData["sshConfigurations"] as? List<Map<String, Any>>
                    
                val sshManager = SSHConfigManager.getInstance(context)
                sshConfigMaps?.forEach { configMap ->
                    try {
                        val config = gson.fromJson(
                            gson.toJson(configMap), SSHConnectionConfig::class.java)
                        sshManager.saveConfig(config)
                        result.incrementSuccessCount()
                    } catch (e: Exception) {
                        result.addError("导入SSH配置失败: ${e.message}")
                    }
                }
            }
            
            // 导入设置
            if (importData.containsKey("settings")) {
                @Suppress("UNCHECKED_CAST")
                val settings = importData["settings"] as? Map<String, Any>
                
                settings?.get("floatingEnabled")?.let { floatingEnabled ->
                    if (floatingEnabled as Boolean) {
                        floatingManager.enableFloating()
                    }
                }
            }
            
        } catch (e: Exception) {
            result.addError("导入数据格式错误: ${e.message}")
            Logger.logError("ConfigFloatingBridge",  e.toString())
        }
        
        return result
    }
    
    private fun notifyDataChanged(type: DataChangeType) {
        for (listener in dataChangeListeners) {
            when (type) {
                DataChangeType.RUN_CONFIG -> listener.onRunConfigChanged()
                DataChangeType.SSH_CONFIG -> listener.onSSHConfigChanged()
                DataChangeType.EXECUTION_STATE -> listener.onExecutionStateChanged()
            }
        }
    }
    
    fun addDataChangeListener(listener: OnDataChangeListener) {
        if (!dataChangeListeners.contains(listener)) {
            dataChangeListeners.add(listener)
        }
    }
    
    fun removeDataChangeListener(listener: OnDataChangeListener) {
        dataChangeListeners.remove(listener)
    }
    
    private enum class DataChangeType {
        RUN_CONFIG, SSH_CONFIG, EXECUTION_STATE
    }
    
    class ValidationResult {
        private val errors = mutableListOf<String>()
        private val warnings = mutableListOf<String>()
        
        fun addError(error: String) { errors.add(error) }
        fun addWarning(warning: String) { warnings.add(warning) }
        
        fun hasErrors(): Boolean = errors.isNotEmpty()
        fun hasWarnings(): Boolean = warnings.isNotEmpty()
        
        fun getErrors(): List<String> = errors
        fun getWarnings(): List<String> = warnings
    }
    
    class ImportResult {
        private var successCount = 0
        private val errors = mutableListOf<String>()
        
        fun incrementSuccessCount() { successCount++ }
        fun addError(error: String) { errors.add(error) }
        
        fun getSuccessCount(): Int = successCount
        fun getErrors(): List<String> = errors
        fun hasErrors(): Boolean = errors.isNotEmpty()
    }
}

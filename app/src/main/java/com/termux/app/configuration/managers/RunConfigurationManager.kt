package com.termux.app.configuration.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.ConfigurationConstants

class RunConfigurationManager private constructor(context: Context) {
    
    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences = context.getSharedPreferences(
        ConfigurationConstants.PREFS_RUN_CONFIG, 
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        @Volatile
        private var INSTANCE: RunConfigurationManager? = null
        
        fun getInstance(context: Context): RunConfigurationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RunConfigurationManager(context).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 保存配置
     */
    fun saveConfiguration(config: RunConfiguration): Boolean {
        return try {
            // 如果ID为空，生成新ID
            if (config.id.isBlank()) {
                config.id = config.generateUniqueId()
            }
            
            val json = gson.toJson(config)
            preferences.edit()
                .putString("${ConfigurationConstants.KEY_RUN_CONFIG_PREFIX}${config.id}", json)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取配置
     */
    fun getConfiguration(id: String): RunConfiguration? {
        return try {
            val json = preferences.getString("${ConfigurationConstants.KEY_RUN_CONFIG_PREFIX}$id", null)
            json?.let { gson.fromJson(it, RunConfiguration::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取所有配置
     */
    fun getAllConfigurations(): List<RunConfiguration> {
        val configs = mutableListOf<RunConfiguration>()
        try {
            val allEntries = preferences.all
            for ((key, value) in allEntries) {
                if (key.startsWith(ConfigurationConstants.KEY_RUN_CONFIG_PREFIX) && value is String) {
                    val config = gson.fromJson(value, RunConfiguration::class.java)
                    config?.let { configs.add(it) }
                }
            }
            // 按最后使用时间排序
            configs.sortByDescending { it.lastUsedTime }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return configs
    }
    
    /**
     * 删除配置
     */
    fun deleteConfiguration(id: String): Boolean {
        return try {
            preferences.edit()
                .remove("${ConfigurationConstants.KEY_RUN_CONFIG_PREFIX}$id")
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 按SSH配置分组
     */
    fun getConfigurationsBySSH(sshConfigId: String): List<RunConfiguration> {
        return getAllConfigurations().filter { it.sshConfigId == sshConfigId }
    }
    
    /**
     * 获取最近使用的配置
     */
    fun getRecentConfigurations(limit: Int = ConfigurationConstants.MAX_RECENT_CONFIGS): List<RunConfiguration> {
        return getAllConfigurations().take(limit)
    }
    
    /**
     * 更新最后使用时间
     */
    fun updateLastUsed(configId: String) {
        val config = getConfiguration(configId)
        config?.let {
            it.updateLastUsed()
            saveConfiguration(it)
            
            // 记录最后使用的配置ID
            preferences.edit()
                .putString(ConfigurationConstants.KEY_LAST_USED, configId)
                .apply()
        }
    }
    
    /**
     * 获取最后使用的配置
     */
    fun getLastUsedConfiguration(): RunConfiguration? {
        val lastUsedId = preferences.getString(ConfigurationConstants.KEY_LAST_USED, null)
        return lastUsedId?.let { getConfiguration(it) }
    }
    
    /**
     * 检查名称是否存在
     */
    fun isNameExists(name: String, excludeId: String? = null): Boolean {
        return getAllConfigurations().any { 
            it.name == name && it.id != excludeId
        }
    }
    
    /**
     * 获取配置数量
     */
    fun getConfigurationCount(): Int {
        return getAllConfigurations().size
    }
    
    /**
     * 清除所有配置
     */
    fun clearAllConfigurations(): Boolean {
        return try {
            val editor = preferences.edit()
            val allEntries = preferences.all
            for (key in allEntries.keys) {
                if (key.startsWith(ConfigurationConstants.KEY_RUN_CONFIG_PREFIX)) {
                    editor.remove(key)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 导出配置为JSON
     */
    fun exportConfigurations(): String? {
        return try {
            val configs = getAllConfigurations()
            gson.toJson(configs)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从JSON导入配置
     */
    fun importConfigurations(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<RunConfiguration>>() {}.type
            val configs: List<RunConfiguration> = gson.fromJson(json, type)
            
            for (config in configs) {
                // 为导入的配置生成新ID避免冲突
                config.id = config.generateUniqueId()
                saveConfiguration(config)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
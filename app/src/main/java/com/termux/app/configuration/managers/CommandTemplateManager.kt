package com.termux.app.configuration.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.app.configuration.models.CommandTemplate
import com.termux.app.configuration.models.LanguageType
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.ConfigurationConstants

class CommandTemplateManager private constructor(context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        ConfigurationConstants.PREFS_COMMAND_TEMPLATES,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val builtInTemplates = CommandTemplate.BUILTIN_TEMPLATES.toList()
    
    companion object {
        @Volatile
        private var INSTANCE: CommandTemplateManager? = null
        
        fun getInstance(context: Context): CommandTemplateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommandTemplateManager(context).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 按语言获取模板
     */
    fun getTemplatesByLanguage(language: LanguageType): List<CommandTemplate> {
        val builtIn = builtInTemplates.filter { it.languageType == language }
        val custom = getCustomTemplatesByLanguage(language)
        return builtIn + custom
    }
    
    /**
     * 保存自定义模板
     */
    fun saveCustomTemplate(template: CommandTemplate): Boolean {
        return try {
            val customTemplate = template.copy(isBuiltIn = false)
            val json = gson.toJson(customTemplate)
            preferences.edit()
                .putString("${ConfigurationConstants.KEY_TEMPLATE_PREFIX}${template.id}", json)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取所有模板
     */
    fun getAllTemplates(): List<CommandTemplate> {
        return builtInTemplates + getCustomTemplates()
    }
    
    /**
     * 获取自定义模板
     */
    private fun getCustomTemplates(): List<CommandTemplate> {
        val templates = mutableListOf<CommandTemplate>()
        try {
            val allEntries = preferences.all
            for ((key, value) in allEntries) {
                if (key.startsWith(ConfigurationConstants.KEY_TEMPLATE_PREFIX) && value is String) {
                    val template = gson.fromJson(value, CommandTemplate::class.java)
                    template?.let { templates.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return templates
    }
    
    /**
     * 按语言获取自定义模板
     */
    private fun getCustomTemplatesByLanguage(language: LanguageType): List<CommandTemplate> {
        return getCustomTemplates().filter { it.languageType == language }
    }
    
    /**
     * 删除自定义模板
     */
    fun deleteCustomTemplate(templateId: String): Boolean {
        // 不允许删除内置模板
        if (builtInTemplates.any { it.id == templateId }) {
            return false
        }
        
        return try {
            preferences.edit()
                .remove("${ConfigurationConstants.KEY_TEMPLATE_PREFIX}$templateId")
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取模板
     */
    fun getTemplate(templateId: String): CommandTemplate? {
        // 先查找内置模板
        builtInTemplates.find { it.id == templateId }?.let { return it }
        
        // 再查找自定义模板
        return try {
            val json = preferences.getString("${ConfigurationConstants.KEY_TEMPLATE_PREFIX}$templateId", null)
            json?.let { gson.fromJson(it, CommandTemplate::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 生成命令
     */
    fun generateCommand(template: CommandTemplate, config: RunConfiguration): String {
        var command = template.commandPattern
        
        // 替换默认参数
        for ((key, value) in template.defaultParams) {
            command = command.replace("{$key}", value)
        }
        
        // 替换配置相关的参数
        command = command.replace("{workingDir}", config.workingDir)
        command = command.replace("{projectPath}", config.projectPath)
        
        return command
    }
    
    /**
     * 获取语言的常用命令
     */
    fun getCommonCommands(language: LanguageType): List<String> {
        return language.commonCommands.toList()
    }
    
    /**
     * 根据命令推荐模板
     */
    fun suggestTemplate(command: String): CommandTemplate? {
        return getAllTemplates().find { template ->
            command.contains(template.commandPattern) || 
            template.commandPattern.contains(command.trim())
        }
    }
    
    /**
     * 检查模板名称是否存在
     */
    fun isTemplateNameExists(name: String, excludeId: String? = null): Boolean {
        return getAllTemplates().any { 
            it.name == name && it.id != excludeId
        }
    }
    
    /**
     * 获取推荐的语言类型
     */
    fun suggestLanguageType(command: String): LanguageType {
        return when {
            command.contains("npm") || command.contains("yarn") || command.contains("pnpm") || 
            command.contains("node") || command.contains("next") -> LanguageType.NODEJS
            
            command.contains("python") || command.contains("pip") || command.contains("django") || 
            command.contains("flask") || command.contains("manage.py") -> LanguageType.PYTHON
            
            command.contains("java") || command.contains("mvn") || command.contains("gradle") || 
            command.contains(".jar") -> LanguageType.JAVA
            
            command.contains("go run") || command.contains("go build") -> LanguageType.GO
            
            else -> LanguageType.CUSTOM
        }
    }
    
    /**
     * 导出自定义模板
     */
    fun exportCustomTemplates(): String? {
        return try {
            val customTemplates = getCustomTemplates()
            gson.toJson(customTemplates)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 导入自定义模板
     */
    fun importCustomTemplates(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<CommandTemplate>>() {}.type
            val templates: List<CommandTemplate> = gson.fromJson(json, type)
            
            for (template in templates) {
                saveCustomTemplate(template)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
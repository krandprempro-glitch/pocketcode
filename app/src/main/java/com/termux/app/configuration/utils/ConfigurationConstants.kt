package com.termux.app.configuration.utils

import com.termux.app.configuration.models.LanguageType

object ConfigurationConstants {
    
    // SharedPreferences相关
    const val PREFS_RUN_CONFIG = "run_configurations"
    const val PREFS_COMMAND_TEMPLATES = "command_templates"
    const val PREFS_CONFIG_SETTINGS = "config_settings"
    
    // 配置项键值
    const val KEY_RUN_CONFIG_PREFIX = "run_config_"
    const val KEY_TEMPLATE_PREFIX = "template_"
    const val KEY_LAST_USED = "last_used_config"
    
    // 默认值
    const val DEFAULT_LOG_FILE = "app.log"
    const val DEFAULT_WORKING_DIR = "."
    const val MAX_RECENT_CONFIGS = 10
    
    // 请求码
    const val REQUEST_CODE_SSH_CONFIG = 1001
    const val REQUEST_CODE_RUN_CONFIG = 1002
    
    // Bundle键值
    const val BUNDLE_CONFIG_ID = "config_id"
    const val BUNDLE_SSH_CONFIG_ID = "ssh_config_id"
    const val BUNDLE_IS_EDIT_MODE = "is_edit_mode"
    
    // 语言类型映射
    val LANGUAGE_MAP = mapOf(
        "js" to LanguageType.NODEJS,
        "ts" to LanguageType.NODEJS,
        "vue" to LanguageType.NODEJS,
        "jsx" to LanguageType.NODEJS,
        "tsx" to LanguageType.NODEJS,
        "py" to LanguageType.PYTHON,
        "java" to LanguageType.JAVA,
        "kt" to LanguageType.JAVA, // Kotlin项目通常使用Java工具链
        "go" to LanguageType.GO
    )
    
    // 文件扩展名到语言类型的映射
    fun getLanguageByExtension(extension: String): LanguageType {
        return LANGUAGE_MAP[extension.lowercase()] ?: LanguageType.CUSTOM
    }
    
    // 根据项目路径推测语言类型
    fun detectLanguageType(projectPath: String): LanguageType {
        return when {
            projectPath.contains("package.json") -> LanguageType.NODEJS
            projectPath.contains("requirements.txt") || projectPath.contains("manage.py") -> LanguageType.PYTHON
            projectPath.contains("pom.xml") || projectPath.contains("build.gradle") -> LanguageType.JAVA
            projectPath.contains("go.mod") || projectPath.contains("main.go") -> LanguageType.GO
            else -> LanguageType.CUSTOM
        }
    }
}
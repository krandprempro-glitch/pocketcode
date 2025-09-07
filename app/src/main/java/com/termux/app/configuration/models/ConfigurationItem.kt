package com.termux.app.configuration.models

import com.termux.R

data class ConfigurationItem(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val type: ConfigurationType,
    val enabled: Boolean = true
) {
    
    enum class ConfigurationType {
        SSH_CONFIG,     // SSH连接配置
        RUN_CONFIG,     // 运行配置
        GLOBAL_SETTINGS, // 全局设置
        ABOUT           // 关于页面
    }
    
    companion object {
        val DEFAULT_ITEMS = arrayOf(
            ConfigurationItem(
                "ssh_config",
                "SSH连接配置",
                "管理SSH服务器连接",
                R.drawable.ic_ssh,
                ConfigurationType.SSH_CONFIG
            ),
            ConfigurationItem(
                "run_config",
                "运行配置管理",
                "配置项目运行命令",
                R.drawable.ic_run,
                ConfigurationType.RUN_CONFIG
            ),
            ConfigurationItem(
                "global_settings",
                "全局设置",
                "应用偏好设置",
                R.drawable.ic_settings,
                ConfigurationType.GLOBAL_SETTINGS
            ),
            ConfigurationItem(
                "about",
                "关于",
                "版本信息和帮助",
                R.drawable.ic_info,
                ConfigurationType.ABOUT
            )
        )
        
        fun getItemById(id: String): ConfigurationItem? {
            return DEFAULT_ITEMS.find { it.id == id }
        }
        
        fun getItemsByType(type: ConfigurationType): List<ConfigurationItem> {
            return DEFAULT_ITEMS.filter { it.type == type }
        }
    }
}
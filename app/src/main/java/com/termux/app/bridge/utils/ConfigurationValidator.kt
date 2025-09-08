package com.termux.app.bridge.utils

import android.text.TextUtils
import com.termux.app.bridge.managers.ConfigFloatingBridge
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.models.SSHConnectionConfig
import java.util.regex.Pattern

/**
 * 配置验证器
 * 验证配置数据的完整性和有效性，提供数据验证和安全检查功能
 */
object ConfigurationValidator {
    
    // 主机名验证正则表达式
    private val HOSTNAME_PATTERN = Pattern.compile(
        "^((\\d{1,3}\\.){3}\\d{1,3}|[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$"
    )
    
    // 环境变量格式验证正则表达式  
    private val ENV_VAR_PATTERN = Pattern.compile("\\w+=[^\\s]*")
    
    // 危险命令检测模式
    private val DANGEROUS_COMMANDS = setOf(
        "rm -rf", "sudo rm", "format", "mkfs", "dd if=", ":(){ :|:& };:",
        "> /dev/", "chmod 777", "chown -R", "deluser", "userdel"
    )
    
    /**
     * 验证运行配置
     */
    @JvmStatic
    fun validateRunConfiguration(config: RunConfiguration): ConfigFloatingBridge.ValidationResult {
        val result = ConfigFloatingBridge.ValidationResult()
        
        // 基本字段验证
        if (TextUtils.isEmpty(config.id)) {
            result.addError("配置ID不能为空")
        }
        
        if (TextUtils.isEmpty(config.name)) {
            result.addError("配置名称不能为空")
        } else if (config.name.length > 50) {
            result.addWarning("配置名称过长，建议不超过50个字符")
        }
        
        if (TextUtils.isEmpty(config.sshConfigId)) {
            result.addError("SSH配置ID不能为空")
        }
        
        if (TextUtils.isEmpty(config.projectPath)) {
            result.addError("项目路径不能为空")
        } else {
            validateProjectPath(config.projectPath, result)
        }
        
        if (TextUtils.isEmpty(config.command)) {
            result.addError("运行命令不能为空")
        } else {
            validateCommand(config.command, result)
        }
        
        // 环境变量格式检查
        if (!TextUtils.isEmpty(config.envVariables)) {
            validateEnvVariables(config.envVariables, result)
        }
        
        // 日志文件名检查
        if (!TextUtils.isEmpty(config.logFileName)) {
            validateLogFileName(config.logFileName, result)
        }
        
        return result
    }
    
    /**
     * 验证SSH配置
     */
    @JvmStatic
    fun validateSSHConfiguration(config: SSHConnectionConfig): ConfigFloatingBridge.ValidationResult {
        val result = ConfigFloatingBridge.ValidationResult()
        
        // 主机地址验证
        if (TextUtils.isEmpty(config.host)) {
            result.addError("主机地址不能为空")
        } else {
            if (!isValidHostname(config.host)) {
                result.addError("主机地址格式不正确")
            }
        }
        
        // 端口验证
        if (config.port <= 0 || config.port > 65535) {
            result.addError("端口号必须在1-65535之间")
        } else if (config.port != 22) {
            result.addWarning("使用非标准SSH端口 (${config.port})，请确认端口正确")
        }
        
        // 用户名验证
        if (TextUtils.isEmpty(config.username)) {
            result.addError("用户名不能为空")
        } else {
            validateUsername(config.username, result)
        }
        
        // 认证方式验证
        val hasPassword = !TextUtils.isEmpty(config.password)
        val hasPrivateKey = !TextUtils.isEmpty(config.privateKeyPath)
        
        if (!hasPassword && !hasPrivateKey) {
            result.addError("必须提供密码或私钥路径")
        }
        
        if (hasPassword && hasPrivateKey) {
            result.addWarning("同时配置了密码和私钥，将优先使用私钥认证")
        }
        
        // 私钥路径验证
        if (hasPrivateKey) {
            validatePrivateKeyPath(config.privateKeyPath, result)
        }
        
        // 连接超时验证
        if (config.connectionTimeout != null) {
            if (config.connectionTimeout!! <= 0) {
                result.addError("连接超时时间必须大于0")
            } else if (config.connectionTimeout!! > 300) {
                result.addWarning("连接超时时间过长 (${config.connectionTimeout}秒)，建议设置在30-60秒之间")
            }
        }
        
        return result
    }
    
    private fun validateProjectPath(path: String, result: ConfigFloatingBridge.ValidationResult) {
        if (!path.startsWith("/")) {
            result.addError("项目路径必须是绝对路径（以 / 开头）")
        }
        
        if (path.contains("..")) {
            result.addWarning("项目路径包含相对路径标识符 (..)，可能存在安全风险")
        }
        
        if (path.length > 500) {
            result.addError("项目路径过长，超过500个字符")
        }
        
        // 检查路径中的危险字符
        val dangerousChars = setOf(";", "&", "|", ">", "<", "*", "?")
        if (dangerousChars.any { path.contains(it) }) {
            result.addWarning("项目路径包含特殊字符，请确认路径正确")
        }
    }
    
    private fun validateCommand(command: String, result: ConfigFloatingBridge.ValidationResult) {
        // 命令长度检查
        if (command.length > 1000) {
            result.addError("运行命令过长，超过1000个字符")
        }
        
        // 危险命令检测
        val lowerCommand = command.lowercase()
        for (dangerousCmd in DANGEROUS_COMMANDS) {
            if (lowerCommand.contains(dangerousCmd)) {
                result.addError("命令包含危险操作: $dangerousCmd")
            }
        }
        
        // 检查命令注入风险
        if (command.contains("$(") || command.contains("`")) {
            result.addWarning("命令包含命令替换语法，请确认安全性")
        }
        
        // 检查重定向操作
        if (command.contains(">") && !command.contains("nohup") && !command.contains("&")) {
            result.addWarning("命令包含输出重定向，建议配置为后台运行")
        }
    }
    
    private fun validateEnvVariables(envVars: String, result: ConfigFloatingBridge.ValidationResult) {
        val pairs = envVars.split("\\s+".toRegex())
        for (pair in pairs) {
            if (pair.isNotEmpty() && !ENV_VAR_PATTERN.matcher(pair).matches()) {
                result.addError("环境变量格式不正确: $pair (正确格式: KEY=value)")
            }
        }
        
        // 检查是否设置了危险的环境变量
        val dangerousEnvVars = setOf("PATH", "LD_LIBRARY_PATH", "HOME")
        for (pair in pairs) {
            val key = pair.substringBefore("=")
            if (dangerousEnvVars.contains(key)) {
                result.addWarning("修改了系统环境变量 $key，可能影响程序执行")
            }
        }
    }
    
    private fun validateLogFileName(logFileName: String, result: ConfigFloatingBridge.ValidationResult) {
        // 检查文件名中的非法字符
        val illegalChars = setOf("/", "\\", ":", "*", "?", "\"", "<", ">", "|")
        if (illegalChars.any { logFileName.contains(it) }) {
            result.addError("日志文件名包含非法字符")
        }
        
        if (logFileName.length > 255) {
            result.addError("日志文件名过长，超过255个字符")
        }
        
        if (!logFileName.endsWith(".log") && !logFileName.endsWith(".txt")) {
            result.addWarning("建议使用 .log 或 .txt 作为日志文件扩展名")
        }
    }
    
    private fun validateUsername(username: String, result: ConfigFloatingBridge.ValidationResult) {
        if (username.length < 1 || username.length > 32) {
            result.addError("用户名长度必须在1-32个字符之间")
        }
        
        if (!username.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]*$"))) {
            result.addError("用户名格式不正确，必须以字母开头，只能包含字母、数字、点、下划线和连字符")
        }
        
        val reservedNames = setOf("root", "admin", "administrator", "daemon", "sys", "sync")
        if (reservedNames.contains(username.lowercase())) {
            result.addWarning("使用了系统保留用户名，请确认是否正确")
        }
    }
    
    private fun validatePrivateKeyPath(keyPath: String, result: ConfigFloatingBridge.ValidationResult) {
        if (!keyPath.startsWith("/")) {
            result.addError("私钥路径必须是绝对路径")
        }
        
        if (!keyPath.contains("/.ssh/") && !keyPath.endsWith(".pem") && !keyPath.endsWith(".key")) {
            result.addWarning("私钥文件路径不符合常见格式，请确认路径正确")
        }
        
        if (keyPath.contains(" ")) {
            result.addWarning("私钥路径包含空格，可能导致认证失败")
        }
    }
    
    private fun isValidHostname(hostname: String): Boolean {
        if (hostname.length > 253) return false
        return HOSTNAME_PATTERN.matcher(hostname).matches()
    }
    
    /**
     * 验证配置数据的业务逻辑一致性
     */
    @JvmStatic
    fun validateConfigurationConsistency(
        runConfig: RunConfiguration,
        sshConfig: SSHConnectionConfig
    ): ConfigFloatingBridge.ValidationResult {
        val result = ConfigFloatingBridge.ValidationResult()
        
        // 检查运行配置是否与SSH配置匹配
        if (runConfig.sshConfigId != sshConfig.id) {
            result.addError("运行配置的SSH配置ID与提供的SSH配置不匹配")
        }
        
        // 根据SSH用户类型检查项目路径
        if (sshConfig.username == "root" && !runConfig.projectPath.startsWith("/root") && !runConfig.projectPath.startsWith("/opt")) {
            result.addWarning("使用root用户但项目路径不在常见的root目录下")
        }
        
        // 检查命令与项目路径的一致性
        if (runConfig.projectPath.contains("node") || runConfig.projectPath.contains("npm")) {
            if (!runConfig.command.contains("npm") && !runConfig.command.contains("node") && !runConfig.command.contains("yarn")) {
                result.addWarning("项目路径暗示这是Node.js项目，但运行命令中未包含相关命令")
            }
        }
        
        return result
    }
    
    /**
     * 快速验证配置基本信息
     */
    @JvmStatic
    fun quickValidate(config: RunConfiguration): Boolean {
        return !TextUtils.isEmpty(config.id) &&
               !TextUtils.isEmpty(config.name) &&
               !TextUtils.isEmpty(config.sshConfigId) &&
               !TextUtils.isEmpty(config.projectPath) &&
               !TextUtils.isEmpty(config.command)
    }
    
    /**
     * 快速验证SSH配置基本信息
     */
    @JvmStatic
    fun quickValidate(config: SSHConnectionConfig): Boolean {
        return !TextUtils.isEmpty(config.host) &&
               config.port in 1..65535 &&
               !TextUtils.isEmpty(config.username) &&
               (!TextUtils.isEmpty(config.password) || !TextUtils.isEmpty(config.privateKeyPath))
    }
}
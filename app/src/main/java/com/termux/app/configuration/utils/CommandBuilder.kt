package com.termux.app.configuration.utils

import android.text.TextUtils
import com.termux.app.configuration.models.RunConfiguration

object CommandBuilder {
    
    /**
     * 构建基础命令
     */
    fun buildBasicCommand(config: RunConfiguration): String {
        val cmd = StringBuilder()
        
        // 切换到项目目录
        cmd.append("cd ${config.projectPath}")
        if (config.workingDir.isNotBlank() && config.workingDir != ".") {
            cmd.append("/${config.workingDir}")
        }
        cmd.append(" && ")
        
        // 添加环境变量
        if (config.envVariables.isNotBlank()) {
            cmd.append("${config.envVariables} ")
        }
        
        // 执行命令
        cmd.append(config.command)
        
        return cmd.toString()
    }
    
    /**
     * 构建后台运行命令
     */
    fun buildBackgroundCommand(config: RunConfiguration): String {
        val basicCommand = buildBasicCommand(config)
        
        if (!config.runInBackground) {
            return basicCommand
        }
        
        val cmd = StringBuilder()
        cmd.append("nohup $basicCommand")
        
        // 添加日志输出
        val logFile = config.logFileName.ifBlank { ConfigurationConstants.DEFAULT_LOG_FILE }
        cmd.append(" > $logFile 2>&1 &")
        
        // 保存PID
        cmd.append(" echo \$! > .pid")
        
        return cmd.toString()
    }
    
    /**
     * 生成Kill命令
     */
    fun generateKillCommand(config: RunConfiguration): String {
        return when (config.languageType) {
            com.termux.app.configuration.models.LanguageType.NODEJS -> {
                "pkill -f \"${config.command}\" 2>/dev/null || true"
            }
            com.termux.app.configuration.models.LanguageType.PYTHON -> {
                "pkill -f \"python.*${extractMainFile(config.command)}\" 2>/dev/null || true"
            }
            com.termux.app.configuration.models.LanguageType.JAVA -> {
                "pkill -f \"java.*${extractJarFile(config.command)}\" 2>/dev/null || true"
            }
            com.termux.app.configuration.models.LanguageType.GO -> {
                "pkill -f \"${extractGoApp(config.command)}\" 2>/dev/null || true"
            }
            else -> {
                "pkill -f \"${config.command}\" 2>/dev/null || true"
            }
        }
    }
    
    /**
     * 构建完整命令(包含Kill+Run)
     */
    fun buildCompleteCommand(config: RunConfiguration): String {
        val killCommand = generateKillCommand(config)
        val runCommand = if (config.runInBackground) {
            buildBackgroundCommand(config)
        } else {
            buildBasicCommand(config)
        }
        
        return "$killCommand; $runCommand"
    }
    
    /**
     * 构建预览命令(用于界面显示)
     */
    fun buildPreviewCommand(config: RunConfiguration): String {
        val commands = mutableListOf<String>()
        
        // Kill命令
        commands.add("# 终止之前的进程")
        commands.add(generateKillCommand(config))
        
        // 空行
        commands.add("")
        
        // 运行命令
        commands.add("# 启动新进程")
        commands.add(buildCompleteCommand(config).substringAfter("; "))
        
        return commands.joinToString("\n")
    }
    
    // 辅助方法
    private fun extractMainFile(command: String): String {
        return when {
            command.contains(".py") -> command.substringAfter("python").trim().split(" ").firstOrNull() ?: ""
            command.contains("manage.py") -> "manage.py"
            else -> ""
        }
    }
    
    private fun extractJarFile(command: String): String {
        return when {
            command.contains("-jar") -> command.substringAfter("-jar").trim().split(" ").firstOrNull() ?: ""
            else -> ""
        }
    }
    
    private fun extractGoApp(command: String): String {
        return when {
            command.contains("go run") -> command.substringAfter("go run").trim().split(" ").firstOrNull() ?: "main.go"
            command.contains("./") -> command.substringAfter("./").split(" ").firstOrNull() ?: "app"
            else -> "go"
        }
    }
}
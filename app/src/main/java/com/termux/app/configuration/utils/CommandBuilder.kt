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
        if (!config.runInBackground) {
            return buildBasicCommand(config)
        }
        // cd 到项目目录（包含工作子目录）
        val cd = StringBuilder()
        cd.append("cd ${config.projectPath}")
        if (config.workingDir.isNotBlank() && config.workingDir != ".") {
            cd.append("/${config.workingDir}")
        }

        // 环境变量 + nohup 命令主体；确保脱离标准输入
        val run = StringBuilder()
        if (config.envVariables.isNotBlank()) {
            run.append("${config.envVariables} ")
        }
        val logFile = config.logFileName.ifBlank { ConfigurationConstants.DEFAULT_LOG_FILE }
        // 注意：这里需要在远端 shell 展开 $!，因此不要转义 $，在 Kotlin 中使用 ${'$'} 生成字面量 $
        run.append("nohup ${config.command} > $logFile 2>&1 < /dev/null & echo ${'$'}! > .pid")

        return "${cd} && ${run}"
    }
    
    /**
     * 生成Kill命令
     */
    fun generateKillCommand(config: RunConfiguration): String {
        // 优先使用端口号杀进程
        if (config.port > 0) {
            val port = config.port
            val killByPort = StringBuilder()
            // 初始化 PID 为空
            killByPort.append("PID=\"\"; ")
            // lsof 优先
            killByPort.append("command -v lsof >/dev/null 2>&1 && PID=${'$'}(lsof -ti tcp:$port 2>/dev/null | head -n1); ")
            // ss 回退
            killByPort.append("if [ -z \"${'$'}PID\" ] && command -v ss >/dev/null 2>&1; then PID=${'$'}(ss -lptn 2>/dev/null | awk -v p=:$port '\${'$'}4 ~ p { if (match(\${'$'}NF, /pid=[0-9]+/)) { print substr(\${'$'}NF,RSTART+4,RLENGTH-4); exit } }'); fi; ")
            // netstat 回退
            killByPort.append("if [ -z \"${'$'}PID\" ] && command -v netstat >/dev/null 2>&1; then PID=${'$'}(netstat -lntp 2>/dev/null | awk -v p=:$port '\${'$'}4 ~ p { split(\${'$'}7,a,\"/\"); print a[1]; exit }'); fi; ")
            // 发送 kill 信号
            killByPort.append("if [ -n \"${'$'}PID\" ]; then kill \"${'$'}PID\" 2>/dev/null || true; sleep 0.5; kill -9 \"${'$'}PID\" 2>/dev/null || true; fi")
            return killByPort.toString()
        }

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
        // 前台命令不附带 pkill，避免自杀式匹配（如 pkill -f "ls -la" 命中当前 shell）
        if (!config.runInBackground) {
            return buildBasicCommand(config)
        }

        val killCommand = generateKillCommand(config)
        val runCommand = buildBackgroundCommand(config)
        return "$killCommand; $runCommand"
    }

    /**
     * 构建查看日志命令（tail -n）
     */
    fun buildTailLogCommand(config: RunConfiguration, lines: Int = 200): String {
        val cd = StringBuilder()
        cd.append("cd ${config.projectPath}")
        if (config.workingDir.isNotBlank() && config.workingDir != ".") {
            cd.append("/${config.workingDir}")
        }
        val logFile = config.logFileName.ifBlank { ConfigurationConstants.DEFAULT_LOG_FILE }
        val run = StringBuilder()
        if (config.envVariables.isNotBlank()) run.append("${config.envVariables} ")
        run.append("tail -n ${lines.coerceAtLeast(1)} $logFile")
        return "${cd} && ${run}"
    }

    /**
     * 构建停止命令（通过 .pid 杀进程）
     */
    fun buildStopCommand(config: RunConfiguration): String {
        // 如果配置了端口，使用端口查找PID停止；否则回退到 .pid
        if (config.port > 0) {
            return generateKillCommand(config)
        }

        val cd = StringBuilder()
        cd.append("cd ${config.projectPath}")
        if (config.workingDir.isNotBlank() && config.workingDir != ".") {
            cd.append("/${config.workingDir}")
        }
        val sb = StringBuilder()
        sb.append("[ -f .pid ] && (kill $(cat .pid) 2>/dev/null || true); ")
        sb.append("sleep 0.5; ")
        sb.append("[ -f .pid ] && (kill -9 $(cat .pid) 2>/dev/null || true); ")
        sb.append("rm -f .pid 2>/dev/null || true")
        return "${cd} && ${sb}"
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

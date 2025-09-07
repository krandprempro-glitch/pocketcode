package com.termux.app.floating.services

import android.text.TextUtils
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.models.SSHConnectionConfig
import com.termux.shared.logger.Logger
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.*

class RemoteCommandService {
    
    companion object {
        private const val TAG = "RemoteCommandService"
        private const val DEFAULT_TIMEOUT = 30000L // 30秒超时
    }
    
    private val executorService = Executors.newCachedThreadPool()
    private val runningTasks = ConcurrentHashMap<String, CompletableFuture<ExecutionResult>>()
    
    /**
     * 异步执行远程命令
     */
    fun executeCommand(
        sshConfig: SSHConnectionConfig,
        command: String,
        progressListener: OnExecutionProgressListener?
    ): CompletableFuture<ExecutionResult> {
        
        val taskId = java.util.UUID.randomUUID().toString()
        
        val future = CompletableFuture.supplyAsync({
            executeCommandSync(sshConfig, command, progressListener)
        }, executorService)
        
        runningTasks[taskId] = future
        
        // 任务完成后清理
        future.whenComplete { _, _ ->
            runningTasks.remove(taskId)
        }
        
        return future
    }
    
    private fun executeCommandSync(
        sshConfig: SSHConnectionConfig,
        command: String,
        progressListener: OnExecutionProgressListener?
    ): ExecutionResult {
        
        val result = ExecutionResult().apply {
            status = ExecutionResult.Status.EXECUTING
            executedCommand = command
            startTime = System.currentTimeMillis()
        }
        
        progressListener?.onProgress("正在建立SSH连接...", result)
        
        var sshClient: SSHClient? = null
        var session: Session? = null
        
        try {
            // 建立SSH连接
            sshClient = SSHClient().apply {
                addHostKeyVerifier(PromiscuousVerifier()) // 开发环境，生产需要验证
                connectTimeout = 10000
                connect(sshConfig.host, sshConfig.port)
            }
            
            progressListener?.onProgress("SSH连接成功，正在认证...", result)
            
            // 身份认证（当前只支持密码认证，私钥认证功能可以后续添加）
            sshClient.authPassword(sshConfig.username, sshConfig.password)
            
            progressListener?.onProgress("认证成功，正在执行命令...", result)
            
            // 创建会话并执行命令
            session = sshClient.startSession()
            val cmd = session.exec(command)
            
            // 读取输出
            val outputBuilder = StringBuilder()
            val errorBuilder = StringBuilder()
            
            cmd.inputStream.use { stdout ->
                cmd.errorStream.use { stderr ->
                    
                    // 异步读取标准输出和错误输出
                    val outputFuture = CompletableFuture.runAsync {
                        try {
                            BufferedReader(InputStreamReader(stdout)).use { reader ->
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    outputBuilder.append(line).append("\n")
                                }
                            }
                        } catch (e: IOException) {
                            Logger.logError("Error reading stdout: ${e.message}")
                        }
                    }
                    
                    val errorFuture = CompletableFuture.runAsync {
                        try {
                            BufferedReader(InputStreamReader(stderr)).use { reader ->
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    errorBuilder.append(line).append("\n")
                                }
                            }
                        } catch (e: IOException) {
                            Logger.logError("Error reading stderr: ${e.message}")
                        }
                    }
                    
                    // 等待命令完成或超时
                    cmd.join(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                    if (cmd.isEOF) {
                        // 等待输出读取完成
                        CompletableFuture.allOf(outputFuture, errorFuture).join()
                        
                        val exitCode = cmd.exitStatus ?: -1
                        result.apply {
                            this.exitCode = exitCode
                            output = outputBuilder.toString()
                            errorOutput = errorBuilder.toString()
                        }
                        
                        if (exitCode == 0) {
                            result.status = ExecutionResult.Status.SUCCESS
                            
                            // 尝试解析进程ID（如果命令包含后台运行）
                            if (command.contains("echo \$! > .pid")) {
                                extractProcessId(result, sshClient)
                            }
                            
                        } else {
                            result.status = ExecutionResult.Status.ERROR
                            result.errorMessage = "命令执行失败，退出码: $exitCode"
                        }
                    } else {
                        // 超时
                        result.status = ExecutionResult.Status.TIMEOUT
                        result.errorMessage = "命令执行超时"
                    }
                }
            }
            
        } catch (e: IOException) {
            result.status = ExecutionResult.Status.ERROR
            result.errorMessage = "SSH连接失败: ${e.message}"
            Logger.logError("SSH connection failed: ${e.message}")
            
        } catch (e: Exception) {
            result.status = ExecutionResult.Status.ERROR
            result.errorMessage = "执行命令时发生错误: ${e.message}"
            Logger.logError("Command execution failed: ${e.message}")
            
        } finally {
            // 清理资源
            session?.let { 
                try {
                    it.close()
                } catch (e: IOException) {
                    Logger.logError("Failed to close session: ${e.message}")
                }
            }
            
            sshClient?.let {
                try {
                    it.disconnect()
                } catch (e: IOException) {
                    Logger.logError("Failed to disconnect SSH client: ${e.message}")
                }
            }
            
            result.endTime = System.currentTimeMillis()
            progressListener?.onCompleted(result)
        }
        
        return result
    }
    
    private fun extractProcessId(result: ExecutionResult, sshClient: SSHClient) {
        try {
            // 读取.pid文件获取进程ID
            val pidSession = sshClient.startSession()
            val pidCmd = pidSession.exec("cat .pid 2>/dev/null || echo ''")
            
            pidCmd.join(5000, TimeUnit.MILLISECONDS)
            if (pidCmd.isEOF) {
                pidCmd.inputStream.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val pidLine = reader.readLine()
                        if (!TextUtils.isEmpty(pidLine)) {
                            try {
                                val pid = pidLine.trim().toInt()
                                result.processId = pid
                            } catch (e: NumberFormatException) {
                                Logger.logError("Failed to parse PID: $pidLine")
                            }
                        }
                    }
                }
            }
            
            pidSession.close()
            
        } catch (e: Exception) {
            Logger.logError("Failed to extract process ID: ${e.message}")
        }
    }
    
    /**
     * 取消正在执行的任务
     */
    fun cancelTask(taskId: String) {
        runningTasks[taskId]?.cancel(true)
    }
    
    /**
     * 获取正在运行的任务数量
     */
    fun getRunningTaskCount(): Int = runningTasks.size
    
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
    
    interface OnExecutionProgressListener {
        fun onProgress(message: String, currentResult: ExecutionResult)
        fun onCompleted(result: ExecutionResult)
    }
}
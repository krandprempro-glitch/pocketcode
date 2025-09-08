package com.termux.app.floating.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.kex.KeyExchange
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider
import java.security.Security
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.shared.logger.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * SSH远程命令执行器
 * 负责执行远程命令并管理执行状态
 */
class RemoteCommandExecutor private constructor(
    private val context: Context
) : CoroutineScope {

    companion object {
        private const val LOG_TAG = "RemoteCommandExecutor"
        private const val CONNECTION_TIMEOUT = 30000 // 30秒连接超时
        private const val COMMAND_TIMEOUT = 300000 // 5分钟命令超时
        
        @Volatile
        private var instance: RemoteCommandExecutor? = null
        
        fun getInstance(context: Context): RemoteCommandExecutor {
            return instance ?: synchronized(this) {
                instance ?: RemoteCommandExecutor(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val sshConfigManager = SSHConfigManager.getInstance(context)
    private val executionStateManager = ExecutionStateManager.getInstance(context)
    private val sftpConnectionManager = SFTPConnectionManager.getInstance()
    
    // 活跃的执行任务
    private val activeExecutions = mutableMapOf<String, Job>()
    
    // SSH连接池
    private val sshConnections = mutableMapOf<String, SSHClient>()

    /**
     * 执行运行配置
     */
    suspend fun executeConfiguration(config: RunConfiguration): ExecutionResult {
        return withContext(Dispatchers.IO) {
            val executionId = generateExecutionId(config)
            
            try {
                Logger.logInfo(LOG_TAG, "Starting execution: ${config.name}")
                
                // 更新执行状态为运行中
                executionStateManager.updateExecutionState(executionId, ExecutionResult.Status.EXECUTING)
                
                // 获取SSH配置
                val sshConfig = sshConfigManager.getConfigByName(config.sshConfigId)
                    ?: throw IllegalArgumentException("SSH配置不存在: ${config.sshConfigId}")
                
                // 建立SSH连接（优先使用SFTP连接管理器的连接）
                val sshClient = getOrCreateConnection(sshConfig)
                
                // 构建完整命令
                val command = CommandBuilder.buildCompleteCommand(config)
                Logger.logInfo(LOG_TAG, "Executing command: $command")
                
                // 执行命令
                val result = executeRemoteCommand(sshClient, command, executionId, config.runInBackground)
                
                // 更新最后使用时间
                config.lastUsedTime = System.currentTimeMillis()
                
                // 保存执行结果
                executionStateManager.saveExecutionResult(executionId, result)
                
                result
                
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Execution failed: ${e.message}")
                
                val errorResult = ExecutionResult(
                    taskId = executionId,
                    status = ExecutionResult.Status.ERROR,
                    executedCommand = config.command,
                    errorMessage = e.message ?: "Unknown error",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis()
                )
                
                executionStateManager.saveExecutionResult(executionId, errorResult)
                errorResult
            }
        }
    }

    /**
     * 执行远程命令
     */
    private suspend fun executeRemoteCommand(
        sshClient: SSHClient,
        command: String,
        executionId: String,
        isBackground: Boolean
    ): ExecutionResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        val errorBuilder = StringBuilder()
        
        var session: Session? = null
        
        try {
            session = sshClient.startSession()
            // 默认不分配 PTY，避免后台任务与 TTY 绑定导致阻塞
            
            val execCommand = session.exec(command)

            if (isBackground) {
                // 后台命令：尽快读取一行（期望为 PID），然后关闭会话，避免 300s 超时
                var pidLine: String? = null
                try {
                    withTimeoutOrNull(10_000) {
                        BufferedReader(InputStreamReader(execCommand.inputStream)).use { reader ->
                            pidLine = reader.readLine()
                        }
                    }
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error reading background PID: ${e.message}")
                }

                try { execCommand.close() } catch (_: Exception) {}
                try { session.close() } catch (_: Exception) {}

                val endTime = System.currentTimeMillis()
                return@withContext ExecutionResult(
                    taskId = executionId,
                    status = ExecutionResult.Status.SUCCESS,
                    executedCommand = command,
                    output = pidLine ?: "",
                    errorOutput = "",
                    exitCode = 0,
                    startTime = startTime,
                    endTime = endTime
                )
            }
            
            // 创建协程来处理输出流
            val outputJob = launch {
                try {
                    BufferedReader(InputStreamReader(execCommand.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            outputBuilder.appendLine(line)
                            
                            // 实时更新执行结果
                            val currentResult = ExecutionResult(
                                taskId = executionId,
                                status = ExecutionResult.Status.EXECUTING,
                                executedCommand = command,
                                output = outputBuilder.toString(),
                                startTime = startTime,
                                endTime = 0
                            )
                            executionStateManager.updateLiveOutput(executionId, currentResult)
                        }
                    }
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error reading output stream: ${e.message}")
                }
            }
            
            // 创建协程来处理错误流
            val errorJob = launch {
                try {
                    BufferedReader(InputStreamReader(execCommand.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            errorBuilder.appendLine(line)
                        }
                    }
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Error reading error stream: ${e.message}")
                }
            }
            
            // 等待命令执行完成
            val exitStatus = withTimeoutOrNull(COMMAND_TIMEOUT.toLong()) {
                execCommand.join()
                execCommand.exitStatus
            }
            
            // 等待输出流处理完成
            outputJob.join()
            errorJob.join()
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            if (exitStatus == null) {
                // 超时
                throw RuntimeException("命令执行超时 (${COMMAND_TIMEOUT / 1000}秒)")
            }
            
            val status = if (exitStatus == 0) {
                ExecutionResult.Status.SUCCESS
            } else {
                ExecutionResult.Status.ERROR
            }
            
            ExecutionResult(
                taskId = executionId,
                status = status,
                executedCommand = command,
                output = outputBuilder.toString(),
                errorOutput = errorBuilder.toString(),
                exitCode = exitStatus,
                startTime = startTime,
                endTime = endTime
            )
            
        } finally {
            session?.close()
        }
    }

    /**
     * 取消执行
     */
    fun cancelExecution(executionId: String): Boolean {
        val job = activeExecutions[executionId]
        if (job != null && job.isActive) {
            job.cancel()
            activeExecutions.remove(executionId)
            
            // 更新状态为已取消
            executionStateManager.updateExecutionState(executionId, ExecutionResult.Status.CANCELLED)
            
            Logger.logInfo(LOG_TAG, "Execution cancelled: $executionId")
            return true
        }
        return false
    }

    /**
     * 获取或创建SSH连接
     * 优先复用SFTPConnectionManager的连接，如果不可用则创建新连接
     */
    private suspend fun getOrCreateConnection(sshConfig: SSHConnectionConfig): SSHClient {
        val connectionKey = "${sshConfig.host}:${sshConfig.port}:${sshConfig.username}"
        
        // 首先尝试复用SFTP连接管理器的连接
        if (sftpConnectionManager.isConnected) {
            val currentConfig = sftpConnectionManager.currentConfig
            if (currentConfig != null && 
                currentConfig.host == sshConfig.host &&
                currentConfig.port == sshConfig.port &&
                currentConfig.username == sshConfig.username) {
                
                Logger.logInfo(LOG_TAG, "Reusing existing SFTP connection for command execution")
                
                // 从SFTP连接管理器获取SSH客户端的反射访问
                try {
                    val field = SFTPConnectionManager::class.java.getDeclaredField("sshClient")
                    field.isAccessible = true
                    val sshClient = field.get(sftpConnectionManager) as? SSHClient
                    if (sshClient != null && sshClient.isConnected) {
                        return sshClient
                    }
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Failed to reuse SFTP connection: ${e.message}")
                }
            }
        }
        
        // 检查本地连接池中的现有连接
        val existingConnection = sshConnections[connectionKey]
        if (existingConnection != null && existingConnection.isConnected) {
            return existingConnection
        }
        
        try {
            Logger.logInfo(LOG_TAG, "Creating new SSH connection to ${sshConfig.host}:${sshConfig.port}")

            // 注册独立的 BouncyCastle 提供者（不依赖系统内置 BC），确保 ECDSA/ECDH 可用
            try {
                val bc = BouncyCastleProvider()
                Security.removeProvider(bc.name)
                Security.insertProviderAt(bc, 1)
            } catch (ignored: Throwable) {}
            
            // 避免 SSHJ 强制注册系统 BC（Android 内置 BC 不完整）
            try { 
                SecurityUtils.setRegisterBouncyCastle(false) 
            } catch (ignored: Throwable) {}
            
            // 使用默认配置基础上，明确剔除 ECDH（仅保留 curve25519 / diffie-hellman），并去掉 ECDSA 签名工厂
            val configObj = DefaultConfig()
            try {
                val kex = ArrayList(configObj.keyExchangeFactories)
                val it = kex.iterator()
                while (it.hasNext()) {
                    val name = it.next().name
                    val lower = name?.lowercase() ?: ""
                    if (!(lower.contains("curve25519") || lower.contains("diffie-hellman"))) {
                        it.remove()
                    }
                }
                configObj.keyExchangeFactories = kex
            } catch (ignored: Throwable) {}
            
            // 创建SSH客户端
            val sshClient = SSHClient(configObj)
            sshClient.addHostKeyVerifier(PromiscuousVerifier()) // 在生产环境中应该使用更安全的验证方式
            
            // 超时设置
            sshClient.connectTimeout = CONNECTION_TIMEOUT
            sshClient.timeout = CONNECTION_TIMEOUT
            
            // 连接到服务器
            withTimeoutOrNull(CONNECTION_TIMEOUT.toLong()) {
                sshClient.connect(sshConfig.host, sshConfig.port)
            } ?: throw RuntimeException("连接超时")
            
            // 身份验证
            if (!sshConfig.privateKeyPath.isNullOrEmpty()) {
                // 使用密钥认证
                sshClient.authPublickey(sshConfig.username, sshConfig.privateKeyPath)
            } else {
                // 使用密码认证
                sshClient.authPassword(sshConfig.username, sshConfig.password ?: "")
            }
            
            // 缓存连接
            sshConnections[connectionKey] = sshClient
            
            Logger.logInfo(LOG_TAG, "SSH connection established successfully: $connectionKey")
            return sshClient
            
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "SSH connection failed: ${e.message}")
            throw RuntimeException("SSH连接失败: ${e.message}", e)
        }
    }

    /**
     * 生成执行ID
     */
    private fun generateExecutionId(config: RunConfiguration): String {
        return "${config.id}_${System.currentTimeMillis()}"
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // 取消所有活跃的执行
        activeExecutions.values.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        activeExecutions.clear()
        
        // 关闭所有SSH连接
        sshConnections.values.forEach { client ->
            try {
                if (client.isConnected) {
                    client.close()
                }
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Error closing SSH connection: ${e.message}")
            }
        }
        sshConnections.clear()
        
        // 取消协程作用域
        job.cancel()
    }

    /**
     * 测试SSH连接
     */
    suspend fun testConnection(sshConfig: SSHConnectionConfig): Boolean {
        return try {
            val sshClient = getOrCreateConnection(sshConfig)
            sshClient.isConnected
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Connection test failed: ${e.message}")
            false
        }
    }

    /**
     * 获取活跃执行的数量
     */
    fun getActiveExecutionsCount(): Int {
        return activeExecutions.count { it.value.isActive }
    }

    /**
     * 获取所有活跃的执行ID
     */
    fun getActiveExecutionIds(): List<String> {
        return activeExecutions.filter { it.value.isActive }.keys.toList()
    }
}

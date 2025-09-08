package com.termux.app.floating.services

import android.content.Context
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.models.ExecutionResult
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.shared.logger.Logger
import kotlinx.coroutines.*
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.kex.KeyExchange
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.Security
import kotlin.coroutines.CoroutineContext

/**
 * SSH远程命令执行器：负责执行远程命令并管理执行状态
 */
class RemoteCommandExecutor private constructor(
    private val context: Context
) : CoroutineScope {

    companion object {
        private const val LOG_TAG = "RemoteCommandExecutor"
        private const val CONNECTION_TIMEOUT_MS = 30_000 // 30秒连接超时
        private const val COMMAND_TIMEOUT_MS = 300_000 // 5分钟命令超时

        @Volatile
        private var instance: RemoteCommandExecutor? = null

        fun getInstance(context: Context): RemoteCommandExecutor {
            return instance ?: synchronized(this) {
                instance ?: RemoteCommandExecutor(context.applicationContext).also { instance = it }
            }
        }
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val sshConfigManager = SSHConfigManager.getInstance(context)
    private val executionStateManager = ExecutionStateManager.getInstance(context)
    private val sftpConnectionManager = SFTPConnectionManager.getInstance()

    private val activeExecutions = mutableMapOf<String, Job>()
    private val sshConnections = mutableMapOf<String, SSHClient>()

    suspend fun executeConfiguration(config: RunConfiguration): ExecutionResult {
        return withContext(Dispatchers.IO) {
            val executionId = generateExecutionId(config)
            try {
                Logger.logInfo(LOG_TAG, "Starting execution: ${config.name}")
                executionStateManager.updateExecutionState(executionId, ExecutionResult.Status.EXECUTING)

                val sshConfig = sshConfigManager.getConfigByName(config.sshConfigId)
                    ?: throw IllegalArgumentException("SSH配置不存在 ${config.sshConfigId}")

                val sshClient = getOrCreateConnection(sshConfig)

                val command = CommandBuilder.buildCompleteCommand(config)
                Logger.logInfo(LOG_TAG, "Executing command: $command")

                val result = executeRemoteCommand(sshClient, command, executionId, config.runInBackground)

                config.lastUsedTime = System.currentTimeMillis()
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
            val wrapped = "sh -lc '" + command.replace("'", "'\"'\"'") + "'"
            val execCommand = session.exec(wrapped)

            if (isBackground) {
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

            val outputJob = launch {
                try {
                    BufferedReader(InputStreamReader(execCommand.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            outputBuilder.appendLine(line)
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

            val exitStatus = withTimeoutOrNull(COMMAND_TIMEOUT_MS.toLong()) {
                execCommand.join()
                execCommand.exitStatus
            }

            outputJob.join()
            errorJob.join()

            val endTime = System.currentTimeMillis()

            if (exitStatus == null) {
                throw RuntimeException("命令执行超时 (${COMMAND_TIMEOUT_MS / 1000}秒)")
            }

            val status = if (exitStatus == 0) ExecutionResult.Status.SUCCESS else ExecutionResult.Status.ERROR

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
            try { session?.close() } catch (_: Exception) {}
        }
    }

    fun cancelExecution(executionId: String): Boolean {
        val job = activeExecutions[executionId]
        if (job != null && job.isActive) {
            job.cancel()
            activeExecutions.remove(executionId)
            executionStateManager.updateExecutionState(executionId, ExecutionResult.Status.CANCELLED)
            Logger.logInfo(LOG_TAG, "Execution cancelled: $executionId")
            return true
        }
        return false
    }

    private suspend fun getOrCreateConnection(sshConfig: SSHConnectionConfig): SSHClient {
        val connectionKey = "${sshConfig.host}:${sshConfig.port}:${sshConfig.username}"

        if (sftpConnectionManager.isConnected) {
            val currentConfig = sftpConnectionManager.currentConfig
            if (currentConfig != null &&
                currentConfig.host == sshConfig.host &&
                currentConfig.port == sshConfig.port &&
                currentConfig.username == sshConfig.username) {
                try {
                    Logger.logInfo(LOG_TAG, "Reusing existing SFTP connection for command execution")
                    val field = SFTPConnectionManager::class.java.getDeclaredField("sshClient")
                    field.isAccessible = true
                    val sshClient = field.get(sftpConnectionManager) as? SSHClient
                    if (sshClient != null && sshClient.isConnected) return sshClient
                } catch (e: Exception) {
                    Logger.logError(LOG_TAG, "Failed to reuse SFTP connection: ${e.message}")
                }
            }
        }

        sshConnections[connectionKey]?.let { if (it.isConnected) return it }

        try {
            Logger.logInfo(LOG_TAG, "Creating new SSH connection to ${sshConfig.host}:${sshConfig.port}")

            try {
                val bc = BouncyCastleProvider()
                Security.removeProvider(bc.name)
                Security.insertProviderAt(bc, 1)
            } catch (_: Throwable) {}

            try { SecurityUtils.setRegisterBouncyCastle(false) } catch (_: Throwable) {}

            val configObj = DefaultConfig()
            try {
                val kex = ArrayList(configObj.keyExchangeFactories)
                val it = kex.iterator()
                while (it.hasNext()) {
                    val name = it.next().name?.lowercase() ?: ""
                    if (!(name.contains("curve25519") || name.contains("diffie-hellman"))) {
                        it.remove()
                    }
                }
                configObj.keyExchangeFactories = kex
            } catch (_: Throwable) {}

            val sshClient = SSHClient(configObj)
            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connectTimeout = CONNECTION_TIMEOUT_MS
            sshClient.timeout = CONNECTION_TIMEOUT_MS

            withTimeoutOrNull(CONNECTION_TIMEOUT_MS.toLong()) {
                sshClient.connect(sshConfig.host, sshConfig.port)
            } ?: throw RuntimeException("连接超时")

            if (!sshConfig.privateKeyPath.isNullOrEmpty()) {
                sshClient.authPublickey(sshConfig.username, sshConfig.privateKeyPath)
            } else {
                sshClient.authPassword(sshConfig.username, sshConfig.password ?: "")
            }

            sshConnections[connectionKey] = sshClient
            Logger.logInfo(LOG_TAG, "SSH connection established successfully: $connectionKey")
            return sshClient
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "SSH connection failed: ${e.message}")
            throw RuntimeException("SSH连接失败: ${e.message}", e)
        }
    }

    private fun generateExecutionId(config: RunConfiguration): String {
        return "${config.id}_${System.currentTimeMillis()}"
    }

    fun cleanup() {
        activeExecutions.values.forEach { if (it.isActive) it.cancel() }
        activeExecutions.clear()

        sshConnections.values.forEach {
            try { if (it.isConnected) it.close() } catch (_: Exception) {}
        }
        sshConnections.clear()

        job.cancel()
    }

    suspend fun testConnection(sshConfig: SSHConnectionConfig): Boolean {
        return try {
            val sshClient = getOrCreateConnection(sshConfig)
            sshClient.isConnected
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Connection test failed: ${e.message}")
            false
        }
    }

    fun getActiveExecutionsCount(): Int = activeExecutions.count { it.value.isActive }
    fun getActiveExecutionIds(): List<String> = activeExecutions.filter { it.value.isActive }.keys.toList()
}


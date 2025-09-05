package com.termux.filebrowser.domain.usecase.connection

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.model.ConnectionResult
import com.termux.filebrowser.domain.repository.SftpRepository
import com.termux.filebrowser.domain.repository.WorkspaceRepository

class ConnectToServerUseCase(
    private val sftpRepository: SftpRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(config: SSHConnectionConfig): ConnectionResult {
        // 建立SFTP连接
        val connected = sftpRepository.connect(config)
        if (!connected) {
            throw ConnectionException("无法建立SFTP连接")
        }
        
        // 获取服务器信息
        val serverInfo = sftpRepository.getServerInfo()
        
        // 创建或获取工作区
        val workspace = workspaceRepository.getOrCreateWorkspace(config)
        
        return ConnectionResult(
            isConnected = true,
            serverInfo = "${config.host}:${config.port}",
            workspace = workspace
        )
    }
}

class ConnectionException(message: String) : Exception(message)
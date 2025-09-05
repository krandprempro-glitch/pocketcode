package com.termux.filebrowser.data.datasource.remote

import com.termux.app.models.SSHConnectionConfig
import kotlinx.coroutines.flow.Flow

data class RemoteFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String,
    val owner: String,
    val group: String
)

interface SftpDataSource {
    suspend fun connect(config: SSHConnectionConfig): Boolean
    suspend fun disconnect()
    suspend fun isConnected(): Boolean
    suspend fun getServerInfo(): String
    suspend fun listFiles(path: String): List<RemoteFileInfo>
    suspend fun downloadFile(
        remotePath: String, 
        localPath: String, 
        onProgress: (Long) -> Unit
    )
    suspend fun uploadFile(
        localPath: String, 
        remotePath: String, 
        onProgress: (Long) -> Unit
    )
    suspend fun deleteFile(path: String): Boolean
    suspend fun createDirectory(path: String): Boolean
    suspend fun getFileInfo(path: String): RemoteFileInfo
    suspend fun getFileSize(path: String): Long
}
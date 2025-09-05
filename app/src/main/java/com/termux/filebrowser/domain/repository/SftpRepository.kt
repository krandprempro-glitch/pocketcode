package com.termux.filebrowser.domain.repository

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.model.FileItem
import kotlinx.coroutines.flow.Flow

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val progressPercent: Int get() = ((downloadedBytes * 100) / totalBytes).toInt()
}

data class UploadProgress(
    val uploadedBytes: Long,
    val totalBytes: Long
) {
    val progressPercent: Int get() = ((uploadedBytes * 100) / totalBytes).toInt()
}

interface SftpRepository {
    suspend fun connect(config: SSHConnectionConfig): Boolean
    suspend fun disconnect()
    suspend fun isConnected(): Boolean
    suspend fun getServerInfo(): String
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun downloadFile(remotePath: String, localPath: String): Flow<DownloadProgress>
    suspend fun uploadFile(localPath: String, remotePath: String): Flow<UploadProgress>
    suspend fun deleteFile(path: String): Boolean
    suspend fun createDirectory(path: String): Boolean
    suspend fun getFileInfo(path: String): FileItem
}
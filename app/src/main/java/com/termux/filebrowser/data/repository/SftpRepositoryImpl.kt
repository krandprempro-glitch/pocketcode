package com.termux.filebrowser.data.repository

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.data.datasource.remote.SftpDataSource
import com.termux.filebrowser.data.mapper.FileItemMapper
import com.termux.filebrowser.domain.model.FileItem
import com.termux.filebrowser.domain.repository.DownloadProgress
import com.termux.filebrowser.domain.repository.SftpRepository
import com.termux.filebrowser.domain.repository.UploadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SftpRepositoryImpl(
    private val sftpDataSource: SftpDataSource,
    private val fileItemMapper: FileItemMapper
) : SftpRepository {
    
    override suspend fun connect(config: SSHConnectionConfig): Boolean {
        return sftpDataSource.connect(config)
    }
    
    override suspend fun disconnect() {
        sftpDataSource.disconnect()
    }
    
    override suspend fun isConnected(): Boolean {
        return sftpDataSource.isConnected()
    }
    
    override suspend fun getServerInfo(): String {
        return sftpDataSource.getServerInfo()
    }
    
    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val remoteFiles = sftpDataSource.listFiles(path)
        fileItemMapper.mapToFileItemList(remoteFiles)
            .sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
    
    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<DownloadProgress> = 
        flow {
            emit(DownloadProgress(0L, 1L)) // Start
            
            withContext(Dispatchers.IO) {
                val totalSize = sftpDataSource.getFileSize(remotePath)
                sftpDataSource.downloadFile(remotePath, localPath) { progress ->
                    // Progress callback - could be enhanced to emit real progress
                }
                emit(DownloadProgress(totalSize, totalSize)) // Complete
            }
        }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): Flow<UploadProgress> = 
        flow {
            emit(UploadProgress(0L, 1L)) // Start
            
            withContext(Dispatchers.IO) {
                val file = java.io.File(localPath)
                val totalSize = file.length()
                sftpDataSource.uploadFile(localPath, remotePath) { progress ->
                    // Progress callback - could be enhanced to emit real progress
                }
                emit(UploadProgress(totalSize, totalSize)) // Complete
            }
        }
    
    override suspend fun deleteFile(path: String): Boolean {
        return sftpDataSource.deleteFile(path)
    }
    
    override suspend fun createDirectory(path: String): Boolean {
        return sftpDataSource.createDirectory(path)
    }
    
    override suspend fun getFileInfo(path: String): FileItem {
        val remoteFileInfo = sftpDataSource.getFileInfo(path)
        return fileItemMapper.mapToFileItem(remoteFileInfo)
    }
}
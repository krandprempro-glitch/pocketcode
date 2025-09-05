package com.termux.filebrowser.domain.usecase.file

import com.termux.filebrowser.domain.repository.DownloadProgress
import com.termux.filebrowser.domain.repository.SftpRepository
import kotlinx.coroutines.flow.Flow

class DownloadFileUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke(remotePath: String, localPath: String): Flow<DownloadProgress> {
        return sftpRepository.downloadFile(remotePath, localPath)
    }
}
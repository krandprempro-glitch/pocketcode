package com.termux.filebrowser.domain.usecase.file

import com.termux.filebrowser.domain.model.FileItem
import com.termux.filebrowser.domain.repository.SftpRepository

class ListFilesUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke(path: String): List<FileItem> {
        return sftpRepository.listFiles(path)
    }
}
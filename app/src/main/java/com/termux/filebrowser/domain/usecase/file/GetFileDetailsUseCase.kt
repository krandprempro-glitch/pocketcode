package com.termux.filebrowser.domain.usecase.file

import com.termux.filebrowser.domain.model.FileItem
import com.termux.filebrowser.domain.repository.SftpRepository

class GetFileDetailsUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke(path: String): FileItem {
        return sftpRepository.getFileInfo(path)
    }
}
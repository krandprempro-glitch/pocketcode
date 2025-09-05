package com.termux.filebrowser.domain.usecase.connection

import com.termux.filebrowser.domain.repository.SftpRepository

class CheckConnectionUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke(): Boolean {
        return sftpRepository.isConnected()
    }
}
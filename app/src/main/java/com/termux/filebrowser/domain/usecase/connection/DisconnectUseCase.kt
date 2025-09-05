package com.termux.filebrowser.domain.usecase.connection

import com.termux.filebrowser.domain.repository.SftpRepository

class DisconnectUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke() {
        sftpRepository.disconnect()
    }
}
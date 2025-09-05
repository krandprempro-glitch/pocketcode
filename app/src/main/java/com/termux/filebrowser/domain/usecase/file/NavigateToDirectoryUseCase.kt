package com.termux.filebrowser.domain.usecase.file

import com.termux.filebrowser.domain.model.NavigationResult
import com.termux.filebrowser.domain.repository.WorkspaceRepository

class NavigateToDirectoryUseCase(
    private val listFilesUseCase: ListFilesUseCase,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(path: String): NavigationResult {
        val files = listFilesUseCase(path)
        
        // 保存导航历史
        workspaceRepository.addToNavigationHistory(path)
        
        // 保存当前路径
        workspaceRepository.saveCurrentPath(path)
        
        return NavigationResult(
            path = path,
            files = files,
            canGoBack = path != "/"
        )
    }
}
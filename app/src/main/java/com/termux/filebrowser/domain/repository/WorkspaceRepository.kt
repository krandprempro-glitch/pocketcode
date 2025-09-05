package com.termux.filebrowser.domain.repository

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.model.Workspace

interface WorkspaceRepository {
    suspend fun getOrCreateWorkspace(config: SSHConnectionConfig): Workspace
    suspend fun saveCurrentPath(path: String)
    suspend fun getCurrentPath(): String
    suspend fun addToNavigationHistory(path: String)
    suspend fun getNavigationHistory(): List<String>
    suspend fun saveExpandedDirectories(paths: Set<String>)
    suspend fun getExpandedDirectories(): Set<String>
    suspend fun updateWorkspace(workspace: Workspace)
    suspend fun getAllWorkspaces(): List<Workspace>
}
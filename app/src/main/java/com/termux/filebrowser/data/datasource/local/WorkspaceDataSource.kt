package com.termux.filebrowser.data.datasource.local

import com.termux.filebrowser.domain.model.Workspace

data class WorkspaceEntity(
    val id: String,
    val connectionId: String,
    val name: String,
    val currentPath: String,
    val navigationHistory: String, // JSON string
    val expandedDirectories: String, // JSON string  
    val lastAccessed: Long
)

interface WorkspaceDataSource {
    suspend fun saveWorkspace(workspace: WorkspaceEntity)
    suspend fun getWorkspace(connectionId: String): WorkspaceEntity?
    suspend fun getAllWorkspaces(): List<WorkspaceEntity>
    suspend fun updateWorkspace(workspace: WorkspaceEntity)
    suspend fun deleteWorkspace(workspaceId: String)
}
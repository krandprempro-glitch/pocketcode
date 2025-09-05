package com.termux.filebrowser.data.repository

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.data.datasource.local.WorkspaceDataSource
import com.termux.filebrowser.data.datasource.local.WorkspaceEntity
import com.termux.filebrowser.data.mapper.WorkspaceMapper
import com.termux.filebrowser.domain.model.Workspace
import com.termux.filebrowser.domain.repository.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkspaceRepositoryImpl(
    private val workspaceDataSource: WorkspaceDataSource,
    private val workspaceMapper: WorkspaceMapper
) : WorkspaceRepository {

    override suspend fun getOrCreateWorkspace(config: SSHConnectionConfig): Workspace = withContext(Dispatchers.IO) {
        val connectionId = "${config.host}:${config.port}:${config.username}"
        val existingWorkspace = workspaceDataSource.getWorkspace(connectionId)
        
        if (existingWorkspace != null) {
            workspaceMapper.mapToWorkspace(existingWorkspace)
        } else {
            // Create new workspace
            val newWorkspace = WorkspaceEntity(
                id = generateWorkspaceId(),
                connectionId = connectionId,
                name = "${config.host} (${config.username})",
                currentPath = "/",
                navigationHistory = "[]",
                expandedDirectories = "[]",
                lastAccessed = System.currentTimeMillis()
            )
            workspaceDataSource.saveWorkspace(newWorkspace)
            workspaceMapper.mapToWorkspace(newWorkspace)
        }
    }

    // Note: These methods would need to be associated with a specific workspace
    // For now, implementing basic versions that work with a "current" workspace
    private var currentWorkspaceId: String? = null
    
    override suspend fun saveCurrentPath(path: String): Unit = withContext(Dispatchers.IO) {
        currentWorkspaceId?.let { workspaceId ->
            val allWorkspaces = workspaceDataSource.getAllWorkspaces()
            val workspace = allWorkspaces.find { it.id == workspaceId }
            workspace?.let {
                val updatedWorkspace = it.copy(currentPath = path, lastAccessed = System.currentTimeMillis())
                workspaceDataSource.updateWorkspace(updatedWorkspace)
            }
        }
    }

    override suspend fun getCurrentPath(): String = withContext(Dispatchers.IO) {
        currentWorkspaceId?.let { workspaceId ->
            val allWorkspaces = workspaceDataSource.getAllWorkspaces()
            val workspace = allWorkspaces.find { it.id == workspaceId }
            workspace?.currentPath ?: "/"
        } ?: "/"
    }

    override suspend fun addToNavigationHistory(path: String) = withContext(Dispatchers.IO) {
        // Implement navigation history logic
        // This would parse JSON, add path, and save back
    }

    override suspend fun getNavigationHistory(): List<String> = withContext(Dispatchers.IO) {
        // Implement navigation history retrieval
        // This would parse JSON and return list
        emptyList()
    }

    override suspend fun saveExpandedDirectories(paths: Set<String>) = withContext(Dispatchers.IO) {
        // Implement expanded directories logic
        // This would convert set to JSON and save
    }

    override suspend fun getExpandedDirectories(): Set<String> = withContext(Dispatchers.IO) {
        // Implement expanded directories retrieval
        // This would parse JSON and return set
        emptySet()
    }

    override suspend fun updateWorkspace(workspace: Workspace) = withContext(Dispatchers.IO) {
        val workspaceData = workspaceMapper.mapToEntity(workspace)
        workspaceDataSource.updateWorkspace(workspaceData)
    }

    override suspend fun getAllWorkspaces(): List<Workspace> = withContext(Dispatchers.IO) {
        val workspacesData = workspaceDataSource.getAllWorkspaces()
        workspacesData.map { workspaceMapper.mapToWorkspace(it) }
    }

    // Helper methods not in interface
    suspend fun saveWorkspace(workspace: Workspace) = withContext(Dispatchers.IO) {
        val workspaceData = workspaceMapper.mapToEntity(workspace)
        workspaceDataSource.saveWorkspace(workspaceData)
    }
    
    suspend fun deleteWorkspace(workspaceId: String) = withContext(Dispatchers.IO) {
        workspaceDataSource.deleteWorkspace(workspaceId)
    }
    
    private fun generateWorkspaceId(): String {
        return "workspace_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    fun setCurrentWorkspace(workspaceId: String) {
        currentWorkspaceId = workspaceId
    }
}
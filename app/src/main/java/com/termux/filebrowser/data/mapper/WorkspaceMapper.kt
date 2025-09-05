package com.termux.filebrowser.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.filebrowser.data.datasource.local.WorkspaceEntity
import com.termux.filebrowser.domain.model.Workspace

class WorkspaceMapper {
    private val gson = Gson()
    
    fun mapToWorkspace(entity: WorkspaceEntity): Workspace {
        val historyType = object : TypeToken<List<String>>() {}.type
        val directoriesType = object : TypeToken<Set<String>>() {}.type
        
        val navigationHistory = try {
            gson.fromJson<List<String>>(entity.navigationHistory, historyType) ?: emptyList()
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        val expandedDirectories = try {
            gson.fromJson<Set<String>>(entity.expandedDirectories, directoriesType) ?: emptySet()
        } catch (e: Exception) {
            emptySet<String>()
        }
        
        return Workspace(
            id = entity.id,
            connectionId = entity.connectionId,
            name = entity.name,
            currentPath = entity.currentPath,
            navigationHistory = navigationHistory,
            expandedDirectories = expandedDirectories,
            lastAccessed = entity.lastAccessed
        )
    }
    
    fun mapToEntity(workspace: Workspace): WorkspaceEntity {
        return WorkspaceEntity(
            id = workspace.id,
            connectionId = workspace.connectionId,
            name = workspace.name,
            currentPath = workspace.currentPath,
            navigationHistory = gson.toJson(workspace.navigationHistory),
            expandedDirectories = gson.toJson(workspace.expandedDirectories),
            lastAccessed = workspace.lastAccessed
        )
    }
}
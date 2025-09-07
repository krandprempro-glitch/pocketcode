package com.termux.app.configuration.utils

import android.content.Context
import com.termux.app.managers.ProjectWorkspaceManager
import com.termux.app.models.DirectoryBookmark
import com.termux.app.models.ProjectWorkspace

object PathBookmarkHelper {
    
    /**
     * 获取指定SSH连接的收藏路径
     */
    fun getBookmarksBySSHConfig(context: Context, sshConfigId: String): List<DirectoryBookmark> {
        val workspaceManager = ProjectWorkspaceManager.getInstance(context)
        
        // 根据SSH配置ID查找对应的工作区
        val workspaces = workspaceManager.loadAllWorkspaces()
        for (workspace in workspaces) {
            if (workspace.connection != null && 
                sshConfigId == workspace.connection.name) {
                return workspaceManager.getProjectBookmarks(workspace.id)
            }
        }
        
        return emptyList()
    }
    
    /**
     * 按SSH分组收藏路径
     */
    fun groupBookmarksBySSH(context: Context): Map<String, List<DirectoryBookmark>> {
        val workspaceManager = ProjectWorkspaceManager.getInstance(context)
        val workspaces = workspaceManager.loadAllWorkspaces()
        val result = mutableMapOf<String, List<DirectoryBookmark>>()
        
        for (workspace in workspaces) {
            val connection = workspace.connection
            if (connection != null) {
                val bookmarks = workspaceManager.getProjectBookmarks(workspace.id)
                result[connection.name] = bookmarks
            }
        }
        
        return result
    }
    
    /**
     * 获取路径显示名称
     */
    fun getPathDisplayName(bookmark: DirectoryBookmark): String {
        return bookmark.name?.takeIf { it.isNotBlank() } ?: 
            // 如果没有自定义名称，使用路径的最后一段
            bookmark.fullPath.substringAfterLast("/").ifBlank { bookmark.fullPath }
    }
    
    /**
     * 获取路径的简短描述
     */
    fun getPathDescription(bookmark: DirectoryBookmark): String {
        return bookmark.fullPath
    }
    
    /**
     * 检查路径是否有效
     */
    fun isValidPath(path: String): Boolean {
        return path.isNotBlank() && path.startsWith("/")
    }
    
    /**
     * 格式化路径显示
     */
    fun formatPathForDisplay(fullPath: String, maxLength: Int = 50): String {
        if (fullPath.length <= maxLength) {
            return fullPath
        }
        
        // 显示开头和结尾部分
        val start = fullPath.take(20)
        val end = fullPath.takeLast(20)
        return "$start...$end"
    }
    
    /**
     * 获取SSH配置对应的所有可用路径
     */
    fun getAvailablePathsForSSH(context: Context, sshConfigId: String): List<Pair<String, String>> {
        val bookmarks = getBookmarksBySSHConfig(context, sshConfigId)
        return bookmarks.map { bookmark ->
            getPathDisplayName(bookmark) to bookmark.fullPath
        }
    }
}
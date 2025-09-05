package com.termux.filebrowser.presentation.state

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.model.FileItem
import com.termux.filebrowser.domain.model.Bookmark

sealed class RemoteFileBrowserUiEvent {
    // 连接相关
    data class ConnectToServer(val config: SSHConnectionConfig) : RemoteFileBrowserUiEvent()
    object Disconnect : RemoteFileBrowserUiEvent()
    
    // 导航相关
    data class NavigateToDirectory(val path: String) : RemoteFileBrowserUiEvent()
    object NavigateBack : RemoteFileBrowserUiEvent()
    object RefreshCurrentDirectory : RemoteFileBrowserUiEvent()
    
    // 文件操作
    data class SelectFile(val file: FileItem) : RemoteFileBrowserUiEvent()
    data class ToggleFileSelection(val path: String) : RemoteFileBrowserUiEvent()
    data class OpenFile(val file: FileItem) : RemoteFileBrowserUiEvent()
    data class DownloadFile(val file: FileItem) : RemoteFileBrowserUiEvent()
    
    // 书签操作
    data class AddBookmark(val path: String, val name: String) : RemoteFileBrowserUiEvent()
    data class RemoveBookmark(val bookmarkId: String) : RemoteFileBrowserUiEvent()
    data class NavigateToBookmark(val bookmark: Bookmark) : RemoteFileBrowserUiEvent()
    
    // UI操作
    object ToggleDrawer : RemoteFileBrowserUiEvent()
    object ClearError : RemoteFileBrowserUiEvent()
    object ClearSelection : RemoteFileBrowserUiEvent()
}
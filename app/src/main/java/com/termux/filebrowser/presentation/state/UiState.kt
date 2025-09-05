package com.termux.filebrowser.presentation.state

import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.model.FileItem
import com.termux.filebrowser.domain.model.Bookmark

data class RemoteFileBrowserUiState(
    val isLoading: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val currentPath: String = "/",
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val bookmarks: List<Bookmark> = emptyList(),
    val fileCount: Int = 0,
    val isDrawerOpen: Boolean = false,
    val selectedFile: FileItem? = null,
    val error: UiError? = null
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(
        val config: SSHConnectionConfig,
        val serverInfo: String
    ) : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}

sealed class UiError {
    data class ConnectionError(val message: String) : UiError()
    data class NetworkError(val message: String, val isRetryable: Boolean = true) : UiError()
    data class FileOperationError(val operation: String, val message: String) : UiError()
    data class ValidationError(val field: String, val message: String) : UiError()
    data class NavigationError(val message: String) : UiError()
}
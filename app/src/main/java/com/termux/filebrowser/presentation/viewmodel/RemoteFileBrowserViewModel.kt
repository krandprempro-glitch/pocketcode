package com.termux.filebrowser.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.models.SSHConnectionConfig
import com.termux.filebrowser.domain.usecase.bookmark.AddBookmarkUseCase
import com.termux.filebrowser.domain.usecase.bookmark.GetBookmarksUseCase
import com.termux.filebrowser.domain.usecase.bookmark.RemoveBookmarkUseCase
import com.termux.filebrowser.domain.usecase.connection.CheckConnectionUseCase
import com.termux.filebrowser.domain.usecase.connection.ConnectToServerUseCase
import com.termux.filebrowser.domain.usecase.connection.DisconnectUseCase
import com.termux.filebrowser.domain.usecase.file.DownloadFileUseCase
import com.termux.filebrowser.domain.usecase.file.NavigateToDirectoryUseCase
import com.termux.filebrowser.presentation.state.ConnectionState
import com.termux.filebrowser.presentation.state.RemoteFileBrowserUiEffect
import com.termux.filebrowser.presentation.state.RemoteFileBrowserUiEvent
import com.termux.filebrowser.presentation.state.RemoteFileBrowserUiState
import com.termux.filebrowser.presentation.state.UiError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RemoteFileBrowserViewModel(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val checkConnectionUseCase: CheckConnectionUseCase,
    private val navigateToDirectoryUseCase: NavigateToDirectoryUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val removeBookmarkUseCase: RemoveBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RemoteFileBrowserUiState())
    val uiState: StateFlow<RemoteFileBrowserUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = Channel<RemoteFileBrowserUiEffect>()
    val uiEffect: Flow<RemoteFileBrowserUiEffect> = _uiEffect.receiveAsFlow()
    
    private var currentConnectionId: String? = null
    
    fun handleEvent(event: RemoteFileBrowserUiEvent) {
        when (event) {
            is RemoteFileBrowserUiEvent.ConnectToServer -> connectToServer(event.config)
            is RemoteFileBrowserUiEvent.Disconnect -> disconnect()
            is RemoteFileBrowserUiEvent.NavigateToDirectory -> navigateToDirectory(event.path)
            is RemoteFileBrowserUiEvent.NavigateBack -> navigateBack()
            is RemoteFileBrowserUiEvent.RefreshCurrentDirectory -> refreshCurrentDirectory()
            is RemoteFileBrowserUiEvent.SelectFile -> selectFile(event.file)
            is RemoteFileBrowserUiEvent.ToggleFileSelection -> toggleFileSelection(event.path)
            is RemoteFileBrowserUiEvent.OpenFile -> openFile(event.file)
            is RemoteFileBrowserUiEvent.DownloadFile -> downloadFile(event.file)
            is RemoteFileBrowserUiEvent.AddBookmark -> addBookmark(event.path, event.name)
            is RemoteFileBrowserUiEvent.RemoveBookmark -> removeBookmark(event.bookmarkId)
            is RemoteFileBrowserUiEvent.NavigateToBookmark -> navigateToBookmark(event.bookmark)
            is RemoteFileBrowserUiEvent.ToggleDrawer -> toggleDrawer()
            is RemoteFileBrowserUiEvent.ClearError -> clearError()
            is RemoteFileBrowserUiEvent.ClearSelection -> clearSelection()
        }
    }
    
    private fun connectToServer(config: SSHConnectionConfig) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                connectionState = ConnectionState.Connecting
            )
            
            try {
                val result = connectToServerUseCase(config)
                currentConnectionId = config.host + ":" + config.port
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionState = ConnectionState.Connected(config, result.serverInfo)
                )
                
                // 连接成功后自动加载根目录和书签
                navigateToDirectory("/")
                loadBookmarks()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionState = ConnectionState.Failed(e.message ?: "连接失败"),
                    error = UiError.ConnectionError(e.message ?: "连接失败")
                )
            }
        }
    }
    
    private fun disconnect() {
        viewModelScope.launch {
            try {
                disconnectUseCase()
                currentConnectionId = null
                _uiState.value = RemoteFileBrowserUiState() // 重置为初始状态
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = UiError.NetworkError(e.message ?: "断开连接失败")
                )
            }
        }
    }
    
    private fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = navigateToDirectoryUseCase(path)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPath = path,
                    files = result.files,
                    fileCount = result.files.size,
                    selectedFiles = emptySet() // 清空选择状态
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError.NavigationError(e.message ?: "目录加载失败")
                )
            }
        }
    }
    
    private fun navigateBack() {
        val currentPath = _uiState.value.currentPath
        if (currentPath != "/" && currentPath.isNotEmpty()) {
            val parentPath = currentPath.substringBeforeLast("/").ifEmpty { "/" }
            navigateToDirectory(parentPath)
        }
    }
    
    private fun refreshCurrentDirectory() {
        navigateToDirectory(_uiState.value.currentPath)
    }
    
    private fun selectFile(file: com.termux.filebrowser.domain.model.FileItem) {
        _uiState.value = _uiState.value.copy(selectedFile = file)
    }
    
    private fun toggleFileSelection(path: String) {
        val currentSelected = _uiState.value.selectedFiles.toMutableSet()
        if (currentSelected.contains(path)) {
            currentSelected.remove(path)
        } else {
            currentSelected.add(path)
        }
        _uiState.value = _uiState.value.copy(selectedFiles = currentSelected)
    }
    
    private fun openFile(file: com.termux.filebrowser.domain.model.FileItem) {
        if (file.isDirectory) {
            navigateToDirectory(file.path)
        } else {
            viewModelScope.launch {
                _uiEffect.send(RemoteFileBrowserUiEffect.NavigateToFileViewer(file))
            }
        }
    }
    
    private fun downloadFile(file: com.termux.filebrowser.domain.model.FileItem) {
        viewModelScope.launch {
            try {
                val localPath = "/sdcard/Download/${file.name}"
                _uiEffect.send(RemoteFileBrowserUiEffect.StartFileDownload(file, localPath))
                
                // 这里可以添加下载进度的处理
                downloadFileUseCase(file.path, localPath).collect { progress ->
                    // 可以更新UI显示下载进度
                }
                
                _uiEffect.send(RemoteFileBrowserUiEffect.ShowToast("文件下载完成"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = UiError.FileOperationError("下载", e.message ?: "下载失败")
                )
            }
        }
    }
    
    private fun addBookmark(path: String, name: String) {
        currentConnectionId?.let { connectionId ->
            viewModelScope.launch {
                try {
                    val success = addBookmarkUseCase(path, name, connectionId)
                    if (success) {
                        loadBookmarks()
                        _uiEffect.send(RemoteFileBrowserUiEffect.ShowToast("书签添加成功"))
                    } else {
                        _uiEffect.send(RemoteFileBrowserUiEffect.ShowToast("书签已存在"))
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = UiError.FileOperationError("添加书签", e.message ?: "添加书签失败")
                    )
                }
            }
        }
    }
    
    private fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                val success = removeBookmarkUseCase(bookmarkId)
                if (success) {
                    loadBookmarks()
                    _uiEffect.send(RemoteFileBrowserUiEffect.ShowToast("书签删除成功"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = UiError.FileOperationError("删除书签", e.message ?: "删除书签失败")
                )
            }
        }
    }
    
    private fun navigateToBookmark(bookmark: com.termux.filebrowser.domain.model.Bookmark) {
        navigateToDirectory(bookmark.path)
        viewModelScope.launch {
            _uiEffect.send(RemoteFileBrowserUiEffect.CloseDrawer)
        }
    }
    
    private fun toggleDrawer() {
        _uiState.value = _uiState.value.copy(isDrawerOpen = !_uiState.value.isDrawerOpen)
    }
    
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedFiles = emptySet(),
            selectedFile = null
        )
    }
    
    private fun loadBookmarks() {
        currentConnectionId?.let { connectionId ->
            viewModelScope.launch {
                try {
                    val bookmarks = getBookmarksUseCase(connectionId)
                    _uiState.value = _uiState.value.copy(bookmarks = bookmarks)
                } catch (e: Exception) {
                    // 书签加载失败不是关键错误，只记录日志
                }
            }
        }
    }
}
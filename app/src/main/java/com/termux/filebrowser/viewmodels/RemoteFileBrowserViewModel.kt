package com.termux.filebrowser.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.managers.ProjectWorkspaceManager
import com.termux.app.models.DirectoryBookmark
import com.termux.app.models.ProjectWorkspace
import com.termux.app.models.RemoteFileItem
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.filebrowser.data.repository.BookmarkRepositoryImpl
import com.termux.filebrowser.data.repository.WorkspaceRepositoryImpl
import com.termux.shared.logger.Logger
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

/**
 * RemoteFileBrowserFragment的ViewModel
 * 使用MVVM架构管理UI状态和业务逻辑
 */
class RemoteFileBrowserViewModel : ViewModel() {

    companion object {
        private const val LOG_TAG = "RemoteFileBrowserViewModel"
    }

    // 数据层依赖
    private val sftpManager = SFTPConnectionManager.getInstance()
    private val compositeDisposable = CompositeDisposable()
    
    // Repository层 (将在下个批次完全集成)
    private var workspaceRepository: WorkspaceRepositoryImpl? = null
    private var bookmarkRepository: BookmarkRepositoryImpl? = null
    
    // 兼容性：使用现有管理器
    private var workspaceManager: ProjectWorkspaceManager? = null
    private var currentWorkspace: ProjectWorkspace? = null

    // UI状态管理
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _fileList = MutableStateFlow<List<RemoteFileItem>>(emptyList())
    val fileList: StateFlow<List<RemoteFileItem>> = _fileList.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 数据状态
    data class UiState(
        val isLoading: Boolean = false,
        val isEmpty: Boolean = false,
        val fileCount: Int = 0
    )

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val config: SSHConnectionConfig) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    init {
        Logger.logInfo(LOG_TAG, "ViewModel initialized")
    }

    fun initializeWithContext(context: Context) {
        // 初始化管理器（兼容性处理）
        workspaceManager = ProjectWorkspaceManager.getInstance(context)
        
        // TODO: 在完全迁移到Repository后替换这部分
        Logger.logInfo(LOG_TAG, "ViewModel initialized with context")
    }

    fun connect(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "Connecting to ${config.host}")
        
        _connectionState.value = ConnectionState.Connecting
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val disposable = sftpManager.connect(config)
            .subscribe(
                { success ->
                    if (success) {
                        onConnectionEstablished(config)
                    } else {
                        onConnectionFailed("连接失败")
                    }
                },
                { throwable ->
                    onConnectionFailed(throwable.message ?: "未知错误")
                }
            )
        
        compositeDisposable.add(disposable)
    }
    
    /**
     * 从外部同步SSH连接状态（用于MainTabActivity的悬浮按钮连接同步）
     */
    fun syncExternalConnection(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "Syncing external connection: ${config.host}")

        // 检查SFTP管理器是否已经连接
        if (sftpManager.isConnected && sftpManager.currentConfig?.host == config.host) {
            // 连接已存在，直接更新UI状态
            _connectionState.value = ConnectionState.Connected(config)
            _uiState.value = _uiState.value.copy(isLoading = false)

            // 设置工作目录
            _currentPath.value = sftpManager.currentWorkingDirectory

            // 加载或创建工作区（关键修复：确保currentWorkspace被设置）
            loadOrCreateWorkspace(config)

            // 立即开始加载文件列表
            navigateToPath(_currentPath.value)

            Logger.logInfo(LOG_TAG, "External connection synced successfully and file loading started")
        } else {
            // 连接不存在或配置不匹配，记录警告并尝试重新连接
            Logger.logError(LOG_TAG, "SFTP not connected or config mismatch, attempting to connect...")
            connect(config)
        }
    }

    fun disconnect() {
        Logger.logInfo(LOG_TAG, "Disconnecting SFTP")
        
        sftpManager.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _fileList.value = emptyList()
        _currentPath.value = "/"
        _uiState.value = UiState()
        
        currentWorkspace = null
    }

    fun navigateToPath(path: String) {
        Logger.logInfo(LOG_TAG, "Navigating to: $path")
        
        if (!sftpManager.isConnected) {
            _errorMessage.value = "未连接到服务器"
            return
        }
        
        _currentPath.value = path
        loadDirectoryContent(path)
        
        // 保存路径到工作区
        saveCurrentPathToWorkspace(path)
    }

    fun refreshCurrentDirectory() {
        Logger.logInfo(LOG_TAG, "Refreshing current directory: ${_currentPath.value}")
        
        if (!sftpManager.isConnected) {
            _errorMessage.value = "未连接到服务器"
            return
        }
        
        loadDirectoryContent(_currentPath.value)
    }

    fun addBookmark(path: String, name: String) {
        Logger.logInfo(LOG_TAG, "Adding bookmark: $name for path: $path")
        
        val workspace = currentWorkspace ?: run {
            _errorMessage.value = "工作区未就绪"
            return
        }
        
        val bookmark = DirectoryBookmark().apply {
            displayName = name
            fullPath = path
            projectId = workspace.id
            createdTime = System.currentTimeMillis()
        }
        
        workspaceManager?.addBookmark(workspace.id, bookmark)
        Logger.logInfo(LOG_TAG, "Bookmark added successfully")
    }

    fun removeBookmark(path: String) {
        Logger.logInfo(LOG_TAG, "Removing bookmark for path: $path")
        
        val workspace = currentWorkspace ?: return
        
        val bookmarks = workspaceManager?.getProjectBookmarks(workspace.id) ?: emptyList()
        val bookmark = bookmarks.find { it.fullPath == path }
        
        bookmark?.let {
            workspaceManager?.removeBookmark(it.id)
            Logger.logInfo(LOG_TAG, "Bookmark removed successfully")
        }
    }

    /**
     * 更新书签（例如重命名）。保持原书签ID不变，直接覆盖保存。
     */
    fun updateBookmark(bookmark: DirectoryBookmark) {
        val workspace = currentWorkspace ?: return
        // 确保项目ID存在
        if (bookmark.projectId == null || bookmark.projectId!!.isEmpty()) {
            bookmark.projectId = workspace.id
        }
        workspaceManager?.addBookmark(bookmark.projectId, bookmark)
        Logger.logInfo(LOG_TAG, "Bookmark updated: ${bookmark.displayName}")
    }

    fun isPathBookmarked(path: String): Boolean {
        val workspace = currentWorkspace ?: return false
        return workspaceManager?.isBookmarked(workspace.id, path) ?: false
    }

    fun getBookmarks(): List<DirectoryBookmark> {
        val workspace = currentWorkspace ?: return emptyList()
        return workspaceManager?.getProjectBookmarks(workspace.id) ?: emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun onConnectionEstablished(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "Connection established to ${config.host}")
        
        _connectionState.value = ConnectionState.Connected(config)
        _uiState.value = _uiState.value.copy(isLoading = false)
        
        // 加载或创建工作区
        loadOrCreateWorkspace(config)
        
        // 导航到用户家目录（SFTP返回的当前工作目录），回退到根目录
        val startPath = try {
            val cwd = sftpManager.currentWorkingDirectory
            if (cwd.isNullOrBlank()) "/" else cwd
        } catch (e: Exception) {
            "/"
        }
        navigateToPath(startPath)
    }

    private fun onConnectionFailed(error: String) {
        Logger.logError(LOG_TAG, "Connection failed: $error")
        
        _connectionState.value = ConnectionState.Error(error)
        _uiState.value = _uiState.value.copy(isLoading = false)
        _errorMessage.value = error
    }

    private fun loadDirectoryContent(path: String) {
        Logger.logInfo(LOG_TAG, "Loading directory content: $path")
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val disposable = sftpManager.listFiles(path)
            .subscribe(
                { files ->
                    Logger.logInfo(LOG_TAG, "Loaded ${files.size} files from $path")
                    
                    _fileList.value = files
                    _uiState.value = UiState(
                        isLoading = false,
                        isEmpty = files.isEmpty(),
                        fileCount = files.size
                    )
                },
                { throwable ->
                    Logger.logError(LOG_TAG, "Failed to load directory: ${throwable.message}")
                    
                    _uiState.value = _uiState.value.copy(isLoading = false, isEmpty = true)
                    _errorMessage.value = "加载目录失败: ${throwable.message}"
                }
            )
        
        compositeDisposable.add(disposable)
    }

    private fun loadOrCreateWorkspace(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "Loading workspace for ${config.host}")
        
        currentWorkspace = workspaceManager?.getOrCreateWorkspaceForConnection(config)
        
        currentWorkspace?.let { workspace ->
            Logger.logInfo(LOG_TAG, "Workspace loaded: ${workspace.id}")
            
            // 恢复上次的路径
            val savedPath = workspace.currentPath
            if (!savedPath.isNullOrEmpty() && savedPath != "/") {
                _currentPath.value = savedPath
            }
            
            // 更新最后访问时间
            workspaceManager?.updateLastAccess(workspace.id)
        } ?: run {
            Logger.logError(LOG_TAG, "Failed to create workspace")
            _errorMessage.value = "工作区创建失败"
        }
    }

    private fun saveCurrentPathToWorkspace(path: String) {
        currentWorkspace?.let { workspace ->
            workspace.currentPath = path
            workspaceManager?.saveWorkspaceState(workspace)
        }
    }
    
    /**
     * 读取远程文件内容
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    @Throws(Exception::class)
    suspend fun readFileContent(filePath: String): String {
        Logger.logInfo(LOG_TAG, "Reading file content: $filePath")
        
        if (!sftpManager.isConnected) {
            throw IllegalStateException("未连接到服务器")
        }
        
        return try {
            // 使用RxJava Single转换为挂起函数
            sftpManager.readFileContent(filePath).await()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to read file content: ${e.message}")
            throw e
        }
    }

    /**
     * 读取远程文件字节（用于图片预览）
     */
    @Throws(Exception::class)
    suspend fun readFileBytes(filePath: String): ByteArray {
        Logger.logInfo(LOG_TAG, "Reading file bytes: $filePath")

        if (!sftpManager.isConnected) {
            throw IllegalStateException("未连接到服务器")
        }

        return try {
            sftpManager.readFileBytes(filePath).await()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to read file bytes: ${e.message}")
            throw e
        }
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return sftpManager.isConnected
    }

    override fun onCleared() {
        super.onCleared()
        Logger.logInfo(LOG_TAG, "ViewModel cleared")
        
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
        
        if (sftpManager.isConnected) {
            sftpManager.disconnect()
        }
    }
}


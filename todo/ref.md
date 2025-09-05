# RemoteFileBrowserFragment Kotlin MVVM 重构方案

## 项目概述

本文档详细说明了将 `RemoteFileBrowserFragment.java`（2204行）重构为现代化 Kotlin MVVM 架构的完整方案。

## 现状分析

### 当前问题

#### 1. 代码复杂度问题
- **单文件过大**: 2204行代码，远超单个类的合理范围（200-500行）
- **职责混乱**: 一个Fragment承担了以下职责：
  - UI控制和事件处理
  - SFTP连接管理
  - 文件系统操作
  - 书签管理
  - 抽屉菜单逻辑
  - 工作区状态管理
  - 错误处理和网络重试

#### 2. 架构问题
- **缺乏分层**: 业务逻辑、数据访问、UI逻辑全部混合在Fragment中
- **紧耦合**: 组件间依赖复杂，难以独立测试
- **状态管理混乱**: 手动维护各种UI状态标志位
- **内存泄漏风险**: RxJava3订阅管理复杂，容易造成内存泄漏

#### 3. 维护性问题
- **代码重复**: 相似的操作逻辑在多处重复
- **硬编码严重**: UI操作直接耦合在业务逻辑中
- **扩展困难**: 添加新功能需要修改核心Fragment
- **测试困难**: 无法进行有效的单元测试

### 技术债务分析

1. **Java语言限制**: 缺乏现代语言特性（协程、密封类、空安全等）
2. **RxJava3复杂性**: 异步操作管理复杂，学习成本高
3. **Fragment臃肿**: 违反单一职责原则
4. **状态同步复杂**: 多个UI组件状态同步困难

## 重构目标

### 架构目标
1. **MVVM架构**: 清晰的View-ViewModel-Repository分层
2. **响应式编程**: 使用Kotlin协程和Flow简化异步操作
3. **状态管理**: 统一的UI状态管理机制
4. **依赖注入**: 降低组件间耦合度
5. **可测试性**: 每个组件都可独立测试

### 技术目标
1. **Kotlin迁移**: 享受现代语言特性带来的便利
2. **协程替换**: 用Kotlin协程替换RxJava3
3. **模块化**: 创建独立的功能模块
4. **类型安全**: 利用Kotlin强类型系统避免运行时错误

### 质量目标
1. **代码行数**: 单个类控制在200行以内
2. **圈复杂度**: 每个方法复杂度小于10
3. **测试覆盖率**: 核心业务逻辑覆盖率达到80%以上
4. **性能优化**: 减少内存占用，提升响应速度

## 新架构设计

### 整体架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │     Domain      │    │      Data       │
│                 │    │                 │    │                 │
│  • Fragment     │────│  • UseCase      │────│  • Repository   │
│  • ViewModel    │    │  • Model        │    │  • DataSource   │
│  • Adapter      │    │  • Repository   │    │  • Mapper       │
│                 │    │    Interface    │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 模块结构

```
remote-file-browser/
├── build.gradle.kts
├── proguard-rules.pro
└── src/main/kotlin/com/termux/filebrowser/
    ├── presentation/           # 表示层 - UI相关
    │   ├── ui/
    │   │   ├── RemoteFileBrowserFragment.kt      # 精简的Fragment
    │   │   ├── adapter/
    │   │   │   ├── FileListAdapter.kt
    │   │   │   ├── FileTreeAdapter.kt
    │   │   │   └── BookmarkAdapter.kt
    │   │   ├── dialog/
    │   │   │   ├── SSHConfigDialog.kt
    │   │   │   ├── FileOperationDialog.kt
    │   │   │   └── BookmarkDialog.kt
    │   │   └── component/
    │   │       ├── PathBreadcrumb.kt
    │   │       └── ConnectionStatusView.kt
    │   ├── viewmodel/
    │   │   ├── RemoteFileBrowserViewModel.kt     # 主ViewModel
    │   │   ├── FileTreeViewModel.kt              # 目录树ViewModel
    │   │   ├── ConnectionViewModel.kt            # 连接管理ViewModel
    │   │   └── BookmarkViewModel.kt              # 书签管理ViewModel
    │   └── state/
    │       ├── UiState.kt                        # UI状态定义
    │       ├── UiEvent.kt                        # UI事件定义
    │       └── UiEffect.kt                       # UI副作用定义
    ├── domain/                 # 业务层 - 纯业务逻辑
    │   ├── model/
    │   │   ├── FileItem.kt
    │   │   ├── Connection.kt
    │   │   ├── Workspace.kt
    │   │   └── Bookmark.kt
    │   ├── usecase/
    │   │   ├── connection/
    │   │   │   ├── ConnectToServerUseCase.kt
    │   │   │   ├── DisconnectUseCase.kt
    │   │   │   └── CheckConnectionUseCase.kt
    │   │   ├── file/
    │   │   │   ├── ListFilesUseCase.kt
    │   │   │   ├── NavigateToDirectoryUseCase.kt
    │   │   │   ├── DownloadFileUseCase.kt
    │   │   │   └── GetFileDetailsUseCase.kt
    │   │   └── bookmark/
    │   │       ├── AddBookmarkUseCase.kt
    │   │       ├── RemoveBookmarkUseCase.kt
    │   │       ├── GetBookmarksUseCase.kt
    │   │       └── ToggleBookmarkUseCase.kt
    │   └── repository/         # Repository接口定义
    │       ├── SftpRepository.kt
    │       ├── WorkspaceRepository.kt
    │       └── BookmarkRepository.kt
    ├── data/                   # 数据层 - 数据访问
    │   ├── repository/         # Repository实现
    │   │   ├── SftpRepositoryImpl.kt
    │   │   ├── WorkspaceRepositoryImpl.kt
    │   │   └── BookmarkRepositoryImpl.kt
    │   ├── datasource/
    │   │   ├── remote/
    │   │   │   ├── SftpDataSource.kt             # SFTP操作封装
    │   │   │   └── SftpClient.kt                 # SFTP客户端管理
    │   │   └── local/
    │   │       ├── WorkspaceDataSource.kt        # 本地工作区数据
    │   │       └── BookmarkDataSource.kt         # 本地书签数据
    │   └── mapper/             # 数据转换
    │       ├── FileItemMapper.kt
    │       ├── ConnectionMapper.kt
    │       └── BookmarkMapper.kt
    └── di/                     # 依赖注入
        └── FileBrowserModule.kt
```

## 核心组件设计

### 1. UI状态管理

#### UiState定义
```kotlin
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
```

#### UiEvent定义
```kotlin
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
```

#### UiEffect定义
```kotlin
sealed class RemoteFileBrowserUiEffect {
    object CloseDrawer : RemoteFileBrowserUiEffect()
    data class ShowToast(val message: String) : RemoteFileBrowserUiEffect()
    data class ShowErrorDialog(val error: UiError) : RemoteFileBrowserUiEffect()
    data class NavigateToFileViewer(val file: FileItem) : RemoteFileBrowserUiEffect()
    data class StartFileDownload(val file: FileItem, val localPath: String) : RemoteFileBrowserUiEffect()
}
```

### 2. ViewModel设计

#### 主ViewModel
```kotlin
class RemoteFileBrowserViewModel(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val listFilesUseCase: ListFilesUseCase,
    private val navigateToDirectoryUseCase: NavigateToDirectoryUseCase,
    private val bookmarkUseCase: BookmarkUseCase,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RemoteFileBrowserUiState())
    val uiState: StateFlow<RemoteFileBrowserUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = Channel<RemoteFileBrowserUiEffect>()
    val uiEffect: Flow<RemoteFileBrowserUiEffect> = _uiEffect.receiveAsFlow()
    
    fun handleEvent(event: RemoteFileBrowserUiEvent) {
        when (event) {
            is RemoteFileBrowserUiEvent.ConnectToServer -> connectToServer(event.config)
            is RemoteFileBrowserUiEvent.NavigateToDirectory -> navigateToDirectory(event.path)
            is RemoteFileBrowserUiEvent.RefreshCurrentDirectory -> refreshCurrentDirectory()
            is RemoteFileBrowserUiEvent.AddBookmark -> addBookmark(event.path, event.name)
            // ... 其他事件处理
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionState = ConnectionState.Connected(config, result.serverInfo)
                )
                // 连接成功后自动加载根目录
                navigateToDirectory("/")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionState = ConnectionState.Failed(e.message ?: "连接失败"),
                    error = UiError.ConnectionError(e.message ?: "连接失败")
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
                
                // 保存当前路径到工作区
                workspaceRepository.saveCurrentPath(path)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError.NavigationError(e.message ?: "目录加载失败")
                )
            }
        }
    }
}
```

### 3. UseCase设计

#### 连接用例
```kotlin
class ConnectToServerUseCase(
    private val sftpRepository: SftpRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(config: SSHConnectionConfig): ConnectionResult {
        // 建立SFTP连接
        val connected = sftpRepository.connect(config)
        if (!connected) {
            throw ConnectionException("无法建立SFTP连接")
        }
        
        // 获取服务器信息
        val serverInfo = sftpRepository.getServerInfo()
        
        // 创建或获取工作区
        val workspace = workspaceRepository.getOrCreateWorkspace(config)
        
        return ConnectionResult(
            isConnected = true,
            serverInfo = "${config.host}:${config.port}",
            workspace = workspace
        )
    }
}

data class ConnectionResult(
    val isConnected: Boolean,
    val serverInfo: String,
    val workspace: Workspace
)
```

#### 文件操作用例
```kotlin
class ListFilesUseCase(
    private val sftpRepository: SftpRepository
) {
    suspend operator fun invoke(path: String): List<FileItem> {
        return sftpRepository.listFiles(path)
            .sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
}

class NavigateToDirectoryUseCase(
    private val listFilesUseCase: ListFilesUseCase,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(path: String): NavigationResult {
        val files = listFilesUseCase(path)
        
        // 保存导航历史
        workspaceRepository.addToNavigationHistory(path)
        
        return NavigationResult(
            path = path,
            files = files,
            canGoBack = path != "/"
        )
    }
}

data class NavigationResult(
    val path: String,
    val files: List<FileItem>,
    val canGoBack: Boolean
)
```

### 4. Repository设计

#### SFTP Repository
```kotlin
interface SftpRepository {
    suspend fun connect(config: SSHConnectionConfig): Boolean
    suspend fun disconnect()
    suspend fun isConnected(): Boolean
    suspend fun getServerInfo(): String
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun downloadFile(remotePath: String, localPath: String): Flow<DownloadProgress>
    suspend fun uploadFile(localPath: String, remotePath: String): Flow<UploadProgress>
    suspend fun deleteFile(path: String): Boolean
    suspend fun createDirectory(path: String): Boolean
    suspend fun getFileInfo(path: String): FileItem
}

class SftpRepositoryImpl(
    private val sftpDataSource: SftpDataSource,
    private val fileItemMapper: FileItemMapper
) : SftpRepository {
    
    override suspend fun connect(config: SSHConnectionConfig): Boolean {
        return sftpDataSource.connect(config)
    }
    
    override suspend fun listFiles(path: String): List<FileItem> {
        val remoteFiles = sftpDataSource.listFiles(path)
        return remoteFiles.map { fileItemMapper.mapToFileItem(it) }
    }
    
    // ... 其他方法实现
}
```

#### 工作区Repository
```kotlin
interface WorkspaceRepository {
    suspend fun getOrCreateWorkspace(config: SSHConnectionConfig): Workspace
    suspend fun saveCurrentPath(path: String)
    suspend fun getCurrentPath(): String
    suspend fun addToNavigationHistory(path: String)
    suspend fun getNavigationHistory(): List<String>
    suspend fun saveExpandedDirectories(paths: Set<String>)
    suspend fun getExpandedDirectories(): Set<String>
}
```

### 5. Fragment精简设计

```kotlin
class RemoteFileBrowserFragment : Fragment() {
    
    private var _binding: FragmentRemoteFileBrowserBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RemoteFileBrowserViewModel by viewModels()
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var bookmarkAdapter: BookmarkAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemoteFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupAdapters()
        observeViewModel()
    }
    
    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.setNavigationOnClickListener {
            viewModel.handleEvent(RemoteFileBrowserUiEvent.ToggleDrawer)
        }
        
        // 设置下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.handleEvent(RemoteFileBrowserUiEvent.RefreshCurrentDirectory)
        }
    }
    
    private fun setupAdapters() {
        fileListAdapter = FileListAdapter { file ->
            when (file.isDirectory) {
                true -> viewModel.handleEvent(RemoteFileBrowserUiEvent.NavigateToDirectory(file.path))
                false -> viewModel.handleEvent(RemoteFileBrowserUiEvent.OpenFile(file))
            }
        }
        binding.recyclerViewFiles.adapter = fileListAdapter
        
        bookmarkAdapter = BookmarkAdapter { bookmark ->
            viewModel.handleEvent(RemoteFileBrowserUiEvent.NavigateToBookmark(bookmark))
        }
        binding.recyclerViewBookmarks.adapter = bookmarkAdapter
    }
    
    private fun observeViewModel() {
        // 观察UI状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
        
        // 观察UI副作用
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEffect.collect { effect ->
                handleUiEffect(effect)
            }
        }
    }
    
    private fun updateUI(state: RemoteFileBrowserUiState) {
        // 更新加载状态
        binding.swipeRefresh.isRefreshing = state.isLoading
        binding.progressBar.isVisible = state.isLoading
        
        // 更新连接状态
        updateConnectionStatus(state.connectionState)
        
        // 更新文件列表
        fileListAdapter.submitList(state.files)
        
        // 更新书签列表
        bookmarkAdapter.submitList(state.bookmarks)
        
        // 更新路径显示
        binding.textCurrentPath.text = state.currentPath
        
        // 更新文件计数
        binding.textFileCount.text = "${state.fileCount} 项"
        
        // 更新选择状态
        binding.textSelectionInfo.text = when (state.selectedFiles.size) {
            0 -> ""
            else -> "已选择 ${state.selectedFiles.size} 项"
        }
        
        // 处理错误
        state.error?.let { error ->
            showError(error)
        }
    }
    
    private fun handleUiEffect(effect: RemoteFileBrowserUiEffect) {
        when (effect) {
            is RemoteFileBrowserUiEffect.ShowToast -> {
                Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
            }
            is RemoteFileBrowserUiEffect.ShowErrorDialog -> {
                showErrorDialog(effect.error)
            }
            is RemoteFileBrowserUiEffect.CloseDrawer -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            // ... 处理其他副作用
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## 实施计划

### 阶段1：基础架构搭建（2天）

#### 1.1 创建新模块
- [ ] 创建 `remote-file-browser` 模块
- [ ] 配置 `build.gradle.kts`
- [ ] 添加必要依赖（Kotlin协程、Fragment-KTX、Lifecycle等）
- [ ] 配置ProGuard规则

#### 1.2 基础类创建
- [ ] 创建 `UiState`、`UiEvent`、`UiEffect` 数据类
- [ ] 创建基础的 `ViewModel` 类
- [ ] 创建基础的 `Repository` 接口
- [ ] 创建依赖注入模块

### 阶段2：数据层重构（3天）

#### 2.1 Repository接口定义
- [ ] 定义 `SftpRepository` 接口
- [ ] 定义 `WorkspaceRepository` 接口  
- [ ] 定义 `BookmarkRepository` 接口

#### 2.2 DataSource重构
- [ ] 将 `SFTPConnectionManager` 重构为 `SftpDataSource`
- [ ] 将 `ProjectWorkspaceManager` 重构为 `WorkspaceDataSource`
- [ ] 创建数据映射器（Mapper）

#### 2.3 Repository实现
- [ ] 实现 `SftpRepositoryImpl`
- [ ] 实现 `WorkspaceRepositoryImpl`
- [ ] 实现 `BookmarkRepositoryImpl`

### 阶段3：业务层构建（3天）

#### 3.1 UseCase创建
- [ ] 实现连接相关UseCase（Connect、Disconnect、CheckConnection）
- [ ] 实现文件操作UseCase（ListFiles、Navigate、Download、Upload）
- [ ] 实现书签管理UseCase（Add、Remove、List、Toggle）

#### 3.2 业务模型
- [ ] 创建 Kotlin 数据类（FileItem、Connection、Workspace、Bookmark）
- [ ] 实现业务规则和验证逻辑
- [ ] 创建异常类型定义

#### 3.3 错误处理
- [ ] 统一的异常处理机制
- [ ] 网络错误处理和重试逻辑
- [ ] 用户友好的错误消息

### 阶段4：表示层重构（4天）

#### 4.1 ViewModel实现
- [ ] 实现 `RemoteFileBrowserViewModel`
- [ ] 实现 `FileTreeViewModel`（如果需要）
- [ ] 实现 `ConnectionViewModel`
- [ ] 实现 `BookmarkViewModel`

#### 4.2 Fragment精简
- [ ] 重构Fragment，移除业务逻辑
- [ ] 实现状态观察和UI更新
- [ ] 实现用户交互事件处理

#### 4.3 适配器重构
- [ ] 重构 `FileListAdapter` 为现代化实现
- [ ] 重构 `FileTreeAdapter`（如果需要）
- [ ] 重构 `BookmarkAdapter`

#### 4.4 UI组件
- [ ] 创建自定义UI组件（路径导航、连接状态显示等）
- [ ] 实现对话框的Kotlin版本
- [ ] 优化UI响应和动画效果

### 阶段5：测试和优化（3天）

#### 5.1 单元测试
- [ ] ViewModel测试
- [ ] UseCase测试  
- [ ] Repository测试
- [ ] 数据映射测试

#### 5.2 集成测试
- [ ] 端到端功能测试
- [ ] UI交互测试
- [ ] 网络连接测试

#### 5.3 性能优化
- [ ] 内存泄漏检查和优化
- [ ] 网络请求优化
- [ ] UI渲染性能优化
- [ ] 启动速度优化

#### 5.4 文档完善
- [ ] API文档
- [ ] 架构文档
- [ ] 使用指南
- [ ] 迁移指南

## 技术细节

### Kotlin特性利用

#### 1. 协程和Flow
```kotlin
class SftpRepositoryImpl : SftpRepository {
    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        sftpClient.listFiles(path).map { mapper.toFileItem(it) }
    }
    
    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<DownloadProgress> = 
        flow {
            val totalSize = sftpClient.getFileSize(remotePath)
            var downloadedSize = 0L
            
            sftpClient.downloadFile(remotePath, localPath) { progress ->
                downloadedSize += progress
                emit(DownloadProgress(downloadedSize, totalSize))
            }
        }.flowOn(Dispatchers.IO)
}
```

#### 2. 密封类和数据类
```kotlin
sealed class UiError {
    data class ConnectionError(val message: String) : UiError()
    data class NetworkError(val message: String, val isRetryable: Boolean = true) : UiError()
    data class FileOperationError(val operation: String, val message: String) : UiError()
    data class ValidationError(val field: String, val message: String) : UiError()
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val permissions: String = "",
    val owner: String = "",
    val group: String = ""
) {
    val displaySize: String get() = when (isDirectory) {
        true -> ""
        false -> formatFileSize(size)
    }
    
    val displayDate: String get() = formatDate(lastModified)
}
```

#### 3. 扩展函数
```kotlin
fun String.optimizePathDisplay(maxLength: Int = 40): String {
    if (length <= maxLength) return this
    
    val parts = split("/")
    if (parts.size <= 2) return this
    
    val fileName = parts.last()
    val parentDir = parts[parts.size - 2]
    return ".../$parentDir/$fileName"
}

fun Long.formatFileSize(): String = when {
    this < 1024L -> "$this B"
    this < 1024L * 1024 -> "${this / 1024} KB"
    this < 1024L * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
    else -> "${this / (1024 * 1024 * 1024)} GB"
}
```

### 架构模式

#### 1. 单向数据流
```
User Interaction → UiEvent → ViewModel → UseCase → Repository → DataSource
                                    ↓
UI Update ← UiState ← ViewModel ← UseCase Result ← Repository ← DataSource
```

#### 2. 依赖注入
```kotlin
object FileBrowserModule {
    
    @Provides
    @Singleton
    fun provideSftpRepository(
        sftpDataSource: SftpDataSource,
        mapper: FileItemMapper
    ): SftpRepository = SftpRepositoryImpl(sftpDataSource, mapper)
    
    @Provides
    @Singleton  
    fun provideSftpDataSource(): SftpDataSource = SftpDataSourceImpl()
    
    @Provides
    fun provideConnectToServerUseCase(
        sftpRepository: SftpRepository,
        workspaceRepository: WorkspaceRepository
    ): ConnectToServerUseCase = ConnectToServerUseCase(sftpRepository, workspaceRepository)
}
```

### 性能优化策略

#### 1. 内存管理
- 使用 `StateFlow` 和 `SharedFlow` 管理状态，避免内存泄漏
- 适配器使用 `DiffUtil` 优化列表更新
- 图片加载使用缓存和懒加载
- 及时释放不需要的资源

#### 2. 网络优化
- 实现连接池管理，复用SFTP连接
- 使用协程实现并发文件操作
- 实现智能重试机制
- 添加请求超时和取消机制

#### 3. UI优化
- 使用 `ViewBinding` 减少 findViewById 调用
- 实现列表项复用和优化
- 使用适当的动画和过渡效果
- 避免在主线程执行重操作

## 兼容性考虑

### 1. 渐进式迁移
- 新模块可与现有代码共存
- 保持现有API接口兼容
- 逐步替换现有功能模块

### 2. 数据迁移
- 兼容现有的数据库结构
- 提供数据迁移工具
- 保持用户设置和书签

### 3. 功能对等
- 确保所有现有功能都能在新架构中实现
- 保持用户体验一致性
- 提供功能增强而非减少

## 预期收益

### 代码质量提升
- **可读性**: Kotlin语法更简洁，代码更易理解
- **可维护性**: 清晰的架构分层，易于修改和扩展
- **可测试性**: 每个组件可独立测试，提升代码质量
- **类型安全**: Kotlin强类型系统减少运行时错误

### 开发效率提升
- **新功能开发**: 明确的架构指导，开发更快
- **Bug修复**: 问题定位更准确，修复更高效
- **代码审查**: 统一的代码结构，审查更容易
- **团队协作**: 清晰的分工界限，协作更顺畅

### 用户体验提升
- **性能优化**: 协程和Flow提供更好的性能
- **响应速度**: 优化的状态管理提升UI响应
- **稳定性**: 更好的错误处理和恢复机制
- **功能丰富**: 架构支持更多高级功能

### 长期维护
- **技术债务减少**: 现代化的架构减少技术债务
- **扩展性**: 易于添加新功能和集成新技术
- **可维护性**: 降低维护成本和风险
- **团队学习**: 现代Android开发最佳实践

## 总结

这个重构方案将 `RemoteFileBrowserFragment` 从一个2204行的巨型类，重构为遵循MVVM架构的现代化Kotlin代码。通过合理的分层设计、状态管理和依赖注入，我们将获得更好的代码质量、开发效率和用户体验。

重构将分5个阶段进行，每个阶段都有明确的目标和可验证的成果。整个过程预计需要15天左右的开发时间，但长期收益远超过短期投入。

这不仅是一次技术重构，更是向现代Android开发实践的全面升级，为项目的长期发展奠定坚实基础。
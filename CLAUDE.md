# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a modified version of the Termux Android terminal application with added remote file browsing functionality. The app provides SSH/SFTP connections and includes a VSCode-style remote file browser with project workspace management, directory tree navigation, and bookmark features.

## Build Commands

### Building the App
```bash
# Build debug APK
./gradlew app:assembleDebug

# Build release APK  
./gradlew app:assembleRelease

# Clean build
./gradlew clean

# Build without Gradle daemon
./gradlew app:compileDebugJavaWithJavac --no-daemon
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run tests with Android resources
./gradlew testDebugUnitTest
```

### Code Quality
```bash
# Check for lint issues
./gradlew lint

# Generate version name
./gradlew versionName
```

## Architecture Overview

### Core Components

1. **TermuxApplication** - Main application class that initializes crash handling, logging, bootstrap setup, and shell environment
2. **MainTabActivity** - Tab-based main interface with SSH connection, file browsing, Git changes, and settings tabs
3. **RemoteFileBrowserActivity** - VSCode-style remote file browser with drawer navigation, directory tree, and file operations
4. **TermuxService** - Background service for terminal operations
5. **TermuxActivity** - Original terminal interface

### Key Packages

- `com.termux.app` - Main application activities and services
- `com.termux.app.fragments` - UI fragments for SSH connection and file browsing
- `com.termux.app.models` - Data models for SSH config, file items, workspace management
- `com.termux.app.adapters` - RecyclerView adapters for file lists and directory trees
- `com.termux.app.managers` - Business logic managers for projects and workspaces
- `com.termux.app.sftp` - SFTP connection management
- `com.termux.shared` - Shared utilities and constants across modules

### Module Structure

- **app/** - Main Android application module
- **terminal-view/** - Terminal view implementation
- **terminal-emulator/** - Terminal emulation logic  
- **termux-shared/** - Shared libraries and utilities

## Development Guidelines

### Adding New Features

When adding new functionality:

1. Follow existing package structure under `com.termux.app`
2. Use the existing `TermuxConstants` class for configuration values
3. Implement proper error handling with `Logger` class
4. Use RxJava for asynchronous operations (already included)
5. Follow Material Design guidelines for UI components

### Working with SFTP Features

The remote file browser uses:
- **SSHJ library** (com.hierynomus:sshj) for SSH/SFTP connections
- **RxJava3** for async operations
- **Gson** for JSON serialization of configs and workspaces
- **RecyclerView** with custom adapters for directory trees

Key classes:
- `SFTPConnectionManager` - Manages SFTP connections
- `ProjectWorkspaceManager` - Handles project workspaces and bookmarks
- `RemoteFileBrowserAdapter` - File list display
- `FileTreeAdapter` - VSCode-style directory tree

### Configuration Files

- **gradle.properties** - Build configuration (SDK versions, memory settings)
- **app/build.gradle** - App-specific dependencies and build settings
- **proguard-rules.pro** - Code obfuscation rules
- Bootstrap packages are downloaded dynamically based on architecture

### Key Dependencies

- AndroidX libraries for UI components
- RxJava3 for reactive programming
- SSHJ for SSH/SFTP connections
- Gson for JSON processing
- Material Design components
- Markwon for markdown rendering

### Testing

The project uses:
- JUnit 4 for unit tests
- Robolectric for Android unit tests
- Tests should be placed in `app/src/test/java/`

### Build Variants

- **Debug builds** - Use test signing key, have debugging enabled
- **Release builds** - Minified, production-ready
- **Package variants** - Support for different Android versions (apt-android-7, apt-android-5)

### Important Notes

- The app uses a shared user ID system with Termux plugins
- Bootstrap packages are architecture-specific and downloaded at build time
- SSH connections support both password and key-based authentication
- The remote file browser maintains project workspaces with persistent state
- Directory trees use lazy loading for performance with large remote directories

### Chinese Language Support

This fork includes Chinese UI text and comments. When working with the codebase:
- UI strings may be in Chinese in the source code
- Comments may be in Chinese, especially in newer remote file browser components
- Follow existing patterns for internationalization if adding new strings

## Android专业开发规范

作为资深Android开发者，在此项目中工作时必须遵循以下企业级开发标准和最佳实践。

### MVVM架构实现指南

#### 核心架构原则
- **严格分层**: View(Activity/Fragment) → ViewModel → Repository → DataSource
- **单向数据流**: UI状态通过LiveData/StateFlow传递，用户操作通过ViewModel处理
- **依赖注入**: 使用构造函数注入或Dagger/Hilt进行依赖管理
- **响应式编程**: 结合RxJava3(已集成)实现数据流管理

#### ViewModel设计标准
```kotlin
class RemoteFileBrowserViewModel(
    private val sftpRepository: SFTPRepository,
    private val workspaceManager: ProjectWorkspaceManager
) : ViewModel() {
    
    private val _uiState = MutableLiveData<FileBrowserUiState>()
    val uiState: LiveData<FileBrowserUiState> = _uiState
    
    private val _navigationEvents = MutableLiveData<NavigationEvent>()
    val navigationEvents: LiveData<NavigationEvent> = _navigationEvents
    
    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            try {
                val files = sftpRepository.listFiles(path)
                _uiState.value = FileBrowserUiState.Success(files)
            } catch (e: Exception) {
                _uiState.value = FileBrowserUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

#### Repository层实现
- **单一职责**: 每个Repository负责特定数据源的管理
- **错误处理**: 统一异常处理和错误映射
- **缓存策略**: 实现合理的本地缓存和数据同步
- **协程使用**: 正确使用suspend函数和Flow进行异步操作

### Android版本适配策略

#### API级别兼容性
- **目标SDK**: 始终使用最新稳定版本(当前项目targetSdk=28，建议升级至33+)
- **最低SDK**: 保持minSdk=21，确保95%+设备覆盖率
- **行为变更**: 每个API级别的行为变更都要进行适配测试

#### 权限处理最佳实践
```kotlin
// 运行时权限请求
class PermissionManager {
    fun requestStoragePermission(activity: ComponentActivity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 分区存储
                requestManageExternalStoragePermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ 运行时权限
                requestLegacyStoragePermission()
            }
        }
    }
}
```

#### 设备兼容性
- **屏幕适配**: 支持多种屏幕尺寸和密度
- **性能分级**: 根据设备性能调整功能复杂度
- **网络状态**: 适配不同网络环境和连接状态

### Material Design UI设计规范

#### 设计系统构建
- **主题系统**: 使用Material Design 3规范
- **颜色系统**: 定义完整的颜色语义化体系
- **字体规范**: 遵循Material Typography标准
- **间距系统**: 使用8dp基础网格系统

#### 组件使用标准
```xml
<!-- 使用Material组件而非原生组件 -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.MaterialComponents.Button.UnelevatedButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/action_connect" />

<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>
```

#### 动画与过渡
- **共享元素**: 实现页面间平滑过渡
- **Material Motion**: 使用标准的Material动画曲线
- **反馈动画**: 为用户交互提供即时视觉反馈
- **性能优化**: 确保动画在低端设备上流畅运行

### 代码质量与性能优化

#### 内存管理
- **生命周期感知**: 正确处理Activity/Fragment生命周期
- **内存泄漏预防**: 避免Context泄漏、静态引用泄漏
- **资源释放**: 及时释放Bitmap、Cursor、流等资源
- **缓存策略**: 实现LRU缓存和磁盘缓存

#### 网络优化
```kotlin
// SFTP连接池管理
class SFTPConnectionPool {
    private val connectionPool = mutableMapOf<String, SFTPClient>()
    
    suspend fun getConnection(config: SSHConnectionConfig): SFTPClient {
        val key = "${config.host}:${config.port}:${config.username}"
        return connectionPool.getOrPut(key) {
            createSFTPConnection(config)
        }
    }
    
    fun releaseConnection(key: String) {
        connectionPool[key]?.disconnect()
        connectionPool.remove(key)
    }
}
```

#### UI性能
- **RecyclerView优化**: 使用DiffUtil、ViewHolder复用、预加载
- **图片加载**: 实现图片懒加载和缓存机制
- **布局优化**: 减少过度绘制、优化布局层级
- **异步处理**: 将耗时操作移至后台线程

### 测试与调试最佳实践

#### 单元测试
```kotlin
@RunWith(MockitoJUnitRunner::class)
class RemoteFileBrowserViewModelTest {
    
    @Mock
    private lateinit var sftpRepository: SFTPRepository
    
    @Mock  
    private lateinit var workspaceManager: ProjectWorkspaceManager
    
    private lateinit var viewModel: RemoteFileBrowserViewModel
    
    @Before
    fun setup() {
        viewModel = RemoteFileBrowserViewModel(sftpRepository, workspaceManager)
    }
    
    @Test
    fun `loadDirectory should update ui state correctly`() = runTest {
        // Given
        val testFiles = listOf(RemoteFileItem("test.txt", FileType.FILE))
        whenever(sftpRepository.listFiles(any())).thenReturn(testFiles)
        
        // When
        viewModel.loadDirectory("/test/path")
        
        // Then
        assertEquals(FileBrowserUiState.Success(testFiles), viewModel.uiState.value)
    }
}
```

#### UI测试
- **Espresso测试**: 编写端到端UI自动化测试
- **页面对象模式**: 使用Page Object Pattern提高测试可维护性
- **测试覆盖率**: 确保关键功能路径100%测试覆盖

#### 调试工具
- **日志系统**: 使用分级日志(Debug/Info/Warn/Error)
- **性能监控**: 集成APM工具监控应用性能
- **崩溃收集**: 实现崩溃日志收集和分析
- **网络调试**: 使用Stetho或Flipper进行网络请求调试

### 安全与隐私

#### 数据保护
- **敏感信息**: SSH密钥、密码等使用Android Keystore加密存储
- **网络安全**: 强制使用HTTPS/SSH连接，验证证书有效性  
- **权限最小化**: 仅申请必要权限，及时释放敏感权限

#### 代码安全
- **代码混淆**: 生产版本启用ProGuard/R8混淆
- **证书绑定**: 实现SSL证书绑定防止中间人攻击
- **反调试**: 添加反调试措施保护关键逻辑

### 性能监控指标

#### 关键性能指标(KPI)
- **启动时间**: 冷启动<3秒，热启动<1秒
- **内存使用**: 峰值内存<200MB，平均<100MB
- **网络效率**: 连接建立<2秒，文件传输>1MB/s
- **电量消耗**: 后台运行功耗<5%/小时
- **崩溃率**: 线上崩溃率<0.1%

#### 监控实现
```kotlin
// 性能监控示例
class PerformanceTracker {
    fun trackScreenLoad(screenName: String, startTime: Long) {
        val loadTime = System.currentTimeMillis() - startTime
        Logger.d("Performance", "$screenName loaded in ${loadTime}ms")
        
        // 上报性能数据到分析平台
        Analytics.trackEvent("screen_load_time") {
            put("screen", screenName)
            put("duration", loadTime)
        }
    }
}
```

遵循以上规范能确保代码质量达到企业级标准，提供优秀的用户体验和系统稳定性。
# Termux远程文件浏览功能技术实现方案

## 1. 技术方案概述

采用基于SFTP协议的直接连接方案，Android应用通过SSHJ库直接连接服务器文件系统，实现VSCode风格的远程文件浏览功能。包含项目工作区管理、左侧目录树、书签收藏、快速连接等高级功能。

## 2. 技术架构设计

### 2.1 核心依赖库
```gradle
dependencies {
    // SFTP连接库
    implementation 'com.hierynomus:sshj:0.32.0'
    
    // JSON解析
    implementation 'com.google.code.gson:gson:2.8.9'
    
    // RecyclerView
    implementation 'androidx.recyclerview:recyclerview:1.2.1'
    
    // 异步任务
    implementation 'io.reactivex.rxjava3:rxjava:3.1.5'
    implementation 'io.reactivex.rxjava3:rxandroid:3.0.0'
    
    // 下拉刷新
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
}
```

### 2.2 核心类设计

```java
// 1. SFTP连接管理器
public class SFTPConnectionManager {
    private SSHClient sshClient;
    private SFTPClient sftpClient;
    
    public Observable<Boolean> connect(SSHConnectionConfig config);
    public Observable<List<RemoteFileItem>> listFiles(String remotePath);
    public Observable<String> readFileContent(String remoteFilePath);
    public Observable<Boolean> downloadFile(String remotePath, String localPath);
    public void disconnect();
}

// 2. 远程文件数据模型
public class RemoteFileItem {
    private String name;           // 文件名
    private String path;           // 完整路径
    private boolean isDirectory;   // 是否为目录
    private long size;            // 文件大小
    private long lastModified;    // 最后修改时间
    private String permissions;   // 文件权限
    private FileType type;        // 文件类型枚举
}

// 3. 项目工作区数据模型
public class ProjectWorkspace {
    private String id;                          // 唯一标识
    private String name;                        // 项目名称
    private String description;                 // 项目描述
    private String remotePath;                  // 远程根目录
    private SSHConnectionConfig connection;     // SSH连接配置
    private List<String> bookmarkedPaths;       // 收藏的路径
    private Map<String, Boolean> expandedFolders; // 展开的文件夹状态
    private String lastOpenedFile;              // 最后打开的文件
    private long lastAccessTime;                // 最后访问时间
}

// 4. 目录收藏书签
public class DirectoryBookmark {
    private String id;
    private String name;                        // 书签显示名称
    private String fullPath;                    // 完整路径
    private String projectId;                   // 所属项目ID
    private String icon;                        // 自定义图标
    private long createdTime;
}

// 5. 目录树节点
public class FileTreeNode {
    private String name;
    private String fullPath;
    private boolean isDirectory;
    private boolean isExpanded;
    private int depth;                          // 缩进深度
    private List<FileTreeNode> children;
    private FileTreeNode parent;
    private FileType fileType;
    private boolean isLoading;                  // 是否正在加载子项
}

// 6. 文件类型识别
public enum FileType {
    DIRECTORY,
    TEXT_FILE,
    CODE_FILE,
    IMAGE_FILE,
    ARCHIVE_FILE,
    EXECUTABLE_FILE,
    UNKNOWN_FILE
}

// 7. 项目工作区管理器
public class ProjectWorkspaceManager {
    // 保存/加载项目配置
    public void saveWorkspace(ProjectWorkspace workspace);
    public List<ProjectWorkspace> loadAllWorkspaces();
    public ProjectWorkspace getWorkspaceById(String id);
    
    // 书签管理
    public void addBookmark(String projectId, DirectoryBookmark bookmark);
    public void removeBookmark(String bookmarkId);
    public List<DirectoryBookmark> getProjectBookmarks(String projectId);
    
    // 最近访问
    public List<ProjectWorkspace> getRecentWorkspaces(int limit);
    public void updateLastAccess(String projectId);
}

// 8. 目录树适配器
public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeViewHolder> {
    private List<FileTreeNode> flattenedNodes;    // 扁平化的节点列表
    private ProjectWorkspace currentProject;
    private OnFileTreeActionListener listener;
    
    public interface OnFileTreeActionListener {
        void onNodeExpanded(FileTreeNode node);
        void onNodeCollapsed(FileTreeNode node);
        void onFileSelected(FileTreeNode node);
        void onDirectoryBookmarked(FileTreeNode node);
    }
    
    // 展开/折叠节点
    public void toggleNode(FileTreeNode node);
    private void expandNode(FileTreeNode node);
    private void collapseNode(FileTreeNode node);
    
    // 刷新树结构
    public void updateTree(List<FileTreeNode> rootNodes);
    private List<FileTreeNode> flattenTree(List<FileTreeNode> nodes);
}

// 9. SFTP配置存储
public class SFTPConfigurationStorage {
    private static final String PREFS_NAME = "sftp_connections";
    private SharedPreferences prefs;
    private Gson gson;
    
    // 保存连接配置
    public void saveConnection(SSHConnectionConfig config);
    
    // 加载所有连接
    public List<SSHConnectionConfig> loadAllConnections();
    
    // 连接历史记录
    public void addToHistory(String connectionId);
    public List<String> getConnectionHistory();
}

// 10. 远程文件浏览Activity
public class RemoteFileBrowserActivity extends AppCompatActivity implements FileTreeAdapter.OnFileTreeActionListener {
    private ProjectWorkspaceManager workspaceManager;
    private SFTPConnectionManager sftpManager;
    private FileTreeAdapter treeAdapter;
    private RemoteFileBrowserAdapter contentAdapter;
    
    private ProjectWorkspace currentWorkspace;
    private String currentPath = "/";
    
    private void connectAndLoadProject(SSHConnectionConfig config);
    private void loadProjectWorkspace(SSHConnectionConfig config);
    private void loadDirectoryTree(String path);
    private void loadNodeChildren(FileTreeNode node);
    private void navigateToDirectory(String dirPath);
    private void openFile(RemoteFileItem file);
    private void showQuickConnectPanel();
}

// 11. 文件列表适配器
public class RemoteFileBrowserAdapter extends RecyclerView.Adapter<FileViewHolder> {
    private List<RemoteFileItem> fileList;
    private OnFileClickListener listener;
    
    public interface OnFileClickListener {
        void onFileClick(RemoteFileItem file);
        void onFileLongClick(RemoteFileItem file);
    }
}

// 12. 快速连接面板
public class QuickConnectPanel extends BottomSheetDialogFragment {
    private RecyclerView recentConnectionsRecyclerView;
    private RecyclerView savedConnectionsRecyclerView;
    private QuickConnectAdapter recentAdapter;
    private QuickConnectAdapter savedAdapter;
    
    private void setupRecentConnections();
    private void setupSavedConnections();
    private void connectToServer(SSHConnectionConfig config);
}
```

## 3. 功能模块设计

### 3.1 核心功能
- **连接管理**: SSH/SFTP连接的建立、维护和断开
- **目录浏览**: 显示远程目录内容，支持目录导航
- **文件预览**: 支持文本文件内容预览
- **文件下载**: 下载文件到本地存储
- **文件信息**: 显示文件详细信息（大小、权限、修改时间）

### 3.2 VSCode风格功能
- **左侧目录树**: 可展开折叠的目录结构，类似VSCode资源管理器
- **项目工作区**: 支持多项目切换，保存项目配置和状态
- **书签收藏**: 收藏常用目录，支持快速跳转
- **智能缓存**: 缓存目录结构和展开状态
- **快速连接**: 连接历史记录和一键重连
- **状态持久化**: 记住展开的文件夹、滚动位置、最后打开的文件

### 3.3 高级功能
- **路径导航**: 面包屑导航栏显示当前路径
- **文件搜索**: 在当前目录搜索文件
- **多选操作**: 支持文件的批量下载和操作
- **文件预览**: 代码文件语法高亮预览
- **拖拽排序**: 书签和项目的拖拽排序

## 4. UI交互设计

### 4.1 VSCode风格主界面设计
```xml
<!-- activity_remote_file_browser.xml -->
<androidx.drawerlayout.widget.DrawerLayout
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- 主内容区 -->
    <LinearLayout android:orientation="vertical">
        <!-- 顶部工具栏 -->
        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="远程文件浏览"
            app:navigationIcon="@drawable/ic_menu" />
        
        <!-- 项目/连接信息栏 -->
        <LinearLayout 
            android:orientation="horizontal" 
            android:padding="8dp"
            android:background="@color/info_bar_background">
            <ImageView 
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_folder_open" />
            <TextView 
                android:id="@+id/project_name"
                android:layout_width="0dp"
                android:layout_weight="1"
                android:layout_marginStart="8dp"
                android:textSize="14sp"
                android:textStyle="bold" />
            <ImageView 
                android:id="@+id/connection_status_icon"
                android:layout_width="12dp"
                android:layout_height="12dp"
                android:src="@drawable/ic_circle_green" />
            <TextView 
                android:id="@+id/connection_status"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="4dp"
                android:textSize="12sp"
                android:text="已连接" />
        </LinearLayout>
        
        <!-- 路径导航栏 -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/breadcrumb_recyclerview"
            android:layout_width="match_parent"
            android:layout_height="36dp"
            android:orientation="horizontal"
            android:background="@color/breadcrumb_background"
            android:paddingHorizontal="8dp" />
        
        <!-- 文件内容区 -->
        <FrameLayout 
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1">
            
            <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
                android:id="@+id/swipe_refresh_layout"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                
                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/file_content_recyclerview"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
                    
            </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
            
            <!-- 空状态提示 -->
            <LinearLayout
                android:id="@+id/empty_state_layout"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:orientation="vertical"
                android:visibility="gone">
                
                <ImageView
                    android:layout_width="64dp"
                    android:layout_height="64dp"
                    android:layout_gravity="center_horizontal"
                    android:src="@drawable/ic_folder_empty"
                    android:alpha="0.3" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:text="此文件夹为空"
                    android:textColor="@color/text_secondary" />
                    
            </LinearLayout>
        </FrameLayout>
        
        <!-- 底部状态栏 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="28dp"
            android:orientation="horizontal"
            android:background="@color/status_bar_background"
            android:paddingHorizontal="12dp"
            android:gravity="center_vertical">
            
            <TextView
                android:id="@+id/file_count_text"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="11sp"
                android:textColor="@color/text_secondary" />
                
            <TextView
                android:id="@+id/selection_info_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="11sp"
                android:textColor="@color/text_secondary"
                android:visibility="gone" />
        </LinearLayout>
    </LinearLayout>
    
    <!-- 左侧抽屉 - VSCode风格目录树 -->
    <LinearLayout
        android:id="@+id/left_drawer"
        android:layout_width="300dp"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:orientation="vertical"
        android:background="@color/drawer_background">
        
        <!-- 抽屉标题栏 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:orientation="horizontal"
            android:paddingHorizontal="16dp"
            android:gravity="center_vertical"
            android:background="@color/drawer_header_background">
            
            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="资源管理器"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@color/drawer_title_color" />
                
            <ImageButton
                android:id="@+id/drawer_settings_button"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:background="@null"
                android:src="@drawable/ic_settings_small"
                android:contentDescription="设置" />
        </LinearLayout>
        
        <!-- 项目选择器 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="12dp"
            android:paddingVertical="8dp">
            
            <Spinner
                android:id="@+id/project_spinner"
                android:layout_width="0dp"
                android:layout_height="36dp"
                android:layout_weight="1" />
                
            <ImageButton
                android:id="@+id/add_project_button"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_marginStart="8dp"
                android:background="@drawable/circular_button_background"
                android:src="@drawable/ic_add"
                android:contentDescription="添加项目" />
        </LinearLayout>
        
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="@color/divider_color" />
        
        <!-- 书签快速访问 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="12dp"
            android:paddingTop="12dp"
            android:paddingBottom="6dp">
            
            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:layout_marginEnd="6dp"
                android:src="@drawable/ic_bookmark_small" />
                
            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="书签"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="@color/drawer_section_title" />
                
            <ImageButton
                android:id="@+id/bookmark_expand_button"
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:background="@null"
                android:src="@drawable/ic_chevron_down"
                android:contentDescription="展开/折叠" />
        </LinearLayout>
        
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/bookmarks_recyclerview"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxHeight="120dp" />
        
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:layout_marginTop="8dp"
            android:background="@color/divider_color" />
        
        <!-- 目录树 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="12dp"
            android:paddingTop="12dp"
            android:paddingBottom="6dp">
            
            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:layout_marginEnd="6dp"
                android:src="@drawable/ic_folder_tree" />
                
            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="目录结构"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="@color/drawer_section_title" />
                
            <ImageButton
                android:id="@+id/tree_refresh_button"
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:background="@null"
                android:src="@drawable/ic_refresh_small"
                android:contentDescription="刷新" />
        </LinearLayout>
        
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/file_tree_recyclerview"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:scrollbars="vertical" />
        
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="@color/divider_color" />
        
        <!-- 底部操作按钮 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:orientation="horizontal"
            android:paddingHorizontal="8dp"
            android:gravity="center_vertical">
            
            <com.google.android.material.button.MaterialButton
                android:id="@+id/quick_connect_button"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="0dp"
                android:layout_height="36dp"
                android:layout_weight="1"
                android:text="快速连接"
                android:textSize="12sp"
                app:icon="@drawable/ic_connect"
                app:iconSize="16dp" />
                
            <com.google.android.material.button.MaterialButton
                android:id="@+id/drawer_settings_button_bottom"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="0dp"
                android:layout_height="36dp"
                android:layout_weight="1"
                android:text="设置"
                android:textSize="12sp"
                app:icon="@drawable/ic_settings"
                app:iconSize="16dp" />
        </LinearLayout>
    </LinearLayout>
</androidx.drawerlayout.widget.DrawerLayout>
```

### 4.2 目录树节点项设计
```xml
<!-- file_tree_item.xml -->
<LinearLayout 
    android:orientation="horizontal" 
    android:padding="4dp"
    android:background="?android:attr/selectableItemBackground"
    android:minHeight="28dp"
    android:gravity="center_vertical">
    
    <!-- 缩进空间 -->
    <Space 
        android:id="@+id/indent_space"
        android:layout_width="0dp" 
        android:layout_height="match_parent" />
    
    <!-- 展开/折叠图标 -->
    <ImageView
        android:id="@+id/expand_icon"
        android:layout_width="14dp"
        android:layout_height="14dp"
        android:src="@drawable/ic_chevron_right"
        android:visibility="gone"
        android:layout_marginEnd="2dp" />
    
    <!-- 文件类型图标 -->
    <ImageView
        android:id="@+id/file_icon"
        android:layout_width="14dp"
        android:layout_height="14dp"
        android:layout_marginEnd="6dp" />
    
    <!-- 文件名 -->
    <TextView
        android:id="@+id/file_name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="13sp"
        android:textColor="@color/tree_item_text"
        android:singleLine="true"
        android:ellipsize="end" />
    
    <!-- 加载指示器 -->
    <ProgressBar
        android:id="@+id/loading_indicator"
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="12dp"
        android:layout_height="12dp"
        android:visibility="gone"
        android:layout_marginEnd="4dp" />
    
    <!-- 书签图标 -->
    <ImageView
        android:id="@+id/bookmark_icon"
        android:layout_width="10dp"
        android:layout_height="10dp"
        android:src="@drawable/ic_bookmark_small"
        android:visibility="gone"
        android:tint="@color/bookmark_color" />
</LinearLayout>
```

### 4.3 文件列表项设计
```xml
<!-- item_remote_file.xml -->
<LinearLayout 
    android:orientation="horizontal" 
    android:padding="12dp"
    android:background="?android:attr/selectableItemBackground"
    android:minHeight="48dp">
    
    <!-- 选择框 -->
    <CheckBox
        android:id="@+id/file_selection_checkbox"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginEnd="12dp"
        android:layout_gravity="center_vertical"
        android:visibility="gone" />
    
    <!-- 文件类型图标 -->
    <ImageView
        android:id="@+id/file_type_icon"
        android:layout_width="28dp"
        android:layout_height="28dp"
        android:layout_marginEnd="12dp"
        android:layout_gravity="center_vertical" />
    
    <!-- 文件信息 -->
    <LinearLayout 
        android:orientation="vertical" 
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_gravity="center_vertical">
        
        <TextView
            android:id="@+id/file_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="15sp"
            android:textColor="@color/primary_text"
            android:singleLine="true"
            android:ellipsize="middle" />
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="2dp">
            
            <TextView
                android:id="@+id/file_info"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="12sp"
                android:textColor="@color/secondary_text" />
                
            <TextView
                android:id="@+id/file_permissions"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="11sp"
                android:textColor="@color/tertiary_text"
                android:fontFamily="monospace"
                android:visibility="gone" />
        </LinearLayout>
    </LinearLayout>
    
    <!-- 书签指示器 -->
    <ImageView
        android:id="@+id/bookmark_indicator"
        android:layout_width="12dp"
        android:layout_height="12dp"
        android:layout_marginEnd="8dp"
        android:layout_gravity="center_vertical"
        android:src="@drawable/ic_bookmark_small"
        android:visibility="gone"
        android:tint="@color/bookmark_color" />
    
    <!-- 更多操作按钮 -->
    <ImageButton
        android:id="@+id/more_options_button"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:layout_gravity="center_vertical"
        android:background="@drawable/circular_button_background"
        android:src="@drawable/ic_more_vert"
        android:contentDescription="更多选项" />
</LinearLayout>
```

### 4.4 快速连接面板设计
```xml
<!-- layout_quick_connect_panel.xml -->
<LinearLayout 
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bottom_sheet_background"
    android:paddingTop="8dp">
    
    <!-- 面板拖拽指示器 -->
    <View
        android:layout_width="32dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginBottom="16dp"
        android:background="@drawable/bottom_sheet_drag_indicator" />
    
    <!-- 标题 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:paddingBottom="16dp">
        
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="快速连接"
            android:textSize="20sp"
            android:textStyle="bold" />
            
        <ImageButton
            android:id="@+id/add_connection_button"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:background="@drawable/circular_button_background"
            android:src="@drawable/ic_add"
            android:contentDescription="添加连接" />
    </LinearLayout>
    
    <!-- 最近连接 -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        android:paddingBottom="8dp"
        android:text="最近连接"
        android:textSize="14sp"
        android:textStyle="bold"
        android:textColor="@color/section_title" />
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recent_connections_recyclerview"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="12dp" />
    
    <!-- 分割线 -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginVertical="16dp"
        android:layout_marginHorizontal="20dp"
        android:background="@color/divider_color" />
    
    <!-- 已保存连接 -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        android:paddingBottom="8dp"
        android:text="已保存连接"
        android:textSize="14sp"
        android:textStyle="bold"
        android:textColor="@color/section_title" />
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/saved_connections_recyclerview"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:maxHeight="240dp"
        android:paddingHorizontal="12dp"
        android:paddingBottom="20dp" />
</LinearLayout>
```

### 4.5 文件操作对话框
```xml
<!-- dialog_file_operations.xml -->
<LinearLayout 
    android:orientation="vertical" 
    android:layout_width="280dp"
    android:layout_height="wrap_content"
    android:background="@drawable/dialog_background"
    android:padding="20dp">
    
    <!-- 文件信息头部 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingBottom="16dp">
        
        <ImageView
            android:id="@+id/dialog_file_icon"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_marginEnd="12dp" />
            
        <LinearLayout
            android:orientation="vertical"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1">
            
            <TextView
                android:id="@+id/dialog_file_name"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="16sp"
                android:textStyle="bold"
                android:singleLine="true"
                android:ellipsize="middle" />
                
            <TextView
                android:id="@+id/dialog_file_path"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="@color/secondary_text"
                android:singleLine="true"
                android:ellipsize="start" />
        </LinearLayout>
    </LinearLayout>
    
    <!-- 操作选项 -->
    <LinearLayout 
        android:orientation="vertical" 
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <TextView
            android:id="@+id/action_view_content"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="查看内容"
            android:textSize="15sp"
            android:gravity="center_vertical"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:background="?android:attr/selectableItemBackground"
            android:drawableStart="@drawable/ic_visibility"
            android:drawablePadding="16dp" />
        
        <TextView
            android:id="@+id/action_download"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="下载到本地"
            android:textSize="15sp"
            android:gravity="center_vertical"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:background="?android:attr/selectableItemBackground"
            android:drawableStart="@drawable/ic_download"
            android:drawablePadding="16dp" />
        
        <TextView
            android:id="@+id/action_add_bookmark"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="添加到书签"
            android:textSize="15sp"
            android:gravity="center_vertical"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:background="?android:attr/selectableItemBackground"
            android:drawableStart="@drawable/ic_bookmark_add"
            android:drawablePadding="16dp" />
        
        <TextView
            android:id="@+id/action_copy_path"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="复制路径"
            android:textSize="15sp"
            android:gravity="center_vertical"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:background="?android:attr/selectableItemBackground"
            android:drawableStart="@drawable/ic_copy"
            android:drawablePadding="16dp" />
        
        <TextView
            android:id="@+id/action_file_info"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:text="文件信息"
            android:textSize="15sp"
            android:gravity="center_vertical"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:background="?android:attr/selectableItemBackground"
            android:drawableStart="@drawable/ic_info"
            android:drawablePadding="16dp" />
    </LinearLayout>
</LinearLayout>
```

## 5. 用户交互流程

### 5.1 快速连接流程
1. 用户点击SSH浮动按钮选择"文件浏览器"或点击抽屉中"快速连接"
2. 显示快速连接面板，展示最近连接和已保存连接
3. 用户选择连接配置或新建配置
4. 应用尝试建立SSH/SFTP连接
5. 连接成功后加载项目工作区，初始化目录树

### 5.2 VSCode风格文件浏览流程
1. **左侧目录树导航**:
   - 展开/折叠文件夹查看层级结构
   - 点击文件夹在右侧显示内容
   - 点击文件直接打开或显示操作菜单

2. **主内容区浏览**:
   - 显示当前目录的文件和文件夹列表
   - 面包屑导航显示当前路径
   - 下拉刷新更新目录内容

3. **书签和快速访问**:
   - 长按目录添加到书签
   - 书签列表快速跳转
   - 项目切换保持各自的书签

### 5.3 项目工作区管理流程
1. **项目创建和配置**:
   - 连接服务器后自动创建或选择项目工作区
   - 设置项目名称、描述和根目录
   - 保存连接配置和项目状态

2. **状态持久化**:
   - 记住展开的文件夹状态
   - 保存最后访问的文件和目录
   - 恢复滚动位置和选中状态

3. **多项目切换**:
   - 抽屉中项目选择器快速切换
   - 每个项目独立的书签和状态
   - 最近访问项目的快速恢复

### 5.4 增强的文件操作流程
1. **文件查看和编辑**:
   - 文本文件内容预览，支持语法高亮
   - 图片文件缩略图显示
   - 大文件分块加载和滚动查看

2. **批量操作**:
   - 多选模式批量下载文件
   - 批量添加到书签
   - 批量复制路径和文件信息

3. **智能搜索**:
   - 当前目录文件名搜索
   - 支持通配符和正则表达式
   - 搜索结果高亮和快速定位

## 6. 技术实现细节

### 6.1 SFTP连接实现
```java
public class SFTPConnectionManager {
    public Observable<Boolean> connect(SSHConnectionConfig config) {
        return Observable.fromCallable(() -> {
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            
            sshClient.connect(config.getHost(), config.getPort());
            sshClient.authPassword(config.getUsername(), config.getPassword());
            
            sftpClient = sshClient.newSFTPClient();
            return true;
        }).subscribeOn(Schedulers.io());
    }
    
    public Observable<List<RemoteFileItem>> listFiles(String remotePath) {
        return Observable.fromCallable(() -> {
            List<RemoteFileItem> files = new ArrayList<>();
            for (RemoteResourceInfo resource : sftpClient.ls(remotePath)) {
                RemoteFileItem item = new RemoteFileItem();
                item.setName(resource.getName());
                item.setPath(remotePath + "/" + resource.getName());
                item.setDirectory(resource.getAttributes().getType() == FileMode.Type.DIRECTORY);
                item.setSize(resource.getAttributes().getSize());
                item.setLastModified(resource.getAttributes().getMtime());
                files.add(item);
            }
            return files;
        }).subscribeOn(Schedulers.io());
    }
}
```

### 6.2 文件类型识别
```java
public class FileTypeUtils {
    private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();
    
    static {
        // 代码文件
        EXTENSION_MAP.put("java", FileType.CODE_FILE);
        EXTENSION_MAP.put("js", FileType.CODE_FILE);
        EXTENSION_MAP.put("py", FileType.CODE_FILE);
        EXTENSION_MAP.put("cpp", FileType.CODE_FILE);
        
        // 文本文件
        EXTENSION_MAP.put("txt", FileType.TEXT_FILE);
        EXTENSION_MAP.put("md", FileType.TEXT_FILE);
        EXTENSION_MAP.put("log", FileType.TEXT_FILE);
        
        // 图片文件
        EXTENSION_MAP.put("jpg", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("png", FileType.IMAGE_FILE);
        EXTENSION_MAP.put("gif", FileType.IMAGE_FILE);
    }
    
    public static FileType getFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return EXTENSION_MAP.getOrDefault(extension, FileType.UNKNOWN_FILE);
    }
}
```

## 7. 集成到现有应用

### 7.1 入口集成
在现有的SSH浮动按钮菜单中添加"远程文件浏览"选项：

```java
// SSHFloatingActionButton.java
private void showSSHOptionsMenu() {
    PopupMenu popup = new PopupMenu(getContext(), this);
    popup.getMenuInflater().inflate(R.menu.ssh_options_menu, popup.getMenu());
    
    popup.setOnMenuItemClickListener(item -> {
        switch (item.getItemId()) {
            case R.id.menu_ssh_connect:
                showSSHConnectionDialog();
                return true;
            case R.id.menu_file_browser:  // 新增选项
                openRemoteFileBrowser();
                return true;
        }
        return false;
    });
    
    popup.show();
}

private void openRemoteFileBrowser() {
    Intent intent = new Intent(getContext(), RemoteFileBrowserActivity.class);
    getContext().startActivity(intent);
}
```

### 7.2 配置共享
复用现有的SSH配置系统：

```java
// RemoteFileBrowserActivity.java
private void loadSSHConfigs() {
    SSHConfigManager configManager = SSHConfigManager.getInstance(this);
    List<SSHConnectionConfig> configs = configManager.getAllConfigs();
    
    // 显示配置选择对话框或使用最后使用的配置
    SSHConnectionConfig lastUsed = configManager.getLastUsedConfig();
    if (lastUsed != null) {
        connectToServer(lastUsed);
    } else {
        showConnectionConfigDialog(configs);
    }
}
```

## 8. VSCode风格功能实现

### 8.1 目录树实现细节
```java
// 目录树扁平化渲染
public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeViewHolder> {
    private List<FileTreeNode> flattenedNodes = new ArrayList<>();
    
    public void updateTree(List<FileTreeNode> rootNodes) {
        flattenedNodes.clear();
        for (FileTreeNode root : rootNodes) {
            flattenTree(root, flattenedNodes, 0);
        }
        notifyDataSetChanged();
    }
    
    private void flattenTree(FileTreeNode node, List<FileTreeNode> result, int depth) {
        node.setDepth(depth);
        result.add(node);
        
        if (node.isExpanded() && node.getChildren() != null) {
            for (FileTreeNode child : node.getChildren()) {
                flattenTree(child, result, depth + 1);
            }
        }
    }
    
    @Override
    public void onBindViewHolder(FileTreeViewHolder holder, int position) {
        FileTreeNode node = flattenedNodes.get(position);
        
        // 设置缩进
        int indentPx = node.getDepth() * INDENT_SIZE_DP;
        holder.indentSpace.getLayoutParams().width = indentPx;
        
        // 设置展开/折叠图标
        if (node.isDirectory() && !node.getChildren().isEmpty()) {
            holder.expandIcon.setVisibility(View.VISIBLE);
            holder.expandIcon.setRotation(node.isExpanded() ? 90 : 0);
        } else {
            holder.expandIcon.setVisibility(View.INVISIBLE);
        }
        
        // 设置文件图标和名称
        holder.fileIcon.setImageResource(getFileTypeIcon(node.getFileType()));
        holder.fileName.setText(node.getName());
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (node.isDirectory()) {
                toggleNode(node);
            } else {
                listener.onFileSelected(node);
            }
        });
    }
}
```

### 8.2 项目工作区持久化
```java
// 项目状态保存和恢复
public class ProjectWorkspaceManager {
    private static final String WORKSPACE_PREFS = "project_workspaces";
    private SharedPreferences prefs;
    private Gson gson;
    
    public void saveWorkspace(ProjectWorkspace workspace) {
        workspace.setLastAccessTime(System.currentTimeMillis());
        String json = gson.toJson(workspace);
        prefs.edit().putString("workspace_" + workspace.getId(), json).apply();
        
        // 更新最近访问列表
        updateRecentAccess(workspace.getId());
    }
    
    public ProjectWorkspace loadWorkspaceForConnection(SSHConnectionConfig config) {
        // 查找是否已有对应的工作区
        List<ProjectWorkspace> workspaces = loadAllWorkspaces();
        for (ProjectWorkspace workspace : workspaces) {
            if (workspace.getConnection().equals(config)) {
                return workspace;
            }
        }
        
        // 创建新的工作区
        ProjectWorkspace newWorkspace = new ProjectWorkspace();
        newWorkspace.setId(UUID.randomUUID().toString());
        newWorkspace.setName(config.getHost());
        newWorkspace.setConnection(config);
        newWorkspace.setRemotePath("/");
        newWorkspace.setBookmarkedPaths(new ArrayList<>());
        newWorkspace.setExpandedFolders(new HashMap<>());
        
        saveWorkspace(newWorkspace);
        return newWorkspace;
    }
}
```

### 8.3 书签管理系统
```java
// 书签管理和快速访问
public class BookmarkManager {
    public void addBookmark(ProjectWorkspace workspace, String path, String customName) {
        DirectoryBookmark bookmark = new DirectoryBookmark();
        bookmark.setId(UUID.randomUUID().toString());
        bookmark.setName(customName != null ? customName : new File(path).getName());
        bookmark.setFullPath(path);
        bookmark.setProjectId(workspace.getId());
        bookmark.setCreatedTime(System.currentTimeMillis());
        
        workspace.getBookmarkedPaths().add(bookmark.getId());
        saveBookmark(bookmark);
        workspaceManager.saveWorkspace(workspace);
    }
    
    public List<DirectoryBookmark> getProjectBookmarks(String projectId) {
        List<DirectoryBookmark> bookmarks = new ArrayList<>();
        Map<String, ?> all = prefs.getAll();
        
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith("bookmark_")) {
                DirectoryBookmark bookmark = gson.fromJson(
                    (String) entry.getValue(), 
                    DirectoryBookmark.class
                );
                if (projectId.equals(bookmark.getProjectId())) {
                    bookmarks.add(bookmark);
                }
            }
        }
        
        // 按创建时间排序
        bookmarks.sort((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()));
        return bookmarks;
    }
}
```

## 9. 性能优化

### 9.1 智能缓存策略
- **目录结构缓存**: 缓存已访问的目录内容，设置过期时间
- **状态缓存**: 缓存展开状态、滚动位置、选中状态
- **连接池管理**: 复用SFTP连接，避免频繁建立连接
- **图标缓存**: 缓存文件类型图标，减少重复加载

### 9.2 懒加载和虚拟化
- **目录树懒加载**: 只在展开时加载子目录
- **大目录分页**: 超过一定数量的文件分页显示
- **虚拟滚动**: 使用RecyclerView的回收机制处理大列表
- **异步预加载**: 后台预加载可能访问的目录

### 9.3 用户体验优化
- **加载状态指示**: 显示加载进度和状态
- **智能刷新**: 检测文件变更，智能更新列表
- **平滑动画**: 展开折叠、导航切换的平滑过渡
- **手势支持**: 支持左滑返回、长按选择等手势

## 9. 错误处理

### 9.1 网络异常
- 连接超时重试机制
- 网络状态检测
- 用户友好的错误提示

### 9.2 权限异常
- 文件访问权限检查
- 清晰的权限错误说明
- 建议解决方案提示

## 10. 安全考虑

### 10.1 连接安全
- 支持SSH密钥认证
- 连接超时设置
- 敏感信息加密存储

### 10.2 本地安全
- 下载文件存储权限控制
- 缓存数据定期清理
- 应用退出时断开连接

---

## 11. 开发实施计划

### 第一阶段：核心数据模型和管理器 (会话1)

#### 任务清单:
- [x] **添加SSHJ库依赖配置**
  - 在app/build.gradle中添加SSHJ (com.hierynomus:sshj:0.32.0)
  - 添加RxJava3、Gson、SwipeRefreshLayout等依赖
  - 配置ProGuard规则保护相关类

- [x] **设计项目收藏和书签功能**
  - 设计ProjectWorkspace数据模型 (id, name, description, remotePath, connection, bookmarkedPaths, expandedFolders, lastOpenedFile, lastAccessTime)
  - 设计DirectoryBookmark数据模型 (id, name, fullPath, projectId, icon, createdTime)
  - 定义项目工作区和书签的JSON存储格式

- [x] **创建项目工作区管理器**
  - 实现ProjectWorkspaceManager类：saveWorkspace、loadAllWorkspaces、getWorkspaceById
  - 实现BookmarkManager类：addBookmark、removeBookmark、getProjectBookmarks
  - 支持最近访问项目：getRecentWorkspaces、updateLastAccess
  - 使用SharedPreferences和Gson进行数据持久化

- [x] **增强SFTP连接管理和配置保存**
  - 扩展SFTPConfigurationStorage类：saveConnection、loadAllConnections、addToHistory
  - 实现连接历史记录功能：最多保存10个最近连接
  - 设计连接状态管理和自动重连机制

### 第二阶段：VSCode风格UI组件 (会话2)

#### 任务清单:
- [x] **实现VSCode风格左侧目录树**
  - 创建FileTreeNode数据结构：name, fullPath, isDirectory, isExpanded, depth, children, parent, fileType, isLoading
  - 实现FileTreeAdapter：支持层级缩进、展开折叠图标、文件类型图标
  - 设计file_tree_item.xml布局：缩进空间、展开图标、文件图标、文件名、加载指示器、书签图标

- [x] **实现目录树展开折叠功能**
  - 扁平化渲染算法：flattenTree方法将树形结构转为列表
  - 展开折叠逻辑：toggleNode、expandNode、collapseNode
  - 动态加载子节点：loadNodeChildren方法异步获取目录内容
  - 保存展开状态到ProjectWorkspace.expandedFolders

- [x] **添加快速连接和最近使用功能**
  - 设计QuickConnectPanel底部弹窗：layout_quick_connect_panel.xml
  - 最近连接区域：显示最近5个连接，支持一键连接
  - 已保存连接区域：显示所有保存的连接配置
  - 添加连接按钮：快速创建新的SSH连接配置

### 第三阶段：主界面和核心功能 (会话3)

#### 任务清单:
- [ ] **创建VSCode风格主界面布局**
  - 设计activity_remote_file_browser.xml：DrawerLayout + 左侧抽屉 + 主内容区
  - 左侧抽屉包含：项目选择器、书签快速访问、目录树、底部操作按钮
  - 主内容区包含：工具栏、项目信息栏、路径导航栏、文件列表、状态栏

- [ ] **实现RemoteFileBrowserActivity主逻辑**
  - 处理Intent参数：连接配置传递和工作区加载
  - 实现连接流程：connectAndLoadProject、loadProjectWorkspace
  - 目录导航功能：navigateToDirectory、更新面包屑导航
  - 文件操作处理：openFile、showFileOperationsDialog

- [ ] **集成SFTP连接和文件操作**
  - 扩展SFTPConnectionManager：connect、listFiles、readFileContent、downloadFile
  - 文件列表适配器：RemoteFileBrowserAdapter支持文件选择、长按菜单
  - 文件操作对话框：查看内容、下载、添加书签、复制路径、文件信息

- [ ] **实现目录树与主内容区联动**
  - FileTreeAdapter.OnFileTreeActionListener：onNodeExpanded、onFileSelected、onDirectoryBookmarked
  - 同步选中状态：目录树选中与主内容区当前路径同步
  - 书签管理集成：从目录树和文件列表添加书签

### 第四阶段：高级功能和性能优化 (会话4)

#### 任务清单:
- [ ] **实现状态持久化和项目切换**
  - 保存和恢复展开状态：ProjectWorkspace.expandedFolders
  - 保存滚动位置和选中文件：lastOpenedFile、滚动位置缓存
  - 项目切换器：Spinner支持快速切换工作区，保持各自独立状态
  - 应用重启恢复：恢复上次使用的项目和状态

- [ ] **添加文件搜索和批量操作**
  - 文件搜索功能：在当前目录搜索文件名，支持通配符
  - 搜索结果高亮：匹配项高亮显示和快速定位
  - 多选模式：CheckBox支持批量选择文件
  - 批量操作：批量下载、批量添加书签、批量复制路径

- [ ] **性能优化和用户体验提升**
  - 懒加载实现：目录树按需加载，大目录分页显示
  - 缓存策略：目录内容缓存、连接状态缓存、图标缓存
  - 加载状态指示：显示加载进度、网络状态、错误提示
  - 手势支持：左滑返回、长按选择、下拉刷新

### 第五阶段：集成和测试完善 (会话5)

#### 任务清单:
- [ ] **集成到现有SSH功能入口**
  - 修改SSHFloatingActionButton：添加"文件浏览器"菜单项
  - 菜单资源文件：R.menu.ssh_options_menu添加menu_file_browser
  - Intent启动：openRemoteFileBrowser方法启动RemoteFileBrowserActivity
  - 配置共享：复用现有SSHConfigManager的连接配置

- [ ] **全面测试和错误处理**
  - 连接错误处理：网络超时、认证失败、权限错误
  - 文件操作错误：文件不存在、权限不足、磁盘空间不足
  - 用户友好的错误提示：具体错误原因和解决建议
  - 边界情况测试：空目录、大文件、特殊字符文件名

- [ ] **文档更新和代码注释**
  - 更新README.md：新功能说明和使用指南
  - 添加代码注释：关键类和方法的详细注释
  - 用户手册：功能介绍、快捷操作、故障排除
  - 开发者文档：架构说明、扩展指南、API参考

### 开发进度追踪:

**已完成任务 (10/20)**:
- ✅ 添加SSHJ库依赖配置
- ✅ 设计项目收藏和书签功能  
- ✅ 创建项目工作区管理器
- ✅ 增强SFTP连接管理和配置保存
- ✅ 实现VSCode风格左侧目录树
- ✅ 实现目录树展开折叠功能
- ✅ 添加快速连接和最近使用功能
- ✅ 将todo任务详细信息添加到技术文档
- ✅ 创建VSCode风格主界面布局
- ✅ 实现RemoteFileBrowserActivity主逻辑

**当前进行 (1/20)**:
- 🔄 集成SFTP连接和文件操作

**待完成任务 (9/20)**:
- ⏳ 集成SFTP连接和文件操作
- ⏳ 实现目录树与主内容区联动
- ⏳ 实现状态持久化和项目切换
- ⏳ 添加文件搜索和批量操作
- ⏳ 性能优化和用户体验提升
- ⏳ 集成到现有SSH功能入口
- ⏳ 全面测试和错误处理
- ⏳ 文档更新和代码注释

---

**文档版本**: v2.0 (VSCode风格版本)  
**更新时间**: 2025-01-XX  
**适用版本**: Termux Android App  
**新增功能**: VSCode风格目录树、项目工作区、书签管理、快速连接

此文档为后续开发会话提供完整的VSCode风格文件浏览器技术实现参考。
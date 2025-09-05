package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.ProjectWorkspace;
import com.termux.app.models.DirectoryBookmark;
import com.termux.app.models.FileTreeNode;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.app.adapters.RemoteFileBrowserAdapter;
import com.termux.app.adapters.BookmarksAdapter;
import com.termux.app.adapters.FileTreeAdapter;
import com.termux.app.managers.ProjectWorkspaceManager;
import com.termux.app.ui.SSHConfigDialog;
import com.termux.app.utils.LightToast;
import com.termux.shared.logger.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 简化版远程文件浏览器主Activity
 * 提供单屏文件浏览和SSH连接功能
 */
public class RemoteFileBrowserActivity extends AppCompatActivity implements 
        RemoteFileBrowserAdapter.OnFileClickListener, 
        SSHConfigDialog.OnSSHConfigListener,
        FileTreeAdapter.OnFileTreeActionListener,
        BookmarksAdapter.OnBookmarkActionListener {
    
    private static final String LOG_TAG = "RemoteFileBrowserActivity";
    
    // UI组件
    private Toolbar toolbar;
    private TextView projectNameText;
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private TextView currentPathText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView fileContentRecyclerView;
    private TextView fileCountText;
    private TextView selectionInfoText;
    private ProgressBar loadingProgress;
    private View emptyStateLayout;
    private DrawerLayout drawerLayout;
    
    // 数据和状态
    private SSHConnectionConfig currentConnection;
    private ProjectWorkspace currentWorkspace;
    private String currentPath = "/";
    private boolean isConnected = false;
    
    // 管理器和适配器
    private SFTPConnectionManager sftpManager;
    private ProjectWorkspaceManager workspaceManager;
    private RemoteFileBrowserAdapter fileAdapter;
    private BookmarksAdapter bookmarksAdapter;
    private FileTreeAdapter treeAdapter;
    private CompositeDisposable compositeDisposable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_file_browser);
        
        Logger.logInfo(LOG_TAG, "RemoteFileBrowserActivity created");
        
        // 初始化管理器
        sftpManager = SFTPConnectionManager.getInstance();
        compositeDisposable = new CompositeDisposable();
        
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupEventListeners();
        
        // 处理Intent传递的连接配置
        handleIntent(getIntent());
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        projectNameText = findViewById(R.id.project_name);
        connectionStatusIcon = findViewById(R.id.connection_status_icon);
        connectionStatusText = findViewById(R.id.connection_status);
        currentPathText = findViewById(R.id.current_path_text);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fileContentRecyclerView = findViewById(R.id.file_content_recyclerview);
        fileCountText = findViewById(R.id.file_count_text);
        selectionInfoText = findViewById(R.id.selection_info_text);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        
        Logger.logInfo(LOG_TAG, "Views initialized");
    }
    
    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_ssh);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // 使用自定义标题布局
        }
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerViews() {
        // 文件内容列表
        fileContentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new RemoteFileBrowserAdapter(this);
        fileContentRecyclerView.setAdapter(fileAdapter);
        
        Logger.logInfo(LOG_TAG, "RecyclerViews configured");
    }
    
    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentDirectory);
        
        Logger.logInfo(LOG_TAG, "Event listeners configured");
    }
    
    /**
     * 处理Intent参数
     */
    private void handleIntent(Intent intent) {
        if (intent != null) {
            String configJson = intent.getStringExtra("ssh_config");
            if (configJson != null) {
                SSHConnectionConfig config = SSHConnectionConfig.fromJson(configJson);
                if (config != null && config.isValid()) {
                    connectAndLoadProject(config);
                } else {
                    Logger.logError(LOG_TAG, "Invalid SSH config in Intent");
                    showSSHConnectionDialog();
                }
            } else {
                // 没有配置，显示SSH配置对话框
                showSSHConnectionDialog();
            }
        } else {
            showSSHConnectionDialog();
        }
    }
    
    /**
     * 连接服务器并加载项目
     */
    private void connectAndLoadProject(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "Connecting to " + config.getHost());
        
        currentConnection = config;
        updateConnectionStatus(false, "连接中...");
        showLoading(true);
        
        // 使用SFTP管理器建立连接
        Disposable disposable = sftpManager.connect(config)
            .subscribe(
                success -> {
                    if (success) {
                        onConnectionEstablished();
                    } else {
                        onConnectionFailed("连接失败");
                    }
                },
                throwable -> onConnectionFailed(throwable.getMessage())
            );
        
        compositeDisposable.add(disposable);
    }
    
    /**
     * 连接建立成功的回调
     */
    private void onConnectionEstablished() {
        Logger.logInfo(LOG_TAG, "SFTP connection established");
        
        isConnected = true;
        updateConnectionStatus(true, "已连接");
        updateProjectInfo(currentConnection.getName() != null ? 
            currentConnection.getName() : currentConnection.getHost());
        
        // 直接导航到根目录
        navigateToDirectory("/");
        
        showLoading(false);
    }
    
    /**
     * 连接失败的回调
     */
    private void onConnectionFailed(String error) {
        Logger.logError(LOG_TAG, "SFTP connection failed: " + error);
        
        isConnected = false;
        updateConnectionStatus(false, "连接失败");
        showLoading(false);
        
        // 显示错误消息并重新显示连接面板
        showErrorMessage("连接失败: " + error);
        showQuickConnectPanel();
    }
    
    /**
     * 加载项目工作区
     */
    private void loadProjectWorkspace(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "Loading project workspace for " + config.getHost());
        
        // 使用工作区管理器获取或创建工作区
        currentWorkspace = workspaceManager.getOrCreateWorkspaceForConnection(config);
        
        if (currentWorkspace != null) {
            // 更新项目显示信息
            updateProjectInfo(currentWorkspace.getDisplayName());
            
            // 恢复上次的路径状态
            String savedCurrentPath = currentWorkspace.getCurrentPath();
            if (savedCurrentPath != null && !savedCurrentPath.isEmpty()) {
                currentPath = savedCurrentPath;
            }
            
            // 更新最后访问时间
            workspaceManager.updateLastAccess(currentWorkspace.getId());
            
            Logger.logInfo(LOG_TAG, "Workspace loaded: " + currentWorkspace.getId());
            Logger.logInfo(LOG_TAG, "Current path restored: " + currentPath);
        } else {
            Logger.logError(LOG_TAG, "Failed to create workspace for connection");
        }
    }
    
    /**
     * 加载目录树
     */
    private void loadDirectoryTree(String path) {
        Logger.logInfo(LOG_TAG, "Loading directory tree for path: " + path);
        
        if (!isConnected || sftpManager == null) {
            Logger.logError(LOG_TAG, "Not connected, cannot load directory tree");
            return;
        }
        
        // 创建根节点
        FileTreeNode rootNode = new FileTreeNode(
            path.equals("/") ? "Root" : path.substring(path.lastIndexOf('/') + 1),
            path,
            true // 是目录
        );
        rootNode.setDepth(0);
        
        // 加载根目录的直接子目录
        loadNodeChildren(rootNode);

        // 设置到tree adapter
        List<FileTreeNode> rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
        treeAdapter.updateTree(rootNodes);
        
        // 默认展开根节点
        treeAdapter.expandNode(rootNode);
        
        // 恢复工作区的展开状态
        restoreTreeExpandedState();
    }
    
    /**
     * 导航到指定目录
     */
    private void navigateToDirectory(String dirPath) {
        Logger.logInfo(LOG_TAG, "Navigating to directory: " + dirPath);
        
        // 使用统一的路径变化处理
        onPathChanged(dirPath);
        
        // 加载目录内容
        loadDirectoryContent(dirPath);
        
        // 确保路径在树中可见
        if (treeAdapter != null) {
            treeAdapter.expandToPath(dirPath);
        }
    }
    
    /**
     * 加载目录内容
     */
    private void loadDirectoryContent(String path) {
        Logger.logInfo(LOG_TAG, "Loading directory content for: " + path);
        showLoading(true);
        
        // 使用SFTP管理器获取目录列表
        Disposable disposable = sftpManager.listFiles(path)
            .subscribe(
                files -> {
                    Logger.logInfo(LOG_TAG, "Loaded " + files.size() + " files from " + path);
                    fileAdapter.updateFiles(files, path);
                    updateFileCount(files.size());
                    showEmptyState(files.isEmpty());
                    showLoading(false);
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "Failed to load directory content: " + throwable.getMessage());
                    showErrorMessage("加载目录失败: " + throwable.getMessage());
                    showEmptyState(true);
                    showLoading(false);
                }
            );
        
        compositeDisposable.add(disposable);
    }
    
    /**
     * 刷新当前目录
     */
    private void refreshCurrentDirectory() {
        Logger.logInfo(LOG_TAG, "Refreshing current directory");
        
        if (isConnected) {
            loadDirectoryContent(currentPath);
        }
        
        postDelayed(() -> {
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }
    
    /**
     * 显示快速连接面板
     */
    private void showQuickConnectPanel() {
        Logger.logInfo(LOG_TAG, "Showing quick connect panel");
        // TODO: 实现快速连接面板
    }
    
    /**
     * 显示添加项目对话框
     */
    private void showAddProjectDialog() {
        Logger.logInfo(LOG_TAG, "Showing add project dialog");
        // TODO: 实现添加项目对话框
    }
    
    /**
     * 显示SSH连接配置对话框
     */
    private void showSSHConnectionDialog() {
        Logger.logInfo(LOG_TAG, "Showing SSH connection dialog");
        SSHConfigDialog dialog = new SSHConfigDialog(this);
        dialog.setOnSSHConfigListener(this);
        dialog.show();
    }
    
    /**
     * 更新当前路径显示，支持长路径的省略显示
     */
    private void updateCurrentPath(String path) {
        if (currentPathText != null) {
            String displayPath = optimizePathDisplay(path);
            currentPathText.setText(displayPath);
            currentPath = path;
        }
    }
    
    /**
     * 优化路径显示，当路径过长时进行省略处理
     */
    private String optimizePathDisplay(String fullPath) {
        if (fullPath == null || fullPath.length() <= 40) {
            return fullPath;
        }
        
        // 对于长路径，显示 "...父目录/当前目录" 格式
        String[] pathParts = fullPath.split("/");
        if (pathParts.length > 2) {
            String currentDir = pathParts[pathParts.length - 1];
            String parentDir = pathParts[pathParts.length - 2];
            return ".../" + parentDir + "/" + currentDir;
        }
        
        return fullPath;
    }
    
    /**
     * 更新连接状态
     */
    private void updateConnectionStatus(boolean connected, String statusText) {
        runOnUiThread(() -> {
            connectionStatusIcon.setImageResource(connected ? 
                android.R.drawable.presence_online : 
                android.R.drawable.presence_offline);
            connectionStatusText.setText(statusText);
        });
    }
    
    /**
     * 更新项目信息
     */
    private void updateProjectInfo(String projectName) {
        runOnUiThread(() -> {
            projectNameText.setText(projectName);
        });
    }
    
    /**
     * 更新面包屑导航
     */
    private void updateBreadcrumb(String path) {
        // TODO: 实现面包屑导航更新
        Logger.logInfo(LOG_TAG, "Updating breadcrumb for path: " + path);
    }
    
    /**
     * 更新文件计数
     */
    private void updateFileCount(int count) {
        runOnUiThread(() -> {
            fileCountText.setText(count + " 项");
        });
    }
    
    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }
    
    /**
     * 显示/隐藏空状态
     */
    private void showEmptyState(boolean show) {
        runOnUiThread(() -> {
            emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            fileContentRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }
    
    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        // TODO: 实现错误消息显示
        Logger.logError(LOG_TAG, "Error: " + message);
    }
    
    /**
     * 延迟执行任务
     */
    private void postDelayed(Runnable runnable, long delayMillis) {
        findViewById(android.R.id.content).postDelayed(runnable, delayMillis);
    }
    
    /**
     * 恢复目录树的展开状态
     */
    private void restoreTreeExpandedState() {
        if (currentWorkspace == null) {
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Restoring tree expanded state");
        
        // 获取保存的展开状态
        Map<String, Boolean> expandedFolders = currentWorkspace.getExpandedFolders();
        if (expandedFolders != null && !expandedFolders.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : expandedFolders.entrySet()) {
                if (entry.getValue()) {
                    // 展开该路径
                    treeAdapter.expandToPath(entry.getKey());
                }
            }
        }
    }
    
    /**
     * 加载书签列表
     */
    private void loadBookmarks() {
        if (currentWorkspace == null) {
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Loading bookmarks for workspace: " + currentWorkspace.getId());
        
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        bookmarksAdapter.updateBookmarks(bookmarks);
        Logger.logInfo(LOG_TAG, "Loaded " + bookmarks.size() + " bookmarks");
    }
    
    /**
     * 同步目录树中的书签状态
     */
    private void syncBookmarkStates() {
        if (currentWorkspace == null || treeAdapter == null) {
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Syncing bookmark states in tree");
        
        List<FileTreeNode> flattenedNodes = treeAdapter.getFlattenedNodes();
        for (FileTreeNode node : flattenedNodes) {
            if (node.isDirectory()) {
                boolean isBookmarked = workspaceManager.isBookmarked(
                    currentWorkspace.getId(), 
                    node.getFullPath()
                );
                if (node.isBookmarked() != isBookmarked) {
                    node.setBookmarked(isBookmarked);
                    treeAdapter.refreshNode(node);
                }
            }
        }
    }
    
    /**
     * 同步目录树和主内容区的选中状态
     */
    private void syncSelectionStates() {
        if (treeAdapter == null || fileAdapter == null) {
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Syncing selection states between tree and main content");
        
        // 同步目录树的选中状态到当前路径
        treeAdapter.setSelectedPath(currentPath);
        
        // 同步文件列表的选中状态（如果需要）
        // 这里可以添加文件列表选中同步逻辑
    }
    
    /**
     * 当路径变化时的同步处理
     */
    private void onPathChanged(String newPath) {
        Logger.logInfo(LOG_TAG, "Path changed to: " + newPath);
        
        // 更新当前路径显示
        updateCurrentPath(newPath);
        
        // 同步选中状态
        syncSelectionStates();
        
        // 保存到工作区
        if (currentWorkspace != null) {
            currentWorkspace.setCurrentPath(newPath);
            workspaceManager.saveWorkspaceState(currentWorkspace);
        }
        
        // 更新面包屑
        updateBreadcrumb(newPath);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remote_file_browser_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        } else if (item.getItemId() == R.id.action_ssh_config) {
            // 触发SSH连接配置对话框
            showSSHConnectionDialog();
            return true;
        } else if (item.getItemId() == R.id.action_bookmarks) {
            // TODO: 后续实现收藏路径功能
            Logger.logInfo(LOG_TAG, "Bookmarks menu clicked - to be implemented");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        Logger.logInfo(LOG_TAG, "RemoteFileBrowserActivity destroyed");
        
        // 清理RxJava订阅
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        
        // 断开SFTP连接
        if (sftpManager != null) {
            sftpManager.disconnect();
        }
        
        super.onDestroy();
    }
    
    // RemoteFileBrowserAdapter.OnFileClickListener 实现
    
    @Override
    public void onFileClick(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "File clicked: " + file.getName());
        
        if (file.isDirectory()) {
            // 导航到目录，这会自动同步目录树
            navigateToDirectory(file.getPath());
        } else {
            // 显示文件操作菜单
            showFileOperationsDialog(file);
        }
    }
    
    @Override
    public void onFileLongClick(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "File long clicked: " + file.getName());
        // 长按启用选择模式，已在适配器中处理
    }
    
    @Override
    public void onMoreOptionsClick(RemoteFileItem file, View anchorView) {
        Logger.logInfo(LOG_TAG, "More options clicked for: " + file.getName());
        showFileOperationsDialog(file);
    }
    
    /**
     * 显示文件操作对话框
     */
    private void showFileOperationsDialog(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "Showing file operations dialog for: " + file.getName());
        // TODO: 实现文件操作对话框
        // 暂时显示简单的Toast
        LightToast.showShort(this, "文件操作: " + file.getName());
    }
    
    // FileTreeAdapter.OnFileTreeActionListener 实现
    
    @Override
    public void onNodeExpanded(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree node expanded: " + node.getName());
        // 保存展开状态到项目工作区
        if (currentWorkspace != null) {
            currentWorkspace.getExpandedFolders().put(node.getFullPath(), true);
            workspaceManager.saveWorkspaceState(currentWorkspace);
        }
    }
    
    @Override
    public void onNodeCollapsed(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree node collapsed: " + node.getName());
        // 保存折叠状态到项目工作区
        if (currentWorkspace != null) {
            currentWorkspace.getExpandedFolders().put(node.getFullPath(), false);
            workspaceManager.saveWorkspaceState(currentWorkspace);
        }
    }
    
    @Override
    public void onFileSelected(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree file selected: " + node.getName());
        
        if (node.isDirectory()) {
            // 同步主内容区到选中的目录
            navigateToDirectory(node.getFullPath());
        } else {
            // 文件被选中，可以实现文件操作
            RemoteFileItem fileItem = createRemoteFileItemFromNode(node);
            showFileOperationsDialog(fileItem);
        }
    }
    
    @Override
    public void onDirectoryBookmarked(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree directory bookmarked: " + node.getName());
        
        if (currentWorkspace != null && node.isDirectory()) {
            // 创建书签对象
            DirectoryBookmark bookmark = new DirectoryBookmark(
                null, // ID will be auto-generated
                node.getName(),
                node.getFullPath(),
                currentWorkspace.getId()
            );
            
            // 检查是否已存在书签
            if (!workspaceManager.isBookmarked(currentWorkspace.getId(), node.getFullPath())) {
                workspaceManager.addBookmark(currentWorkspace.getId(), bookmark);
                LightToast.showShort(this, "已添加书签: " + node.getName());
                
                // 标记为已收藏
                node.setBookmarked(true);
                treeAdapter.refreshNode(node);
                
                // 刷新书签列表
                loadBookmarks();
            } else {
                LightToast.showShort(this, "书签已存在: " + node.getName());
            }
        }
    }
    
    @Override
    public void onLoadNodeChildren(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Loading children for tree node: " + node.getName());
        loadNodeChildren(node);
    }
    
    /**
     * 加载目录树节点的子节点
     */
    private void loadNodeChildren(FileTreeNode parentNode) {
        if (!isConnected || sftpManager == null) {
            Logger.logError(LOG_TAG, "Not connected, cannot load node children");
            return;
        }
        
        // 使用SFTP管理器获取目录列表
        Disposable disposable = sftpManager.listFiles(parentNode.getFullPath())
            .subscribe(
                files -> {
                    Logger.logInfo(LOG_TAG, "Loaded " + files.size() + " children for " + parentNode.getName());
                    
                    // 转换为FileTreeNode并只包含目录
                    List<FileTreeNode> childNodes = new ArrayList<>();
                    for (RemoteFileItem file : files) {
                        if (file.isDirectory()) { // 只显示目录在树中
                            FileTreeNode childNode = new FileTreeNode(
                                file.getName(),
                                file.getPath(),
                                file.isDirectory()
                            );
                            childNode.setDepth(parentNode.getDepth() + 1);
                            childNode.setFileType(file.getType());
                            childNodes.add(childNode);
                        }
                    }
                    
                    // 添加到父节点
                    treeAdapter.addChildrenToNode(parentNode, childNodes);
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "Failed to load node children: " + throwable.getMessage());
                    parentNode.setLoading(false);
                    treeAdapter.refreshNode(parentNode);
                }
            );
        
        compositeDisposable.add(disposable);
    }
    
    /**
     * 从FileTreeNode创建RemoteFileItem
     */
    private RemoteFileItem createRemoteFileItemFromNode(FileTreeNode node) {
        RemoteFileItem item = new RemoteFileItem();
        item.setName(node.getName());
        item.setPath(node.getFullPath());
        item.setDirectory(node.isDirectory());
        item.setType(node.getFileType());
        // 其他属性需要从SFTP获取具体文件信息，这里先用默认值
        item.setSize(0);
        item.setLastModified(0);
        item.setPermissions("");
        return item;
    }
    
    // BookmarksAdapter.OnBookmarkActionListener 实现
    
    @Override
    public void onBookmarkClick(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Bookmark clicked: " + bookmark.getDisplayName());
        
        // 导航到书签路径
        navigateToDirectory(bookmark.getFullPath());
        
        // 关闭抽屉
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    
    @Override
    public void onBookmarkLongClick(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Bookmark long clicked: " + bookmark.getDisplayName());
        // 显示书签编辑操作菜单
        showBookmarkOptionsDialog(bookmark);
    }
    
    @Override
    public void onBookmarkRemove(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Removing bookmark: " + bookmark.getDisplayName());
        
        // 从数据库删除书签
        boolean removed = workspaceManager.removeBookmark(bookmark.getId());
        if (removed) {
            // 从适配器删除
            bookmarksAdapter.removeBookmark(bookmark);
            
            // 更新目录树中的书签状态
            syncBookmarkStates();
            
            LightToast.showShort(this, "已删除书签: " + bookmark.getDisplayName());
        } else {
            LightToast.showShort(this, "删除书签失败");
        }
    }
    
    /**
     * 显示书签操作对话框
     */
    private void showBookmarkOptionsDialog(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Showing bookmark options dialog for: " + bookmark.getDisplayName());
        
        // 创建操作选项列表
        String[] options = {"跳转到该目录", "重命名书签", "删除书签"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("书签操作: " + bookmark.getDisplayName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 跳转到该目录
                            onBookmarkClick(bookmark);
                            break;
                        case 1: // 重命名书签
                            showRenameBookmarkDialog(bookmark);
                            break;
                        case 2: // 删除书签
                            onBookmarkRemove(bookmark);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示重命名书签对话框
     */
    private void showRenameBookmarkDialog(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Showing rename bookmark dialog for: " + bookmark.getDisplayName());
        
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setText(bookmark.getDisplayName());
        editText.setSelection(bookmark.getDisplayName().length());
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("重命名书签")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(bookmark.getDisplayName())) {
                        // 更新书签名称
                        bookmark.setDisplayName(newName);
                        workspaceManager.addBookmark(currentWorkspace.getId(), bookmark);
                        
                        // 刷新书签列表
                        loadBookmarks();
                        
                        LightToast.showShort(this, "书签已重命名");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // SSHConfigDialog.OnSSHConfigListener 实现
    
    @Override
    public void onSSHConnect(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "SSH connect requested from dialog: " + config.getHost());
        connectAndLoadProject(config);
    }
    
    @Override
    public void onSSHConfigSaved(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "SSH config saved from dialog: " + config.getHost());
        // 配置已保存，可以选择连接
        LightToast.showShort(this, "SSH配置已保存");
    }
    
    @Override
    public void onSSHConfigDeleted(String configName) {
        Logger.logInfo(LOG_TAG, "SSH config deleted from dialog: " + configName);
        LightToast.showShort(this, "SSH配置已删除: " + configName);
    }
    
    @Override
    public void onDialogClosed() {
        Logger.logInfo(LOG_TAG, "SSH config dialog closed");
    }
}

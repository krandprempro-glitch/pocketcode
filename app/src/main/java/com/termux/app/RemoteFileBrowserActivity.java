package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.FileTreeNode;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.app.adapters.RemoteFileBrowserAdapter;
import com.termux.app.adapters.FileTreeAdapter;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * VSCode风格远程文件浏览器主Activity
 * 提供目录树导航、文件浏览、书签管理等功能
 */
public class RemoteFileBrowserActivity extends AppCompatActivity implements RemoteFileBrowserAdapter.OnFileClickListener {
    
    private static final String LOG_TAG = "RemoteFileBrowserActivity";
    
    // UI组件
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView projectNameText;
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private RecyclerView breadcrumbRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView fileContentRecyclerView;
    private RecyclerView fileTreeRecyclerView;
    private RecyclerView bookmarksRecyclerView;
    private Spinner projectSpinner;
    private ImageButton addProjectButton;
    private Button quickConnectButton;
    private TextView fileCountText;
    private TextView selectionInfoText;
    private ProgressBar loadingProgress;
    private View emptyStateLayout;
    
    // 数据和状态
    private SSHConnectionConfig currentConnection;
    private String currentPath = "/";
    private boolean isConnected = false;
    
    // 管理器和适配器
    private SFTPConnectionManager sftpManager;
    private RemoteFileBrowserAdapter fileAdapter;
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
        setupDrawer();
        setupRecyclerViews();
        setupEventListeners();
        
        // 处理Intent传递的连接配置
        handleIntent(getIntent());
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        projectNameText = findViewById(R.id.project_name);
        connectionStatusIcon = findViewById(R.id.connection_status_icon);
        connectionStatusText = findViewById(R.id.connection_status);
        breadcrumbRecyclerView = findViewById(R.id.breadcrumb_recyclerview);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fileContentRecyclerView = findViewById(R.id.file_content_recyclerview);
        fileTreeRecyclerView = findViewById(R.id.file_tree_recyclerview);
        bookmarksRecyclerView = findViewById(R.id.bookmarks_recyclerview);
        projectSpinner = findViewById(R.id.project_spinner);
        addProjectButton = findViewById(R.id.add_project_button);
        quickConnectButton = findViewById(R.id.quick_connect_button);
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
        }
    }
    
    /**
     * 设置抽屉菜单
     */
    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerViews() {
        // 路径导航栏
        breadcrumbRecyclerView.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        
        // 文件内容列表
        fileContentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new RemoteFileBrowserAdapter(this);
        fileContentRecyclerView.setAdapter(fileAdapter);
        
        // 目录树
        fileTreeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 书签列表
        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        Logger.logInfo(LOG_TAG, "RecyclerViews configured");
    }
    
    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentDirectory);
        
        // 快速连接按钮
        quickConnectButton.setOnClickListener(v -> showQuickConnectPanel());
        
        // 添加项目按钮
        addProjectButton.setOnClickListener(v -> showAddProjectDialog());
        
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
                    showQuickConnectPanel();
                }
            } else {
                // 没有配置，显示快速连接面板
                showQuickConnectPanel();
            }
        } else {
            showQuickConnectPanel();
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
        
        // 加载项目工作区
        loadProjectWorkspace(currentConnection);
        
        // 加载根目录
        loadDirectoryTree("/");
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
        // TODO: 实现项目工作区管理
        Logger.logInfo(LOG_TAG, "Loading project workspace for " + config.getHost());
    }
    
    /**
     * 加载目录树
     */
    private void loadDirectoryTree(String path) {
        Logger.logInfo(LOG_TAG, "Loading directory tree for path: " + path);
        // TODO: 实现目录树加载逻辑
    }
    
    /**
     * 导航到指定目录
     */
    private void navigateToDirectory(String dirPath) {
        Logger.logInfo(LOG_TAG, "Navigating to directory: " + dirPath);
        
        currentPath = dirPath;
        updateBreadcrumb(dirPath);
        loadDirectoryContent(dirPath);
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
                    fileAdapter.updateFiles(files);
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
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
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
            // 导航到目录
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
        android.widget.Toast.makeText(this, 
            "文件操作: " + file.getName(), 
            android.widget.Toast.LENGTH_SHORT).show();
    }
}

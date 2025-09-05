package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.ProjectWorkspace;
import com.termux.app.models.DirectoryBookmark;
import com.termux.app.models.FileTreeNode;
import com.termux.app.models.DrawerMenuItem;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.app.adapters.RemoteFileBrowserAdapter;
import com.termux.app.adapters.BookmarksAdapter;
import com.termux.app.adapters.FileTreeAdapter;
import com.termux.app.adapters.DrawerMenuAdapter;
import com.termux.app.managers.ProjectWorkspaceManager;
import com.termux.app.models.SSHConfigManager;
import com.termux.app.ui.SSHConfigDialog;
import com.termux.app.utils.NetworkErrorHandler;
import com.termux.app.utils.LightToast;
import com.termux.shared.logger.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 远程文件浏览Fragment
 * 实现SFTP文件浏览功能
 */
public class RemoteFileBrowserFragment extends Fragment implements 
        RemoteFileBrowserAdapter.OnFileClickListener,
        SSHConfigDialog.OnSSHConfigListener,
        FileTreeAdapter.OnFileTreeActionListener,
        BookmarksAdapter.OnBookmarkActionListener {
    private static final String LOG_TAG = "RemoteFileBrowserFragment";
    
    // UI组件
    private Toolbar toolbar;
    private TextView projectNameText;
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private TextView currentPathText;
    private ImageView bookmarkIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView fileContentRecyclerView;
    private TextView fileCountText;
    private TextView selectionInfoText;
    private ProgressBar loadingProgress;
    private View emptyStateLayout;
    private DrawerLayout drawerLayout;
    
    // 抽屉相关组件
    private TextView breadcrumbText;
    private TextView drawerProjectName;
    private TextView drawerConnectionInfo;
    private TextView drawerCurrentPath;
    private FrameLayout mainContentArea;
    private View welcomeLayout;
    private FrameLayout codeViewerContainer;
    private TextView statusTextLeft;
    private TextView statusTextRight;
    
    // 原有组件（兼容性保留）
    private RecyclerView rvFiles;
    private EditText etCurrentPath;
    private Button btnConnect, btnBack, btnRefresh;
    private ImageButton btnSshConnect;
    private ProgressBar progressLoading;
    
    // 数据和状态
    private SSHConnectionConfig currentConnection;
    private ProjectWorkspace currentWorkspace;
    private boolean isConnected = false;
    
    // 管理器和适配器
    private SFTPConnectionManager sftpManager;
    private ProjectWorkspaceManager workspaceManager;
    private RemoteFileBrowserAdapter fileAdapter;
    private BookmarksAdapter bookmarksAdapter;
    private FileTreeAdapter treeAdapter;
    private CompositeDisposable compositeDisposable;
    
    // 抽屉菜单相关
    private RecyclerView drawerMenuRecyclerView;
    private DrawerMenuAdapter drawerMenuAdapter;
    private List<DrawerMenuItem> drawerMenuItems;
    
    // 原有组件（兼容性保留）
    private SFTPConnectionManager connectionManager;
    private SSHConfigManager configManager;
    private SSHConfigDialog configDialog;
    
    private String currentPath = "/";
    private SSHConnectionConfig currentConfig;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化管理器
        sftpManager = SFTPConnectionManager.getInstance();
        workspaceManager = ProjectWorkspaceManager.getInstance(requireContext());
        compositeDisposable = new CompositeDisposable();
        
        // 兼容性保留
        connectionManager = sftpManager;
        configManager = SSHConfigManager.getInstance(requireContext());
        
        // 启用菜单
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        configDialog = new SSHConfigDialog(context);
        setupSSHConfigDialog();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote_file_browser_drawer, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupToolbar();
        setupRecyclerViews();
        setupEventListeners();
        updateConnectionStatus(false, "未连接");
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews(View view) {
        // 抽屉布局组件
        drawerLayout = view.findViewById(R.id.drawer_layout);
        com.google.android.material.navigation.NavigationView navView = view.findViewById(R.id.nav_view);
        
        // 动态设置抽屉宽度为屏幕宽度的50%
        if (navView != null) {
            setDrawerWidth(navView);
        }
        
        // ActionBar组件
        toolbar = view.findViewById(R.id.toolbar);
        breadcrumbText = view.findViewById(R.id.breadcrumb_text);
        connectionStatusIcon = view.findViewById(R.id.connection_status_icon);
        connectionStatusText = view.findViewById(R.id.connection_status_text);
        
        // 主内容区域
        mainContentArea = view.findViewById(R.id.main_content_area);
        welcomeLayout = view.findViewById(R.id.welcome_layout);
        codeViewerContainer = view.findViewById(R.id.code_viewer_container);
        
        // 状态栏
        statusTextLeft = view.findViewById(R.id.status_text_left);
        statusTextRight = view.findViewById(R.id.status_text_right);
        
        // 抽屉头部
        drawerProjectName = view.findViewById(R.id.drawer_project_name);
        drawerConnectionInfo = view.findViewById(R.id.drawer_connection_info);
        drawerCurrentPath = view.findViewById(R.id.drawer_current_path);
        
        // 抽屉内容区域
        SwipeRefreshLayout drawerSwipeRefresh = view.findViewById(R.id.drawer_swipe_refresh);
        RecyclerView drawerFileList = view.findViewById(R.id.drawer_file_list);
        View drawerEmptyState = view.findViewById(R.id.drawer_empty_state);
        ProgressBar drawerLoadingProgress = view.findViewById(R.id.drawer_loading_progress);
        
        // 兼容性映射：将抽屉中的文件列表映射到原有变量
        swipeRefreshLayout = drawerSwipeRefresh;
        fileContentRecyclerView = drawerFileList;
        emptyStateLayout = drawerEmptyState;
        loadingProgress = drawerLoadingProgress;
        
        // 兼容性映射：将新布局的组件映射到原有的变量名
        rvFiles = fileContentRecyclerView;
        progressLoading = loadingProgress;
        
        // 兼容性映射：路径显示
        projectNameText = drawerProjectName;
        currentPathText = drawerCurrentPath;
        
        // 这些组件在新布局中不存在或已重新映射，设为null以防空指针
        etCurrentPath = null; // 新布局中路径显示为TextView，不可编辑
        btnConnect = null; // 新布局中没有连接按钮
        btnBack = null; // 新布局中没有返回按钮
        btnRefresh = null; // 新布局中没有刷新按钮
        btnSshConnect = null; // 新布局中没有SSH连接按钮
        bookmarkIndicator = null; // 新布局中没有书签指示器
        fileCountText = statusTextLeft; // 文件计数显示在状态栏左侧
        selectionInfoText = statusTextRight; // 选择信息显示在状态栏右侧
        
        // 抽屉菜单RecyclerView
        LinearLayout drawerMenuContainer = view.findViewById(R.id.drawer_menu_container);
        setupDrawerMenu(drawerMenuContainer);
        
        Logger.logInfo(LOG_TAG, "Drawer views initialized");
    }

    /**
     * 动态设置抽屉宽度为屏幕宽度的50%
     */
    private void setDrawerWidth(com.google.android.material.navigation.NavigationView navView) {
        if (getActivity() != null) {
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int drawerWidth = screenWidth / 2; // 50% 屏幕宽度
            
            ViewGroup.LayoutParams params = navView.getLayoutParams();
            params.width = drawerWidth;
            navView.setLayoutParams(params);
        }
    }
    
    /**
     * 设置工具栏和抽屉
     */
    private void setupToolbar() {
        if (toolbar != null && getActivity() != null) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu); // 汉堡菜单图标
            }
        }
        
        // 设置抽屉切换
        setupDrawerToggle();
    }

    /**
     * 设置抽屉开关切换
     */
    private void setupDrawerToggle() {
        if (drawerLayout != null && toolbar != null) {
            androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                getActivity(), drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            
            // 抽屉状态监听
            drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    // 可以添加滑动动画效果
                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    Logger.logInfo(LOG_TAG, "Drawer opened");
                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    Logger.logInfo(LOG_TAG, "Drawer closed");
                }

                @Override
                public void onDrawerStateChanged(int newState) {
                    // 抽屉状态变化
                }
            });
        }
    }
    
    /**
     * 设置抽屉菜单
     */
    private void setupDrawerMenu(LinearLayout menuContainer) {
        if (menuContainer == null) return;
        
        // 创建RecyclerView用于菜单
        drawerMenuRecyclerView = new RecyclerView(requireContext());
        drawerMenuRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        drawerMenuRecyclerView.setNestedScrollingEnabled(false);
        
        // 设置适配器
        drawerMenuAdapter = new DrawerMenuAdapter(requireContext(), new DrawerMenuAdapter.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(DrawerMenuItem item) {
                handleDrawerMenuClick(item);
            }
            
            @Override
            public void onSubMenuItemClick(DrawerMenuItem parentItem, DrawerMenuItem subItem) {
                handleDrawerSubMenuClick(parentItem, subItem);
            }
        });
        
        drawerMenuRecyclerView.setAdapter(drawerMenuAdapter);
        
        // 添加到容器
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        menuContainer.addView(drawerMenuRecyclerView, params);
        
        // 初始化菜单项
        initDrawerMenuItems();
    }

    /**
     * 初始化抽屉菜单项
     */
    private void initDrawerMenuItems() {
        drawerMenuItems = new ArrayList<>();
        
        // 创建主功能模块 (可折叠的模块，包含4个核心功能)
        DrawerMenuItem mainFunctionsModule = new DrawerMenuItem("main_functions", "功能菜单", R.drawable.ic_menu);
        
        // 添加SSH配置子项
        mainFunctionsModule.addSubItem(new DrawerMenuItem("ssh_config", "SSH连接配置", R.drawable.ic_ssh, DrawerMenuItem.MenuAction.SSH_CONFIG));
        
        // 添加收藏夹管理子项 (本身也是可展开的)
        DrawerMenuItem bookmarksSubItem = new DrawerMenuItem("bookmarks", "收藏夹管理", R.drawable.ic_bookmark_small);
        bookmarksSubItem.addSubItem(new DrawerMenuItem("bookmark_add", "收藏当前目录", R.drawable.ic_bookmark_add, DrawerMenuItem.MenuAction.BOOKMARK_ADD_CURRENT));
        bookmarksSubItem.addSubItem(new DrawerMenuItem("bookmark_manage", "管理所有书签", R.drawable.ic_settings_small, DrawerMenuItem.MenuAction.BOOKMARK_MANAGE_ALL));
        
        // 根据当前收藏数量更新徽章
        updateBookmarksBadge(bookmarksSubItem);
        mainFunctionsModule.addSubItem(bookmarksSubItem);
        
        // 添加刷新目录子项
        mainFunctionsModule.addSubItem(new DrawerMenuItem("refresh", "刷新目录", R.drawable.ic_refresh_small, DrawerMenuItem.MenuAction.REFRESH));
        
        // 添加设置选项子项
        DrawerMenuItem settingsSubItem = new DrawerMenuItem("settings", "设置选项", R.drawable.ic_settings_small);
        // 可以添加设置的具体子项
        settingsSubItem.addSubItem(new DrawerMenuItem("settings_display", "显示设置", R.drawable.ic_settings_small, DrawerMenuItem.MenuAction.SETTINGS_DISPLAY));
        settingsSubItem.addSubItem(new DrawerMenuItem("settings_connection", "连接设置", R.drawable.ic_settings_small, DrawerMenuItem.MenuAction.SETTINGS_CONNECTION));
        mainFunctionsModule.addSubItem(settingsSubItem);
        
        // 将主功能模块添加到菜单列表
        drawerMenuItems.add(mainFunctionsModule);
        
        // TODO: 在这里可以添加其他独立模块
        // 例如：文件操作模块、项目管理模块等
        
        // 更新适配器
        drawerMenuAdapter.updateMenuItems(drawerMenuItems);
    }

    /**
     * 更新收藏夹徽章显示
     */
    private void updateBookmarksBadge(DrawerMenuItem bookmarksMenuItem) {
        if (currentWorkspace != null && workspaceManager != null) {
            List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
            int bookmarkCount = bookmarks.size();
            
            if (bookmarkCount > 0) {
                bookmarksMenuItem.setBadgeText(String.valueOf(bookmarkCount));
                
                // 更新"管理所有书签"子项的徽章
                List<DrawerMenuItem> subItems = bookmarksMenuItem.getSubItems();
                if (subItems != null && subItems.size() > 1) {
                    subItems.get(1).setBadgeText("(" + bookmarkCount + ")");
                }
            } else {
                bookmarksMenuItem.setBadgeText(null);
            }
        }
    }

    /**
     * 处理抽屉菜单点击
     */
    private void handleDrawerMenuClick(DrawerMenuItem item) {
        Logger.logInfo(LOG_TAG, "Drawer menu clicked: " + item.getTitle());
        
        if (item.getAction() == null) return;
        
        switch (item.getAction()) {
            case SSH_CONFIG:
                showConnectionDialog();
                closeDrawer();
                break;
            case REFRESH:
                refreshCurrentDirectory();
                closeDrawer();
                break;
            default:
                break;
        }
    }

    /**
     * 处理抽屉子菜单点击
     */
    private void handleDrawerSubMenuClick(DrawerMenuItem parentItem, DrawerMenuItem subItem) {
        Logger.logInfo(LOG_TAG, "Drawer submenu clicked: " + parentItem.getTitle() + " -> " + subItem.getTitle());
        
        // 如果子项有action，直接执行
        if (subItem.getAction() != null) {
            switch (subItem.getAction()) {
                case SSH_CONFIG:
                    showConnectionDialog();
                    closeDrawer();
                    break;
                case REFRESH:
                    refreshCurrentDirectory();
                    closeDrawer();
                    break;
                case BOOKMARK_ADD_CURRENT:
                    addBookmarkForCurrentDirectory();
                    closeDrawer();
                    break;
                case BOOKMARK_MANAGE_ALL:
                    showAllBookmarksDialog();
                    closeDrawer();
                    break;
                case BOOKMARK_NAVIGATE:
                    // 动态书签导航将在下一个批次实现
                    break;
                case SETTINGS:
                    // 设置功能待实现
                    if (getContext() != null) {
                        LightToast.showShort(getContext(), "设置功能待实现");
                    }
                    closeDrawer();
                    break;
                case SETTINGS_DISPLAY:
                    // 显示设置功能待实现
                    if (getContext() != null) {
                        LightToast.showShort(getContext(), "显示设置功能待实现");
                    }
                    closeDrawer();
                    break;
                case SETTINGS_CONNECTION:
                    // 连接设置功能待实现
                    if (getContext() != null) {
                        LightToast.showShort(getContext(), "连接设置功能待实现");
                    }
                    closeDrawer();
                    break;
                default:
                    break;
            }
        } else {
            // 如果子项没有action但有子项，这意味着它是一个可展开的菜单组
            // 这种情况下，点击会触发展开/折叠，由适配器处理
            Logger.logInfo(LOG_TAG, "Clicked expandable submenu: " + subItem.getTitle());
        }
    }

    /**
     * 关闭抽屉
     */
    private void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * 刷新抽屉菜单 (在连接状态变化时调用)
     */
    private void refreshDrawerMenu() {
        if (drawerMenuAdapter != null && drawerMenuItems != null) {
            // 更新收藏夹徽章 - 现在需要在主功能模块的子项中查找
            for (DrawerMenuItem mainItem : drawerMenuItems) {
                if ("main_functions".equals(mainItem.getId())) {
                    // 在主功能模块的子项中查找收藏夹项
                    List<DrawerMenuItem> subItems = mainItem.getSubItems();
                    if (subItems != null) {
                        for (DrawerMenuItem subItem : subItems) {
                            if ("bookmarks".equals(subItem.getId())) {
                                updateBookmarksBadge(subItem);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            
            drawerMenuAdapter.updateMenuItems(drawerMenuItems);
        }
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerViews() {
        // 文件内容列表
        if (fileContentRecyclerView != null) {
            fileContentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            fileAdapter = new RemoteFileBrowserAdapter(this);
            fileContentRecyclerView.setAdapter(fileAdapter);
        } else if (rvFiles != null) {
            // 兼容性处理
            setupRecyclerView();
        }
        
        Logger.logInfo(LOG_TAG, "RecyclerViews configured");
    }
    
    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        // 下拉刷新
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentDirectory);
        }
        
        // 当前路径长按收藏
        if (currentPathText != null) {
            currentPathText.setOnLongClickListener(v -> {
                showPathActionDialog();
                return true;
            });
        }
        
        // 原有监听器
        setupListeners();
        
        Logger.logInfo(LOG_TAG, "Event listeners configured");
    }
    
    /**
     * 显示路径操作对话框
     */
    private void showPathActionDialog() {
        if (currentPath == null || currentWorkspace == null) return;
        
        // 检查当前路径是否已收藏
        boolean isCurrentPathBookmarked = workspaceManager != null && 
                workspaceManager.isBookmarked(currentWorkspace.getId(), currentPath);
        
        List<String> options = new ArrayList<>();
        options.add("📋 复制路径");
        
        if (isCurrentPathBookmarked) {
            options.add("⭐ 取消收藏此目录");
        } else {
            options.add("⭐ 收藏此目录");
        }
        
        options.add("📚 查看所有书签");
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("当前路径: " + optimizePathDisplay(currentPath))
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    switch (which) {
                        case 0: // 复制路径
                            copyPathToClipboard();
                            break;
                        case 1: // 收藏/取消收藏
                            if (isCurrentPathBookmarked) {
                                removeBookmarkForPath(currentPath);
                            } else {
                                addBookmarkForCurrentDirectory();
                            }
                            break;
                        case 2: // 查看所有书签
                            showAllBookmarksDialog();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 复制当前路径到剪贴板
     */
    private void copyPathToClipboard() {
        if (getContext() == null || currentPath == null) return;
        
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        
        android.content.ClipData clip = android.content.ClipData.newPlainText("Remote Path", currentPath);
        clipboard.setPrimaryClip(clip);
        
        LightToast.showShort(getContext(), "路径已复制到剪贴板");
    }
    
    /**
     * 原有RecyclerView设置（兼容性保留）
     */
    private void setupRecyclerView() {
        if (fileAdapter == null) {
            // 新版本使用RemoteFileBrowserAdapter
            fileAdapter = new RemoteFileBrowserAdapter(this);
        }
        
        if (rvFiles != null) {
            rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFiles.setAdapter(fileAdapter);
        }
    }
    
    /**
     * 原有事件监听器（兼容性保留）
     */
    private void setupListeners() {
        // 这些按钮在新布局中不存在，跳过设置监听器
        if (btnConnect != null) {
            btnConnect.setOnClickListener(v -> {
                if (isConnected) {
                    disconnect();
                    btnConnect.setText("连接");
                } else {
                    showConnectionDialog();
                }
            });
        }
        if (btnSshConnect != null) {
            btnSshConnect.setOnClickListener(v -> showConnectionDialog());
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refreshCurrentDirectory());
        }
        
        if (etCurrentPath != null) {
            etCurrentPath.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    navigateToPath(etCurrentPath.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // 不再使用ActionBar菜单，所有功能已移至抽屉菜单
        // inflater.inflate(R.menu.remote_file_browser_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            }
            return false;
        } else if (item.getItemId() == R.id.action_ssh_config) {
            showConnectionDialog();
            return true;
        } else if (item.getItemId() == R.id.action_bookmarks) {
            showBookmarksManagementDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupSSHConfigDialog() {
        if (configDialog != null) {
            configDialog.setOnSSHConfigListener(new SSHConfigDialog.OnSSHConfigListener() {
                @Override
                public void onSSHConnect(SSHConnectionConfig config) {
                    connectToServer(config);
                }
                
                @Override
                public void onSSHConfigSaved(SSHConnectionConfig config) {
                    // 配置已保存，无需特别处理
                }
                
                @Override
                public void onSSHConfigDeleted(String configName) {
                    // 配置已删除，无需特别处理
                }
                
                @Override
                public void onDialogClosed() {
                    // 对话框关闭，无需特别处理
                }
            });
        }
    }
    
    /**
     * 显示收藏管理对话框
     */
    private void showBookmarksManagementDialog() {
        if (currentWorkspace == null) {
            if (getContext() != null) {
                LightToast.showShort(getContext(), "请先连接SSH服务器");
            }
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Showing bookmarks management dialog");
        
        // 获取当前项目的书签列表
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        
        // 创建对话框选项列表
        List<String> options = new ArrayList<>();
        options.add("📂 收藏当前目录");
        options.add("📚 管理所有书签 (" + bookmarks.size() + ")");
        
        if (!bookmarks.isEmpty()) {
            options.add(""); // 分隔符
            options.add("== 快速跳转到书签 ==");
            for (DirectoryBookmark bookmark : bookmarks) {
                options.add("⭐ " + bookmark.getDisplayName());
            }
        }
        
        // 显示选项对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("收藏夹管理")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        // 收藏当前目录
                        addBookmarkForCurrentDirectory();
                    } else if (which == 1) {
                        // 管理所有书签
                        showAllBookmarksDialog();
                    } else if (which > 3) {
                        // 跳转到指定书签
                        int bookmarkIndex = which - 4;
                        if (bookmarkIndex >= 0 && bookmarkIndex < bookmarks.size()) {
                            DirectoryBookmark bookmark = bookmarks.get(bookmarkIndex);
                            navigateToDirectory(bookmark.getFullPath());
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 为当前目录添加书签
     */
    private void addBookmarkForCurrentDirectory() {
        if (currentWorkspace == null || workspaceManager == null) {
            if (getContext() != null) {
                LightToast.showShort(getContext(), "无法添加书签：工作区未就绪");
            }
            return;
        }
        
        if (currentPath == null || currentPath.isEmpty()) {
            if (getContext() != null) {
                LightToast.showShort(getContext(), "无法添加书签：当前路径无效");
            }
            return;
        }
        
        // 检查是否已经收藏
        if (workspaceManager.isBookmarked(currentWorkspace.getId(), currentPath)) {
            if (getContext() != null) {
                LightToast.showShort(getContext(), "此目录已在收藏夹中");
            }
            return;
        }
        
        // 显示输入书签名称的对话框
        showAddBookmarkDialog(currentPath);
    }
    
    /**
     * 显示添加书签对话框
     */
    private void showAddBookmarkDialog(String path) {
        if (getContext() == null) return;
        
        // 生成默认书签名称
        final String defaultName = path.equals("/") ? "根目录" : path.substring(path.lastIndexOf('/') + 1);
        final String finalDefaultName = defaultName.isEmpty() ? "未命名目录" : defaultName;
        
        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setText(finalDefaultName);
        editText.setSelection(finalDefaultName.length());
        editText.setHint("请输入书签名称");
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("添加书签")
                .setMessage("路径: " + path)
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String inputName = editText.getText().toString().trim();
                    String finalBookmarkName = inputName.isEmpty() ? finalDefaultName : inputName;
                    
                    // 创建书签
                    DirectoryBookmark bookmark = new DirectoryBookmark(
                            null, // ID will be auto-generated
                            finalBookmarkName,
                            path,
                            currentWorkspace.getId()
                    );
                    
                    Logger.logInfo(LOG_TAG, "Creating bookmark: " + finalBookmarkName + " for path: " + path + " in workspace: " + currentWorkspace.getId());
                    
                    // 保存书签
                    workspaceManager.addBookmark(currentWorkspace.getId(), bookmark);
                    
                    // 验证书签是否保存成功
                    List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
                    Logger.logInfo(LOG_TAG, "After adding bookmark, total bookmarks: " + bookmarks.size());
                    
                    if (getContext() != null) {
                        LightToast.showShort(getContext(), "已添加书签: " + finalBookmarkName);
                    }
                    
                    // 同步书签状态
                    syncBookmarkStates();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示所有书签管理对话框
     */
    private void showAllBookmarksDialog() {
        if (currentWorkspace == null || workspaceManager == null) {
            Logger.logError(LOG_TAG, "Cannot show bookmarks: currentWorkspace=" + currentWorkspace + ", workspaceManager=" + workspaceManager);
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Showing all bookmarks for workspace: " + currentWorkspace.getId());
        
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        Logger.logInfo(LOG_TAG, "Retrieved bookmarks count: " + bookmarks.size());
        
        if (bookmarks.isEmpty()) {
            Logger.logInfo(LOG_TAG, "No bookmarks found, showing empty state dialog");
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("收藏夹")
                    .setMessage("还没有收藏任何目录。\n\n你可以通过以下方式添加书签：\n• 使用菜单中的「收藏当前目录」\n• 在目录树中长按目录")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }
        
        // 创建书签选项列表
        String[] bookmarkItems = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            DirectoryBookmark bookmark = bookmarks.get(i);
            bookmarkItems[i] = bookmark.getDisplayName() + "\n" + bookmark.getFullPath();
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("收藏夹 (" + bookmarks.size() + " 项)")
                .setItems(bookmarkItems, (dialog, which) -> {
                    DirectoryBookmark selectedBookmark = bookmarks.get(which);
                    showBookmarkOptionsDialog(selectedBookmark);
                })
                .setNegativeButton("关闭", null)
                .show();
    }
    
    private void showConnectionDialog() {
        Logger.logInfo(LOG_TAG, "Showing SSH connection dialog");
        if (configDialog != null) {
            configDialog.show();
        }
    }
    
    
    /**
     * 连接服务器并加载项目
     */
    private void connectAndLoadProject(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "Connecting to " + config.getHost());
        
        currentConnection = config;
        currentConfig = config; // 兼容性
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
     * 原有连接方法（兼容性保留）
     */
    private void connectToServer(SSHConnectionConfig config) {
        connectAndLoadProject(config);
    }
    
    private void onConnectionEstablished() {
        Logger.logInfo(LOG_TAG, "SFTP connection established");
        
        isConnected = true;
        updateConnectionStatus(true, "已连接 - " + currentConfig.getHost());
        if (btnConnect != null) {
            btnConnect.setText("断开");
        }
        
        // 加载项目工作区
        loadProjectWorkspace(currentConfig);
        
        // 刷新抽屉菜单状态
        refreshDrawerMenu();
        
        // 加载根目录
        navigateToPath("/");
        showLoading(false);
    }
    
    private void onConnectionFailed(Throwable error) {
        Logger.logError(LOG_TAG, "SFTP connection failed: " + error.getMessage());
        
        isConnected = false;
        updateConnectionStatus(false, "连接失败");
        if (btnConnect != null) {
            btnConnect.setText("连接");
        }
        showLoading(false);
        
        // 使用NetworkErrorHandler显示友好的错误信息
        NetworkErrorHandler.showErrorDialog(requireContext(), error, () -> {
            // 重试连接
            if (currentConfig != null) {
                connectToServer(currentConfig);
            }
        });
    }
    
    private void onConnectionFailed(String error) {
        onConnectionFailed(new Exception(error));
    }
    
    private void navigateToPath(String path) {
        if (!isConnected) {
            showErrorDialog("未连接", "请先建立SFTP连接");
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Navigating to: " + path);
        currentPath = path;
        if (etCurrentPath != null) {
            etCurrentPath.setText(path);
        }
        
        loadDirectoryContent(path);
        updateBackButton();
    }
    
    private void navigateBack() {
        if (!currentPath.equals("/")) {
            String parentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            if (parentPath.isEmpty()) {
                parentPath = "/";
            }
            navigateToPath(parentPath);
        }
    }
    
    private void refreshCurrentDirectory() {
        if (isConnected) {
            loadDirectoryContent(currentPath);
        }
    }
    
    private void loadDirectoryContent(String path) {
        Logger.logInfo(LOG_TAG, "Loading directory content: " + path);
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
                    
                    // 停止刷新动画
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "Failed to load directory content: " + throwable.getMessage());
                    showErrorMessage("加载目录失败: " + throwable.getMessage());
                    showEmptyState(true);
                    showLoading(false);
                    
                    // 停止刷新动画
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    
                    // 使用NetworkErrorHandler显示友好的错误信息
                    NetworkErrorHandler.showErrorDialog(requireContext(), throwable, () -> {
                        // 重试加载目录
                        loadDirectoryContent(path);
                    });
                }
            );
        
        compositeDisposable.add(disposable);
    }
    
    private void updateConnectionStatus(boolean connected, String statusText) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                connectionStatusIcon.setImageResource(connected ? 
                    android.R.drawable.presence_online : android.R.drawable.presence_offline);
                connectionStatusText.setText(statusText);
            });
        }
    }
    
    private void updateBackButton() {
        if (btnBack != null) {
            btnBack.setEnabled(!currentPath.equals("/"));
        }
    }
    
    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
    
    private void showErrorDialog(String title, String message) {
        if (getContext() != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
        }
    }
    
    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        // TODO: 实现错误消息显示
        Logger.logError(LOG_TAG, "Error: " + message);
    }
    
    
    private void showFileOptionsDialog(RemoteFileItem file) {
        String[] options;
        if (file.isDirectory()) {
            options = new String[]{"进入目录", "查看属性"};
        } else {
            options = new String[]{"下载文件", "查看属性"};
        }
        
        new AlertDialog.Builder(requireContext())
            .setTitle(file.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        if (file.isDirectory()) {
                            navigateToPath(file.getPath());
                        } else {
                            // TODO: 实现文件下载
                            showErrorDialog("提示", "文件下载功能待实现");
                        }
                        break;
                    case 1:
                        showFilePropertiesDialog(file);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showFilePropertiesDialog(RemoteFileItem file) {
        StringBuilder properties = new StringBuilder();
        properties.append("名称: ").append(file.getName()).append("\n");
        properties.append("路径: ").append(file.getPath()).append("\n");
        properties.append("类型: ").append(file.isDirectory() ? "目录" : "文件").append("\n");
        properties.append("大小: ").append(file.getSize()).append(" 字节\n");
        properties.append("权限: ").append(file.getPermissions()).append("\n");
        properties.append("修改时间: ").append(new java.util.Date(file.getLastModified()));
        
        new AlertDialog.Builder(requireContext())
            .setTitle("文件属性")
            .setMessage(properties.toString())
            .setPositiveButton("确定", null)
            .show();
    }
    
    /**
     * SSH连接成功回调
     * 从SSH连接Fragment调用
     */
    public void onSSHConnected(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "SSH connected from SSH tab: " + config.getHost());
        currentConfig = config;
        connectToServer(config);
    }
    
    /**
     * SSH断开连接回调
     * 从SSH连接Fragment调用
     */
    public void onSSHDisconnected() {
        Logger.logInfo(LOG_TAG, "SSH disconnected from SSH tab");
        disconnect();
    }
    
    /**
     * 断开SFTP连接
     */
    private void disconnect() {
        if (connectionManager != null && isConnected) {
            connectionManager.disconnect();
            isConnected = false;
            currentConfig = null;
            updateConnectionStatus(false, "已断开连接");
            
            // 清空文件列表
            if (fileAdapter != null) {
                fileAdapter.updateFiles(null);
            }
            
            // 重置当前路径
            currentPath = "/";
            if (etCurrentPath != null) {
                etCurrentPath.setText(currentPath);
            }
            if (btnBack != null) {
                btnBack.setEnabled(false);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        if (connectionManager != null && isConnected) {
            connectionManager.disconnect();
        }
        if (configDialog != null && configDialog.isShowing()) {
            configDialog.dismiss();
        }
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
        
        if (getContext() == null) return;
        
        // 准备操作选项
        List<String> options = new ArrayList<>();
        
        if (file.isDirectory()) {
            options.add("📂 进入目录");
            
            // 检查是否已收藏
            boolean isBookmarked = currentWorkspace != null && workspaceManager != null && 
                    workspaceManager.isBookmarked(currentWorkspace.getId(), file.getPath());
            
            if (isBookmarked) {
                options.add("⭐ 取消收藏");
            } else {
                options.add("⭐ 收藏目录");
            }
            
            options.add("ℹ️ 查看属性");
        } else {
            options.add("📥 下载文件");
            options.add("ℹ️ 查看属性");
        }
        
        // 创建对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(file.getName())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (file.isDirectory()) {
                        handleDirectoryOperation(file, which, options);
                    } else {
                        handleFileOperation(file, which, options);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 处理目录操作
     */
    private void handleDirectoryOperation(RemoteFileItem file, int optionIndex, List<String> options) {
        String selectedOption = options.get(optionIndex);
        
        if (selectedOption.contains("进入目录")) {
            navigateToDirectory(file.getPath());
        } else if (selectedOption.contains("收藏目录")) {
            showAddBookmarkDialog(file.getPath());
        } else if (selectedOption.contains("取消收藏")) {
            removeBookmarkForPath(file.getPath());
        } else if (selectedOption.contains("查看属性")) {
            showFilePropertiesDialog(file);
        }
    }
    
    /**
     * 处理文件操作
     */
    private void handleFileOperation(RemoteFileItem file, int optionIndex, List<String> options) {
        String selectedOption = options.get(optionIndex);
        
        if (selectedOption.contains("下载文件")) {
            // TODO: 实现文件下载
            if (getContext() != null) {
                LightToast.showShort(getContext(), "文件下载功能待实现");
            }
        } else if (selectedOption.contains("查看属性")) {
            showFilePropertiesDialog(file);
        }
    }
    
    /**
     * 删除指定路径的书签
     */
    private void removeBookmarkForPath(String path) {
        if (currentWorkspace == null || workspaceManager == null) return;
        
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        for (DirectoryBookmark bookmark : bookmarks) {
            if (path.equals(bookmark.getFullPath())) {
                boolean removed = workspaceManager.removeBookmark(bookmark.getId());
                if (removed) {
                    if (getContext() != null) {
                        LightToast.showShort(getContext(), "已取消收藏: " + bookmark.getDisplayName());
                    }
                    // 同步书签状态
                    syncBookmarkStates();
                }
                break;
            }
        }
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
     * 加载项目工作区
     */
    private void loadProjectWorkspace(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "Loading project workspace for " + config.getHost());
        
        if (workspaceManager == null) return;
        
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
            
            // 刷新抽屉菜单 (更新收藏夹数量)
            refreshDrawerMenu();
        } else {
            Logger.logError(LOG_TAG, "Failed to create workspace for connection");
        }
    }
    
    /**
     * 更新项目信息
     */
    private void updateProjectInfo(String projectName) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (projectNameText != null) {
                    projectNameText.setText(projectName);
                }
            });
        }
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
        if (etCurrentPath != null) {
            etCurrentPath.setText(path);
        }
        
        // 更新收藏状态指示器
        updateBookmarkIndicator(path);
    }
    
    /**
     * 更新收藏状态指示器
     */
    private void updateBookmarkIndicator(String path) {
        if (bookmarkIndicator == null || currentWorkspace == null || workspaceManager == null) {
            return;
        }
        
        // 检查当前路径是否已收藏
        boolean isBookmarked = workspaceManager.isBookmarked(currentWorkspace.getId(), path);
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                bookmarkIndicator.setVisibility(isBookmarked ? View.VISIBLE : View.GONE);
            });
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
     * 更新文件计数
     */
    private void updateFileCount(int count) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (fileCountText != null) {
                    fileCountText.setText(count + " 项");
                }
            });
        }
    }
    
    /**
     * 显示/隐藏空状态
     */
    private void showEmptyState(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyStateLayout != null) {
                    emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (fileContentRecyclerView != null) {
                        fileContentRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                }
            });
        }
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
        if (currentWorkspace != null && workspaceManager != null) {
            currentWorkspace.setCurrentPath(newPath);
            workspaceManager.saveWorkspaceState(currentWorkspace);
        }
        
        // 更新面包屑
        updateBreadcrumb(newPath);
    }
    
    /**
     * 更新面包屑导航
     */
    private void updateBreadcrumb(String path) {
        // TODO: 实现面包屑导航更新
        Logger.logInfo(LOG_TAG, "Updating breadcrumb for path: " + path);
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
    
    // FileTreeAdapter.OnFileTreeActionListener 实现
    
    @Override
    public void onNodeExpanded(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree node expanded: " + node.getName());
        // 保存展开状态到项目工作区
        if (currentWorkspace != null && workspaceManager != null) {
            currentWorkspace.getExpandedFolders().put(node.getFullPath(), true);
            workspaceManager.saveWorkspaceState(currentWorkspace);
        }
    }
    
    @Override
    public void onNodeCollapsed(FileTreeNode node) {
        Logger.logInfo(LOG_TAG, "Tree node collapsed: " + node.getName());
        // 保存折叠状态到项目工作区
        if (currentWorkspace != null && workspaceManager != null) {
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
        
        if (currentWorkspace != null && workspaceManager != null && node.isDirectory()) {
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
                if (getContext() != null) {
                    LightToast.showShort(getContext(), "已添加书签: " + node.getName());
                }
                
                // 标记为已收藏
                node.setBookmarked(true);
                if (treeAdapter != null) {
                    treeAdapter.refreshNode(node);
                }
                
                // 刷新书签列表
                loadBookmarks();
            } else {
                if (getContext() != null) {
                    LightToast.showShort(getContext(), "书签已存在: " + node.getName());
                }
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
                    if (treeAdapter != null) {
                        treeAdapter.addChildrenToNode(parentNode, childNodes);
                    }
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "Failed to load node children: " + throwable.getMessage());
                    parentNode.setLoading(false);
                    if (treeAdapter != null) {
                        treeAdapter.refreshNode(parentNode);
                    }
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
    
    /**
     * 加载书签列表
     */
    private void loadBookmarks() {
        if (currentWorkspace == null || workspaceManager == null) {
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Loading bookmarks for workspace: " + currentWorkspace.getId());
        
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        if (bookmarksAdapter != null) {
            bookmarksAdapter.updateBookmarks(bookmarks);
        }
        Logger.logInfo(LOG_TAG, "Loaded " + bookmarks.size() + " bookmarks");
    }
    
    // BookmarksAdapter.OnBookmarkActionListener 实现
    
    @Override
    public void onBookmarkClick(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Bookmark clicked: " + bookmark.getDisplayName());
        
        // 导航到书签路径
        navigateToDirectory(bookmark.getFullPath());
        
        // 关闭抽屉
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
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
        
        if (workspaceManager != null) {
            // 从数据库删除书签
            boolean removed = workspaceManager.removeBookmark(bookmark.getId());
            if (removed) {
                // 从适配器删除
                if (bookmarksAdapter != null) {
                    bookmarksAdapter.removeBookmark(bookmark);
                }
                
                // 更新目录树中的书签状态
                syncBookmarkStates();
                
                if (getContext() != null) {
                    LightToast.showShort(getContext(), "已删除书签: " + bookmark.getDisplayName());
                }
            } else {
                if (getContext() != null) {
                    LightToast.showShort(getContext(), "删除书签失败");
                }
            }
        }
    }
    
    /**
     * 显示书签操作对话框
     */
    private void showBookmarkOptionsDialog(DirectoryBookmark bookmark) {
        Logger.logInfo(LOG_TAG, "Showing bookmark options dialog for: " + bookmark.getDisplayName());
        
        if (getContext() == null) return;
        
        // 创建操作选项列表
        String[] options = {"跳转到该目录", "重命名书签", "删除书签"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
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
        
        if (getContext() == null) return;
        
        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setText(bookmark.getDisplayName());
        editText.setSelection(bookmark.getDisplayName().length());
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("重命名书签")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(bookmark.getDisplayName()) && workspaceManager != null && currentWorkspace != null) {
                        // 更新书签名称
                        bookmark.setDisplayName(newName);
                        workspaceManager.addBookmark(currentWorkspace.getId(), bookmark);
                        
                        // 刷新书签列表
                        loadBookmarks();
                        
                        if (getContext() != null) {
                            LightToast.showShort(getContext(), "书签已重命名");
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 同步目录树中的书签状态
     */
    private void syncBookmarkStates() {
        if (currentWorkspace == null || treeAdapter == null || workspaceManager == null) {
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
        
        // 同时更新当前路径的收藏状态指示器
        updateBookmarkIndicator(currentPath);
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
        if (getContext() != null) {
            LightToast.showShort(getContext(), "SSH配置已保存");
        }
    }
    
    @Override
    public void onSSHConfigDeleted(String configName) {
        Logger.logInfo(LOG_TAG, "SSH config deleted from dialog: " + configName);
        if (getContext() != null) {
            LightToast.showShort(getContext(), "SSH配置已删除: " + configName);
        }
    }
    
    @Override
    public void onDialogClosed() {
        Logger.logInfo(LOG_TAG, "SSH config dialog closed");
    }
}

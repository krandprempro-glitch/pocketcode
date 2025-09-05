# Batch 001: 基础布局和抽屉结构实现

## 目标
创建DrawerLayout基础结构，实现抽屉的基本开关功能，为后续开发奠定基础。

## 任务列表

### 1. 创建新的抽屉布局文件
**文件**: `app/src/main/res/layout/fragment_remote_file_browser_drawer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 抽屉式远程文件浏览器主布局 -->
<androidx.drawerlayout.widget.DrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:openDrawer="start">

    <!-- 主内容区域 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:background="@android:color/white">

        <!-- ActionBar -->
        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar">
            
            <!-- 紧凑布局：汉堡菜单 + 面包屑导航 + 连接状态 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:paddingEnd="8dp">
                
                <!-- 面包屑导航区域 -->
                <TextView
                    android:id="@+id/breadcrumb_text"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="/"
                    android:textSize="14sp"
                    android:textColor="@android:color/white"
                    android:maxLines="1"
                    android:ellipsize="start"
                    android:gravity="center_vertical"
                    android:layout_marginStart="16dp" />
                
                <!-- 连接状态指示器 -->
                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">
                    
                    <ImageView 
                        android:id="@+id/connection_status_icon"
                        android:layout_width="12dp"
                        android:layout_height="12dp"
                        android:src="@android:drawable/presence_offline"
                        android:tint="@android:color/white" />
                        
                    <TextView 
                        android:id="@+id/connection_status_text"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="4dp"
                        android:text="未连接"
                        android:textSize="12sp"
                        android:textColor="@android:color/white" />
                </LinearLayout>
            </LinearLayout>
        </androidx.appcompat.widget.Toolbar>

        <!-- 主内容区域 - 代码查看器区域 -->
        <FrameLayout
            android:id="@+id/main_content_area"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:background="@android:color/white">
            
            <!-- 欢迎页面 - 未选择文件时显示 -->
            <LinearLayout
                android:id="@+id/welcome_layout"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:orientation="vertical"
                android:gravity="center_horizontal">
                
                <ImageView
                    android:layout_width="120dp"
                    android:layout_height="120dp"
                    android:src="@drawable/ic_file_code"
                    android:alpha="0.3"
                    android:tint="#666666" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="24dp"
                    android:text="选择文件查看代码"
                    android:textSize="16sp"
                    android:textColor="#666666" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="从左侧抽屉中选择要查看的文件"
                    android:textSize="14sp"
                    android:textColor="#999999" />
            </LinearLayout>
            
            <!-- 代码查看器容器 - 后续批次实现 -->
            <FrameLayout
                android:id="@+id/code_viewer_container"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:visibility="gone" />
                
        </FrameLayout>

        <!-- 底部状态栏 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="32dp"
            android:orientation="horizontal"
            android:background="#F8F8F8"
            android:paddingHorizontal="12dp"
            android:gravity="center_vertical">
            
            <TextView
                android:id="@+id/status_text_left"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="12sp"
                android:textColor="#666666"
                android:text="就绪" />
                
            <TextView
                android:id="@+id/status_text_right"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="#666666" />
        </LinearLayout>
    </LinearLayout>

    <!-- 左侧导航抽屉 (50% 屏幕宽度) -->
    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        android:layout_width="@dimen/drawer_width"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:background="@android:color/white"
        android:fitsSystemWindows="false">

        <!-- 抽屉内容容器 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical">

            <!-- 抽屉头部 - 连接信息 -->
            <LinearLayout
                android:id="@+id/drawer_header"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:orientation="vertical"
                android:background="?attr/colorPrimary"
                android:padding="16dp"
                android:gravity="bottom">

                <TextView
                    android:id="@+id/drawer_project_name"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="远程文件浏览"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="@android:color/white" />

                <TextView
                    android:id="@+id/drawer_connection_info"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="未连接"
                    android:textSize="14sp"
                    android:textColor="#E0E0E0" />

                <TextView
                    android:id="@+id/drawer_current_path"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="/"
                    android:textSize="12sp"
                    android:textColor="#C0C0C0"
                    android:maxLines="1"
                    android:ellipsize="start" />
            </LinearLayout>

            <!-- 功能菜单区域 - 后续批次实现 -->
            <LinearLayout
                android:id="@+id/drawer_menu_container"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="#FAFAFA">
                <!-- 菜单项将在下一个批次添加 -->
            </LinearLayout>

            <!-- 文件树区域 -->
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1"
                android:background="@android:color/white">

                <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
                    android:id="@+id/drawer_swipe_refresh"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent">

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/drawer_file_list"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:scrollbars="vertical"
                        android:padding="8dp" />

                </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

                <!-- 空状态提示 -->
                <LinearLayout
                    android:id="@+id/drawer_empty_state"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:orientation="vertical"
                    android:visibility="gone">

                    <ImageView
                        android:layout_width="48dp"
                        android:layout_height="48dp"
                        android:layout_gravity="center_horizontal"
                        android:src="@drawable/ic_folder_empty"
                        android:alpha="0.3" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:text="请先连接SSH服务器"
                        android:textColor="#999999"
                        android:textSize="14sp" />
                </LinearLayout>

                <!-- 加载指示器 -->
                <ProgressBar
                    android:id="@+id/drawer_loading_progress"
                    style="?android:attr/progressBarStyle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:visibility="gone" />
            </FrameLayout>

        </LinearLayout>
    </com.google.android.material.navigation.NavigationView>

</androidx.drawerlayout.widget.DrawerLayout>
```

### 2. 创建抽屉宽度尺寸值
**文件**: `app/src/main/res/values/dimens.xml` (如果不存在则创建)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 抽屉宽度 - 50% 屏幕宽度 -->
    <dimen name="drawer_width">0dp</dimen>
</resources>
```

需要在代码中动态设置抽屉宽度为屏幕宽度的50%。

### 3. 更新Fragment布局引用
修改 `RemoteFileBrowserFragment.java` 中的 `onCreateView` 方法:

```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    // 使用新的抽屉布局
    return inflater.inflate(R.layout.fragment_remote_file_browser_drawer, container, false);
}
```

### 4. 更新Fragment的视图初始化代码
在 `RemoteFileBrowserFragment.java` 的 `initViews` 方法中添加抽屉相关组件:

```java
/**
 * 初始化UI组件
 */
private void initViews(View view) {
    // 抽屉布局组件
    drawerLayout = view.findViewById(R.id.drawer_layout);
    NavigationView navView = view.findViewById(R.id.nav_view);
    
    // 动态设置抽屉宽度为屏幕宽度的50%
    if (navView != null) {
        setDrawerWidth(navView);
    }
    
    // ActionBar组件
    toolbar = view.findViewById(R.id.toolbar);
    TextView breadcrumbText = view.findViewById(R.id.breadcrumb_text);
    connectionStatusIcon = view.findViewById(R.id.connection_status_icon);
    connectionStatusText = view.findViewById(R.id.connection_status_text);
    
    // 主内容区域
    FrameLayout mainContentArea = view.findViewById(R.id.main_content_area);
    View welcomeLayout = view.findViewById(R.id.welcome_layout);
    FrameLayout codeViewerContainer = view.findViewById(R.id.code_viewer_container);
    
    // 状态栏
    TextView statusTextLeft = view.findViewById(R.id.status_text_left);
    TextView statusTextRight = view.findViewById(R.id.status_text_right);
    
    // 抽屉头部
    TextView drawerProjectName = view.findViewById(R.id.drawer_project_name);
    TextView drawerConnectionInfo = view.findViewById(R.id.drawer_connection_info);
    TextView drawerCurrentPath = view.findViewById(R.id.drawer_current_path);
    
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
    
    // 保存新增的组件引用
    this.breadcrumbText = breadcrumbText;
    this.drawerProjectName = drawerProjectName;
    this.drawerConnectionInfo = drawerConnectionInfo;
    this.drawerCurrentPath = drawerCurrentPath;
    this.mainContentArea = mainContentArea;
    this.welcomeLayout = welcomeLayout;
    this.codeViewerContainer = codeViewerContainer;
    this.statusTextLeft = statusTextLeft;
    this.statusTextRight = statusTextRight;
    
    Logger.logInfo(LOG_TAG, "Drawer views initialized");
}

/**
 * 动态设置抽屉宽度为屏幕宽度的50%
 */
private void setDrawerWidth(NavigationView navView) {
    if (getActivity() != null) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int drawerWidth = screenWidth / 2; // 50% 屏幕宽度
        
        ViewGroup.LayoutParams params = navView.getLayoutParams();
        params.width = drawerWidth;
        navView.setLayoutParams(params);
    }
}
```

### 5. 实现抽屉开关功能
在 `RemoteFileBrowserFragment.java` 中添加抽屉控制逻辑:

```java
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
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
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
```

### 6. 添加必要的字符串资源
**文件**: `app/src/main/res/values/strings.xml`

```xml
<!-- 在现有strings.xml中添加 -->
<string name="drawer_open">打开抽屉</string>
<string name="drawer_close">关闭抽屉</string>
```

### 7. 添加必要的import语句
在 `RemoteFileBrowserFragment.java` 文件头部添加:

```java
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
```

### 8. 更新菜单项处理逻辑
修改 `onOptionsItemSelected` 方法以支持抽屉切换:

```java
@Override
public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        }
        return true;
    }
    // 其他菜单项处理将在下一个批次移至抽屉菜单
    return super.onOptionsItemSelected(item);
}
```

## 需要添加的新成员变量
在 `RemoteFileBrowserFragment.java` 中添加:

```java
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
```

## 验证要点
1. 抽屉可以正常打开/关闭
2. 抽屉宽度为屏幕宽度的50%
3. ActionBar显示汉堡菜单图标
4. 主内容区域显示欢迎页面
5. 抽屉内显示文件列表(使用现有逻辑)
6. 连接状态正确显示在ActionBar和抽屉头部

## 预估工作量
- 布局文件创建和UI组件设置: ~4万token
- Fragment代码修改和抽屉逻辑: ~3万token
- 调试和细节调整: ~1万token
- 总计: ~8万token

## 下一步
完成此批次后，抽屉基础结构就绪，可以继续进行 Batch 002 的菜单区域实现。
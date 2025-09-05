# Batch 006: 文件标签页和最终集成

## 目标
实现文件标签页功能以支持多文件切换，完善抽屉与主内容区域的数据同步，添加未选择文件时的欢迎页面，进行最终的测试和bug修复，确保所有功能正常工作。

## 任务列表

### 1. 创建文件标签页布局
**文件**: `app/src/main/res/layout/layout_file_tabs.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 文件标签页布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/tab_bar_background">

    <!-- 标签页滚动区域 -->
    <HorizontalScrollView
        android:id="@+id/tabs_scroll_view"
        android:layout_width="match_parent"
        android:layout_height="@dimen/tab_height"
        android:scrollbars="none">

        <LinearLayout
            android:id="@+id/tabs_container"
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            <!-- 标签页项目将通过代码动态添加 -->
        </LinearLayout>
    </HorizontalScrollView>

    <!-- 标签页底部分隔线 -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="@color/tab_separator" />

</LinearLayout>
```

### 2. 创建单个标签页项布局
**文件**: `app/src/main/res/layout/item_file_tab.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 单个文件标签页项布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="12dp"
    android:paddingEnd="8dp"
    android:minWidth="120dp"
    android:maxWidth="200dp"
    android:background="@drawable/tab_background_selector"
    android:clickable="true"
    android:focusable="true">

    <!-- 文件类型图标 -->
    <ImageView
        android:id="@+id/tab_file_icon"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:layout_marginEnd="6dp"
        android:src="@drawable/ic_file_unknown"
        android:contentDescription="文件图标" />

    <!-- 文件名 -->
    <TextView
        android:id="@+id/tab_file_name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="13sp"
        android:textColor="@color/tab_text_color"
        android:maxLines="1"
        android:ellipsize="end"
        android:text="文件名.java" />

    <!-- 关闭按钮 -->
    <ImageView
        android:id="@+id/tab_close_button"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:layout_marginStart="4dp"
        android:padding="2dp"
        android:src="@drawable/ic_close_small"
        android:tint="@color/tab_close_button_color"
        android:background="@drawable/close_button_background"
        android:contentDescription="关闭标签"
        android:clickable="true"
        android:focusable="true" />

</LinearLayout>
```

### 3. 创建标签页相关样式资源
**文件**: `app/src/main/res/values/dimens.xml` (添加到现有文件)

```xml
<!-- 在现有dimens.xml中添加 -->

<!-- 标签页尺寸 -->
<dimen name="tab_height">40dp</dimen>
```

**文件**: `app/src/main/res/values/colors.xml` (添加到现有文件)

```xml
<!-- 在现有colors.xml中添加 -->

<!-- 标签页颜色 -->
<color name="tab_bar_background">#F5F5F5</color>
<color name="tab_background_normal">#FFFFFF</color>
<color name="tab_background_active">#E3F2FD</color>
<color name="tab_text_color">#333333</color>
<color name="tab_text_color_active">#1976D2</color>
<color name="tab_separator">#E0E0E0</color>
<color name="tab_close_button_color">#666666</color>
```

### 4. 创建标签页背景选择器
**文件**: `app/src/main/res/drawable/tab_background_selector.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 选中状态 -->
    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/tab_background_active" />
            <stroke android:width="1dp" android:color="@color/tab_separator" />
        </shape>
    </item>
    
    <!-- 按压状态 -->
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#F0F0F0" />
            <stroke android:width="1dp" android:color="@color/tab_separator" />
        </shape>
    </item>
    
    <!-- 正常状态 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/tab_background_normal" />
            <stroke android:width="1dp" android:color="@color/tab_separator" />
        </shape>
    </item>
</selector>
```

**文件**: `app/src/main/res/drawable/close_button_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 按压状态 -->
    <item android:state_pressed="true">
        <shape android:shape="oval">
            <solid android:color="#E0E0E0" />
        </shape>
    </item>
    
    <!-- 正常状态 - 透明 -->
    <item>
        <shape android:shape="oval">
            <solid android:color="@android:color/transparent" />
        </shape>
    </item>
</selector>
```

### 5. 创建关闭图标
**文件**: `app/src/main/res/drawable/ic_close_small.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="12dp"
    android:height="12dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#666666"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/>
</vector>
```

### 6. 创建文件标签管理器
**文件**: `app/src/main/java/com/termux/app/managers/FileTabManager.java`

```java
package com.termux.app.managers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.FileTab;
import com.termux.app.utils.FileIconUtils;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件标签页管理器
 * 管理打开的文件标签页
 */
public class FileTabManager {
    private static final String LOG_TAG = "FileTabManager";
    private static final int MAX_TABS = 10; // 最大标签页数量
    
    private Context context;
    private LinearLayout tabsContainer;
    private HorizontalScrollView tabsScrollView;
    private List<FileTab> openTabs;
    private FileTab activeTab;
    private OnTabActionListener listener;
    
    public interface OnTabActionListener {
        void onTabSelected(FileTab tab);
        void onTabClosed(FileTab tab);
        void onAllTabsClosed();
    }
    
    public FileTabManager(Context context, LinearLayout tabsContainer, HorizontalScrollView tabsScrollView) {
        this.context = context;
        this.tabsContainer = tabsContainer;
        this.tabsScrollView = tabsScrollView;
        this.openTabs = new ArrayList<>();
    }
    
    public void setOnTabActionListener(OnTabActionListener listener) {
        this.listener = listener;
    }
    
    /**
     * 打开新的文件标签页
     */
    public FileTab openFile(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "Opening file tab: " + file.getName());
        
        // 检查文件是否已经打开
        FileTab existingTab = findTabByPath(file.getPath());
        if (existingTab != null) {
            selectTab(existingTab);
            return existingTab;
        }
        
        // 检查标签页数量限制
        if (openTabs.size() >= MAX_TABS) {
            // 关闭最旧的标签页
            closeTab(openTabs.get(0));
        }
        
        // 创建新标签页
        FileTab newTab = new FileTab(file);
        openTabs.add(newTab);
        
        // 创建标签页UI
        View tabView = createTabView(newTab);
        newTab.setTabView(tabView);
        tabsContainer.addView(tabView);
        
        // 选中新标签页
        selectTab(newTab);
        
        // 滚动到新标签页
        scrollToTab(newTab);
        
        Logger.logInfo(LOG_TAG, "File tab opened: " + file.getName() + ", total tabs: " + openTabs.size());
        return newTab;
    }
    
    /**
     * 创建标签页视图
     */
    private View createTabView(FileTab tab) {
        View tabView = LayoutInflater.from(context).inflate(R.layout.item_file_tab, tabsContainer, false);
        
        ImageView fileIcon = tabView.findViewById(R.id.tab_file_icon);
        TextView fileName = tabView.findViewById(R.id.tab_file_name);
        ImageView closeButton = tabView.findViewById(R.id.tab_close_button);
        
        // 设置文件图标
        int iconRes = FileIconUtils.getFileIcon(tab.getFile());
        fileIcon.setImageResource(iconRes);
        
        // 设置文件名
        fileName.setText(tab.getFile().getName());
        
        // 设置点击事件
        tabView.setOnClickListener(v -> selectTab(tab));
        
        // 设置关闭按钮事件
        closeButton.setOnClickListener(v -> closeTab(tab));
        
        return tabView;
    }
    
    /**
     * 选中标签页
     */
    public void selectTab(FileTab tab) {
        if (activeTab == tab) return;
        
        Logger.logInfo(LOG_TAG, "Selecting tab: " + tab.getFile().getName());
        
        // 取消之前选中的标签页
        if (activeTab != null && activeTab.getTabView() != null) {
            activeTab.getTabView().setSelected(false);
            updateTabTextColor(activeTab, false);
        }
        
        // 选中新标签页
        activeTab = tab;
        if (tab.getTabView() != null) {
            tab.getTabView().setSelected(true);
            updateTabTextColor(tab, true);
        }
        
        // 通知监听器
        if (listener != null) {
            listener.onTabSelected(tab);
        }
        
        // 滚动到选中的标签页
        scrollToTab(tab);
    }
    
    /**
     * 更新标签页文字颜色
     */
    private void updateTabTextColor(FileTab tab, boolean active) {
        if (tab.getTabView() != null) {
            TextView fileName = tab.getTabView().findViewById(R.id.tab_file_name);
            if (fileName != null) {
                int colorRes = active ? R.color.tab_text_color_active : R.color.tab_text_color;
                fileName.setTextColor(context.getResources().getColor(colorRes, null));
            }
        }
    }
    
    /**
     * 关闭标签页
     */
    public void closeTab(FileTab tab) {
        Logger.logInfo(LOG_TAG, "Closing tab: " + tab.getFile().getName());
        
        int tabIndex = openTabs.indexOf(tab);
        if (tabIndex == -1) return;
        
        // 从容器中移除视图
        if (tab.getTabView() != null) {
            tabsContainer.removeView(tab.getTabView());
        }
        
        // 从列表中移除
        openTabs.remove(tab);
        
        // 如果关闭的是当前活动标签页，需要选择其他标签页
        if (activeTab == tab) {
            activeTab = null;
            
            if (!openTabs.isEmpty()) {
                // 选择相邻的标签页
                int newIndex = Math.min(tabIndex, openTabs.size() - 1);
                selectTab(openTabs.get(newIndex));
            } else {
                // 所有标签页都关闭了
                if (listener != null) {
                    listener.onAllTabsClosed();
                }
            }
        }
        
        // 通知监听器
        if (listener != null) {
            listener.onTabClosed(tab);
        }
        
        Logger.logInfo(LOG_TAG, "Tab closed: " + tab.getFile().getName() + ", remaining tabs: " + openTabs.size());
    }
    
    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        Logger.logInfo(LOG_TAG, "Closing all tabs");
        
        // 从后往前关闭，避免索引问题
        for (int i = openTabs.size() - 1; i >= 0; i--) {
            FileTab tab = openTabs.get(i);
            if (tab.getTabView() != null) {
                tabsContainer.removeView(tab.getTabView());
            }
        }
        
        openTabs.clear();
        activeTab = null;
        
        if (listener != null) {
            listener.onAllTabsClosed();
        }
    }
    
    /**
     * 滚动到指定标签页
     */
    private void scrollToTab(FileTab tab) {
        if (tab.getTabView() != null && tabsScrollView != null) {
            tabsScrollView.post(() -> {
                int tabLeft = tab.getTabView().getLeft();
                int tabWidth = tab.getTabView().getWidth();
                int scrollX = tabLeft - (tabsScrollView.getWidth() - tabWidth) / 2;
                tabsScrollView.smoothScrollTo(scrollX, 0);
            });
        }
    }
    
    /**
     * 根据文件路径查找标签页
     */
    private FileTab findTabByPath(String filePath) {
        for (FileTab tab : openTabs) {
            if (filePath.equals(tab.getFile().getPath())) {
                return tab;
            }
        }
        return null;
    }
    
    /**
     * 获取当前活动标签页
     */
    public FileTab getActiveTab() {
        return activeTab;
    }
    
    /**
     * 获取所有打开的标签页
     */
    public List<FileTab> getAllTabs() {
        return new ArrayList<>(openTabs);
    }
    
    /**
     * 获取标签页数量
     */
    public int getTabCount() {
        return openTabs.size();
    }
    
    /**
     * 检查是否有打开的标签页
     */
    public boolean hasTabs() {
        return !openTabs.isEmpty();
    }
    
    /**
     * 检查指定文件是否已打开
     */
    public boolean isFileOpen(String filePath) {
        return findTabByPath(filePath) != null;
    }
}
```

### 7. 创建文件标签页数据模型
**文件**: `app/src/main/java/com/termux/app/models/FileTab.java`

```java
package com.termux.app.models;

import android.view.View;

/**
 * 文件标签页数据模型
 */
public class FileTab {
    private RemoteFileItem file;
    private View tabView;
    private String content;
    private boolean isModified = false;
    private long lastAccessTime;
    private int scrollPosition = 0;
    
    public FileTab(RemoteFileItem file) {
        this.file = file;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public RemoteFileItem getFile() { return file; }
    public void setFile(RemoteFileItem file) { this.file = file; }
    
    public View getTabView() { return tabView; }
    public void setTabView(View tabView) { this.tabView = tabView; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public boolean isModified() { return isModified; }
    public void setModified(boolean modified) { isModified = modified; }
    
    public long getLastAccessTime() { return lastAccessTime; }
    public void updateLastAccessTime() { this.lastAccessTime = System.currentTimeMillis(); }
    
    public int getScrollPosition() { return scrollPosition; }
    public void setScrollPosition(int scrollPosition) { this.scrollPosition = scrollPosition; }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        String name = file.getName();
        return isModified ? name + "*" : name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileTab fileTab = (FileTab) obj;
        return file.getPath().equals(fileTab.file.getPath());
    }
    
    @Override
    public int hashCode() {
        return file.getPath().hashCode();
    }
    
    @Override
    public String toString() {
        return "FileTab{" +
                "fileName='" + file.getName() + '\'' +
                ", path='" + file.getPath() + '\'' +
                ", isModified=" + isModified +
                '}';
    }
}
```

### 8. 更新主布局以支持标签页
修改 `fragment_remote_file_browser_drawer.xml` 中的主内容区域:

```xml
<!-- 更新主内容区域部分 -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="vertical">

    <!-- 文件标签页区域 -->
    <include
        android:id="@+id/file_tabs_layout"
        layout="@layout/layout_file_tabs"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="gone" />

    <!-- 代码查看器区域 -->
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
                android:textSize="18sp"
                android:textColor="#666666" />
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="从左侧抽屉中选择要查看的文件"
                android:textSize="14sp"
                android:textColor="#999999" />
                
            <TextView
                android:id="@+id/welcome_stats_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text=""
                android:textSize="12sp"
                android:textColor="#BBBBBB"
                android:visibility="gone" />
        </LinearLayout>
        
        <!-- 代码查看器容器 -->
        <FrameLayout
            android:id="@+id/code_viewer_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone" />
            
    </FrameLayout>

</LinearLayout>
```

### 9. 更新Fragment以集成标签页功能
在 `RemoteFileBrowserFragment.java` 中集成标签页管理:

```java
// 添加新的成员变量
private FileTabManager fileTabManager;
private View fileTabsLayout;
private LinearLayout tabsContainer;
private HorizontalScrollView tabsScrollView;

/**
 * 初始化UI组件 (更新现有方法)
 */
private void initViews(View view) {
    // ... 原有代码 ...
    
    // 初始化标签页组件
    setupFileTabs(view);
    
    Logger.logInfo(LOG_TAG, "File tabs initialized");
}

/**
 * 设置文件标签页
 */
private void setupFileTabs(View view) {
    fileTabsLayout = view.findViewById(R.id.file_tabs_layout);
    tabsContainer = view.findViewById(R.id.tabs_container);
    tabsScrollView = view.findViewById(R.id.tabs_scroll_view);
    
    if (tabsContainer != null && tabsScrollView != null) {
        fileTabManager = new FileTabManager(requireContext(), tabsContainer, tabsScrollView);
        fileTabManager.setOnTabActionListener(new FileTabManager.OnTabActionListener() {
            @Override
            public void onTabSelected(FileTab tab) {
                handleTabSelected(tab);
            }
            
            @Override
            public void onTabClosed(FileTab tab) {
                handleTabClosed(tab);
            }
            
            @Override
            public void onAllTabsClosed() {
                handleAllTabsClosed();
            }
        });
    }
}

/**
 * 处理标签页选中
 */
private void handleTabSelected(FileTab tab) {
    Logger.logInfo(LOG_TAG, "Tab selected: " + tab.getFile().getName());
    
    // 更新最后访问时间
    tab.updateLastAccessTime();
    
    // 如果已经有缓存的内容，直接显示
    if (tab.getContent() != null) {
        displayCachedFileContent(tab);
    } else {
        // 重新加载文件内容
        loadFileContentForTab(tab);
    }
    
    // 更新UI状态
    updateMainContentStatus(tab.getFile());
    updateSelectedFilePath(tab.getFile().getPath());
}

/**
 * 处理标签页关闭
 */
private void handleTabClosed(FileTab tab) {
    Logger.logInfo(LOG_TAG, "Tab closed: " + tab.getFile().getName());
    
    // 如果关闭的是当前选中的文件，清除选中状态
    if (selectedFilePath != null && selectedFilePath.equals(tab.getFile().getPath())) {
        selectedFilePath = null;
    }
}

/**
 * 处理所有标签页关闭
 */
private void handleAllTabsClosed() {
    Logger.logInfo(LOG_TAG, "All tabs closed");
    
    // 隐藏标签页栏
    if (fileTabsLayout != null) {
        fileTabsLayout.setVisibility(View.GONE);
    }
    
    // 显示欢迎页面
    showWelcomeScreen();
    
    // 清除选中文件
    selectedFilePath = null;
    
    // 重置状态栏
    resetStatusBar();
}

/**
 * 显示欢迎页面
 */
private void showWelcomeScreen() {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            if (welcomeLayout != null && codeViewerContainer != null) {
                welcomeLayout.setVisibility(View.VISIBLE);
                codeViewerContainer.setVisibility(View.GONE);
            }
            
            // 更新欢迎页面统计信息
            updateWelcomeStats();
        });
    }
}

/**
 * 更新欢迎页面统计信息
 */
private void updateWelcomeStats() {
    TextView welcomeStatsText = findViewById(R.id.welcome_stats_text);
    if (welcomeStatsText != null && currentWorkspace != null && workspaceManager != null) {
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        String statsText = "当前项目: " + currentWorkspace.getDisplayName() + 
                          "\n收藏夹: " + bookmarks.size() + " 项";
        
        welcomeStatsText.setText(statsText);
        welcomeStatsText.setVisibility(View.VISIBLE);
    }
}

/**
 * 重置状态栏
 */
private void resetStatusBar() {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            if (statusTextLeft != null) {
                statusTextLeft.setText("就绪");
            }
            if (statusTextRight != null) {
                statusTextRight.setText("");
            }
            if (breadcrumbText != null) {
                breadcrumbText.setText(optimizePathDisplay(currentPath));
            }
        });
    }
}

/**
 * 更新在主内容区打开文件的方法
 */
private void openFileInMainContent(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "Opening file in main content: " + file.getName());
    
    // 使用标签页管理器打开文件
    if (fileTabManager != null) {
        FileTab tab = fileTabManager.openFile(file);
        
        // 显示标签页栏
        if (fileTabsLayout != null) {
            fileTabsLayout.setVisibility(View.VISIBLE);
        }
        
        // 加载文件内容
        loadFileContentForTab(tab);
    } else {
        // 降级到原有方式
        loadCodeFileContent(file);
    }
    
    // 更新选中文件路径
    updateSelectedFilePath(file.getPath());
    
    // 隐藏欢迎页面，显示内容区域
    if (welcomeLayout != null && codeViewerContainer != null) {
        welcomeLayout.setVisibility(View.GONE);
        codeViewerContainer.setVisibility(View.VISIBLE);
    }
}

/**
 * 为标签页加载文件内容
 */
private void loadFileContentForTab(FileTab tab) {
    Logger.logInfo(LOG_TAG, "Loading content for tab: " + tab.getFile().getName());
    
    RemoteFileItem file = tab.getFile();
    
    if (!FileContentLoader.canPreviewFile(file)) {
        showFilePreviewNotSupported(file);
        return;
    }
    
    // 显示加载中状态
    updateMainContentStatus(file, "加载中...");
    
    if (fileContentLoader == null) {
        showErrorMessage("文件加载器未初始化");
        return;
    }
    
    // 清除现有内容
    if (codeViewer != null) {
        codeViewer.clear();
    }
    
    // 异步加载文件内容
    Disposable disposable = fileContentLoader.loadFileContent(file)
        .subscribe(
            content -> {
                Logger.logInfo(LOG_TAG, "File content loaded for tab: " + file.getName());
                
                // 缓存内容到标签页
                tab.setContent(content);
                
                // 如果这是当前活动标签页，显示内容
                if (fileTabManager.getActiveTab() == tab) {
                    displayFileContent(tab);
                }
            },
            throwable -> {
                Logger.logError(LOG_TAG, "Failed to load content for tab: " + throwable.getMessage());
                
                String errorMessage = "加载文件失败: " + throwable.getMessage();
                
                // 如果这是当前活动标签页，显示错误
                if (fileTabManager.getActiveTab() == tab) {
                    showSimpleTextPreview(file, errorMessage);
                    updateMainContentStatus(file, "加载失败");
                }
                
                // 显示友好的错误对话框
                NetworkErrorHandler.showErrorDialog(requireContext(), throwable, () -> {
                    // 重试加载
                    loadFileContentForTab(tab);
                });
            }
        );
    
    compositeDisposable.add(disposable);
}

/**
 * 显示缓存的文件内容
 */
private void displayCachedFileContent(FileTab tab) {
    Logger.logInfo(LOG_TAG, "Displaying cached content for tab: " + tab.getFile().getName());
    displayFileContent(tab);
}

/**
 * 显示文件内容
 */
private void displayFileContent(FileTab tab) {
    if (codeViewer != null && tab.getContent() != null) {
        codeViewer.loadCode(tab.getContent(), tab.getFile().getName());
        
        // 恢复滚动位置
        if (tab.getScrollPosition() > 0) {
            codeViewer.post(() -> {
                codeViewer.scrollToLine(tab.getScrollPosition());
            });
        }
    }
}

/**
 * 更新选中文件路径
 */
private void updateSelectedFilePath(String filePath) {
    this.selectedFilePath = filePath;
    
    // 同步抽屉中的选中状态（如果需要的话）
    // 这里可以添加抽屉文件列表的选中状态同步逻辑
}

/**
 * 在Fragment销毁时清理标签页
 */
@Override
public void onDestroy() {
    // 保存当前滚动位置
    if (fileTabManager != null && codeViewer != null) {
        FileTab activeTab = fileTabManager.getActiveTab();
        if (activeTab != null) {
            // TODO: 获取并保存当前滚动位置
            // activeTab.setScrollPosition(codeViewer.getCurrentLine());
        }
    }
    
    // 清理标签页
    if (fileTabManager != null) {
        fileTabManager.closeAllTabs();
    }
    
    super.onDestroy();
}
```

### 10. 添加键盘快捷键支持 (可选增强功能)
在 `RemoteFileBrowserFragment.java` 中添加快捷键支持:

```java
/**
 * 处理按键事件 (如果需要快捷键支持)
 */
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (fileTabManager != null) {
        // Ctrl+W 关闭当前标签页
        if (event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_W) {
            FileTab activeTab = fileTabManager.getActiveTab();
            if (activeTab != null) {
                fileTabManager.closeTab(activeTab);
                return true;
            }
        }
        
        // Ctrl+Tab 切换标签页
        if (event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_TAB) {
            List<FileTab> tabs = fileTabManager.getAllTabs();
            if (tabs.size() > 1) {
                FileTab activeTab = fileTabManager.getActiveTab();
                int currentIndex = tabs.indexOf(activeTab);
                int nextIndex = (currentIndex + 1) % tabs.size();
                fileTabManager.selectTab(tabs.get(nextIndex));
                return true;
            }
        }
    }
    
    return false;
}
```

## 验证要点
1. 标签页正确显示和切换
2. 文件内容正确缓存和恢复
3. 标签页关闭功能正常
4. 多文件之间切换流畅
5. 标签页滚动和布局正确
6. 欢迎页面在无标签页时正确显示
7. 状态栏和面包屑导航正确更新
8. 内存使用合理，无内存泄漏
9. 所有原有功能保持完整
10. 抽屉与主内容区域数据同步正确

## 预估工作量
- 标签页UI组件和布局: ~3万token
- FileTabManager和数据模型: ~3万token
- Fragment集成和事件处理: ~2万token
- 总计: ~8万token

## 完成标志
完成此批次后，整个抽屉式远程文件浏览器功能完整，包括：
- ✅ 抽屉式布局(50%屏幕宽度)
- ✅ 可折叠的功能菜单
- ✅ 文件树导航和交互
- ✅ VSCode Light风格代码查看器
- ✅ 语法高亮支持
- ✅ 多文件标签页切换
- ✅ 完整的用户体验和错误处理

所有6个批次执行完毕后，用户将获得一个功能完整的、现代化的远程代码浏览器界面。
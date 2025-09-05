# Batch 003: 文件树集成和交互

## 目标
将现有的文件浏览功能完整集成到抽屉中，优化文件树显示效果，实现文件树与主内容区域的交互逻辑，确保所有现有功能完整保持。

## 任务列表

### 1. 优化抽屉中的文件项布局
**文件**: `app/src/main/res/layout/item_drawer_file.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 抽屉中的文件项布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="12dp"
    android:paddingEnd="8dp"
    android:background="?android:attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true">

    <!-- 缩进指示器 (用于目录层级) -->
    <View
        android:id="@+id/indent_view"
        android:layout_width="0dp"
        android:layout_height="1dp"
        android:visibility="gone" />

    <!-- 文件类型图标 -->
    <ImageView
        android:id="@+id/file_icon"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginEnd="12dp"
        android:src="@drawable/ic_file_unknown"
        android:contentDescription="文件图标" />

    <!-- 文件名 -->
    <TextView
        android:id="@+id/file_name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="14sp"
        android:textColor="#333333"
        android:maxLines="1"
        android:ellipsize="end"
        android:text="文件名" />

    <!-- 文件大小/类型信息 -->
    <TextView
        android:id="@+id/file_info"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:textSize="11sp"
        android:textColor="#888888"
        android:visibility="gone" />

    <!-- 更多操作按钮 -->
    <ImageView
        android:id="@+id/file_more_button"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:layout_marginStart="4dp"
        android:padding="4dp"
        android:src="@drawable/ic_more_vert"
        android:tint="#999999"
        android:background="?android:attr/selectableItemBackgroundBorderless"
        android:contentDescription="更多操作"
        android:visibility="gone" />

</LinearLayout>
```

### 2. 创建适用于抽屉的文件适配器
**文件**: `app/src/main/java/com/termux/app/adapters/DrawerFileAdapter.java`

```java
package com.termux.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.utils.FileIconUtils;
import com.termux.app.utils.FileUtils;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽屉文件适配器 - 专门用于抽屉中的文件树显示
 * 与原有RemoteFileBrowserAdapter类似，但针对抽屉环境优化
 */
public class DrawerFileAdapter extends RecyclerView.Adapter<DrawerFileAdapter.FileViewHolder> {
    private static final String LOG_TAG = "DrawerFileAdapter";
    
    private Context context;
    private List<RemoteFileItem> files;
    private String currentPath;
    private OnFileActionListener listener;
    private boolean showFileInfo;
    
    public interface OnFileActionListener {
        void onFileClick(RemoteFileItem file);
        void onFileMoreClick(RemoteFileItem file, View anchorView);
        void onDirectoryEnter(RemoteFileItem directory);
    }
    
    public DrawerFileAdapter(Context context, OnFileActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.files = new ArrayList<>();
        this.showFileInfo = false; // 抽屉中默认不显示文件信息，节省空间
    }
    
    public void updateFiles(List<RemoteFileItem> newFiles, String path) {
        this.files = newFiles != null ? new ArrayList<>(newFiles) : new ArrayList<>();
        this.currentPath = path;
        notifyDataSetChanged();
        Logger.logInfo(LOG_TAG, "Files updated for path: " + path + ", count: " + files.size());
    }
    
    public void updateFiles(List<RemoteFileItem> newFiles) {
        updateFiles(newFiles, currentPath);
    }
    
    public void setShowFileInfo(boolean show) {
        if (this.showFileInfo != show) {
            this.showFileInfo = show;
            notifyDataSetChanged();
        }
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_file, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        RemoteFileItem file = files.get(position);
        bindFileViewHolder(holder, file, position);
    }
    
    private void bindFileViewHolder(FileViewHolder holder, RemoteFileItem file, int position) {
        // 设置文件名
        holder.fileName.setText(file.getName());
        
        // 设置文件图标
        int iconRes = FileIconUtils.getFileIcon(file);
        holder.fileIcon.setImageResource(iconRes);
        
        // 根据文件类型设置不同的文字颜色
        if (file.isDirectory()) {
            holder.fileName.setTextColor(context.getResources().getColor(R.color.directory_text_color, null));
        } else {
            holder.fileName.setTextColor(context.getResources().getColor(R.color.file_text_color, null));
        }
        
        // 设置文件信息 (大小、修改时间等)
        if (showFileInfo && !file.isDirectory()) {
            holder.fileInfo.setVisibility(View.VISIBLE);
            holder.fileInfo.setText(formatFileInfo(file));
        } else {
            holder.fileInfo.setVisibility(View.GONE);
        }
        
        // 设置更多操作按钮
        setupMoreButton(holder, file);
        
        // 设置点击事件
        setupClickListeners(holder, file);
    }
    
    /**
     * 格式化文件信息显示
     */
    private String formatFileInfo(RemoteFileItem file) {
        if (file.isDirectory()) {
            return "目录";
        }
        
        String sizeStr = FileUtils.formatFileSize(file.getSize());
        return sizeStr;
    }
    
    /**
     * 设置更多操作按钮
     */
    private void setupMoreButton(FileViewHolder holder, RemoteFileItem file) {
        // 只对文件显示更多按钮，目录通过长按操作
        if (!file.isDirectory()) {
            holder.fileMoreButton.setVisibility(View.VISIBLE);
            holder.fileMoreButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFileMoreClick(file, v);
                }
            });
        } else {
            holder.fileMoreButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners(FileViewHolder holder, RemoteFileItem file) {
        // 普通点击
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (file.isDirectory()) {
                    listener.onDirectoryEnter(file);
                } else {
                    listener.onFileClick(file);
                }
            }
        });
        
        // 长按操作 (主要用于目录的收藏等操作)
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && file.isDirectory()) {
                listener.onFileMoreClick(file, v);
                return true;
            }
            return false;
        });
    }
    
    @Override
    public int getItemCount() {
        return files.size();
    }
    
    public List<RemoteFileItem> getFiles() {
        return new ArrayList<>(files);
    }
    
    public String getCurrentPath() {
        return currentPath;
    }
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        View indentView;
        ImageView fileIcon;
        TextView fileName;
        TextView fileInfo;
        ImageView fileMoreButton;
        
        FileViewHolder(View itemView) {
            super(itemView);
            indentView = itemView.findViewById(R.id.indent_view);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
            fileMoreButton = itemView.findViewById(R.id.file_more_button);
        }
    }
}
```

### 3. 创建文件图标工具类
**文件**: `app/src/main/java/com/termux/app/utils/FileIconUtils.java`

```java
package com.termux.app.utils;

import com.termux.R;
import com.termux.app.models.RemoteFileItem;

/**
 * 文件图标工具类
 * 根据文件类型返回相应的图标资源
 */
public class FileIconUtils {
    
    public static int getFileIcon(RemoteFileItem file) {
        if (file.isDirectory()) {
            return R.drawable.ic_folder_blue;
        }
        
        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);
        
        // 代码文件
        if (isCodeFile(extension)) {
            return R.drawable.ic_file_code;
        }
        
        // 文档文件
        if (isDocumentFile(extension)) {
            return R.drawable.ic_file_document;
        }
        
        // 图片文件
        if (isImageFile(extension)) {
            return R.drawable.ic_file_image;
        }
        
        // 压缩文件
        if (isArchiveFile(extension)) {
            return R.drawable.ic_file_archive;
        }
        
        // 文本文件
        if (isTextFile(extension)) {
            return R.drawable.ic_file_text;
        }
        
        // 默认文件图标
        return R.drawable.ic_file_unknown;
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    private static boolean isCodeFile(String extension) {
        return extension.equals("java") || extension.equals("kt") || extension.equals("js") ||
               extension.equals("ts") || extension.equals("py") || extension.equals("cpp") ||
               extension.equals("c") || extension.equals("h") || extension.equals("css") ||
               extension.equals("html") || extension.equals("xml") || extension.equals("json") ||
               extension.equals("gradle") || extension.equals("properties") || extension.equals("yml") ||
               extension.equals("yaml") || extension.equals("sh") || extension.equals("sql") ||
               extension.equals("php") || extension.equals("rb") || extension.equals("go") ||
               extension.equals("rs") || extension.equals("swift");
    }
    
    private static boolean isDocumentFile(String extension) {
        return extension.equals("pdf") || extension.equals("doc") || extension.equals("docx") ||
               extension.equals("xls") || extension.equals("xlsx") || extension.equals("ppt") ||
               extension.equals("pptx") || extension.equals("rtf");
    }
    
    private static boolean isImageFile(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") ||
               extension.equals("gif") || extension.equals("bmp") || extension.equals("svg") ||
               extension.equals("ico") || extension.equals("webp");
    }
    
    private static boolean isArchiveFile(String extension) {
        return extension.equals("zip") || extension.equals("rar") || extension.equals("7z") ||
               extension.equals("tar") || extension.equals("gz") || extension.equals("bz2") ||
               extension.equals("xz") || extension.equals("jar") || extension.equals("apk");
    }
    
    private static boolean isTextFile(String extension) {
        return extension.equals("txt") || extension.equals("log") || extension.equals("md") ||
               extension.equals("readme") || extension.equals("cfg") || extension.equals("conf") ||
               extension.equals("ini") || extension.equals("env");
    }
}
```

### 4. 创建文件工具类
**文件**: `app/src/main/java/com/termux/app/utils/FileUtils.java`

```java
package com.termux.app.utils;

/**
 * 文件工具类
 */
public class FileUtils {
    
    /**
     * 格式化文件大小显示
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 检查是否为代码文件
     */
    public static boolean isCodeFile(String fileName) {
        if (fileName == null) return false;
        
        String extension = getFileExtension(fileName.toLowerCase());
        return isCodeExtension(extension);
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    private static boolean isCodeExtension(String extension) {
        return extension.equals("java") || extension.equals("kt") || extension.equals("js") ||
               extension.equals("ts") || extension.equals("py") || extension.equals("cpp") ||
               extension.equals("c") || extension.equals("h") || extension.equals("css") ||
               extension.equals("html") || extension.equals("xml") || extension.equals("json") ||
               extension.equals("gradle") || extension.equals("properties") || extension.equals("yml") ||
               extension.equals("yaml") || extension.equals("sh") || extension.equals("sql") ||
               extension.equals("php") || extension.equals("rb") || extension.equals("go") ||
               extension.equals("rs") || extension.equals("swift") || extension.equals("md") ||
               extension.equals("txt") || extension.equals("log");
    }
}
```

### 5. 添加颜色资源
**文件**: `app/src/main/res/values/colors.xml` (如不存在则创建)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 文件树颜色 -->
    <color name="directory_text_color">#2196F3</color>
    <color name="file_text_color">#333333</color>
    <color name="file_selected_background">#E3F2FD</color>
</resources>
```

### 6. 更新Fragment集成新的文件适配器
在 `RemoteFileBrowserFragment.java` 中更新文件树相关代码:

```java
// 添加新的成员变量
private DrawerFileAdapter drawerFileAdapter;
private String selectedFilePath; // 当前在主内容区打开的文件路径

/**
 * 设置RecyclerView (更新现有方法)
 */
private void setupRecyclerViews() {
    // 原有的主内容区文件列表 (现在用于抽屉)
    if (fileContentRecyclerView != null) {
        fileContentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 使用新的抽屉文件适配器
        drawerFileAdapter = new DrawerFileAdapter(requireContext(), new DrawerFileAdapter.OnFileActionListener() {
            @Override
            public void onFileClick(RemoteFileItem file) {
                handleDrawerFileClick(file);
            }
            
            @Override
            public void onFileMoreClick(RemoteFileItem file, View anchorView) {
                handleDrawerFileMoreClick(file, anchorView);
            }
            
            @Override
            public void onDirectoryEnter(RemoteFileItem directory) {
                handleDrawerDirectoryEnter(directory);
            }
        });
        
        fileContentRecyclerView.setAdapter(drawerFileAdapter);
        
        // 保持兼容性，将原有适配器引用指向新适配器
        fileAdapter = new RemoteFileBrowserAdapter.AdapterWrapper(drawerFileAdapter);
        
    } else if (rvFiles != null) {
        // 兼容性处理
        setupRecyclerView();
    }
    
    Logger.logInfo(LOG_TAG, "RecyclerViews configured with drawer file adapter");
}

/**
 * 处理抽屉文件点击
 */
private void handleDrawerFileClick(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "Drawer file clicked: " + file.getName());
    
    if (file.isDirectory()) {
        // 目录直接进入，这与原有逻辑一致
        navigateToDirectory(file.getPath());
    } else {
        // 文件在主内容区打开
        openFileInMainContent(file);
        
        // 关闭抽屉，让用户查看文件内容
        closeDrawer();
    }
}

/**
 * 处理抽屉文件更多操作点击
 */
private void handleDrawerFileMoreClick(RemoteFileItem file, View anchorView) {
    Logger.logInfo(LOG_TAG, "Drawer file more clicked: " + file.getName());
    
    // 复用现有的文件操作对话框逻辑
    showFileOperationsDialog(file);
}

/**
 * 处理抽屉目录进入
 */
private void handleDrawerDirectoryEnter(RemoteFileItem directory) {
    Logger.logInfo(LOG_TAG, "Drawer directory enter: " + directory.getName());
    
    // 导航到目录，这会更新抽屉内容和路径显示
    navigateToDirectory(directory.getPath());
}

/**
 * 在主内容区打开文件
 */
private void openFileInMainContent(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "Opening file in main content: " + file.getName());
    
    selectedFilePath = file.getPath();
    
    // 检查是否为代码文件
    if (FileUtils.isCodeFile(file.getName())) {
        // 加载并显示代码内容 (将在下一个批次实现)
        loadCodeFileContent(file);
    } else {
        // 显示文件信息或提示不支持预览
        showFilePreviewNotSupported(file);
    }
    
    // 更新状态栏信息
    updateMainContentStatus(file);
    
    // 隐藏欢迎页面，显示内容区域
    if (welcomeLayout != null && codeViewerContainer != null) {
        welcomeLayout.setVisibility(View.GONE);
        codeViewerContainer.setVisibility(View.VISIBLE);
    }
}

/**
 * 加载代码文件内容 (占位方法，下个批次实现)
 */
private void loadCodeFileContent(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "Loading code file content: " + file.getName());
    
    // 显示加载中状态
    updateMainContentStatus(file, "加载中...");
    
    // TODO: 在Batch 004中实现具体的代码加载和显示逻辑
    // 现在先显示一个简单的占位文本
    showSimpleTextPreview(file, "代码文件: " + file.getName() + "\n\n文件内容加载将在下一个批次实现...");
}

/**
 * 显示文件不支持预览
 */
private void showFilePreviewNotSupported(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "File preview not supported: " + file.getName());
    
    String message = "文件类型: " + FileUtils.getFileExtension(file.getName()) + "\n" +
                    "文件大小: " + FileUtils.formatFileSize(file.getSize()) + "\n\n" +
                    "此文件类型暂不支持预览，请使用下载功能查看内容。";
    
    showSimpleTextPreview(file, message);
}

/**
 * 显示简单文本预览 (临时方法)
 */
private void showSimpleTextPreview(RemoteFileItem file, String content) {
    if (codeViewerContainer != null) {
        // 清除现有内容
        codeViewerContainer.removeAllViews();
        
        // 创建简单的文本视图
        TextView textView = new TextView(requireContext());
        textView.setText(content);
        textView.setTextSize(14);
        textView.setTextColor(requireContext().getResources().getColor(R.color.file_text_color, null));
        textView.setPadding(32, 32, 32, 32);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        
        codeViewerContainer.addView(textView);
    }
}

/**
 * 更新主内容区状态
 */
private void updateMainContentStatus(RemoteFileItem file) {
    updateMainContentStatus(file, null);
}

private void updateMainContentStatus(RemoteFileItem file, String status) {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            // 更新底部状态栏
            if (statusTextLeft != null) {
                String leftText = status != null ? status : "已打开: " + file.getName();
                statusTextLeft.setText(leftText);
            }
            
            if (statusTextRight != null && status == null) {
                String rightText = FileUtils.formatFileSize(file.getSize());
                statusTextRight.setText(rightText);
            }
            
            // 更新面包屑导航
            if (breadcrumbText != null) {
                String breadcrumb = currentPath + " → " + file.getName();
                breadcrumbText.setText(breadcrumb);
            }
        });
    }
}

/**
 * 重写现有的导航方法以支持抽屉更新
 */
private void navigateToDirectory(String dirPath) {
    Logger.logInfo(LOG_TAG, "Navigating to directory: " + dirPath);
    
    // 使用统一的路径变化处理
    onPathChanged(dirPath);
    
    // 加载目录内容到抽屉
    loadDirectoryContentToDrawer(dirPath);
    
    // 如果主内容区有打开的文件，清除它并显示欢迎页面
    closeMainContentFile();
}

/**
 * 加载目录内容到抽屉
 */
private void loadDirectoryContentToDrawer(String path) {
    Logger.logInfo(LOG_TAG, "Loading directory content to drawer: " + path);
    showLoading(true);
    
    // 使用SFTP管理器获取目录列表
    Disposable disposable = sftpManager.listFiles(path)
        .subscribe(
            files -> {
                Logger.logInfo(LOG_TAG, "Loaded " + files.size() + " files to drawer from " + path);
                
                // 更新抽屉文件适配器
                if (drawerFileAdapter != null) {
                    drawerFileAdapter.updateFiles(files, path);
                }
                
                // 兼容性：也更新原有适配器引用
                if (fileAdapter != null) {
                    fileAdapter.updateFiles(files, path);
                }
                
                updateFileCount(files.size());
                showEmptyState(files.isEmpty());
                showLoading(false);
                
                // 停止刷新动画
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                // 更新抽屉路径显示
                updateDrawerPathInfo(path, files.size());
            },
            throwable -> {
                Logger.logError(LOG_TAG, "Failed to load directory content to drawer: " + throwable.getMessage());
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
                    loadDirectoryContentToDrawer(path);
                });
            }
        );
    
    compositeDisposable.add(disposable);
}

/**
 * 更新抽屉路径信息显示
 */
private void updateDrawerPathInfo(String path, int fileCount) {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            if (drawerCurrentPath != null) {
                drawerCurrentPath.setText(optimizePathDisplay(path));
            }
            
            // 可以在抽屉头部显示文件数量
            if (drawerConnectionInfo != null && currentConnection != null) {
                String info = currentConnection.getHost() + " (" + fileCount + " 项)";
                drawerConnectionInfo.setText(info);
            }
        });
    }
}

/**
 * 关闭主内容区文件
 */
private void closeMainContentFile() {
    selectedFilePath = null;
    
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            // 隐藏代码查看器，显示欢迎页面
            if (codeViewerContainer != null && welcomeLayout != null) {
                codeViewerContainer.setVisibility(View.GONE);
                welcomeLayout.setVisibility(View.VISIBLE);
            }
            
            // 重置状态栏
            if (statusTextLeft != null) {
                statusTextLeft.setText("就绪");
            }
            if (statusTextRight != null) {
                statusTextRight.setText("");
            }
            
            // 重置面包屑
            if (breadcrumbText != null) {
                breadcrumbText.setText(optimizePathDisplay(currentPath));
            }
        });
    }
}

/**
 * 重写现有的loadDirectoryContent方法以使用新逻辑
 */
private void loadDirectoryContent(String path) {
    loadDirectoryContentToDrawer(path);
}
```

### 7. 添加适配器包装类以保持兼容性
在 `RemoteFileBrowserAdapter.java` 中添加包装类:

```java
/**
 * 适配器包装类，用于保持与DrawerFileAdapter的兼容性
 */
public static class AdapterWrapper extends RemoteFileBrowserAdapter {
    private DrawerFileAdapter drawerAdapter;
    
    public AdapterWrapper(DrawerFileAdapter drawerAdapter) {
        super(null); // 不使用原有监听器
        this.drawerAdapter = drawerAdapter;
    }
    
    @Override
    public void updateFiles(List<RemoteFileItem> files, String path) {
        if (drawerAdapter != null) {
            drawerAdapter.updateFiles(files, path);
        }
    }
    
    @Override
    public void updateFiles(List<RemoteFileItem> files) {
        if (drawerAdapter != null) {
            drawerAdapter.updateFiles(files);
        }
    }
}
```

## 验证要点
1. 抽屉中正确显示文件树
2. 点击目录能正确导航
3. 点击文件能在主内容区打开(显示占位内容)
4. 文件图标根据类型正确显示
5. 长按目录显示操作菜单
6. 抽屉路径信息正确更新
7. 主内容区状态栏正确显示文件信息
8. 现有所有SFTP功能保持完整

## 预估工作量
- 文件适配器和工具类创建: ~4万token
- Fragment交互逻辑实现: ~3万token
- 兼容性处理和测试: ~2万token
- 总计: ~9万token

## 下一步
完成此批次后，文件树交互功能完整，可以继续进行 Batch 004 的基础代码查看器实现。
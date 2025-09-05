# Batch 004: 基础代码查看器实现

## 目标
创建VSCode Light风格的代码查看器组件，实现基础的文本文件显示功能，包括行号显示、等宽字体、白色背景等基础特性，支持大文件的流畅滚动。

## 任务列表

### 1. 创建代码查看器主布局
**文件**: `app/src/main/res/layout/layout_code_viewer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- VSCode风格代码查看器布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:background="@color/code_viewer_background">

    <!-- 行号区域 -->
    <ScrollView
        android:id="@+id/line_numbers_scroll"
        android:layout_width="@dimen/line_number_width"
        android:layout_height="match_parent"
        android:scrollbars="none"
        android:scrollbarSize="0dp"
        android:background="@color/line_number_background">

        <LinearLayout
            android:id="@+id/line_numbers_container"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingTop="@dimen/code_viewer_padding_vertical"
            android:paddingBottom="@dimen/code_viewer_padding_vertical">
            <!-- 行号将通过代码动态添加 -->
        </LinearLayout>
    </ScrollView>

    <!-- 分隔线 -->
    <View
        android:layout_width="1dp"
        android:layout_height="match_parent"
        android:background="@color/line_number_separator" />

    <!-- 代码内容区域 -->
    <HorizontalScrollView
        android:id="@+id/code_horizontal_scroll"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:scrollbars="horizontal">

        <ScrollView
            android:id="@+id/code_content_scroll"
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:scrollbars="vertical"
            android:scrollbarThumbVertical="@color/scrollbar_thumb"
            android:scrollbarTrackVertical="@color/scrollbar_track">

            <LinearLayout
                android:id="@+id/code_lines_container"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:paddingStart="@dimen/code_viewer_padding_horizontal"
                android:paddingEnd="@dimen/code_viewer_padding_horizontal"
                android:paddingTop="@dimen/code_viewer_padding_vertical"
                android:paddingBottom="@dimen/code_viewer_padding_vertical">
                <!-- 代码行将通过代码动态添加 -->
            </LinearLayout>
        </ScrollView>
    </HorizontalScrollView>

</LinearLayout>
```

### 2. 创建代码查看器样式资源
**文件**: `app/src/main/res/values/dimens.xml` (添加到现有文件)

```xml
<!-- 在现有dimens.xml中添加 -->

<!-- 代码查看器尺寸 -->
<dimen name="line_number_width">60dp</dimen>
<dimen name="code_viewer_padding_horizontal">12dp</dimen>
<dimen name="code_viewer_padding_vertical">8dp</dimen>
<dimen name="code_line_height">20sp</dimen>
<dimen name="code_text_size">13sp</dimen>
<dimen name="line_number_text_size">12sp</dimen>
```

**文件**: `app/src/main/res/values/colors.xml` (添加到现有文件)

```xml
<!-- 在现有colors.xml中添加 -->

<!-- VSCode Light主题颜色 -->
<color name="code_viewer_background">#FFFFFF</color>
<color name="line_number_background">#F8F8F8</color>
<color name="line_number_text">#999999</color>
<color name="line_number_separator">#E0E0E0</color>
<color name="code_text_default">#333333</color>
<color name="code_selection_background">#ADD6FF</color>
<color name="scrollbar_thumb">#C0C0C0</color>
<color name="scrollbar_track">#F0F0F0</color>

<!-- 基础语法高亮颜色 (下个批次使用) -->
<color name="code_keyword">#0000FF</color>
<color name="code_string">#A31515</color>
<color name="code_comment">#008000</color>
<color name="code_number">#09885A</color>
```

### 3. 创建代码查看器自定义组件
**文件**: `app/src/main/java/com/termux/app/ui/CodeViewer.java`

```java
package com.termux.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.termux.R;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * VSCode风格的代码查看器
 * 支持行号显示、语法高亮、水平垂直滚动
 */
public class CodeViewer extends LinearLayout {
    private static final String LOG_TAG = "CodeViewer";
    private static final int MAX_LINES_INITIAL = 1000; // 初始最大显示行数
    
    // UI组件
    private ScrollView lineNumbersScroll;
    private LinearLayout lineNumbersContainer;
    private HorizontalScrollView codeHorizontalScroll;
    private ScrollView codeContentScroll;
    private LinearLayout codeLinesContainer;
    
    // 数据
    private List<String> codeLines;
    private String fileName;
    private boolean isLoading = false;
    
    // 样式
    private Typeface codeTypeface;
    private int codeTextSize;
    private int lineNumberTextSize;
    private int lineHeight;
    
    // 回调接口
    public interface OnCodeViewerListener {
        void onCodeLoaded(int totalLines);
        void onCodeLoadError(String error);
        void onLineClicked(int lineNumber, String lineContent);
    }
    
    private OnCodeViewerListener listener;
    private ExecutorService executor;
    
    public CodeViewer(@NonNull Context context) {
        this(context, null);
    }
    
    public CodeViewer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public CodeViewer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }
    
    private void initView() {
        // 加载布局
        LayoutInflater.from(getContext()).inflate(R.layout.layout_code_viewer, this, true);
        
        // 初始化组件
        lineNumbersScroll = findViewById(R.id.line_numbers_scroll);
        lineNumbersContainer = findViewById(R.id.line_numbers_container);
        codeHorizontalScroll = findViewById(R.id.code_horizontal_scroll);
        codeContentScroll = findViewById(R.id.code_content_scroll);
        codeLinesContainer = findViewById(R.id.code_lines_container);
        
        // 初始化样式
        initStyles();
        
        // 初始化数据
        codeLines = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
        
        // 同步滚动
        setupSynchronizedScrolling();
        
        Logger.logInfo(LOG_TAG, "CodeViewer initialized");
    }
    
    private void initStyles() {
        Context context = getContext();
        
        // 代码字体 - 优先使用等宽字体
        codeTypeface = Typeface.MONOSPACE;
        try {
            // 尝试使用更好的等宽字体
            codeTypeface = Typeface.create("monospace", Typeface.NORMAL);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to create monospace font, using default");
        }
        
        // 文字大小
        codeTextSize = (int) context.getResources().getDimension(R.dimen.code_text_size);
        lineNumberTextSize = (int) context.getResources().getDimension(R.dimen.line_number_text_size);
        lineHeight = (int) context.getResources().getDimension(R.dimen.code_line_height);
    }
    
    /**
     * 设置同步滚动
     */
    private void setupSynchronizedScrolling() {
        // 监听代码区域的垂直滚动，同步到行号区域
        codeContentScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (lineNumbersScroll != null) {
                lineNumbersScroll.scrollTo(0, scrollY);
            }
        });
        
        // 监听行号区域的垂直滚动，同步到代码区域
        lineNumbersScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (codeContentScroll != null) {
                codeContentScroll.scrollTo(codeContentScroll.getScrollX(), scrollY);
            }
        });
    }
    
    /**
     * 设置监听器
     */
    public void setOnCodeViewerListener(OnCodeViewerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 加载代码内容
     */
    public void loadCode(String content, String fileName) {
        this.fileName = fileName;
        
        if (content == null || content.isEmpty()) {
            showEmptyContent();
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Loading code for file: " + fileName);
        
        isLoading = true;
        showLoadingState();
        
        // 在后台线程处理文本分割
        executor.execute(() -> {
            try {
                List<String> lines = splitContentIntoLines(content);
                
                // 回到主线程更新UI
                post(() -> {
                    codeLines = lines;
                    displayCodeLines();
                    isLoading = false;
                    
                    if (listener != null) {
                        listener.onCodeLoaded(lines.size());
                    }
                });
                
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error processing code content: " + e.getMessage());
                post(() -> {
                    showErrorState("处理文件内容时出错: " + e.getMessage());
                    isLoading = false;
                    
                    if (listener != null) {
                        listener.onCodeLoadError(e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 将内容分割成行
     */
    private List<String> splitContentIntoLines(String content) {
        List<String> lines = new ArrayList<>();
        
        // 处理不同的换行符
        String[] lineArray = content.split("\\r?\\n");
        
        for (String line : lineArray) {
            // 将制表符转换为空格
            String processedLine = line.replace("\t", "    ");
            lines.add(processedLine);
        }
        
        return lines;
    }
    
    /**
     * 显示代码行
     */
    private void displayCodeLines() {
        Logger.logInfo(LOG_TAG, "Displaying " + codeLines.size() + " lines of code");
        
        // 清空现有内容
        lineNumbersContainer.removeAllViews();
        codeLinesContainer.removeAllViews();
        
        // 分批显示，避免一次性创建太多View导致卡顿
        int linesToShow = Math.min(codeLines.size(), MAX_LINES_INITIAL);
        
        for (int i = 0; i < linesToShow; i++) {
            addCodeLine(i + 1, codeLines.get(i));
        }
        
        // 如果还有更多行，可以考虑实现懒加载
        if (codeLines.size() > MAX_LINES_INITIAL) {
            Logger.logInfo(LOG_TAG, "Large file detected (" + codeLines.size() + " lines), showing first " + linesToShow + " lines");
            // TODO: 在后续版本中实现虚拟化滚动来支持大文件
        }
    }
    
    /**
     * 添加单行代码
     */
    private void addCodeLine(int lineNumber, String lineContent) {
        Context context = getContext();
        
        // 创建行号TextView
        TextView lineNumberView = createLineNumberView(context, lineNumber);
        lineNumbersContainer.addView(lineNumberView);
        
        // 创建代码行TextView
        TextView codeLineView = createCodeLineView(context, lineContent, lineNumber);
        codeLinesContainer.addView(codeLineView);
    }
    
    /**
     * 创建行号视图
     */
    private TextView createLineNumberView(Context context, int lineNumber) {
        TextView lineNumberView = new TextView(context);
        lineNumberView.setText(String.valueOf(lineNumber));
        lineNumberView.setTextSize(lineNumberTextSize);
        lineNumberView.setTextColor(ContextCompat.getColor(context, R.color.line_number_text));
        lineNumberView.setTypeface(codeTypeface);
        lineNumberView.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        lineNumberView.setPadding(8, 2, 8, 2);
        
        // 设置行高
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            lineHeight
        );
        lineNumberView.setLayoutParams(params);
        
        return lineNumberView;
    }
    
    /**
     * 创建代码行视图
     */
    private TextView createCodeLineView(Context context, String lineContent, int lineNumber) {
        TextView codeLineView = new TextView(context);
        
        // 处理空行
        String displayText = TextUtils.isEmpty(lineContent) ? " " : lineContent;
        codeLineView.setText(displayText);
        
        codeLineView.setTextSize(codeTextSize);
        codeLineView.setTextColor(ContextCompat.getColor(context, R.color.code_text_default));
        codeLineView.setTypeface(codeTypeface);
        codeLineView.setPadding(0, 2, 16, 2);
        
        // 设置行高
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            lineHeight
        );
        codeLineView.setLayoutParams(params);
        
        // 设置点击事件
        codeLineView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLineClicked(lineNumber, lineContent);
            }
        });
        
        // 设置背景选择器
        codeLineView.setBackgroundResource(android.R.drawable.list_selector_background);
        
        return codeLineView;
    }
    
    /**
     * 显示空内容状态
     */
    private void showEmptyContent() {
        lineNumbersContainer.removeAllViews();
        codeLinesContainer.removeAllViews();
        
        TextView emptyView = new TextView(getContext());
        emptyView.setText("文件内容为空");
        emptyView.setTextSize(16);
        emptyView.setTextColor(ContextCompat.getColor(getContext(), R.color.line_number_text));
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(32, 32, 32, 32);
        
        codeLinesContainer.addView(emptyView);
    }
    
    /**
     * 显示加载状态
     */
    private void showLoadingState() {
        lineNumbersContainer.removeAllViews();
        codeLinesContainer.removeAllViews();
        
        TextView loadingView = new TextView(getContext());
        loadingView.setText("正在加载...");
        loadingView.setTextSize(16);
        loadingView.setTextColor(ContextCompat.getColor(getContext(), R.color.line_number_text));
        loadingView.setGravity(android.view.Gravity.CENTER);
        loadingView.setPadding(32, 32, 32, 32);
        
        codeLinesContainer.addView(loadingView);
    }
    
    /**
     * 显示错误状态
     */
    private void showErrorState(String errorMessage) {
        lineNumbersContainer.removeAllViews();
        codeLinesContainer.removeAllViews();
        
        TextView errorView = new TextView(getContext());
        errorView.setText("加载失败\n\n" + errorMessage);
        errorView.setTextSize(14);
        errorView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
        errorView.setGravity(android.view.Gravity.CENTER);
        errorView.setPadding(32, 32, 32, 32);
        
        codeLinesContainer.addView(errorView);
    }
    
    /**
     * 滚动到指定行
     */
    public void scrollToLine(int lineNumber) {
        if (lineNumber > 0 && lineNumber <= codeLines.size()) {
            int scrollY = (lineNumber - 1) * lineHeight;
            codeContentScroll.smoothScrollTo(0, scrollY);
        }
    }
    
    /**
     * 获取总行数
     */
    public int getTotalLines() {
        return codeLines.size();
    }
    
    /**
     * 获取文件名
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * 是否正在加载
     */
    public boolean isLoading() {
        return isLoading;
    }
    
    /**
     * 清空内容
     */
    public void clear() {
        if (codeLines != null) {
            codeLines.clear();
        }
        if (lineNumbersContainer != null) {
            lineNumbersContainer.removeAllViews();
        }
        if (codeLinesContainer != null) {
            codeLinesContainer.removeAllViews();
        }
        fileName = null;
        isLoading = false;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
```

### 4. 创建文件内容加载器
**文件**: `app/src/main/java/com/termux/app/utils/FileContentLoader.java`

```java
package com.termux.app.utils;

import com.termux.app.models.RemoteFileItem;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.shared.logger.Logger;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 文件内容加载器
 * 负责从SFTP服务器加载文件内容
 */
public class FileContentLoader {
    private static final String LOG_TAG = "FileContentLoader";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB 最大文件大小
    private static final int BUFFER_SIZE = 8192;
    
    private SFTPConnectionManager sftpManager;
    
    public FileContentLoader(SFTPConnectionManager sftpManager) {
        this.sftpManager = sftpManager;
    }
    
    /**
     * 加载远程文件内容
     */
    public Single<String> loadFileContent(RemoteFileItem file) {
        return Single.fromCallable(() -> {
            Logger.logInfo(LOG_TAG, "Loading content for file: " + file.getPath());
            
            // 检查文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IOException("文件太大，无法预览 (大小: " + FileUtils.formatFileSize(file.getSize()) + ")");
            }
            
            // 从SFTP服务器读取文件内容
            return readFileFromSFTP(file);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 从SFTP服务器读取文件
     */
    private String readFileFromSFTP(RemoteFileItem file) throws IOException {
        if (sftpManager == null || !sftpManager.isConnected()) {
            throw new IOException("SFTP连接未建立");
        }
        
        try {
            // 获取文件输入流
            InputStream inputStream = sftpManager.getFileInputStream(file.getPath());
            if (inputStream == null) {
                throw new IOException("无法打开文件: " + file.getPath());
            }
            
            return readInputStreamToString(inputStream);
            
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to read file from SFTP: " + e.getMessage());
            throw new IOException("读取文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 读取输入流内容为字符串
     */
    private String readInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            // 移除最后一个换行符
            if (content.length() > 0) {
                content.setLength(content.length() - 1);
            }
            
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Logger.logError(LOG_TAG, "Error closing input stream: " + e.getMessage());
            }
        }
        
        Logger.logInfo(LOG_TAG, "File content loaded, size: " + content.length() + " characters");
        return content.toString();
    }
    
    /**
     * 检查文件是否可以预览
     */
    public static boolean canPreviewFile(RemoteFileItem file) {
        if (file.isDirectory()) {
            return false;
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        // 检查文件类型
        String fileName = file.getName().toLowerCase();
        return FileUtils.isCodeFile(fileName) || isTextFile(fileName);
    }
    
    private static boolean isTextFile(String fileName) {
        String extension = FileUtils.getFileExtension(fileName);
        return extension.equals("txt") || extension.equals("log") || extension.equals("cfg") ||
               extension.equals("conf") || extension.equals("ini") || extension.equals("env") ||
               extension.equals("readme") || extension.equals("license");
    }
}
```

### 5. 更新SFTP连接管理器支持文件读取
在 `SFTPConnectionManager.java` 中添加文件读取方法:

```java
/**
 * 获取文件输入流
 */
public InputStream getFileInputStream(String filePath) throws IOException {
    if (!isConnected()) {
        throw new IOException("SFTP connection not established");
    }
    
    try {
        return sftpClient.newSFTPFileTransfer().newFileInputStream(filePath);
    } catch (Exception e) {
        Logger.logError(LOG_TAG, "Failed to get file input stream: " + e.getMessage());
        throw new IOException("Failed to open file: " + filePath, e);
    }
}
```

### 6. 更新Fragment集成代码查看器
在 `RemoteFileBrowserFragment.java` 中更新代码查看相关逻辑:

```java
// 添加新的成员变量
private CodeViewer codeViewer;
private FileContentLoader fileContentLoader;

/**
 * 初始化UI组件 (更新现有方法)
 */
private void initViews(View view) {
    // ... 原有代码 ...
    
    // 初始化代码查看器
    setupCodeViewer();
    
    // 初始化文件内容加载器
    fileContentLoader = new FileContentLoader(sftpManager);
    
    Logger.logInfo(LOG_TAG, "Code viewer initialized");
}

/**
 * 设置代码查看器
 */
private void setupCodeViewer() {
    if (codeViewerContainer != null) {
        codeViewer = new CodeViewer(requireContext());
        codeViewer.setOnCodeViewerListener(new CodeViewer.OnCodeViewerListener() {
            @Override
            public void onCodeLoaded(int totalLines) {
                Logger.logInfo(LOG_TAG, "Code loaded with " + totalLines + " lines");
                updateMainContentStatus(null, totalLines + " 行");
            }
            
            @Override
            public void onCodeLoadError(String error) {
                Logger.logError(LOG_TAG, "Code load error: " + error);
                updateMainContentStatus(null, "加载失败");
            }
            
            @Override
            public void onLineClicked(int lineNumber, String lineContent) {
                Logger.logInfo(LOG_TAG, "Line " + lineNumber + " clicked");
                // 可以在这里实现行点击的功能，如跳转等
            }
        });
        
        // 添加到容器
        codeViewerContainer.addView(codeViewer);
    }
}

/**
 * 更新加载代码文件内容的实现
 */
private void loadCodeFileContent(RemoteFileItem file) {
    Logger.logInfo(LOG_TAG, "Loading code file content: " + file.getName());
    
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
                Logger.logInfo(LOG_TAG, "File content loaded successfully: " + file.getName());
                
                if (codeViewer != null) {
                    codeViewer.loadCode(content, file.getName());
                }
            },
            throwable -> {
                Logger.logError(LOG_TAG, "Failed to load file content: " + throwable.getMessage());
                
                String errorMessage = "加载文件失败: " + throwable.getMessage();
                showSimpleTextPreview(file, errorMessage);
                updateMainContentStatus(file, "加载失败");
                
                // 显示友好的错误对话框
                NetworkErrorHandler.showErrorDialog(requireContext(), throwable, () -> {
                    // 重试加载
                    loadCodeFileContent(file);
                });
            }
        );
    
    compositeDisposable.add(disposable);
}

/**
 * 更新主内容区状态 (重载方法)
 */
private void updateMainContentStatus(RemoteFileItem file, String status) {
    if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
            // 更新底部状态栏
            if (statusTextLeft != null) {
                if (file != null) {
                    String leftText = status != null ? status : "已打开: " + file.getName();
                    statusTextLeft.setText(leftText);
                } else if (status != null) {
                    statusTextLeft.setText(status);
                }
            }
            
            if (statusTextRight != null && file != null && status == null) {
                String rightText = FileUtils.formatFileSize(file.getSize());
                statusTextRight.setText(rightText);
            }
            
            // 更新面包屑导航
            if (breadcrumbText != null && file != null) {
                String breadcrumb = optimizePathDisplay(currentPath) + " → " + file.getName();
                breadcrumbText.setText(breadcrumb);
            }
        });
    }
}
```

### 7. 添加必要的import语句
在相关Java文件中添加:

```java
// RemoteFileBrowserFragment.java
import com.termux.app.ui.CodeViewer;
import com.termux.app.utils.FileContentLoader;

// SFTPConnectionManager.java  
import java.io.InputStream;
```

## 验证要点
1. 代码查看器正确显示VSCode Light风格
2. 行号与代码内容正确对齐
3. 等宽字体正确应用
4. 垂直和水平滚动同步工作
5. 大文件加载不会导致卡顿
6. 文件加载错误正确处理
7. 空文件和加载状态正确显示
8. 行点击事件正确响应

## 预估工作量
- CodeViewer组件和布局创建: ~5万token
- 文件内容加载器实现: ~2万token
- Fragment集成和测试: ~2万token
- 总计: ~9万token

## 下一步
完成此批次后，基础代码查看器功能完整，可以继续进行 Batch 005 的代码语法高亮和样式完善。
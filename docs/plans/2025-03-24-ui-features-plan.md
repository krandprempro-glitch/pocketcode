# UI功能改进实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan.

**Goal:** 实现三个UI功能：1)终端快捷指令添加收藏路径CD切换 2)文件浏览器顶部状态栏简化 3)文件浏览器双栏布局重构

**Architecture:** 基于现有Termux架构，修改Fragment和Adapter，保持MVVM模式，复用现有Repository获取数据

**Tech Stack:** Android Java/Kotlin, RecyclerView, MVVM, SharedPreferences

---

## 文件结构变更

### 修改文件
1. `TermuxFragment.java` - 添加快捷指令收藏路径分组
2. `fragment_remote_file_browser_drawer.xml` - 重构Toolbar和主体布局
3. `RemoteFileBrowserFragment.kt` - 添加双栏显示和导航逻辑
4. `RemoteFileBrowserViewModel.kt` - 添加文件夹内容状态

### 新增文件
1. `FolderGridAdapter.kt` - 文件夹网格适配器
2. `item_folder_grid.xml` - 文件夹网格项布局
3. `ic_circle_connected.xml` - 绿色连接状态图标
4. `ic_circle_disconnected.xml` - 灰色断开状态图标

---

## Chunk 1: 快捷指令收藏路径功能

### Task 1.1: 修改快捷指令菜单数据结构

**Files:**
- Modify: `app/src/main/java/com/termux/app/terminal/CommandGroupAdapter.java` (如需要添加新Category)
- Modify: `app/src/main/java/com/termux/app/fragments/TermuxFragment.java:1196-1254`

**Context:**
当前 `prepareCommandGroups()` 方法返回 SSH连接、AI指令、系统指令三组。需要添加第四组"收藏路径"。

- [ ] **Step 1: 在 TermuxFragment 中添加 BookmarkRepository 依赖**

在 TermuxFragment 类中添加：
```java
private BookmarkRepository mBookmarkRepository;
```

在 `onCreate()` 或初始化方法中注入：
```java
mBookmarkRepository = BookmarkRepository.getInstance(getContext());
```

- [ ] **Step 2: 修改 prepareCommandGroups() 添加收藏路径分组**

修改 `prepareCommandGroups()` 方法，在返回 groups 之前添加：

```java
// 4. 收藏路径类 (第四类)
List<ClaudeCodeMenuHelper.Command> bookmarkCommands = new ArrayList<>();
try {
    // 获取当前连接的收藏列表
    Connection currentConnection = mSSHConnectionManager.getCurrentConnection();
    if (currentConnection != null) {
        List<Bookmark> bookmarks = mBookmarkRepository.getBookmarksForConnection(currentConnection.id);
        for (Bookmark bookmark : bookmarks) {
            String cdCommand = "cd " + bookmark.path;
            bookmarkCommands.add(new ClaudeCodeMenuHelper.Command(cdCommand, bookmark.name));
        }
    }
} catch (Exception e) {
    Logger.logError(LOG_TAG, "Failed to load bookmarks for menu: " + e.getMessage());
}

if (!bookmarkCommands.isEmpty()) {
    groups.add(0, new CommandGroupAdapter.CommandGroup(
        CommandGroupAdapter.CommandCategory.BOOKMARKS, bookmarkCommands));
}
```

- [ ] **Step 3: 添加 CommandCategory.BOOKMARKS 枚举**

在 `CommandGroupAdapter.java` 中添加：
```java
public enum CommandCategory {
    BOOKMARKS("📁 收藏路径"),
    SSH_CONNECTIONS("🔌 SSH连接"),
    AI_COMMANDS("🤖 AI指令"),
    SYSTEM_COMMANDS("⚙️ 系统指令");

    private final String displayName;

    CommandCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

- [ ] **Step 4: 确保所有分组默认折叠**

在 `CommandGroupAdapter` 的构造函数或初始化时，设置所有分组默认折叠：
```java
// 默认所有分组折叠
for (int i = 0; i < groups.size(); i++) {
    collapsedGroups.put(i, true);
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/TermuxFragment.java
git add app/src/main/java/com/termux/app/terminal/CommandGroupAdapter.java
git commit -m "feat: 快捷指令菜单添加收藏路径分组，所有分组默认折叠"
```

---

## Chunk 2: 文件浏览器顶部状态栏简化

### Task 2.1: 修改 Toolbar 布局

**Files:**
- Modify: `app/src/main/res/layout/fragment_remote_file_browser_drawer.xml:19-73`
- Modify: `app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt` - 更新状态栏逻辑

- [ ] **Step 1: 修改 Toolbar 布局 XML**

替换 Toolbar 区域 (lines 19-73)：
```xml
<!-- ActionBar -->
<androidx.appcompat.widget.Toolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="?attr/actionBarSize"
    android:background="?attr/colorPrimary"
    android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar">

    <!-- 简化布局：汉堡菜单 + 目录名 + 连接状态点 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingEnd="16dp">

        <!-- 目录名 - 只显示当前文件夹名 -->
        <TextView
            android:id="@+id/toolbar_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="project"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="@android:color/white"
            android:maxLines="1"
            android:ellipsize="end"
            android:layout_marginStart="8dp" />

        <!-- 连接状态指示点 -->
        <ImageView
            android:id="@+id/connection_status_dot"
            android:layout_width="10dp"
            android:layout_height="10dp"
            android:src="@drawable/ic_circle_disconnected"
            android:layout_marginStart="8dp"
            android:contentDescription="连接状态" />
    </LinearLayout>
</androidx.appcompat.widget.Toolbar>
```

- [ ] **Step 2: 创建连接状态图标**

创建 `app/src/main/res/drawable/ic_circle_connected.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#4CAF50" />
    <size android:width="10dp" android:height="10dp" />
</shape>
```

创建 `app/src/main/res/drawable/ic_circle_disconnected.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#9E9E9E" />
    <size android:width="10dp" android:height="10dp" />
</shape>
```

- [ ] **Step 3: 更新 Fragment 中的状态栏逻辑**

在 `RemoteFileBrowserFragment.kt` 中添加/修改方法：

```kotlin
private fun updateToolbarTitle(path: String) {
    val toolbarTitle = view?.findViewById<TextView>(R.id.toolbar_title)
    // 提取最后一段路径名
    val folderName = path.split("/").lastOrNull()?.takeIf { it.isNotEmpty() } ?: "/"
    toolbarTitle?.text = folderName
}

private fun updateConnectionStatus(isConnected: Boolean, ip: String? = null) {
    val statusDot = view?.findViewById<ImageView>(R.id.connection_status_dot)
    val drawableRes = if (isConnected) {
        R.drawable.ic_circle_connected
    } else {
        R.drawable.ic_circle_disconnected
    }
    statusDot?.setImageResource(drawableRes)

    // 长按显示完整信息
    statusDot?.setOnLongClickListener {
        val message = if (isConnected && ip != null) {
            "已连接到: $ip"
        } else {
            "未连接"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        true
    }
}
```

- [ ] **Step 4: 在导航和连接状态变化时调用更新**

在 `onViewCreated` 和状态观察中调用：
```kotlin
// 观察当前路径变化
viewModel.currentPath.observe(viewLifecycleOwner) { path ->
    updateToolbarTitle(path)
    // ... 其他逻辑
}

// 观察连接状态变化
viewModel.connectionState.observe(viewLifecycleOwner) { state ->
    when (state) {
        is ConnectionState.Connected -> updateConnectionStatus(true, state.ip)
        is ConnectionState.Disconnected -> updateConnectionStatus(false)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/fragment_remote_file_browser_drawer.xml
git add app/src/main/res/drawable/ic_circle_connected.xml
git add app/src/main/res/drawable/ic_circle_disconnected.xml
git add app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt
git commit -m "feat: 文件浏览器顶部状态栏简化，显示目录名+连接状态点"
```

---

## Chunk 3: 文件浏览器双栏布局重构

### Task 3.1: 重构主布局为双栏

**Files:**
- Modify: `app/src/main/res/layout/fragment_remote_file_browser_drawer.xml:75-237`

- [ ] **Step 1: 替换主内容区域为双栏布局**

替换原主内容区域 (lines 75-237)：
```xml
<!-- 双栏主内容区域 -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="horizontal">

    <!-- 左侧文件树 (30%) -->
    <FrameLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="3"
        android:background="@android:color/white">

        <!-- 文件树头部 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical">

            <!-- 文件树标题栏 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:background="?attr/colorPrimary"
                android:text="文件"
                android:textColor="@android:color/white"
                android:textSize="14sp"
                android:gravity="center_vertical"
                android:paddingStart="12dp" />

            <!-- 文件树列表 -->
            <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
                android:id="@+id/file_tree_swipe_refresh"
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1">

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/file_tree_recycler_view"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:scrollbars="vertical"
                    android:padding="4dp" />
            </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
        </LinearLayout>

        <!-- 加载指示器 -->
        <ProgressBar
            android:id="@+id/file_tree_loading"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone" />
    </FrameLayout>

    <!-- 拖拽分割线 -->
    <View
        android:id="@+id/divider"
        android:layout_width="4dp"
        android:layout_height="match_parent"
        android:background="#E0E0E0" />

    <!-- 右侧主内容区 (70%) -->
    <FrameLayout
        android:id="@+id/main_content_container"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="7"
        android:background="@android:color/white">

        <!-- 文件夹内容网格 -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/folder_grid_recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="8dp"
            android:visibility="visible" />

        <!-- 代码编辑器 -->
        <FrameLayout
            android:id="@+id/code_editor_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone">

            <!-- 编辑器标题栏 -->
            <LinearLayout
                android:id="@+id/editor_header"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:orientation="horizontal"
                android:background="#F5F5F5"
                android:gravity="center_vertical"
                android:paddingHorizontal="12dp">

                <ImageButton
                    android:id="@+id/editor_back_button"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:src="@drawable/ic_arrow_back"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:contentDescription="返回" />

                <TextView
                    android:id="@+id/editor_filename"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="12dp"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:maxLines="1"
                    android:ellipsize="middle" />
            </LinearLayout>

            <!-- 代码内容 -->
            <ScrollView
                android:id="@+id/code_scroll_view"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_marginTop="48dp"
                android:fillViewport="true">

                <HorizontalScrollView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content">

                    <TextView
                        android:id="@+id/code_content_text"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:padding="12dp"
                        android:fontFamily="monospace"
                        android:textSize="14sp"
                        android:background="#1E1E1E"
                        android:textColor="#D4D4D4" />
                </HorizontalScrollView>
            </ScrollView>
        </FrameLayout>

        <!-- 空状态 -->
        <LinearLayout
            android:id="@+id/empty_state_layout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:orientation="vertical"
            android:gravity="center"
            android:visibility="gone">

            <ImageView
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:src="@drawable/ic_folder_empty"
                android:alpha="0.3" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="该目录为空"
                android:textSize="16sp"
                android:textColor="#999999" />
        </LinearLayout>
    </FrameLayout>
</LinearLayout>
```

- [ ] **Step 2: 移除 NavigationView（不再需要）**

删除 NavigationView 部分 (原 lines 238-363)。

- [ ] **Step 3: Commit 布局变更**

```bash
git add app/src/main/res/layout/fragment_remote_file_browser_drawer.xml
git commit -m "feat: 文件浏览器布局重构为双栏，左侧文件树右侧主内容区"
```

### Task 3.2: 创建文件夹网格适配器

**Files:**
- Create: `app/src/main/java/com/termux/filebrowser/adapters/FolderGridAdapter.kt`
- Create: `app/src/main/res/layout/item_folder_grid.xml`

- [ ] **Step 1: 创建文件夹项布局 XML**

创建 `app/src/main/res/layout/item_folder_grid.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="12dp"
    android:background="?attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true">

    <ImageView
        android:id="@+id/item_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@drawable/ic_folder" />

    <TextView
        android:id="@+id/item_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textSize="12sp"
        android:maxLines="2"
        android:ellipsize="end"
        android:gravity="center" />

</LinearLayout>
```

- [ ] **Step 2: 创建 FolderGridAdapter**

创建 `app/src/main/java/com/termux/filebrowser/adapters/FolderGridAdapter.kt`：
```kotlin
package com.termux.filebrowser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.filebrowser.domain.model.FileItem

class FolderGridAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean = { false }
) : RecyclerView.Adapter<FolderGridAdapter.ViewHolder>() {

    private var items: List<FileItem> = emptyList()

    fun submitList(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val name: TextView = itemView.findViewById(R.id.item_name)

        fun bind(item: FileItem) {
            name.text = item.name

            // 根据类型设置图标
            icon.setImageResource(when {
                item.isDirectory -> R.drawable.ic_folder
                item.name.endsWith(".java") || item.name.endsWith(".kt") -> R.drawable.ic_file_code
                item.name.endsWith(".xml") || item.name.endsWith(".html") -> R.drawable.ic_file_xml
                item.name.endsWith(".txt") || item.name.endsWith(".md") -> R.drawable.ic_file_text
                else -> R.drawable.ic_file_generic
            })

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item) }
        }
    }
}
```

- [ ] **Step 3: Commit 适配器**

```bash
git add app/src/main/java/com/termux/filebrowser/adapters/FolderGridAdapter.kt
git add app/src/main/res/layout/item_folder_grid.xml
git commit -m "feat: 添加文件夹网格适配器 FolderGridAdapter"
```

### Task 3.3: 修改 RemoteFileBrowserFragment 实现双栏逻辑

**Files:**
- Modify: `app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt`

- [ ] **Step 1: 添加新视图组件引用**

在 Fragment 类中添加：
```kotlin
private lateinit var folderGridRecyclerView: RecyclerView
private lateinit var folderGridAdapter: FolderGridAdapter
private lateinit var codeEditorContainer: FrameLayout
private lateinit var folderGridContainer: RecyclerView
private lateinit var editorBackButton: ImageButton
private lateinit var editorFilename: TextView
private lateinit var codeContentText: TextView
```

- [ ] **Step 2: 初始化网格适配器**

在 `onViewCreated` 中：
```kotlin
// 初始化文件夹网格
folderGridAdapter = FolderGridAdapter(
    onItemClick = { item ->
        when {
            item.isDirectory -> viewModel.navigateToDirectory(item.path)
            isCodeOrTextFile(item.name) -> openCodeEditor(item)
            else -> showFileOptions(item)
        }
    },
    onItemLongClick = { item ->
        showFileContextMenu(item)
        true
    }
)

folderGridRecyclerView.layoutManager = GridLayoutManager(context, 3)
folderGridRecyclerView.adapter = folderGridAdapter
```

- [ ] **Step 3: 实现代码编辑器打开逻辑**

```kotlin
private fun openCodeEditor(file: FileItem) {
    // 切换到编辑器视图
    folderGridRecyclerView.visibility = View.GONE
    codeEditorContainer.visibility = View.VISIBLE

    // 设置文件名
    editorFilename.text = file.name

    // 加载文件内容
    lifecycleScope.launch {
        val content = viewModel.loadFileContent(file.path)
        codeContentText.text = content
    }
}

private fun closeCodeEditor() {
    codeEditorContainer.visibility = View.GONE
    folderGridRecyclerView.visibility = View.VISIBLE
}

private fun isCodeOrTextFile(filename: String): Boolean {
    val extensions = listOf(".java", ".kt", ".xml", ".html", ".css", ".js", ".json", ".md", ".txt", ".py", ".sh", ".c", ".cpp", ".h")
    return extensions.any { filename.endsWith(it, ignoreCase = true) }
}
```

- [ ] **Step 4: 设置返回按钮监听**

```kotlin
editorBackButton.setOnClickListener {
    closeCodeEditor()
}
```

- [ ] **Step 5: 处理系统返回键**

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // ... 其他初始化

    // 处理返回键
    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    codeEditorContainer.visibility == View.VISIBLE -> {
                        closeCodeEditor()
                    }
                    viewModel.canGoBack() -> {
                        viewModel.navigateUp()
                    }
                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        }
    )
}
```

- [ ] **Step 6: 观察文件夹内容并更新网格**

```kotlin
viewModel.folderContents.observe(viewLifecycleOwner) { files ->
    folderGridAdapter.submitList(files)

    // 显示/隐藏空状态
    if (files.isEmpty()) {
        emptyStateLayout.visibility = View.VISIBLE
        folderGridRecyclerView.visibility = View.GONE
    } else {
        emptyStateLayout.visibility = View.GONE
        folderGridRecyclerView.visibility = View.VISIBLE
    }
}
```

- [ ] **Step 7: Commit Fragment 修改**

```bash
git add app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt
git commit -m "feat: 实现双栏布局逻辑，文件夹网格显示和代码编辑器"
```

### Task 3.4: 修改 ViewModel 添加文件夹内容状态

**Files:**
- Modify: `app/src/main/java/com/termux/filebrowser/viewmodels/RemoteFileBrowserViewModel.kt`

- [ ] **Step 1: 添加 LiveData 和状态**

```kotlin
private val _folderContents = MutableLiveData<List<FileItem>>()
val folderContents: LiveData<List<FileItem>> = _folderContents

private val _currentPath = MutableLiveData<String>()
val currentPath: LiveData<String> = _currentPath

private val pathStack = Stack<String>()
```

- [ ] **Step 2: 添加导航方法**

```kotlin
fun navigateToDirectory(path: String) {
    pathStack.push(_currentPath.value)
    _currentPath.value = path
    loadFolderContents(path)
}

fun navigateUp(): Boolean {
    return if (pathStack.isNotEmpty()) {
        val parentPath = pathStack.pop()
        _currentPath.value = parentPath
        loadFolderContents(parentPath)
        true
    } else {
        false
    }
}

fun canGoBack(): Boolean = pathStack.isNotEmpty()

private fun loadFolderContents(path: String) {
    viewModelScope.launch {
        try {
            val files = listFilesUseCase(path)
            _folderContents.value = files
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
```

- [ ] **Step 3: 添加加载文件内容方法**

```kotlin
suspend fun loadFileContent(path: String): String {
    return try {
        getFileContentUseCase(path)
    } catch (e: Exception) {
        "Error loading file: ${e.message}"
    }
}
```

- [ ] **Step 4: Commit ViewModel 修改**

```bash
git add app/src/main/java/com/termux/filebrowser/viewmodels/RemoteFileBrowserViewModel.kt
git commit -m "feat: ViewModel添加文件夹内容状态和导航方法"
```

---

## 测试检查清单

### 功能1 测试
- [ ] 打开快捷指令菜单，确认"收藏路径"分组显示
- [ ] 确认所有分组默认折叠
- [ ] 展开收藏路径，点击收藏项，确认 `cd` 命令发送到终端
- [ ] 无收藏时显示空状态

### 功能2 测试
- [ ] 确认 Toolbar 只显示当前目录名
- [ ] SSH连接后显示绿色圆点
- [ ] 断开连接后显示灰色圆点
- [ ] 长按状态点显示完整连接信息

### 功能3 测试
- [ ] 双栏布局正常显示，左侧文件树右侧主内容
- [ ] 点击文件夹右侧显示该文件夹内容
- [ ] 点击代码文件打开编辑器
- [ ] 编辑器返回按钮正常返回文件夹视图
- [ ] 系统返回键优先返回上级目录

---

## 总结

完成此计划后，将实现：
1. 终端快捷指令支持收藏路径 CD 切换
2. 文件浏览器顶部显示简化（目录名+连接状态点）
3. 文件浏览器改为双栏布局（左侧文件树，右侧主内容区+编辑器）

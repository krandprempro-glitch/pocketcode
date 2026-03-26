package com.termux.filebrowser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.MainTabActivity
import com.termux.app.models.DrawerMenuItem
import com.termux.app.models.RemoteFileItem
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.ui.SSHConfigDialog
import com.termux.app.utils.LightToast
import com.termux.databinding.FragmentRemoteFileBrowserDrawerBinding
import com.termux.filebrowser.adapters.DrawerFileAdapter
import com.termux.filebrowser.adapters.DrawerMenuAdapter
import com.termux.filebrowser.adapters.FileListAdapter
import com.termux.filebrowser.viewmodels.RemoteFileBrowserViewModel
import com.termux.shared.logger.Logger
import kotlinx.coroutines.launch
import android.graphics.Typeface
import java.util.Date

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Simple highlighter utility
import com.termux.filebrowser.highlight.SimpleSyntaxHighlighter


/**
 * 远程文件浏览Fragment - Kotlin重构版本
 * 使用MVVM架构，DataBinding和现代Android开发最佳实践
 */
class RemoteFileBrowserFragment : Fragment(),
    DrawerFileAdapter.OnFileActionListener,
    DrawerFileAdapter.BookmarkStateProvider,
    DrawerFileAdapter.BookmarkToggleListener,
    SSHConfigDialog.OnSSHConfigListener {

    companion object {
        private const val LOG_TAG = "RemoteFileBrowserFragment"
    }

    /**
     * 目录变化监听器接口
     * 用于通知其他组件（如MainTabActivity）当前目录已变化
     */
    interface OnDirectoryChangeListener {
        /**
         * 当目录变化时调用
         * @param newPath 新的目录路径
         */
        fun onDirectoryChanged(newPath: String)
    }

    // 目录变化监听器
    private var directoryChangeListener: OnDirectoryChangeListener? = null

    /**
     * 设置目录变化监听器
     * @param listener 要设置的监听器
     */
    fun setOnDirectoryChangeListener(listener: OnDirectoryChangeListener?) {
        this.directoryChangeListener = listener
    }

    /**
     * 获取当前目录路径
     * @return 当前查看的目录路径
     */
    fun getCurrentDirectory(): String {
        return viewModel.currentPath.value
    }

    private var _binding: FragmentRemoteFileBrowserDrawerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RemoteFileBrowserViewModel by viewModels()

    // 适配器
    private lateinit var drawerMenuAdapter: DrawerMenuAdapter
    private lateinit var fileListAdapter: FileListAdapter

    // 当前查看的文件
    private var currentViewedFile: RemoteFileItem? = null

    // 对话框
    private var sshConfigDialog: SSHConfigDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemoteFileBrowserDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel的Context依赖
        viewModel.initializeWithContext(requireContext())

        initViews()
        setupAdapters()
        setupObservers()
        setupEventListeners()

        Logger.logInfo(LOG_TAG, "Fragment initialized with MVVM architecture")
    }
    
    override fun onResume() {
        super.onResume()
        
        // 在Fragment恢复时记录抽屉状态用于调试
        Logger.logInfo(LOG_TAG, "Fragment resumed, drawer open: ${binding.drawerLayout.isDrawerOpen(GravityCompat.START)}")
        // Re-measure drawer to avoid scrim-without-drawer after tab switches
        binding.navView.requestLayout()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sshConfigDialog = SSHConfigDialog(context).apply {
            setOnSSHConfigListener(this@RemoteFileBrowserFragment)
        }
    }

    private fun initViews() {
        setupToolbar()
        setupDrawerLayout()
        setupCodeEditor()
        updateConnectionStatus(false, "未连接")
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun setupDrawerLayout() {
        val toggle = ActionBarDrawerToggle(
            requireActivity(),
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        
        // 添加抽屉状态监听器，记录状态变化用于调试
        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                toggle.onDrawerSlide(drawerView, slideOffset)
            }

            override fun onDrawerOpened(drawerView: View) {
                toggle.onDrawerOpened(drawerView)
                Logger.logInfo(LOG_TAG, "Drawer opened successfully")
            }

            override fun onDrawerClosed(drawerView: View) {
                toggle.onDrawerClosed(drawerView)
                Logger.logInfo(LOG_TAG, "Drawer closed successfully")
            }

            override fun onDrawerStateChanged(newState: Int) {
                toggle.onDrawerStateChanged(newState)
                Logger.logInfo(LOG_TAG, "Drawer state changed: $newState")
            }
        })
        
        // 确保toggle状态同步
        toggle.syncState()

        // 动态设置抽屉宽度为屏幕宽度的50%
        val displayMetrics = requireActivity().resources.displayMetrics
        val drawerWidth = displayMetrics.widthPixels / 2
        val navView = binding.navView
        val params = navView.layoutParams
        params.width = drawerWidth
        navView.layoutParams = params
        // Ensure drawer remeasures after dynamic width change
        navView.requestLayout()
    }

    private fun setupCodeEditor() {
        // 新布局使用 codeContentText 作为代码编辑器
        // 初始化逻辑在显示文件时处理
        Logger.logInfo(LOG_TAG, "Code editor initialized")
    }

    // 已迁移到 CodeView 简单高亮，无需 TextMate 初始化

    private fun setupAdapters() {
        // 抽屉菜单适配器
        drawerMenuAdapter =
            DrawerMenuAdapter(requireContext(), object : DrawerMenuAdapter.OnMenuItemClickListener {
                override fun onMenuItemClick(item: DrawerMenuItem) {
                    handleDrawerMenuClick(item)
                }

                override fun onSubMenuItemClick(
                    parentItem: DrawerMenuItem,
                    subItem: DrawerMenuItem
                ) {
                    handleDrawerSubMenuClick(parentItem, subItem)
                }
            })

        // 设置菜单到容器
        binding.drawerMenuContainer?.let { container ->
            val recyclerView = RecyclerView(requireContext()).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = drawerMenuAdapter
                isNestedScrollingEnabled = false
            }
            container.addView(recyclerView)
        }

        // 初始化全屏文件列表适配器
        fileListAdapter = FileListAdapter(
            onItemClick = { item ->
                when {
                    item.name == ".." -> {
                        // 返回上一级目录
                        val currentPath = viewModel.currentPath.value
                        val parentPath = getParentPath(currentPath)
                        if (parentPath != null) {
                            viewModel.navigateToPath(parentPath)
                        }
                    }
                    item.isDirectory -> viewModel.navigateToPath(item.path)
                    isCodeOrTextFile(item.name) -> openFile(item)
                    else -> showFileOptions(item)
                }
            },
            onItemLongClick = { item ->
                showFileOperationsDialog(item)
                true
            },
            onBookmarkClick = { item, position ->
                if (viewModel.isPathBookmarked(item.path)) {
                    viewModel.removeBookmark(item.path)
                    LightToast.showShort(requireContext(), "已取消收藏")
                } else {
                    viewModel.addBookmark(item.path, item.name)
                    LightToast.showShort(requireContext(), "已添加收藏")
                }
                // 只刷新当前项的收藏图标
                fileListAdapter.notifyItemChanged(position)
            },
            isBookmarked = { path -> viewModel.isPathBookmarked(path) }
        )

        binding.fileListRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileListAdapter
        }

        initDrawerMenuItems()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察UI状态
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiStateChange(state)
                    }
                }

                // 观察连接状态
                launch {
                    viewModel.connectionState.collect { state ->
                        handleConnectionStateChange(state)
                    }
                }

                // 观察Home目录路径（连接成功后获取）
                launch {
                    viewModel.homeDirectory.collect { homePath ->
                        homePath?.let {
                            Logger.logInfo(LOG_TAG, "Home directory resolved: $it")
                        }
                    }
                }

                // 观察文件列表
                launch {
                    viewModel.fileList.collect { files ->
                        val currentPath = viewModel.currentPath.value
                        val displayFiles = if (currentPath != "/") {
                            // 添加返回上级目录条目
                            val parentItem = RemoteFileItem().apply {
                                name = ".."
                                path = getParentPath(currentPath) ?: "/"
                                isDirectory = true
                                permissions = "drwxr-xr-x"
                                lastModified = System.currentTimeMillis() / 1000
                            }
                            listOf(parentItem) + files
                        } else {
                            files
                        }
                        fileListAdapter.submitList(displayFiles)

                        // 更新空状态显示
                        if (files.isEmpty() && currentPath == "/") {
                            binding.emptyStateLayout?.visibility = View.VISIBLE
                            binding.fileListRecyclerView?.visibility = View.GONE
                        } else {
                            binding.emptyStateLayout?.visibility = View.GONE
                            binding.fileListRecyclerView?.visibility = View.VISIBLE
                        }
                    }
                }

                // 观察当前路径
                launch {
                    viewModel.currentPath.collect { path ->
                        updatePathDisplay(path)
                        // 通知目录变化监听器（用于GitHistoryFragment等组件同步）
                        directoryChangeListener?.onDirectoryChanged(path)
                    }
                }

                // 观察错误信息
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            showErrorMessage(it)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupEventListeners() {
        // 下拉刷新
        binding.fileListSwipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentDirectory()
        }

        // 路径长按操作
        binding.drawerCurrentPath.setOnLongClickListener {
            showPathActionDialog()
            true
        }

        // toolbar导航点击监听
        binding.toolbar.setNavigationOnClickListener {
            Logger.logInfo(LOG_TAG, "Toolbar navigation clicked")

            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                // 抽屉已打开，直接关闭
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                // 抽屉未打开，尝试打开
                binding.drawerLayout.openDrawer(GravityCompat.START)

                // 延迟检查是否成功打开，如果没有则尝试修复
                binding.root.postDelayed({
                    if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START) &&
                        binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                        Logger.logInfo(LOG_TAG, "Drawer open failed, attempting to fix state")
                        checkAndFixDrawerState()
                    }
                }, 200)
            }
        }

        // Home按钮点击事件 - 返回用户主目录
        binding.btnGoHome.setOnClickListener {
            val homePath = viewModel.homeDirectory.value
            if (homePath != null) {
                viewModel.navigateToPath(homePath)
                LightToast.showShort(requireContext(), "已返回主目录")
            } else {
                LightToast.showShort(requireContext(), "主目录未获取")
            }
        }
    }

    private fun initDrawerMenuItems() {
        val menuItems = mutableListOf<DrawerMenuItem>()

        // 主功能模块
        val mainModule = DrawerMenuItem("main_functions", "功能菜单", R.drawable.ic_menu)
        mainModule.addSubItem(
            DrawerMenuItem(
                "ssh_config",
                "SSH连接配置",
                R.drawable.ic_ssh,
                DrawerMenuItem.MenuAction.SSH_CONFIG
            )
        )
        mainModule.addSubItem(
            DrawerMenuItem(
                "bookmarks",
                "收藏夹管理",
                R.drawable.ic_bookmark_small,
                DrawerMenuItem.MenuAction.BOOKMARK_MANAGE_ALL
            )
        )
        mainModule.addSubItem(
            DrawerMenuItem(
                "refresh",
                "刷新目录",
                R.drawable.ic_refresh_small,
                DrawerMenuItem.MenuAction.REFRESH
            )
        )

        // 设置子模块
        val settingsModule = DrawerMenuItem("settings", "设置选项", R.drawable.ic_settings_small)
        settingsModule.addSubItem(
            DrawerMenuItem(
                "settings_display",
                "显示设置",
                R.drawable.ic_settings_small,
                DrawerMenuItem.MenuAction.SETTINGS_DISPLAY
            )
        )
        settingsModule.addSubItem(
            DrawerMenuItem(
                "settings_connection",
                "连接设置",
                R.drawable.ic_settings_small,
                DrawerMenuItem.MenuAction.SETTINGS_CONNECTION
            )
        )
        mainModule.addSubItem(settingsModule)

        menuItems.add(mainModule)
        drawerMenuAdapter.updateMenuItems(menuItems)
    }

    private fun handleUiStateChange(state: RemoteFileBrowserViewModel.UiState) {
        binding.apply {
            loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            emptyStateLayout.visibility = if (state.isEmpty) View.VISIBLE else View.GONE

            // 根据连接状态更新空状态提示文案
            if (state.isEmpty) {
                val emptyText = if (viewModel.isConnected()) "该目录为空" else "请先连接SSH服务器"
                emptyStateText?.text = emptyText
            }

            // 停止刷新动画
            if (!state.isLoading && fileListSwipeRefresh.isRefreshing) {
                fileListSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun handleConnectionStateChange(state: RemoteFileBrowserViewModel.ConnectionState) {
        val isConnected = state is RemoteFileBrowserViewModel.ConnectionState.Connected
        val statusText = when (state) {
            is RemoteFileBrowserViewModel.ConnectionState.Disconnected -> "未连接"
            is RemoteFileBrowserViewModel.ConnectionState.Connecting -> "连接中..."
            is RemoteFileBrowserViewModel.ConnectionState.Connected -> "已连接"
            is RemoteFileBrowserViewModel.ConnectionState.Error -> "连接失败"
        }

        updateConnectionStatus(isConnected, statusText, if (state is RemoteFileBrowserViewModel.ConnectionState.Connected) state.config else null)

        // 更新Home按钮可见性（连接成功后显示）
        binding.btnGoHome.visibility = if (isConnected) View.VISIBLE else View.GONE

        // 更新抽屉头部的连接信息和项目名称
        when (state) {
            is RemoteFileBrowserViewModel.ConnectionState.Connected -> {
                val config = state.config
                val connectionInfo = "${config.username}@${config.host}:${config.port}"
                val projectName = config.displayName

                binding.drawerConnectionInfo.text = connectionInfo
                binding.drawerProjectName.text = projectName
            }
            else -> {
                binding.drawerConnectionInfo.text = "未连接"
                binding.drawerProjectName.text = "远程文件浏览"
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean, statusText: String, config: SSHConnectionConfig? = null) {
        // 更新状态点为绿色或灰色
        val drawableRes = if (connected) R.drawable.ic_circle_connected else R.drawable.ic_circle_disconnected
        binding.connectionStatusDot.setImageResource(drawableRes)

        // 长按显示完整连接信息
        binding.connectionStatusDot.setOnLongClickListener {
            val message = if (connected && config != null) {
                "${config.displayName} (${config.host}) - $statusText"
            } else {
                statusText
            }
            LightToast.showShort(requireContext(), message)
            true
        }
    }

    private fun updatePathDisplay(path: String) {
        binding.drawerCurrentPath.text = optimizePathDisplay(path)
        // 只显示目录名在Toolbar
        val folderName = path.split("/").lastOrNull()?.takeIf { it.isNotEmpty() } ?: "/"
        binding.toolbarTitle.text = folderName
    }

    private fun optimizePathDisplay(fullPath: String): String {
        if (fullPath.length <= 40) return fullPath

        val pathParts = fullPath.split("/")
        return if (pathParts.size > 2) {
            val currentDir = pathParts.last()
            val parentDir = pathParts[pathParts.size - 2]
            ".../$parentDir/$currentDir"
        } else {
            fullPath
        }
    }

    private fun handleDrawerMenuClick(item: DrawerMenuItem) {
        Logger.logInfo(LOG_TAG, "Drawer menu clicked: ${item.title}")

        when (item.action) {
            DrawerMenuItem.MenuAction.SSH_CONFIG -> {
                showConnectionDialog()
                closeDrawer()
            }
            DrawerMenuItem.MenuAction.REFRESH -> {
                viewModel.refreshCurrentDirectory()
                closeDrawer()
            }
            else -> {}
        }
    }

    private fun handleDrawerSubMenuClick(parentItem: DrawerMenuItem, subItem: DrawerMenuItem) {
        Logger.logInfo(LOG_TAG, "Drawer submenu clicked: ${subItem.title}")

        when (subItem.action) {
            DrawerMenuItem.MenuAction.SSH_CONFIG -> {
                showConnectionDialog()
                closeDrawer()
            }
            DrawerMenuItem.MenuAction.REFRESH -> {
                viewModel.refreshCurrentDirectory()
                closeDrawer()
            }
            DrawerMenuItem.MenuAction.BOOKMARK_MANAGE_ALL -> {
                showAllBookmarksDialog()
                closeDrawer()
            }
            DrawerMenuItem.MenuAction.SETTINGS_DISPLAY,
            DrawerMenuItem.MenuAction.SETTINGS_CONNECTION -> {
                LightToast.showShort(requireContext(), "功能开发中...")
                closeDrawer()
            }
            else -> {}
        }
    }

    private fun closeDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
    
    /**
     * 检查并修复抽屉的异常状态
     * 只在真正检测到问题时才介入处理
     */
    private fun checkAndFixDrawerState() {
        try {
            val isOpen = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
            val isVisible = binding.drawerLayout.isDrawerVisible(GravityCompat.START)
            
            Logger.logInfo(LOG_TAG, "Drawer state check - isOpen: $isOpen, isVisible: $isVisible")
            
            // 只在检测到异常状态时才处理：有蒙层但抽屉不可见
            if (!isOpen && isVisible) {
                Logger.logInfo(LOG_TAG, "Detected abnormal state: visible but not open, fixing...")
                
                // 温和地重置状态
                binding.drawerLayout.closeDrawer(GravityCompat.START, false)
                
                // 给系统一点时间处理状态变更，然后重新打开
                binding.root.postDelayed({
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }, 50)
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to check drawer state: ${e.message}")
        }
    }

    private fun showConnectionDialog() {
        sshConfigDialog?.show()
    }

    private fun showAllBookmarksDialog() {
        val bookmarks = viewModel.getBookmarks().toMutableList()

        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("收藏夹")
                .setMessage("还没有收藏任何目录。\n\n你可以通过菜单中的「收藏当前目录」功能添加书签。")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        com.termux.app.ui.dialogs.BookmarkManagementDialog.show(
            requireContext(),
            bookmarks,
            object : com.termux.app.ui.dialogs.BookmarkManagementDialog.OnBookmarkManagementListener {
                override fun onBookmarkNavigate(bookmark: com.termux.app.models.DirectoryBookmark) {
                    viewModel.navigateToPath(bookmark.fullPath)
                    closeDrawer()
                }

                override fun onBookmarkEdit(bookmark: com.termux.app.models.DirectoryBookmark) {
                    viewModel.updateBookmark(bookmark)
                    viewModel.refreshCurrentDirectory()
                }

                override fun onBookmarkDelete(bookmark: com.termux.app.models.DirectoryBookmark) {
                    viewModel.removeBookmark(bookmark.fullPath)
                    viewModel.refreshCurrentDirectory()
                }

                override fun onBookmarkSendToTerminal(bookmark: com.termux.app.models.DirectoryBookmark) {
                    // 发送 cd 命令到终端
                    sendCdCommandToTerminal(bookmark.fullPath)
                    LightToast.showShort(requireContext(), "已发送 cd 命令到终端: ${bookmark.fullPath}")
                }

                override fun onBookmarkAdd(path: String, name: String) {
                    viewModel.addBookmark(path, name)
                    viewModel.refreshCurrentDirectory()
                }
            }
        )
    }

    private fun showPathActionDialog() {
        val currentPath = viewModel.currentPath.value
        if (currentPath.isBlank()) return

        val isBookmarked = viewModel.isPathBookmarked(currentPath)
        val options = mutableListOf<String>()

        options.add("📋 复制路径")
        options.add(if (isBookmarked) "⭐ 取消收藏此目录" else "⭐ 收藏此目录")
        options.add("📚 查看所有书签")

        AlertDialog.Builder(requireContext())
            .setTitle("当前路径: ${optimizePathDisplay(currentPath)}")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> copyPathToClipboard(currentPath)
                    1 -> if (isBookmarked) {
                        viewModel.removeBookmark(currentPath)
                    } else {
                        showAddBookmarkDialog(currentPath)
                    }
                    2 -> showAllBookmarksDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun copyPathToClipboard(path: String) {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Remote Path", path)
        clipboard.setPrimaryClip(clip)
        LightToast.showShort(requireContext(), "路径已复制到剪贴板")
    }

    private fun showAddBookmarkDialog(path: String) {
        val defaultName = if (path == "/") "根目录" else path.substringAfterLast('/')
        val editText = EditText(requireContext()).apply {
            setText(defaultName)
            setSelection(defaultName.length)
            hint = "请输入书签名称"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("添加书签")
            .setMessage("路径: $path")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val name = editText.text.toString().trim().ifEmpty { defaultName }
                viewModel.addBookmark(path, name)
                LightToast.showShort(requireContext(), "已添加书签: $name")
                viewModel.refreshCurrentDirectory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showErrorMessage(error: String) {
        Logger.logError(LOG_TAG, "Error: $error")
        LightToast.showShort(requireContext(), error)
    }

    // DrawerFileAdapter.OnFileActionListener 实现
    override fun onFileClick(file: RemoteFileItem) {
        if (!file.isDirectory && com.termux.app.utils.FileUtils.isImageFile(file.name)) {
            showImagePreviewDialog(file)
            return
        }
        if (file.isDirectory) {
            viewModel.navigateToPath(file.path)
        } else {
            // 处理文件点击，在主体区域显示代码
            loadFileContent(file)
        }
    }

    override fun onFileMoreClick(file: RemoteFileItem, anchorView: View) {
        showFileOperationsDialog(file)
    }

    override fun onDirectoryEnter(directory: RemoteFileItem) {
        viewModel.navigateToPath(directory.path)
    }

    private fun showFileOperationsDialog(file: RemoteFileItem) {
        val options = mutableListOf<String>()

        if (file.isDirectory) {
            options.add("📂 进入目录")
            options.add(if (isBookmarked(file.path)) "⭐ 取消收藏" else "⭐ 收藏目录")
        } else {
            options.add("📥 下载文件")
        }
        options.add("ℹ️ 查看属性")

        AlertDialog.Builder(requireContext())
            .setTitle(file.name)
            .setItems(options.toTypedArray()) { _, which ->
                handleFileOperation(file, which, options)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun handleFileOperation(file: RemoteFileItem, optionIndex: Int, options: List<String>) {
        val selectedOption = options[optionIndex]

        when {
            selectedOption.contains("进入目录") -> viewModel.navigateToPath(file.path)
            selectedOption.contains("收藏目录") -> {
                showAddBookmarkDialog(file.path)
                viewModel.refreshCurrentDirectory()
            }
            selectedOption.contains("取消收藏") -> {
                viewModel.removeBookmark(file.path)
                viewModel.refreshCurrentDirectory()
            }
            selectedOption.contains("下载文件") -> LightToast.showShort(requireContext(), "下载功能开发中...")
            selectedOption.contains("查看属性") -> showFileProperties(file)
        }
    }

    private fun showFileProperties(file: RemoteFileItem) {
        val properties = buildString {
            append("名称: ${file.name}\n")
            append("路径: ${file.path}\n")
            append("类型: ${if (file.isDirectory) "目录" else "文件"}\n")
            append("大小: ${file.size} 字节\n")
            append("权限: ${file.permissions}\n")
            append("修改时间: ${Date(file.lastModified)}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("文件属性")
            .setMessage(properties)
            .setPositiveButton("确定", null)
            .show()
    }

    // BookmarkStateProvider 和 BookmarkToggleListener 实现
    override fun isBookmarked(path: String): Boolean = viewModel.isPathBookmarked(path)

    override fun onBookmarkToggle(file: RemoteFileItem) {
        if (!file.isDirectory) return

        if (isBookmarked(file.path)) {
            viewModel.removeBookmark(file.path)
            LightToast.showShort(requireContext(), "已取消收藏")
        } else {
            viewModel.addBookmark(file.path, file.name)
            LightToast.showShort(requireContext(), "已添加收藏")
        }
        viewModel.refreshCurrentDirectory()
    }

    // SSHConfigDialog.OnSSHConfigListener 实现
    override fun onSSHConnect(config: SSHConnectionConfig) {
        viewModel.connect(config)
    }

    override fun onSSHConfigSaved(config: SSHConnectionConfig) {
        LightToast.showShort(requireContext(), "SSH配置已保存")
    }

    override fun onSSHConfigDeleted(configName: String) {
        LightToast.showShort(requireContext(), "SSH配置已删除: $configName")
    }

    override fun onDialogClosed() {
        Logger.logInfo(LOG_TAG, "SSH config dialog closed")
    }

    /**
     * SSH连接成功回调
     * 从MainTabActivity调用，当SSH连接Fragment连接成功时通知此Fragment
     */
    fun onSSHConnected(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "SSH connected notification from SSH tab: ${config.host}")
        viewModel.connect(config)
    }

    /**
     * SSH断开连接回调
     * 从MainTabActivity调用，当SSH连接Fragment断开连接时通知此Fragment
     */
    fun onSSHDisconnected() {
        Logger.logInfo(LOG_TAG, "SSH disconnected notification from SSH tab")
        viewModel.disconnect()
    }

    /**
     * 加载文件内容到代码编辑器
     */
    private fun loadFileContent(file: RemoteFileItem) {
        if (!viewModel.isConnected()) {
            LightToast.showShort(requireContext(), "请先连接SSH服务器")
            return
        }

        // 检查文件大小限制(5MB)
        if (file.size > 5 * 1024 * 1024) {
            LightToast.showShort(requireContext(), "文件过大，无法预览 (>5MB)")
            return
        }

        currentViewedFile = file
        
        // 显示加载状态
        binding.statusTextLeft.text = "正在加载文件..."
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 在后台线程读取文件内容
                val content = withContext(Dispatchers.IO) {
                    viewModel.readFileContent(file.path)
                }
                
                // 在主线程更新UI
                displayFileContent(file, content)
                
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Failed to load file content: ${e.message}")
                LightToast.showShort(requireContext(), "文件加载失败: ${e.message}")
                binding.statusTextLeft.text = "就绪"
            }
        }
    }

    /**
     * 图片预览弹窗
     */
    private fun showImagePreviewDialog(file: RemoteFileItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.preview_image)
        val titleView = dialogView.findViewById<TextView>(R.id.preview_title)
        titleView.text = "${file.name}\n${file.path}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()

        dialog.setOnShowListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) { viewModel.readFileBytes(file.path) }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageView.setImageBitmap(bmp)
                } catch (e: Exception) {
                    LightToast.showShort(requireContext(), "图片加载失败: ${e.message}")
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        dialog.window?.setDimAmount(0.6f)
    }
    
    /**
     * 打开文件
     */
    private fun openFile(file: RemoteFileItem) {
        when {
            file.name.endsWith(".jpg", ignoreCase = true) ||
            file.name.endsWith(".jpeg", ignoreCase = true) ||
            file.name.endsWith(".png", ignoreCase = true) ||
            file.name.endsWith(".gif", ignoreCase = true) ||
            file.name.endsWith(".webp", ignoreCase = true) -> {
                showImagePreviewDialog(file)
            }
            else -> {
                // 读取并显示文件内容
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val content = withContext(Dispatchers.IO) {
                            viewModel.readFileContent(file.path)
                        }
                        displayFileContent(file, content)
                    } catch (e: Exception) {
                        LightToast.showShort(requireContext(), "无法打开文件: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 在代码编辑器中显示文件内容
     */
    private fun displayFileContent(file: RemoteFileItem, content: String) {
        // 使用新的代码编辑器视图
        binding.codeContentText?.let { editor ->
            // 根据文件扩展名生成高亮文本并显示
            val highlighted = SimpleSyntaxHighlighter.build(content, file.name)
            editor.text = highlighted

            // 更新文件名显示
            binding.editorFilename?.text = file.name

            // 显示代码编辑器，隐藏文件列表
            binding.fileListRecyclerView?.visibility = View.GONE
            binding.codeEditorContainer?.visibility = View.VISIBLE

            // 设置返回按钮点击事件
            binding.editorBackButton?.setOnClickListener {
                closeCodeEditor()
            }

            // 更新状态栏
            val lines = content.lines().size
            binding.statusTextLeft?.text = "文件已加载"
            binding.statusTextRight?.text = "$lines 行 | ${formatFileSize(content.length.toLong())}"

            Logger.logInfo(LOG_TAG, "File loaded: ${file.name}, ${content.length} chars, $lines lines")
        }
    }

    private fun closeCodeEditor() {
        binding.codeEditorContainer?.visibility = View.GONE
        binding.fileListRecyclerView?.visibility = View.VISIBLE
    }

    private fun isCodeOrTextFile(filename: String): Boolean {
        val extensions = listOf(".java", ".kt", ".xml", ".html", ".css", ".js", ".json", ".md", ".txt", ".py", ".sh", ".c", ".cpp", ".h", "properties", "gradle")
        return extensions.any { filename.endsWith(it, ignoreCase = true) }
    }

    private fun showFileOptions(file: RemoteFileItem) {
        // 显示文件选项对话框
        val options = arrayOf("查看", "下载", "分享")
        AlertDialog.Builder(requireContext())
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> downloadFile(file)
                    2 -> shareFile(file)
                }
            }
            .show()
    }

    private fun downloadFile(file: RemoteFileItem) {
        // 实现下载逻辑
        LightToast.showShort(requireContext(), "下载: ${file.name}")
    }

    private fun shareFile(file: RemoteFileItem) {
        // 实现分享逻辑
        LightToast.showShort(requireContext(), "分享: ${file.name}")
    }

    // 语言检测与高亮由 SimpleSyntaxHighlighter 处理
    
    /**
     * 格式化文件大小显示
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    /**
     * 从外部同步SSH连接状态（由MainTabActivity调用）
     */
    fun syncConnectionFromExternal(config: SSHConnectionConfig) {
        Logger.logInfo(LOG_TAG, "Syncing external SSH connection: ${config.host}")

        // 通过ViewModel同步连接状态
        viewModel.syncExternalConnection(config)

        // 连接成功后，刷新当前目录的文件列表
        viewModel.refreshCurrentDirectory()

        // 如果抽屉已关闭，可以选择性地自动展开抽屉显示文件
        if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * 发送 cd 命令到终端
     */
    private fun sendCdCommandToTerminal(path: String) {
        (requireActivity() as? MainTabActivity)?.sendCommandToTerminal("cd $path")
    }

    /**
     * 获取父目录路径
     */
    private fun getParentPath(path: String): String? {
        if (path == "/") return null
        val lastSlash = path.lastIndexOf('/')
        return when {
            lastSlash <= 0 -> "/"
            else -> path.substring(0, lastSlash)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sshConfigDialog?.dismiss()
        currentViewedFile = null
        _binding = null
    }
}

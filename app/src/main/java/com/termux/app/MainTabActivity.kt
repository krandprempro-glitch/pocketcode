package com.termux.app

import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.termux.R
import com.termux.app.configuration.fragments.ConfigurationMainFragment
import com.termux.app.floating.views.FloatingActionButton
import android.widget.Toast
import com.termux.app.ui.SSHConfigDialog
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.shared.logger.Logger
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import com.termux.app.fragments.GitHistoryFragment
import com.termux.app.fragments.TermuxFragment
import com.termux.app.sessions.SessionListFragment
import com.termux.app.clipboard.ClipboardSyncManager
import com.termux.app.clipboard.ClipboardSyncStatusView
import com.termux.filebrowser.RemoteFileBrowserFragment
import com.termux.filebrowser.RemoteFileBrowserFragment.OnDirectoryChangeListener

/**
 * 主Tab界面Activity
 * 包含4个Tab页面：终端、SFTP文件浏览、待开发、配置
 * 集成悬浮窗功能管理
 */
class MainTabActivity : AppCompatActivity(), OnDirectoryChangeListener {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: TabPagerAdapter
    private var floatingActionButton: FloatingActionButton? = null
    private var clipboardSyncStatus: ClipboardSyncStatusView? = null
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use standard tabs layout without hybrid approach
        setContentView(R.layout.activity_main_tabs)

        // Fix status bar color to match dark background
        window.statusBarColor = getColor(R.color.dark_background)

        initTabViews()
        initClipboardSync()
        initFloatingButton()
        handleIntent(intent)
    }

    private fun initTabViews() {
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        pagerAdapter = TabPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 关键修复：设置offscreenPageLimit为3，保留所有4个Fragment不被销毁
        // 防止TermuxFragment在切换tab时被销毁导致终端状态丢失
        viewPager.offscreenPageLimit = 3

        // Disable horizontal swipe gestures
        viewPager.isUserInputEnabled = false

        // Disable all ViewPager2 animations completely
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as RecyclerView

            // Remove all animations
            recyclerView.itemAnimator = null
            recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            // Disable the scroll animation duration
            val scrollDurationField = ViewPager2::class.java.getDeclaredField("mScrollEventAdapter")
            scrollDurationField.isAccessible = true
        } catch (e: Exception) {
            // Ignore reflection errors
        }

        // Connect TabLayout with ViewPager2 and override tab selection behavior
        val mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "终端"
                    tab.setIcon(android.R.drawable.ic_menu_manage)
                }
                1 -> {
                    tab.text = "文件浏览"
                    tab.setIcon(android.R.drawable.ic_menu_view)
                }
                2 -> {
                    tab.text = "Git 记录"
                    tab.setIcon(android.R.drawable.ic_menu_recent_history)
                }
                3 -> {
                    tab.text = "配置"
                    tab.setIcon(android.R.drawable.ic_menu_preferences)
                }
            }
        }
        mediator.attach()

        // Override tab selection to disable animation
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    // Force immediate switch without animation
                    viewPager.setCurrentItem(it.position, false)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun initClipboardSync() {
        ClipboardSyncManager.getInstance().init(this)

        // 初始化状态图标
        clipboardSyncStatus = findViewById(R.id.clipboard_sync_status)

        // 监听连接状态
        SFTPConnectionManager.getInstance().connectionStatus
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { status ->
                when (status) {
                    SFTPConnectionManager.ConnectionStatus.CONNECTED -> {
                        clipboardSyncStatus?.showSyncing()
                    }
                    else -> {
                        clipboardSyncStatus?.hide()
                    }
                }
            }
    }

    private fun initFloatingButton() {
        floatingActionButton = FloatingActionButton(this)
        floatingActionButton?.setOnFloatingActionListener(object : FloatingActionButton.OnFloatingActionListener {
            override fun onMenuToggle(isVisible: Boolean) {
                // 菜单切换
            }

            override fun onSSHConnectionClicked() {
                showSSHConnectionDialog()
            }

            override fun onSSHConfigSelected(config: com.termux.app.models.SSHConnectionConfig) {
                // 从悬浮菜单选择SSH配置后直接连接
                connectToSSH(config)
            }

            override fun onRunCommandClicked() {
                showRunCommandDialog()
            }

            override fun onQuickSettingsClicked() {
                // 跳转到配置页面
                tabLayout.getTabAt(3)?.select()
            }
        })

        // 在Activity的根布局中显示悬浮按钮
        val rootContainer = findViewById<android.widget.FrameLayout>(android.R.id.content)
        floatingActionButton?.show(rootContainer)
    }

    private fun showSSHConnectionDialog() {
        val sshDialog = SSHConfigDialog(this)
        sshDialog.setOnSSHConfigListener(object : SSHConfigDialog.OnSSHConfigListener {
            override fun onSSHConnect(config: com.termux.app.models.SSHConnectionConfig?) {
                // 直接执行SSH连接
                config?.let {
                    connectToSSH(it)
                }
            }

            override fun onSSHConfigSaved(config: com.termux.app.models.SSHConnectionConfig?) {
                // 配置保存成功
                Toast.makeText(this@MainTabActivity, "SSH配置已保存", Toast.LENGTH_SHORT).show()
            }

            override fun onSSHConfigDeleted(configName: String?) {
                // 配置删除成功
                Toast.makeText(this@MainTabActivity, "SSH配置已删除: $configName", Toast.LENGTH_SHORT).show()
            }

            override fun onDialogClosed() {
                // 对话框关闭
            }
        })
        sshDialog.show()
    }

    private fun connectToSSH(config: com.termux.app.models.SSHConnectionConfig) {
        // 显示连接中提示
        Toast.makeText(this, "正在连接到 ${config.name} (${config.host}:${config.port})", Toast.LENGTH_SHORT).show()

        // 使用SFTP连接管理器进行真实连接
        val sftpManager = SFTPConnectionManager.getInstance()

        val disposable = sftpManager.connect(config)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { success ->
                    if (success) {
                        Toast.makeText(this@MainTabActivity, "SSH连接成功: ${config.name}", Toast.LENGTH_LONG).show()
                        // 同步更新RemoteFileBrowserFragment的抽屉文件显示
                        syncRemoteFileBrowserConnection(config)
                    } else {
                        Toast.makeText(this@MainTabActivity, "SSH连接失败: 连接未建立", Toast.LENGTH_LONG).show()
                    }
                },
                { error ->
                    val errorMsg = when (error) {
                        is java.net.UnknownHostException -> "主机不可达: ${config.host}"
                        is java.net.ConnectException -> "连接被拒绝: ${config.host}:${config.port}"
                        is java.net.SocketTimeoutException -> "连接超时"
                        is java.lang.SecurityException -> "认证失败，请检查用户名和密码"
                        else -> "连接失败: ${error.message}"
                    }
                    Toast.makeText(this@MainTabActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            )

        disposables.add(disposable)
    }

    private fun showRunCommandDialog() {
        // 使用完整的运行命令功能
        val actionExtensions = com.termux.app.floating.extensions.FloatingActionExtensions(this)
        actionExtensions.handleRunCommandAction()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val navigateTo = it.getStringExtra("navigate_to")
            if ("configuration" == navigateTo) {
                // 导航到配置页面
                tabLayout.getTabAt(3)?.select() // Tab4
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingActionButton?.hide()
        disposables.clear()
    }

    /**
     * 发送命令到终端
     * 由其他Fragment调用，将命令发送到TermuxFragment的终端中
     */
    fun sendCommandToTerminal(command: String) {
        val terminalFragment = pagerAdapter.getCurrentFragment(0) as? TermuxFragment
        if (terminalFragment != null) {
            terminalFragment.sendCommandToTerminal(command)
            // 切换到终端Tab
            viewPager.setCurrentItem(0, false)
        } else {
            Toast.makeText(this, "终端不可用", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 同步更新RemoteFileBrowserFragment的抽屉文件显示
     */
    private fun syncRemoteFileBrowserConnection(config: com.termux.app.models.SSHConnectionConfig) {
        // 获取当前显示的Fragment
        val currentFragment = pagerAdapter.getCurrentFragment(1) // Tab1是文件浏览
        if (currentFragment is RemoteFileBrowserFragment) {
            // 通过Fragment的ViewModel同步连接状态和加载文件列表
            currentFragment.syncConnectionFromExternal(config)

            // 同时同步GitHistoryFragment，获取当前目录并通知其刷新
            val gitHistoryFragment = pagerAdapter.getCurrentFragment(2)
            if (gitHistoryFragment is GitHistoryFragment) {
                val currentDir = currentFragment.getCurrentDirectory()
                if (currentDir.isNotEmpty()) {
                    gitHistoryFragment.onDirectoryChanged(currentDir)
                }
            }
        }
    }

    /**
     * 实现 OnDirectoryChangeListener 接口
     * 当RemoteFileBrowserFragment中的目录变化时，自动同步GitHistoryFragment
     */
    override fun onDirectoryChanged(newPath: String) {
        Logger.logDebug("MainTabActivity", "onDirectoryChanged called with path: $newPath")

        // 获取GitHistoryFragment（Tab3）
        val gitHistoryFragment = pagerAdapter.getCurrentFragment(2)
        Logger.logDebug("MainTabActivity", "GitHistoryFragment instance: $gitHistoryFragment")
        if (gitHistoryFragment is GitHistoryFragment) {
            gitHistoryFragment.onDirectoryChanged(newPath)
        }
    }

    /**
     * Tab页面适配器 - 包含4个Fragment
     */
    private class TabPagerAdapter(@NonNull private val fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

        private val fragmentList = mutableListOf<Fragment?>(null, null, null, null)

        fun getCurrentFragment(position: Int): Fragment? {
            return fragmentList.getOrNull(position)
        }

        @NonNull
        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> SessionListFragment() // Session list tab
                1 -> RemoteFileBrowserFragment() // File browser tab
                2 -> GitHistoryFragment() // Git 记录 tab
                3 -> ConfigurationMainFragment() // Configuration tab
                else -> SessionListFragment()
            }

            // 为RemoteFileBrowserFragment设置目录变化监听器
            if (position == 1 && fragment is RemoteFileBrowserFragment) {
                fragment.setOnDirectoryChangeListener(fragmentActivity as? OnDirectoryChangeListener)
            }

            // 保存Fragment引用用于后续同步
            if (position < fragmentList.size) {
                fragmentList[position] = fragment
            }

            return fragment
        }

        override fun getItemCount(): Int {
            return 4 // 4 tabs: Terminal, File Browser, Placeholder, Settings
        }
    }

}
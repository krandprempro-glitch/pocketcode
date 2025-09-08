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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.termux.R
import com.termux.app.configuration.fragments.ConfigurationMainFragment
import com.termux.app.floating.views.FloatingActionButton
import android.widget.Toast
import com.termux.app.ui.SSHConfigDialog
import com.termux.app.sftp.SFTPConnectionManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import com.termux.app.fragments.GitChangesFragment
import com.termux.app.fragments.SSHConnectionFragment
import com.termux.app.fragments.TermuxFragment
import com.termux.filebrowser.RemoteFileBrowserFragment

/**
 * 主Tab界面Activity
 * 包含4个Tab页面：终端、SFTP文件浏览、Git变更、配置
 * 集成悬浮窗功能管理
 */
class MainTabActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: TabPagerAdapter
    private var floatingActionButton: FloatingActionButton? = null
    private val disposables = CompositeDisposable()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use standard tabs layout without hybrid approach
        setContentView(R.layout.activity_main_tabs)
        
        initTabViews()
        initFloatingButton()
        handleIntent(intent)
    }
    
    private fun initTabViews() {
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        
        pagerAdapter = TabPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
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
                    tab.text = "Git变更"
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
    
    private fun initFloatingButton() {
        floatingActionButton = FloatingActionButton(this)
        floatingActionButton?.setOnFloatingActionListener(object : FloatingActionButton.OnFloatingActionListener {
            override fun onMenuToggle(isVisible: Boolean) {
                // 菜单切换
            }
            
            override fun onSSHConnectionClicked() {
                showSSHConnectionDialog()
            }
            
            override fun onRunCommandClicked() {
                showRunCommandDialog()
            }
            
            override fun onQuickSettingsClicked() {
                // 跳转到配置页面
                tabLayout.getTabAt(3)?.select()
            }
        })
        
        // 显示悬浮按钮
        floatingActionButton?.show()
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
        // 显示运行命令dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("运行命令")
            .setMessage("运行命令功能开发中...")
            .setPositiveButton("确定", null)
            .show()
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
     * 同步更新RemoteFileBrowserFragment的抽屉文件显示
     */
    private fun syncRemoteFileBrowserConnection(config: com.termux.app.models.SSHConnectionConfig) {
        // 获取当前显示的Fragment
        val currentFragment = pagerAdapter.getCurrentFragment(1) // Tab1是文件浏览
        if (currentFragment is RemoteFileBrowserFragment) {
            // 通过Fragment的ViewModel同步连接状态和加载文件列表
            currentFragment.syncConnectionFromExternal(config)
        }
    }
    
    /**
     * Tab页面适配器 - 包含所有4个Fragment
     */
    private class TabPagerAdapter(@NonNull fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        
        private val fragmentList = mutableListOf<Fragment?>(null, null, null, null)
        
        fun getCurrentFragment(position: Int): Fragment? {
            return fragmentList.getOrNull(position)
        }
        
        @NonNull
        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> TermuxFragment() // Terminal tab
                1 -> RemoteFileBrowserFragment() // File browser tab
                2 -> GitChangesFragment() // Git changes tab
                3 -> ConfigurationMainFragment() // Configuration tab
                else -> TermuxFragment()
            }
            
            // 保存Fragment引用用于后续同步
            if (position < fragmentList.size) {
                fragmentList[position] = fragment
            }
            
            return fragment
        }
        
        override fun getItemCount(): Int {
            return 4 // All 4 tabs are fragments now
        }
    }
}
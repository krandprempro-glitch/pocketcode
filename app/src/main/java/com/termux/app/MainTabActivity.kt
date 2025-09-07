package com.termux.app

import android.content.Intent
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.termux.R
import com.termux.app.configuration.fragments.ConfigurationMainFragment
import com.termux.app.floating.views.FloatingActionButton
import com.termux.app.ui.SSHConfigDialog
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
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
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
        }.attach()
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
                // 连接SSH服务器
                config?.let {
                    // 切换到文件浏览tab
                    tabLayout.getTabAt(1)?.select()
                    // TODO: 这里可以进一步集成SSH连接逻辑
                }
            }
            
            override fun onSSHConfigSaved(config: com.termux.app.models.SSHConnectionConfig?) {
                // 配置保存成功
            }
            
            override fun onSSHConfigDeleted(configName: String?) {
                // 配置删除成功
            }
            
            override fun onDialogClosed() {
                // 对话框关闭
            }
        })
        sshDialog.show()
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
    }
    
    /**
     * Tab页面适配器 - 包含所有4个Fragment
     */
    private class TabPagerAdapter(@NonNull fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        
        @NonNull
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TermuxFragment() // Terminal tab
                1 -> RemoteFileBrowserFragment() // File browser tab
                2 -> GitChangesFragment() // Git changes tab
                3 -> ConfigurationMainFragment() // Configuration tab
                else -> TermuxFragment()
            }
        }
        
        override fun getItemCount(): Int {
            return 4 // All 4 tabs are fragments now
        }
    }
}
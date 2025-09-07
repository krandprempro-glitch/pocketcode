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
import com.termux.app.floating.managers.FloatingWindowManager
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
    private lateinit var floatingManager: FloatingWindowManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use standard tabs layout without hybrid approach
        setContentView(R.layout.activity_main_tabs)
        
        initTabViews()
        initFloatingWindow()
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
    
    private fun initFloatingWindow() {
        floatingManager = FloatingWindowManager.getInstance(this)
        
        // 应用启动时恢复悬浮按钮状态
        floatingManager.onAppStarted()
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
        floatingManager.onAppStopped()
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
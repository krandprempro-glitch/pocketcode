package com.termux.app;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.termux.R;
import com.termux.app.fragments.SSHConnectionFragment;
import com.termux.app.fragments.TermuxFragment;
import com.termux.filebrowser.RemoteFileBrowserFragment;
import com.termux.app.fragments.GitChangesFragment;
import com.termux.app.fragments.SettingsFragment;
import com.termux.app.models.SSHConnectionConfig;

/**
 * 主Tab界面Activity
 * 包含4个Tab页面：终端、SFTP文件浏览、Git变更、设置
 * 所有Tab都使用Fragment实现
 */
public class MainTabActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabPagerAdapter pagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Use standard tabs layout without hybrid approach
        setContentView(R.layout.activity_main_tabs);
        
        initTabViews();
    }
    
    private void initTabViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        
        pagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Disable horizontal swipe gestures
        viewPager.setUserInputEnabled(false);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("终端");
                    tab.setIcon(android.R.drawable.ic_menu_manage);
                    break;
                case 1:
                    tab.setText("文件浏览");
                    tab.setIcon(android.R.drawable.ic_menu_view);
                    break;
                case 2:
                    tab.setText("Git变更");
                    tab.setIcon(android.R.drawable.ic_menu_recent_history);
                    break;
                case 3:
                    tab.setText("设置");
                    tab.setIcon(android.R.drawable.ic_menu_preferences);
                    break;
            }
        }).attach();
    }
    
    /**
     * Tab页面适配器 - 包含所有4个Fragment
     */
    private static class TabPagerAdapter extends FragmentStateAdapter {

        public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new TermuxFragment(); // Terminal tab
                case 1:
                    return new RemoteFileBrowserFragment(); // File browser tab
                case 2:
                    return new GitChangesFragment(); // Git changes tab
                case 3:
                    return new SettingsFragment(); // Settings tab
                default:
                    return new TermuxFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4; // All 4 tabs are fragments now
        }
    }
}

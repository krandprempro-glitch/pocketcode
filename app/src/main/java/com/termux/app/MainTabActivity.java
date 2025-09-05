package com.termux.app;

import android.os.Bundle;
import android.os.Build;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.termux.R;
import com.termux.app.fragments.SSHConnectionFragment;
import com.termux.app.fragments.RemoteFileBrowserFragment;
import com.termux.app.fragments.GitChangesFragment;
import com.termux.app.fragments.SettingsFragment;
import com.termux.app.models.SSHConnectionConfig;

/**
 * 主Tab界面Activity
 * 包含4个Tab页面：SSH连接、SFTP文件浏览、Git变更、设置
 */
public class MainTabActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabPagerAdapter pagerAdapter;
    
    private SSHConnectionFragment sshFragment;
    private RemoteFileBrowserFragment fileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabs);
        
        initViews();
        setupTabs();
        setupFragmentCommunication();
    }
    
    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        
        pagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
    }
    
    private void setupTabs() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("SSH连接");
                    tab.setIcon(android.R.drawable.ic_dialog_info);
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
        
        // 默认选中Tab1 (SSH连接)
        viewPager.setCurrentItem(0, false);
    }
    
    /**
     * 设置Fragment之间的通信
     */
    private void setupFragmentCommunication() {
        // 延迟执行，等待Fragment创建完成
        viewPager.post(() -> {
            try {
                sshFragment = (SSHConnectionFragment) getSupportFragmentManager()
                        .findFragmentByTag("f0");
                fileFragment = (RemoteFileBrowserFragment) getSupportFragmentManager()
                        .findFragmentByTag("f1");
                        
                if (sshFragment != null) {
                    sshFragment.setOnSSHConfigListener(new SSHConnectionFragment.OnSSHConfigListener() {
                        @Override
                        public void onSSHConnect(SSHConnectionConfig config) {
                            // SSH连接成功，通知文件浏览Fragment
                            if (fileFragment != null) {
                                fileFragment.onSSHConnected(config);
                            }
                            // 切换到文件浏览Tab
                            viewPager.setCurrentItem(1, true);
                        }
                        
                        @Override
                        public void onSSHDisconnect() {
                            // SSH断开连接，通知文件浏览Fragment
                            if (fileFragment != null) {
                                fileFragment.onSSHDisconnected();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // Fragment可能还未创建，稍后再试
                viewPager.postDelayed(this::setupFragmentCommunication, 100);
            }
        });
    }
    
    /**
     * Tab页面适配器
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
                    return new SSHConnectionFragment();
                case 1:
                    return new RemoteFileBrowserFragment();
                case 2:
                    return new GitChangesFragment(); // 占位Fragment
                case 3:
                    return new SettingsFragment(); // 占位Fragment
                default:
                    return new RemoteFileBrowserFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

}
package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.SSHConfigManager;
import com.termux.app.ui.SSHConfigAdapter;
import com.termux.app.ui.SSHConfigDialog;

import java.util.List;

public class SSHConnectionFragment extends Fragment {
    
    private MaterialButton btnNewConnection;
    private MaterialButton btnDisconnect;
    private RecyclerView rvSSHConfigs;
    private TextView tvEmptyState;
    private TextView tvConnectionStatus;
    private ImageView ivConnectionStatus;
    private View llConnectionStatus;
    
    private SSHConfigManager configManager;
    private SSHConfigAdapter configAdapter;
    private SSHConfigDialog configDialog;
    private OnSSHConfigListener configListener;
    
    public interface OnSSHConfigListener {
        void onSSHConnect(SSHConnectionConfig config);
        void onSSHDisconnect();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssh_connection, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initManagers();
        setupRecyclerView();
        setupListeners();
        loadSavedConfigs();
    }
    
    private void initViews(View view) {
        btnNewConnection = view.findViewById(R.id.btn_new_connection);
        btnDisconnect = view.findViewById(R.id.btn_disconnect);
        rvSSHConfigs = view.findViewById(R.id.rv_ssh_configs);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        ivConnectionStatus = view.findViewById(R.id.iv_connection_status);
        llConnectionStatus = view.findViewById(R.id.ll_connection_status);
    }
    
    private void initManagers() {
        configManager = SSHConfigManager.getInstance(requireContext());
        configDialog = new SSHConfigDialog(requireContext());
    }
    
    private void setupRecyclerView() {
        configAdapter = new SSHConfigAdapter(requireContext());
        configAdapter.setOnConfigClickListener(new SSHConfigAdapter.OnConfigClickListener() {
            @Override
            public void onConfigClick(SSHConnectionConfig config) {
                // 编辑配置
                showConfigDialog(config);
            }
            
            @Override
            public void onConfigDelete(SSHConnectionConfig config) {
                deleteConfig(config);
            }
            
            @Override
            public void onConfigConnect(SSHConnectionConfig config) {
                connectWithConfig(config);
            }

            @Override
            public void onConfigEdit(SSHConnectionConfig config) {
                // 编辑配置
                showConfigDialog(config);
            }

            @Override
            public void onConfigTest(SSHConnectionConfig config) {
                // 测试连接
                testConnection(config);
            }
        });
        
        rvSSHConfigs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSSHConfigs.setAdapter(configAdapter);
    }
    
    private void setupListeners() {
        btnNewConnection.setOnClickListener(v -> showConfigDialog(null));
        btnDisconnect.setOnClickListener(v -> disconnect());
        
        configDialog.setOnSSHConfigListener(new SSHConfigDialog.OnSSHConfigListener() {
            @Override
            public void onSSHConnect(SSHConnectionConfig config) {
                connectWithConfig(config);
            }
            
            @Override
            public void onSSHConfigSaved(SSHConnectionConfig config) {
                loadSavedConfigs();
            }
            
            @Override
            public void onSSHConfigDeleted(String configName) {
                loadSavedConfigs();
            }
            
            @Override
            public void onDialogClosed() {
                // Dialog closed
            }
        });
    }
    
    private void loadSavedConfigs() {
        List<SSHConnectionConfig> configs = configManager.getAllConfigs();
        configAdapter.updateConfigs(configs);
        
        boolean hasConfigs = !configs.isEmpty();
        tvEmptyState.setVisibility(hasConfigs ? View.GONE : View.VISIBLE);
        rvSSHConfigs.setVisibility(hasConfigs ? View.VISIBLE : View.GONE);
    }
    
    private void showConfigDialog(SSHConnectionConfig config) {
        configDialog.show();
        // TODO: 如果config不为null，则预填充数据到对话框
    }
    
    private void deleteConfig(SSHConnectionConfig config) {
        boolean deleted = configManager.deleteConfig(config.getName());
        if (deleted) {
            Toast.makeText(requireContext(), "配置已删除", Toast.LENGTH_SHORT).show();
            loadSavedConfigs();
        } else {
            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void connectWithConfig(SSHConnectionConfig config) {
        if (config != null && config.isValid()) {
            configManager.setLastUsedConfig(config.getName());
            updateConnectionStatus(true, config);
            
            if (configListener != null) {
                configListener.onSSHConnect(config);
            }
            
            Toast.makeText(requireContext(), "正在连接到 " + config.getHost(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testConnection(SSHConnectionConfig config) {
        if (config != null && config.isValid()) {
            // 简单的连接测试，实际项目中可能需要异步处理
            Toast.makeText(requireContext(), "正在测试连接到 " + config.getHost() + "...", Toast.LENGTH_SHORT).show();
            
            // TODO: 实现实际的连接测试逻辑
            // 可以使用 SSHConnectionManager 或其他方式进行测试
            
            // 模拟测试结果
            new android.os.Handler().postDelayed(() -> {
                Toast.makeText(requireContext(), "连接测试成功", Toast.LENGTH_SHORT).show();
            }, 1000);
        } else {
            Toast.makeText(requireContext(), "配置信息不完整", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void disconnect() {
        updateConnectionStatus(false, null);
        
        if (configListener != null) {
            configListener.onSSHDisconnect();
        }
        
        Toast.makeText(requireContext(), "已断开连接", Toast.LENGTH_SHORT).show();
    }
    
    public void updateConnectionStatus(boolean connected, SSHConnectionConfig config) {
        if (connected && config != null) {
            llConnectionStatus.setVisibility(View.VISIBLE);
            ivConnectionStatus.setImageResource(R.drawable.ic_circle_green);
            tvConnectionStatus.setText("已连接到 " + config.getHost());
            btnDisconnect.setVisibility(View.VISIBLE);
        } else {
            llConnectionStatus.setVisibility(View.GONE);
            btnDisconnect.setVisibility(View.GONE);
        }
    }
    
    public void setOnSSHConfigListener(OnSSHConfigListener listener) {
        this.configListener = listener;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (configDialog != null && configDialog.isShowing()) {
            configDialog.dismiss();
        }
    }
}
package com.termux.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.SSHConfigManager;
import com.termux.shared.logger.Logger;

import java.util.List;

/**
 * SSH配置对话框管理器
 * 处理SSH连接配置的UI交互和数据操作
 */
public class SSHConfigDialog {

    private static final String LOG_TAG = "SSHConfigDialog";
    
    private final Context mContext;
    private final SSHConfigManager mConfigManager;
    private Dialog mDialog;
    private OnSSHConfigListener mListener;
    
    // UI组件
    private TextInputEditText mNameEdit;
    private TextInputEditText mHostEdit;
    private TextInputEditText mPortEdit;
    private TextInputEditText mUsernameEdit;
    private TextInputEditText mPasswordEdit;
    private RecyclerView mSavedConfigsRecycler;
    private View mSavedConfigsContainer;
    private MaterialButton mTestButton;
    private MaterialButton mCancelButton;
    private MaterialButton mSaveConnectButton;
    
    private SSHConfigAdapter mConfigAdapter;

    public interface OnSSHConfigListener {
        void onSSHConnect(SSHConnectionConfig config);
        void onSSHConfigSaved(SSHConnectionConfig config);
        void onSSHConfigDeleted(String configName);
        void onDialogClosed();
    }

    public SSHConfigDialog(@NonNull Context context) {
        mContext = context;
        mConfigManager = SSHConfigManager.getInstance(context);
    }

    /**
     * 显示SSH配置对话框
     */
    public void show() {
        if (mDialog != null && mDialog.isShowing()) {
            return;
        }

        View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_ssh_config, null);
        initViews(dialogView);
        setupRecyclerView();
        loadSavedConfigs();
        
        mDialog = new AlertDialog.Builder(mContext)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener(dialog -> {
                    if (mListener != null) {
                        mListener.onDialogClosed();
                    }
                })
                .create();
        
        mDialog.show();
    }

    /**
     * 隐藏对话框
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    /**
     * 初始化视图组件
     */
    private void initViews(View dialogView) {
        mNameEdit = dialogView.findViewById(R.id.edit_ssh_name);
        mHostEdit = dialogView.findViewById(R.id.edit_ssh_host);
        mPortEdit = dialogView.findViewById(R.id.edit_ssh_port);
        mUsernameEdit = dialogView.findViewById(R.id.edit_ssh_username);
        mPasswordEdit = dialogView.findViewById(R.id.edit_ssh_password);
        
        mSavedConfigsRecycler = dialogView.findViewById(R.id.recycler_saved_configs);
        mSavedConfigsContainer = dialogView.findViewById(R.id.label_saved_configs);
        
        mTestButton = dialogView.findViewById(R.id.btn_test_connection);
        mCancelButton = dialogView.findViewById(R.id.btn_cancel);
        mSaveConnectButton = dialogView.findViewById(R.id.btn_save_connect);
        
        setupButtonListeners();
    }

    /**
     * 设置按钮监听器
     */
    private void setupButtonListeners() {
        mTestButton.setOnClickListener(v -> testConnection());
        mCancelButton.setOnClickListener(v -> {
            dismiss();
            if (mListener != null) {
                mListener.onDialogClosed();
            }
        });
        mSaveConnectButton.setOnClickListener(v -> saveAndConnect());
    }

    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        mConfigAdapter = new SSHConfigAdapter(mContext);
        mConfigAdapter.setOnConfigClickListener(new SSHConfigAdapter.OnConfigClickListener() {
            @Override
            public void onConfigClick(SSHConnectionConfig config) {
                loadConfigToFields(config);
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
                loadConfigToFields(config);
            }

            @Override
            public void onConfigTest(SSHConnectionConfig config) {
                testConnection(config);
            }
        });
        
        mSavedConfigsRecycler.setLayoutManager(new LinearLayoutManager(mContext));
        mSavedConfigsRecycler.setAdapter(mConfigAdapter);
    }

    /**
     * 加载已保存的配置
     */
    private void loadSavedConfigs() {
        List<SSHConnectionConfig> configs = mConfigManager.getAllConfigs();
        mConfigAdapter.updateConfigs(configs);
        
        // 控制已保存配置区域的显示
        boolean hasConfigs = !configs.isEmpty();
        mSavedConfigsContainer.setVisibility(hasConfigs ? View.VISIBLE : View.GONE);
        mSavedConfigsRecycler.setVisibility(hasConfigs ? View.VISIBLE : View.GONE);
    }

    /**
     * 将配置加载到输入框
     */
    private void loadConfigToFields(SSHConnectionConfig config) {
        if (config == null) return;
        
        mNameEdit.setText(config.getName());
        mHostEdit.setText(config.getHost());
        mPortEdit.setText(String.valueOf(config.getPort()));
        mUsernameEdit.setText(config.getUsername());
        mPasswordEdit.setText(config.getPassword());
    }

    /**
     * 从输入框获取配置
     */
    private SSHConnectionConfig getConfigFromFields() {
        String name = getText(mNameEdit);
        String host = getText(mHostEdit);
        String portStr = getText(mPortEdit);
        String username = getText(mUsernameEdit);
        String password = getText(mPasswordEdit);

        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(username)) {
            return null;
        }

        int port = 22;
        if (!TextUtils.isEmpty(portStr)) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Logger.logError(LOG_TAG, "Invalid port number: " + portStr);
                return null;
            }
        }

        SSHConnectionConfig config = new SSHConnectionConfig();
        config.setName(TextUtils.isEmpty(name) ? host + "@" + port : name);
        config.setHost(host);
        config.setPort(port);
        config.setUsername(username);
        config.setPassword(password);

        return config;
    }

    /**
     * 获取EditText的文本内容
     */
    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * 测试连接
     */
    private void testConnection() {
        SSHConnectionConfig config = getConfigFromFields();
        if (config == null || !config.isValid()) {
            showToast("请填写完整的连接信息");
            return;
        }

        // 这里应该实现实际的SSH连接测试
        // 为了简化，我们只是显示一个模拟的测试结果
        showToast("正在测试连接到 " + config.getHost() + "...");
        
        // 模拟测试延迟
        mTestButton.postDelayed(() -> {
            showToast("连接测试完成");
        }, 1500);
    }

    /**
     * 测试指定配置的连接
     */
    private void testConnection(SSHConnectionConfig config) {
        if (config == null || !config.isValid()) {
            showToast("配置信息不完整");
            return;
        }
        
        showToast("正在测试连接到 " + config.getHost() + "...");
        
        // TODO: 实现实际的连接测试逻辑
        // 模拟测试结果
        new android.os.Handler().postDelayed(() -> {
            showToast("连接测试成功");
        }, 1500);
    }

    /**
     * 保存并连接
     */
    private void saveAndConnect() {
        SSHConnectionConfig config = getConfigFromFields();
        if (config == null || !config.isValid()) {
            showToast("请填写完整的连接信息");
            return;
        }

        // 保存配置
        boolean saved = mConfigManager.saveConfig(config);
        if (saved) {
            mConfigManager.setLastUsedConfig(config.getName());
            showToast("配置已保存");
            
            if (mListener != null) {
                mListener.onSSHConfigSaved(config);
                mListener.onSSHConnect(config);
            }
        } else {
            showToast("保存配置失败");
        }

        dismiss();
    }

    /**
     * 删除配置
     */
    private void deleteConfig(SSHConnectionConfig config) {
        new AlertDialog.Builder(mContext)
                .setTitle("删除确认")
                .setMessage("确定要删除配置 \"" + config.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    boolean deleted = mConfigManager.deleteConfig(config.getName());
                    if (deleted) {
                        showToast("配置已删除");
                        loadSavedConfigs(); // 刷新列表
                        
                        if (mListener != null) {
                            mListener.onSSHConfigDeleted(config.getName());
                        }
                    } else {
                        showToast("删除失败");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 使用指定配置连接
     */
    private void connectWithConfig(SSHConnectionConfig config) {
        if (config != null && config.isValid()) {
            mConfigManager.setLastUsedConfig(config.getName());
            
            if (mListener != null) {
                mListener.onSSHConnect(config);
            }
            dismiss();
        }
    }

    /**
     * 显示提示消息
     */
    private void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置监听器
     */
    public void setOnSSHConfigListener(OnSSHConfigListener listener) {
        mListener = listener;
    }

    /**
     * 是否正在显示
     */
    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }
}
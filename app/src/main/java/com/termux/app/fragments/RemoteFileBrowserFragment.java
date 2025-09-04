package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.app.adapters.RemoteFileAdapter;
import com.termux.app.models.SSHConfigManager;
import com.termux.app.utils.NetworkErrorHandler;
import com.termux.shared.logger.Logger;
import java.util.List;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 远程文件浏览Fragment
 * 实现SFTP文件浏览功能
 */
public class RemoteFileBrowserFragment extends Fragment implements RemoteFileAdapter.OnFileItemClickListener {
    private static final String LOG_TAG = "RemoteFileBrowserFragment";
    
    private RecyclerView rvFiles;
    private EditText etCurrentPath;
    private Button btnConnect, btnBack, btnRefresh;
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private ProgressBar progressLoading;
    
    private RemoteFileAdapter fileAdapter;
    private SFTPConnectionManager connectionManager;
    private SSHConfigManager configManager;
    private CompositeDisposable compositeDisposable;
    
    private String currentPath = "/";
    private boolean isConnected = false;
    private SSHConnectionConfig currentConfig;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionManager = SFTPConnectionManager.getInstance();
        configManager = SSHConfigManager.getInstance(requireContext());
        compositeDisposable = new CompositeDisposable();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote_file_browser, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        updateConnectionStatus(false, "未连接");
    }
    
    private void initViews(View view) {
        rvFiles = view.findViewById(R.id.rv_files);
        etCurrentPath = view.findViewById(R.id.et_current_path);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnBack = view.findViewById(R.id.btn_back);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        connectionStatusIcon = view.findViewById(R.id.connection_status_icon);
        connectionStatusText = view.findViewById(R.id.connection_status_text);
        progressLoading = view.findViewById(R.id.progress_loading);
    }
    
    private void setupRecyclerView() {
        fileAdapter = new RemoteFileAdapter();
        rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFiles.setAdapter(fileAdapter);
        fileAdapter.setOnFileItemClickListener(this);
    }
    
    private void setupListeners() {
        btnConnect.setOnClickListener(v -> showConnectionDialog());
        btnBack.setOnClickListener(v -> navigateBack());
        btnRefresh.setOnClickListener(v -> refreshCurrentDirectory());
        
        etCurrentPath.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigateToPath(etCurrentPath.getText().toString());
                return true;
            }
            return false;
        });
    }
    
    private void showConnectionDialog() {
        Logger.logInfo(LOG_TAG, "Showing connection dialog");
        
        List<SSHConnectionConfig> configs = configManager.getAllConfigs();
        if (configs.isEmpty()) {
            showQuickConnectDialog();
        } else {
            showSavedConfigsDialog(configs);
        }
    }
    
    private void showQuickConnectDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ssh_config, null);
        
        EditText etHost = dialogView.findViewById(R.id.edit_ssh_host);
        EditText etPort = dialogView.findViewById(R.id.edit_ssh_port);
        EditText etUsername = dialogView.findViewById(R.id.edit_ssh_username);
        EditText etPassword = dialogView.findViewById(R.id.edit_ssh_password);
        
        etPort.setText("22");
        
        new AlertDialog.Builder(requireContext())
            .setTitle("快速连接")
            .setView(dialogView)
            .setPositiveButton("连接", (dialog, which) -> {
                String host = etHost.getText().toString().trim();
                String port = etPort.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString();
                
                if (!host.isEmpty() && !username.isEmpty()) {
                    SSHConnectionConfig config = new SSHConnectionConfig();
                    config.setHost(host);
                    config.setPort(port.isEmpty() ? 22 : Integer.parseInt(port));
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setName(username + "@" + host);
                    
                    connectToServer(config);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showSavedConfigsDialog(List<SSHConnectionConfig> configs) {
        String[] configNames = new String[configs.size() + 1];
        for (int i = 0; i < configs.size(); i++) {
            configNames[i] = configs.get(i).getDisplayName();
        }
        configNames[configs.size()] = "新建连接...";
        
        new AlertDialog.Builder(requireContext())
            .setTitle("选择连接")
            .setItems(configNames, (dialog, which) -> {
                if (which < configs.size()) {
                    connectToServer(configs.get(which));
                } else {
                    showQuickConnectDialog();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void connectToServer(SSHConnectionConfig config) {
        Logger.logInfo(LOG_TAG, "Connecting to " + config.getHost());
        
        currentConfig = config;
        updateConnectionStatus(false, "连接中...");
        showLoading(true);
        
        Disposable disposable = connectionManager.connect(config)
            .subscribe(
                success -> {
                    if (success) {
                        onConnectionEstablished();
                    } else {
                        onConnectionFailed("连接失败");
                    }
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "SFTP connection failed: " + throwable.getMessage());
                    onConnectionFailed(throwable);
                }
            );
        
        compositeDisposable.add(disposable);
    }
    
    private void onConnectionEstablished() {
        Logger.logInfo(LOG_TAG, "SFTP connection established");
        
        isConnected = true;
        updateConnectionStatus(true, "已连接 - " + currentConfig.getHost());
        btnConnect.setText("断开");
        
        // 加载根目录
        navigateToPath("/");
        showLoading(false);
    }
    
    private void onConnectionFailed(Throwable error) {
        Logger.logError(LOG_TAG, "SFTP connection failed: " + error.getMessage());
        
        isConnected = false;
        updateConnectionStatus(false, "连接失败");
        btnConnect.setText("连接");
        showLoading(false);
        
        // 使用NetworkErrorHandler显示友好的错误信息
        NetworkErrorHandler.showErrorDialog(requireContext(), error, () -> {
            // 重试连接
            if (currentConfig != null) {
                connectToServer(currentConfig);
            }
        });
    }
    
    private void onConnectionFailed(String error) {
        onConnectionFailed(new Exception(error));
    }
    
    private void navigateToPath(String path) {
        if (!isConnected) {
            showErrorDialog("未连接", "请先建立SFTP连接");
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Navigating to: " + path);
        currentPath = path;
        etCurrentPath.setText(path);
        
        loadDirectoryContent(path);
        updateBackButton();
    }
    
    private void navigateBack() {
        if (!currentPath.equals("/")) {
            String parentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            if (parentPath.isEmpty()) {
                parentPath = "/";
            }
            navigateToPath(parentPath);
        }
    }
    
    private void refreshCurrentDirectory() {
        if (isConnected) {
            loadDirectoryContent(currentPath);
        }
    }
    
    private void loadDirectoryContent(String path) {
        Logger.logInfo(LOG_TAG, "Loading directory content: " + path);
        showLoading(true);
        
        Disposable disposable = connectionManager.listFiles(path)
            .subscribe(
                files -> {
                    Logger.logInfo(LOG_TAG, "Loaded " + files.size() + " files");
                    fileAdapter.updateFiles(files);
                    showLoading(false);
                },
                throwable -> {
                    Logger.logError(LOG_TAG, "Failed to load directory: " + throwable.getMessage());
                    showLoading(false);
                    
                    // 使用NetworkErrorHandler显示友好的错误信息
                    NetworkErrorHandler.showErrorDialog(requireContext(), throwable, () -> {
                        // 重试加载目录
                        loadDirectoryContent(path);
                    });
                }
            );
        
        compositeDisposable.add(disposable);
    }
    
    private void updateConnectionStatus(boolean connected, String statusText) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                connectionStatusIcon.setImageResource(connected ? 
                    android.R.drawable.presence_online : android.R.drawable.presence_offline);
                connectionStatusText.setText(statusText);
            });
        }
    }
    
    private void updateBackButton() {
        btnBack.setEnabled(!currentPath.equals("/"));
    }
    
    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        }
    }
    
    private void showErrorDialog(String title, String message) {
        if (getContext() != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
        }
    }
    
    // RemoteFileAdapter.OnFileItemClickListener 实现
    @Override
    public void onFileItemClick(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "File clicked: " + file.getName());
        
        if (file.isDirectory()) {
            navigateToPath(file.getPath());
        } else {
            showFileOptionsDialog(file);
        }
    }
    
    @Override
    public void onFileItemLongClick(RemoteFileItem file) {
        Logger.logInfo(LOG_TAG, "File long clicked: " + file.getName());
        showFileOptionsDialog(file);
    }
    
    private void showFileOptionsDialog(RemoteFileItem file) {
        String[] options;
        if (file.isDirectory()) {
            options = new String[]{"进入目录", "查看属性"};
        } else {
            options = new String[]{"下载文件", "查看属性"};
        }
        
        new AlertDialog.Builder(requireContext())
            .setTitle(file.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        if (file.isDirectory()) {
                            navigateToPath(file.getPath());
                        } else {
                            // TODO: 实现文件下载
                            showErrorDialog("提示", "文件下载功能待实现");
                        }
                        break;
                    case 1:
                        showFilePropertiesDialog(file);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showFilePropertiesDialog(RemoteFileItem file) {
        StringBuilder properties = new StringBuilder();
        properties.append("名称: ").append(file.getName()).append("\n");
        properties.append("路径: ").append(file.getPath()).append("\n");
        properties.append("类型: ").append(file.isDirectory() ? "目录" : "文件").append("\n");
        properties.append("大小: ").append(file.getSize()).append(" 字节\n");
        properties.append("权限: ").append(file.getPermissions()).append("\n");
        properties.append("修改时间: ").append(new java.util.Date(file.getLastModified()));
        
        new AlertDialog.Builder(requireContext())
            .setTitle("文件属性")
            .setMessage(properties.toString())
            .setPositiveButton("确定", null)
            .show();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        if (connectionManager != null && isConnected) {
            connectionManager.disconnect();
        }
    }
}
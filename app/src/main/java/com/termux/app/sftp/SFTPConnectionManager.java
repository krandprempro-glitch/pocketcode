package com.termux.app.sftp;

import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.shared.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.transport.kex.KeyExchange;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Provider;
import java.security.Security;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.Response;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * SFTP连接管理器
 * 负责SSH/SFTP连接的建立、维护和文件操作
 */
public class SFTPConnectionManager {
    
    private static final String LOG_TAG = "SFTPConnectionManager";
    private static SFTPConnectionManager instance;
    
    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        DISCONNECTED("已断开"),
        CONNECTING("连接中"),
        CONNECTED("已连接"),
        ERROR("连接错误"),
        RECONNECTING("重连中");
        
        private final String description;
        
        ConnectionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // SSHJ clients
    private SSHClient sshClient;
    private SFTPClient sftpClient;
    
    private ExecutorService executorService;
    private SSHConnectionConfig currentConfig;
    private boolean isConnected = false;
    private String currentWorkingDirectory = "/";
    
    // 连接状态监控
    private BehaviorSubject<ConnectionStatus> connectionStatusSubject = 
            BehaviorSubject.createDefault(ConnectionStatus.DISCONNECTED);
    
    // 自动重连设置
    private boolean autoReconnectEnabled = true;
    private int maxReconnectAttempts = 3;
    private int currentReconnectAttempts = 0;
    private long reconnectDelaySeconds = 5;
    
    private SFTPConnectionManager() {
        executorService = Executors.newFixedThreadPool(3);
    }
    
    public static synchronized SFTPConnectionManager getInstance() {
        if (instance == null) {
            instance = new SFTPConnectionManager();
        }
        return instance;
    }
    
    /**
     * 建立SFTP连接
     */
    public Single<Boolean> connect(SSHConnectionConfig config) {
        return Single.<Boolean>create(emitter -> {
            try {
                Logger.logInfo(LOG_TAG, "Connecting to " + config.getHost() + ":" + config.getPort());

                currentConfig = config;
                
                // 真实连接逻辑（SSHJ）
                // 注册独立的 BouncyCastle 提供者（不依赖系统内置 BC），确保 ECDSA/ECDH 可用
                try {
                    Provider bc = new BouncyCastleProvider();
                    Security.removeProvider(bc.getName());
                    Security.insertProviderAt(bc, 1);
                } catch (Throwable ignored) {}
                // 避免 SSHJ 强制注册系统 BC（Android 内置 BC 不完整）
                try { SecurityUtils.setRegisterBouncyCastle(false); } catch (Throwable ignored) {}
                // 使用默认配置基础上，明确剔除 ECDH（仅保留 curve25519 / diffie-hellman），并去掉 ECDSA 签名工厂
                DefaultConfig configObj = new DefaultConfig();
                try {
                    java.util.List<Factory.Named<KeyExchange>> kex =
                            new java.util.ArrayList<>(configObj.getKeyExchangeFactories());
                    java.util.Iterator<Factory.Named<KeyExchange>> it = kex.iterator();
                    while (it.hasNext()) {
                        String name = it.next().getName();
                        String lower = name != null ? name.toLowerCase(java.util.Locale.ROOT) : "";
                        if (!(lower.contains("curve25519") || lower.contains("diffie-hellman"))) {
                            it.remove();
                        }
                    }
                    configObj.setKeyExchangeFactories(kex);
                } catch (Throwable ignored) {}
                // 不再修改签名算法工厂（服务端已避免 ECDSA）
                sshClient = new SSHClient(configObj);
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
                
                // 超时设置
                sshClient.setConnectTimeout(30000);
                sshClient.setTimeout(30000);
                
                sshClient.connect(config.getHost(), config.getPort());
                
                // 简化：仅支持密码登录（后续可扩展私钥）
                if (config.getPassword() != null) {
                    sshClient.authPassword(config.getUsername(), config.getPassword());
                } else {
                    sshClient.authPublickey(config.getUsername());
                }
                
                sftpClient = sshClient.newSFTPClient();
                
                // 获取用户家目录作为初始工作目录
                try {
                    currentWorkingDirectory = sftpClient.canonicalize(".");
                } catch (Exception e) {
                    currentWorkingDirectory = "/";
                }

                isConnected = true;
                Logger.logInfo(LOG_TAG, "SFTP connection established successfully, cwd=" + currentWorkingDirectory);
                emitter.onSuccess(true);

            } catch (UnknownHostException e) {
                String errorMsg = "主机地址无效或无法解析: " + config.getHost();
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            } catch (ConnectException e) {
                String errorMsg = "无法连接到服务器，请检查主机地址和端口: " + config.getHost() + ":" + config.getPort();
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            } catch (SocketTimeoutException e) {
                String errorMsg = "连接超时，请检查网络连接和防火墙设置";
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            } catch (SecurityException e) {
                String errorMsg = "用户认证失败: " + e.getMessage();
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            } catch (IOException e) {
                String errorMsg = "网络IO错误: " + e.getMessage();
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            } catch (Exception e) {
                String errorMsg = "连接过程发生未知错误: " + e.getMessage();
                Logger.logError(LOG_TAG, errorMsg);
                isConnected = false;
                emitter.onError(new RuntimeException(errorMsg));
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 断开SFTP连接
     */
    public void disconnect() {
        try {
            if (isConnected) {
                Logger.logInfo(LOG_TAG, "Disconnecting SFTP connection");
                if (sftpClient != null) {
                    try { sftpClient.close(); } catch (Exception ignored) {}
                    sftpClient = null;
                }
                if (sshClient != null) {
                    try { sshClient.disconnect(); } catch (Exception ignored) {}
                    sshClient = null;
                }
                isConnected = false;
                currentConfig = null;
                currentWorkingDirectory = "/";
                
                Logger.logInfo(LOG_TAG, "SFTP connection disconnected");
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error during disconnect: " + e.getMessage());
        }
    }
    
    /**
     * 列出指定目录的文件
     */
    public Single<List<RemoteFileItem>> listFiles(String remotePath) {
        return Single.<List<RemoteFileItem>>create(emitter -> {
            try {
                if (!isConnected) {
                    emitter.onError(new RuntimeException("未建立SFTP连接，请先连接服务器"));
                    return;
                }
                
                // 验证路径格式
                if (remotePath == null || remotePath.trim().isEmpty()) {
                    emitter.onError(new RuntimeException("目录路径不能为空"));
                    return;
                }
                
                Logger.logInfo(LOG_TAG, "Listing files in directory: " + remotePath);
                
                List<RemoteFileItem> files = new ArrayList<>();
                try {
                    for (RemoteResourceInfo res : sftpClient.ls(remotePath)) {
                        String name = res.getName();
                        if (".".equals(name) || "..".equals(name)) continue;
                        // Skip hidden files and directories (names starting with '.')
                        if (name != null && name.startsWith(".")) continue;

                        RemoteFileItem item = new RemoteFileItem();
                        item.setName(name);
                        item.setPath(normalizePath(remotePath, name));
                        FileMode.Type type = res.getAttributes().getType();
                        item.setDirectory(type == FileMode.Type.DIRECTORY);
                        Long size = res.getAttributes().getSize();
                        if (size != null) item.setSize(size);
                        Long mtime = res.getAttributes().getMtime();
                        if (mtime != null) item.setLastModified(mtime);
                        item.setPermissions(String.valueOf(res.getAttributes().getMode().getPermissionsMask()));
                        files.add(item);
                    }
                } catch (SFTPException e) {
                    Response.StatusCode code = e.getStatusCode();
                    if (code == Response.StatusCode.NO_SUCH_FILE) {
                        throw new RuntimeException("目录不存在: " + remotePath);
                    } else if (code == Response.StatusCode.PERMISSION_DENIED) {
                        throw new RuntimeException("权限不足，无法访问目录: " + remotePath);
                    } else {
                        throw new RuntimeException("无法读取目录内容: " + e.getMessage());
                    }
                }
                
                Logger.logInfo(LOG_TAG, "Found " + files.size() + " items in directory: " + remotePath);
                emitter.onSuccess(files);

            } catch (SecurityException e) {
                String errorMsg = "权限不足，无法访问目录: " + remotePath;
                Logger.logError(LOG_TAG, errorMsg);
                emitter.onError(new RuntimeException(errorMsg));
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                    errorMsg = "读取目录时发生未知错误: " + remotePath;
                }
                Logger.logError(LOG_TAG, "Failed to list files in " + remotePath + ": " + errorMsg);
                emitter.onError(new RuntimeException(errorMsg));
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 读取文件内容
     */
    public Single<String> readFileContent(String remoteFilePath) {
        return Single.<String>create(emitter -> {
            try {
                if (!isConnected) {
                    throw new IllegalStateException("SFTP connection is not established");
                }
                
                Logger.logInfo(LOG_TAG, "Reading file content: " + remoteFilePath);
                
                try (RemoteFile rf = sftpClient.open(remoteFilePath, EnumSet.of(OpenMode.READ))) {
                    try (InputStream in = rf.new RemoteFileInputStream(0)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            baos.write(buf, 0, n);
                        }
                        String content = baos.toString(StandardCharsets.UTF_8.name());
                        Logger.logInfo(LOG_TAG, "File content read successfully: " + remoteFilePath + ", bytes=" + baos.size());
                        emitter.onSuccess(content);
                    }
                }
                
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Failed to read file content: " + remoteFilePath + " - " + e.getMessage());
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 读取二进制文件字节（用于图片等非文本内容）
     */
    public Single<byte[]> readFileBytes(String remoteFilePath) {
        return Single.<byte[]>create(emitter -> {
            try {
                if (!isConnected) {
                    throw new IllegalStateException("SFTP connection is not established");
                }

                Logger.logInfo(LOG_TAG, "Reading file bytes: " + remoteFilePath);

                try (RemoteFile rf = sftpClient.open(remoteFilePath, EnumSet.of(OpenMode.READ))) {
                    try (InputStream in = rf.new RemoteFileInputStream(0)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            baos.write(buf, 0, n);
                        }
                        byte[] content = baos.toByteArray();
                        Logger.logInfo(LOG_TAG, "File bytes read successfully: " + remoteFilePath + ", bytes=" + content.length);
                        emitter.onSuccess(content);
                    }
                }

            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Failed to read file bytes: " + remoteFilePath + " - " + e.getMessage());
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 下载文件到本地
     */
    public Single<Boolean> downloadFile(String remotePath, String localPath) {
        return Single.<Boolean>create(emitter -> {
            try {
                if (!isConnected) {
                    throw new IllegalStateException("SFTP connection is not established");
                }
                
                Logger.logInfo(LOG_TAG, "Downloading file from " + remotePath + " to " + localPath);
                
                // TODO: 实现实际的文件下载逻辑
                /*
                sftpClient.get(remotePath, localPath);
                */
                
                // 临时模拟下载过程
                Thread.sleep(1000);
                
                Logger.logInfo(LOG_TAG, "File downloaded successfully: " + remotePath);
                emitter.onSuccess(true);
                
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Failed to download file: " + remotePath + " - " + e.getMessage());
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 获取当前连接配置
     */
    public SSHConnectionConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * 获取当前工作目录
     */
    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectory;
    }
    
    /**
     * 设置当前工作目录
     */
    public void setCurrentWorkingDirectory(String directory) {
        this.currentWorkingDirectory = directory;
    }
    
    /**
     * 规范化路径
     */
    private String normalizePath(String parentPath, String childName) {
        if (parentPath.equals("/")) {
            return "/" + childName;
        } else {
            return parentPath + "/" + childName;
        }
    }
    
    /**
     * 获取文件信息
     */
    public Single<RemoteFileItem> getFileInfo(String remotePath) {
        return Single.<RemoteFileItem>create(emitter -> {
            try {
                if (!isConnected) {
                    throw new IllegalStateException("SFTP connection is not established");
                }
                
                Logger.logInfo(LOG_TAG, "Getting file info: " + remotePath);
                
                // TODO: 实现实际的文件信息获取
                /*
                FileAttributes attrs = sftpClient.stat(remotePath);
                RemoteFileItem fileInfo = new RemoteFileItem();
                // 设置文件属性...
                */
                
                // 临时模拟文件信息
                Thread.sleep(200);
                String fileName = remotePath.substring(remotePath.lastIndexOf('/') + 1);
                RemoteFileItem fileInfo = new RemoteFileItem(fileName, remotePath, false);
                fileInfo.setSize(1024);
                fileInfo.setLastModified(System.currentTimeMillis() / 1000);
                fileInfo.setPermissions("644");
                
                emitter.onSuccess(fileInfo);
                
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Failed to get file info: " + remotePath + " - " + e.getMessage());
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 获取连接状态监控Observable
     */
    public Observable<ConnectionStatus> getConnectionStatus() {
        return connectionStatusSubject;
    }
    
    /**
     * 更新连接状态
     */
    private void updateConnectionStatus(ConnectionStatus status) {
        Logger.logDebug(LOG_TAG, "Connection status updated: " + status.getDescription());
        connectionStatusSubject.onNext(status);
    }
    
    /**
     * 带状态监控的连接方法
     */
    public Single<Boolean> connectWithStatusMonitoring(SSHConnectionConfig config) {
        updateConnectionStatus(ConnectionStatus.CONNECTING);
        currentReconnectAttempts = 0;
        
        return connect(config)
                .doOnSuccess(success -> {
                    updateConnectionStatus(ConnectionStatus.CONNECTED);
                    currentReconnectAttempts = 0;
                })
                .doOnError(error -> {
                    updateConnectionStatus(ConnectionStatus.ERROR);
                    if (autoReconnectEnabled && currentReconnectAttempts < maxReconnectAttempts) {
                        scheduleReconnect(config);
                    }
                });
    }
    
    /**
     * 安排自动重连
     */
    private void scheduleReconnect(SSHConnectionConfig config) {
        if (currentReconnectAttempts >= maxReconnectAttempts) {
            Logger.logInfo(LOG_TAG, "Max reconnect attempts reached, giving up");
            return;
        }
        
        currentReconnectAttempts++;
        Logger.logInfo(LOG_TAG, "Scheduling reconnect attempt " + currentReconnectAttempts + "/" + maxReconnectAttempts);
        
        updateConnectionStatus(ConnectionStatus.RECONNECTING);
        
        Observable.timer(reconnectDelaySeconds, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    Logger.logInfo(LOG_TAG, "Attempting automatic reconnection...");
                    connectWithStatusMonitoring(config).subscribe(
                            success -> Logger.logInfo(LOG_TAG, "Automatic reconnection successful"),
                            error -> Logger.logError(LOG_TAG, "Automatic reconnection failed: " + error.getMessage())
                    );
                });
    }
    
    /**
     * 手动断开连接
     */
    public void disconnectManually() {
        autoReconnectEnabled = false;
        disconnect();
        updateConnectionStatus(ConnectionStatus.DISCONNECTED);
    }
    
    /**
     * 启用自动重连
     */
    public void enableAutoReconnect(boolean enabled) {
        this.autoReconnectEnabled = enabled;
        Logger.logInfo(LOG_TAG, "Auto-reconnect " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * 设置最大重连尝试次数
     */
    public void setMaxReconnectAttempts(int maxAttempts) {
        this.maxReconnectAttempts = Math.max(0, maxAttempts);
        Logger.logInfo(LOG_TAG, "Max reconnect attempts set to: " + this.maxReconnectAttempts);
    }
    
    /**
     * 设置重连延迟时间（秒）
     */
    public void setReconnectDelay(long delaySeconds) {
        this.reconnectDelaySeconds = Math.max(1, delaySeconds);
        Logger.logInfo(LOG_TAG, "Reconnect delay set to: " + this.reconnectDelaySeconds + " seconds");
    }
    
    /**
     * 获取当前重连尝试次数
     */
    public int getCurrentReconnectAttempts() {
        return currentReconnectAttempts;
    }
    
    /**
     * 获取最大重连尝试次数
     */
    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }
    
    /**
     * 是否启用自动重连
     */
    public boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled;
    }
    
    /**
     * 测试连接状态（心跳检测）
     */
    public Single<Boolean> testConnection() {
        return Single.<Boolean>create(emitter -> {
            if (!isConnected) {
                emitter.onError(new RuntimeException("连接未建立"));
                return;
            }
            
            try {
                // 简单心跳：列目录
                sftpClient.ls(".");
                emitter.onSuccess(true);
                
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Connection test failed: " + e.getMessage());
                isConnected = false;
                updateConnectionStatus(ConnectionStatus.ERROR);
                emitter.onError(new RuntimeException("连接已断开"));
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 启动定期心跳检测
     */
    public void startHeartbeat(long intervalSeconds) {
        Observable.interval(intervalSeconds, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    if (isConnected && currentConfig != null) {
                        testConnection().subscribe(
                                success -> {
                                    // 连接正常，无需处理
                                },
                                error -> {
                                    Logger.logError(LOG_TAG, "Heartbeat failed, attempting reconnect");
                                    if (autoReconnectEnabled) {
                                        scheduleReconnect(currentConfig);
                                    }
                                }
                        );
                    }
                });
    }
    
    /**
     * 关闭连接管理器
     */
    public void shutdown() {
        autoReconnectEnabled = false;
        disconnect();
        updateConnectionStatus(ConnectionStatus.DISCONNECTED);
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (connectionStatusSubject != null) {
            connectionStatusSubject.onComplete();
        }
    }
}

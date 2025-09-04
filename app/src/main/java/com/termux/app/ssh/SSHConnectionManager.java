package com.termux.app.ssh;

import android.content.Context;
import android.text.TextUtils;

import com.termux.app.models.SSHConnectionConfig;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalSession;

/**
 * SSH信息管理器
 * 负责保存SSH配置信息和生成连接命令
 */
public class SSHConnectionManager {
    
    private static final String LOG_TAG = "SSHConnectionManager";
    private final Context mContext;
    
    public interface SSHConnectionCallback {
        void onCommandGenerated(String sshCommand);
        void onConfigSaved(SSHConnectionConfig config);
        void onError(String error);
    }
    
    public SSHConnectionManager(Context context) {
        mContext = context;
    }
    
    /**
     * 保存SSH配置并生成连接命令
     * @param config SSH配置
     * @param callback 回调接口
     */
    public void saveConfigAndGenerateCommand(SSHConnectionConfig config, SSHConnectionCallback callback) {
        if (config == null || !config.isValid()) {
            if (callback != null) {
                callback.onError("SSH配置信息无效");
            }
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Saving SSH config for " + config.getHost());
        
        // 生成SSH连接命令
        String sshCommand = generateSSHCommand(config);
        if (TextUtils.isEmpty(sshCommand)) {
            if (callback != null) {
                callback.onError("生成SSH命令失败");
            }
            return;
        }
        
        Logger.logInfo(LOG_TAG, "Generated SSH command: " + sshCommand);
        
        if (callback != null) {
            callback.onConfigSaved(config);
            callback.onCommandGenerated(sshCommand);
        }
    }
    
    /**
     * 生成SSH连接命令
     * @param config SSH配置
     * @return SSH命令字符串
     */
    public static String generateSSHCommand(SSHConnectionConfig config) {
        if (config == null || !config.isValid()) {
            return null;
        }
        
        StringBuilder command = new StringBuilder();
        command.append("ssh ");
        
        // 添加一些常用选项
        command.append("-o StrictHostKeyChecking=no ");
        command.append("-o UserKnownHostsFile=/dev/null ");
        
        // 添加端口参数（如果不是默认端口）
        if (config.getPort() != 22) {
            command.append("-p ").append(config.getPort()).append(" ");
        }
        
        // 添加用户名和主机
        command.append(config.getUsername()).append("@").append(config.getHost());
        
        return command.toString();
    }
    
    /**
     * 发送命令到终端
     * @param command 要发送的命令
     * @param session 终端会话
     */
    public void sendCommandToTerminal(String command, TerminalSession session) {
        if (TextUtils.isEmpty(command) || session == null) {
            return;
        }
        
        try {
            byte[] commandBytes = (command + "\r").getBytes();
            session.write(commandBytes, 0, commandBytes.length);
            Logger.logInfo(LOG_TAG, "Command sent to terminal: " + command);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to send command to terminal: " + e.getMessage());
        }
    }
}
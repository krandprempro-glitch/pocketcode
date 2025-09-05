package com.termux.app.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import com.termux.shared.logger.Logger;

/**
 * 错误对话框帮助类
 * 提供用户友好的错误提示和处理建议
 */
public class ErrorDialogHelper {
    
    private static final String LOG_TAG = "ErrorDialogHelper";
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(Context context, String title, String message, 
                                     DialogInterface.OnClickListener retryListener) {
        if (context == null) {
            Logger.logError(LOG_TAG, "Context is null, cannot show error dialog");
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("确定", null);
        
        // 如果提供了重试监听器，添加重试按钮
        if (retryListener != null) {
            builder.setNegativeButton("重试", retryListener);
        }
        
        try {
            builder.show();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to show error dialog: " + e.getMessage());
            // 回退到Toast提示
            Toast.makeText(context, title + ": " + message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 显示简单错误对话框（无重试按钮）
     */
    public static void showErrorDialog(Context context, String title, String message) {
        showErrorDialog(context, title, message, null);
    }
    
    /**
     * 根据错误类型显示相应的错误对话框
     */
    public static void handleError(Context context, Throwable error, 
                                 DialogInterface.OnClickListener retryListener) {
        if (context == null || error == null) {
            Logger.logError(LOG_TAG, "Context or error is null");
            return;
        }
        
        String errorMessage = error.getMessage();
        String title;
        String message;
        
        if (errorMessage == null) {
            errorMessage = "未知错误";
        }
        
        // 根据错误信息分类处理
        if (errorMessage.contains("主机地址无效") || errorMessage.contains("无法解析")) {
            title = "网络连接错误";
            message = "无法解析服务器地址，请检查：\n" +
                     "• 主机地址是否正确\n" +
                     "• 网络连接是否正常\n" +
                     "• DNS设置是否正确";
        } else if (errorMessage.contains("无法连接到服务器") || errorMessage.contains("连接被拒绝")) {
            title = "连接失败";
            message = "无法连接到服务器，请检查：\n" +
                     "• 服务器地址和端口是否正确\n" +
                     "• 服务器是否正在运行\n" +
                     "• 防火墙是否阻止了连接\n" +
                     "• 网络连接是否正常";
        } else if (errorMessage.contains("连接超时")) {
            title = "连接超时";
            message = "连接服务器超时，请检查：\n" +
                     "• 网络连接是否稳定\n" +
                     "• 服务器响应是否正常\n" +
                     "• 防火墙设置是否正确\n" +
                     "• 尝试稍后重新连接";
        } else if (errorMessage.contains("用户认证失败")) {
            title = "认证失败";
            message = "用户名或密码错误，请检查：\n" +
                     "• 用户名是否正确\n" +
                     "• 密码是否正确\n" +
                     "• 账户是否被锁定\n" +
                     "• 是否需要使用SSH密钥认证";
        } else if (errorMessage.contains("权限不足")) {
            title = "权限错误";
            message = "没有足够权限执行此操作，请检查：\n" +
                     "• 当前用户是否有相应权限\n" +
                     "• 目录或文件的访问权限设置\n" +
                     "• 是否需要管理员权限";
        } else if (errorMessage.contains("目录不存在")) {
            title = "路径错误";
            message = "指定的目录不存在，请检查：\n" +
                     "• 目录路径是否正确\n" +
                     "• 目录是否已被删除或移动\n" +
                     "• 大小写是否匹配";
        } else if (errorMessage.contains("网络错误") || errorMessage.contains("网络IO错误")) {
            title = "网络错误";
            message = "网络通信出现问题，请检查：\n" +
                     "• 网络连接是否稳定\n" +
                     "• 服务器是否正常运行\n" +
                     "• 尝试重新连接";
        } else if (errorMessage.contains("未建立SFTP连接")) {
            title = "连接状态错误";
            message = "尚未建立SFTP连接，请先连接到服务器";
        } else {
            title = "操作失败";
            message = "操作过程中发生错误：\n" + errorMessage + 
                     "\n\n如果问题持续存在，请检查网络连接或联系管理员";
        }
        
        showErrorDialog(context, title, message, retryListener);
    }
    
    /**
     * 显示简单的错误处理（无重试按钮）
     */
    public static void handleError(Context context, Throwable error) {
        handleError(context, error, null);
    }
    
    /**
     * 显示网络连接错误的专门对话框
     */
    public static void showConnectionError(Context context, String host, int port,
                                         DialogInterface.OnClickListener retryListener) {
        String title = "连接失败";
        String message = String.format("无法连接到服务器 %s:%d\n\n请检查：\n" +
                                      "• 服务器地址和端口是否正确\n" +
                                      "• 网络连接是否正常\n" +
                                      "• 服务器是否正在运行\n" +
                                      "• 防火墙设置是否正确", host, port);
        
        showErrorDialog(context, title, message, retryListener);
    }
    
    /**
     * 显示认证错误的专门对话框
     */
    public static void showAuthenticationError(Context context, String username,
                                             DialogInterface.OnClickListener retryListener) {
        String title = "认证失败";
        String message = String.format("用户 '%s' 认证失败\n\n可能的原因：\n" +
                                      "• 用户名或密码不正确\n" +
                                      "• 账户被锁定或禁用\n" +
                                      "• 需要使用SSH密钥认证\n" +
                                      "• 服务器认证策略发生变化", username);
        
        showErrorDialog(context, title, message, retryListener);
    }
    
    /**
     * 显示权限错误的专门对话框
     */
    public static void showPermissionError(Context context, String path) {
        String title = "权限不足";
        String message = String.format("无法访问路径：%s\n\n请检查：\n" +
                                      "• 当前用户是否有访问权限\n" +
                                      "• 文件或目录的权限设置\n" +
                                      "• 是否需要提升权限", path);
        
        showErrorDialog(context, title, message);
    }
    
    /**
     * 显示操作成功的Toast提示
     */
    public static void showSuccessToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示警告Toast提示
     */
    public static void showWarningToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, "警告: " + message, Toast.LENGTH_LONG).show();
        }
    }
}

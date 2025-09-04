package com.termux.app.utils;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.termux.shared.logger.Logger;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * 网络错误处理工具类
 * 提供统一的网络错误分类和用户友好的错误提示
 */
public class NetworkErrorHandler {
    
    private static final String LOG_TAG = "NetworkErrorHandler";
    
    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NETWORK_UNAVAILABLE,
        CONNECTION_TIMEOUT,
        HOST_UNREACHABLE,
        AUTHENTICATION_FAILED,
        PERMISSION_DENIED,
        FILE_NOT_FOUND,
        CONNECTION_LOST,
        SERVER_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * 错误信息类
     */
    public static class ErrorInfo {
        private ErrorType type;
        private String title;
        private String message;
        private String technicalDetails;
        private boolean isRetryable;
        
        public ErrorInfo(ErrorType type, String title, String message, String technicalDetails, boolean isRetryable) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.technicalDetails = technicalDetails;
            this.isRetryable = isRetryable;
        }
        
        // Getters
        public ErrorType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getTechnicalDetails() { return technicalDetails; }
        public boolean isRetryable() { return isRetryable; }
    }
    
    /**
     * 分析异常并返回错误信息
     */
    public static ErrorInfo analyzeError(Throwable throwable) {
        Logger.logError(LOG_TAG, "Analyzing error: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
        
        String message = throwable.getMessage() != null ? throwable.getMessage() : "";
        
        // 网络连接错误
        if (throwable instanceof UnknownHostException) {
            return new ErrorInfo(
                ErrorType.HOST_UNREACHABLE,
                "无法连接到服务器",
                "请检查服务器地址是否正确，或者网络连接是否正常。",
                "DNS解析失败: " + message,
                true
            );
        }
        
        // 连接超时
        if (throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException) {
            return new ErrorInfo(
                ErrorType.CONNECTION_TIMEOUT,
                "连接超时",
                "服务器响应超时，请检查网络连接或稍后重试。",
                "连接超时: " + message,
                true
            );
        }
        
        // 连接拒绝
        if (throwable instanceof ConnectException) {
            if (message.contains("Connection refused")) {
                return new ErrorInfo(
                    ErrorType.HOST_UNREACHABLE,
                    "连接被拒绝",
                    "无法连接到服务器，请检查服务器是否运行或端口是否正确。",
                    "连接被拒绝: " + message,
                    true
                );
            }
        }
        
        // 网络不可达
        if (throwable instanceof NoRouteToHostException) {
            return new ErrorInfo(
                ErrorType.NETWORK_UNAVAILABLE,
                "网络不可达",
                "无法到达目标服务器，请检查网络连接。",
                "网络路由错误: " + message,
                true
            );
        }
        
        // SSH认证失败
        if (message.toLowerCase().contains("auth") || message.toLowerCase().contains("authentication")) {
            return new ErrorInfo(
                ErrorType.AUTHENTICATION_FAILED,
                "身份验证失败",
                "用户名或密码错误，请检查登录凭据。",
                "认证失败: " + message,
                true
            );
        }
        
        // 权限拒绝
        if (message.toLowerCase().contains("permission denied") || message.toLowerCase().contains("access denied")) {
            return new ErrorInfo(
                ErrorType.PERMISSION_DENIED,
                "权限不足",
                "没有足够的权限执行此操作，请检查用户权限。",
                "权限拒绝: " + message,
                false
            );
        }
        
        // 文件不存在
        if (message.toLowerCase().contains("no such file") || message.toLowerCase().contains("file not found")) {
            return new ErrorInfo(
                ErrorType.FILE_NOT_FOUND,
                "文件不存在",
                "指定的文件或目录不存在。",
                "文件未找到: " + message,
                false
            );
        }
        
        // 连接丢失
        if (message.toLowerCase().contains("connection lost") || message.toLowerCase().contains("broken pipe")) {
            return new ErrorInfo(
                ErrorType.CONNECTION_LOST,
                "连接中断",
                "与服务器的连接已中断，请重新连接。",
                "连接丢失: " + message,
                true
            );
        }
        
        // 服务器错误
        if (message.toLowerCase().contains("server error") || message.toLowerCase().contains("internal error")) {
            return new ErrorInfo(
                ErrorType.SERVER_ERROR,
                "服务器错误",
                "服务器内部发生错误，请联系管理员或稍后重试。",
                "服务器错误: " + message,
                true
            );
        }
        
        // 未知错误
        return new ErrorInfo(
            ErrorType.UNKNOWN_ERROR,
            "未知错误",
            "发生了未知错误，请查看详细信息或联系技术支持。",
            throwable.getClass().getSimpleName() + ": " + message,
            true
        );
    }
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(Context context, Throwable throwable, Runnable retryAction) {
        ErrorInfo errorInfo = analyzeError(throwable);
        showErrorDialog(context, errorInfo, retryAction);
    }
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(Context context, ErrorInfo errorInfo, Runnable retryAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(errorInfo.getTitle())
            .setMessage(errorInfo.getMessage())
            .setPositiveButton("确定", null);
        
        // 如果错误可重试，添加重试按钮
        if (errorInfo.isRetryable() && retryAction != null) {
            builder.setNegativeButton("重试", (dialog, which) -> {
                Logger.logInfo(LOG_TAG, "User requested retry for error: " + errorInfo.getType());
                retryAction.run();
            });
        }
        
        // 添加查看详情按钮
        builder.setNeutralButton("详情", (dialog, which) -> {
            showTechnicalDetailsDialog(context, errorInfo);
        });
        
        builder.show();
    }
    
    /**
     * 显示技术详情对话框
     */
    private static void showTechnicalDetailsDialog(Context context, ErrorInfo errorInfo) {
        new AlertDialog.Builder(context)
            .setTitle("错误详情")
            .setMessage("错误类型: " + errorInfo.getType() + "\n\n" +
                       "技术详情: " + errorInfo.getTechnicalDetails())
            .setPositiveButton("确定", null)
            .show();
    }
    
    /**
     * 检查是否为网络相关错误
     */
    public static boolean isNetworkError(Throwable throwable) {
        return throwable instanceof UnknownHostException ||
               throwable instanceof SocketTimeoutException ||
               throwable instanceof ConnectException ||
               throwable instanceof NoRouteToHostException ||
               throwable instanceof TimeoutException;
    }
    
    /**
     * 检查是否为可重试错误
     */
    public static boolean isRetryableError(Throwable throwable) {
        ErrorInfo errorInfo = analyzeError(throwable);
        return errorInfo.isRetryable();
    }
    
    /**
     * 获取简化的错误消息
     */
    public static String getSimpleErrorMessage(Throwable throwable) {
        ErrorInfo errorInfo = analyzeError(throwable);
        return errorInfo.getMessage();
    }
}
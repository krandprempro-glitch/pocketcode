package com.termux.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.termux.shared.logger.Logger;

/**
 * 权限检查和处理工具类
 * 处理文件下载、存储访问等权限需求
 */
public class PermissionHelper {
    
    private static final String LOG_TAG = "PermissionHelper";
    
    // 权限请求码
    public static final int REQUEST_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_ALL_FILES_ACCESS = 1002;
    
    /**
     * 权限检查回调接口
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied(String reason);
        void onPermissionExplanationNeeded(String explanation);
    }
    
    /**
     * 检查存储权限
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用管理所有文件权限
            return Environment.isExternalStorageManager();
        } else {
            // Android 10及以下使用传统存储权限
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * 检查读取存储权限
     */
    public static boolean hasReadStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * 请求存储权限（用于文件下载）
     */
    public static void requestStoragePermission(Activity activity, PermissionCallback callback) {
        if (hasStoragePermission(activity)) {
            callback.onPermissionGranted();
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要请求管理所有文件权限
            requestAllFilesAccessPermission(activity, callback);
        } else {
            // Android 10及以下请求传统存储权限
            requestLegacyStoragePermission(activity, callback);
        }
    }
    
    /**
     * 请求传统存储权限（Android 10及以下）
     */
    private static void requestLegacyStoragePermission(Activity activity, PermissionCallback callback) {
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        // 检查是否需要显示权限说明
        boolean shouldShowRationale = false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
                break;
            }
        }
        
        if (shouldShowRationale) {
            String explanation = "需要存储权限来保存下载的文件。\n\n" +
                                "此权限用于：\n" +
                                "• 下载远程文件到本地存储\n" +
                                "• 保存文件浏览历史记录\n" +
                                "• 缓存远程文件内容";
            callback.onPermissionExplanationNeeded(explanation);
        } else {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_STORAGE_PERMISSION);
        }
    }
    
    /**
     * 请求所有文件访问权限（Android 11+）
     */
    private static void requestAllFilesAccessPermission(Activity activity, PermissionCallback callback) {
        try {
            String explanation = "为了正常使用文件下载功能，需要授予\"所有文件访问权限\"。\n\n" +
                                "此权限用于：\n" +
                                "• 下载远程服务器上的文件\n" +
                                "• 管理下载的文件\n" +
                                "• 访问应用专用存储区域\n\n" +
                                "请在设置页面中选择\"允许访问所有文件\"";
            
            callback.onPermissionExplanationNeeded(explanation);
            
            // 注意：这里应该引导用户到设置页面，但由于需要Intent，暂时记录日志
            Logger.logInfo(LOG_TAG, "Requesting all files access permission for Android 11+");
            
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to request all files access permission: " + e.getMessage());
            callback.onPermissionDenied("无法请求文件访问权限");
        }
    }
    
    /**
     * 处理权限请求结果
     */
    public static void handlePermissionResult(int requestCode, String[] permissions, 
                                            int[] grantResults, PermissionCallback callback) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                handleStoragePermissionResult(permissions, grantResults, callback);
                break;
            case REQUEST_ALL_FILES_ACCESS:
                handleAllFilesAccessResult(grantResults, callback);
                break;
            default:
                Logger.logMessage(Log.WARN,LOG_TAG, "Unknown permission request code: " + requestCode);
                callback.onPermissionDenied("未知的权限请求");
                break;
        }
    }
    
    /**
     * 处理存储权限结果
     */
    private static void handleStoragePermissionResult(String[] permissions, int[] grantResults, 
                                                    PermissionCallback callback) {
        if (grantResults.length == 0) {
            callback.onPermissionDenied("权限请求被取消");
            return;
        }
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            Logger.logInfo(LOG_TAG, "Storage permission granted");
            callback.onPermissionGranted();
        } else {
            Logger.logMessage(Log.WARN,LOG_TAG, "Storage permission denied");
            String reason = "存储权限被拒绝，无法下载文件到本地存储。\n\n" +
                           "如需使用下载功能，请到应用设置中手动授予存储权限。";
            callback.onPermissionDenied(reason);
        }
    }
    
    /**
     * 处理所有文件访问权限结果
     */
    private static void handleAllFilesAccessResult(int[] grantResults, PermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Logger.logInfo(LOG_TAG, "All files access permission granted");
                callback.onPermissionGranted();
            } else {
                Logger.logError( LOG_TAG, "All files access permission denied");
                String reason = "文件管理权限被拒绝，无法访问外部存储。\n\n" +
                               "如需使用完整的文件管理功能，请到应用设置中授予\"所有文件访问权限\"。";
                callback.onPermissionDenied(reason);
            }
        } else {
            // 不应该到达这里，但为了安全起见
            callback.onPermissionDenied("系统版本不支持此权限模式");
        }
    }
    
    /**
     * 检查文件下载权限并处理
     */
    public static void checkDownloadPermission(Activity activity, PermissionCallback callback) {
        if (hasStoragePermission(activity)) {
            callback.onPermissionGranted();
        } else {
            requestStoragePermission(activity, callback);
        }
    }
    
    /**
     * 检查网络权限（通常在manifest中静态声明）
     */
    public static boolean hasNetworkPermission(Context context) {
        int internetPermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.INTERNET);
        int networkStatePermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_NETWORK_STATE);
        
        return internetPermission == PackageManager.PERMISSION_GRANTED && 
               networkStatePermission == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 获取下载目录路径
     */
    public static String getDownloadDirectory(Context context) {
        try {
            if (hasStoragePermission(context)) {
                // 使用公共下载目录
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            } else {
                // 使用应用私有目录
                return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to get download directory: " + e.getMessage());
            // 回退到应用内部存储
            return context.getFilesDir().getAbsolutePath() + "/downloads";
        }
    }
    
    /**
     * 检查是否可以写入指定目录
     */
    public static boolean canWriteToDirectory(String directoryPath) {
        try {
            java.io.File directory = new java.io.File(directoryPath);
            return directory.exists() && directory.canWrite();
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to check directory write permission: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建下载目录
     */
    public static boolean createDownloadDirectory(String directoryPath) {
        try {
            java.io.File directory = new java.io.File(directoryPath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                Logger.logInfo(LOG_TAG, "Download directory created: " + created + " - " + directoryPath);
                return created;
            }
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to create download directory: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查权限状态的详细信息
     */
    public static String getPermissionStatusInfo(Context context) {
        StringBuilder info = new StringBuilder();
        
        info.append("权限状态检查：\n");
        info.append("• Android版本: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("• 存储权限: ").append(hasStoragePermission(context) ? "已授予" : "未授予").append("\n");
        info.append("• 读取存储权限: ").append(hasReadStoragePermission(context) ? "已授予" : "未授予").append("\n");
        info.append("• 网络权限: ").append(hasNetworkPermission(context) ? "已授予" : "未授予").append("\n");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            info.append("• 所有文件访问权限: ").append(Environment.isExternalStorageManager() ? "已授予" : "未授予").append("\n");
        }
        
        String downloadDir = getDownloadDirectory(context);
        info.append("• 下载目录: ").append(downloadDir).append("\n");
        info.append("• 下载目录可写: ").append(canWriteToDirectory(downloadDir) ? "是" : "否").append("\n");
        
        return info.toString();
    }
}

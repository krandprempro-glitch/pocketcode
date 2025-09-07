package com.termux.app.floating.services

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.termux.shared.logger.Logger

/**
 * 悬浮窗权限管理服务
 * 处理悬浮窗权限申请和检查
 */
class FloatingPermissionService(context: Context) {
    
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        private const val LOG_TAG = "FloatingPermissionService"
    }
    
    private val context = context.applicationContext
    
    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Android 6.0以下默认有权限
        }
    }
    
    /**
     * 申请悬浮窗权限
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            showPermissionDialog(activity)
        }
    }
    
    private fun showPermissionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("需要悬浮窗权限")
            .setMessage("为了使用快捷操作功能，需要允许应用显示悬浮窗。请在设置中开启此权限。")
            .setPositiveButton("去设置") { _, _ -> 
                openOverlaySettings(activity) 
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openOverlaySettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to open overlay settings: ${e.message}")
            Toast.makeText(context, "无法打开设置页面", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 权限申请结果处理
     */
    fun onPermissionResult(requestCode: Int, listener: OnPermissionResultListener?) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            val granted = hasOverlayPermission()
            listener?.onPermissionResult(granted)
        }
    }
    
    interface OnPermissionResultListener {
        fun onPermissionResult(granted: Boolean)
    }
}
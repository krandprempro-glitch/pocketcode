package com.termux.app.floating.managers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.termux.app.floating.services.FloatingPermissionService
import com.termux.app.floating.services.FloatingWindowService

/**
 * 悬浮窗管理器
 * 统一管理悬浮窗的显示、隐藏和状态
 */
class FloatingWindowManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: FloatingWindowManager? = null
        
        private const val PREFS_NAME = "floating_settings"
        private const val KEY_FLOATING_ENABLED = "floating_enabled"
        
        fun getInstance(context: Context): FloatingWindowManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingWindowManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var isFloatingEnabled = false
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        loadSettings()
    }
    
    /**
     * 启用悬浮按钮
     */
    fun enableFloating() {
        if (!isFloatingEnabled) {
            FloatingWindowService.showFloating(context)
            isFloatingEnabled = true
            saveSettings()
        }
    }
    
    /**
     * 禁用悬浮按钮
     */
    fun disableFloating() {
        if (isFloatingEnabled) {
            FloatingWindowService.hideFloating(context)
            isFloatingEnabled = false
            saveSettings()
        }
    }
    
    /**
     * 切换悬浮按钮状态
     */
    fun toggleFloating() {
        if (isFloatingEnabled) {
            disableFloating()
        } else {
            enableFloating()
        }
    }
    
    /**
     * 检查悬浮按钮是否启用
     */
    fun isFloatingEnabled(): Boolean {
        return isFloatingEnabled
    }
    
    /**
     * 检查是否有悬浮窗权限
     */
    fun hasFloatingPermission(): Boolean {
        val permissionService = FloatingPermissionService(context)
        return permissionService.hasOverlayPermission()
    }
    
    /**
     * 请求悬浮窗权限
     */
    fun requestFloatingPermission(
        activity: Activity,
        listener: FloatingPermissionService.OnPermissionResultListener?
    ) {
        val permissionService = FloatingPermissionService(context)
        if (!permissionService.hasOverlayPermission()) {
            permissionService.requestOverlayPermission(activity)
            // TODO: 处理权限申请结果回调
        } else {
            listener?.onPermissionResult(true)
        }
    }
    
    private fun loadSettings() {
        isFloatingEnabled = sharedPrefs.getBoolean(KEY_FLOATING_ENABLED, false)
        
        // 如果之前启用过且有权限，则自动启动
        if (isFloatingEnabled && hasFloatingPermission()) {
            FloatingWindowService.showFloating(context)
        }
    }
    
    private fun saveSettings() {
        sharedPrefs.edit()
            .putBoolean(KEY_FLOATING_ENABLED, isFloatingEnabled)
            .apply()
    }
    
    /**
     * 在应用启动时调用，恢复悬浮按钮状态
     */
    fun onAppStarted() {
        if (isFloatingEnabled && hasFloatingPermission()) {
            FloatingWindowService.showFloating(context)
        }
    }
    
    /**
     * 在应用退出时调用
     */
    fun onAppStopped() {
        // 不自动隐藏，让用户手动控制
    }
}
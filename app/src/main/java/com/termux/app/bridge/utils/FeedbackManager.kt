package com.termux.app.bridge.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.termux.shared.logger.Logger

/**
 * 用户反馈管理器
 * 统一管理用户交互反馈，包括Toast消息、震动反馈和Snackbar
 */
class FeedbackManager private constructor(private val context: Context) {
    
    companion object {
        @JvmStatic
        private var instance: FeedbackManager? = null
        
        @JvmStatic
        fun getInstance(context: Context): FeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: FeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
    
    /**
     * 显示成功提示
     */
    fun showSuccess(message: String) {
        showToast(message, Toast.LENGTH_SHORT)
        vibrateIfEnabled(VibrationPattern.SUCCESS)
    }
    
    /**
     * 显示错误提示
     */
    fun showError(message: String) {
        showToast(message, Toast.LENGTH_LONG)
        vibrateIfEnabled(VibrationPattern.ERROR)
    }
    
    /**
     * 显示警告提示
     */
    fun showWarning(message: String) {
        showToast(message, Toast.LENGTH_LONG)
        vibrateIfEnabled(VibrationPattern.WARNING)
    }
    
    /**
     * 显示信息提示
     */
    fun showInfo(message: String) {
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示加载状态
     */
    fun showLoading(message: String) {
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示带操作的Snackbar
     */
    fun showSnackbarWithAction(
        parentView: View, 
        message: String, 
        actionText: String? = null, 
        action: (() -> Unit)? = null
    ) {
        try {
            val snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_LONG)
            if (actionText != null && action != null) {
                snackbar.setAction(actionText) { action() }
            }
            snackbar.show()
        } catch (e: Exception) {
            // 如果Snackbar显示失败，回退到Toast
            showToast(message, Toast.LENGTH_LONG)
            Logger.logError("FeedbackManager", e.toString())
        }
    }
    
    /**
     * 显示撤销操作的Snackbar
     */
    fun showUndoSnackbar(
        parentView: View,
        message: String,
        undoAction: () -> Unit
    ) {
        showSnackbarWithAction(parentView, message, "撤销", undoAction)
    }
    
    /**
     * 显示确认操作的Snackbar
     */
    fun showConfirmSnackbar(
        parentView: View,
        message: String,
        confirmText: String = "确认",
        confirmAction: () -> Unit
    ) {
        showSnackbarWithAction(parentView, message, confirmText, confirmAction)
    }
    
    private fun showToast(message: String, duration: Int) {
        try {
            // 确保在主线程显示Toast
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context, message, duration).show()
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, duration).show()
                }
            }
        } catch (e: Exception) {
            Logger.logError("FeedbackManager",  e.toString())
        }
    }
    
    private fun vibrateIfEnabled(pattern: VibrationPattern) {
        if (!isVibrationEnabled()) return
        
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern.pattern, -1)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern.pattern, -1)
                }
            }
        } catch (e: Exception) {
            Logger.logError("FeedbackManager",  e.toString())
        }
    }
    
    private fun isVibrationEnabled(): Boolean {
        return preferences.getBoolean("vibration_enabled", true)
    }
    
    /**
     * 设置是否启用震动反馈
     */
    fun setVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("vibration_enabled", enabled).apply()
    }
    
    /**
     * 获取震动设置状态
     */
    fun isVibrationEnabledSetting(): Boolean {
        return isVibrationEnabled()
    }
    
    /**
     * 震动模式枚举
     */
    private enum class VibrationPattern(val pattern: LongArray) {
        SUCCESS(longArrayOf(0, 50)),
        ERROR(longArrayOf(0, 100, 50, 100)),
        WARNING(longArrayOf(0, 80)),
        INFO(longArrayOf(0, 30))
    }
    
    /**
     * 反馈类型枚举
     */
    enum class FeedbackType {
        SUCCESS, ERROR, WARNING, INFO
    }
    
    /**
     * 批量显示反馈消息
     */
    fun showFeedback(type: FeedbackType, message: String) {
        when (type) {
            FeedbackType.SUCCESS -> showSuccess(message)
            FeedbackType.ERROR -> showError(message)
            FeedbackType.WARNING -> showWarning(message)
            FeedbackType.INFO -> showInfo(message)
        }
    }
    
    /**
     * 显示多行消息的Toast
     */
    fun showMultilineToast(messages: List<String>, type: FeedbackType = FeedbackType.INFO) {
        val combinedMessage = messages.joinToString("\n")
        showFeedback(type, combinedMessage)
    }
    
    /**
     * 显示带格式的消息
     */
    fun showFormattedMessage(title: String, content: String, type: FeedbackType = FeedbackType.INFO) {
        val formattedMessage = "$title\n$content"
        showFeedback(type, formattedMessage)
    }
}

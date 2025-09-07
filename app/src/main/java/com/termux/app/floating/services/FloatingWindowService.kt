package com.termux.app.floating.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.termux.R
import com.termux.app.MainTabActivity
import com.termux.app.floating.views.FloatingActionButton

/**
 * 悬浮窗服务
 * 管理悬浮窗的生命周期和状态
 */
class FloatingWindowService : Service() {
    
    companion object {
        private const val ACTION_SHOW = "action_show"
        private const val ACTION_HIDE = "action_hide"
        private const val ACTION_TOGGLE = "action_toggle"
        
        private const val NOTIFICATION_ID = 1
        private const val PERMISSION_NOTIFICATION_ID = 2
        
        private const val CHANNEL_SERVICE = "floating_service"
        private const val CHANNEL_PERMISSION = "floating_permission"
        
        fun showFloating(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }
        
        fun hideFloating(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
        
        fun toggleFloating(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_TOGGLE
            }
            context.startService(intent)
        }
    }
    
    private var floatingButton: FloatingActionButton? = null
    private lateinit var permissionService: FloatingPermissionService
    private var isFloatingVisible = false
    
    override fun onCreate() {
        super.onCreate()
        permissionService = FloatingPermissionService(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_SHOW -> showFloatingButton()
                ACTION_HIDE -> hideFloatingButton()
                ACTION_TOGGLE -> {
                    if (isFloatingVisible) {
                        hideFloatingButton()
                    } else {
                        showFloatingButton()
                    }
                }
            }
        }
        
        return START_STICKY // 服务被杀死后自动重启
    }
    
    private fun showFloatingButton() {
        if (!permissionService.hasOverlayPermission()) {
            // 如果没有权限，发送通知提示用户
            showPermissionNotification()
            return
        }
        
        if (floatingButton == null) {
            floatingButton = FloatingActionButton(this).apply {
                setOnFloatingActionListener(object : FloatingActionButton.OnFloatingActionListener {
                    override fun onMenuToggle(isVisible: Boolean) {
                        // 菜单状态变化
                    }
                    
                    override fun onSSHConnectionClicked() {
                        handleSSHConnectionAction()
                    }
                    
                    override fun onRunCommandClicked() {
                        handleRunCommandAction()
                    }
                    
                    override fun onQuickSettingsClicked() {
                        handleQuickSettingsAction()
                    }
                })
            }
        }
        
        if (!isFloatingVisible) {
            floatingButton?.show()
            isFloatingVisible = true
            
            // 创建前台服务通知
            startForeground(NOTIFICATION_ID, createForegroundNotification())
        }
    }
    
    private fun hideFloatingButton() {
        if (floatingButton != null && isFloatingVisible) {
            floatingButton?.hide()
            isFloatingVisible = false
            
            stopForeground(true)
        }
    }
    
    private fun handleSSHConnectionAction() {
        // TODO: 在Phase 4中实现
        val intent = Intent(this, MainTabActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    
    private fun handleRunCommandAction() {
        // TODO: 在Phase 4中实现
        Toast.makeText(this, "运行命令功能", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleQuickSettingsAction() {
        val intent = Intent(this, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "configuration")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    
    private fun showPermissionNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_PERMISSION,
                "悬浮窗权限",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_PERMISSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("需要悬浮窗权限")
            .setContentText("点击设置悬浮窗权限以使用快捷操作功能")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(PERMISSION_NOTIFICATION_ID, notification)
    }
    
    private fun createForegroundNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICE,
                "悬浮按钮服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供悬浮按钮快捷操作功能"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val hideIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_HIDE
        }
        val hidePendingIntent = PendingIntent.getService(
            this, 0, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("悬浮按钮服务运行中")
            .setContentText("点击隐藏悬浮按钮")
            .setContentIntent(hidePendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(R.drawable.ic_close, "隐藏", hidePendingIntent)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingButton()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
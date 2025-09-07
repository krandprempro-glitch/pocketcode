package com.termux.app.floating.extensions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.termux.app.MainTabActivity

/**
 * FloatingActionButton的命令执行扩展功能
 */
class FloatingActionExtensions(private val context: Context) {
    
    /**
     * 处理运行命令操作
     */
    fun handleRunCommandAction() {
        // 在悬浮窗中直接跳转到主界面的运行配置页面
        Toast.makeText(context, "正在打开运行配置页面...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "run_config")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理SSH连接操作
     */
    fun handleSSHConnectionAction() {
        // 在悬浮窗中直接跳转到主界面的SSH连接页面
        Toast.makeText(context, "正在打开SSH连接页面...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "ssh_connection")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理快捷设置操作
     */
    fun handleQuickSettingsAction() {
        // 跳转到设置页面
        Toast.makeText(context, "正在打开设置页面...", Toast.LENGTH_SHORT).show()

        val intent = Intent(context, MainTabActivity::class.java).apply {
            putExtra("navigate_to", "settings")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}

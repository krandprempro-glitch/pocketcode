package com.termux.app.bridge.utils

import android.content.Context
import com.termux.app.floating.managers.ExecutionStateManager
import com.termux.app.floating.managers.FloatingWindowManager
import com.termux.shared.logger.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常情况，提供崩溃日志记录和资源清理
 */
class GlobalExceptionHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "GlobalExceptionHandler"
        
        @JvmStatic
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(context))
        }
    }
    
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            // 记录异常信息
            logException(e)
            
            // 清理资源
            cleanupResources()
            
            // 保存崩溃信息
            saveCrashReport(e)
            
        } catch (ex: Exception) {
            Logger.logError(TAG, ex.toString())
        } finally {
            // 调用默认处理器
            defaultHandler?.uncaughtException(t, e)
        }
    }
    
    private fun logException(e: Throwable) {
        Logger.logError(TAG, "Uncaught exception in thread ${Thread.currentThread().name}"+ e.toString())
        
        // 特殊异常处理
        when {
            e is OutOfMemoryError -> {
                Logger.logError(TAG, "OutOfMemoryError detected - cleaning up resources")
                // 清理内存占用较大的组件
                cleanupMemoryIntensiveComponents()
            }
            e is SecurityException -> {
                Logger.logError(TAG, "SecurityException - possible permission issue")
            }
            e.cause is IOException -> {
                Logger.logError(TAG, "IOException - possible network or file system issue")
            }
        }
    }
    
    private fun cleanupResources() {
        try {
            // 清理悬浮窗
            val floatingManager = FloatingWindowManager.getInstance(context)
            if (floatingManager.isFloatingEnabled()) {
                floatingManager.disableFloating()
            }
            
            // 清理执行状态管理器
            val executionManager = ExecutionStateManager.getInstance(context)
            executionManager.shutdown()
            
        } catch (e: Exception) {
            Logger.logError(TAG,  e.toString())
        }
    }
    
    private fun cleanupMemoryIntensiveComponents() {
        try {
            // 清理缓存
            System.gc()
            
            // 清理执行历史
            val executionManager = ExecutionStateManager.getInstance(context)
            executionManager.clearHistory()
            
        } catch (e: Exception) {
            Logger.logError(TAG, e.toString())
        }
    }
    
    private fun saveCrashReport(e: Throwable) {
        try {
            val crashReport = StringBuilder().apply {
                append("=== CRASH REPORT ===\n")
                append("Time: ${Date()}\n")
                append("Thread: ${Thread.currentThread().name}\n")
                append("Exception: ${e.javaClass.name}\n")
                append("Message: ${e.message}\n")
                append("\nStack Trace:\n")
                
                for (element in e.stackTrace) {
                    append("$element\n")
                }
            }
            
            // 保存到文件
            val crashDir = File(context.filesDir, "crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val filename = "crash_${System.currentTimeMillis()}.txt"
            val crashFile = File(crashDir, filename)
            
            FileWriter(crashFile).use { writer ->
                writer.write(crashReport.toString())
            }
            
            Logger.logInfo(TAG, "Crash report saved to: ${crashFile.absolutePath}")
            
        } catch (ex: Exception) {
            Logger.logError(TAG,  ex.toString())
        }
    }
}

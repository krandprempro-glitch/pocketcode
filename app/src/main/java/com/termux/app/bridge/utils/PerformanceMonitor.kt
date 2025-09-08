package com.termux.app.bridge.utils

import com.termux.shared.logger.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * 性能监控器
 * 监控应用性能并提供优化建议，包括操作耗时和内存使用监控
 */
class PerformanceMonitor private constructor() {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MAX_HISTORY_SIZE = 100
        private const val SLOW_OPERATION_THRESHOLD = 5000L // 5秒
        
        @JvmStatic
        private var instance: PerformanceMonitor? = null
        
        @JvmStatic
        fun getInstance(): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor().also { instance = it }
            }
        }
    }
    
    private val operationStartTimes = ConcurrentHashMap<String, Long>()
    private val operationHistory = ConcurrentHashMap<String, MutableList<Long>>()
    private val runtime = Runtime.getRuntime()
    
    /**
     * 开始监控操作
     */
    fun startOperation(operationName: String) {
        operationStartTimes[operationName] = System.currentTimeMillis()
    }
    
    /**
     * 结束监控操作
     * @return 操作耗时（毫秒）
     */
    fun endOperation(operationName: String): Long {
        val startTime = operationStartTimes.remove(operationName)
        return if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            recordOperationDuration(operationName, duration)
            
            // 如果操作耗时过长，记录警告
            if (duration > SLOW_OPERATION_THRESHOLD) {
                Logger.logError(TAG, "Slow operation detected: $operationName took ${duration}ms")
            }
            
            duration
        } else {
            0L
        }
    }
    
    /**
     * 记录操作耗时
     */
    private fun recordOperationDuration(operationName: String, duration: Long) {
        val history = operationHistory.getOrPut(operationName) { mutableListOf() }
        history.add(duration)
        
        // 限制历史记录数量
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
        }
    }
    
    /**
     * 获取操作平均耗时
     */
    fun getAverageOperationTime(operationName: String): Double {
        val history = operationHistory[operationName]
        return if (history.isNullOrEmpty()) {
            0.0
        } else {
            history.average()
        }
    }
    
    /**
     * 获取操作最大耗时
     */
    fun getMaxOperationTime(operationName: String): Long {
        val history = operationHistory[operationName]
        return history?.maxOrNull() ?: 0L
    }
    
    /**
     * 获取操作最小耗时
     */
    fun getMinOperationTime(operationName: String): Long {
        val history = operationHistory[operationName]
        return history?.minOrNull() ?: 0L
    }
    
    /**
     * 获取内存使用情况
     */
    fun getMemoryInfo(): MemoryInfo {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryInfo(totalMemory, freeMemory, maxMemory, usedMemory)
    }
    
    /**
     * 检查内存是否紧张
     */
    fun isMemoryLow(): Boolean {
        val memInfo = getMemoryInfo()
        val usageRatio = memInfo.usedMemory.toDouble() / memInfo.maxMemory
        return usageRatio > 0.8 // 超过80%认为内存紧张
    }
    
    /**
     * 获取所有监控的操作名称
     */
    fun getAllOperationNames(): Set<String> {
        return operationHistory.keys.toSet()
    }
    
    /**
     * 清空指定操作的历史记录
     */
    fun clearOperationHistory(operationName: String) {
        operationHistory.remove(operationName)
    }
    
    /**
     * 清空所有操作的历史记录
     */
    fun clearAllHistory() {
        operationHistory.clear()
        operationStartTimes.clear()
    }
    
    /**
     * 生成性能报告
     */
    fun generatePerformanceReport(): String {
        val report = StringBuilder()
        report.append("=== Performance Report ===\n")
        
        // 内存信息
        val memInfo = getMemoryInfo()
        report.append("Memory Usage:\n")
        report.append("  Used: ${formatBytes(memInfo.usedMemory)}\n")
        report.append("  Total: ${formatBytes(memInfo.totalMemory)}\n")
        report.append("  Max: ${formatBytes(memInfo.maxMemory)}\n")
        report.append("  Usage: ${String.format("%.1f%%", memInfo.usedMemory.toDouble() / memInfo.maxMemory * 100)}\n")
        
        // 操作耗时统计
        report.append("\nOperation Times:\n")
        for ((opName, history) in operationHistory) {
            if (history.isNotEmpty()) {
                val avgTime = getAverageOperationTime(opName)
                val maxTime = getMaxOperationTime(opName)
                val minTime = getMinOperationTime(opName)
                val count = history.size
                
                report.append("  $opName:\n")
                report.append("    Executions: $count\n")
                report.append("    Avg: ${String.format("%.1fms", avgTime)}\n")
                report.append("    Min: ${minTime}ms\n")
                report.append("    Max: ${maxTime}ms\n")
            }
        }
        
        // 性能建议
        report.append("\nPerformance Suggestions:\n")
        val suggestions = generatePerformanceSuggestions()
        if (suggestions.isEmpty()) {
            report.append("  No issues detected.\n")
        } else {
            suggestions.forEach { suggestion ->
                report.append("  - $suggestion\n")
            }
        }
        
        return report.toString()
    }
    
    /**
     * 生成性能建议
     */
    private fun generatePerformanceSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 检查内存使用
        if (isMemoryLow()) {
            suggestions.add("内存使用率过高，建议清理缓存或减少同时运行的任务")
        }
        
        // 检查慢操作
        for ((opName, history) in operationHistory) {
            if (history.isNotEmpty()) {
                val avgTime = getAverageOperationTime(opName)
                val maxTime = getMaxOperationTime(opName)
                
                if (avgTime > SLOW_OPERATION_THRESHOLD) {
                    suggestions.add("操作 '$opName' 平均耗时过长 (${avgTime.roundToInt()}ms)，建议优化")
                }
                
                if (maxTime > SLOW_OPERATION_THRESHOLD * 2) {
                    suggestions.add("操作 '$opName' 最大耗时过长 (${maxTime}ms)，存在性能瓶颈")
                }
            }
        }
        
        return suggestions
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usedMemory: Long
    ) {
        /**
         * 获取内存使用率
         */
        fun getUsageRatio(): Double = usedMemory.toDouble() / maxMemory
        
        /**
         * 获取内存使用百分比
         */
        fun getUsagePercentage(): Double = getUsageRatio() * 100
    }
}

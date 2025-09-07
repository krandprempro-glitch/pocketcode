package com.termux.app.floating.models

import java.util.UUID

data class ExecutionResult(
    val taskId: String = UUID.randomUUID().toString(),
    var status: Status = Status.EXECUTING,
    var executedCommand: String = "",
    var startTime: Long = System.currentTimeMillis(),
    var endTime: Long = 0,
    var exitCode: Int = 0,
    var processId: Int = 0,
    var output: String = "",
    var errorOutput: String = "",
    var errorMessage: String = "",
    var logFilePath: String = "",
    var killedPrevious: Boolean = false
) {
    
    enum class Status {
        EXECUTING,   // 正在执行
        SUCCESS,     // 执行成功
        ERROR,       // 执行失败
        TIMEOUT      // 执行超时
    }
    
    // 工具方法
    fun isSuccess(): Boolean = status == Status.SUCCESS
    
    fun isRunning(): Boolean = status == Status.EXECUTING
    
    fun isError(): Boolean = status == Status.ERROR || status == Status.TIMEOUT
    
    fun getDuration(): Long {
        return if (endTime > 0 && startTime > 0) {
            endTime - startTime
        } else {
            0
        }
    }
    
    fun getFormattedDuration(): String {
        val duration = getDuration()
        return if (duration < 1000) {
            "${duration}ms"
        } else {
            String.format("%.1fs", duration / 1000.0)
        }
    }
    
    override fun toString(): String {
        return "ExecutionResult(" +
                "taskId='$taskId', " +
                "status=$status, " +
                "exitCode=$exitCode, " +
                "duration=${getFormattedDuration()}" +
                ")"
    }
}
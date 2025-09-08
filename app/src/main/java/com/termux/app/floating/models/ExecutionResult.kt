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
        RUNNING,     // 正在运行 (别名)
        SUCCESS,     // 执行成功
        ERROR,       // 执行失败
        FAILED,      // 执行失败 (别名)
        TIMEOUT,     // 执行超时
        CANCELLED    // 执行取消
    }
    
    // 检查状态是否已完成
    fun Status.isFinished(): Boolean {
        return this in listOf(Status.SUCCESS, Status.ERROR, Status.FAILED, Status.TIMEOUT, Status.CANCELLED)
    }
    
    // 工具方法
    fun isSuccess(): Boolean = status == Status.SUCCESS
    
    fun isRunning(): Boolean = status == Status.EXECUTING || status == Status.RUNNING
    
    fun isError(): Boolean = status == Status.ERROR || status == Status.FAILED || status == Status.TIMEOUT
    
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
package com.termux.app.bridge.utils

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.min
import kotlin.math.pow

/**
 * 网络错误处理器
 * 专门处理网络相关的错误，提供用户友好的错误信息和重试策略
 */
object NetworkErrorHandler {
    
    /**
     * 获取用户友好的错误信息
     */
    @JvmStatic
    fun getReadableErrorMessage(error: Throwable): String {
        return when {
            error is ConnectException -> {
                "无法连接到服务器，请检查网络连接和服务器地址"
            }
            error is SocketTimeoutException -> {
                "连接超时，请检查网络状况或稍后重试"
            }
            error is UnknownHostException -> {
                "无法解析服务器地址，请检查主机名是否正确"
            }
            error.message?.contains("SSH") == true -> {
                "SSH连接失败: ${error.message}"
            }
            error.message?.contains("Authentication", ignoreCase = true) == true -> {
                "身份验证失败，请检查用户名和密码"
            }
            error.message?.contains("Permission denied", ignoreCase = true) == true -> {
                "权限被拒绝，请检查用户权限"
            }
            else -> {
                "网络错误: ${error.message ?: "未知错误"}"
            }
        }
    }
    
    /**
     * 判断是否为可重试的错误
     */
    @JvmStatic
    fun isRetryableError(error: Throwable): Boolean {
        return error is SocketTimeoutException ||
               error is ConnectException ||
               (error.message?.contains("timeout", ignoreCase = true) == true)
    }
    
    /**
     * 获取建议的重试延迟时间（毫秒）
     * 使用指数退避策略
     */
    @JvmStatic
    fun getSuggestedRetryDelay(retryCount: Int): Int {
        // 指数退避策略: 1秒, 2秒, 4秒, 8秒, 16秒, 30秒(最大)
        return min(1000 * 2.0.pow(retryCount).toInt(), 30000)
    }
    
    /**
     * 错误类型枚举
     */
    enum class ErrorType {
        NETWORK_UNAVAILABLE,
        CONNECTION_REFUSED,
        TIMEOUT,
        DNS_RESOLUTION_FAILED,
        AUTHENTICATION_FAILED,
        PERMISSION_DENIED,
        SSH_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * 分析错误类型
     */
    @JvmStatic
    fun analyzeErrorType(error: Throwable): ErrorType {
        return when {
            error is UnknownHostException -> ErrorType.DNS_RESOLUTION_FAILED
            error is ConnectException -> ErrorType.CONNECTION_REFUSED
            error is SocketTimeoutException -> ErrorType.TIMEOUT
            error.message?.contains("Authentication", ignoreCase = true) == true -> ErrorType.AUTHENTICATION_FAILED
            error.message?.contains("Permission denied", ignoreCase = true) == true -> ErrorType.PERMISSION_DENIED
            error.message?.contains("SSH", ignoreCase = true) == true -> ErrorType.SSH_ERROR
            else -> ErrorType.UNKNOWN_ERROR
        }
    }
    
    /**
     * 获取错误修复建议
     */
    @JvmStatic
    fun getErrorSuggestion(error: Throwable): String {
        return when (analyzeErrorType(error)) {
            ErrorType.DNS_RESOLUTION_FAILED -> "请检查主机名拼写是否正确，或尝试使用IP地址连接"
            ErrorType.CONNECTION_REFUSED -> "目标服务器拒绝连接，请检查服务是否正在运行或防火墙设置"
            ErrorType.TIMEOUT -> "连接超时，请检查网络连接或尝试增加超时时间"
            ErrorType.AUTHENTICATION_FAILED -> "身份验证失败，请检查用户名、密码或SSH密钥"
            ErrorType.PERMISSION_DENIED -> "权限不足，请检查用户是否有相应的操作权限"
            ErrorType.SSH_ERROR -> "SSH连接出现问题，请检查SSH服务器配置和密钥设置"
            ErrorType.NETWORK_UNAVAILABLE -> "网络不可用，请检查网络连接状态"
            ErrorType.UNKNOWN_ERROR -> "发生未知错误，请查看详细日志信息"
        }
    }
}
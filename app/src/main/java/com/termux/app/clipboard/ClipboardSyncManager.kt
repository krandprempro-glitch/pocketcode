package com.termux.app.clipboard

import com.termux.app.sftp.SFTPConnectionManager
import com.termux.shared.logger.Logger
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 剪贴板同步管理器
 * 负责在SSH连接建立后自动同步服务器剪贴板到手机
 */
class ClipboardSyncManager private constructor() {

    companion object {
        private const val LOG_TAG = "ClipboardSyncManager"
        private const val SYNC_INTERVAL_SECONDS = 5L
        private const val MAX_CONTENT_SIZE = 1024 * 1024 // 1MB

        @Volatile
        private var instance: ClipboardSyncManager? = null

        fun getInstance(): ClipboardSyncManager {
            return instance ?: synchronized(this) {
                instance ?: ClipboardSyncManager().also { instance = it }
            }
        }
    }

    private var applicationContext: android.content.Context? = null
    private val disposables = CompositeDisposable()
    private var syncDisposable: io.reactivex.rxjava3.disposables.Disposable? = null

    // 缓存状态
    private var lastServerFingerprint: String? = null
    private var lastPhoneFingerprint: String? = null
    private var isEnabled = false

    /**
     * 初始化剪贴板管理器
     */
    fun init(context: android.content.Context) {
        applicationContext = context.applicationContext
    }

    // 服务器剪贴板命令（根据环境检测）
    private enum class ClipboardBackend {
        XCLIP,       // Linux桌面
        PBPASTE,     // macOS
        NONE         // 不支持
    }
    private var backend: ClipboardBackend = ClipboardBackend.NONE

    private var readCommand: String = ""
    private var writeCommand: String = ""

    /**
     * 初始化并开始同步
     */
    fun startSync() {
        if (isEnabled) return
        isEnabled = true

        // 检测服务器环境
        detectClipboardBackend()
        if (backend == ClipboardBackend.NONE) {
            Logger.logInfo(LOG_TAG, "剪贴板同步: 服务器不支持剪贴板访问")
            return
        }

        // 开始定时同步
        startPeriodicSync()
        Logger.logInfo(LOG_TAG, "剪贴板同步已启动，使用后端: $backend")
    }

    /**
     * 停止同步
     */
    fun stopSync() {
        isEnabled = false
        syncDisposable?.dispose()
        syncDisposable = null
        Logger.logInfo(LOG_TAG, "剪贴板同步已停止")
    }

    /**
     * 检测服务器剪贴板环境
     */
    private fun detectClipboardBackend() {
        val sftpManager = SFTPConnectionManager.getInstance()
        if (!sftpManager.isConnected()) {
            backend = ClipboardBackend.NONE
            return
        }

        // 尝试检测macOS
        sftpManager.executeCommand("uname -s")
            .map { it.trim() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ output ->
                when {
                    output.contains("Darwin") -> {
                        backend = ClipboardBackend.PBPASTE
                        readCommand = "pbpaste"
                        writeCommand = "pbcopy"
                    }
                    else -> {
                        // 尝试xclip
                        detectXclip()
                    }
                }
            }, { detectXclip() })
    }

    private fun detectXclip() {
        SFTPConnectionManager.getInstance().executeCommand("which xclip")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ output ->
                if (output.contains("xclip")) {
                    backend = ClipboardBackend.XCLIP
                    readCommand = "xclip -selection clipboard -o"
                    writeCommand = "xclip -selection clipboard -i"
                } else {
                    backend = ClipboardBackend.NONE
                }
            }, {
                backend = ClipboardBackend.NONE
            })
    }

    /**
     * 开始定时同步
     */
    private fun startPeriodicSync() {
        syncDisposable = Observable.interval(SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (isEnabled && backend != ClipboardBackend.NONE) {
                    syncFromServer()
                }
            }, { error ->
                Logger.logError(LOG_TAG, "同步出错: ${error.message}")
            })
    }

    /**
     * 从服务器拉取剪贴板
     */
    private fun syncFromServer() {
        if (!isEnabled || readCommand.isEmpty()) return

        SFTPConnectionManager.getInstance().executeCommand(readCommand)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ content ->
                // 检查内容大小
                if (content.length > MAX_CONTENT_SIZE) {
                    Logger.logInfo(LOG_TAG, "剪贴板内容过大，跳过同步")
                    return@subscribe
                }

                // 计算指纹
                val fingerprint = md5(content)

                // 对比指纹，避免重复
                if (fingerprint != lastServerFingerprint) {
                    lastServerFingerprint = fingerprint

                    // 获取手机当前剪贴板指纹
                    val phoneContent = getPhoneClipboardContent()
                    val phoneFingerprint = md5(phoneContent)

                    // 只有手机剪贴板和服务器不同时才更新
                    if (fingerprint != phoneFingerprint) {
                        setPhoneClipboard(content)
                        lastPhoneFingerprint = fingerprint
                        Logger.logDebug(LOG_TAG, "剪贴板已同步，内容长度: ${content.length}")
                    }
                }
            }, { error ->
                // 静默失败，不打扰用户
            })
    }

    /**
     * 将手机剪贴板推送到服务器
     */
    fun pushToServer(content: String) {
        if (!isEnabled || writeCommand.isEmpty()) return

        if (content.length > MAX_CONTENT_SIZE) {
            Logger.logInfo(LOG_TAG, "剪贴板内容过大，跳过推送")
            return
        }

        // 使用echo传递内容避免shell转义问题
        val escapedContent = content
            .replace("\\", "\\\\")
            .replace("'", "'\\''")
        val command = "echo '$escapedContent' | $writeCommand"

        SFTPConnectionManager.getInstance().executeCommand(command)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // 更新服务器指纹缓存
                lastServerFingerprint = md5(content)
                // 更新手机指纹缓存，避免下次被服务器内容覆盖
                lastPhoneFingerprint = md5(content)
                Logger.logDebug(LOG_TAG, "剪贴板已推送到服务器")
            }, { error ->
                Logger.logError(LOG_TAG, "推送失败: ${error.message}")
            })
    }

    /**
     * 获取手机剪贴板内容
     */
    private fun getPhoneClipboardContent(): String {
        val context = applicationContext ?: return ""
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return ""

        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return ""

        return try {
            clip.getItemAt(0).coerceToText(context).toString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 设置手机剪贴板
     */
    private fun setPhoneClipboard(content: String) {
        val context = applicationContext ?: return
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return

        val clip = android.content.ClipData.newPlainText("server_clipboard", content)
        clipboard.setPrimaryClip(clip)
        // 立即更新手机指纹，避免重复拉取
        lastPhoneFingerprint = md5(content)
    }

    /**
     * 计算MD5指纹
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 是否正在同步
     */
    fun isSyncEnabled(): Boolean = isEnabled

    /**
     * 销毁
     */
    fun destroy() {
        stopSync()
        disposables.clear()
        instance = null
    }
}
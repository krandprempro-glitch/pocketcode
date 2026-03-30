package com.termux.app.clipboard

import android.content.Context
import androidx.preference.PreferenceManager
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

        // SharedPreferences keys (must match GlobalSettingsFragment)
        private const val PREF_CLIPBOARD_SYNC_MASTER = "clipboard_sync_master"
        private const val PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE = "clipboard_sync_server_to_phone"
        private const val PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER = "clipboard_sync_phone_to_server"

        @Volatile
        private var instance: ClipboardSyncManager? = null

        fun getInstance(): ClipboardSyncManager {
            return instance ?: synchronized(this) {
                instance ?: ClipboardSyncManager().also { instance = it }
            }
        }
    }

    private var applicationContext: Context? = null
    private val disposables = CompositeDisposable()
    private var syncDisposable: io.reactivex.rxjava3.disposables.Disposable? = null

    // 缓存状态
    private var lastServerFingerprint: String? = null
    private var lastPhoneFingerprint: String? = null
    private var isEnabled = false

    // 独立的轮询和自动推送状态
    private var isPolling = false
    private var isAutoPushing = false

    // Android剪贴板监听器
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

    // 服务器剪贴板命令（根据环境检测）
    private enum class ClipboardBackend {
        XCLIP,       // Linux桌面 (xclip)
        PBPASTE,     // macOS (pbpaste/pbcopy)
        TERMUX,      // Termux (termux-clipboard-get/set)
        NONE         // 不支持
    }
    private var backend: ClipboardBackend = ClipboardBackend.NONE

    private var readCommand: String = ""
    private var writeCommand: String = ""

    /**
     * 初始化剪贴板管理器
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * 初始化并开始同步（向后兼容入口）
     * 检测后端后，根据SharedPreferences中的设置决定启动哪些同步
     */
    fun startSync() {
        if (isEnabled) return
        isEnabled = true

        detectClipboardBackend()
    }

    /**
     * 后端检测完成后的回调处理
     * 读取SharedPreferences设置并应用
     */
    private fun onBackendDetected() {
        if (!isEnabled) return

        val context = applicationContext ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val master = prefs.getBoolean(PREF_CLIPBOARD_SYNC_MASTER, false)
        val serverToPhone = prefs.getBoolean(PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE, true)
        val phoneToServer = prefs.getBoolean(PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER, false)

        updateSettings(master, serverToPhone, phoneToServer)
    }

    /**
     * 根据设置更新同步状态
     * 由 GlobalSettingsFragment 和 onBackendDetected 调用
     */
    fun updateSettings(master: Boolean, serverToPhone: Boolean, phoneToServer: Boolean) {
        // Bug fix: 如果尚未启动同步但master被打开，先启动同步（后端检测）
        if (!isEnabled && master) {
            startSync()
            // startSync -> detectClipboardBackend -> onBackendDetected -> 会再次读取设置并调用 updateSettings
            // 所以这里不需要继续执行，等后端检测完成后自动处理
            return
        }

        if (!isEnabled) return

        if (!master) {
            stopPolling()
            stopAutoPush()
            return
        }

        // Master is ON
        if (serverToPhone && backend != ClipboardBackend.NONE) {
            startPolling()
        } else {
            stopPolling()
        }

        if (phoneToServer && backend != ClipboardBackend.NONE) {
            startAutoPush()
        } else {
            stopAutoPush()
        }
    }

    /**
     * 停止同步
     */
    fun stopSync() {
        isEnabled = false
        stopPolling()
        stopAutoPush()
        Logger.logInfo(LOG_TAG, "剪贴板同步已停止")
    }

    /**
     * 检测服务器剪贴板环境
     */
    private fun detectClipboardBackend() {
        val sftpManager = SFTPConnectionManager.getInstance()
        if (!sftpManager.isConnected()) {
            backend = ClipboardBackend.NONE
            Logger.logWarn(LOG_TAG, "SSH未连接，无法检测剪贴板后端，等待连接后重试")
            // 不直接return，仍调用onBackendDetected让上层知道状态
            onBackendDetected()
            return
        }

        Logger.logInfo(LOG_TAG, "开始检测服务器剪贴板后端...")
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
                        onBackendDetected()
                    }
                    else -> detectTermuxClipboard()
                }
            }, { detectTermuxClipboard() })
    }

    /**
     * 检测Termux剪贴板环境（termux-clipboard-get/set）
     */
    private fun detectTermuxClipboard() {
        SFTPConnectionManager.getInstance().executeCommand("which termux-clipboard-get")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ output ->
                if (output.contains("termux-clipboard-get")) {
                    backend = ClipboardBackend.TERMUX
                    readCommand = "termux-clipboard-get"
                    writeCommand = "termux-clipboard-set"
                    Logger.logInfo(LOG_TAG, "检测到Termux剪贴板后端")
                    onBackendDetected()
                } else {
                    detectXclip()
                }
            }, {
                // termux-clipboard-get not found, try xclip
                detectXclip()
            })
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
                onBackendDetected()
            }, {
                backend = ClipboardBackend.NONE
                Logger.logInfo(LOG_TAG, "未检测到可用的剪贴板后端 (xclip/termux-clipboard)")
                onBackendDetected()
            })
    }

    /**
     * 开始定时轮询（服务器→手机）
     */
    private fun startPolling() {
        if (isPolling) return
        isPolling = true

        syncDisposable = Observable.interval(SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (isEnabled && isPolling && backend != ClipboardBackend.NONE) {
                    syncFromServer()
                }
            }, { error ->
                Logger.logError(LOG_TAG, "同步出错: ${error.message}")
            })
        Logger.logInfo(LOG_TAG, "剪贴板轮询已启动，使用后端: $backend")
    }

    /**
     * 停止定时轮询
     */
    private fun stopPolling() {
        if (!isPolling) return
        isPolling = false
        syncDisposable?.dispose()
        syncDisposable = null
    }

    /**
     * 启动自动推送（手机→服务器）
     * 监听Android剪贴板变化并推送到服务器
     */
    private fun startAutoPush() {
        if (isAutoPushing) return
        isAutoPushing = true

        val context = applicationContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return

        clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            val clipText = getPhoneClipboardContent()
            if (clipText.isEmpty()) return@OnPrimaryClipChangedListener

            val fingerprint = md5(clipText)

            // 跳过来自服务器同步的内容（防止循环）
            if (fingerprint == lastServerFingerprint) return@OnPrimaryClipChangedListener
            // 跳过未变化的内容
            if (fingerprint == lastPhoneFingerprint) return@OnPrimaryClipChangedListener

            pushToServer(clipText)
        }
        clipboard.addPrimaryClipChangedListener(clipboardListener)
        Logger.logInfo(LOG_TAG, "剪贴板自动推送已启动")
    }

    /**
     * 停止自动推送
     */
    private fun stopAutoPush() {
        if (!isAutoPushing) return
        isAutoPushing = false

        val context = applicationContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return

        clipboardListener?.let { clipboard.removePrimaryClipChangedListener(it) }
        clipboardListener = null
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
                if (content.length > MAX_CONTENT_SIZE) {
                    Logger.logInfo(LOG_TAG, "剪贴板内容过大，跳过同步")
                    return@subscribe
                }

                val fingerprint = md5(content)

                if (fingerprint != lastServerFingerprint) {
                    lastServerFingerprint = fingerprint

                    val phoneContent = getPhoneClipboardContent()
                    val phoneFingerprint = md5(phoneContent)

                    if (fingerprint != phoneFingerprint) {
                        setPhoneClipboard(content)
                        lastPhoneFingerprint = fingerprint
                        Logger.logDebug(LOG_TAG, "剪贴板已同步，内容长度: ${content.length}")
                    }
                }
            }, { _ ->
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

        // 使用 base64 编码避免 shell 转义和多行内容问题
        val base64Content = android.util.Base64.encodeToString(
            content.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val command = "echo '$base64Content' | base64 -d | $writeCommand"

        Logger.logDebug(LOG_TAG, "推送剪贴板到服务器，内容长度: ${content.length}, 后端: $backend")

        SFTPConnectionManager.getInstance().executeCommand(command)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                lastServerFingerprint = md5(content)
                lastPhoneFingerprint = md5(content)
                Logger.logDebug(LOG_TAG, "剪贴板已推送到服务器")
            }, { error ->
                Logger.logError(LOG_TAG, "推送失败: ${error.message}")
            })
    }

    private fun getPhoneClipboardContent(): String {
        val context = applicationContext ?: return ""
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return ""

        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return ""

        return try {
            clip.getItemAt(0).coerceToText(context).toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun setPhoneClipboard(content: String) {
        val context = applicationContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return

        val clip = android.content.ClipData.newPlainText("server_clipboard", content)
        clipboard.setPrimaryClip(clip)
        lastPhoneFingerprint = md5(content)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isSyncEnabled(): Boolean = isEnabled

    fun destroy() {
        stopSync()
        disposables.clear()
        instance = null
    }
}

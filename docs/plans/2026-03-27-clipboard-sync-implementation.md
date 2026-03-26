# 剪贴板同步功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在Tab2 SSH连接后自动同步服务器剪贴板到手机，用户也可主动推送手机剪贴板到服务器

**Architecture:** 复用现有SFTPConnectionManager的SSH连接，通过定时executeCommand轮询服务器剪贴板变化，以MD5指纹避免重复同步

**Tech Stack:** Kotlin, RxJava3, SSHJ, Android ClipboardManager

---

## 实现步骤

### Task 1: 创建ClipboardSyncManager单例

**Files:**
- Create: `app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt`

**Step 1: 创建文件结构**

```kotlin
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

    private val disposables = CompositeDisposable()
    private var syncDisposable: io.reactivex.rxjava3.disposables.Disposable? = null

    // 缓存状态
    private var lastServerFingerprint: String? = null
    private var lastPhoneFingerprint: String? = null
    private var isEnabled = false

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
                lastServerFingerprint = md5(content)
                Logger.logDebug(LOG_TAG, "剪贴板已推送到服务器")
            }, { error ->
                Logger.logError(LOG_TAG, "推送失败: ${error.message}")
            })
    }

    /**
     * 获取手机剪贴板内容
     */
    private fun getPhoneClipboardContent(): String {
        // 临时实现，后续接入Android ClipboardManager
        return ""
    }

    /**
     * 设置手机剪贴板
     */
    private fun setPhoneClipboard(content: String) {
        // 临时实现，后续接入Android ClipboardManager
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
```

**Step 2: 验证代码格式**

Run: `./gradlew app:compileDebugKotlin --no-daemon 2>&1 | head -50`
Expected: 编译无语法错误

**Step 3: 提交**

```bash
git add app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt
git commit -m "feat: 添加ClipboardSyncManager剪贴板同步管理器"
```

---

### Task 2: 集成Android剪贴板

**Files:**
- Modify: `app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt`

**Step 1: 添加Context和应用引用**

```kotlin
// 在构造函数中初始化
class ClipboardSyncManager private constructor() {
    private var applicationContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        applicationContext = context.applicationContext
    }

    // 修改 getPhoneClipboardContent
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

    // 修改 setPhoneClipboard
    private fun setPhoneClipboard(content: String) {
        val context = applicationContext ?: return
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return

        val clip = android.content.ClipData.newPlainText("server_clipboard", content)
        clipboard.setPrimaryClip(clip)
    }
}
```

**Step 2: 在MainTabActivity中初始化**

找到 MainTabActivity.kt 的 onCreate 方法，添加：

```kotlin
// 在 initTabViews() 之后添加
ClipboardSyncManager.getInstance().init(this)
```

**Step 3: 编译验证**

Run: `./gradlew app:compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt app/src/main/java/com/termux/app/MainTabActivity.kt
git commit -m "feat: 集成Android剪贴板到ClipboardSyncManager"
```

---

### Task 3: SSH连接状态联动

**Files:**
- Modify: `app/src/main/java/com/termux/app/MainTabActivity.kt`

**Step 1: 在SSH连接成功时启动同步**

在 TabPagerAdapter 或连接管理逻辑中，找到连接成功的回调：

```kotlin
// 在连接成功的地方添加
SFTPConnectionManager.getInstance().connectWithStatusMonitoring(config)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ success ->
        if (success) {
            // 连接成功，启用剪贴板同步
            ClipboardSyncManager.getInstance().startSync()
        }
    }, { error ->
        // 连接失败
    })
```

**Step 2: 在断开连接时停止同步**

```kotlin
// 在断开连接的地方添加
SFTPConnectionManager.getInstance().disconnect()
ClipboardSyncManager.getInstance().stopSync()
```

**Step 3: 编译验证**

Run: `./gradlew app:compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/termux/app/MainTabActivity.kt
git commit -m "feat: SSH连接状态联动剪贴板同步"
```

---

### Task 4: 添加UI状态图标

**Files:**
- Create: `app/src/main/java/com/termux/app/clipboard/ClipboardSyncStatusView.kt`
- Modify: `app/src/main/res/layout/activity_main_tabs.xml`

**Step 1: 创建状态图标View**

```kotlin
package com.termux.app.clipboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.termux.R

/**
 * 剪贴板同步状态图标
 */
class ClipboardSyncStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val syncIcon: ImageView

    init {
        orientation = HORIZONTAL
        syncIcon = ImageView(context).apply {
            layoutParams = LayoutParams(48, 48)
            setImageResource(android.R.drawable.ic_menu_sync)
            visibility = View.GONE
        }
        addView(syncIcon)
    }

    fun showSyncing() {
        syncIcon.visibility = View.VISIBLE
        // TODO: 旋转动画
    }

    fun showSynced() {
        syncIcon.visibility = View.VISIBLE
        // TODO: 显示勾号
    }

    fun hide() {
        syncIcon.visibility = View.GONE
    }
}
```

**Step 2: 在布局中添加**

在 activity_main_tabs.xml 的 Tab2 布局区域添加：

```xml
<com.termux.app.clipboard.ClipboardSyncStatusView
    android:id="@+id/clipboard_sync_status"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="end|center_vertical"
    android:padding="8dp" />
```

**Step 3: 在Fragment中控制状态**

在 Tab2 的 Fragment 中：

```kotlin
private lateinit var syncStatusView: ClipboardSyncStatusView

// 连接状态监听
SFTPConnectionManager.getInstance().connectionStatus
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { status ->
        when (status) {
            SFTPConnectionManager.ConnectionStatus.CONNECTED -> {
                syncStatusView.showSyncing()
            }
            else -> {
                syncStatusView.hide()
            }
        }
    }
```

**Step 4: 编译验证**

Run: `./gradlew app:compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: 提交**

```bash
git add app/src/main/java/com/termux/app/clipboard/ClipboardSyncStatusView.kt
git add app/src/main/res/layout/activity_main_tabs.xml
git commit -m "feat: 添加剪贴板同步状态图标UI"
```

---

### Task 5: 优化指纹缓存避免死循环

**Files:**
- Modify: `app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt`

**Step 1: 在推送时同时更新本地缓存**

```kotlin
// 修改 pushToServer 方法
fun pushToServer(content: String) {
    // ...
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
```

**Step 2: 在setPhoneClipboard后更新指纹**

```kotlin
// 修改 setPhoneClipboard
private fun setPhoneClipboard(content: String) {
    // ...
    clipboard.setPrimaryClip(clip)
    // 立即更新手机指纹，避免重复拉取
    lastPhoneFingerprint = md5(content)
}
```

**Step 3: 编译验证**

Run: `./gradlew app:compileDebugKotlin --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/termux/app/clipboard/ClipboardSyncManager.kt
git commit -m "fix: 优化指纹缓存避免同步死循环"
```

---

## 测试验证

### 手动测试步骤

1. 打开App，连接SSH
2. 在服务器执行 `echo "test123" | xclip -selection clipboard -i`
3. 等待5秒，检查手机剪贴板是否显示 "test123"
4. 在手机复制内容，检查服务器剪贴板是否更新

### 预期结果

- SSH连接建立后同步自动开始
- 服务器剪贴板变化后5秒内同步到手机
- 手机复制内容后推送到服务器
- 状态图标显示同步状态

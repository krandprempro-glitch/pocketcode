# Tab1 会话列表 UI 优化设计方案

## 1. 概述

将 Termux Tab1 从单一的终端视图改为类似微信的会话列表模式：
- Tab1 显示所有终端会话列表
- 点击会话启动独立的 TerminalActivity
- 每个会话独立运行 Claude Code/终端
- 返回列表时刷新会话状态

## 2. 页面结构

### 2.1 MainTabActivity (保持不变)

```
Tab1: SessionListFragment (会话列表)
Tab2: FileBrowser
Tab3: GitHistory
Tab4: Settings
```

### 2.2 会话列表页面 (SessionListFragment)

```
┌─────────────────────────────────────────────────────────┐
│  终端会话                              [+ 新建]          │  ← Toolbar
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐   │
│  │ ◐ │ ~/project          10:30        ◐ 思考中   │   │
│  │   │ > 请帮我构建登录功能                         │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ● │ ~/code              09:15        ● 运行中  │   │
│  │   │ root@server ❯ ssh connect...                │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│                                              ┌─────┐   │
│                                              │  +  │   │  ← FAB
│                                              └─────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.3 终端页面 (TerminalActivity)

```
┌─────────────────────────────────────────────────────────┐
│ [←] 会话名称                              [⋮] 菜单      │  ← Toolbar 56dp
├─────────────────────────────────────────────────────────┤
│                                                         │
│  TerminalView (全屏)                                    │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  TerminalToolbar (75dp, 可折叠)                        │
│  [▼] ESC TAB CTRL ...                                  │
├─────────────────────────────────────────────────────────┤
│  TerminalInput (~56dp)                                 │
│  [输入框...] [菜单] [发送]                             │
└─────────────────────────────────────────────────────────┘
```

## 3. 数据模型

### 3.1 SessionInfo

```kotlin
data class SessionInfo(
    val id: String,              // 唯一标识 UUID
    val name: String,            // 显示名称 ~/project
    val sshConfigName: String?,  // SSH配置名称 (null=本地终端)
    val currentPath: String,      // 当前路径
    val lastCommand: String,      // 最后一条命令
    val userPrompt: String?,     // 用户在Claude Code的提问
    val status: SessionStatus,   // 会话状态
    val statusDetail: String?,  // 状态详情
    val startTime: Long,         // 创建时间
    val lastActiveTime: Long    // 最后活跃时间
)

enum class SessionStatus {
    IDLE,           // 空闲
    RUNNING,        // 运行中
    CLAUDE_THINKING,// Claude Code 思考中
    CLAUDE_WORKING, // Claude Code 工作输出中
    WAITING_INPUT,  // 等待输入
    ERROR           // 错误
}
```

## 4. 新建会话对话框

### 4.1 布局

```
┌─────────────────────────┐
│  新建会话                 │
│                          │
│  [SSH连接 ▼]  my-server  │  ← 下拉选择已保存SSH
│  [收藏路径 ▼] ~/project  │  ← 下拉选择收藏路径
│  [会话名称]  项目终端     │  ← 可选，默认"主机名:路径"
│                          │
│  [取消]      [创建]      │
└─────────────────────────┘
```

### 4.2 行为

1. 选择SSH配置 → 连接远程
2. 选择收藏路径 → cd 到该目录
3. 创建SessionInfo → 启动TerminalActivity
4. 自动执行SSH连接 + 切换目录

## 5. 状态图标

| 状态 | 图标 | 颜色 | 说明 |
|------|------|------|------|
| 空闲 | ○ | #64748b | 等待输入 |
| 运行中 | ● | #3b82f6 | 终端运行中 |
| 思考中 | ◐ | #f59e0b | Claude Code思考中 |
| 输出中 | ◑ | #8b5cf6 | Claude Code输出中 |
| 等待输入 | ◕ | #22c55e | 等待用户确认 |
| 错误 | ⚠ | #ef4444 | 出错 |

## 6. 文件结构

```
app/src/main/java/com/termux/app/
├── MainTabActivity.kt
├── sessions/
│   ├── SessionListFragment.kt      # Tab1 会话列表
│   ├── SessionInfo.kt              # 会话数据模型
│   ├── SessionAdapter.kt           # RecyclerView适配器
│   ├── NewSessionDialog.kt         # 新建会话对话框
│   └── SessionManager.kt           # 会话管理器(单例)
└── terminal/
    └── TerminalSessionActivity.kt # 全屏终端Activity

app/src/main/res/layout/
├── fragment_session_list.xml        # 会话列表布局
├── item_session.xml                # 会话卡片
├── dialog_new_session.xml          # 新建会话对话框
└── activity_terminal_session.xml   # 终端页面布局
```

## 7. 会话管理器 (SessionManager)

单例模式，管理所有会话：
- 会话列表 CRUD
- 持久化存储 (SharedPreferences/数据库)
- 状态更新通知

```kotlin
object SessionManager {
    private val sessions = mutableListOf<SessionInfo>()

    fun createSession(config: SSHConnectionConfig?, path: String): SessionInfo
    fun closeSession(sessionId: String)
    fun updateSessionStatus(sessionId: String, status: SessionStatus, detail: String?)
    fun updateSessionPrompt(sessionId: String, prompt: String)
    fun getSession(sessionId: String): SessionInfo?
    fun getAllSessions(): List<SessionInfo>
    fun saveToStorage()
    fun loadFromStorage()
}
```

## 8. 状态刷新时机

1. **返回列表时** - onResume() 刷新所有会话状态
2. **会话结束时** - 标记为空闲/错误
3. **新建会话后** - 添加到列表

不需要实时同步，仅在可见时刷新。

## 9. 交互设计

| 交互 | 行为 |
|------|------|
| 点击FAB | 显示新建会话对话框 |
| 点击会话卡片 | 启动TerminalActivity |
| 长按会话卡片 | 显示菜单 [关闭会话] |
| 右滑会话卡片 | 显示删除按钮 |
| 返回列表 | onResume() 刷新状态 |

## 10. 颜色规范

| Token | 色值 | 用途 |
|-------|------|------|
| background | #020617 | 页面背景 |
| surface | #0f172a | 卡片背景 |
| surface_elevated | #1e293b | 选中/按压 |
| primary | #3b82f6 | 主操作 |
| text_primary | #f8fafc | 主文字 |
| text_secondary | #94a3b8 | 次要文字 |
| text_hint | #64748b | 提示文字 |
| status_running | #3b82f6 | 运行中 |
| status_thinking | #f59e0b | 思考中 |
| status_working | #8b5cf6 | 输出中 |
| status_waiting | #22c55e | 等待输入 |
| status_error | #ef4444 | 错误 |
| status_idle | #64748b | 空闲 |

## 11. 实现步骤

1. 创建 SessionInfo 数据模型
2. 创建 SessionManager 单例
3. 创建会话列表布局 fragment_session_list.xml
4. 创建会话卡片布局 item_session.xml
5. 创建 SessionAdapter
6. 创建 SessionListFragment
7. 创建新建会话对话框 NewSessionDialog
8. 创建 TerminalSessionActivity
9. 修改 MainTabActivity Tab1 指向 SessionListFragment
10. 集成测试

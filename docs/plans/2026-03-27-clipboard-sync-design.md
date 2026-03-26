# 剪贴板同步功能设计

## 1. 功能概述

在Tab2（文件浏览页面）SSH连接建立后，自动在后台同步手机与服务器之间的剪贴板内容。同步为单向（服务器→手机），但用户也可主动将手机剪贴板推送到服务器。

## 2. 核心逻辑

```
┌─────────────┐    定时拉取(5s)    ┌──────────────────┐
│   手机App    │ ←───────────────  │   SSH连接复用     │
│             │                    │  executeCommand() │
│ Clipboard   │ ────────────────→ │                   │
│  Manager    │   主动推送         │  xclip/pbpaste   │
└─────────────┘                    └──────────────────┘
```

**拉取流程（自动）**
1. 每5秒通过SSH执行 `xclip -selection clipboard -o`（Linux）或 `pbpaste`（macOS）
2. 对比本地缓存的服务器剪贴板指纹（MD5）
3. 有变化则写入Android系统剪贴板
4. 更新本地指纹缓存

**推送流程（主动）**
1. 用户在App内复制内容时
2. 通过SSH执行写入命令（`xclip -selection clipboard -i` 或 `pbcopy`）
3. 同时更新本地指纹缓存

## 3. 服务器端适配

| 环境 | 读取 | 写入 |
|------|------|------|
| Linux桌面(xclip) | `xclip -selection clipboard -o` | `xclip -selection clipboard -i` |
| macOS | `pbpaste` | `pbcopy` |
| Linux服务器(无xclip) | fallback: `cat /dev/clipboard` | fallback: 需要配置 |

首次同步前自动检测服务器环境，选择合适命令。

## 4. 数据结构

```kotlin
data class ClipboardCache(
    val serverFingerprint: String,  // MD5 of server clipboard
    val phoneFingerprint: String,   // MD5 of phone clipboard
    val lastSyncTime: Long,
    val lastServerContent: String   // 缓存避免重复写入
)
```

## 5. UI

- 状态图标：Tab2页面右上角
  - 同步中：旋转图标
  - 已同步：勾号
  - 断开/未连接：不显示
- 静默后台运行，用户无感知

## 6. 错误处理

| 情况 | 处理 |
|------|------|
| SSH断开 | 暂停同步，重连后继续 |
| xclip不存在 | 静默失败，不影响其他功能 |
| 内容过大(>1MB) | 跳过同步 |

## 7. 实现计划

### Phase 1: 基础功能
- `ClipboardSyncManager` 单例管理同步逻辑
- 服务器环境自动检测（Linux/macOS/Server）
- 定时拉取机制（5秒间隔）
- Android剪贴板读写

### Phase 2: UI集成
- Tab2页面状态图标
- 同步状态监听

### Phase 3: 推送功能
- 手机复制时主动推送
- 指纹缓存避免死循环

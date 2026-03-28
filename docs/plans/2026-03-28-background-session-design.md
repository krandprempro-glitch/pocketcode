# 后台终端会话设计

## 需求概述

用户按返回键返回会话列表后，终端应该继续在后台运行，SSH 连接也保持连接。用户可以随时点击会话列表项恢复到对应的终端视图。

## 核心问题

当前 `TermuxService` 在 `FullTerminalActivity.onDestroy()` 时会解绑并可能清理会话。需要修改为：返回列表时服务保持运行，会话继续在后台。

## 架构设计

### 1. TermuxService 保持运行

- 当所有 Activity 解绑时，`TermuxService` 不自动停止
- 使用 `START_STICKY` 确保服务被杀死后能重启
- 在 `onUnbind()` 中返回 `false` 防止销毁

### 2. 会话持久化

- `TermuxSession` 继续在服务中运行
- 通过 `terminalHandle` (session.mHandle) 标识会话
- `SessionManager` 保存 `terminalHandle` 用于恢复会话

### 3. 恢复终端视图

- 当用户点击会话列表项启动 `FullTerminalActivity` 时
- 通过 `terminalHandle` 查找已存在的 `TermuxSession`
- 直接 attach 到现有 session，而不是创建新的

## 数据流

```
用户按返回键                    用户点击会话
─────────────────────>        ─────────────────────>
     │                              │
     ▼                              ▼
Activity.onDestroy()          Activity.onCreate()
     │                              │
     ▼                              ▼
解绑 ServiceConnection      bindService()
     │                              │
     │                         findSessionByHandle()
     │                              │
     │                              ▼
     │                         attachSession()
     ▼                              │
Service 继续运行                       │
(mTermuxSessions 保持)                  ▼
                               恢复终端视图
```

## 实现要点

### FullTerminalActivity 修改

1. **onDestroy()** - 只解绑不断开服务，不清理会话
2. 保存 `terminalHandle` 到 SessionManager

### TermuxService 修改

1. 维护 `mTermuxSessions` 列表，保持所有会话
2. 提供通过 handle 查找 session 的方法
3. 使用 `START_STICKY` 确保服务存活

### SessionManager 修改

1. `terminalHandle` 字段用于标识后台会话
2. `updateTerminalHandle()` 更新会话句柄

### SessionListFragment 修改

1. 点击会话时传递 `terminalHandle`
2. FullTerminalActivity 通过 handle 恢复会话

## 错误处理

- 如果后台会话已被清理（服务被系统杀死），则创建新会话
- 如果 SSH 连接已断开，需要重新建立连接
- 定期保存会话状态到 SharedPreferences

## 成功标准

1. 返回会话列表后，终端进程继续运行
2. SSH 连接保持连接状态
3. 点击会话列表项能恢复到之前的终端视图
4. 多个会话可以同时在后台运行

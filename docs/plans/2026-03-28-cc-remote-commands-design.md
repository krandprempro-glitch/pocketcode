# Design: Dynamic Claude Code Remote Commands

## Context

After SSH login, users run Claude Code or Codex on the remote server. The current AI command list is hardcoded with some fictional commands (`thinkharder`, `ultrathink`, `!(bash)`, `#(memory)`) that don't exist in Claude Code CLI. The goal is to:

1. Replace hardcoded commands with actual Claude Code CLI slash commands
2. Dynamically fetch user-defined custom commands from `~/.claude/commands/` via SFTP
3. Keep built-in and custom commands in **separate groups** for easy navigation

## Architecture

```
ClaudeCodeCommandManager (Singleton)
├── getDefaultCommands() → built-in CC commands
├── fetchRemoteCommands() → SFTP read ~/.claude/commands/*.md
├── getMergedCommands() → both lists
├── refreshRemoteCommands() → force re-fetch
└── clearCache() → on disconnect

UI Menu (BottomSheet):
├── BOOKMARKS
├── SSH_CONNECTIONS
├── QUICK_COMMANDS
├── AI_COMMANDS (built-in Claude Code slash commands)
├── AI_CUSTOM_COMMANDS (remote user-defined commands, from ~/.claude/commands/)
└── SYSTEM_COMMANDS (claude/codex launch commands)
```

## New Class: ClaudeCodeCommandManager

**File**: `app/src/main/java/com/termux/app/managers/ClaudeCodeCommandManager.kt`

**Singleton** with these responsibilities:

1. `getDefaultCommands()` — hardcoded list of real Claude Code built-in commands
2. `fetchRemoteCommands(forceRefresh: Boolean)` — uses `SFTPConnectionManager` to:
   - List `~/.claude/commands/` directory
   - Filter `.md` files
   - Map filename to command: `review.md` → `/user:review`
   - Optionally read first line of `.md` as description
   - Cache in memory
3. `getBuiltInCommands()` / `getCustomCommands()` — separate accessors
4. `refreshRemoteCommands()` — clear cache + re-fetch
5. `clearCache()` — called on SSH disconnect

### Default Built-in Commands

| Command | Description |
|---------|-------------|
| `/resume` | 恢复会话 |
| `/clear` | 清除对话 |
| `/compact` | 压缩对话 |
| `/model` | 切换模型 |
| `/config` | 配置设置 |
| `/help` | 获取帮助 |
| `/init` | 初始化项目 |
| `/cost` | Token用量和费用 |
| `/status` | 会话状态 |
| `/permissions` | 管理权限 |
| `/memory` | 编辑记忆文件 |
| `/doctor` | 健康检查 |
| `/mcp` | MCP服务器管理 |
| `/add-dir` | 添加目录 |
| `/review` | 代码审查 |
| `/bug` | 报告Bug |
| `/terminal-setup` | 终端集成设置 |
| `/vim` | Vim输入模式 |

### Remote Custom Command Discovery

Claude Code stores user custom commands as `.md` files:
- `~/.claude/commands/` — global user commands
- `.claude/commands/` in project root — project-specific commands (future scope)

Discovery flow:
1. Check `SFTPConnectionManager.isConnected()`
2. Get home dir from `sftpManager.getCurrentWorkingDirectory()`
3. List `~/.claude/commands/` via `sftpManager.listFiles()`
4. Filter files with `.md` extension
5. Strip extension: `my-review.md` → command `/user:my-review`
6. Read first line of each `.md` as description (optional, fallback to filename)
7. Cache in `Map<String, List<Command>>`

### Caching Strategy

- Fetch remote commands once on SSH connect (when `SFTPConnectionManager` establishes connection)
- Store in memory `List<Command>` with a timestamp
- Manual refresh via button in the AI_CUSTOM_COMMANDS group header
- Clear cache on disconnect
- TTL: no auto-expiry, only manual refresh

## UI Changes

### CommandGroupAdapter

Add new category enum: `AI_CUSTOM_COMMANDS`

```java
enum CommandCategory {
    BOOKMARKS,
    SSH_CONNECTIONS,
    QUICK_COMMANDS,
    AI_COMMANDS,        // built-in Claude Code commands
    AI_CUSTOM_COMMANDS, // remote user custom commands
    SYSTEM_COMMANDS
}
```

### FullTerminalActivity.java

In `prepareCommandGroups()`:
- Replace hardcoded AI commands with `ClaudeCodeCommandManager.getDefaultCommands()`
- Add new group `AI_CUSTOM_COMMANDS` with `ClaudeCodeCommandManager.getCustomCommands()`
- Skip group if custom commands list is empty

### TermuxFragment.java

Same changes in `showSimpleClaudeCodeMenu()`.

### ClaudeCodeMenuHelper.java

Replace `getDefaultCommands()` to delegate to `ClaudeCodeCommandManager.getDefaultCommands()`.

### System Commands (updated)

```java
new Command("claude", "启动Claude Code"),
new Command("claude --resume", "恢复上次会话"),
new Command("claude -p \"\"", "快速提问模式"),
new Command("codex", "启动Codex")
```

## Files to Modify

| File | Change |
|------|--------|
| **NEW** `managers/ClaudeCodeCommandManager.kt` | Core manager class |
| `CommandGroupAdapter.java` | Add `AI_CUSTOM_COMMANDS` enum |
| `FullTerminalActivity.java` | Use manager in `prepareCommandGroups()` |
| `TermuxFragment.java` | Use manager in `showSimpleClaudeCodeMenu()` |
| `ClaudeCodeMenuHelper.java` | Delegate to manager |
| `item_command_group.xml` | Optional: add refresh icon for custom group |

## Verification

1. `./gradlew app:assembleDebug` — build succeeds
2. Open terminal without SSH — AI_COMMANDS shows default commands, no AI_CUSTOM_COMMANDS group
3. SSH into remote server with Claude Code installed, ensure `~/.claude/commands/` has some `.md` files
4. Open command menu — AI_CUSTOM_COMMANDS group appears with remote commands
5. Tap a custom command — inserts into input field correctly
6. Test refresh button — re-fetches remote commands
7. Disconnect SSH — custom commands cache cleared

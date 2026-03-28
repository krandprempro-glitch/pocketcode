# Task 1 Report: Tab3 Git History Integration Points

## 0. Changes Already Made in This Branch

### GitHistoryFragment.java (MODIFIED)
**File:** `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`

The fragment has significant in-progress work:

| Field/Method | Lines | Description |
|--------------|-------|-------------|
| `viewModel` | 29 | `GitHistoryViewModel` instance |
| `progressBar`, `statusText`, `tvCurrentBranch`, `branchChip` | 31-37 | UI element references |
| `commitsRecyclerView`, `commitsHeader`, `retryButton` | 35-37 | Additional UI elements |
| `commitAdapter` | 39 | `GitCommitAdapter` instance |
| `cachedBranches` | 40 | Cached branch list for dialog |
| `initViews()` | 59-71 | Binds views and sets up RecyclerView |
| `setupObservers()` | 73-78 | Observes ViewModel LiveData |
| `setupListeners()` | 80-83 | Sets up retry and branch chip click |
| `updateUiState()` | 85-117 | Handles 4 states: NOT_CONNECTED, LOADING, SUCCESS, ERROR |
| `updateBranches()` | 119-127 | Updates current branch display |
| `updateCommits()` | 129-131 | Submits commits to adapter |
| `showBranchSwitchDialog()` | 139-177 | AlertDialog for branch switching |
| `onDirectoryChanged()` | 182-191 | Syncs directory from file browser |
| `onResume()` | 193-208 | Refreshes git history on resume |

**Key Observations:**
- Uses `GitHistoryViewModel` via ViewModelProvider
- Observes `uiState`, `branches`, `commits`, `errorMessage` LiveData
- Shows "请先在文件浏览页连接 SSH" when NOT_CONNECTED
- `onDirectoryChanged()` logs debug info and calls `viewModel.loadGitHistory(newPath)`
- `onResume()` defaults to "/" if path is null

### fragment_git_history.xml (MODIFIED)
**File:** `app/src/main/res/layout/fragment_git_history.xml`

Layout structure:
- **Top header (48dp, dark background):** Git icon + title left, branch chip right with dropdown arrow
- **Content area (white background):**
  - Status bar with status text and retry button
  - Commits header ("提交历史")
  - `commits_recycler_view` (weight=1, takes remaining space)

**Current State:** Layout defines commit list display but NO layout for showing changed files within a commit.

### FullTerminalActivity.java (MODIFIED)
**File:** `app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java`

Key additions relevant to Tab3:

| Section | Lines | Description |
|---------|-------|-------------|
| `mClaudeCodeMenuButton` | 100 | ImageButton for Claude Code menu |
| `mCommandAdapter` | 113 | `CommandGroupAdapter` instance |
| `mCommandMenuDisposables` | 114 | RxJava CompositeDisposable |
| `setupInputView()` | 674-710 | Sets up Claude Code menu button click |
| `openClaudeCodeMenu()` | 712-714 | Calls `showCommandMenu()` |
| `showCommandMenu()` | 745-779 | Shows bottom sheet with command groups |
| `prepareCommandGroups()` | 781-857 | Builds 6 command categories |

**Command Groups (lines 781-857):**
1. Bookmarks - from `ProjectWorkspaceManager`
2. SSH Connections - from `SSHConfigManager`
3. Quick Commands - from `QuickCommandManager`
4. AI Commands (built-in)
5. AI Custom Commands (remote)
6. System Commands (claude, claude --resume, etc.)

**Bottom Sheet Usage:** `showCommandMenu()` at line 745 creates a `BottomSheetDialog` and inflates `R.layout.bottom_sheet_claude_commands`.

### bottom_sheet_claude_commands.xml (MODIFIED)
**File:** `app/src/main/res/layout/bottom_sheet_claude_commands.xml`

Layout structure:
- Drag indicator at top
- Title "Claude Code 快捷指令" with search button
- Hidden search input (`visibility="gone"`)
- `commands_recycler_view` (maxHeight=400dp)

**Relationship to Tab3:** This bottom sheet is for **Claude Code terminal commands**, NOT for viewing Git commit file content. It is displayed by `FullTerminalActivity.showCommandMenu()` and uses `CommandGroupAdapter` (not `GitCommitAdapter`).

### CommandItemAdapter.java (MODIFIED)
**File:** `app/src/main/java/com/termux/app/terminal/CommandItemAdapter.java`

An adapter for terminal command items with **two view types**:

| View Type | Layout | Description |
|-----------|--------|-------------|
| `VIEW_TYPE_NORMAL` (0) | `item_command_improved` | Single-line: command left, description right |
| `VIEW_TYPE_SSH` (1) | `item_ssh_connection` | Two-line card: config name + user@IP |

**Relationship to Tab3:** This adapter is for the Claude Code command menu in `FullTerminalActivity`, not for Git commit display.

---

## 1. Git History Loading Flow

### GitHistoryFragment
**File:** `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`

| Method | Line | Description |
|--------|------|-------------|
| `onDirectoryChanged(String newPath)` | 182-191 | Called to sync path from file browser |
| `onResume()` | 193-208 | Refreshes git history when fragment resumes |

### GitHistoryViewModel
**File:** `app/src/main/java/com/termux/app/viewmodels/GitHistoryViewModel.java`

| Method | Line | Description |
|--------|------|-------------|
| `loadGitHistory(String remotePath)` | 65-87 | Main entry point for loading git history |
| `parseBranches(String output)` | 89-126 | Parses branch list output |
| `parseCommits(String output)` | 128-158 | Parses commit log output |

### Git Commands Used

**Branch listing (line 78):**
```bash
git -C "path" branch -a --no-color 2>&1
```

**Commit log (line 118):**
```bash
git -C "path" log --oneline -20 --format="%H|%s|%an|%ad" --date=unix 2>&1
```

Output format: `hash|message|author|timestamp(unix)`

### Data Models

**GitBranch** (`app/src/main/java/com/termux/app/models/GitBranch.java`):
- `name: String`
- `isCurrent: boolean`

**GitCommit** (`app/src/main/java/com/termux/app/models/GitCommit.java`):
- `hash: String` (7-char short hash)
- `fullHash: String` (40-char full hash)
- `message: String`
- `author: String`
- `timestamp: long` (unix timestamp in seconds)

---

## 2. RecyclerView/Adapter and Layout

### GitCommitAdapter
**File:** `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java`

| Line | Description |
|------|-------------|
| 20 | `GitCommitAdapter extends ListAdapter<GitCommit, CommitViewHolder>` |
| 28-31 | `onCreateViewHolder()` inflates `R.layout.item_git_commit` |
| 35-37 | `onBindViewHolder()` calls `bind()` |
| 39-58 | `CommitViewHolder` binds data to 4 TextViews |

### Commit Item Layout
**File:** `app/src/main/res/layout/item_git_commit.xml`

| Element ID | Line | Style |
|------------|------|-------|
| `commit_hash` | 14-20 | monospace, color #0366D6 (blue) |
| `commit_message` | 23-32 | maxLines=1, ellipsize=end |
| `commit_author` | 41-47 | color #666666 |
| `commit_time` | 49-58 | color #999999, gravity=end |

### Click Handler Status
**IMPORTANT:** The current `GitCommitAdapter` has **NO click listener** implemented. The `CommitViewHolder.bind()` method (lines 53-58) only sets text values - no `OnClickListener` is attached to any view.

---

## 3. Tab2 File Viewer Integration

### RemoteFileBrowserFragment (Tab2)
**File:** `app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt`

| Method | Line | Description |
|--------|------|-------------|
| `loadFileContent(file: RemoteFileItem)` | 855-888 | Loads file content via SFTP and displays it |
| `openFile(file: RemoteFileItem)` | 923-946 | Opens file (images or text in editor) |
| `displayFileContent(file, content)` | 951-977 | Shows content in code editor |
| `closeCodeEditor()` | 979-982 | Hides code editor, shows file list |
| `getCurrentDirectory()` | 92-94 | Returns current path |

### OnDirectoryChangeListener Interface
**Lines 69-75:**
```kotlin
interface OnDirectoryChangeListener {
    fun onDirectoryChanged(newPath: String)
}
```

### Current File Viewing Mechanism

1. `openFile()` or `loadFileContent()` is called with a `RemoteFileItem`
2. File content is read via SFTP using `viewModel.readFileContent(file.path)` (line 876)
3. Content is displayed in `codeContentText` (code editor view) with syntax highlighting
4. The file path is stored in `RemoteFileItem.path`

### Intent Parameters for File Viewing

**RemoteFileItem data class** (used by file browser):
- `path: String` - Full file path
- `name: String` - File name
- `isDirectory: Boolean`
- `size: Long`
- `permissions: String`
- `lastModified: Long`

---

## 4. Other Relevant Integration Points

### SFTPConnectionManager
**File:** `app/src/main/java/com/termux/app/sftp/SFTPConnectionManager.java`

For viewing files at a specific git commit, the existing `executeCommand()` method (line ~636) can be used to run:
```bash
git -C "path" show "commitHash:relative/path" 2>&1
```

### GitHistoryFragment Directory Sync
**Lines 182-191:**
```java
public void onDirectoryChanged(String newPath) {
    if (viewModel != null && viewModel.isConnected() && newPath != null && !newPath.isEmpty()) {
        viewModel.loadGitHistory(newPath);
    }
}
```

### GitCommitAdapter Click Listener Addition Required
To make commit items clickable, code needs to be added in `GitCommitAdapter.java`:
1. Add an `OnCommitClickListener` interface
2. Set listener in `onBindViewHolder()` or constructor
3. Call `listener.onCommitClick(commit)` when item is clicked

---

## 5. Key File Locations Summary

| Component | File Path |
|-----------|-----------|
| GitHistoryFragment | `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java` |
| GitHistoryViewModel | `app/src/main/java/com/termux/app/viewmodels/GitHistoryViewModel.java` |
| GitCommitAdapter | `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java` |
| GitCommit model | `app/src/main/java/com/termux/app/models/GitCommit.java` |
| GitBranch model | `app/src/main/java/com/termux/app/models/GitBranch.java` |
| Commit item layout | `app/src/main/res/layout/item_git_commit.xml` |
| Fragment layout | `app/src/main/res/layout/fragment_git_history.xml` |
| RemoteFileBrowserFragment (Tab2) | `app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.kt` |
| RemoteFileBrowserViewModel | `app/src/main/java/com/termux/filebrowser/viewmodels/RemoteFileBrowserViewModel.kt` |
| SFTPConnectionManager | `app/src/main/java/com/termux/app/sftp/SFTPConnectionManager.java` |

---

## 6. Current State Assessment

### What's Already Implemented
- Git commit list display with branch switching
- Directory sync from file browser via `onDirectoryChanged()`
- Connection state handling (NOT_CONNECTED, LOADING, SUCCESS, ERROR)
- RecyclerView with `GitCommitAdapter` for commit list
- Branch chip that shows current branch and allows switching

### What's MISSING for Commit File Viewing Feature
1. **No click listener on commit items** - `GitCommitAdapter.CommitViewHolder.bind()` only sets text, no `OnClickListener`
2. **No layout for showing changed files within a commit**
3. **No mechanism to get files changed in a commit** - would need `git show --stat` or similar
4. **No integration with `FullTerminalActivity`** - while `FullTerminalActivity` exists and can display content, there is no code path from `GitHistoryFragment` to launch it for commit viewing
5. **FullTerminalActivity bottom sheet is for Claude commands, not commit files** - `bottom_sheet_claude_commands.xml` is a command menu, not a commit file viewer

### How FullTerminalActivity Could Display Commit File Content
`FullTerminalActivity` already has:
- Terminal session handling (`mTerminalSession`)
- `sendCommandToTerminal()` method to execute commands
- Bottom sheet infrastructure (though currently used for Claude commands)

To view files in a commit, the flow would be:
1. User taps a commit in `GitHistoryFragment`
2. Fragment calls `git -C "path" show --stat "commitHash"` to get changed files
3. Either: display in a new bottom sheet/dialog in `GitHistoryFragment`, OR launch `FullTerminalActivity` with the appropriate `git show` command

### Key Gap
**The `GitHistoryFragment` has no click handler for commits.** The `GitCommitAdapter` (lines 39-58) only binds data without setting any click listener. Adding a click listener and a way to display the changed files is the primary remaining work.

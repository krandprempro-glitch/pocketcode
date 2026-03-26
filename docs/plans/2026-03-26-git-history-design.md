# Git 记录展示功能设计

## 1. 概述

在 Tab3（待开发占位页）实现远程 SSH 目录的 Git 记录展示功能。复用 Tab2 文件浏览器的 SSH 连接状态，展示当前目录的 Branch 列表和 Commit 历史。

## 2. 功能范围

### 核心功能
- 展示当前 Git 分支名称
- 展示分支列表（当前分支高亮）
- 展示 Commit 历史（每条记录包含 hash、消息、时间）

### 非功能范围
- **仅查看模式**，不支持切换分支、查看详情
- **跟随文件浏览器目录**，用户在文件浏览器导航到哪个目录，Git 记录就展示哪个目录

## 3. 架构设计

### 3.1 复用现有组件

| 组件 | 复用方式 |
|------|----------|
| `SFTPConnectionManager` | 添加 `executeCommand` 方法执行 git 命令 |
| `SSHConnectionConfig` | 复用现有连接配置 |
| `ConnectionStatus` | 复用现有连接状态管理 |

### 3.2 新增组件

| 组件 | 职责 |
|------|------|
| `GitHistoryFragment` | Tab3 的 Fragment，替代 PlaceholderFragment |
| `GitHistoryViewModel` | 管理 Git 记录的 UI 状态 |
| `GitBranch` | 分支数据模型 |
| `GitCommit` | Commit 数据模型 |

### 3.3 数据流

```
SFTPConnectionManager.executeCommand("git ...")
    ↓
解析 stdout 输出
    ↓
GitHistoryViewModel 更新状态
    ↓
GitHistoryFragment 展示列表
```

## 4. 详细设计

### 4.1 SFTPConnectionManager 扩展

在 `SFTPConnectionManager` 添加方法：

```java
/**
 * 执行远程命令
 * @param command 要执行的命令
 * @return Single<String> 命令输出结果
 */
public Single<String> executeCommand(String command) {
    return Single.<String>create(emitter -> {
        try {
            if (!isConnected) {
                emitter.onError(new RuntimeException("未建立SSH连接"));
                return;
            }
            Session session = sshClient.startSession();
            session.allocateDefaultPTY();
            Session.Command cmd = session.exec(command);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = cmd.getInputStream().read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            cmd.join(30, TimeUnit.SECONDS);
            session.close();
            emitter.onSuccess(baos.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            emitter.onError(e);
        }
    }).subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread());
}
```

### 4.2 数据模型

```java
// GitBranch.java
public class GitBranch {
    private String name;           // 分支名
    private boolean isCurrent;      // 是否当前分支
}

// GitCommit.java
public class GitCommit {
    private String hash;           // 7位hash
    private String fullHash;        // 完整40位hash
    private String message;         // 提交消息
    private String author;          // 作者
    private long timestamp;         // 时间戳
}
```

### 4.3 GitHistoryViewModel

```java
public class GitHistoryViewModel extends ViewModel {

    private final SFTPConnectionManager sftpManager;
    private final MutableLiveData<List<GitBranch>> branches = new MutableLiveData<>();
    private final MutableLiveData<List<GitCommit>> commits = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public void loadGitHistory(String remotePath) {
        isLoading.setValue(true);
        // 1. git branch -a 获取分支列表
        // 2. git log --oneline -20 获取提交历史
    }

    // getter...
}
```

### 4.4 GitHistoryFragment UI

布局结构：
```
LinearLayout (vertical)
├── 连接状态栏 (未连接/加载中/已连接)
├── 分支信息区
│   ├── 当前分支 TextView
│   └── 分支列表 RecyclerView (横向滚动)
├── Divider
├── Commit 历史列表 RecyclerView (垂直)
└── 错误提示区
```

### 4.5 命令列表

| 功能 | 命令 |
|------|------|
| 获取分支列表 | `git branch -a --no-color` |
| 获取当前分支 | `git rev-parse --abbrev-ref HEAD` |
| 获取 Commit 历史 | `git log --oneline -20 --format="%H|%s|%an|%ad" --date=unix` |

## 5. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 未建立 SSH 连接 | 显示"请先连接 SSH" |
| 目录不是 Git 仓库 | 显示"该目录不是 Git 仓库" |
| 命令执行失败 | 显示错误信息 |
| 连接断开 | 自动尝试重新连接或提示用户 |

## 6. UI 状态

| 状态 | 显示内容 |
|------|----------|
| 未连接 | 提示用户先在文件浏览页连接 SSH |
| 加载中 | 显示 ProgressBar |
| 已连接 | 显示分支和 Commit 列表 |
| 错误 | 显示错误信息，可重试 |

## 7. 实现步骤

1. 在 `SFTPConnectionManager` 添加 `executeCommand` 方法
2. 创建 `GitBranch` 和 `GitCommit` 数据模型
3. 创建 `GitHistoryViewModel`
4. 创建 `GitHistoryFragment` 替代 `PlaceholderFragment`
5. 创建对应的布局文件 `fragment_git_history.xml`
6. 更新 `MainTabActivity` 的 `TabPagerAdapter` 使用新 Fragment
7. 测试各场景错误处理

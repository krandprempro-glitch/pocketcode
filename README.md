<p align="center">
  <img src="art/ic_launcher2.png" width="120" alt="PocketCode Logo"/>
</p>

<h1 align="center">PocketCode</h1>

<p align="center">
  <strong>Android 上的远程开发工作站 —— 为 Claude Code / AI 辅助编程而生</strong>
</p>

<p align="center">
  <a href="#功能概览">功能概览</a> •
  <a href="#下载安装">下载安装</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#使用指南">使用指南</a> •
  <a href="#远程连接技巧">远程连接技巧</a> •
  <a href="#致谢">致谢</a>
</p>

---

## 项目简介

**PocketCode** 是基于 [Termux](https://github.com/termux/termux-app) (v0.118.0) 深度定制的 Android 终端应用，专为移动端远程开发场景设计。在保留 Termux 完整终端能力的基础上，新增了 VSCode 风格的远程文件浏览器、Git 历史查看器、Claude Code 快捷指令集成、剪贴板同步、项目管理等工作站级功能。

> **本项目基于 [Termux](https://github.com/termux/termux-app) 开源项目开发，遵循 GPLv3 许可证。**
> Termux 是一个强大的 Android 终端模拟器和 Linux 环境应用，由 Termux 社区维护。感谢 Termux 团队的卓越工作。

### 核心特性

- **四标签页工作台** — 终端会话 / 远程文件浏览 / Git 历史 / 配置中心
- **Claude Code 深度集成** — 18 个内置快捷指令 + 自定义指令菜单 + 剪贴板同步
- **VSCode 风格文件浏览** — 目录树导航 + 书签管理 + 语法高亮预览
- **Git 可视化** — 提交历史 + 分支切换 + 彩色 Diff 查看器
- **一键项目运行** — 预配置 Node.js / Python / Java / Go / PHP / Ruby 运行命令

---

## 功能概览

```
┌──────────────────────────────────────────────────┐
│  Tab 0: 终端会话    │  Tab 1: 远程文件浏览       │
│  · 多会话管理       │  · SFTP 文件浏览            │
│  · SSH 快速连接     │  · 目录树 + 书签            │
│  · Claude Code 菜单 │  · 语法高亮预览             │
│  · 快捷指令         │  · 文件下载 / 属性查看       │
├──────────────────────────────────────────────────┤
│  Tab 2: Git 历史    │  Tab 3: 配置中心            │
│  · 提交历史列表     │  · SSH 连接配置              │
│  · 分支切换         │  · 运行配置管理              │
│  · 文件 Diff 查看   │  · 快捷指令管理              │
│  · 跟随目录联动     │  · 全局设置                  │
└──────────────────────────────────────────────────┘
```

---

## 下载安装

### 环境要求

- Android 5.0 (API 21) 及以上
- 支持架构：`arm64-v8a`、`armeabi-v7a`、`x86_64`

### 编译环境

- **JDK 11**（必需）— 项目使用 AGP 4.2.2 + Gradle 7.2，不兼容 JDK 8 或 JDK 17+
- **Android SDK** — 推荐 API 28+（compileSdkVersion）
- **Gradle 7.2** — 项目已包含 Gradle Wrapper，无需单独安装

```bash
# 验证 Java 版本（需为 11.x）
java -version
# 输出应为: openjdk version "11.x.x" 或 javac 11.x.x

# 如果版本不对，设置 JAVA_HOME：
export JAVA_HOME=/path/to/jdk-11
```

> **提示**：可在 [Adoptium](https://adoptium.net/) 下载 JDK 11。Android Studio 自带的 JDK 也可使用（Help → About 查看 Bundled JDK 版本）。

### 构建

```bash
# 克隆项目
git clone https://github.com/YOUR_USERNAME/termux-ai-dev.git
cd termux-ai-dev

# 构建 Debug APK
./gradlew app:assembleDebug

# 构建 Release APK
./gradlew app:assembleRelease

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

---

## 快速开始

### 1. 首次启动

安装 APK 后首次打开，应用会自动完成以下初始化（约 40 秒）：

- **Bootstrap 安装** — 自动解压架构对应的 Linux 文件系统
- **环境配置** — 创建 `$PREFIX` 目录、设置 PATH、创建符号链接
- **存储挂载** — 在 `~/storage/` 下创建共享存储、下载、图片等目录的符号链接

> **SSH 开箱即用** — openssh、sshpass 已预装在 Bootstrap 中，无需手动安装。

> 如果初始化失败，可尝试删除应用数据后重新启动。

### 2. 配置 SSH 连接

1. 进入 **配置中心** 标签页
2. 点击 **SSH 配置**
3. 点击 `+` 新建连接，填写：
   - **配置名称**：如 `我的服务器`
   - **IP 地址**：服务器 IP 或域名
   - **端口**：默认 22
   - **用户名**：登录用户名
   - **密码**：登录密码（可选密钥认证）
4. 点击 **测试连接** 验证，然后 **保存**

---

## 使用指南

### Tab 0：终端会话

#### 多会话管理

- **新建会话** — 点击右下角 `+` 按钮，可选择：
  - 关联 SSH 连接（自动登录远程服务器）
  - 指定启动路径（通过收藏路径）
  - 自定义会话名称
- **切换会话** — 在会话列表中点击目标会话
- **会话操作** — 长按会话卡片弹出选项菜单（重命名 / 关闭）
- **会话状态** — 每个会话显示实时状态标签（运行中 / 空闲 / 错误等）

#### Claude Code 快捷指令菜单

在终端界面点击 **命令输入框左侧的菜单按钮**（或按 `Ctrl+/` / `Shift+Tab`），打开 BottomSheet 菜单：

| 分类 | 内容 |
|------|------|
| **收藏路径** | 已保存的常用目录，点击自动 `cd` 到目标路径 |
| **SSH 连接** | 已保存的 SSH 配置，点击自动连接 |
| **快捷指令** | 用户自定义的常用命令 |
| **AI 内置指令** | 18 个 Claude Code 内置指令 |
| **AI 自定义指令** | 从远程服务器 `~/.claude/commands/` 同步的自定义技能 |
| **系统命令** | `claude`、`claude --resume`、`claude -p`、`codex` |

**内置 Claude Code 指令列表：**

| 指令 | 功能 |
|------|------|
| `/resume` | 恢复上次会话 |
| `/clear` | 清空对话 |
| `/compact` | 压缩对话上下文 |
| `/model` | 切换模型 |
| `/config` | 打开配置 |
| `/help` | 帮助信息 |
| `/init` | 初始化项目 |
| `/cost` | 查看 Token 用量 |
| `/status` | 查看状态 |
| `/permissions` | 权限管理 |
| `/memory` | 编辑记忆文件 |
| `/doctor` | 健康检查 |
| `/mcp` | MCP 服务器管理 |
| `/add-dir` | 添加工作目录 |
| `/review` | 代码审查 |
| `/bug` | 报告 Bug |
| `/terminal-setup` | 终端设置 |
| `/vim` | Vim 输入模式 |

#### 键盘快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + /` | 打开 Claude Code 菜单 |
| `Shift + Tab` | 打开 Claude Code 菜单 |
| `Ctrl + L` | 清屏 |
| `Ctrl + C` | 中断信号 |
| `Ctrl + Enter` | 发送命令 |

#### 额外按键栏

终端底部提供常用控制键快捷栏：`ESC`、`TAB`、`CTRL`、方向键、`ALT`、`SHIFT` 等，方便在没有硬件键盘的设备上操作。

---

### Tab 1：远程文件浏览

#### 文件浏览

- **左侧抽屉** — VSCode 风格的目录树导航（占屏幕 50% 宽度）
- **连接状态指示** — 绿色/灰色圆点显示当前 SSH 连接状态
- **文件列表** — 显示文件名、大小、修改时间、权限
- **下拉刷新** — 支持 SwipeRefreshLayout 刷新当前目录
- **首页按钮** — 一键返回远程用户 Home 目录

#### 文件操作

长按文件或点击文件右侧操作按钮，弹出操作面板：

| 操作 | 说明 |
|------|------|
| **查看内容** | 内置代码查看器，支持语法高亮（Java/Kotlin/XML/HTML/CSS/JS/JSON/Markdown/Python/Shell/C/C++ 等） |
| **下载到本地** | 通过 SFTP 下载文件到 Android 设备 |
| **添加到书签** | 将文件/目录路径添加到收藏夹 |
| **复制路径** | 复制远程路径到剪贴板 |
| **文件信息** | 查看详细属性（类型/路径/大小/修改时间/权限/MIME 类型等） |

**图片预览** — 对 JPG/PNG/GIF/WebP 图片文件，点击直接弹出图片预览弹窗。

#### 书签管理

- **添加书签** — 在文件操作面板中点击"添加到书签"
- **管理书签** — 通过左侧抽屉菜单打开书签管理，支持：
  - 重命名书签
  - 移除书签
  - 将书签路径发送到终端（自动执行 `cd`）
- **路径收藏** — 长按目录路径栏可快速收藏当前目录
- **工作区持久化** — 每个 SSH 连接维护独立的工作区状态，保存展开的目录、滚动位置等

#### 目录联动

文件浏览器与 **Git 历史** 标签页联动 — 切换目录时自动同步 Git 历史到当前目录。

---

### Tab 2：Git 历史

#### 提交历史浏览

- **分页加载** — 滚动到底部自动加载更多提交记录
- **分支显示** — 顶部显示当前分支名
- **分支切换** — 点击分支名弹出分支选择对话框
- **展开详情** — 点击提交记录展开查看变更文件列表

#### 变更文件查看

每个提交展开后显示变更文件列表，带状态标识：

| 标识 | 含义 |
|------|------|
| `A` | 新增文件 (Added) |
| `M` | 修改文件 (Modified) |
| `D` | 删除文件 (Deleted) |
| `R` | 重命名文件 (Renamed) |

#### Diff 查看器

点击变更文件打开 **GitFileDetailActivity**，提供 GitLab 风格的彩色 Diff 视图：

- `+` 增加行：绿色背景
- `-` 删除行：红色背景
- 上下文行：正常显示
- Hunk 头部信息：显示 `@@ ... @@` 行号范围

---

### Tab 3：配置中心

配置中心是统一的设置入口，包含五个模块：

#### SSH 配置

管理所有远程 SSH 连接，每个配置包含：
- 配置名称、IP 地址、端口、用户名、密码
- 支持密钥认证（ED25519 / ECDSA）
- 一键测试连接

#### 运行配置

预配置项目运行命令，支持 **6 种语言** 自动识别：

| 语言 | 预置命令示例 |
|------|-------------|
| **Node.js** | `npm run dev`、`yarn dev`、`pnpm dev`、`npm start` |
| **Python** | `python app.py`、`flask run`、`gunicorn app:app` |
| **Java** | `mvn spring-boot:run`、`gradle bootRun`、`java -jar app.jar` |
| **Go** | `go run main.go`、`go build && ./app` |
| **PHP** | `php artisan serve`、`composer serve` |
| **Ruby** | `rails server`、`bundle exec rails s` |

每个运行配置可设置：
- 关联的 SSH 连接
- 项目路径
- 工作目录
- 环境变量
- 端口号
- 是否后台运行
- 日志文件名

**执行流程**：选择运行配置 → 确认执行（显示命令预览）→ 远程执行 → 实时显示输出

#### 快捷指令

管理常用命令快捷方式，每个指令包含：
- 名称、命令内容、描述
- 分类标签
- 使用次数统计

#### 全局设置

- **悬浮窗开关** — 开启后在其他应用上层显示快捷操作按钮
- **远程命令测试** — 快速执行测试命令验证连接

---

### 悬浮窗系统

开启悬浮窗后，在任何应用中可通过悬浮按钮快速操作：

- **SSH 快速连接** — 从悬浮菜单选择已保存的 SSH 配置直接连接
- **运行命令** — 选择运行配置，一键远程执行
- **快捷设置** — 快速切换常用设置

> 需要在系统设置中授予「显示在其他应用上层」权限。

---

### 剪贴板同步

PocketCode 支持 Android 设备与远程服务器之间的**双向剪贴板同步**，在手机上复制的内容自动推送到远程服务器，在远程服务器上复制的内容自动拉取到手机。

#### 前置条件

剪贴板同步依赖远程服务器的剪贴板工具：

| 环境 | 所需工具 | 安装方式 |
|------|---------|---------|
| **Linux 桌面** (Ubuntu/Debian/CentOS 等) | `xclip` | `sudo apt install xclip` |
| **macOS** | `pbcopy` / `pbpaste` | 系统自带，无需安装 |
| **无头 Linux** (无桌面环境) | 不支持 | — |

> **重要**：`xclip` 需要 X11 桌面环境才能工作。纯终端 / SSH-only 服务器无法使用剪贴板同步。App 会自动检测远程服务器的 DISPLAY 环境变量（`:0`、`:1` 等），无需手动配置。

#### 开启步骤

1. **连接 SSH** — 在 Tab 0 终端或 Tab 1 文件浏览器中建立 SSH 连接
2. **打开全局设置** — Tab 3 配置中心 → **全局设置**
3. **开启总开关** — 打开「剪贴板同步」主开关
4. **选择方向** — 根据需要开启子开关：
   - **服务器 → 手机**：每 5 秒轮询远程剪贴板，有新内容自动同步到手机
   - **手机 → 服务器**：监听手机剪贴板变化，复制内容自动推送到远程

#### 工作原理

```
┌─────────────┐     SSH exec      ┌─────────────────┐
│  Android App │ ◄──────────────► │  Remote Server   │
│              │                   │                  │
│ ClipboardMgr │  xclip -o (read)  │  X11 Clipboard   │
│   Listener   │  xclip -i (write) │                  │
└─────────────┘                   └─────────────────┘

服务器→手机: 每5秒执行 DISPLAY=:N xclip -o 读取远程剪贴板
手机→服务器: 手机剪贴板变化时执行 DISPLAY=:N xclip -i 写入远程
防循环: MD5 指纹比对，跳过已同步的内容
```

#### 技术细节

- **自动检测后端** — macOS → `pbcopy`/`pbpaste`，Linux → `xclip`（自动遍历 DISPLAY `:1`/`:0`/`:2`）
- **定时轮询** — 每 5 秒检查一次服务器剪贴板变化
- **MD5 去重** — 通过指纹比对避免循环同步
- **大小限制** — 单次同步上限 1MB
- **Base64 传输** — 通过 SSH 执行 base64 编码的命令，避免 shell 转义问题
- **自动启停** — SSH 连接建立时自动启动，断开时自动停止

#### 使用场景

- 在远程服务器上 `git clone` 一个仓库 URL → 手机剪贴板自动获得该 URL → 在手机浏览器中粘贴查看
- 在手机上复制一段代码 → 远程服务器的 Vim/VSCode 中直接 `Ctrl+V` 粘贴
- 跨设备共享链接、命令、配置片段

---

### 内置脚本

通过终端会话标签页右上角的 **脚本按钮** (📜) 访问：

#### 1. Claude Code 技能初始化 (`setup-claude-commands.sh`)

在远程服务器上安装 Claude Code 自定义技能索引系统：

```bash
# 安装到远程服务器
./setup-claude-commands.sh --install

# 更新索引
./setup-claude-commands.sh --update
```

**作用**：扫描远程服务器上 `~/.claude/commands/` 目录下的所有自定义命令和技能，生成 `commands.md` 索引文件。该索引文件会被 App 通过 SFTP 读取，用于填充终端中的「AI 自定义指令」菜单。

#### 2. SSH Keepalive 配置 (`ssh-keepalive-setup.sh`)

配置 SSH 服务端 Keepalive 参数，防止移动网络环境下 SSH 长连接断开：

```bash
# 自动应用（需 root）
./ssh-keepalive-setup.sh --apply

# 检查当前配置
./ssh-keepalive-setup.sh --check

# 恢复默认配置
./ssh-keepalive-setup.sh --undo
```

**配置参数**：
- `ClientAliveInterval 30` — 每 30 秒发送一次心跳
- `ClientAliveCountMax 3` — 3 次无响应后断开
- `TCPKeepAlive yes` — 启用 TCP 保活

> **强烈推荐在移动端使用前执行此脚本**，移动网络切换（WiFi ↔ 4G/5G）极易导致 SSH 断连。

---

## 远程连接技巧

### Tailscale 组网（推荐）

在移动开发场景中，**Tailscale** 是最简单的远程连接方案，无需公网 IP、无需端口映射、无需担心防火墙。

#### 为什么用 Tailscale？

| 场景 | 传统方案 | Tailscale |
|------|---------|-----------|
| 公司内网开发机 | VPN / 跳板机 | 直接连接 |
| 家里的服务器 | 内网穿透 / DDNS | 直接连接 |
| 咖啡馆 / 公共 WiFi | 无法连接 | 自动打洞直连 |
| 多台设备互连 | 配置复杂 | 一键组网 |

#### 配置步骤

**1. 在远程服务器上安装 Tailscale**

```bash
# Ubuntu/Debian
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up

# macOS
brew install tailscale
```

**2. 在 Android 设备上安装 Tailscale**

从 [Google Play](https://play.google.com/store/apps/details?id=com.tailscale.ipn) 或 [F-Droid](https://f-droid.org/packages/com.tailscale.ipn/) 安装 Tailscale 客户端，登录同一账号并开启 VPN。

**3. 在 Termux AI Dev 中使用 Tailscale IP**

- 在 SSH 配置中使用 Tailscale 分配的 IP 地址（通常是 `100.x.x.x`）
- 无论服务器在家里的 NAT 后面还是公司内网，都能直接 SSH 连接
- Tailscale 连接是端到端加密的，安全性有保障

**4. 获取服务器 Tailscale IP**

```bash
# 在远程服务器上执行
tailscale ip -4
# 输出类似: 100.64.0.1
```

> **提示**：Tailscale 免费套餐支持 100 台设备，个人开发完全够用。开启 `tailscale up --accept-routes` 还可以访问远程服务器所在局域网的其他设备。

### SSH 连接优化建议

#### 保持后台连接

Android 系统可能会杀掉后台进程，建议：

1. 在 Termux 通知栏中点击 **Acquire Wakelock**（防止 CPU 休眠）
2. 在系统设置中关闭 Termux 的电池优化
3. 使用内置的 `ssh-keepalive-setup.sh` 脚本配置服务端心跳

#### 密钥认证（更安全）

```bash
# 在 Termux 中生成密钥对
ssh-keygen -t ed25519

# 将公钥复制到服务器
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server

# 之后在 SSH 配置中填写私钥路径即可
```

#### 多服务器管理

在配置中心创建多个 SSH 配置，每个配置可绑定不同的：
- 运行配置（一键启动不同项目）
- 工作区（独立的目录状态和书签）
- 快捷指令（不同服务器的常用命令）

---

## 技术架构

```
├── app/                          # 主应用模块
│   ├── activities/               # Activity 层
│   ├── fragments/                # Fragment 层（MVVM）
│   ├── terminal/                 # 终端集成
│   ├── filebrowser/              # 远程文件浏览器
│   ├── configuration/            # 配置管理（SSH/运行/快捷指令）
│   ├── sessions/                 # 会话管理
│   ├── clipboard/                # 剪贴板同步
│   ├── floating/                 # 悬浮窗系统
│   ├── models/                   # 数据模型
│   ├── managers/                 # 业务逻辑管理器
│   ├── adapters/                 # RecyclerView 适配器
│   ├── sftp/                     # SFTP 连接管理
│   └── api/                      # 外部 API 接口
├── terminal-view/                # 终端视图库（Apache 2.0）
├── terminal-emulator/            # 终端模拟器库（Apache 2.0）
└── termux-shared/                # 共享工具库（MIT）
```

### 关键依赖

| 库 | 版本 | 用途 |
|----|------|------|
| SSHJ | 0.34.0 | SSH/SFTP 客户端 |
| RxJava3 | 3.1.5 | 响应式异步编程 |
| Gson | 2.10.1 | JSON 序列化 |
| BouncyCastle | 1.70 | 加密算法支持 |
| EDDSA | 0.3.0 | ED25519 密钥支持 |
| Markwon | — | Markdown 渲染 |
| Material Components | — | Material Design UI |

---

## 常见问题

### Q: 首次启动卡在 Bootstrap 安装？

尝试清除应用数据后重新启动。确保网络畅通（部分架构需要下载 bootstrap 包）。

### Q: SSH 连接频繁断开？

1. 执行内置脚本 `ssh-keepalive-setup.sh --apply`（需服务端 root 权限）
2. 在 Termux 通知中开启 Wakelock
3. 关闭 Android 系统对 Termux 的电池优化
4. 考虑使用 Tailscale 组网（更稳定的网络通道）

### Q: 悬浮窗不显示？

需要在系统设置 → 应用 → Termux → 权限中开启「显示在其他应用上层」权限。

### Q: 剪贴板同步不工作？

排查步骤：

1. **确认远程服务器有桌面环境** — `xclip` 依赖 X11，无头服务器（纯 SSH 终端）不支持剪贴板同步
2. **确认 xclip 已安装** — 在远程服务器上执行 `which xclip`，如果找不到：
   ```bash
   # Ubuntu/Debian
   sudo apt install xclip
   # CentOS/RHEL
   sudo yum install xclip
   ```
3. **确认 xclip 可用** — 在远程服务器上（非 SSH 会话中）执行：
   ```bash
   echo "test" | xclip -selection clipboard -i
   xclip -selection clipboard -o
   # 应输出 "test"
   ```
4. **检查 App 设置** — Tab 3 配置中心 → 全局设置 → 确认「剪贴板同步」总开关和方向开关已开启
5. **查看日志** — App 会自动检测 DISPLAY 环境变量，如果检测失败可查看 logcat 日志：
   ```bash
   adb logcat | grep ClipboardSyncManager
   ```
6. **重启 App** — 如果之前同步异常，在手机设置中强制停止 App 后重新打开，后端会重新检测

---

## 致谢

### 上游项目

- **[Termux](https://github.com/termux/termux-app)** — 本项目基于 Termux 开发，感谢 Termux 团队和社区的贡献
- **[Terminal Emulator for Android](https://github.com/jackpal/Android-Terminal-Emulator)** — 终端视图和模拟器核心（Apache 2.0）
- **[SSHJ](https://github.com/hierynomus/sshj)** — SSH/SFTP 连接库

### 许可证

本项目遵循 **GPLv3 only** 许可证。部分代码遵循不同许可证：

| 模块 | 许可证 |
|------|--------|
| termux-app | GPLv3 only |
| terminal-view / terminal-emulator | Apache 2.0 |
| termux-shared | MIT (含 GPLv3 / GPLv2+CE 部分) |

详见 [LICENSE.md](LICENSE.md)。

---

<p align="center">
  Made with ❤️ for mobile developers
</p>

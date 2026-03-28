# Tab1 脚本快捷执行功能设计

## 概述

在 Tab1（终端会话列表）的右上角添加脚本图标按钮，点击后弹出脚本选择对话框，从 `assets/scripts/` 读取脚本，通过 SSH exec 通道在远程服务器执行。

## 核心流程

1. 用户点击 Tab1 右上角脚本图标
2. 弹出脚本选择对话框，显示可用脚本列表
3. 用户选择脚本
4. 系统通过 SSH exec 通道执行脚本内容
5. 执行结果输出到当前终端会话

## 数据模型

### ScriptItem
| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 脚本名称 |
| description | String | 脚本描述 |
| fileName | String | 文件名（如 "deploy.sh"） |
| content | String | 脚本完整内容 |

## 组件设计

### 1. ScriptManager
单例类，负责脚本资源管理：
- 从 `assets/scripts/` 加载脚本列表
- 解析 `scripts.json` 获取脚本描述
- 缓存脚本内容
- 提供 `getScripts()` 和 `getScriptContent(name)` 方法

### 2. ScriptSelectionDialog
对话框组件：
- RecyclerView + ListAdapter 显示脚本列表
- 列表项：脚本名称 + 描述
- 点击执行：读取内容 → SSH exec

### 3. SFTPConnectionManager.execCommand()
新增方法，通过 SSHJ exec 通道执行命令：
```java
public Single<String> execCommand(String command)
```
- 使用 `sshClient.startSession()` → `session.exec(command)`
- 返回执行结果字符串

## 脚本存储格式

```
app/src/main/assets/scripts/
├── scripts.json          # 脚本索引
├── connect-remote.sh     # 示例脚本
├── deploy.sh             # 示例脚本
└── quick-task.sh         # 示例脚本
```

**scripts.json 格式：**
```json
{
  "scripts": [
    {
      "name": "connect-remote",
      "description": "连接远程服务器并执行任务",
      "file": "connect-remote.sh"
    }
  ]
}
```

## UI 改动

### fragment_session_list.xml
在右上角 `btn_settings` 旁边添加脚本图标按钮：
```xml
<ImageButton
    android:id="@+id/btn_scripts"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="脚本"
    android:src="@drawable/ic_script"
    app:tint="@color/white" />
```

### SessionListFragment.kt
- 新增 `btn_scripts` 点击处理
- 调用 `showScriptSelectionDialog()`

### menu_session_list.xml
添加脚本菜单项：
```xml
<item
    android:id="@+id/action_scripts"
    android:icon="@drawable/ic_script"
    android:title="脚本" />
```

## SSH 执行方式

通过 SSHJ exec 通道执行：
```bash
bash -c '脚本内容'
```

支持：
- 多行脚本
- 特殊字符转义
- 命令输出回传

## 依赖项

- 复用现有 `SFTPConnectionManager` 的 SSHJ 连接
- 复用现有 `SessionManager` 管理终端会话
- 新增 `ScriptManager` 管理脚本资源

# 常用指令功能实现计划

## 项目背景
为 Tab4 配置管理添加"常用指令"功能，允许用户自定义保存常用命令，支持添加、编辑、删除、执行命令。数据需要持久化保存到 SharedPreferences。

## 参考现有模式
- 数据管理: 参考 `RunConfigurationManager.kt` (SharedPreferences + Gson)
- 列表页面: 参考 `RunConfigListActivity.kt` + `RunConfigListFragment.kt`
- 编辑页面: 参考 `RunConfigDetailActivity.kt` + `RunConfigDetailFragment.kt`
- 主入口集成: 参考 `ConfigurationMainFragment.kt` 中 `RUN_CONFIG` 的处理方式

## 功能需求
1. 在配置主页面新增"常用指令"入口
2. 列表页面显示所有保存的指令，支持搜索
3. 支持添加、编辑、删除指令
4. 点击指令可直接发送到终端执行
5. 数据使用 SharedPreferences + Gson 持久化保存

---

## 任务分解

### 任务1: 创建数据模型和Manager
**目标**: 创建 QuickCommand 数据模型和 QuickCommandManager 管理类

**需要创建的文件**:
1. `app/src/main/java/com/termux/app/configuration/models/QuickCommand.kt`
2. `app/src/main/java/com/termux/app/configuration/managers/QuickCommandManager.kt`

**QuickCommand 模型要求**:
```kotlin
data class QuickCommand(
    var id: String = "",
    var name: String = "",           // 命令名称（如：启动MySQL）
    var command: String = "",        // 实际命令文本
    var description: String = "",    // 描述（可选）
    var category: String = "",       // 分类（可选）
    var createdTime: Long = System.currentTimeMillis(),
    var lastUsedTime: Long = 0L,
    var useCount: Int = 0            // 使用次数
)
```

**QuickCommandManager 功能要求**:
- 单例模式，使用 SharedPreferences 存储
- 存储键值格式: `quick_cmd_${id}`
- 方法: saveCommand, getCommand, getAllCommands, deleteCommand, updateLastUsed
- 排序: 按最后使用时间倒序

**参考文件**:
- `RunConfigurationManager.kt` - 完全参考其模式
- `ConfigurationConstants.kt` - 添加新的 PREFS_QUICK_COMMANDS 常量

---

### 任务2: 创建列表Activity
**目标**: 创建 QuickCommandListActivity，遵循现有Activity模式

**需要创建的文件**:
1. `app/src/main/java/com/termux/app/configuration/activities/QuickCommandListActivity.kt`

**要求**:
- 参考 `RunConfigListActivity.kt` 的结构
- 使用布局 `activity_configuration_detail.xml`
- 标题设置为"常用指令"
- 支持返回按钮

**入口方法**:
```kotlin
companion object {
    fun newIntent(context: Context): Intent
}
```

---

### 任务3: 创建列表Fragment
**目标**: 创建 QuickCommandListFragment，实现列表展示和操作

**需要创建的文件**:
1. `app/src/main/java/com/termux/app/configuration/fragments/QuickCommandListFragment.kt`

**功能要求**:
- 列表展示所有指令（名称、描述、命令预览）
- 支持搜索过滤
- 点击指令 → 发送到终端执行
- 长按/点击编辑图标 → 进入编辑页面
- 点击删除 → 确认删除
- 空状态提示
- 新建按钮

**交互细节**:
- 点击执行前需要确认对话框，显示完整命令
- 执行后更新 useCount 和 lastUsedTime
- 使用 MainTabActivity.sendCommandToTerminal() 发送命令

---

### 任务4: 创建编辑Fragment
**目标**: 创建 QuickCommandEditFragment，实现添加/编辑功能

**需要创建的文件**:
1. `app/src/main/java/com/termux/app/configuration/fragments/QuickCommandEditFragment.kt`

**功能要求**:
- 表单字段: 名称(必填)、命令(必填)、描述(可选)、分类(可选)
- 验证: 名称和命令不能为空
- 保存按钮
- 返回时刷新列表
- 支持新建和编辑两种模式

**参考**:
- `RunConfigDetailFragment.kt` 的表单处理方式

---

### 任务5: 创建适配器和布局文件
**目标**: 创建适配器和所有需要的布局文件

**需要创建的文件**:
1. `app/src/main/java/com/termux/app/configuration/adapters/QuickCommandAdapter.kt`
2. `app/src/main/res/layout/fragment_quick_command_list.xml`
3. `app/src/main/res/layout/fragment_quick_command_edit.xml`
4. `app/src/main/res/layout/item_quick_command.xml`

**布局要求**:
- 遵循 Material Design 3 规范
- 使用项目现有的颜色定义 (rc_background, text_primary, etc.)
- 列表项显示: 名称、描述(可选)、命令预览(截断显示)
- 编辑页面使用 TextInputLayout

---

### 任务6: 集成到配置主页面
**目标**: 在 ConfigurationMainFragment 中添加"常用指令"入口

**需要修改的文件**:
1. `app/src/main/java/com/termux/app/configuration/models/ConfigurationItem.kt`
2. `app/src/main/java/com/termux/app/configuration/fragments/ConfigurationMainFragment.kt`
3. `app/src/main/java/com/termux/app/configuration/utils/ConfigurationConstants.kt`

**修改要求**:
- `ConfigurationItem.ConfigurationType` 添加 `QUICK_COMMANDS`
- `DEFAULT_ITEMS` 数组添加新配置项:
  ```kotlin
  ConfigurationItem(
      "quick_commands",
      "常用指令",
      "管理常用命令快捷方式",
      R.drawable.ic_terminal,  // 或使用新的图标
      ConfigurationType.QUICK_COMMANDS
  )
  ```
- `ConfigurationMainFragment.onConfigurationItemClicked()` 添加处理
- `ConfigurationConstants` 添加 `PREFS_QUICK_COMMANDS` 常量

---

## 技术规范

### 存储规范
- 使用 SharedPreferences 存储
- 键值前缀: `quick_cmd_`
- Preferences文件名: `quick_commands`
- JSON序列化使用 Gson

### UI规范
- 遵循 Material Design 3
- 使用现有颜色资源
- 支持暗黑模式（使用项目主题）
- 中文界面

### 代码规范
- Kotlin 语言
- 遵循现有代码风格
- 使用 Logger 记录日志
- 适当的错误处理和用户提示 (Toast)

### 集成点
- 命令执行通过 MainTabActivity.sendCommandToTerminal(command: String)
- 添加到配置主页面作为第四个配置项

---

## 测试检查清单

- [ ] 新建指令正常保存
- [ ] 编辑指令正常更新
- [ ] 删除指令正常移除
- [ ] 列表按最后使用时间排序
- [ ] 点击指令弹出确认对话框
- [ ] 确认后命令发送到终端
- [ ] 空状态正确显示
- [ ] 搜索功能正常过滤
- [ ] 数据重启应用后仍然保留

---

## 文件清单

### 新增文件 (8个)
1. `app/src/main/java/com/termux/app/configuration/models/QuickCommand.kt`
2. `app/src/main/java/com/termux/app/configuration/managers/QuickCommandManager.kt`
3. `app/src/main/java/com/termux/app/configuration/activities/QuickCommandListActivity.kt`
4. `app/src/main/java/com/termux/app/configuration/fragments/QuickCommandListFragment.kt`
5. `app/src/main/java/com/termux/app/configuration/fragments/QuickCommandEditFragment.kt`
6. `app/src/main/java/com/termux/app/configuration/adapters/QuickCommandAdapter.kt`
7. `app/src/main/res/layout/fragment_quick_command_list.xml`
8. `app/src/main/res/layout/item_quick_command.xml`

### 修改文件 (3个)
1. `app/src/main/java/com/termux/app/configuration/models/ConfigurationItem.kt`
2. `app/src/main/java/com/termux/app/configuration/fragments/ConfigurationMainFragment.kt`
3. `app/src/main/java/com/termux/app/configuration/utils/ConfigurationConstants.kt`

### 可选 (编辑页面布局)
- `app/src/main/res/layout/fragment_quick_command_edit.xml` 或复用现有编辑布局

---

## 参考代码位置

- 数据管理参考: `app/src/main/java/com/termux/app/configuration/managers/RunConfigurationManager.kt`
- 列表Activity参考: `app/src/main/java/com/termux/app/configuration/activities/RunConfigListActivity.kt`
- 列表Fragment参考: `app/src/main/java/com/termux/app/configuration/fragments/RunConfigListFragment.kt`
- 配置主页面: `app/src/main/java/com/termux/app/configuration/fragments/ConfigurationMainFragment.kt`
- 主TabActivity: `app/src/main/java/com/termux/app/MainTabActivity.kt` (提供 sendCommandToTerminal)

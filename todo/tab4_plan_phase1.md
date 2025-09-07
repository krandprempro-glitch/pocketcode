# Phase 1: 基础架构搭建任务 (≤12w tokens)

## 🎯 阶段目标
建立Configuration配置管理模块的基础架构，包括数据模型、管理器、常量定义和基础Fragment框架。为后续开发阶段奠定坚实基础。

## 📋 具体任务列表

### 1. 重构现有SettingsFragment
**文件**: `SettingsFragment.java` → `ConfigurationMainFragment.java`

**当前状态分析**:
```java
// 当前SettingsFragment很简单，只有占位文本
public class SettingsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText("设置功能\n(待实现)");
        textView.setGravity(Gravity.CENTER);
        return view;
    }
}
```

**重构任务**:
- 将`SettingsFragment`重命名并移动到`configuration`包下
- 创建`ConfigurationMainFragment`作为配置主页
- 实现RecyclerView列表显示配置项
- 添加点击导航到二级页面的功能

### 2. 创建核心数据模型

#### 2.1 运行配置模型 - RunConfiguration.java
```java
package com.termux.app.configuration.models;

public class RunConfiguration {
    private String id;                    // 唯一标识符
    private String name;                  // 配置名称
    private String sshConfigId;           // 关联的SSH配置ID  
    private String projectPath;           // 项目路径
    private String languageType;          // 语言类型
    private String command;               // 运行命令
    private String workingDir;            // 工作目录
    private String envVariables;          // 环境变量
    private boolean runInBackground;      // 后台运行
    private String logFileName;           // 日志文件名
    private long createdTime;             // 创建时间
    private long lastUsedTime;            // 最后使用时间
    
    // 构造函数、getter/setter、工具方法
    public boolean isValid() { /* 验证配置有效性 */ }
    public String generateUniqueId() { /* 生成唯一ID */ }
    // ... 其他方法
}
```

#### 2.2 命令模板模型 - CommandTemplate.java  
```java
package com.termux.app.configuration.models;

public class CommandTemplate {
    private String id;                    // 模板ID
    private String name;                  // 模板名称  
    private LanguageType languageType;    // 语言类型
    private String commandPattern;        // 命令模式
    private String description;           // 描述信息
    private boolean isBuiltIn;           // 是否内置模板
    private Map<String, String> defaultParams; // 默认参数
    
    // 预定义模板常量
    public static final CommandTemplate[] BUILTIN_TEMPLATES = {
        new CommandTemplate("nodejs_npm_dev", "npm run dev", NODEJS, "npm run dev", "Node.js npm开发服务器", true),
        new CommandTemplate("python_flask", "python app.py", PYTHON, "python app.py", "Python Flask应用", true),
        // ... 更多预定义模板
    };
}
```

#### 2.3 语言类型枚举 - LanguageType.java
```java
package com.termux.app.configuration.models;

public enum LanguageType {
    NODEJS("Node.js", "node", new String[]{"npm run dev", "yarn dev", "pnpm dev"}),
    PYTHON("Python", "python", new String[]{"python app.py", "python manage.py runserver", "gunicorn app:app"}),
    JAVA("Java", "java", new String[]{"java -jar app.jar", "mvn spring-boot:run", "gradle bootRun"}),
    GO("Go", "go", new String[]{"go run main.go", "go run .", "./app"}),
    CUSTOM("自定义", "custom", new String[]{});
    
    private final String displayName;
    private final String identifier;
    private final String[] commonCommands;
    
    // 构造函数和getter方法
    LanguageType(String displayName, String identifier, String[] commonCommands) {
        this.displayName = displayName;
        this.identifier = identifier;
        this.commonCommands = commonCommands;
    }
}
```

#### 2.4 配置项模型 - ConfigurationItem.java
```java
package com.termux.app.configuration.models;

public class ConfigurationItem {
    private String id;                    // 配置项ID
    private String title;                 // 标题
    private String description;           // 描述  
    private int iconRes;                  // 图标资源ID
    private ConfigurationType type;       // 配置类型
    private boolean enabled;              // 是否启用
    
    public enum ConfigurationType {
        SSH_CONFIG,                       // SSH连接配置
        RUN_CONFIG,                       // 运行配置
        GLOBAL_SETTINGS,                  // 全局设置
        ABOUT                             // 关于页面
    }
    
    // 预定义配置项
    public static final ConfigurationItem[] DEFAULT_ITEMS = {
        new ConfigurationItem("ssh_config", "SSH连接配置", "管理SSH服务器连接", R.drawable.ic_ssh, SSH_CONFIG),
        new ConfigurationItem("run_config", "运行配置管理", "配置项目运行命令", R.drawable.ic_run, RUN_CONFIG),
        new ConfigurationItem("global_settings", "全局设置", "应用偏好设置", R.drawable.ic_settings, GLOBAL_SETTINGS),
        new ConfigurationItem("about", "关于", "版本信息和帮助", R.drawable.ic_info, ABOUT)
    };
}
```

### 3. 实现数据管理器

#### 3.1 运行配置管理器 - RunConfigurationManager.java
```java
package com.termux.app.configuration.managers;

public class RunConfigurationManager {
    private static final String PREFS_NAME = "run_configurations";
    private static final String CONFIG_PREFIX = "run_config_";
    
    private static RunConfigurationManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    private final Context context;
    
    private RunConfigurationManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    public static synchronized RunConfigurationManager getInstance(Context context) {
        if (instance == null) {
            instance = new RunConfigurationManager(context);
        }
        return instance;
    }
    
    // 核心CRUD方法
    public void saveConfiguration(RunConfiguration config) { /* 保存配置 */ }
    public RunConfiguration getConfiguration(String id) { /* 获取配置 */ }
    public List<RunConfiguration> getAllConfigurations() { /* 获取所有配置 */ }
    public boolean deleteConfiguration(String id) { /* 删除配置 */ }
    
    // 查询方法
    public List<RunConfiguration> getConfigurationsBySSH(String sshConfigId) { /* 按SSH分组 */ }
    public List<RunConfiguration> getRecentConfigurations(int limit) { /* 最近使用的配置 */ }
    
    // 工具方法
    public void updateLastUsed(String configId) { /* 更新使用时间 */ }
    public boolean isNameExists(String name, String excludeId) { /* 检查名称重复 */ }
}
```

#### 3.2 命令模板管理器 - CommandTemplateManager.java
```java
package com.termux.app.configuration.managers;

public class CommandTemplateManager {
    private static CommandTemplateManager instance;
    private final List<CommandTemplate> builtInTemplates;
    private final SharedPreferences preferences;
    
    private CommandTemplateManager(Context context) {
        this.preferences = context.getSharedPreferences("command_templates", Context.MODE_PRIVATE);
        this.builtInTemplates = Arrays.asList(CommandTemplate.BUILTIN_TEMPLATES);
    }
    
    // 模板管理方法
    public List<CommandTemplate> getTemplatesByLanguage(LanguageType language) { /* 按语言获取模板 */ }
    public void saveCustomTemplate(CommandTemplate template) { /* 保存自定义模板 */ }
    public List<CommandTemplate> getAllTemplates() { /* 获取所有模板 */ }
    
    // 命令生成方法
    public String generateCommand(CommandTemplate template, RunConfiguration config) { /* 生成命令 */ }
    public List<String> getCommonCommands(LanguageType language) { /* 获取常用命令 */ }
}
```

#### 3.3 页面导航管理器 - ConfigNavigationManager.java
```java
package com.termux.app.configuration.managers;

public class ConfigNavigationManager {
    private final FragmentManager fragmentManager;
    private final int containerId;
    
    public ConfigNavigationManager(FragmentManager fragmentManager, int containerId) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }
    
    // 导航方法
    public void navigateToSSHConfigList() { /* 导航到SSH配置列表 */ }
    public void navigateToSSHConfigDetail(String configId) { /* 导航到SSH配置详情 */ }
    public void navigateToRunConfigList() { /* 导航到运行配置列表 */ }  
    public void navigateToRunConfigDetail(String configId) { /* 导航到运行配置详情 */ }
    public void navigateBack() { /* 返回上级页面 */ }
    
    // Fragment管理
    private void replaceFragment(Fragment fragment, boolean addToBackStack) { /* 替换Fragment */ }
    private void addFragment(Fragment fragment) { /* 添加Fragment */ }
}
```

### 4. 创建工具类和常量

#### 4.1 命令构建工具 - CommandBuilder.java
```java
package com.termux.app.configuration.utils;

public class CommandBuilder {
    
    // 基础命令构建
    public static String buildBasicCommand(RunConfiguration config) {
        StringBuilder cmd = new StringBuilder();
        
        // 切换到项目目录
        cmd.append("cd ").append(config.getProjectPath());
        if (!TextUtils.isEmpty(config.getWorkingDir()) && !".".equals(config.getWorkingDir())) {
            cmd.append("/").append(config.getWorkingDir());
        }
        cmd.append(" && ");
        
        // 添加环境变量
        if (!TextUtils.isEmpty(config.getEnvVariables())) {
            cmd.append(config.getEnvVariables()).append(" ");
        }
        
        // 执行命令
        cmd.append(config.getCommand());
        
        return cmd.toString();
    }
    
    // 后台运行命令构建
    public static String buildBackgroundCommand(RunConfiguration config) {
        String basicCommand = buildBasicCommand(config);
        
        if (!config.isRunInBackground()) {
            return basicCommand;
        }
        
        StringBuilder cmd = new StringBuilder();
        cmd.append("nohup ").append(basicCommand);
        
        // 添加日志输出
        String logFile = config.getLogFileName();
        if (TextUtils.isEmpty(logFile)) {
            logFile = "app.log";
        }
        cmd.append(" > ").append(logFile).append(" 2>&1 &");
        
        // 保存PID
        cmd.append(" echo $! > .pid");
        
        return cmd.toString();
    }
    
    // 生成Kill命令
    public static String generateKillCommand(RunConfiguration config) { /* 生成进程终止命令 */ }
    
    // 完整命令(包含Kill+Run)
    public static String buildCompleteCommand(RunConfiguration config) { /* 生成完整命令 */ }
}
```

#### 4.2 路径收藏辅助类 - PathBookmarkHelper.java  
```java
package com.termux.app.configuration.utils;

public class PathBookmarkHelper {
    
    // 获取指定SSH连接的收藏路径
    public static List<DirectoryBookmark> getBookmarksBySSHConfig(Context context, String sshConfigId) {
        ProjectWorkspaceManager workspaceManager = ProjectWorkspaceManager.getInstance(context);
        
        // 根据SSH配置ID查找对应的工作区
        List<ProjectWorkspace> workspaces = workspaceManager.loadAllWorkspaces();
        for (ProjectWorkspace workspace : workspaces) {
            if (workspace.getConnection() != null && 
                sshConfigId.equals(workspace.getConnection().getName())) {
                return workspaceManager.getProjectBookmarks(workspace.getId());
            }
        }
        
        return new ArrayList<>();
    }
    
    // 按SSH分组收藏路径
    public static Map<String, List<DirectoryBookmark>> groupBookmarksBySSH(Context context) { /* 分组收藏路径 */ }
    
    // 获取路径显示名称
    public static String getPathDisplayName(DirectoryBookmark bookmark) { /* 获取显示名称 */ }
}
```

#### 4.3 常量定义 - ConfigurationConstants.java
```java
package com.termux.app.configuration.utils;

public class ConfigurationConstants {
    
    // SharedPreferences相关
    public static final String PREFS_RUN_CONFIG = "run_configurations";
    public static final String PREFS_COMMAND_TEMPLATES = "command_templates";
    public static final String PREFS_CONFIG_SETTINGS = "config_settings";
    
    // 配置项键值
    public static final String KEY_RUN_CONFIG_PREFIX = "run_config_";
    public static final String KEY_TEMPLATE_PREFIX = "template_";
    public static final String KEY_LAST_USED = "last_used_config";
    
    // 默认值
    public static final String DEFAULT_LOG_FILE = "app.log";
    public static final String DEFAULT_WORKING_DIR = ".";
    public static final int MAX_RECENT_CONFIGS = 10;
    
    // 请求码
    public static final int REQUEST_CODE_SSH_CONFIG = 1001;
    public static final int REQUEST_CODE_RUN_CONFIG = 1002;
    
    // Bundle键值
    public static final String BUNDLE_CONFIG_ID = "config_id";
    public static final String BUNDLE_SSH_CONFIG_ID = "ssh_config_id";
    public static final String BUNDLE_IS_EDIT_MODE = "is_edit_mode";
    
    // 语言类型映射
    public static final Map<String, LanguageType> LANGUAGE_MAP = new HashMap<String, LanguageType>() {{
        put("js", LanguageType.NODEJS);
        put("ts", LanguageType.NODEJS);
        put("vue", LanguageType.NODEJS);
        put("py", LanguageType.PYTHON);
        put("java", LanguageType.JAVA);
        put("go", LanguageType.GO);
    }};
}
```

### 5. 创建基础Fragment框架

#### 5.1 配置主页Fragment - ConfigurationMainFragment.java
```java
package com.termux.app.configuration.fragments;

public class ConfigurationMainFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConfigurationMainAdapter adapter;
    private ConfigNavigationManager navigationManager;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuration_main, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupNavigation();
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_configuration_items);
    }
    
    private void setupRecyclerView() {
        adapter = new ConfigurationMainAdapter(requireContext());
        adapter.setOnItemClickListener(this::onConfigurationItemClicked);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        
        // 设置配置项数据
        adapter.setItems(Arrays.asList(ConfigurationItem.DEFAULT_ITEMS));
    }
    
    private void onConfigurationItemClicked(ConfigurationItem item) {
        switch (item.getType()) {
            case SSH_CONFIG:
                navigationManager.navigateToSSHConfigList();
                break;
            case RUN_CONFIG:  
                navigationManager.navigateToRunConfigList();
                break;
            case GLOBAL_SETTINGS:
                // TODO: 导航到全局设置
                break;
            case ABOUT:
                // TODO: 显示关于信息
                break;
        }
    }
}
```

### 6. 创建布局文件

#### 6.1 配置主页布局 - fragment_configuration_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="?android:attr/colorBackground">

    <!-- 标题 -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="配置管理"
        android:textSize="24sp"
        android:textStyle="bold"
        android:textColor="?attr/colorOnSurface"
        android:layout_marginBottom="16dp"
        android:gravity="center" />

    <!-- 配置项列表 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_configuration_items"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

#### 6.2 配置项布局 - item_configuration_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="8dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:focusable="true"
    android:foreground="?android:attr/selectableItemBackground">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">

        <ImageView
            android:id="@+id/iv_icon"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@drawable/ic_settings"
            android:tint="?attr/colorPrimary" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="16dp">

            <TextView
                android:id="@+id/tv_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="配置项标题"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="?attr/colorOnSurface" />

            <TextView
                android:id="@+id/tv_description"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="配置项描述信息"
                android:textSize="14sp"
                android:textColor="?attr/colorOnSurfaceVariant"
                android:layout_marginTop="4dp" />

        </LinearLayout>

        <ImageView
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:src="@drawable/ic_chevron_right"
            android:tint="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

### 7. 更新MainTabActivity

需要将现有的`SettingsFragment`替换为新的`ConfigurationMainFragment`:

```java
// 在MainTabActivity的createFragment方法中
case 3:
    return new ConfigurationMainFragment(); // 替换原来的SettingsFragment
```

## ⚡ 实施步骤

1. **创建目录结构**: 建立`configuration`包及其子包
2. **迁移现有代码**: 将`SettingsFragment`重构为`ConfigurationMainFragment`
3. **实现数据模型**: 创建所有核心数据模型类
4. **开发管理器**: 实现各种业务管理器和工具类
5. **创建布局文件**: 设计配置主页和列表项布局
6. **集成测试**: 确保配置主页可以正常显示和导航

## 📊 验收标准

1. ✅ 配置主页能够正常显示四个配置项
2. ✅ 点击配置项能够触发导航事件(暂时可以是Toast提示)
3. ✅ 所有数据模型类创建完成且能正常序列化
4. ✅ 管理器类的基础CRUD方法实现并测试通过
5. ✅ 工具类和常量定义完整且符合规范
6. ✅ 布局文件遵循Material Design规范
7. ✅ 代码注释完善，遵循项目编码规范

## 🔍 注意事项

- 保持与现有代码架构的一致性
- 复用现有的SSH配置管理逻辑，避免重复开发
- 确保数据模型的向前兼容性
- 预留扩展接口以便后续功能添加
- 所有硬编码字符串需要提取到strings.xml
- 遵循Android开发最佳实践和性能优化建议

---

*本阶段完成后将为Phase 2的具体功能开发奠定坚实的基础架构。*
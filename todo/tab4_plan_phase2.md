# Phase 2: Configuration配置页面开发 (≤12w tokens)

## 🎯 阶段目标
基于Phase 1建立的基础架构，完成Configuration配置管理模块的完整界面开发，包括SSH配置管理、运行配置管理、路径选择集成等核心功能。

## 📋 具体任务列表

### 1. SSH配置管理页面

#### 1.1 SSH配置列表Fragment - SshConfigListFragment.java
**目标**: 复用现有SSH配置逻辑，创建列表管理界面

```java
package com.termux.app.configuration.fragments;

public class SshConfigListFragment extends Fragment {
    private RecyclerView recyclerView;
    private SshConfigAdapter adapter;
    private ConfigNavigationManager navigationManager;
    private SSHConfigManager sshConfigManager;
    private MaterialButton btnNewConfig;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssh_config_list, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadSSHConfigs();
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_ssh_configs);
        btnNewConfig = view.findViewById(R.id.btn_new_ssh_config);
        sshConfigManager = SSHConfigManager.getInstance(requireContext());
    }
    
    private void setupRecyclerView() {
        adapter = new SshConfigAdapter(requireContext());
        adapter.setOnConfigActionListener(new SshConfigAdapter.OnConfigActionListener() {
            @Override
            public void onEditConfig(SSHConnectionConfig config) {
                navigationManager.navigateToSSHConfigDetail(config.getId());
            }
            
            @Override
            public void onDeleteConfig(SSHConnectionConfig config) {
                showDeleteConfirmDialog(config);
            }
            
            @Override
            public void onTestConnection(SSHConnectionConfig config) {
                testSSHConnection(config);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadSSHConfigs() {
        List<SSHConnectionConfig> configs = sshConfigManager.getAllConfigs();
        adapter.setConfigs(configs);
    }
    
    // 测试SSH连接
    private void testSSHConnection(SSHConnectionConfig config) {
        // TODO: 实现连接测试逻辑
        Toast.makeText(getContext(), "正在测试连接到 " + config.getHost() + "...", Toast.LENGTH_SHORT).show();
    }
    
    // 删除确认对话框
    private void showDeleteConfirmDialog(SSHConnectionConfig config) {
        new AlertDialog.Builder(requireContext())
            .setTitle("删除SSH配置")
            .setMessage("确定要删除配置 \"" + config.getName() + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> deleteConfig(config))
            .setNegativeButton("取消", null)
            .show();
    }
}
```

#### 1.2 SSH配置详情Fragment - SshConfigDetailFragment.java
**目标**: 新建/编辑SSH配置的详情页面

```java
package com.termux.app.configuration.fragments;

public class SshConfigDetailFragment extends Fragment {
    private EditText etConfigName, etHost, etPort, etUsername, etPassword;
    private Switch swUseKeyAuth;
    private EditText etPrivateKeyPath;
    private MaterialButton btnSave, btnTest, btnCancel;
    
    private SSHConnectionConfig currentConfig;
    private boolean isEditMode;
    private String configId;
    
    public static SshConfigDetailFragment newInstance(String configId) {
        SshConfigDetailFragment fragment = new SshConfigDetailFragment();
        Bundle args = new Bundle();
        args.putString(ConfigurationConstants.BUNDLE_CONFIG_ID, configId);
        args.putBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, configId != null);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle args = getArguments();
        if (args != null) {
            configId = args.getString(ConfigurationConstants.BUNDLE_CONFIG_ID);
            isEditMode = args.getBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, false);
        }
        
        if (isEditMode && configId != null) {
            loadExistingConfig();
        }
    }
    
    private void loadExistingConfig() {
        SSHConfigManager manager = SSHConfigManager.getInstance(requireContext());
        currentConfig = manager.getConfig(configId);
        if (currentConfig != null) {
            populateFields();
        }
    }
    
    private void populateFields() {
        etConfigName.setText(currentConfig.getName());
        etHost.setText(currentConfig.getHost());
        etPort.setText(String.valueOf(currentConfig.getPort()));
        etUsername.setText(currentConfig.getUsername());
        // ... 填充其他字段
    }
    
    private void saveConfig() {
        if (!validateInput()) {
            return;
        }
        
        SSHConnectionConfig config = buildConfigFromInput();
        SSHConfigManager manager = SSHConfigManager.getInstance(requireContext());
        
        boolean success = isEditMode ? 
            manager.updateConfig(config) : 
            manager.saveConfig(config);
            
        if (success) {
            Toast.makeText(getContext(), "配置保存成功", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        } else {
            Toast.makeText(getContext(), "配置保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}
```

### 2. 运行配置管理页面

#### 2.1 运行配置列表Fragment - RunConfigListFragment.java
**目标**: 显示和管理所有运行配置

```java
package com.termux.app.configuration.fragments;

public class RunConfigListFragment extends Fragment {
    private RecyclerView recyclerView;
    private RunConfigAdapter adapter;
    private RunConfigurationManager configManager;
    private MaterialButton btnNewConfig;
    private TextView tvEmptyState;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_run_config_list, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadRunConfigs();
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_run_configs);
        btnNewConfig = view.findViewById(R.id.btn_new_run_config);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        configManager = RunConfigurationManager.getInstance(requireContext());
    }
    
    private void setupRecyclerView() {
        adapter = new RunConfigAdapter(requireContext());
        adapter.setOnConfigActionListener(new RunConfigAdapter.OnConfigActionListener() {
            @Override
            public void onEditConfig(RunConfiguration config) {
                navigationManager.navigateToRunConfigDetail(config.getId());
            }
            
            @Override
            public void onDeleteConfig(RunConfiguration config) {
                showDeleteConfirmDialog(config);
            }
            
            @Override
            public void onCopyCommand(RunConfiguration config) {
                copyCommandToClipboard(config);
            }
            
            @Override
            public void onQuickRun(RunConfiguration config) {
                // TODO: 实现快速运行功能
                Toast.makeText(getContext(), "快速运行: " + config.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadRunConfigs() {
        List<RunConfiguration> configs = configManager.getAllConfigurations();
        adapter.setConfigurations(configs);
        
        // 显示/隐藏空状态
        boolean isEmpty = configs.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
    
    private void copyCommandToClipboard(RunConfiguration config) {
        String command = CommandBuilder.buildCompleteCommand(config);
        ShareUtils.copyTextToClipboard(getContext(), command, "运行命令已复制到剪贴板");
    }
}
```

#### 2.2 运行配置详情Fragment - RunConfigDetailFragment.java
**目标**: 新建/编辑运行配置的完整表单页面

```java
package com.termux.app.configuration.fragments;

public class RunConfigDetailFragment extends Fragment {
    // UI组件
    private EditText etConfigName, etCommand, etWorkingDir, etEnvVariables, etLogFileName;
    private Spinner spSSHConfig, spLanguageType, spProjectPath;
    private Switch swRunInBackground;
    private TextView tvCommandPreview;
    private MaterialButton btnSave, btnCancel, btnPreview;
    
    // 数据和管理器
    private RunConfiguration currentConfig;
    private RunConfigurationManager configManager;
    private SSHConfigManager sshConfigManager;
    private boolean isEditMode;
    private String configId;
    
    // 适配器
    private ArrayAdapter<SSHConnectionConfig> sshAdapter;
    private ArrayAdapter<LanguageType> languageAdapter;
    private PathBookmarkSpinnerAdapter pathAdapter;
    
    public static RunConfigDetailFragment newInstance(String configId) {
        RunConfigDetailFragment fragment = new RunConfigDetailFragment();
        Bundle args = new Bundle();
        args.putString(ConfigurationConstants.BUNDLE_CONFIG_ID, configId);
        args.putBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, configId != null);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_run_config_detail, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupSpinners();
        setupClickListeners();
        setupTextWatchers();
        
        if (isEditMode) {
            loadExistingConfig();
        } else {
            setDefaultValues();
        }
    }
    
    private void initViews(View view) {
        etConfigName = view.findViewById(R.id.et_config_name);
        etCommand = view.findViewById(R.id.et_command);
        etWorkingDir = view.findViewById(R.id.et_working_dir);
        etEnvVariables = view.findViewById(R.id.et_env_variables);
        etLogFileName = view.findViewById(R.id.et_log_file_name);
        spSSHConfig = view.findViewById(R.id.sp_ssh_config);
        spLanguageType = view.findViewById(R.id.sp_language_type);
        spProjectPath = view.findViewById(R.id.sp_project_path);
        swRunInBackground = view.findViewById(R.id.sw_run_in_background);
        tvCommandPreview = view.findViewById(R.id.tv_command_preview);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnPreview = view.findViewById(R.id.btn_preview);
        
        configManager = RunConfigurationManager.getInstance(requireContext());
        sshConfigManager = SSHConfigManager.getInstance(requireContext());
    }
    
    private void setupSpinners() {
        // SSH配置下拉框
        setupSSHConfigSpinner();
        
        // 语言类型下拉框
        setupLanguageTypeSpinner();
        
        // 项目路径下拉框 - 根据选择的SSH配置动态更新
        setupProjectPathSpinner();
    }
    
    private void setupSSHConfigSpinner() {
        List<SSHConnectionConfig> sshConfigs = sshConfigManager.getAllConfigs();
        sshAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sshConfigs);
        sshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSSHConfig.setAdapter(sshAdapter);
        
        spSSHConfig.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SSHConnectionConfig selectedSSH = sshAdapter.getItem(position);
                if (selectedSSH != null) {
                    updateProjectPathOptions(selectedSSH.getId());
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupLanguageTypeSpinner() {
        List<LanguageType> languageTypes = Arrays.asList(LanguageType.values());
        languageAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languageTypes);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLanguageType.setAdapter(languageAdapter);
        
        spLanguageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LanguageType selectedType = languageAdapter.getItem(position);
                if (selectedType != null) {
                    updateCommandSuggestions(selectedType);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupProjectPathSpinner() {
        pathAdapter = new PathBookmarkSpinnerAdapter(requireContext());
        spProjectPath.setAdapter(pathAdapter);
    }
    
    // 根据选择的SSH配置更新项目路径选项
    private void updateProjectPathOptions(String sshConfigId) {
        List<DirectoryBookmark> bookmarks = PathBookmarkHelper.getBookmarksBySSHConfig(requireContext(), sshConfigId);
        pathAdapter.setBookmarks(bookmarks);
        
        if (bookmarks.isEmpty()) {
            // 如果没有收藏路径，显示提示信息
            Toast.makeText(getContext(), "该SSH连接暂无收藏路径，请先在文件浏览器中添加收藏", Toast.LENGTH_LONG).show();
        }
    }
    
    // 根据选择的语言类型更新命令建议
    private void updateCommandSuggestions(LanguageType languageType) {
        CommandTemplateManager templateManager = CommandTemplateManager.getInstance(requireContext());
        List<String> commonCommands = templateManager.getCommonCommands(languageType);
        
        if (!commonCommands.isEmpty() && TextUtils.isEmpty(etCommand.getText().toString())) {
            // 如果当前命令为空，使用第一个常用命令作为默认值
            etCommand.setText(commonCommands.get(0));
        }
        
        // TODO: 可以显示命令建议下拉菜单
    }
    
    private void setupTextWatchers() {
        // 监听输入变化，实时更新命令预览
        TextWatcher previewUpdater = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                updateCommandPreview();
            }
        };
        
        etCommand.addTextChangedListener(previewUpdater);
        etWorkingDir.addTextChangedListener(previewUpdater);
        etEnvVariables.addTextChangedListener(previewUpdater);
        etLogFileName.addTextChangedListener(previewUpdater);
    }
    
    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveConfiguration());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        btnPreview.setOnClickListener(v -> showCommandPreviewDialog());
        
        swRunInBackground.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etLogFileName.setEnabled(isChecked);
            updateCommandPreview();
        });
    }
    
    private void updateCommandPreview() {
        try {
            RunConfiguration tempConfig = buildConfigurationFromInput();
            if (tempConfig.isValid()) {
                String command = CommandBuilder.buildCompleteCommand(tempConfig);
                tvCommandPreview.setText(command);
                tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_success));
            } else {
                tvCommandPreview.setText("请完善配置信息");
                tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_warning));
            }
        } catch (Exception e) {
            tvCommandPreview.setText("配置信息有误: " + e.getMessage());
            tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_error));
        }
    }
    
    private void showCommandPreviewDialog() {
        String command = tvCommandPreview.getText().toString();
        
        new AlertDialog.Builder(requireContext())
            .setTitle("命令预览")
            .setMessage(command)
            .setPositiveButton("复制", (dialog, which) -> {
                ShareUtils.copyTextToClipboard(getContext(), command, "命令已复制到剪贴板");
            })
            .setNegativeButton("关闭", null)
            .show();
    }
    
    private boolean validateInput() {
        // 验证必填字段
        if (TextUtils.isEmpty(etConfigName.getText())) {
            etConfigName.setError("请输入配置名称");
            return false;
        }
        
        if (spSSHConfig.getSelectedItem() == null) {
            Toast.makeText(getContext(), "请选择SSH配置", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (spProjectPath.getSelectedItem() == null) {
            Toast.makeText(getContext(), "请选择项目路径", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (TextUtils.isEmpty(etCommand.getText())) {
            etCommand.setError("请输入运行命令");
            return false;
        }
        
        // 验证名称唯一性
        String name = etConfigName.getText().toString().trim();
        String excludeId = isEditMode ? configId : null;
        if (configManager.isNameExists(name, excludeId)) {
            etConfigName.setError("配置名称已存在");
            return false;
        }
        
        return true;
    }
    
    private RunConfiguration buildConfigurationFromInput() {
        RunConfiguration config = new RunConfiguration();
        
        if (isEditMode) {
            config.setId(configId);
        } else {
            config.setId(UUID.randomUUID().toString());
            config.setCreatedTime(System.currentTimeMillis());
        }
        
        config.setName(etConfigName.getText().toString().trim());
        
        SSHConnectionConfig selectedSSH = (SSHConnectionConfig) spSSHConfig.getSelectedItem();
        config.setSshConfigId(selectedSSH != null ? selectedSSH.getId() : null);
        
        DirectoryBookmark selectedPath = (DirectoryBookmark) spProjectPath.getSelectedItem();
        config.setProjectPath(selectedPath != null ? selectedPath.getFullPath() : null);
        
        LanguageType selectedType = (LanguageType) spLanguageType.getSelectedItem();
        config.setLanguageType(selectedType != null ? selectedType.name() : LanguageType.CUSTOM.name());
        
        config.setCommand(etCommand.getText().toString().trim());
        config.setWorkingDir(etWorkingDir.getText().toString().trim());
        config.setEnvVariables(etEnvVariables.getText().toString().trim());
        config.setRunInBackground(swRunInBackground.isChecked());
        config.setLogFileName(etLogFileName.getText().toString().trim());
        
        return config;
    }
    
    private void saveConfiguration() {
        if (!validateInput()) {
            return;
        }
        
        RunConfiguration config = buildConfigurationFromInput();
        
        boolean success = configManager.saveConfiguration(config);
        if (success) {
            Toast.makeText(getContext(), "配置保存成功", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        } else {
            Toast.makeText(getContext(), "配置保存失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadExistingConfig() {
        currentConfig = configManager.getConfiguration(configId);
        if (currentConfig != null) {
            populateFieldsFromConfig();
        }
    }
    
    private void populateFieldsFromConfig() {
        etConfigName.setText(currentConfig.getName());
        etCommand.setText(currentConfig.getCommand());
        etWorkingDir.setText(currentConfig.getWorkingDir());
        etEnvVariables.setText(currentConfig.getEnvVariables());
        etLogFileName.setText(currentConfig.getLogFileName());
        swRunInBackground.setChecked(currentConfig.isRunInBackground());
        
        // 设置Spinner选中项
        setSpinnerSelection(spSSHConfig, currentConfig.getSshConfigId());
        setSpinnerSelection(spLanguageType, LanguageType.valueOf(currentConfig.getLanguageType()));
        // ... 其他Spinner设置
        
        updateCommandPreview();
    }
    
    private void setDefaultValues() {
        etWorkingDir.setText(ConfigurationConstants.DEFAULT_WORKING_DIR);
        etLogFileName.setText(ConfigurationConstants.DEFAULT_LOG_FILE);
        swRunInBackground.setChecked(true);
        
        updateCommandPreview();
    }
}
```

### 3. 创建适配器类

#### 3.1 配置主页适配器 - ConfigurationMainAdapter.java
```java
package com.termux.app.configuration.adapters;

public class ConfigurationMainAdapter extends RecyclerView.Adapter<ConfigurationMainAdapter.ViewHolder> {
    private List<ConfigurationItem> items;
    private Context context;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(ConfigurationItem item);
    }
    
    public ConfigurationMainAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_configuration_main, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfigurationItem item = items.get(position);
        
        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.ivIcon.setImageResource(item.getIconRes());
        
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void setItems(List<ConfigurationItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        ImageView ivIcon;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }
}
```

#### 3.2 运行配置适配器 - RunConfigAdapter.java
```java
package com.termux.app.configuration.adapters;

public class RunConfigAdapter extends RecyclerView.Adapter<RunConfigAdapter.ViewHolder> {
    private List<RunConfiguration> configurations;
    private Context context;
    private OnConfigActionListener actionListener;
    
    public interface OnConfigActionListener {
        void onEditConfig(RunConfiguration config);
        void onDeleteConfig(RunConfiguration config);
        void onCopyCommand(RunConfiguration config);
        void onQuickRun(RunConfiguration config);
    }
    
    // ... 标准RecyclerView适配器实现
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunConfiguration config = configurations.get(position);
        
        holder.tvConfigName.setText(config.getName());
        holder.tvLanguageType.setText(config.getLanguageType());
        holder.tvCommand.setText(config.getCommand());
        holder.tvLastUsed.setText(formatLastUsedTime(config.getLastUsedTime()));
        
        // 设置点击事件
        holder.btnEdit.setOnClickListener(v -> actionListener.onEditConfig(config));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteConfig(config));
        holder.btnCopyCommand.setOnClickListener(v -> actionListener.onCopyCommand(config));
        holder.btnQuickRun.setOnClickListener(v -> actionListener.onQuickRun(config));
    }
    
    private String formatLastUsedTime(long timestamp) {
        if (timestamp == 0) return "从未使用";
        
        long diff = System.currentTimeMillis() - timestamp;
        long hours = diff / (1000 * 60 * 60);
        
        if (hours < 1) return "刚刚使用";
        if (hours < 24) return hours + "小时前";
        
        long days = hours / 24;
        return days + "天前";
    }
}
```

#### 3.3 路径收藏下拉适配器 - PathBookmarkSpinnerAdapter.java
```java
package com.termux.app.configuration.adapters;

public class PathBookmarkSpinnerAdapter extends ArrayAdapter<DirectoryBookmark> {
    private List<DirectoryBookmark> bookmarks;
    
    public PathBookmarkSpinnerAdapter(Context context) {
        super(context, android.R.layout.simple_spinner_item);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.bookmarks = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view;
        
        DirectoryBookmark bookmark = getItem(position);
        if (bookmark != null) {
            String displayText = bookmark.getDisplayName() + "\n" + bookmark.getFullPath();
            textView.setText(displayText);
        }
        
        return view;
    }
    
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = (TextView) view;
        
        DirectoryBookmark bookmark = getItem(position);
        if (bookmark != null) {
            textView.setText(bookmark.getDisplayName() + " - " + bookmark.getFullPath());
        }
        
        return view;
    }
    
    public void setBookmarks(List<DirectoryBookmark> bookmarks) {
        this.bookmarks = bookmarks;
        clear();
        addAll(bookmarks);
        notifyDataSetChanged();
    }
}
```

### 4. 创建布局文件

#### 4.1 SSH配置列表布局 - fragment_ssh_config_list.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- 标题栏 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="SSH连接配置"
            android:textSize="20sp"
            android:textStyle="bold" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_new_ssh_config"
            style="@style/Widget.MaterialComponents.Button.Icon"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="新建"
            app:icon="@drawable/ic_add" />

    </LinearLayout>

    <!-- SSH配置列表 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_ssh_configs"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

#### 4.2 运行配置详情布局 - fragment_run_config_detail.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- 基本信息 -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="8dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="基本信息"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="16dp" />

                <!-- 配置名称 -->
                <com.google.android.material.textfield.TextInputLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp"
                    app:helperText="用于识别此配置的名称">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et_config_name"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="配置名称" />

                </com.google.android.material.textfield.TextInputLayout>

                <!-- SSH连接 -->
                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="SSH连接"
                    android:layout_marginBottom="8dp" />

                <Spinner
                    android:id="@+id/sp_ssh_config"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp" />

                <!-- 项目路径 -->
                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="项目路径"
                    android:layout_marginBottom="8dp" />

                <Spinner
                    android:id="@+id/sp_project_path"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp" />

            </LinearLayout>

        </com.google.android.material.card.MaterialCardView>

        <!-- 运行配置 -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="8dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="运行配置"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="16dp" />

                <!-- 语言类型 -->
                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="语言类型"
                    android:layout_marginBottom="8dp" />

                <Spinner
                    android:id="@+id/sp_language_type"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp" />

                <!-- 运行命令 -->
                <com.google.android.material.textfield.TextInputLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp"
                    app:helperText="例如: npm run dev, python app.py">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et_command"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="运行命令" />

                </com.google.android.material.textfield.TextInputLayout>

                <!-- 工作目录 -->
                <com.google.android.material.textfield.TextInputLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp"
                    app:helperText="相对于项目路径的工作目录">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et_working_dir"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="工作目录 (默认: .)" />

                </com.google.android.material.textfield.TextInputLayout>

                <!-- 环境变量 -->
                <com.google.android.material.textfield.TextInputLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="16dp"
                    app:helperText="例如: NODE_ENV=production PORT=3000">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et_env_variables"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="环境变量 (可选)" />

                </com.google.android.material.textfield.TextInputLayout>

                <!-- 后台运行选项 -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginBottom="12dp">

                    <com.google.android.material.switchmaterial.SwitchMaterial
                        android:id="@+id/sw_run_in_background"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="后台运行" />

                    <View
                        android:layout_width="0dp"
                        android:layout_height="1dp"
                        android:layout_weight="1" />

                </LinearLayout>

                <!-- 日志文件名 -->
                <com.google.android.material.textfield.TextInputLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    app:helperText="后台运行时的日志文件名">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/et_log_file_name"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="日志文件名"
                        android:enabled="false" />

                </com.google.android.material.textfield.TextInputLayout>

            </LinearLayout>

        </com.google.android.material.card.MaterialCardView>

        <!-- 命令预览 -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="8dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginBottom="12dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="命令预览"
                        android:textSize="18sp"
                        android:textStyle="bold" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_preview"
                        style="@style/Widget.MaterialComponents.Button.TextButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="详情" />

                </LinearLayout>

                <TextView
                    android:id="@+id/tv_command_preview"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="12dp"
                    android:background="@drawable/bg_code_block"
                    android:fontFamily="monospace"
                    android:text="请完善配置信息"
                    android:textSize="12sp" />

            </LinearLayout>

        </com.google.android.material.card.MaterialCardView>

        <!-- 操作按钮 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_cancel"
                style="@style/Widget.MaterialComponents.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="取消"
                android:layout_marginEnd="8dp" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_save"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="保存配置" />

        </LinearLayout>

    </LinearLayout>

</ScrollView>
```

### 5. 完善导航管理器

更新`ConfigNavigationManager`以支持所有页面导航:

```java
public class ConfigNavigationManager {
    // 添加新的导航方法
    public void navigateToSSHConfigList() {
        SshConfigListFragment fragment = new SshConfigListFragment();
        replaceFragment(fragment, true);
    }
    
    public void navigateToRunConfigList() {
        RunConfigListFragment fragment = new RunConfigListFragment();
        replaceFragment(fragment, true);
    }
    
    public void navigateToRunConfigDetail(String configId) {
        RunConfigDetailFragment fragment = RunConfigDetailFragment.newInstance(configId);
        replaceFragment(fragment, true);
    }
    
    // ... 其他导航方法
}
```

## ⚡ 实施步骤

1. **创建Fragment骨架**: 先创建所有Fragment的基础结构
2. **实现适配器**: 完成各种RecyclerView和Spinner适配器
3. **创建布局文件**: 设计所有页面的XML布局
4. **集成数据管理**: 连接Fragment与Manager进行数据操作
5. **实现页面导航**: 完善导航管理器和页面跳转
6. **添加交互功能**: 实现表单验证、数据保存、命令预览等
7. **优化用户体验**: 添加动画、提示信息、错误处理

## 📊 验收标准

1. ✅ 配置主页能够正确导航到各个二级页面
2. ✅ SSH配置管理页面能够增删改查SSH配置
3. ✅ 运行配置管理页面功能完整且界面友好
4. ✅ 路径选择能够按SSH配置正确分组显示
5. ✅ 命令预览功能实时更新且格式正确
6. ✅ 表单验证完善，错误提示清晰
7. ✅ 所有数据操作能够正确持久化
8. ✅ 界面遵循Material Design规范

## 🔍 注意事项

- 确保SSH配置与现有逻辑完全兼容
- 路径收藏功能要与Tab2的文件浏览器数据同步
- 命令预览要考虑各种边界情况和错误状态
- 所有用户输入都需要进行有效性验证
- 保持良好的用户体验，及时的状态反馈

---

*本阶段完成后将拥有一个功能完整的配置管理界面，为Phase 3的悬浮按钮开发提供数据基础。*
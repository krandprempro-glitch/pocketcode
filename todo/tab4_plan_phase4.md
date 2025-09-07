# Phase 4: 命令执行系统集成 (≤12w tokens)

## 🎯 阶段目标
完成命令生成、SSH远程执行、状态反馈等核心功能，实现Configuration和Floating模块之间的数据桥接，让用户能够通过悬浮按钮一键执行运行配置。

## 📋 具体任务列表

### 1. 命令执行对话框系统

#### 1.1 运行配置选择对话框 - RunConfigSelectionDialog.java
**目标**: 在悬浮按钮菜单中选择要执行的运行配置

```java
package com.termux.app.floating.dialogs;

public class RunConfigSelectionDialog extends Dialog {
    private RecyclerView recyclerView;
    private RunConfigSelectionAdapter adapter;
    private RunConfigurationManager configManager;
    private TextView tvEmptyState;
    private MaterialButton btnNewConfig;
    private OnConfigSelectedListener configSelectedListener;
    
    public interface OnConfigSelectedListener {
        void onConfigSelected(RunConfiguration config);
        void onNewConfigRequested();
    }
    
    public RunConfigSelectionDialog(Context context) {
        super(context, R.style.Theme_FloatingDialog);
        init();
    }
    
    private void init() {
        setContentView(R.layout.dialog_run_config_selection);
        
        // 设置对话框属性
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        
        initViews();
        setupRecyclerView();
        loadConfigurations();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.rv_run_configs);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnNewConfig = findViewById(R.id.btn_new_config);
        
        configManager = RunConfigurationManager.getInstance(getContext());
        
        btnNewConfig.setOnClickListener(v -> {
            if (configSelectedListener != null) {
                configSelectedListener.onNewConfigRequested();
            }
            dismiss();
        });
    }
    
    private void setupRecyclerView() {
        adapter = new RunConfigSelectionAdapter(getContext());
        adapter.setOnConfigClickListener(config -> {
            if (configSelectedListener != null) {
                configSelectedListener.onConfigSelected(config);
            }
            dismiss();
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadConfigurations() {
        List<RunConfiguration> configs = configManager.getAllConfigurations();
        
        // 按最近使用时间排序
        configs.sort((a, b) -> Long.compare(b.getLastUsedTime(), a.getLastUsedTime()));
        
        adapter.setConfigurations(configs);
        
        // 显示/隐藏空状态
        boolean isEmpty = configs.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
    
    public void setOnConfigSelectedListener(OnConfigSelectedListener listener) {
        this.configSelectedListener = listener;
    }
}
```

#### 1.2 执行确认对话框 - ExecutionConfirmDialog.java
**目标**: 显示将要执行的命令，让用户确认执行

```java
package com.termux.app.floating.dialogs;

public class ExecutionConfirmDialog extends Dialog {
    private TextView tvConfigName, tvSSHConnection, tvProjectPath, tvCommandPreview;
    private Switch swKillPrevious, swRunInBackground;
    private MaterialButton btnExecute, btnCopyCommand, btnCancel;
    private RunConfiguration configuration;
    private OnExecutionActionListener actionListener;
    
    public interface OnExecutionActionListener {
        void onExecuteCommand(RunConfiguration config, String command);
        void onCopyCommand(String command);
    }
    
    public ExecutionConfirmDialog(Context context, RunConfiguration config) {
        super(context, R.style.Theme_FloatingDialog);
        this.configuration = config;
        init();
    }
    
    private void init() {
        setContentView(R.layout.dialog_execution_confirm);
        
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        
        initViews();
        populateData();
        setupListeners();
    }
    
    private void initViews() {
        tvConfigName = findViewById(R.id.tv_config_name);
        tvSSHConnection = findViewById(R.id.tv_ssh_connection);
        tvProjectPath = findViewById(R.id.tv_project_path);
        tvCommandPreview = findViewById(R.id.tv_command_preview);
        swKillPrevious = findViewById(R.id.sw_kill_previous);
        swRunInBackground = findViewById(R.id.sw_run_in_background);
        btnExecute = findViewById(R.id.btn_execute);
        btnCopyCommand = findViewById(R.id.btn_copy_command);
        btnCancel = findViewById(R.id.btn_cancel);
    }
    
    private void populateData() {
        tvConfigName.setText(configuration.getName());
        
        // 获取SSH配置信息
        SSHConfigManager sshManager = SSHConfigManager.getInstance(getContext());
        SSHConnectionConfig sshConfig = sshManager.getConfig(configuration.getSshConfigId());
        if (sshConfig != null) {
            tvSSHConnection.setText(sshConfig.getName() + " (" + sshConfig.getHost() + ")");
        }
        
        tvProjectPath.setText(configuration.getProjectPath());
        
        // 设置默认选项
        swKillPrevious.setChecked(true);
        swRunInBackground.setChecked(configuration.isRunInBackground());
        
        updateCommandPreview();
    }
    
    private void setupListeners() {
        btnExecute.setOnClickListener(v -> executeCommand());
        btnCopyCommand.setOnClickListener(v -> copyCommand());
        btnCancel.setOnClickListener(v -> dismiss());
        
        // 监听开关变化，更新命令预览
        swKillPrevious.setOnCheckedChangeListener((buttonView, isChecked) -> updateCommandPreview());
        swRunInBackground.setOnCheckedChangeListener((buttonView, isChecked) -> updateCommandPreview());
    }
    
    private void updateCommandPreview() {
        String command = generateCommand();
        tvCommandPreview.setText(command);
    }
    
    private String generateCommand() {
        // 根据当前选项生成完整命令
        RunConfiguration tempConfig = configuration.copy();
        tempConfig.setRunInBackground(swRunInBackground.isChecked());
        
        if (swKillPrevious.isChecked()) {
            return CommandBuilder.buildCompleteCommand(tempConfig);
        } else {
            return CommandBuilder.buildBackgroundCommand(tempConfig);
        }
    }
    
    private void executeCommand() {
        String command = generateCommand();
        if (actionListener != null) {
            actionListener.onExecuteCommand(configuration, command);
        }
        dismiss();
    }
    
    private void copyCommand() {
        String command = generateCommand();
        ShareUtils.copyTextToClipboard(getContext(), command, "命令已复制到剪贴板");
        
        if (actionListener != null) {
            actionListener.onCopyCommand(command);
        }
    }
    
    public void setOnExecutionActionListener(OnExecutionActionListener listener) {
        this.actionListener = listener;
    }
}
```

#### 1.3 执行结果对话框 - ExecutionResultDialog.java
**目标**: 显示命令执行的结果和状态

```java
package com.termux.app.floating.dialogs;

public class ExecutionResultDialog extends Dialog {
    private ImageView ivResultIcon;
    private TextView tvResultTitle, tvResultMessage, tvCommandOutput;
    private MaterialButton btnViewLog, btnReExecute, btnClose;
    private ProgressBar progressBar;
    private ExecutionResult executionResult;
    private OnResultActionListener actionListener;
    
    public interface OnResultActionListener {
        void onViewLogRequested(String logFilePath);
        void onReExecuteRequested();
    }
    
    public ExecutionResultDialog(Context context, ExecutionResult result) {
        super(context, R.style.Theme_FloatingDialog);
        this.executionResult = result;
        init();
    }
    
    private void init() {
        setContentView(R.layout.dialog_execution_result);
        
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        
        initViews();
        displayResult();
        setupListeners();
    }
    
    private void initViews() {
        ivResultIcon = findViewById(R.id.iv_result_icon);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultMessage = findViewById(R.id.tv_result_message);
        tvCommandOutput = findViewById(R.id.tv_command_output);
        btnViewLog = findViewById(R.id.btn_view_log);
        btnReExecute = findViewById(R.id.btn_re_execute);
        btnClose = findViewById(R.id.btn_close);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void displayResult() {
        switch (executionResult.getStatus()) {
            case EXECUTING:
                showExecutingState();
                break;
            case SUCCESS:
                showSuccessState();
                break;
            case ERROR:
                showErrorState();
                break;
            case TIMEOUT:
                showTimeoutState();
                break;
        }
    }
    
    private void showExecutingState() {
        progressBar.setVisibility(View.VISIBLE);
        ivResultIcon.setVisibility(View.GONE);
        
        tvResultTitle.setText("正在执行命令");
        tvResultMessage.setText("请稍候，命令正在远程服务器上执行...");
        
        btnReExecute.setEnabled(false);
        btnViewLog.setEnabled(false);
        
        // 显示执行的命令
        tvCommandOutput.setText(executionResult.getExecutedCommand());
    }
    
    private void showSuccessState() {
        progressBar.setVisibility(View.GONE);
        ivResultIcon.setVisibility(View.VISIBLE);
        ivResultIcon.setImageResource(R.drawable.ic_check_circle);
        ivResultIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.success_color));
        
        tvResultTitle.setText("执行成功");
        
        StringBuilder message = new StringBuilder();
        message.append("✅ SSH连接成功\\n");
        
        if (executionResult.isKilledPrevious()) {
            message.append("✅ 已终止前一次进程\\n");
        }
        
        message.append("✅ 命令执行成功");
        
        if (executionResult.getProcessId() > 0) {
            message.append("\\n📝 进程ID: ").append(executionResult.getProcessId());
        }
        
        if (!TextUtils.isEmpty(executionResult.getLogFilePath())) {
            message.append("\\n📄 日志文件: ").append(executionResult.getLogFilePath());
        }
        
        tvResultMessage.setText(message.toString());
        
        // 显示命令输出（如果有）
        String output = executionResult.getOutput();
        if (!TextUtils.isEmpty(output)) {
            tvCommandOutput.setText(output);
            tvCommandOutput.setVisibility(View.VISIBLE);
        }
        
        btnReExecute.setEnabled(true);
        btnViewLog.setEnabled(!TextUtils.isEmpty(executionResult.getLogFilePath()));
    }
    
    private void showErrorState() {
        progressBar.setVisibility(View.GONE);
        ivResultIcon.setVisibility(View.VISIBLE);
        ivResultIcon.setImageResource(R.drawable.ic_error_circle);
        ivResultIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.error_color));
        
        tvResultTitle.setText("执行失败");
        tvResultMessage.setText("❌ " + executionResult.getErrorMessage());
        
        // 显示错误输出
        String errorOutput = executionResult.getErrorOutput();
        if (!TextUtils.isEmpty(errorOutput)) {
            tvCommandOutput.setText(errorOutput);
            tvCommandOutput.setVisibility(View.VISIBLE);
        }
        
        btnReExecute.setEnabled(true);
        btnViewLog.setEnabled(false);
    }
    
    private void showTimeoutState() {
        progressBar.setVisibility(View.GONE);
        ivResultIcon.setVisibility(View.VISIBLE);
        ivResultIcon.setImageResource(R.drawable.ic_timeout_circle);
        ivResultIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.warning_color));
        
        tvResultTitle.setText("执行超时");
        tvResultMessage.setText("⏰ 命令执行超时，可能仍在后台运行");
        
        btnReExecute.setEnabled(true);
        btnViewLog.setEnabled(!TextUtils.isEmpty(executionResult.getLogFilePath()));
    }
    
    private void setupListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        
        btnViewLog.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewLogRequested(executionResult.getLogFilePath());
            }
        });
        
        btnReExecute.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReExecuteRequested();
            }
            dismiss();
        });
    }
    
    /**
     * 更新执行状态（用于实时更新）
     */
    public void updateResult(ExecutionResult result) {
        this.executionResult = result;
        displayResult();
    }
    
    public void setOnResultActionListener(OnResultActionListener listener) {
        this.actionListener = listener;
    }
}
```

### 2. 远程命令执行服务

#### 2.1 远程命令服务 - RemoteCommandService.java
**目标**: 通过SSH执行远程命令并返回结果

```java
package com.termux.app.floating.services;

public class RemoteCommandService {
    private static final String TAG = "RemoteCommandService";
    private static final int DEFAULT_TIMEOUT = 30000; // 30秒超时
    
    private ExecutorService executorService;
    private Map<String, CompletableFuture<ExecutionResult>> runningTasks;
    
    public RemoteCommandService() {
        executorService = Executors.newCachedThreadPool();
        runningTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * 异步执行远程命令
     */
    public CompletableFuture<ExecutionResult> executeCommand(
            SSHConnectionConfig sshConfig, 
            String command, 
            OnExecutionProgressListener progressListener) {
            
        String taskId = UUID.randomUUID().toString();
        
        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(() -> {
            return executeCommandSync(sshConfig, command, progressListener);
        }, executorService);
        
        runningTasks.put(taskId, future);
        
        // 任务完成后清理
        future.whenComplete((result, throwable) -> {
            runningTasks.remove(taskId);
        });
        
        return future;
    }
    
    private ExecutionResult executeCommandSync(
            SSHConnectionConfig sshConfig, 
            String command,
            OnExecutionProgressListener progressListener) {
            
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionResult.Status.EXECUTING);
        result.setExecutedCommand(command);
        result.setStartTime(System.currentTimeMillis());
        
        if (progressListener != null) {
            progressListener.onProgress("正在建立SSH连接...", result);
        }
        
        SSHClient sshClient = null;
        Session session = null;
        
        try {
            // 建立SSH连接
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier()); // 开发环境，生产需要验证
            sshClient.setConnectTimeout(10000);
            sshClient.connect(sshConfig.getHost(), sshConfig.getPort());
            
            if (progressListener != null) {
                progressListener.onProgress("SSH连接成功，正在认证...", result);
            }
            
            // 身份认证
            if (!TextUtils.isEmpty(sshConfig.getPrivateKeyPath())) {
                // 使用私钥认证
                sshClient.authPublickey(sshConfig.getUsername(), sshConfig.getPrivateKeyPath());
            } else {
                // 使用密码认证
                sshClient.authPassword(sshConfig.getUsername(), sshConfig.getPassword());
            }
            
            if (progressListener != null) {
                progressListener.onProgress("认证成功，正在执行命令...", result);
            }
            
            // 创建会话并执行命令
            session = sshClient.startSession();
            Session.Command cmd = session.exec(command);
            
            // 读取输出
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();
            
            try (InputStream stdout = cmd.getInputStream();
                 InputStream stderr = cmd.getErrorStream()) {
                
                // 异步读取标准输出和错误输出
                CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            outputBuilder.append(line).append("\\n");
                        }
                    } catch (IOException e) {
                        Logger.logError(TAG, "Error reading stdout: " + e.getMessage());
                    }
                });
                
                CompletableFuture<Void> errorFuture = CompletableFuture.runAsync(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorBuilder.append(line).append("\\n");
                        }
                    } catch (IOException e) {
                        Logger.logError(TAG, "Error reading stderr: " + e.getMessage());
                    }
                });
                
                // 等待命令完成或超时
                if (cmd.join(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    // 等待输出读取完成
                    CompletableFuture.allOf(outputFuture, errorFuture).join();
                    
                    int exitCode = cmd.getExitStatus();
                    result.setExitCode(exitCode);
                    result.setOutput(outputBuilder.toString());
                    result.setErrorOutput(errorBuilder.toString());
                    
                    if (exitCode == 0) {
                        result.setStatus(ExecutionResult.Status.SUCCESS);
                        
                        // 尝试解析进程ID（如果命令包含后台运行）
                        if (command.contains("echo $! > .pid")) {
                            extractProcessId(result, sshClient, session);
                        }
                        
                    } else {
                        result.setStatus(ExecutionResult.Status.ERROR);
                        result.setErrorMessage("命令执行失败，退出码: " + exitCode);
                    }
                } else {
                    // 超时
                    result.setStatus(ExecutionResult.Status.TIMEOUT);
                    result.setErrorMessage("命令执行超时");
                }
            }
            
        } catch (IOException e) {
            result.setStatus(ExecutionResult.Status.ERROR);
            result.setErrorMessage("SSH连接失败: " + e.getMessage());
            Logger.logError(TAG, "SSH connection failed", e);
            
        } catch (Exception e) {
            result.setStatus(ExecutionResult.Status.ERROR);
            result.setErrorMessage("执行命令时发生错误: " + e.getMessage());
            Logger.logError(TAG, "Command execution failed", e);
            
        } finally {
            // 清理资源
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    Logger.logError(TAG, "Failed to close session", e);
                }
            }
            
            if (sshClient != null) {
                try {
                    sshClient.disconnect();
                } catch (IOException e) {
                    Logger.logError(TAG, "Failed to disconnect SSH client", e);
                }
            }
            
            result.setEndTime(System.currentTimeMillis());
            
            if (progressListener != null) {
                progressListener.onCompleted(result);
            }
        }
        
        return result;
    }
    
    private void extractProcessId(ExecutionResult result, SSHClient sshClient, Session session) {
        try {
            // 读取.pid文件获取进程ID
            Session pidSession = sshClient.startSession();
            Session.Command pidCmd = pidSession.exec("cat .pid 2>/dev/null || echo ''");
            
            if (pidCmd.join(5000, TimeUnit.MILLISECONDS)) {
                try (InputStream inputStream = pidCmd.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    
                    String pidLine = reader.readLine();
                    if (!TextUtils.isEmpty(pidLine)) {
                        try {
                            int pid = Integer.parseInt(pidLine.trim());
                            result.setProcessId(pid);
                        } catch (NumberFormatException e) {
                            Logger.logWarning(TAG, "Failed to parse PID: " + pidLine);
                        }
                    }
                }
            }
            
            pidSession.close();
            
        } catch (Exception e) {
            Logger.logWarning(TAG, "Failed to extract process ID", e);
        }
    }
    
    /**
     * 取消正在执行的任务
     */
    public void cancelTask(String taskId) {
        CompletableFuture<ExecutionResult> future = runningTasks.get(taskId);
        if (future != null) {
            future.cancel(true);
        }
    }
    
    /**
     * 获取正在运行的任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    public interface OnExecutionProgressListener {
        void onProgress(String message, ExecutionResult currentResult);
        void onCompleted(ExecutionResult result);
    }
}
```

### 3. 执行状态管理

#### 3.1 执行结果模型 - ExecutionResult.java
**目标**: 封装命令执行的结果和状态信息

```java
package com.termux.app.floating.models;

public class ExecutionResult {
    public enum Status {
        EXECUTING,   // 正在执行
        SUCCESS,     // 执行成功
        ERROR,       // 执行失败
        TIMEOUT      // 执行超时
    }
    
    private String taskId;              // 任务ID
    private Status status;              // 执行状态
    private String executedCommand;     // 执行的命令
    private long startTime;             // 开始时间
    private long endTime;               // 结束时间
    private int exitCode;               // 退出码
    private int processId;              // 进程ID
    private String output;              // 标准输出
    private String errorOutput;        // 错误输出
    private String errorMessage;       // 错误消息
    private String logFilePath;        // 日志文件路径
    private boolean killedPrevious;    // 是否杀掉了前一次进程
    
    public ExecutionResult() {
        this.taskId = UUID.randomUUID().toString();
        this.status = Status.EXECUTING;
        this.startTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getExecutedCommand() { return executedCommand; }
    public void setExecutedCommand(String executedCommand) { this.executedCommand = executedCommand; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
    
    public int getProcessId() { return processId; }
    public void setProcessId(int processId) { this.processId = processId; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public String getErrorOutput() { return errorOutput; }
    public void setErrorOutput(String errorOutput) { this.errorOutput = errorOutput; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
    
    public boolean isKilledPrevious() { return killedPrevious; }
    public void setKilledPrevious(boolean killedPrevious) { this.killedPrevious = killedPrevious; }
    
    // 工具方法
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean isRunning() {
        return status == Status.EXECUTING;
    }
    
    public boolean isError() {
        return status == Status.ERROR || status == Status.TIMEOUT;
    }
    
    public long getDuration() {
        if (endTime > 0 && startTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }
    
    public String getFormattedDuration() {
        long duration = getDuration();
        if (duration < 1000) {
            return duration + "ms";
        } else {
            return String.format("%.1fs", duration / 1000.0);
        }
    }
    
    @Override
    public String toString() {
        return "ExecutionResult{" +
                "taskId='" + taskId + '\\'' +
                ", status=" + status +
                ", exitCode=" + exitCode +
                ", duration=" + getFormattedDuration() +
                '}';
    }
}
```

#### 3.2 执行状态管理器 - ExecutionStateManager.java
**目标**: 管理命令执行的状态和历史记录

```java
package com.termux.app.floating.managers;

public class ExecutionStateManager {
    private static ExecutionStateManager instance;
    private Context context;
    private RemoteCommandService commandService;
    private Map<String, ExecutionResult> executionHistory;
    private List<OnExecutionStateChangeListener> stateListeners;
    
    public interface OnExecutionStateChangeListener {
        void onExecutionStarted(ExecutionResult result);
        void onExecutionProgress(String message, ExecutionResult result);
        void onExecutionCompleted(ExecutionResult result);
    }
    
    private ExecutionStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.commandService = new RemoteCommandService();
        this.executionHistory = new ConcurrentHashMap<>();
        this.stateListeners = new ArrayList<>();
    }
    
    public static synchronized ExecutionStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExecutionStateManager(context);
        }
        return instance;
    }
    
    /**
     * 执行运行配置
     */
    public void executeConfiguration(RunConfiguration config) {
        // 获取SSH配置
        SSHConfigManager sshManager = SSHConfigManager.getInstance(context);
        SSHConnectionConfig sshConfig = sshManager.getConfig(config.getSshConfigId());
        
        if (sshConfig == null) {
            notifyExecutionError("找不到对应的SSH配置");
            return;
        }
        
        // 生成完整命令
        String command = CommandBuilder.buildCompleteCommand(config);
        
        // 创建执行结果对象
        ExecutionResult result = new ExecutionResult();
        result.setExecutedCommand(command);
        
        // 通知开始执行
        notifyExecutionStarted(result);
        
        // 异步执行命令
        CompletableFuture<ExecutionResult> future = commandService.executeCommand(
                sshConfig, command, new RemoteCommandService.OnExecutionProgressListener() {
                    @Override
                    public void onProgress(String message, ExecutionResult currentResult) {
                        notifyExecutionProgress(message, currentResult);
                    }
                    
                    @Override
                    public void onCompleted(ExecutionResult completedResult) {
                        // 更新配置的最后使用时间
                        updateConfigLastUsed(config);
                        
                        // 保存执行历史
                        executionHistory.put(completedResult.getTaskId(), completedResult);
                        
                        // 通知执行完成
                        notifyExecutionCompleted(completedResult);
                    }
                });
        
        // 保存任务引用
        executionHistory.put(result.getTaskId(), result);
    }
    
    /**
     * 执行自定义命令
     */
    public void executeCustomCommand(SSHConnectionConfig sshConfig, String command) {
        ExecutionResult result = new ExecutionResult();
        result.setExecutedCommand(command);
        
        notifyExecutionStarted(result);
        
        CompletableFuture<ExecutionResult> future = commandService.executeCommand(
                sshConfig, command, new RemoteCommandService.OnExecutionProgressListener() {
                    @Override
                    public void onProgress(String message, ExecutionResult currentResult) {
                        notifyExecutionProgress(message, currentResult);
                    }
                    
                    @Override
                    public void onCompleted(ExecutionResult completedResult) {
                        executionHistory.put(completedResult.getTaskId(), completedResult);
                        notifyExecutionCompleted(completedResult);
                    }
                });
        
        executionHistory.put(result.getTaskId(), result);
    }
    
    private void updateConfigLastUsed(RunConfiguration config) {
        RunConfigurationManager configManager = RunConfigurationManager.getInstance(context);
        configManager.updateLastUsed(config.getId());
    }
    
    private void notifyExecutionStarted(ExecutionResult result) {
        for (OnExecutionStateChangeListener listener : stateListeners) {
            listener.onExecutionStarted(result);
        }
    }
    
    private void notifyExecutionProgress(String message, ExecutionResult result) {
        for (OnExecutionStateChangeListener listener : stateListeners) {
            listener.onExecutionProgress(message, result);
        }
    }
    
    private void notifyExecutionCompleted(ExecutionResult result) {
        for (OnExecutionStateChangeListener listener : stateListeners) {
            listener.onExecutionCompleted(result);
        }
    }
    
    private void notifyExecutionError(String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionResult.Status.ERROR);
        result.setErrorMessage(errorMessage);
        result.setEndTime(System.currentTimeMillis());
        
        notifyExecutionCompleted(result);
    }
    
    /**
     * 获取执行历史
     */
    public List<ExecutionResult> getExecutionHistory() {
        return new ArrayList<>(executionHistory.values());
    }
    
    /**
     * 清理执行历史
     */
    public void clearHistory() {
        executionHistory.clear();
    }
    
    /**
     * 添加状态监听器
     */
    public void addStateListener(OnExecutionStateChangeListener listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }
    
    /**
     * 移除状态监听器
     */
    public void removeStateListener(OnExecutionStateChangeListener listener) {
        stateListeners.remove(listener);
    }
    
    /**
     * 获取正在运行的任务数量
     */
    public int getRunningTaskCount() {
        return commandService.getRunningTaskCount();
    }
    
    public void shutdown() {
        commandService.shutdown();
    }
}
```

### 4. 完善悬浮按钮交互

#### 4.1 更新FloatingActionButton处理运行命令
**目标**: 集成命令执行流程到悬浮按钮

```java
// 在FloatingActionButton中添加运行命令处理
private void handleRunCommandAction() {
    // 显示运行配置选择对话框
    RunConfigSelectionDialog selectionDialog = new RunConfigSelectionDialog(getContext());
    selectionDialog.setOnConfigSelectedListener(new RunConfigSelectionDialog.OnConfigSelectedListener() {
        @Override
        public void onConfigSelected(RunConfiguration config) {
            showExecutionConfirmDialog(config);
        }
        
        @Override
        public void onNewConfigRequested() {
            // 跳转到配置管理页面
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.putExtra("navigate_to", "run_config_new");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
    });
    selectionDialog.show();
}

private void showExecutionConfirmDialog(RunConfiguration config) {
    ExecutionConfirmDialog confirmDialog = new ExecutionConfirmDialog(getContext(), config);
    confirmDialog.setOnExecutionActionListener(new ExecutionConfirmDialog.OnExecutionActionListener() {
        @Override
        public void onExecuteCommand(RunConfiguration config, String command) {
            executeConfiguration(config);
        }
        
        @Override
        public void onCopyCommand(String command) {
            ShareUtils.copyTextToClipboard(getContext(), command, "命令已复制到剪贴板");
        }
    });
    confirmDialog.show();
}

private void executeConfiguration(RunConfiguration config) {
    ExecutionStateManager executionManager = ExecutionStateManager.getInstance(getContext());
    
    // 显示执行结果对话框
    ExecutionResult initialResult = new ExecutionResult();
    initialResult.setStatus(ExecutionResult.Status.EXECUTING);
    
    ExecutionResultDialog resultDialog = new ExecutionResultDialog(getContext(), initialResult);
    resultDialog.setOnResultActionListener(new ExecutionResultDialog.OnResultActionListener() {
        @Override
        public void onViewLogRequested(String logFilePath) {
            // TODO: 实现日志查看功能
            Toast.makeText(getContext(), "查看日志: " + logFilePath, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onReExecuteRequested() {
            executeConfiguration(config); // 重新执行
        }
    });
    
    // 监听执行状态变化
    ExecutionStateManager.OnExecutionStateChangeListener stateListener = 
        new ExecutionStateManager.OnExecutionStateChangeListener() {
            @Override
            public void onExecutionStarted(ExecutionResult result) {
                // 更新对话框显示
                resultDialog.updateResult(result);
            }
            
            @Override
            public void onExecutionProgress(String message, ExecutionResult result) {
                // 更新进度
                resultDialog.updateResult(result);
            }
            
            @Override
            public void onExecutionCompleted(ExecutionResult result) {
                // 显示最终结果
                resultDialog.updateResult(result);
                
                // 清理监听器
                executionManager.removeStateListener(this);
            }
        };
    
    executionManager.addStateListener(stateListener);
    resultDialog.show();
    
    // 开始执行
    executionManager.executeConfiguration(config);
}
```

### 5. SSH连接快捷操作

#### 5.1 SSH连接对话框 - SshConnectionDialog.java
**目标**: 快速SSH连接选择和管理

```java
package com.termux.app.floating.dialogs;

public class SshConnectionDialog extends Dialog {
    private RecyclerView recyclerView;
    private SshConnectionAdapter adapter;
    private SSHConfigManager sshConfigManager;
    private OnConnectionActionListener actionListener;
    
    public interface OnConnectionActionListener {
        void onConnectRequested(SSHConnectionConfig config);
        void onManageConfigsRequested();
    }
    
    // 实现类似RunConfigSelectionDialog的结构
    // 显示SSH连接列表，支持一键连接
}
```

### 6. 布局文件创建

#### 6.1 运行配置选择对话框布局 - dialog_run_config_selection.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="选择运行配置"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"
        android:gravity="center" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_run_configs"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:maxHeight="400dp" />

    <TextView
        android:id="@+id/tv_empty_state"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="暂无运行配置\n请先创建配置"
        android:textSize="16sp"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_new_config"
        style="@style/Widget.MaterialComponents.Button.TextButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="新建配置"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

## ⚡ 实施步骤

1. **对话框系统**: 创建配置选择、执行确认、结果展示对话框
2. **命令执行服务**: 实现SSH远程命令执行核心逻辑
3. **状态管理**: 建立执行状态管理和历史记录系统
4. **悬浮按钮集成**: 将命令执行流程集成到悬浮按钮
5. **SSH连接快捷**: 实现SSH连接的快捷操作功能
6. **错误处理**: 完善各种异常情况的处理逻辑
7. **用户体验**: 优化交互流程和状态反馈

## 📊 验收标准

1. ✅ 悬浮按钮能够正确显示运行配置列表
2. ✅ 执行确认对话框显示正确的命令预览
3. ✅ SSH连接和命令执行功能正常工作
4. ✅ 执行结果能够正确展示成功、失败、超时状态
5. ✅ Kill前一次进程逻辑工作正常
6. ✅ 后台运行和PID解析功能正确
7. ✅ 错误处理完善，用户反馈清晰
8. ✅ 执行历史记录和状态管理正常

## 🔍 注意事项

- SSH连接安全性要求，生产环境需要验证主机密钥
- 命令执行超时处理，避免长时间阻塞
- 网络异常和连接中断的错误处理
- 用户输入验证，防止命令注入攻击
- 内存和资源的及时释放
- 多任务并发执行的资源管理

---

*本阶段完成后将实现完整的命令执行功能，用户可以通过悬浮按钮一键执行预配置的运行命令。*
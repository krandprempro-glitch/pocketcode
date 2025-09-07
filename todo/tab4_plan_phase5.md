# Phase 5: 整合优化与测试完善 (≤12w tokens)

## 🎯 阶段目标
完成整个系统的集成测试、性能优化、错误处理完善、用户体验优化，确保Configuration和Floating两大模块协同工作，达到生产就绪状态。

## 📋 具体任务列表

### 1. 系统集成与数据同步

#### 1.1 Bridge桥接模块完善 - ConfigFloatingBridge.java
**目标**: 确保Configuration和Floating模块之间的数据同步和状态一致性

```java
package com.termux.app.bridge.managers;

public class ConfigFloatingBridge {
    private static ConfigFloatingBridge instance;
    private Context context;
    private RunConfigurationManager configManager;
    private ExecutionStateManager executionManager;
    private FloatingWindowManager floatingManager;
    private List<OnDataChangeListener> dataChangeListeners;
    
    public interface OnDataChangeListener {
        void onRunConfigChanged();
        void onSSHConfigChanged(); 
        void onExecutionStateChanged();
    }
    
    private ConfigFloatingBridge(Context context) {
        this.context = context.getApplicationContext();
        this.configManager = RunConfigurationManager.getInstance(context);
        this.executionManager = ExecutionStateManager.getInstance(context);
        this.floatingManager = FloatingWindowManager.getInstance(context);
        this.dataChangeListeners = new ArrayList<>();
        
        setupDataSyncListeners();
    }
    
    public static synchronized ConfigFloatingBridge getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigFloatingBridge(context);
        }
        return instance;
    }
    
    private void setupDataSyncListeners() {
        // 监听运行配置变化
        configManager.addConfigChangeListener(new RunConfigurationManager.OnConfigChangeListener() {
            @Override
            public void onConfigAdded(RunConfiguration config) {
                notifyDataChanged(DataChangeType.RUN_CONFIG);
            }
            
            @Override
            public void onConfigUpdated(RunConfiguration config) {
                notifyDataChanged(DataChangeType.RUN_CONFIG);
            }
            
            @Override
            public void onConfigDeleted(String configId) {
                notifyDataChanged(DataChangeType.RUN_CONFIG);
            }
        });
        
        // 监听执行状态变化
        executionManager.addStateListener(new ExecutionStateManager.OnExecutionStateChangeListener() {
            @Override
            public void onExecutionStarted(ExecutionResult result) {
                notifyDataChanged(DataChangeType.EXECUTION_STATE);
            }
            
            @Override
            public void onExecutionProgress(String message, ExecutionResult result) {
                notifyDataChanged(DataChangeType.EXECUTION_STATE);
            }
            
            @Override
            public void onExecutionCompleted(ExecutionResult result) {
                notifyDataChanged(DataChangeType.EXECUTION_STATE);
            }
        });
    }
    
    /**
     * 获取最近使用的运行配置
     */
    public List<RunConfiguration> getRecentRunConfigs(int limit) {
        return configManager.getRecentConfigurations(limit);
    }
    
    /**
     * 获取按SSH分组的运行配置
     */
    public Map<String, List<RunConfiguration>> getConfigsBySSHGroup() {
        List<RunConfiguration> allConfigs = configManager.getAllConfigurations();
        Map<String, List<RunConfiguration>> groupedConfigs = new HashMap<>();
        
        for (RunConfiguration config : allConfigs) {
            String sshConfigId = config.getSshConfigId();
            if (!groupedConfigs.containsKey(sshConfigId)) {
                groupedConfigs.put(sshConfigId, new ArrayList<>());
            }
            groupedConfigs.get(sshConfigId).add(config);
        }
        
        return groupedConfigs;
    }
    
    /**
     * 验证运行配置的完整性
     */
    public ValidationResult validateRunConfiguration(RunConfiguration config) {
        ValidationResult result = new ValidationResult();
        
        // 检查SSH配置是否存在
        SSHConfigManager sshManager = SSHConfigManager.getInstance(context);
        SSHConnectionConfig sshConfig = sshManager.getConfig(config.getSshConfigId());
        if (sshConfig == null) {
            result.addError("SSH配置不存在或已被删除");
        }
        
        // 检查项目路径是否仍在收藏中
        List<DirectoryBookmark> bookmarks = PathBookmarkHelper.getBookmarksBySSHConfig(
            context, config.getSshConfigId());
        boolean pathExists = bookmarks.stream()
            .anyMatch(bookmark -> bookmark.getFullPath().equals(config.getProjectPath()));
            
        if (!pathExists) {
            result.addWarning("项目路径可能已从收藏中移除");
        }
        
        // 检查命令格式
        if (TextUtils.isEmpty(config.getCommand())) {
            result.addError("运行命令不能为空");
        }
        
        return result;
    }
    
    /**
     * 清理无效的运行配置
     */
    public int cleanupInvalidConfigurations() {
        List<RunConfiguration> allConfigs = configManager.getAllConfigurations();
        int cleanedCount = 0;
        
        for (RunConfiguration config : allConfigs) {
            ValidationResult validation = validateRunConfiguration(config);
            if (validation.hasErrors()) {
                configManager.deleteConfiguration(config.getId());
                cleanedCount++;
            }
        }
        
        return cleanedCount;
    }
    
    /**
     * 导出配置数据
     */
    public String exportConfigurations() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // 导出运行配置
            List<RunConfiguration> runConfigs = configManager.getAllConfigurations();
            exportData.put("runConfigurations", runConfigs);
            
            // 导出SSH配置
            SSHConfigManager sshManager = SSHConfigManager.getInstance(context);
            List<SSHConnectionConfig> sshConfigs = sshManager.getAllConfigs();
            exportData.put("sshConfigurations", sshConfigs);
            
            // 导出设置
            Map<String, Object> settings = new HashMap<>();
            settings.put("floatingEnabled", floatingManager.isFloatingEnabled());
            exportData.put("settings", settings);
            
            // 添加导出信息
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportTime", System.currentTimeMillis());
            metadata.put("version", "1.0");
            exportData.put("metadata", metadata);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(exportData);
            
        } catch (Exception e) {
            Logger.logError("ConfigFloatingBridge", "Failed to export configurations", e);
            return null;
        }
    }
    
    /**
     * 导入配置数据
     */
    public ImportResult importConfigurations(String jsonData) {
        ImportResult result = new ImportResult();
        
        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> importData = gson.fromJson(jsonData, mapType);
            
            // 导入运行配置
            if (importData.containsKey("runConfigurations")) {
                List<Map<String, Object>> runConfigMaps = 
                    (List<Map<String, Object>>) importData.get("runConfigurations");
                    
                for (Map<String, Object> configMap : runConfigMaps) {
                    try {
                        RunConfiguration config = gson.fromJson(
                            gson.toJson(configMap), RunConfiguration.class);
                        configManager.saveConfiguration(config);
                        result.incrementSuccessCount();
                    } catch (Exception e) {
                        result.addError("导入运行配置失败: " + e.getMessage());
                    }
                }
            }
            
            // 导入SSH配置
            if (importData.containsKey("sshConfigurations")) {
                List<Map<String, Object>> sshConfigMaps = 
                    (List<Map<String, Object>>) importData.get("sshConfigurations");
                    
                SSHConfigManager sshManager = SSHConfigManager.getInstance(context);
                for (Map<String, Object> configMap : sshConfigMaps) {
                    try {
                        SSHConnectionConfig config = gson.fromJson(
                            gson.toJson(configMap), SSHConnectionConfig.class);
                        sshManager.saveConfig(config);
                        result.incrementSuccessCount();
                    } catch (Exception e) {
                        result.addError("导入SSH配置失败: " + e.getMessage());
                    }
                }
            }
            
            // 导入设置
            if (importData.containsKey("settings")) {
                Map<String, Object> settings = (Map<String, Object>) importData.get("settings");
                
                if (settings.containsKey("floatingEnabled")) {
                    boolean floatingEnabled = (Boolean) settings.get("floatingEnabled");
                    if (floatingEnabled) {
                        floatingManager.enableFloating();
                    }
                }
            }
            
        } catch (Exception e) {
            result.addError("导入数据格式错误: " + e.getMessage());
            Logger.logError("ConfigFloatingBridge", "Failed to import configurations", e);
        }
        
        return result;
    }
    
    private void notifyDataChanged(DataChangeType type) {
        for (OnDataChangeListener listener : dataChangeListeners) {
            switch (type) {
                case RUN_CONFIG:
                    listener.onRunConfigChanged();
                    break;
                case SSH_CONFIG:
                    listener.onSSHConfigChanged();
                    break;
                case EXECUTION_STATE:
                    listener.onExecutionStateChanged();
                    break;
            }
        }
    }
    
    public void addDataChangeListener(OnDataChangeListener listener) {
        if (!dataChangeListeners.contains(listener)) {
            dataChangeListeners.add(listener);
        }
    }
    
    public void removeDataChangeListener(OnDataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }
    
    private enum DataChangeType {
        RUN_CONFIG, SSH_CONFIG, EXECUTION_STATE
    }
    
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
    
    public static class ImportResult {
        private int successCount = 0;
        private List<String> errors = new ArrayList<>();
        
        public void incrementSuccessCount() { successCount++; }
        public void addError(String error) { errors.add(error); }
        
        public int getSuccessCount() { return successCount; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
}
```

### 2. 错误处理与异常管理

#### 2.1 全局异常处理器 - GlobalExceptionHandler.java
**目标**: 统一处理系统中的各种异常情况

```java
package com.termux.app.bridge.utils;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "GlobalExceptionHandler";
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context context;
    
    public GlobalExceptionHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            // 记录异常信息
            logException(e);
            
            // 清理资源
            cleanupResources();
            
            // 保存崩溃信息
            saveCrashReport(e);
            
        } catch (Exception ex) {
            Logger.logError(TAG, "Error in exception handler", ex);
        } finally {
            // 调用默认处理器
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(t, e);
            }
        }
    }
    
    private void logException(Throwable e) {
        Logger.logError(TAG, "Uncaught exception in thread " + Thread.currentThread().getName(), e);
        
        // 特殊异常处理
        if (e instanceof OutOfMemoryError) {
            Logger.logError(TAG, "OutOfMemoryError detected - cleaning up resources");
            // 清理内存占用较大的组件
            cleanupMemoryIntensiveComponents();
        } else if (e instanceof SecurityException) {
            Logger.logError(TAG, "SecurityException - possible permission issue");
        } else if (e.getCause() instanceof IOException) {
            Logger.logError(TAG, "IOException - possible network or file system issue");
        }
    }
    
    private void cleanupResources() {
        try {
            // 清理悬浮窗
            FloatingWindowManager floatingManager = FloatingWindowManager.getInstance(context);
            if (floatingManager.isFloatingEnabled()) {
                floatingManager.disableFloating();
            }
            
            // 清理执行状态管理器
            ExecutionStateManager executionManager = ExecutionStateManager.getInstance(context);
            executionManager.shutdown();
            
        } catch (Exception e) {
            Logger.logError(TAG, "Error during resource cleanup", e);
        }
    }
    
    private void cleanupMemoryIntensiveComponents() {
        // 清理缓存
        try {
            // 清理图片缓存
            System.gc();
            
            // 清理执行历史
            ExecutionStateManager executionManager = ExecutionStateManager.getInstance(context);
            executionManager.clearHistory();
            
        } catch (Exception e) {
            Logger.logError(TAG, "Error during memory cleanup", e);
        }
    }
    
    private void saveCrashReport(Throwable e) {
        try {
            StringBuilder crashReport = new StringBuilder();
            crashReport.append("=== CRASH REPORT ===\\n");
            crashReport.append("Time: ").append(new Date().toString()).append("\\n");
            crashReport.append("Thread: ").append(Thread.currentThread().getName()).append("\\n");
            crashReport.append("Exception: ").append(e.getClass().getName()).append("\\n");
            crashReport.append("Message: ").append(e.getMessage()).append("\\n");
            crashReport.append("\\nStack Trace:\\n");
            
            for (StackTraceElement element : e.getStackTrace()) {
                crashReport.append(element.toString()).append("\\n");
            }
            
            // 保存到文件
            File crashDir = new File(context.getFilesDir(), "crashes");
            if (!crashDir.exists()) {
                crashDir.mkdirs();
            }
            
            String filename = "crash_" + System.currentTimeMillis() + ".txt";
            File crashFile = new File(crashDir, filename);
            
            try (FileWriter writer = new FileWriter(crashFile)) {
                writer.write(crashReport.toString());
            }
            
            Logger.logInfo(TAG, "Crash report saved to: " + crashFile.getAbsolutePath());
            
        } catch (Exception ex) {
            Logger.logError(TAG, "Failed to save crash report", ex);
        }
    }
    
    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(context));
    }
}
```

#### 2.2 网络错误处理 - NetworkErrorHandler.java
**目标**: 专门处理网络相关的错误

```java
package com.termux.app.bridge.utils;

public class NetworkErrorHandler {
    
    public static String getReadableErrorMessage(Throwable error) {
        if (error instanceof ConnectException) {
            return "无法连接到服务器，请检查网络连接和服务器地址";
        } else if (error instanceof SocketTimeoutException) {
            return "连接超时，请检查网络状况或稍后重试";
        } else if (error instanceof UnknownHostException) {
            return "无法解析服务器地址，请检查主机名是否正确";
        } else if (error instanceof SSHException) {
            return "SSH连接失败: " + error.getMessage();
        } else if (error.getMessage() != null && error.getMessage().contains("Authentication")) {
            return "身份验证失败，请检查用户名和密码";
        } else if (error.getMessage() != null && error.getMessage().contains("Permission denied")) {
            return "权限被拒绝，请检查用户权限";
        } else {
            return "网络错误: " + (error.getMessage() != null ? error.getMessage() : "未知错误");
        }
    }
    
    public static boolean isRetryableError(Throwable error) {
        return error instanceof SocketTimeoutException ||
               error instanceof ConnectException ||
               (error.getMessage() != null && error.getMessage().contains("timeout"));
    }
    
    public static int getSuggestedRetryDelay(int retryCount) {
        // 指数退避策略
        return Math.min(1000 * (int) Math.pow(2, retryCount), 30000); // 最大30秒
    }
}
```

### 3. 性能监控与优化

#### 3.1 性能监控器 - PerformanceMonitor.java
**目标**: 监控应用性能并提供优化建议

```java
package com.termux.app.bridge.utils;

public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static PerformanceMonitor instance;
    
    private Map<String, Long> operationStartTimes;
    private Map<String, List<Long>> operationHistory;
    private Runtime runtime;
    
    private PerformanceMonitor() {
        this.operationStartTimes = new ConcurrentHashMap<>();
        this.operationHistory = new ConcurrentHashMap<>();
        this.runtime = Runtime.getRuntime();
    }
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * 开始监控操作
     */
    public void startOperation(String operationName) {
        operationStartTimes.put(operationName, System.currentTimeMillis());
    }
    
    /**
     * 结束监控操作
     */
    public long endOperation(String operationName) {
        Long startTime = operationStartTimes.remove(operationName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            recordOperationDuration(operationName, duration);
            
            // 如果操作耗时过长，记录警告
            if (duration > 5000) { // 超过5秒
                Logger.logWarning(TAG, "Slow operation detected: " + operationName + " took " + duration + "ms");
            }
            
            return duration;
        }
        return 0;
    }
    
    private void recordOperationDuration(String operationName, long duration) {
        operationHistory.computeIfAbsent(operationName, k -> new ArrayList<>()).add(duration);
        
        // 限制历史记录数量
        List<Long> history = operationHistory.get(operationName);
        if (history.size() > 100) {
            history.remove(0);
        }
    }
    
    /**
     * 获取操作平均耗时
     */
    public double getAverageOperationTime(String operationName) {
        List<Long> history = operationHistory.get(operationName);
        if (history == null || history.isEmpty()) {
            return 0;
        }
        
        return history.stream().mapToLong(Long::longValue).average().orElse(0);
    }
    
    /**
     * 获取内存使用情况
     */
    public MemoryInfo getMemoryInfo() {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(totalMemory, freeMemory, maxMemory, usedMemory);
    }
    
    /**
     * 检查内存是否紧张
     */
    public boolean isMemoryLow() {
        MemoryInfo memInfo = getMemoryInfo();
        double usageRatio = (double) memInfo.getUsedMemory() / memInfo.getMaxMemory();
        return usageRatio > 0.8; // 超过80%认为内存紧张
    }
    
    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Report ===\\n");
        
        // 内存信息
        MemoryInfo memInfo = getMemoryInfo();
        report.append("Memory Usage:\\n");
        report.append("  Used: ").append(formatBytes(memInfo.getUsedMemory())).append("\\n");
        report.append("  Total: ").append(formatBytes(memInfo.getTotalMemory())).append("\\n");
        report.append("  Max: ").append(formatBytes(memInfo.getMaxMemory())).append("\\n");
        report.append("  Usage: ").append(String.format("%.1f%%", 
            (double) memInfo.getUsedMemory() / memInfo.getMaxMemory() * 100)).append("\\n");
        
        // 操作耗时统计
        report.append("\\nOperation Times:\\n");
        for (Map.Entry<String, List<Long>> entry : operationHistory.entrySet()) {
            String opName = entry.getKey();
            double avgTime = getAverageOperationTime(opName);
            report.append("  ").append(opName).append(": ")
                  .append(String.format("%.1fms avg", avgTime)).append("\\n");
        }
        
        return report.toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    public static class MemoryInfo {
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;
        private final long usedMemory;
        
        public MemoryInfo(long totalMemory, long freeMemory, long maxMemory, long usedMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.usedMemory = usedMemory;
        }
        
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getMaxMemory() { return maxMemory; }
        public long getUsedMemory() { return usedMemory; }
    }
}
```

### 4. 用户体验优化

#### 4.1 交互反馈管理器 - FeedbackManager.java
**目标**: 统一管理用户交互反馈

```java
package com.termux.app.bridge.utils;

public class FeedbackManager {
    private static FeedbackManager instance;
    private Context context;
    
    private FeedbackManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized FeedbackManager getInstance(Context context) {
        if (instance == null) {
            instance = new FeedbackManager(context);
        }
        return instance;
    }
    
    /**
     * 显示成功提示
     */
    public void showSuccess(String message) {
        showToast(message, Toast.LENGTH_SHORT);
        // 可以添加震动反馈
        vibrateIfEnabled(VibrationPattern.SUCCESS);
    }
    
    /**
     * 显示错误提示
     */
    public void showError(String message) {
        showToast(message, Toast.LENGTH_LONG);
        vibrateIfEnabled(VibrationPattern.ERROR);
    }
    
    /**
     * 显示警告提示
     */
    public void showWarning(String message) {
        showToast(message, Toast.LENGTH_LONG);
        vibrateIfEnabled(VibrationPattern.WARNING);
    }
    
    /**
     * 显示加载状态
     */
    public void showLoading(String message) {
        // 可以显示进度对话框或状态栏通知
        showToast(message, Toast.LENGTH_SHORT);
    }
    
    /**
     * 显示带操作的Snackbar
     */
    public void showSnackbarWithAction(View parentView, String message, String actionText, Runnable action) {
        if (parentView != null) {
            Snackbar snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_LONG);
            if (actionText != null && action != null) {
                snackbar.setAction(actionText, v -> action.run());
            }
            snackbar.show();
        } else {
            showToast(message, Toast.LENGTH_LONG);
        }
    }
    
    private void showToast(String message, int duration) {
        // 确保在主线程显示Toast
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, message, duration).show();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(context, message, duration).show());
        }
    }
    
    private void vibrateIfEnabled(VibrationPattern pattern) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // 检查用户设置是否启用震动
                if (isVibrationEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern.getPattern(), -1));
                    } else {
                        vibrator.vibrate(pattern.getPattern(), -1);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logWarning("FeedbackManager", "Failed to vibrate", e);
        }
    }
    
    private boolean isVibrationEnabled() {
        SharedPreferences prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("vibration_enabled", true);
    }
    
    private enum VibrationPattern {
        SUCCESS(new long[]{0, 50}),
        ERROR(new long[]{0, 100, 50, 100}),
        WARNING(new long[]{0, 80});
        
        private final long[] pattern;
        
        VibrationPattern(long[] pattern) {
            this.pattern = pattern;
        }
        
        public long[] getPattern() {
            return pattern;
        }
    }
}
```

#### 4.2 动画管理器 - AnimationManager.java
**目标**: 统一管理界面动画效果

```java
package com.termux.app.bridge.utils;

public class AnimationManager {
    
    /**
     * 淡入动画
     */
    public static void fadeIn(View view, int duration, Runnable onComplete) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(onComplete)
            .start();
    }
    
    /**
     * 淡出动画
     */
    public static void fadeOut(View view, int duration, Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> {
                view.setVisibility(View.GONE);
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .start();
    }
    
    /**
     * 缩放弹出动画
     */
    public static void scaleIn(View view, int duration, Runnable onComplete) {
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .withEndAction(onComplete)
            .start();
    }
    
    /**
     * 滑动进入动画
     */
    public static void slideInFromRight(View view, int duration, Runnable onComplete) {
        view.setTranslationX(view.getWidth());
        view.setVisibility(View.VISIBLE);
        
        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(onComplete)
            .start();
    }
    
    /**
     * 点击波纹效果
     */
    public static void rippleEffect(View view, Runnable onComplete) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction(onComplete)
                    .start();
            })
            .start();
    }
}
```

### 5. 配置验证与数据迁移

#### 5.1 配置验证器 - ConfigurationValidator.java
**目标**: 验证配置数据的完整性和有效性

```java
package com.termux.app.bridge.utils;

public class ConfigurationValidator {
    
    public static ValidationResult validateRunConfiguration(RunConfiguration config) {
        ValidationResult result = new ValidationResult();
        
        // 基本字段验证
        if (TextUtils.isEmpty(config.getId())) {
            result.addError("配置ID不能为空");
        }
        
        if (TextUtils.isEmpty(config.getName())) {
            result.addError("配置名称不能为空");
        }
        
        if (TextUtils.isEmpty(config.getSshConfigId())) {
            result.addError("SSH配置ID不能为空");
        }
        
        if (TextUtils.isEmpty(config.getProjectPath())) {
            result.addError("项目路径不能为空");
        }
        
        if (TextUtils.isEmpty(config.getCommand())) {
            result.addError("运行命令不能为空");
        }
        
        // 路径格式验证
        if (config.getProjectPath() != null && !config.getProjectPath().startsWith("/")) {
            result.addError("项目路径必须是绝对路径");
        }
        
        // 命令安全性检查
        if (config.getCommand() != null) {
            if (config.getCommand().contains("rm -rf") || 
                config.getCommand().contains("sudo") ||
                config.getCommand().contains(">")) {
                result.addWarning("命令包含潜在危险操作");
            }
        }
        
        // 环境变量格式检查
        if (!TextUtils.isEmpty(config.getEnvVariables())) {
            if (!isValidEnvVariableFormat(config.getEnvVariables())) {
                result.addError("环境变量格式不正确");
            }
        }
        
        return result;
    }
    
    public static ValidationResult validateSSHConfiguration(SSHConnectionConfig config) {
        ValidationResult result = new ValidationResult();
        
        if (TextUtils.isEmpty(config.getHost())) {
            result.addError("主机地址不能为空");
        } else {
            if (!isValidHostname(config.getHost())) {
                result.addError("主机地址格式不正确");
            }
        }
        
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            result.addError("端口号必须在1-65535之间");
        }
        
        if (TextUtils.isEmpty(config.getUsername())) {
            result.addError("用户名不能为空");
        }
        
        if (TextUtils.isEmpty(config.getPassword()) && TextUtils.isEmpty(config.getPrivateKeyPath())) {
            result.addError("必须提供密码或私钥路径");
        }
        
        return result;
    }
    
    private static boolean isValidEnvVariableFormat(String envVars) {
        // 简单的环境变量格式检查: KEY=value KEY2=value2
        String[] pairs = envVars.split("\\\\s+");
        for (String pair : pairs) {
            if (!pair.matches("\\\\w+=[^\\\\s]*")) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isValidHostname(String hostname) {
        // IP地址或域名格式检查
        return hostname.matches("^((\\\\d{1,3}\\\\.){3}\\\\d{1,3}|[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,})$");
    }
}
```

### 6. 完整的集成测试

#### 6.1 集成测试套件 - IntegrationTestSuite.java
**目标**: 验证各模块集成后的功能正确性

```java
package com.termux.app.bridge.test;

public class IntegrationTestSuite {
    private Context context;
    private ConfigFloatingBridge bridge;
    
    public IntegrationTestSuite(Context context) {
        this.context = context;
        this.bridge = ConfigFloatingBridge.getInstance(context);
    }
    
    /**
     * 执行完整的集成测试
     */
    public TestResults runAllTests() {
        TestResults results = new TestResults();
        
        // 测试配置管理
        results.add("配置管理测试", testConfigurationManagement());
        
        // 测试SSH连接
        results.add("SSH连接测试", testSSHConnection());
        
        // 测试悬浮窗
        results.add("悬浮窗测试", testFloatingWindow());
        
        // 测试数据同步
        results.add("数据同步测试", testDataSynchronization());
        
        // 测试命令生成
        results.add("命令生成测试", testCommandGeneration());
        
        return results;
    }
    
    private TestResult testConfigurationManagement() {
        try {
            RunConfigurationManager manager = RunConfigurationManager.getInstance(context);
            
            // 创建测试配置
            RunConfiguration testConfig = createTestConfiguration();
            
            // 测试保存
            manager.saveConfiguration(testConfig);
            
            // 测试加载
            RunConfiguration loadedConfig = manager.getConfiguration(testConfig.getId());
            if (loadedConfig == null) {
                return TestResult.failure("配置保存后无法加载");
            }
            
            // 测试更新
            loadedConfig.setName("Updated Name");
            manager.saveConfiguration(loadedConfig);
            
            // 测试删除
            boolean deleted = manager.deleteConfiguration(testConfig.getId());
            if (!deleted) {
                return TestResult.failure("配置删除失败");
            }
            
            return TestResult.success();
            
        } catch (Exception e) {
            return TestResult.failure("配置管理测试异常: " + e.getMessage());
        }
    }
    
    private TestResult testSSHConnection() {
        // 测试SSH配置管理和连接验证
        // 注意：这里只测试配置管理，不进行实际连接
        try {
            SSHConfigManager manager = SSHConfigManager.getInstance(context);
            
            SSHConnectionConfig testConfig = createTestSSHConfiguration();
            
            // 验证配置
            ValidationResult validation = ConfigurationValidator.validateSSHConfiguration(testConfig);
            if (validation.hasErrors()) {
                return TestResult.failure("SSH配置验证失败: " + validation.getErrors().toString());
            }
            
            return TestResult.success();
            
        } catch (Exception e) {
            return TestResult.failure("SSH连接测试异常: " + e.getMessage());
        }
    }
    
    private TestResult testFloatingWindow() {
        try {
            FloatingWindowManager manager = FloatingWindowManager.getInstance(context);
            
            // 测试权限检查
            boolean hasPermission = manager.hasFloatingPermission();
            
            // 测试状态管理
            boolean initialState = manager.isFloatingEnabled();
            manager.toggleFloating();
            boolean toggledState = manager.isFloatingEnabled();
            
            if (initialState == toggledState) {
                return TestResult.failure("悬浮窗状态切换失败");
            }
            
            // 恢复原状态
            manager.toggleFloating();
            
            return TestResult.success();
            
        } catch (Exception e) {
            return TestResult.failure("悬浮窗测试异常: " + e.getMessage());
        }
    }
    
    private TestResult testDataSynchronization() {
        try {
            // 测试Configuration和Floating模块之间的数据同步
            List<RunConfiguration> configs = bridge.getRecentRunConfigs(5);
            Map<String, List<RunConfiguration>> groupedConfigs = bridge.getConfigsBySSHGroup();
            
            // 验证数据一致性
            int totalFromGroups = groupedConfigs.values().stream()
                .mapToInt(List::size)
                .sum();
            
            RunConfigurationManager manager = RunConfigurationManager.getInstance(context);
            int totalFromManager = manager.getAllConfigurations().size();
            
            if (totalFromGroups != totalFromManager) {
                return TestResult.failure("数据同步不一致");
            }
            
            return TestResult.success();
            
        } catch (Exception e) {
            return TestResult.failure("数据同步测试异常: " + e.getMessage());
        }
    }
    
    private TestResult testCommandGeneration() {
        try {
            RunConfiguration config = createTestConfiguration();
            
            // 测试基础命令生成
            String basicCommand = CommandBuilder.buildBasicCommand(config);
            if (TextUtils.isEmpty(basicCommand)) {
                return TestResult.failure("基础命令生成失败");
            }
            
            // 测试后台运行命令生成
            String backgroundCommand = CommandBuilder.buildBackgroundCommand(config);
            if (TextUtils.isEmpty(backgroundCommand)) {
                return TestResult.failure("后台运行命令生成失败");
            }
            
            // 测试完整命令生成
            String completeCommand = CommandBuilder.buildCompleteCommand(config);
            if (TextUtils.isEmpty(completeCommand)) {
                return TestResult.failure("完整命令生成失败");
            }
            
            // 验证命令包含必要组件
            if (!completeCommand.contains("cd " + config.getProjectPath())) {
                return TestResult.failure("命令不包含项目路径切换");
            }
            
            if (config.isRunInBackground() && !completeCommand.contains("nohup")) {
                return TestResult.failure("后台运行命令不包含nohup");
            }
            
            return TestResult.success();
            
        } catch (Exception e) {
            return TestResult.failure("命令生成测试异常: " + e.getMessage());
        }
    }
    
    private RunConfiguration createTestConfiguration() {
        RunConfiguration config = new RunConfiguration();
        config.setId(UUID.randomUUID().toString());
        config.setName("Test Configuration");
        config.setSshConfigId("test_ssh");
        config.setProjectPath("/home/user/project");
        config.setCommand("npm run dev");
        config.setRunInBackground(true);
        config.setLogFileName("test.log");
        return config;
    }
    
    private SSHConnectionConfig createTestSSHConfiguration() {
        SSHConnectionConfig config = new SSHConnectionConfig();
        config.setId("test_ssh");
        config.setName("Test SSH");
        config.setHost("127.0.0.1");
        config.setPort(22);
        config.setUsername("testuser");
        config.setPassword("testpass");
        return config;
    }
    
    public static class TestResults {
        private Map<String, TestResult> results = new LinkedHashMap<>();
        
        public void add(String testName, TestResult result) {
            results.put(testName, result);
        }
        
        public boolean allPassed() {
            return results.values().stream().allMatch(TestResult::isSuccess);
        }
        
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Integration Test Results ===\\n");
            
            for (Map.Entry<String, TestResult> entry : results.entrySet()) {
                TestResult result = entry.getValue();
                report.append(entry.getKey()).append(": ")
                      .append(result.isSuccess() ? "PASS" : "FAIL");
                      
                if (!result.isSuccess()) {
                    report.append(" - ").append(result.getErrorMessage());
                }
                report.append("\\n");
            }
            
            int passedCount = (int) results.values().stream().filter(TestResult::isSuccess).count();
            int totalCount = results.size();
            
            report.append("\\nSummary: ").append(passedCount).append("/").append(totalCount).append(" tests passed");
            
            return report.toString();
        }
    }
    
    public static class TestResult {
        private final boolean success;
        private final String errorMessage;
        
        private TestResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static TestResult success() {
            return new TestResult(true, null);
        }
        
        public static TestResult failure(String errorMessage) {
            return new TestResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}
```

### 7. 应用生命周期管理

#### 7.1 应用生命周期管理器 - ApplicationLifecycleManager.java
**目标**: 管理整个应用的生命周期和资源清理

```java
package com.termux.app.bridge.managers;

public class ApplicationLifecycleManager implements Application.ActivityLifecycleCallbacks {
    private static ApplicationLifecycleManager instance;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private Context context;
    
    private ApplicationLifecycleManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized ApplicationLifecycleManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApplicationLifecycleManager(context);
        }
        return instance;
    }
    
    public void initialize() {
        if (context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(this);
        }
        
        // 安装全局异常处理器
        GlobalExceptionHandler.install(context);
        
        // 初始化性能监控
        PerformanceMonitor.getInstance();
        
        // 恢复悬浮窗状态
        FloatingWindowManager floatingManager = FloatingWindowManager.getInstance(context);
        floatingManager.onAppStarted();
    }
    
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // Activity创建时的处理
    }
    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // 应用进入前台
            onAppForeground();
        }
    }
    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // Activity恢复时的处理
    }
    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Activity暂停时的处理
    }
    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // 应用进入后台
            onAppBackground();
        }
    }
    
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // 保存实例状态
    }
    
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Activity销毁时的处理
    }
    
    private void onAppForeground() {
        Logger.logInfo("ApplicationLifecycle", "App entered foreground");
        
        // 检查数据完整性
        performDataIntegrityCheck();
        
        // 更新悬浮窗状态
        FloatingWindowManager floatingManager = FloatingWindowManager.getInstance(context);
        floatingManager.onAppStarted();
    }
    
    private void onAppBackground() {
        Logger.logInfo("ApplicationLifecycle", "App entered background");
        
        // 清理临时数据
        cleanupTemporaryData();
        
        // 生成性能报告
        String performanceReport = PerformanceMonitor.getInstance().generatePerformanceReport();
        Logger.logInfo("ApplicationLifecycle", "Performance Report:\\n" + performanceReport);
    }
    
    private void performDataIntegrityCheck() {
        try {
            ConfigFloatingBridge bridge = ConfigFloatingBridge.getInstance(context);
            int cleanedCount = bridge.cleanupInvalidConfigurations();
            if (cleanedCount > 0) {
                Logger.logInfo("ApplicationLifecycle", "Cleaned up " + cleanedCount + " invalid configurations");
            }
        } catch (Exception e) {
            Logger.logError("ApplicationLifecycle", "Data integrity check failed", e);
        }
    }
    
    private void cleanupTemporaryData() {
        try {
            // 清理执行历史（保留最近50条）
            ExecutionStateManager executionManager = ExecutionStateManager.getInstance(context);
            // executionManager.cleanupHistory(50);
            
            // 清理缓存文件
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                long cacheSize = calculateDirectorySize(cacheDir);
                if (cacheSize > 50 * 1024 * 1024) { // 超过50MB
                    // 清理缓存
                    deleteDirectory(cacheDir);
                    Logger.logInfo("ApplicationLifecycle", "Cache cleared: " + formatBytes(cacheSize));
                }
            }
            
        } catch (Exception e) {
            Logger.logError("ApplicationLifecycle", "Temporary data cleanup failed", e);
        }
    }
    
    public void shutdown() {
        try {
            // 停止悬浮窗
            FloatingWindowManager floatingManager = FloatingWindowManager.getInstance(context);
            floatingManager.disableFloating();
            
            // 停止执行管理器
            ExecutionStateManager executionManager = ExecutionStateManager.getInstance(context);
            executionManager.shutdown();
            
            // 取消注册生命周期回调
            if (context instanceof Application) {
                ((Application) context).unregisterActivityLifecycleCallbacks(this);
            }
            
        } catch (Exception e) {
            Logger.logError("ApplicationLifecycle", "Shutdown failed", e);
        }
    }
    
    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
```

## ⚡ 实施步骤

1. **桥接模块完善**: 实现Configuration和Floating模块间的数据同步
2. **异常处理系统**: 建立全局异常处理和错误管理机制
3. **性能监控**: 实现性能监控和内存管理
4. **用户体验优化**: 完善交互反馈和动画效果
5. **配置验证**: 实现数据验证和迁移功能
6. **集成测试**: 执行完整的系统集成测试
7. **生命周期管理**: 完善应用生命周期和资源管理
8. **文档和部署**: 完成文档整理和部署准备

## 📊 验收标准

1. ✅ Configuration和Floating模块数据完全同步
2. ✅ 异常处理完善，应用稳定性高
3. ✅ 内存占用合理，无内存泄漏
4. ✅ 用户交互流畅，反馈及时
5. ✅ 数据验证完整，配置迁移正常
6. ✅ 集成测试全部通过
7. ✅ 应用生命周期管理正确
8. ✅ 性能指标达到预期要求

## 🔍 注意事项

- 确保数据同步的原子性和一致性
- 异常处理要覆盖所有可能的失败场景
- 性能监控不能影响正常功能的使用
- 用户体验优化要考虑不同设备的差异
- 集成测试要覆盖关键业务流程
- 应用生命周期管理要处理各种边界情况

## 🎯 最终交付物

1. **完整的功能模块**: Configuration和Floating两大模块功能完整
2. **稳定的桥接系统**: 数据同步和状态管理机制
3. **健壮的错误处理**: 全面的异常处理和恢复机制
4. **优秀的用户体验**: 流畅的交互和及时的反馈
5. **完善的测试覆盖**: 集成测试和性能监控
6. **详细的技术文档**: 架构说明和使用指南

---

*Phase 5完成后，整个Termux配置管理与悬浮快捷操作系统将达到生产就绪状态，为用户提供完整、稳定、高效的使用体验。*
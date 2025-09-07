# Phase 3: Floating悬浮按钮核心功能 (≤12w tokens)

## 🎯 阶段目标
开发全局悬浮按钮和相关交互界面，实现系统级悬浮窗、拖拽功能、菜单展开动画以及权限管理等核心功能。为Phase 4的命令执行功能提供用户界面基础。

## 📋 具体任务列表

### 1. 悬浮窗权限管理

#### 1.1 权限管理服务 - FloatingPermissionService.java
**目标**: 处理悬浮窗权限申请和检查

```java
package com.termux.app.floating.services;

public class FloatingPermissionService {
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private Context context;
    
    public FloatingPermissionService(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 检查是否有悬浮窗权限
     */
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0以下默认有权限
    }
    
    /**
     * 申请悬浮窗权限
     */
    public void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            showPermissionDialog(activity);
        }
    }
    
    private void showPermissionDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("需要悬浮窗权限")
            .setMessage("为了使用快捷操作功能，需要允许应用显示悬浮窗。请在设置中开启此权限。")
            .setPositiveButton("去设置", (dialog, which) -> openOverlaySettings(activity))
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void openOverlaySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        } catch (Exception e) {
            Toast.makeText(context, "无法打开设置页面", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 权限申请结果处理
     */
    public void onPermissionResult(int requestCode, OnPermissionResultListener listener) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            boolean granted = hasOverlayPermission();
            if (listener != null) {
                listener.onPermissionResult(granted);
            }
        }
    }
    
    public interface OnPermissionResultListener {
        void onPermissionResult(boolean granted);
    }
}
```

### 2. 悬浮窗核心视图组件

#### 2.1 可拖拽视图基类 - DraggableView.java
**目标**: 提供拖拽功能的基础视图组件

```java
package com.termux.app.floating.views;

public abstract class DraggableView extends FrameLayout {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private boolean isDragging = false;
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    
    public DraggableView(Context context) {
        super(context);
        init();
    }
    
    private void init() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        setupLayoutParams();
        setupTouchListener();
    }
    
    private void setupLayoutParams() {
        layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        
        // 默认位置：右下角
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = getScreenWidth() - dpToPx(60); // 距离右边缘60dp
        layoutParams.y = getScreenHeight() - dpToPx(120); // 距离底部120dp
    }
    
    private void setupTouchListener() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return onTouchDown(event);
                        
                    case MotionEvent.ACTION_MOVE:
                        return onTouchMove(event);
                        
                    case MotionEvent.ACTION_UP:
                        return onTouchUp(event);
                }
                return false;
            }
        });
    }
    
    private boolean onTouchDown(MotionEvent event) {
        initialX = layoutParams.x;
        initialY = layoutParams.y;
        initialTouchX = event.getRawX();
        initialTouchY = event.getRawY();
        
        onDragStart();
        return true;
    }
    
    private boolean onTouchMove(MotionEvent event) {
        if (!isDragging && (Math.abs(event.getRawX() - initialTouchX) > 10 || 
                            Math.abs(event.getRawY() - initialTouchY) > 10)) {
            isDragging = true;
            onDragMove();
        }
        
        if (isDragging) {
            layoutParams.x = (int) (initialX + (event.getRawX() - initialTouchX));
            layoutParams.y = (int) (initialY + (event.getRawY() - initialTouchY));
            
            // 限制拖拽范围
            constrainPosition();
            
            windowManager.updateViewLayout(this, layoutParams);
            return true;
        }
        
        return false;
    }
    
    private boolean onTouchUp(MotionEvent event) {
        if (isDragging) {
            isDragging = false;
            snapToEdge();
            onDragEnd();
            return true;
        } else {
            // 如果不是拖拽，则视为点击
            performClick();
            return true;
        }
    }
    
    /**
     * 约束拖拽位置，限制在屏幕右侧边缘
     */
    private void constrainPosition() {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        
        // 限制在屏幕右半部分
        layoutParams.x = Math.max(screenWidth / 2, Math.min(screenWidth - viewWidth, layoutParams.x));
        
        // 限制在屏幕范围内
        layoutParams.y = Math.max(0, Math.min(screenHeight - viewHeight, layoutParams.y));
    }
    
    /**
     * 松手后自动吸附到边缘
     */
    private void snapToEdge() {
        int screenWidth = getScreenWidth();
        int targetX = screenWidth - getWidth(); // 吸附到右边缘
        
        ValueAnimator animator = ValueAnimator.ofInt(layoutParams.x, targetX);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            layoutParams.x = (int) animation.getAnimatedValue();
            windowManager.updateViewLayout(this, layoutParams);
        });
        animator.start();
    }
    
    // 抽象方法，子类实现
    protected abstract void onDragStart();
    protected abstract void onDragMove();
    protected abstract void onDragEnd();
    
    // 工具方法
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, 
                getContext().getResources().getDisplayMetrics());
    }
    
    private int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }
    
    private int getScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }
    
    public void show() {
        try {
            windowManager.addView(this, layoutParams);
        } catch (Exception e) {
            Logger.logError("DraggableView", "Failed to show floating view: " + e.getMessage());
        }
    }
    
    public void hide() {
        try {
            windowManager.removeView(this);
        } catch (Exception e) {
            Logger.logError("DraggableView", "Failed to hide floating view: " + e.getMessage());
        }
    }
}
```

#### 2.2 主悬浮按钮 - FloatingActionButton.java
**目标**: 主要的悬浮操作按钮

```java
package com.termux.app.floating.views;

public class FloatingActionButton extends DraggableView {
    private ImageView iconView;
    private FloatingMenuPanel menuPanel;
    private boolean isMenuVisible = false;
    private OnFloatingActionListener actionListener;
    
    public interface OnFloatingActionListener {
        void onMenuToggle(boolean isVisible);
        void onSSHConnectionClicked();
        void onRunCommandClicked();
        void onQuickSettingsClicked();
    }
    
    public FloatingActionButton(Context context) {
        super(context);
        initView();
        initMenuPanel();
    }
    
    private void initView() {
        // 加载布局
        LayoutInflater.from(getContext()).inflate(R.layout.floating_action_button, this, true);
        iconView = findViewById(R.id.iv_floating_icon);
        
        // 设置点击效果
        setBackgroundResource(R.drawable.bg_floating_button);
        setElevation(dpToPx(8));
        
        // 设置图标
        iconView.setImageResource(R.drawable.ic_rocket);
        iconView.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.white));
    }
    
    private void initMenuPanel() {
        menuPanel = new FloatingMenuPanel(getContext());
        menuPanel.setOnMenuItemClickListener(new FloatingMenuPanel.OnMenuItemClickListener() {
            @Override
            public void onSSHConnectionClicked() {
                hideMenu();
                if (actionListener != null) {
                    actionListener.onSSHConnectionClicked();
                }
            }
            
            @Override
            public void onRunCommandClicked() {
                hideMenu();
                if (actionListener != null) {
                    actionListener.onRunCommandClicked();
                }
            }
            
            @Override
            public void onQuickSettingsClicked() {
                hideMenu();
                if (actionListener != null) {
                    actionListener.onQuickSettingsClicked();
                }
            }
        });
    }
    
    @Override
    public boolean performClick() {
        super.performClick();
        toggleMenu();
        return true;
    }
    
    private void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }
    
    private void showMenu() {
        if (!isMenuVisible) {
            isMenuVisible = true;
            
            // 计算菜单位置（悬浮按钮上方）
            int[] location = new int[2];
            getLocationOnScreen(location);
            
            menuPanel.showAt(location[0], location[1] - menuPanel.getMenuHeight());
            
            // 按钮旋转动画
            animateButtonRotation(0, 45);
            
            if (actionListener != null) {
                actionListener.onMenuToggle(true);
            }
        }
    }
    
    private void hideMenu() {
        if (isMenuVisible) {
            isMenuVisible = false;
            menuPanel.hide();
            
            // 按钮旋转动画
            animateButtonRotation(45, 0);
            
            if (actionListener != null) {
                actionListener.onMenuToggle(false);
            }
        }
    }
    
    private void animateButtonRotation(float from, float to) {
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(iconView, "rotation", from, to);
        rotationAnimator.setDuration(200);
        rotationAnimator.setInterpolator(new DecelerateInterpolator());
        rotationAnimator.start();
    }
    
    @Override
    protected void onDragStart() {
        // 拖拽开始时缩放效果
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
        
        // 如果菜单显示中，先隐藏菜单
        if (isMenuVisible) {
            hideMenu();
        }
    }
    
    @Override
    protected void onDragMove() {
        // 拖拽过程中保持缩放状态
    }
    
    @Override
    protected void onDragEnd() {
        // 拖拽结束后恢复正常大小
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
    }
    
    public void setOnFloatingActionListener(OnFloatingActionListener listener) {
        this.actionListener = listener;
    }
    
    @Override
    public void hide() {
        if (isMenuVisible) {
            hideMenu();
        }
        super.hide();
    }
    
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, 
                getContext().getResources().getDisplayMetrics());
    }
}
```

#### 2.3 悬浮菜单面板 - FloatingMenuPanel.java
**目标**: 悬浮按钮展开的菜单面板

```java
package com.termux.app.floating.views;

public class FloatingMenuPanel extends FrameLayout {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private LinearLayout menuContainer;
    private boolean isShowing = false;
    private OnMenuItemClickListener menuItemClickListener;
    
    public interface OnMenuItemClickListener {
        void onSSHConnectionClicked();
        void onRunCommandClicked();
        void onQuickSettingsClicked();
    }
    
    public FloatingMenuPanel(Context context) {
        super(context);
        init();
    }
    
    private void init() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        setupLayoutParams();
        initView();
    }
    
    private void setupLayoutParams() {
        layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        
        layoutParams.gravity = Gravity.TOP | Gravity.START;
    }
    
    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.floating_menu_panel, this, true);
        menuContainer = findViewById(R.id.ll_menu_container);
        
        // 设置背景和阴影
        setBackgroundResource(R.drawable.bg_floating_menu);
        setElevation(dpToPx(12));
        
        setupMenuItems();
    }
    
    private void setupMenuItems() {
        // SSH连接菜单项
        View sshItem = createMenuItem(R.drawable.ic_ssh, "SSH连接", this::onSSHConnectionClicked);
        menuContainer.addView(sshItem);
        
        // 添加分割线
        menuContainer.addView(createDivider());
        
        // 运行命令菜单项
        View runItem = createMenuItem(R.drawable.ic_run, "运行命令", this::onRunCommandClicked);
        menuContainer.addView(runItem);
        
        // 添加分割线
        menuContainer.addView(createDivider());
        
        // 快捷设置菜单项
        View settingsItem = createMenuItem(R.drawable.ic_settings, "快捷设置", this::onQuickSettingsClicked);
        menuContainer.addView(settingsItem);
    }
    
    private View createMenuItem(int iconRes, String text, Runnable clickAction) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_floating_menu, null);
        
        ImageView iconView = itemView.findViewById(R.id.iv_menu_icon);
        TextView textView = itemView.findViewById(R.id.tv_menu_text);
        
        iconView.setImageResource(iconRes);
        textView.setText(text);
        
        itemView.setOnClickListener(v -> {
            animateItemClick(itemView, clickAction);
        });
        
        // 添加点击效果
        itemView.setBackgroundResource(R.drawable.bg_menu_item_selector);
        
        return itemView;
    }
    
    private View createDivider() {
        View divider = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        params.setMargins(dpToPx(16), 0, dpToPx(16), 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.divider_color));
        return divider;
    }
    
    private void animateItemClick(View itemView, Runnable clickAction) {
        // 点击动画
        itemView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(() -> {
                itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .withEndAction(clickAction)
                    .start();
            })
            .start();
    }
    
    private void onSSHConnectionClicked() {
        if (menuItemClickListener != null) {
            menuItemClickListener.onSSHConnectionClicked();
        }
    }
    
    private void onRunCommandClicked() {
        if (menuItemClickListener != null) {
            menuItemClickListener.onRunCommandClicked();
        }
    }
    
    private void onQuickSettingsClicked() {
        if (menuItemClickListener != null) {
            menuItemClickListener.onQuickSettingsClicked();
        }
    }
    
    public void showAt(int x, int y) {
        if (!isShowing) {
            layoutParams.x = x;
            layoutParams.y = y;
            
            try {
                windowManager.addView(this, layoutParams);
                isShowing = true;
                
                // 显示动画
                setAlpha(0f);
                setScaleX(0.8f);
                setScaleY(0.8f);
                
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
                
            } catch (Exception e) {
                Logger.logError("FloatingMenuPanel", "Failed to show menu: " + e.getMessage());
            }
        }
    }
    
    public void hide() {
        if (isShowing) {
            // 隐藏动画
            animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    try {
                        windowManager.removeView(this);
                        isShowing = false;
                    } catch (Exception e) {
                        Logger.logError("FloatingMenuPanel", "Failed to hide menu: " + e.getMessage());
                    }
                })
                .start();
        }
    }
    
    public int getMenuHeight() {
        // 测量菜单高度
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return getMeasuredHeight();
    }
    
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.menuItemClickListener = listener;
    }
    
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, 
                getContext().getResources().getDisplayMetrics());
    }
}
```

### 3. 悬浮窗服务管理

#### 3.1 悬浮窗服务 - FloatingWindowService.java
**目标**: 管理悬浮窗的生命周期和状态

```java
package com.termux.app.floating.services;

public class FloatingWindowService extends Service {
    private static final String ACTION_SHOW = "action_show";
    private static final String ACTION_HIDE = "action_hide";
    private static final String ACTION_TOGGLE = "action_toggle";
    
    private FloatingActionButton floatingButton;
    private FloatingPermissionService permissionService;
    private boolean isFloatingVisible = false;
    
    public static void showFloating(Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(ACTION_SHOW);
        context.startService(intent);
    }
    
    public static void hideFloating(Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(ACTION_HIDE);
        context.startService(intent);
    }
    
    public static void toggleFloating(Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(ACTION_TOGGLE);
        context.startService(intent);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        permissionService = new FloatingPermissionService(this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            if (ACTION_SHOW.equals(action)) {
                showFloatingButton();
            } else if (ACTION_HIDE.equals(action)) {
                hideFloatingButton();
            } else if (ACTION_TOGGLE.equals(action)) {
                if (isFloatingVisible) {
                    hideFloatingButton();
                } else {
                    showFloatingButton();
                }
            }
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    private void showFloatingButton() {
        if (!permissionService.hasOverlayPermission()) {
            // 如果没有权限，发送通知提示用户
            showPermissionNotification();
            return;
        }
        
        if (floatingButton == null) {
            floatingButton = new FloatingActionButton(this);
            floatingButton.setOnFloatingActionListener(new FloatingActionButton.OnFloatingActionListener() {
                @Override
                public void onMenuToggle(boolean isVisible) {
                    // 菜单状态变化
                }
                
                @Override
                public void onSSHConnectionClicked() {
                    handleSSHConnectionAction();
                }
                
                @Override
                public void onRunCommandClicked() {
                    handleRunCommandAction();
                }
                
                @Override
                public void onQuickSettingsClicked() {
                    handleQuickSettingsAction();
                }
            });
        }
        
        if (!isFloatingVisible) {
            floatingButton.show();
            isFloatingVisible = true;
            
            // 创建前台服务通知
            startForeground(1, createForegroundNotification());
        }
    }
    
    private void hideFloatingButton() {
        if (floatingButton != null && isFloatingVisible) {
            floatingButton.hide();
            isFloatingVisible = false;
            
            stopForeground(true);
        }
    }
    
    private void handleSSHConnectionAction() {
        // TODO: 在Phase 4中实现
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    private void handleRunCommandAction() {
        // TODO: 在Phase 4中实现
        Toast.makeText(this, "运行命令功能", Toast.LENGTH_SHORT).show();
    }
    
    private void handleQuickSettingsAction() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_to", "configuration");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    private void showPermissionNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "floating_permission", "悬浮窗权限", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Notification notification = new NotificationCompat.Builder(this, "floating_permission")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("需要悬浮窗权限")
            .setContentText("点击设置悬浮窗权限以使用快捷操作功能")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build();
            
        notificationManager.notify(2, notification);
    }
    
    private Notification createForegroundNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "floating_service", "悬浮按钮服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("提供悬浮按钮快捷操作功能");
            notificationManager.createNotificationChannel(channel);
        }
        
        Intent hideIntent = new Intent(this, FloatingWindowService.class);
        hideIntent.setAction(ACTION_HIDE);
        PendingIntent hidePendingIntent = PendingIntent.getService(this, 0, hideIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, "floating_service")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("悬浮按钮服务运行中")
            .setContentText("点击隐藏悬浮按钮")
            .setContentIntent(hidePendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(R.drawable.ic_close, "隐藏", hidePendingIntent)
            .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFloatingButton();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
```

### 4. 悬浮窗管理器

#### 4.1 悬浮窗管理器 - FloatingWindowManager.java
**目标**: 统一管理悬浮窗的显示、隐藏和状态

```java
package com.termux.app.floating.managers;

public class FloatingWindowManager {
    private static FloatingWindowManager instance;
    private Context context;
    private boolean isFloatingEnabled = false;
    
    private FloatingWindowManager(Context context) {
        this.context = context.getApplicationContext();
        loadSettings();
    }
    
    public static synchronized FloatingWindowManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatingWindowManager(context);
        }
        return instance;
    }
    
    /**
     * 启用悬浮按钮
     */
    public void enableFloating() {
        if (!isFloatingEnabled) {
            FloatingWindowService.showFloating(context);
            isFloatingEnabled = true;
            saveSettings();
        }
    }
    
    /**
     * 禁用悬浮按钮
     */
    public void disableFloating() {
        if (isFloatingEnabled) {
            FloatingWindowService.hideFloating(context);
            isFloatingEnabled = false;
            saveSettings();
        }
    }
    
    /**
     * 切换悬浮按钮状态
     */
    public void toggleFloating() {
        if (isFloatingEnabled) {
            disableFloating();
        } else {
            enableFloating();
        }
    }
    
    /**
     * 检查悬浮按钮是否启用
     */
    public boolean isFloatingEnabled() {
        return isFloatingEnabled;
    }
    
    /**
     * 检查是否有悬浮窗权限
     */
    public boolean hasFloatingPermission() {
        FloatingPermissionService permissionService = new FloatingPermissionService(context);
        return permissionService.hasOverlayPermission();
    }
    
    /**
     * 请求悬浮窗权限
     */
    public void requestFloatingPermission(Activity activity, FloatingPermissionService.OnPermissionResultListener listener) {
        FloatingPermissionService permissionService = new FloatingPermissionService(context);
        if (!permissionService.hasOverlayPermission()) {
            permissionService.requestOverlayPermission(activity);
            // TODO: 处理权限申请结果回调
        } else {
            if (listener != null) {
                listener.onPermissionResult(true);
            }
        }
    }
    
    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("floating_settings", Context.MODE_PRIVATE);
        isFloatingEnabled = prefs.getBoolean("floating_enabled", false);
        
        // 如果之前启用过且有权限，则自动启动
        if (isFloatingEnabled && hasFloatingPermission()) {
            FloatingWindowService.showFloating(context);
        }
    }
    
    private void saveSettings() {
        SharedPreferences prefs = context.getSharedPreferences("floating_settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("floating_enabled", isFloatingEnabled).apply();
    }
    
    /**
     * 在应用启动时调用，恢复悬浮按钮状态
     */
    public void onAppStarted() {
        if (isFloatingEnabled && hasFloatingPermission()) {
            FloatingWindowService.showFloating(context);
        }
    }
    
    /**
     * 在应用退出时调用
     */
    public void onAppStopped() {
        // 不自动隐藏，让用户手动控制
    }
}
```

### 5. 布局文件创建

#### 5.1 悬浮按钮布局 - floating_action_button.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="56dp"
    android:layout_height="56dp">

    <ImageView
        android:id="@+id/iv_floating_icon"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:layout_gravity="center"
        android:src="@drawable/ic_rocket"
        android:contentDescription="快捷操作按钮" />

</FrameLayout>
```

#### 5.2 悬浮菜单面板布局 - floating_menu_panel.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="8dp">

    <LinearLayout
        android:id="@+id/ll_menu_container"
        android:layout_width="160dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingTop="8dp"
        android:paddingBottom="8dp" />

</FrameLayout>
```

#### 5.3 菜单项布局 - item_floating_menu.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:clickable="true"
    android:focusable="true">

    <ImageView
        android:id="@+id/iv_menu_icon"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:src="@drawable/ic_placeholder"
        android:tint="?attr/colorOnSurface" />

    <TextView
        android:id="@+id/tv_menu_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="12dp"
        android:text="菜单项"
        android:textSize="14sp"
        android:textColor="?attr/colorOnSurface" />

</LinearLayout>
```

### 6. 背景资源和样式

#### 6.1 悬浮按钮背景 - bg_floating_button.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    
    <solid android:color="?attr/colorPrimary" />
    
    <stroke
        android:width="2dp"
        android:color="@android:color/white" />
    
</shape>
```

#### 6.2 悬浮菜单背景 - bg_floating_menu.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    
    <solid android:color="?attr/colorSurface" />
    
    <corners android:radius="8dp" />
    
    <stroke
        android:width="1dp"
        android:color="?attr/colorOutline" />
    
</shape>
```

#### 6.3 菜单项选择器 - bg_menu_item_selector.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="?attr/colorSurfaceVariant" />
        </shape>
    </item>
    
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@android:color/transparent" />
        </shape>
    </item>
    
</selector>
```

### 7. 集成到MainTabActivity

在MainTabActivity中添加悬浮窗控制：

```java
public class MainTabActivity extends AppCompatActivity {
    private FloatingWindowManager floatingManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ... 原有初始化代码 ...
        
        initFloatingWindow();
        handleIntent(getIntent());
    }
    
    private void initFloatingWindow() {
        floatingManager = FloatingWindowManager.getInstance(this);
        
        // 应用启动时恢复悬浮按钮状态
        floatingManager.onAppStarted();
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null) {
            String navigateTo = intent.getStringExtra("navigate_to");
            if ("configuration".equals(navigateTo)) {
                // 导航到配置页面
                tabLayout.getTabAt(3).select(); // Tab4
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (floatingManager != null) {
            floatingManager.onAppStopped();
        }
    }
}
```

## ⚡ 实施步骤

1. **权限管理**: 实现FloatingPermissionService权限检查和申请
2. **基础视图**: 创建DraggableView基类和拖拽功能
3. **悬浮按钮**: 实现FloatingActionButton主要交互
4. **菜单面板**: 开发FloatingMenuPanel和菜单动画
5. **服务管理**: 创建FloatingWindowService后台服务
6. **管理器集成**: 实现FloatingWindowManager统一管理
7. **布局资源**: 创建所有布局文件和样式资源
8. **主应用集成**: 将悬浮功能集成到MainTabActivity

## 📊 验收标准

1. ✅ 悬浮按钮能够正确显示和隐藏
2. ✅ 拖拽功能流畅，能够吸附到屏幕右边缘
3. ✅ 菜单展开收起动画自然美观
4. ✅ 权限申请流程用户友好
5. ✅ 前台服务运行稳定，通知显示正确
6. ✅ 菜单项点击响应正确（暂时显示Toast或跳转）
7. ✅ 悬浮窗状态能够正确持久化
8. ✅ 内存占用合理，无明显性能问题

## 🔍 注意事项

- 严格处理悬浮窗权限，提供清晰的引导流程
- 确保拖拽体验流畅，避免卡顿和抖动
- 悬浮按钮位置要合理，不遮挡重要内容
- 前台服务通知要简洁明了，提供必要的控制选项
- 考虑不同屏幕尺寸和密度的适配
- 妥善处理系统杀死服务的情况

---

*本阶段完成后将拥有一个功能完整的全局悬浮操作系统，为Phase 4的命令执行功能提供交互入口。*
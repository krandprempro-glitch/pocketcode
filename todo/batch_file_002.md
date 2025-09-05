# Batch 002: 抽屉菜单区域实现

## 目标
实现抽屉中可折叠的功能菜单区域，包括SSH配置、收藏夹管理、刷新等功能，并将原ActionBar菜单迁移到抽屉中。

## 任务列表

### 1. 创建抽屉菜单项布局
**文件**: `app/src/main/res/layout/item_drawer_menu.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 抽屉菜单项布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="20dp"
    android:paddingEnd="16dp"
    android:background="?android:attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true">

    <!-- 菜单图标 -->
    <ImageView
        android:id="@+id/menu_icon"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:layout_marginEnd="16dp"
        android:tint="#666666"
        android:contentDescription="菜单图标" />

    <!-- 菜单文字 -->
    <TextView
        android:id="@+id/menu_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="16sp"
        android:textColor="#333333"
        android:text="菜单项" />

    <!-- 展开/折叠指示器 (仅用于可展开项) -->
    <ImageView
        android:id="@+id/expand_indicator"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:src="@drawable/ic_chevron_right"
        android:tint="#999999"
        android:visibility="gone"
        android:contentDescription="展开指示器" />

</LinearLayout>
```

### 2. 创建可折叠菜单子项布局
**文件**: `app/src/main/res/layout/item_drawer_submenu.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 抽屉子菜单项布局 -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="40dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="60dp"
    android:paddingEnd="16dp"
    android:background="#F8F8F8"
    android:clickable="true"
    android:focusable="true">

    <!-- 子菜单图标 -->
    <ImageView
        android:id="@+id/submenu_icon"
        android:layout_width="18dp"
        android:layout_height="18dp"
        android:layout_marginEnd="12dp"
        android:tint="#888888"
        android:contentDescription="子菜单图标" />

    <!-- 子菜单文字 -->
    <TextView
        android:id="@+id/submenu_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="14sp"
        android:textColor="#555555"
        android:text="子菜单项" />

    <!-- 状态指示器 (如收藏数量等) -->
    <TextView
        android:id="@+id/submenu_badge"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="#888888"
        android:visibility="gone" />

</LinearLayout>
```

### 3. 创建抽屉菜单适配器
**文件**: `app/src/main/java/com/termux/app/adapters/DrawerMenuAdapter.java`

```java
package com.termux.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.DrawerMenuItem;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽屉菜单适配器
 * 支持可折叠的菜单项和子菜单
 */
public class DrawerMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG_TAG = "DrawerMenuAdapter";
    
    private static final int TYPE_MENU_ITEM = 1;
    private static final int TYPE_SUBMENU_ITEM = 2;
    
    private Context context;
    private List<DrawerMenuItem> menuItems;
    private OnMenuItemClickListener listener;
    
    public interface OnMenuItemClickListener {
        void onMenuItemClick(DrawerMenuItem item);
        void onSubMenuItemClick(DrawerMenuItem parentItem, DrawerMenuItem subItem);
    }
    
    public DrawerMenuAdapter(Context context, OnMenuItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.menuItems = new ArrayList<>();
    }
    
    public void updateMenuItems(List<DrawerMenuItem> items) {
        this.menuItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        DrawerMenuItem item = menuItems.get(position);
        return item.isSubMenu() ? TYPE_SUBMENU_ITEM : TYPE_MENU_ITEM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SUBMENU_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_submenu, parent, false);
            return new SubMenuViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_menu, parent, false);
            return new MenuViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DrawerMenuItem item = menuItems.get(position);
        
        if (holder instanceof MenuViewHolder) {
            bindMenuViewHolder((MenuViewHolder) holder, item, position);
        } else if (holder instanceof SubMenuViewHolder) {
            bindSubMenuViewHolder((SubMenuViewHolder) holder, item, position);
        }
    }
    
    private void bindMenuViewHolder(MenuViewHolder holder, DrawerMenuItem item, int position) {
        holder.menuText.setText(item.getTitle());
        holder.menuIcon.setImageResource(item.getIconRes());
        
        // 设置展开指示器
        if (item.hasSubItems()) {
            holder.expandIndicator.setVisibility(View.VISIBLE);
            // 根据展开状态旋转指示器
            float rotation = item.isExpanded() ? 90f : 0f;
            holder.expandIndicator.setRotation(rotation);
        } else {
            holder.expandIndicator.setVisibility(View.GONE);
        }
        
        // 点击处理
        holder.itemView.setOnClickListener(v -> {
            if (item.hasSubItems()) {
                toggleExpansion(item, position);
            } else {
                if (listener != null) {
                    listener.onMenuItemClick(item);
                }
            }
        });
    }
    
    private void bindSubMenuViewHolder(SubMenuViewHolder holder, DrawerMenuItem item, int position) {
        holder.submenuText.setText(item.getTitle());
        holder.submenuIcon.setImageResource(item.getIconRes());
        
        // 设置徽章文本
        if (item.getBadgeText() != null && !item.getBadgeText().isEmpty()) {
            holder.submenuBadge.setVisibility(View.VISIBLE);
            holder.submenuBadge.setText(item.getBadgeText());
        } else {
            holder.submenuBadge.setVisibility(View.GONE);
        }
        
        // 点击处理
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                DrawerMenuItem parentItem = findParentItem(item);
                listener.onSubMenuItemClick(parentItem, item);
            }
        });
    }
    
    /**
     * 切换菜单项的展开/折叠状态
     */
    private void toggleExpansion(DrawerMenuItem item, int position) {
        boolean wasExpanded = item.isExpanded();
        item.setExpanded(!wasExpanded);
        
        if (wasExpanded) {
            // 折叠：移除子项
            collapseMenuItem(item, position);
        } else {
            // 展开：添加子项
            expandMenuItem(item, position);
        }
        
        // 旋转展开指示器
        animateExpandIndicator(position, !wasExpanded);
    }
    
    /**
     * 展开菜单项
     */
    private void expandMenuItem(DrawerMenuItem item, int position) {
        List<DrawerMenuItem> subItems = item.getSubItems();
        if (subItems != null && !subItems.isEmpty()) {
            // 标记所有子项为子菜单
            for (DrawerMenuItem subItem : subItems) {
                subItem.setSubMenu(true);
                subItem.setParentId(item.getId());
            }
            
            // 在当前位置后插入子项
            menuItems.addAll(position + 1, subItems);
            notifyItemRangeInserted(position + 1, subItems.size());
            
            Logger.logInfo(LOG_TAG, "Expanded menu: " + item.getTitle() + " with " + subItems.size() + " sub items");
        }
    }
    
    /**
     * 折叠菜单项
     */
    private void collapseMenuItem(DrawerMenuItem item, int position) {
        int removeCount = 0;
        List<Integer> toRemove = new ArrayList<>();
        
        // 找到所有需要移除的子项位置
        for (int i = position + 1; i < menuItems.size(); i++) {
            DrawerMenuItem currentItem = menuItems.get(i);
            if (currentItem.isSubMenu() && item.getId().equals(currentItem.getParentId())) {
                toRemove.add(i);
                removeCount++;
            } else {
                break; // 遇到不属于此菜单的项，停止
            }
        }
        
        // 从后往前移除，避免索引变化
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            int indexToRemove = toRemove.get(i);
            menuItems.remove(indexToRemove);
        }
        
        if (removeCount > 0) {
            notifyItemRangeRemoved(position + 1, removeCount);
            Logger.logInfo(LOG_TAG, "Collapsed menu: " + item.getTitle() + ", removed " + removeCount + " sub items");
        }
    }
    
    /**
     * 查找子项的父菜单项
     */
    private DrawerMenuItem findParentItem(DrawerMenuItem subItem) {
        String parentId = subItem.getParentId();
        if (parentId != null) {
            for (DrawerMenuItem item : menuItems) {
                if (!item.isSubMenu() && parentId.equals(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * 动画展开指示器
     */
    private void animateExpandIndicator(int position, boolean expanded) {
        RecyclerView.ViewHolder holder = null; // 需要从RecyclerView获取
        if (holder instanceof MenuViewHolder) {
            ImageView indicator = ((MenuViewHolder) holder).expandIndicator;
            float fromRotation = expanded ? 0f : 90f;
            float toRotation = expanded ? 90f : 0f;
            
            RotateAnimation animation = new RotateAnimation(
                fromRotation, toRotation,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            animation.setDuration(200);
            animation.setFillAfter(true);
            indicator.startAnimation(animation);
        }
    }
    
    @Override
    public int getItemCount() {
        return menuItems.size();
    }
    
    // ViewHolder for main menu items
    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView menuIcon;
        TextView menuText;
        ImageView expandIndicator;
        
        MenuViewHolder(View itemView) {
            super(itemView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            menuText = itemView.findViewById(R.id.menu_text);
            expandIndicator = itemView.findViewById(R.id.expand_indicator);
        }
    }
    
    // ViewHolder for sub menu items
    static class SubMenuViewHolder extends RecyclerView.ViewHolder {
        ImageView submenuIcon;
        TextView submenuText;
        TextView submenuBadge;
        
        SubMenuViewHolder(View itemView) {
            super(itemView);
            submenuIcon = itemView.findViewById(R.id.submenu_icon);
            submenuText = itemView.findViewById(R.id.submenu_text);
            submenuBadge = itemView.findViewById(R.id.submenu_badge);
        }
    }
}
```

### 4. 创建抽屉菜单项数据模型
**文件**: `app/src/main/java/com/termux/app/models/DrawerMenuItem.java`

```java
package com.termux.app.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽屉菜单项数据模型
 */
public class DrawerMenuItem {
    private String id;
    private String title;
    private int iconRes;
    private boolean expanded = false;
    private boolean isSubMenu = false;
    private String parentId;
    private String badgeText;
    private List<DrawerMenuItem> subItems;
    private MenuAction action;
    
    public enum MenuAction {
        SSH_CONFIG,
        BOOKMARKS,
        REFRESH,
        SETTINGS,
        // 子菜单动作
        BOOKMARK_ADD_CURRENT,
        BOOKMARK_MANAGE_ALL,
        BOOKMARK_NAVIGATE
    }
    
    public DrawerMenuItem(String id, String title, int iconRes, MenuAction action) {
        this.id = id;
        this.title = title;
        this.iconRes = iconRes;
        this.action = action;
        this.subItems = new ArrayList<>();
    }
    
    // 创建带子项的菜单项
    public DrawerMenuItem(String id, String title, int iconRes) {
        this(id, title, iconRes, null);
    }
    
    public void addSubItem(DrawerMenuItem subItem) {
        if (subItems == null) {
            subItems = new ArrayList<>();
        }
        subItem.setSubMenu(true);
        subItem.setParentId(this.id);
        subItems.add(subItem);
    }
    
    public boolean hasSubItems() {
        return subItems != null && !subItems.isEmpty();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    
    public boolean isSubMenu() { return isSubMenu; }
    public void setSubMenu(boolean subMenu) { isSubMenu = subMenu; }
    
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    
    public String getBadgeText() { return badgeText; }
    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }
    
    public List<DrawerMenuItem> getSubItems() { return subItems; }
    public void setSubItems(List<DrawerMenuItem> subItems) { this.subItems = subItems; }
    
    public MenuAction getAction() { return action; }
    public void setAction(MenuAction action) { this.action = action; }
}
```

### 5. 更新Fragment以集成抽屉菜单
在 `RemoteFileBrowserFragment.java` 中添加菜单相关代码:

```java
// 添加新的成员变量
private RecyclerView drawerMenuRecyclerView;
private DrawerMenuAdapter drawerMenuAdapter;
private List<DrawerMenuItem> drawerMenuItems;

/**
 * 初始化UI组件 (在原有方法中添加)
 */
private void initViews(View view) {
    // ... 原有代码 ...
    
    // 抽屉菜单RecyclerView
    LinearLayout drawerMenuContainer = view.findViewById(R.id.drawer_menu_container);
    setupDrawerMenu(drawerMenuContainer);
    
    Logger.logInfo(LOG_TAG, "Drawer menu initialized");
}

/**
 * 设置抽屉菜单
 */
private void setupDrawerMenu(LinearLayout menuContainer) {
    if (menuContainer == null) return;
    
    // 创建RecyclerView用于菜单
    drawerMenuRecyclerView = new RecyclerView(requireContext());
    drawerMenuRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    drawerMenuRecyclerView.setNestedScrollingEnabled(false);
    
    // 设置适配器
    drawerMenuAdapter = new DrawerMenuAdapter(requireContext(), new DrawerMenuAdapter.OnMenuItemClickListener() {
        @Override
        public void onMenuItemClick(DrawerMenuItem item) {
            handleDrawerMenuClick(item);
        }
        
        @Override
        public void onSubMenuItemClick(DrawerMenuItem parentItem, DrawerMenuItem subItem) {
            handleDrawerSubMenuClick(parentItem, subItem);
        }
    });
    
    drawerMenuRecyclerView.setAdapter(drawerMenuAdapter);
    
    // 添加到容器
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    menuContainer.addView(drawerMenuRecyclerView, params);
    
    // 初始化菜单项
    initDrawerMenuItems();
}

/**
 * 初始化抽屉菜单项
 */
private void initDrawerMenuItems() {
    drawerMenuItems = new ArrayList<>();
    
    // SSH配置菜单
    DrawerMenuItem sshMenuItem = new DrawerMenuItem("ssh_config", "SSH连接配置", R.drawable.ic_ssh, DrawerMenuItem.MenuAction.SSH_CONFIG);
    drawerMenuItems.add(sshMenuItem);
    
    // 收藏夹菜单 (可折叠)
    DrawerMenuItem bookmarksMenuItem = new DrawerMenuItem("bookmarks", "收藏夹管理", R.drawable.ic_bookmark_small);
    bookmarksMenuItem.addSubItem(new DrawerMenuItem("bookmark_add", "收藏当前目录", R.drawable.ic_bookmark_add, DrawerMenuItem.MenuAction.BOOKMARK_ADD_CURRENT));
    bookmarksMenuItem.addSubItem(new DrawerMenuItem("bookmark_manage", "管理所有书签", R.drawable.ic_settings_small, DrawerMenuItem.MenuAction.BOOKMARK_MANAGE_ALL));
    
    // 根据当前收藏数量更新徽章
    updateBookmarksBadge(bookmarksMenuItem);
    drawerMenuItems.add(bookmarksMenuItem);
    
    // 刷新菜单
    DrawerMenuItem refreshMenuItem = new DrawerMenuItem("refresh", "刷新目录", R.drawable.ic_refresh_small, DrawerMenuItem.MenuAction.REFRESH);
    drawerMenuItems.add(refreshMenuItem);
    
    // 设置菜单 (可折叠)
    DrawerMenuItem settingsMenuItem = new DrawerMenuItem("settings", "设置选项", R.drawable.ic_settings_small);
    // 可以添加设置子项
    drawerMenuItems.add(settingsMenuItem);
    
    // 更新适配器
    drawerMenuAdapter.updateMenuItems(drawerMenuItems);
}

/**
 * 更新收藏夹徽章显示
 */
private void updateBookmarksBadge(DrawerMenuItem bookmarksMenuItem) {
    if (currentWorkspace != null && workspaceManager != null) {
        List<DirectoryBookmark> bookmarks = workspaceManager.getProjectBookmarks(currentWorkspace.getId());
        int bookmarkCount = bookmarks.size();
        
        if (bookmarkCount > 0) {
            bookmarksMenuItem.setBadgeText(String.valueOf(bookmarkCount));
            
            // 更新"管理所有书签"子项的徽章
            List<DrawerMenuItem> subItems = bookmarksMenuItem.getSubItems();
            if (subItems != null && subItems.size() > 1) {
                subItems.get(1).setBadgeText("(" + bookmarkCount + ")");
            }
        } else {
            bookmarksMenuItem.setBadgeText(null);
        }
    }
}

/**
 * 处理抽屉菜单点击
 */
private void handleDrawerMenuClick(DrawerMenuItem item) {
    Logger.logInfo(LOG_TAG, "Drawer menu clicked: " + item.getTitle());
    
    if (item.getAction() == null) return;
    
    switch (item.getAction()) {
        case SSH_CONFIG:
            showConnectionDialog();
            closeDrawer();
            break;
        case REFRESH:
            refreshCurrentDirectory();
            closeDrawer();
            break;
        default:
            break;
    }
}

/**
 * 处理抽屉子菜单点击
 */
private void handleDrawerSubMenuClick(DrawerMenuItem parentItem, DrawerMenuItem subItem) {
    Logger.logInfo(LOG_TAG, "Drawer submenu clicked: " + parentItem.getTitle() + " -> " + subItem.getTitle());
    
    if (subItem.getAction() == null) return;
    
    switch (subItem.getAction()) {
        case BOOKMARK_ADD_CURRENT:
            addBookmarkForCurrentDirectory();
            closeDrawer();
            break;
        case BOOKMARK_MANAGE_ALL:
            showAllBookmarksDialog();
            closeDrawer();
            break;
        case BOOKMARK_NAVIGATE:
            // 动态书签导航将在下一个批次实现
            break;
        default:
            break;
    }
}

/**
 * 关闭抽屉
 */
private void closeDrawer() {
    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
        drawerLayout.closeDrawer(GravityCompat.START);
    }
}

/**
 * 刷新抽屉菜单 (在连接状态变化时调用)
 */
private void refreshDrawerMenu() {
    if (drawerMenuAdapter != null && drawerMenuItems != null) {
        // 更新收藏夹徽章
        for (DrawerMenuItem item : drawerMenuItems) {
            if ("bookmarks".equals(item.getId())) {
                updateBookmarksBadge(item);
                break;
            }
        }
        
        drawerMenuAdapter.updateMenuItems(drawerMenuItems);
    }
}
```

### 6. 移除原有ActionBar菜单
修改 `onCreateOptionsMenu` 方法:

```java
@Override
public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    // 不再使用ActionBar菜单，所有功能已移至抽屉菜单
    // inflater.inflate(R.menu.remote_file_browser_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
}
```

### 7. 更新连接状态变化时的菜单刷新
在相关方法中添加菜单刷新调用:

```java
private void onConnectionEstablished() {
    // ... 原有代码 ...
    
    // 刷新抽屉菜单状态
    refreshDrawerMenu();
    
    // ... 原有代码 ...
}

private void loadProjectWorkspace(SSHConnectionConfig config) {
    // ... 原有代码 ...
    
    // 刷新抽屉菜单 (更新收藏夹数量)
    refreshDrawerMenu();
    
    // ... 原有代码 ...
}
```

### 8. 需要的图标资源
确保以下图标存在，如果没有需要创建简单的矢量图标:
- `ic_menu` (汉堡菜单图标)
- `ic_refresh_small` (小刷新图标)
- `ic_settings_small` (小设置图标) 
- `ic_bookmark_small` (小收藏图标)
- `ic_bookmark_add` (添加收藏图标)

### 9. 添加必要的import语句
在 `RemoteFileBrowserFragment.java` 中添加:

```java
import androidx.recyclerview.widget.LinearLayoutManager;
import com.termux.app.adapters.DrawerMenuAdapter;
import com.termux.app.models.DrawerMenuItem;
import java.util.ArrayList;
```

## 验证要点
1. 抽屉菜单区域正确显示
2. SSH配置、收藏夹等菜单项可以点击
3. 收藏夹菜单可以展开/折叠
4. 展开指示器正确旋转动画
5. 子菜单项正确缩进显示
6. 收藏夹数量徽章正确更新
7. ActionBar原有菜单功能转移完成

## 预估工作量
- 布局文件和适配器创建: ~5万token
- Fragment菜单集成和事件处理: ~3万token
- 动画和细节优化: ~1万token
- 总计: ~9万token

## 下一步
完成此批次后，抽屉菜单功能完整，可以继续进行 Batch 003 的文件树集成和交互功能。
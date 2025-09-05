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
        BOOKMARK_NAVIGATE,
        // 设置子菜单动作
        SETTINGS_DISPLAY,
        SETTINGS_CONNECTION
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
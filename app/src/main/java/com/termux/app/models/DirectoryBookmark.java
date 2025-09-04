package com.termux.app.models;

/**
 * 目录书签数据模型
 * 用于保存收藏的目录快速访问功能
 */
public class DirectoryBookmark {
    
    private String id;                  // 唯一标识符
    private String name;                // 书签显示名称
    private String fullPath;            // 完整路径
    private String projectId;           // 所属项目ID
    private String icon;                // 自定义图标
    private long createdTime;           // 创建时间
    private int sortOrder;              // 排序顺序
    private String color;               // 书签颜色标记
    
    public DirectoryBookmark() {
        this.createdTime = System.currentTimeMillis();
        this.sortOrder = 0;
    }
    
    public DirectoryBookmark(String id, String name, String fullPath, String projectId) {
        this();
        this.id = id;
        this.name = name;
        this.fullPath = fullPath;
        this.projectId = projectId;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFullPath() {
        return fullPath;
    }
    
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    // 工具方法
    
    /**
     * 获取显示名称（如果没有自定义名称则使用路径最后一部分）
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        } else if (fullPath != null && !fullPath.isEmpty()) {
            String[] parts = fullPath.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                return lastPart.isEmpty() ? "/" : lastPart;
            }
        }
        return "Unnamed Bookmark";
    }
    
    /**
     * 设置显示名称
     */
    public void setDisplayName(String displayName) {
        this.name = displayName;
    }
    
    /**
     * 获取父目录路径
     */
    public String getParentPath() {
        if (fullPath != null && !fullPath.equals("/")) {
            int lastSlash = fullPath.lastIndexOf('/');
            if (lastSlash > 0) {
                return fullPath.substring(0, lastSlash);
            } else if (lastSlash == 0) {
                return "/";
            }
        }
        return null;
    }
    
    /**
     * 获取路径深度
     */
    public int getDepth() {
        if (fullPath == null || fullPath.equals("/")) {
            return 0;
        }
        return fullPath.split("/").length - 1;
    }
    
    /**
     * 检查书签是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
               fullPath != null && !fullPath.isEmpty() &&
               projectId != null && !projectId.isEmpty();
    }
    
    /**
     * 检查是否为根目录书签
     */
    public boolean isRootBookmark() {
        return "/".equals(fullPath);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DirectoryBookmark that = (DirectoryBookmark) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "DirectoryBookmark{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", fullPath='" + fullPath + '\'' +
                ", projectId='" + projectId + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
    
    /**
     * 创建副本
     */
    public DirectoryBookmark copy() {
        DirectoryBookmark copy = new DirectoryBookmark();
        copy.id = this.id;
        copy.name = this.name;
        copy.fullPath = this.fullPath;
        copy.projectId = this.projectId;
        copy.icon = this.icon;
        copy.createdTime = this.createdTime;
        copy.sortOrder = this.sortOrder;
        copy.color = this.color;
        return copy;
    }
}
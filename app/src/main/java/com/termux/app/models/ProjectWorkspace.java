package com.termux.app.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目工作区数据模型
 * 用于保存远程项目的状态和配置信息
 */
public class ProjectWorkspace {
    
    private String id;                              // 唯一标识符
    private String name;                            // 项目名称
    private String description;                     // 项目描述
    private String remotePath;                      // 远程根目录路径
    private SSHConnectionConfig connection;         // SSH连接配置
    private List<String> bookmarkedPaths;           // 收藏的路径列表
    private Map<String, Boolean> expandedFolders;   // 展开的文件夹状态
    private String lastOpenedFile;                  // 最后打开的文件路径
    private long lastAccessTime;                    // 最后访问时间
    private String currentPath;                     // 当前浏览路径
    private int scrollPosition;                     // 滚动位置
    
    public ProjectWorkspace() {
        this.bookmarkedPaths = new ArrayList<>();
        this.expandedFolders = new HashMap<>();
        this.remotePath = "/";
        this.currentPath = "/";
        this.scrollPosition = 0;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public ProjectWorkspace(String id, String name, SSHConnectionConfig connection) {
        this();
        this.id = id;
        this.name = name;
        this.connection = connection;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRemotePath() {
        return remotePath;
    }
    
    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
    
    public SSHConnectionConfig getConnection() {
        return connection;
    }
    
    public void setConnection(SSHConnectionConfig connection) {
        this.connection = connection;
    }
    
    public List<String> getBookmarkedPaths() {
        return bookmarkedPaths;
    }
    
    public void setBookmarkedPaths(List<String> bookmarkedPaths) {
        this.bookmarkedPaths = bookmarkedPaths != null ? bookmarkedPaths : new ArrayList<>();
    }
    
    public Map<String, Boolean> getExpandedFolders() {
        return expandedFolders;
    }
    
    public void setExpandedFolders(Map<String, Boolean> expandedFolders) {
        this.expandedFolders = expandedFolders != null ? expandedFolders : new HashMap<>();
    }
    
    public String getLastOpenedFile() {
        return lastOpenedFile;
    }
    
    public void setLastOpenedFile(String lastOpenedFile) {
        this.lastOpenedFile = lastOpenedFile;
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    
    public String getCurrentPath() {
        return currentPath;
    }
    
    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }
    
    public int getScrollPosition() {
        return scrollPosition;
    }
    
    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }
    
    // 工具方法
    
    /**
     * 添加书签路径
     */
    public void addBookmark(String path) {
        if (path != null && !bookmarkedPaths.contains(path)) {
            bookmarkedPaths.add(path);
        }
    }
    
    /**
     * 移除书签路径
     */
    public void removeBookmark(String path) {
        bookmarkedPaths.remove(path);
    }
    
    /**
     * 检查路径是否已收藏
     */
    public boolean isBookmarked(String path) {
        return bookmarkedPaths.contains(path);
    }
    
    /**
     * 设置文件夹展开状态
     */
    public void setFolderExpanded(String folderPath, boolean expanded) {
        if (folderPath != null) {
            if (expanded) {
                expandedFolders.put(folderPath, true);
            } else {
                expandedFolders.remove(folderPath);
            }
        }
    }
    
    /**
     * 检查文件夹是否展开
     */
    public boolean isFolderExpanded(String folderPath) {
        return expandedFolders.containsKey(folderPath) && expandedFolders.get(folderPath);
    }
    
    /**
     * 更新最后访问时间
     */
    public void updateLastAccess() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * 获取显示名称（优先使用自定义名称，否则使用连接主机名）
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        } else if (connection != null) {
            return connection.getHost();
        } else {
            return "Unknown Project";
        }
    }
    
    /**
     * 检查工作区是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && 
               connection != null && connection.isValid() &&
               remotePath != null && !remotePath.isEmpty();
    }
    
    /**
     * 获取连接标识符（用于匹配相同的连接）
     */
    public String getConnectionId() {
        if (connection != null) {
            return connection.getHost() + ":" + connection.getPort() + "@" + connection.getUsername();
        }
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ProjectWorkspace that = (ProjectWorkspace) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "ProjectWorkspace{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", connection=" + (connection != null ? connection.getHost() : null) +
                ", remotePath='" + remotePath + '\'' +
                ", currentPath='" + currentPath + '\'' +
                ", bookmarks=" + bookmarkedPaths.size() +
                ", lastAccess=" + lastAccessTime +
                '}';
    }
    
    /**
     * 创建副本
     */
    public ProjectWorkspace copy() {
        ProjectWorkspace copy = new ProjectWorkspace();
        copy.id = this.id;
        copy.name = this.name;
        copy.description = this.description;
        copy.remotePath = this.remotePath;
        copy.connection = this.connection; // 浅拷贝
        copy.bookmarkedPaths = new ArrayList<>(this.bookmarkedPaths);
        copy.expandedFolders = new HashMap<>(this.expandedFolders);
        copy.lastOpenedFile = this.lastOpenedFile;
        copy.lastAccessTime = this.lastAccessTime;
        copy.currentPath = this.currentPath;
        copy.scrollPosition = this.scrollPosition;
        return copy;
    }
}
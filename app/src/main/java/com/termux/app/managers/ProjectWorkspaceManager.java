package com.termux.app.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.termux.app.models.DirectoryBookmark;
import com.termux.app.models.ProjectWorkspace;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.shared.logger.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 项目工作区管理器
 * 负责项目工作区的保存、加载、管理和书签功能
 */
public class ProjectWorkspaceManager {
    
    private static final String TAG = "ProjectWorkspaceManager";
    private static final String PREFS_NAME = "project_workspaces";
    private static final String BOOKMARKS_PREFS_NAME = "directory_bookmarks";
    private static final String WORKSPACE_PREFIX = "workspace_";
    private static final String BOOKMARK_PREFIX = "bookmark_";
    private static final String RECENT_WORKSPACES_KEY = "recent_workspaces";
    private static final String LAST_WORKSPACE_KEY = "last_workspace_id";
    private static final int MAX_RECENT_WORKSPACES = 10;
    
    private static ProjectWorkspaceManager instance;
    private final SharedPreferences workspacePrefs;
    private final SharedPreferences bookmarkPrefs;
    private final Gson gson;
    private final Context context;
    
    private ProjectWorkspaceManager(Context context) {
        this.context = context.getApplicationContext();
        this.workspacePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.bookmarkPrefs = context.getSharedPreferences(BOOKMARKS_PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ProjectWorkspaceManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProjectWorkspaceManager(context);
        }
        return instance;
    }
    
    // 项目工作区管理
    
    /**
     * 保存项目工作区
     */
    public void saveWorkspace(ProjectWorkspace workspace) {
        if (workspace == null || !workspace.isValid()) {
            Logger.logError(TAG, "Cannot save invalid workspace");
            return;
        }
        
        try {
            workspace.updateLastAccess();
            String json = gson.toJson(workspace);
            workspacePrefs.edit()
                    .putString(WORKSPACE_PREFIX + workspace.getId(), json)
                    .apply();
            
            // 更新最近访问列表
            updateRecentAccess(workspace.getId());
            
            Logger.logInfo(TAG, "Workspace saved: " + workspace.getId());
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to save workspace: " + e.getMessage());
        }
    }
    
    /**
     * 加载指定ID的项目工作区
     */
    public ProjectWorkspace getWorkspaceById(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            return null;
        }
        
        try {
            String json = workspacePrefs.getString(WORKSPACE_PREFIX + workspaceId, null);
            if (json != null) {
                ProjectWorkspace workspace = gson.fromJson(json, ProjectWorkspace.class);
                Logger.logInfo(TAG, "Workspace loaded: " + workspaceId);
                return workspace;
            }
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load workspace " + workspaceId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 加载所有项目工作区
     */
    public List<ProjectWorkspace> loadAllWorkspaces() {
        List<ProjectWorkspace> workspaces = new ArrayList<>();
        
        try {
            Map<String, ?> all = workspacePrefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (entry.getKey().startsWith(WORKSPACE_PREFIX) && entry.getValue() instanceof String) {
                    try {
                        ProjectWorkspace workspace = gson.fromJson((String) entry.getValue(), ProjectWorkspace.class);
                        if (workspace != null && workspace.isValid()) {
                            workspaces.add(workspace);
                        }
                    } catch (Exception e) {
                        Logger.logError(TAG, "Failed to parse workspace: " + entry.getKey());
                    }
                }
            }
            
            // 按最后访问时间排序
            workspaces.sort((a, b) -> Long.compare(b.getLastAccessTime(), a.getLastAccessTime()));
            
            Logger.logInfo(TAG, "Loaded " + workspaces.size() + " workspaces");
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load workspaces: " + e.getMessage());
        }
        
        return workspaces;
    }
    
    /**
     * 删除项目工作区
     */
    public boolean deleteWorkspace(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            return false;
        }
        
        try {
            workspacePrefs.edit()
                    .remove(WORKSPACE_PREFIX + workspaceId)
                    .apply();
            
            // 同时删除相关书签
            deleteWorkspaceBookmarks(workspaceId);
            
            // 从最近访问列表中移除
            removeFromRecentAccess(workspaceId);
            
            Logger.logInfo(TAG, "Workspace deleted: " + workspaceId);
            return true;
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to delete workspace: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据连接配置查找或创建工作区
     */
    public ProjectWorkspace getOrCreateWorkspaceForConnection(SSHConnectionConfig config) {
        if (config == null || !config.isValid()) {
            return null;
        }
        
        // 尝试查找已有的工作区
        List<ProjectWorkspace> workspaces = loadAllWorkspaces();
        for (ProjectWorkspace workspace : workspaces) {
            if (workspace.getConnection() != null && 
                workspace.getConnection().getHost().equals(config.getHost()) &&
                workspace.getConnection().getPort() == config.getPort() &&
                workspace.getConnection().getUsername().equals(config.getUsername())) {
                Logger.logInfo(TAG, "Found existing workspace for connection: " + config.getHost());
                return workspace;
            }
        }
        
        // 创建新的工作区
        ProjectWorkspace newWorkspace = new ProjectWorkspace();
        newWorkspace.setId(UUID.randomUUID().toString());
        newWorkspace.setName(config.getName() != null ? config.getName() : config.getHost());
        newWorkspace.setConnection(config);
        newWorkspace.setRemotePath("/");
        newWorkspace.setCurrentPath("/");
        newWorkspace.setDescription("Remote workspace for " + config.getHost());
        
        saveWorkspace(newWorkspace);
        Logger.logInfo(TAG, "Created new workspace for connection: " + config.getHost());
        
        return newWorkspace;
    }
    
    // 最近访问管理
    
    /**
     * 获取最近访问的项目工作区
     */
    public List<ProjectWorkspace> getRecentWorkspaces(int limit) {
        List<String> recentIds = getRecentWorkspaceIds();
        List<ProjectWorkspace> recentWorkspaces = new ArrayList<>();
        
        int count = Math.min(limit, recentIds.size());
        for (int i = 0; i < count; i++) {
            ProjectWorkspace workspace = getWorkspaceById(recentIds.get(i));
            if (workspace != null) {
                recentWorkspaces.add(workspace);
            }
        }
        
        return recentWorkspaces;
    }
    
    /**
     * 更新最后访问的工作区
     */
    public void updateLastAccess(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            return;
        }
        
        // 保存最后使用的工作区ID
        workspacePrefs.edit()
                .putString(LAST_WORKSPACE_KEY, workspaceId)
                .apply();
        
        updateRecentAccess(workspaceId);
    }
    
    /**
     * 获取最后使用的工作区
     */
    public ProjectWorkspace getLastWorkspace() {
        String lastId = workspacePrefs.getString(LAST_WORKSPACE_KEY, null);
        if (lastId != null) {
            return getWorkspaceById(lastId);
        }
        
        // 如果没有记录，返回最近访问的第一个
        List<ProjectWorkspace> recent = getRecentWorkspaces(1);
        return recent.isEmpty() ? null : recent.get(0);
    }
    
    private void updateRecentAccess(String workspaceId) {
        List<String> recentIds = getRecentWorkspaceIds();
        
        // 移除已存在的记录
        recentIds.remove(workspaceId);
        
        // 添加到列表开头
        recentIds.add(0, workspaceId);
        
        // 限制列表大小
        while (recentIds.size() > MAX_RECENT_WORKSPACES) {
            recentIds.remove(recentIds.size() - 1);
        }
        
        // 保存更新后的列表
        saveRecentWorkspaceIds(recentIds);
    }
    
    private void removeFromRecentAccess(String workspaceId) {
        List<String> recentIds = getRecentWorkspaceIds();
        recentIds.remove(workspaceId);
        saveRecentWorkspaceIds(recentIds);
    }
    
    private List<String> getRecentWorkspaceIds() {
        try {
            String json = workspacePrefs.getString(RECENT_WORKSPACES_KEY, null);
            if (json != null) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                return new ArrayList<>(gson.fromJson(json, listType));
            }
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load recent workspace IDs: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    
    private void saveRecentWorkspaceIds(List<String> ids) {
        try {
            String json = gson.toJson(ids);
            workspacePrefs.edit()
                    .putString(RECENT_WORKSPACES_KEY, json)
                    .apply();
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to save recent workspace IDs: " + e.getMessage());
        }
    }
    
    // 书签管理
    
    /**
     * 添加目录书签
     */
    public void addBookmark(String projectId, DirectoryBookmark bookmark) {
        if (projectId == null || bookmark == null) {
            Logger.logError(TAG, "Cannot add null bookmark or projectId");
            return;
        }
        
        try {
            // 先设置 projectId 和 id（如果需要）
            bookmark.setProjectId(projectId);
            if (bookmark.getId() == null || bookmark.getId().isEmpty()) {
                bookmark.setId(UUID.randomUUID().toString());
            }
            
            // 现在再验证有效性
            if (!bookmark.isValid()) {
                Logger.logError(TAG, "Cannot add invalid bookmark: " + bookmark.toString());
                return;
            }
            
            String json = gson.toJson(bookmark);
            bookmarkPrefs.edit()
                    .putString(BOOKMARK_PREFIX + bookmark.getId(), json)
                    .apply();
            
            Logger.logInfo(TAG, "Bookmark added: " + bookmark.getDisplayName() + " with ID: " + bookmark.getId());
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to add bookmark: " + e.getMessage());
        }
    }
    
    /**
     * 删除书签
     */
    public boolean removeBookmark(String bookmarkId) {
        if (bookmarkId == null || bookmarkId.isEmpty()) {
            return false;
        }
        
        try {
            bookmarkPrefs.edit()
                    .remove(BOOKMARK_PREFIX + bookmarkId)
                    .apply();
            
            Logger.logInfo(TAG, "Bookmark removed: " + bookmarkId);
            return true;
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to remove bookmark: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有书签（不限制项目）- 兼容性版本，不强制要求projectId
     */
    public List<DirectoryBookmark> getAllBookmarks() {
        List<DirectoryBookmark> bookmarks = new ArrayList<>();

        try {
            Map<String, ?> all = bookmarkPrefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (entry.getKey().startsWith(BOOKMARK_PREFIX) && entry.getValue() instanceof String) {
                    try {
                        DirectoryBookmark bookmark = gson.fromJson((String) entry.getValue(), DirectoryBookmark.class);
                        // 兼容性检查：只验证必要字段，不强制要求projectId
                        if (bookmark != null && bookmark.getFullPath() != null && !bookmark.getFullPath().isEmpty()) {
                            bookmarks.add(bookmark);
                        }
                    } catch (Exception e) {
                        Logger.logError(TAG, "Failed to parse bookmark: " + entry.getKey());
                    }
                }
            }

            // 按创建时间排序
            bookmarks.sort((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()));

        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load all bookmarks: " + e.getMessage());
        }

        return bookmarks;
    }

    /**
     * 获取项目的所有书签
     */
    public List<DirectoryBookmark> getProjectBookmarks(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DirectoryBookmark> bookmarks = new ArrayList<>();
        
        try {
            Map<String, ?> all = bookmarkPrefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (entry.getKey().startsWith(BOOKMARK_PREFIX) && entry.getValue() instanceof String) {
                    try {
                        DirectoryBookmark bookmark = gson.fromJson((String) entry.getValue(), DirectoryBookmark.class);
                        if (bookmark != null && projectId.equals(bookmark.getProjectId())) {
                            bookmarks.add(bookmark);
                        }
                    } catch (Exception e) {
                        Logger.logError(TAG, "Failed to parse bookmark: " + entry.getKey());
                    }
                }
            }
            
            // 按创建时间排序
            bookmarks.sort((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()));
            
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load bookmarks for project " + projectId + ": " + e.getMessage());
        }
        
        return bookmarks;
    }
    
    /**
     * 删除项目的所有书签
     */
    private void deleteWorkspaceBookmarks(String projectId) {
        List<DirectoryBookmark> bookmarks = getProjectBookmarks(projectId);
        for (DirectoryBookmark bookmark : bookmarks) {
            removeBookmark(bookmark.getId());
        }
    }
    
    /**
     * 检查路径是否已被收藏
     */
    public boolean isBookmarked(String projectId, String path) {
        List<DirectoryBookmark> bookmarks = getProjectBookmarks(projectId);
        for (DirectoryBookmark bookmark : bookmarks) {
            if (path.equals(bookmark.getFullPath())) {
                return true;
            }
        }
        return false;
    }
    
    // 状态持久化
    
    /**
     * 保存工作区状态（展开的文件夹、当前路径等）
     */
    public void saveWorkspaceState(ProjectWorkspace workspace) {
        if (workspace != null && workspace.isValid()) {
            saveWorkspace(workspace);
        }
    }
    
    /**
     * 清理过期或无效的数据
     */
    public void cleanup() {
        // 清理无效的工作区
        Map<String, ?> workspaceData = workspacePrefs.getAll();
        SharedPreferences.Editor editor = workspacePrefs.edit();
        
        for (Map.Entry<String, ?> entry : workspaceData.entrySet()) {
            if (entry.getKey().startsWith(WORKSPACE_PREFIX)) {
                try {
                    ProjectWorkspace workspace = gson.fromJson((String) entry.getValue(), ProjectWorkspace.class);
                    if (workspace == null || !workspace.isValid()) {
                        editor.remove(entry.getKey());
                        Logger.logInfo(TAG, "Removed invalid workspace: " + entry.getKey());
                    }
                } catch (Exception e) {
                    editor.remove(entry.getKey());
                    Logger.logInfo(TAG, "Removed corrupted workspace: " + entry.getKey());
                }
            }
        }
        
        editor.apply();
        
        // 清理无效的书签
        Map<String, ?> bookmarkData = bookmarkPrefs.getAll();
        SharedPreferences.Editor bookmarkEditor = bookmarkPrefs.edit();
        
        for (Map.Entry<String, ?> entry : bookmarkData.entrySet()) {
            if (entry.getKey().startsWith(BOOKMARK_PREFIX)) {
                try {
                    DirectoryBookmark bookmark = gson.fromJson((String) entry.getValue(), DirectoryBookmark.class);
                    if (bookmark == null || !bookmark.isValid()) {
                        bookmarkEditor.remove(entry.getKey());
                        Logger.logInfo(TAG, "Removed invalid bookmark: " + entry.getKey());
                    }
                } catch (Exception e) {
                    bookmarkEditor.remove(entry.getKey());
                    Logger.logInfo(TAG, "Removed corrupted bookmark: " + entry.getKey());
                }
            }
        }
        
        bookmarkEditor.apply();
        
        Logger.logInfo(TAG, "Cleanup completed");
    }
}
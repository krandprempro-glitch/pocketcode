package com.termux.app.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 远程文件项数据模型
 * 表示SFTP服务器上的文件或目录
 */
public class RemoteFileItem implements Serializable {
    
    private String name;           // 文件名
    private String path;           // 完整路径
    private boolean isDirectory;   // 是否为目录
    private long size;            // 文件大小
    private long lastModified;    // 最后修改时间
    private String permissions;   // 文件权限
    private FileType type;        // 文件类型枚举
    private boolean isHidden;     // 是否为隐藏文件
    private String owner;         // 文件所有者
    private String group;         // 文件所属组
    
    public RemoteFileItem() {
        this.type = FileType.UNKNOWN_FILE;
        this.isHidden = false;
    }
    
    public RemoteFileItem(String name, String path, boolean isDirectory) {
        this();
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.type = isDirectory ? FileType.DIRECTORY : FileTypeUtils.getFileType(name);
        this.isHidden = name.startsWith(".");
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        if (name != null) {
            this.isHidden = name.startsWith(".");
            if (!isDirectory) {
                this.type = FileTypeUtils.getFileType(name);
            }
        }
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public void setDirectory(boolean directory) {
        this.isDirectory = directory;
        if (directory) {
            this.type = FileType.DIRECTORY;
        }
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public FileType getType() {
        return type;
    }
    
    public void setType(FileType type) {
        this.type = type;
    }
    
    public boolean isHidden() {
        return isHidden;
    }
    
    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    /**
     * 获取格式化的文件大小
     */
    public String getFormattedSize() {
        if (isDirectory) {
            return "--";
        }
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取格式化的修改时间
     */
    public String getFormattedDate() {
        if (lastModified == 0) {
            return "--";
        }
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return formatter.format(new Date(lastModified * 1000));
    }
    
    /**
     * 获取文件信息字符串（大小 + 修改时间）
     */
    public String getFileInfo() {
        return getFormattedSize() + " • " + getFormattedDate();
    }
    
    /**
     * 获取父目录路径
     */
    public String getParentPath() {
        if (path == null || path.equals("/")) {
            return null;
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlash);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        RemoteFileItem that = (RemoteFileItem) obj;
        return path != null ? path.equals(that.path) : that.path == null;
    }
    
    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "RemoteFileItem{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", isDirectory=" + isDirectory +
                ", size=" + size +
                ", type=" + type +
                '}';
    }
}
package com.termux.app.models;

import com.termux.app.models.FileType;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件树节点数据模型
 * 用于表示VSCode风格的目录树结构
 */
public class FileTreeNode {
    
    private String name;                    // 节点名称
    private String fullPath;                // 完整路径
    private boolean isDirectory;            // 是否为目录
    private boolean isExpanded;             // 是否展开
    private int depth;                      // 缩进深度
    private List<FileTreeNode> children;    // 子节点列表
    private FileTreeNode parent;            // 父节点
    private FileType fileType;              // 文件类型
    private boolean isLoading;              // 是否正在加载子项
    private boolean isBookmarked;           // 是否已收藏
    private long lastModified;              // 最后修改时间
    private long size;                      // 文件大小
    
    public FileTreeNode() {
        this.children = new ArrayList<>();
        this.isExpanded = false;
        this.isLoading = false;
        this.isBookmarked = false;
        this.depth = 0;
    }
    
    public FileTreeNode(String name, String fullPath, boolean isDirectory) {
        this();
        this.name = name;
        this.fullPath = fullPath;
        this.isDirectory = isDirectory;
        this.fileType = isDirectory ? FileType.DIRECTORY : FileTypeUtils.getFileType(name);
    }
    
    public FileTreeNode(RemoteFileItem fileItem) {
        this();
        this.name = fileItem.getName();
        this.fullPath = fileItem.getPath();
        this.isDirectory = fileItem.isDirectory();
        this.fileType = fileItem.getType();
        this.size = fileItem.getSize();
        this.lastModified = fileItem.getLastModified();
    }
    
    // Getters and Setters
    
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
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public void setDirectory(boolean directory) {
        this.isDirectory = directory;
        if (directory) {
            this.fileType = FileType.DIRECTORY;
        }
    }
    
    public boolean isExpanded() {
        return isExpanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public List<FileTreeNode> getChildren() {
        return children;
    }
    
    public void setChildren(List<FileTreeNode> children) {
        this.children = children != null ? children : new ArrayList<>();
        // 设置父节点关系
        for (FileTreeNode child : this.children) {
            child.setParent(this);
            child.setDepth(this.depth + 1);
        }
    }
    
    public void addChild(FileTreeNode child) {
        if (child != null) {
            this.children.add(child);
            child.setParent(this);
            child.setDepth(this.depth + 1);
        }
    }
    
    public void removeChild(FileTreeNode child) {
        if (child != null) {
            this.children.remove(child);
            child.setParent(null);
        }
    }
    
    public void clearChildren() {
        for (FileTreeNode child : children) {
            child.setParent(null);
        }
        this.children.clear();
    }
    
    public FileTreeNode getParent() {
        return parent;
    }
    
    public void setParent(FileTreeNode parent) {
        this.parent = parent;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }
    
    public boolean isBookmarked() {
        return isBookmarked;
    }
    
    public void setBookmarked(boolean bookmarked) {
        this.isBookmarked = bookmarked;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    // 工具方法
    
    /**
     * 检查是否有子节点
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    /**
     * 获取子节点数量
     */
    public int getChildCount() {
        return children.size();
    }
    
    /**
     * 根据名称查找子节点
     */
    public FileTreeNode findChild(String name) {
        for (FileTreeNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * 获取根节点
     */
    public FileTreeNode getRoot() {
        FileTreeNode root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }
    
    /**
     * 获取节点路径（从根到当前节点的路径）
     */
    public List<FileTreeNode> getNodePath() {
        List<FileTreeNode> path = new ArrayList<>();
        FileTreeNode current = this;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }
    
    /**
     * 判断是否为叶子节点
     */
    public boolean isLeaf() {
        return !isDirectory || children.isEmpty();
    }
    
    /**
     * 递归计算总的子节点数量（包括间接子节点）
     */
    public int getTotalChildCount() {
        int count = children.size();
        for (FileTreeNode child : children) {
            count += child.getTotalChildCount();
        }
        return count;
    }
    
    /**
     * 展开到指定路径
     */
    public boolean expandToPath(String targetPath) {
        if (fullPath.equals(targetPath)) {
            return true;
        }
        
        if (targetPath.startsWith(fullPath)) {
            setExpanded(true);
            for (FileTreeNode child : children) {
                if (child.expandToPath(targetPath)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取缩进像素值
     */
    public int getIndentPixels(int indentSizeDp) {
        return depth * indentSizeDp;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileTreeNode that = (FileTreeNode) obj;
        return fullPath != null ? fullPath.equals(that.fullPath) : that.fullPath == null;
    }
    
    @Override
    public int hashCode() {
        return fullPath != null ? fullPath.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "FileTreeNode{" +
                "name='" + name + '\'' +
                ", fullPath='" + fullPath + '\'' +
                ", isDirectory=" + isDirectory +
                ", isExpanded=" + isExpanded +
                ", depth=" + depth +
                ", childCount=" + children.size() +
                '}';
    }
    
    /**
     * 深度复制节点（不包括父子关系）
     */
    public FileTreeNode copy() {
        FileTreeNode copy = new FileTreeNode();
        copy.name = this.name;
        copy.fullPath = this.fullPath;
        copy.isDirectory = this.isDirectory;
        copy.isExpanded = this.isExpanded;
        copy.depth = this.depth;
        copy.fileType = this.fileType;
        copy.isLoading = this.isLoading;
        copy.isBookmarked = this.isBookmarked;
        copy.lastModified = this.lastModified;
        copy.size = this.size;
        return copy;
    }
    
    /**
     * 创建RemoteFileItem对象
     */
    public RemoteFileItem toRemoteFileItem() {
        RemoteFileItem item = new RemoteFileItem(name, fullPath, isDirectory);
        item.setType(fileType);
        item.setSize(size);
        item.setLastModified(lastModified);
        return item;
    }
}
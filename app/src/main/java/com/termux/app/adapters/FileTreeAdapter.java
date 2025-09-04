package com.termux.app.adapters;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.FileTreeNode;
import com.termux.app.models.FileTypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件树适配器
 * 实现VSCode风格的层级目录树显示
 */
public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.FileTreeViewHolder> {
    
    private static final int INDENT_SIZE_DP = 20; // 每级缩进的dp值
    
    private List<FileTreeNode> rootNodes;          // 根节点列表
    private List<FileTreeNode> flattenedNodes;     // 扁平化后的节点列表
    private OnFileTreeActionListener listener;
    private String selectedPath = null;            // 当前选中的路径
    
    public interface OnFileTreeActionListener {
        void onNodeExpanded(FileTreeNode node);
        void onNodeCollapsed(FileTreeNode node);
        void onFileSelected(FileTreeNode node);
        void onDirectoryBookmarked(FileTreeNode node);
        void onLoadNodeChildren(FileTreeNode node);
    }
    
    public FileTreeAdapter(OnFileTreeActionListener listener) {
        this.rootNodes = new ArrayList<>();
        this.flattenedNodes = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FileTreeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_tree_item, parent, false);
        return new FileTreeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileTreeViewHolder holder, int position) {
        FileTreeNode node = flattenedNodes.get(position);
        
        // 设置缩进
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        int indentPx = (int) (node.getDepth() * INDENT_SIZE_DP * density);
        ViewGroup.LayoutParams params = holder.indentSpace.getLayoutParams();
        params.width = indentPx;
        holder.indentSpace.setLayoutParams(params);
        
        // 设置展开/折叠图标
        if (node.isDirectory()) {
            if (node.hasChildren() || !node.isExpanded()) {
                holder.expandIcon.setVisibility(View.VISIBLE);
                // 设置箭头旋转角度
                float rotation = node.isExpanded() ? 90f : 0f;
                holder.expandIcon.setRotation(rotation);
            } else {
                holder.expandIcon.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.expandIcon.setVisibility(View.INVISIBLE);
        }
        
        // 设置文件图标
        holder.fileIcon.setImageResource(FileTypeUtils.getFileTypeIcon(node.getFileType()));
        
        // 设置文件名
        holder.fileName.setText(node.getName());
        
        // 设置加载指示器
        holder.loadingIndicator.setVisibility(node.isLoading() ? View.VISIBLE : View.GONE);
        
        // 设置书签图标
        holder.bookmarkIcon.setVisibility(node.isBookmarked() ? View.VISIBLE : View.GONE);
        
        // 设置选中状态
        boolean isSelected = node.getFullPath().equals(selectedPath);
        holder.itemView.setSelected(isSelected);
        if (isSelected) {
            holder.itemView.setBackgroundColor(0x1A2196F3); // 浅蓝色背景
        } else {
            holder.itemView.setBackground(null);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (node.isDirectory()) {
                toggleNode(node);
            } else if (listener != null) {
                listener.onFileSelected(node);
            }
        });
        
        // 设置展开图标点击事件
        holder.expandIcon.setOnClickListener(v -> {
            if (node.isDirectory()) {
                toggleNode(node);
            }
        });
        
        // 设置长按事件（用于书签功能）
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && node.isDirectory()) {
                listener.onDirectoryBookmarked(node);
            }
            return true;
        });
    }
    
    @Override
    public int getItemCount() {
        return flattenedNodes.size();
    }
    
    /**
     * 更新根节点列表并重建树
     */
    public void updateTree(List<FileTreeNode> rootNodes) {
        this.rootNodes.clear();
        if (rootNodes != null) {
            this.rootNodes.addAll(rootNodes);
        }
        rebuildFlattenedList();
        notifyDataSetChanged();
    }
    
    /**
     * 添加根节点
     */
    public void addRootNode(FileTreeNode node) {
        if (node != null) {
            rootNodes.add(node);
            node.setDepth(0);
            rebuildFlattenedList();
            notifyDataSetChanged();
        }
    }
    
    /**
     * 切换节点展开/折叠状态
     */
    public void toggleNode(FileTreeNode node) {
        if (!node.isDirectory()) {
            return;
        }
        
        if (node.isExpanded()) {
            collapseNode(node);
        } else {
            expandNode(node);
        }
    }
    
    /**
     * 展开节点
     */
    public void expandNode(FileTreeNode node) {
        if (!node.isDirectory() || node.isExpanded()) {
            return;
        }
        
        node.setExpanded(true);
        
        // 如果没有子节点，需要加载
        if (!node.hasChildren() && listener != null) {
            node.setLoading(true);
            listener.onLoadNodeChildren(node);
        }
        
        rebuildFlattenedList();
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onNodeExpanded(node);
        }
    }
    
    /**
     * 折叠节点
     */
    public void collapseNode(FileTreeNode node) {
        if (!node.isDirectory() || !node.isExpanded()) {
            return;
        }
        
        node.setExpanded(false);
        node.setLoading(false);
        
        rebuildFlattenedList();
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onNodeCollapsed(node);
        }
    }
    
    /**
     * 为节点添加子节点
     */
    public void addChildrenToNode(FileTreeNode parentNode, List<FileTreeNode> children) {
        if (parentNode == null || children == null) {
            return;
        }
        
        parentNode.setLoading(false);
        parentNode.setChildren(children);
        
        // 设置子节点的深度
        for (FileTreeNode child : children) {
            child.setDepth(parentNode.getDepth() + 1);
        }
        
        rebuildFlattenedList();
        notifyDataSetChanged();
    }
    
    /**
     * 设置选中的路径
     */
    public void setSelectedPath(String path) {
        this.selectedPath = path;
        notifyDataSetChanged();
    }
    
    /**
     * 展开到指定路径
     */
    public void expandToPath(String targetPath) {
        for (FileTreeNode root : rootNodes) {
            if (root.expandToPath(targetPath)) {
                break;
            }
        }
        rebuildFlattenedList();
        notifyDataSetChanged();
    }
    
    /**
     * 查找指定路径的节点
     */
    public FileTreeNode findNodeByPath(String path) {
        for (FileTreeNode node : flattenedNodes) {
            if (node.getFullPath().equals(path)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 重新构建扁平化列表
     */
    private void rebuildFlattenedList() {
        flattenedNodes.clear();
        for (FileTreeNode root : rootNodes) {
            flattenTree(root, flattenedNodes);
        }
    }
    
    /**
     * 递归扁平化树结构
     */
    private void flattenTree(FileTreeNode node, List<FileTreeNode> result) {
        result.add(node);
        
        if (node.isExpanded() && node.hasChildren()) {
            for (FileTreeNode child : node.getChildren()) {
                flattenTree(child, result);
            }
        }
    }
    
    /**
     * 获取扁平化节点列表
     */
    public List<FileTreeNode> getFlattenedNodes() {
        return new ArrayList<>(flattenedNodes);
    }
    
    /**
     * 清空树
     */
    public void clear() {
        rootNodes.clear();
        flattenedNodes.clear();
        selectedPath = null;
        notifyDataSetChanged();
    }
    
    /**
     * 刷新指定节点
     */
    public void refreshNode(FileTreeNode node) {
        if (node == null) {
            return;
        }
        
        int index = flattenedNodes.indexOf(node);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }
    
    /**
     * 播放展开/折叠动画
     */
    private void animateExpansion(View expandIcon, boolean expanding) {
        float fromRotation = expanding ? 0f : 90f;
        float toRotation = expanding ? 90f : 0f;
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(expandIcon, "rotation", fromRotation, toRotation);
        animator.setDuration(200);
        animator.start();
    }
    
    static class FileTreeViewHolder extends RecyclerView.ViewHolder {
        Space indentSpace;
        ImageView expandIcon;
        ImageView fileIcon;
        TextView fileName;
        ProgressBar loadingIndicator;
        ImageView bookmarkIcon;
        
        public FileTreeViewHolder(@NonNull View itemView) {
            super(itemView);
            indentSpace = itemView.findViewById(R.id.indent_space);
            expandIcon = itemView.findViewById(R.id.expand_icon);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            loadingIndicator = itemView.findViewById(R.id.loading_indicator);
            bookmarkIcon = itemView.findViewById(R.id.bookmark_icon);
        }
    }
}

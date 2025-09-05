package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.termux.R;
import com.termux.app.models.FileType;
import com.termux.app.models.FileTypeUtils;
import com.termux.app.models.RemoteFileItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 远程文件列表适配器
 * 用于显示SFTP服务器上的文件和目录列表
 */
public class RemoteFileBrowserAdapter extends RecyclerView.Adapter<RemoteFileBrowserAdapter.FileViewHolder> {
    
    private List<RemoteFileItem> fileList;
    private Set<String> selectedFiles;
    private boolean selectionMode = false;
    private boolean showHiddenFiles = false;
    private String currentPath = "/";
    private OnFileClickListener listener;
    
    public interface OnFileClickListener {
        void onFileClick(RemoteFileItem file);
        void onFileLongClick(RemoteFileItem file);
        void onMoreOptionsClick(RemoteFileItem file, View anchorView);
    }
    
    /**
     * 书签状态提供接口
     */
    public interface BookmarkStateProvider {
        boolean isBookmarked(String path);
    }
    
    public interface BookmarkToggleListener {
        void onBookmarkToggle(RemoteFileItem file);
    }
    
    public RemoteFileBrowserAdapter(OnFileClickListener listener) {
        this.fileList = new ArrayList<>();
        this.selectedFiles = new HashSet<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_remote_file, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        RemoteFileItem file = fileList.get(position);
        
        // 设置文件图标
        holder.fileTypeIcon.setImageResource(FileTypeUtils.getFileTypeIcon(file.getType()));
        
        // 设置文件名
        holder.fileName.setText(file.getName());
        
        // 设置文件信息
        if (file.isDirectory()) {
            // 文件夹显示项目数量
            long itemCount = file.getSize(); // 假设size字段存储了子项数量
            if (itemCount > 0) {
                holder.fileInfo.setText(itemCount + " 项");
            } else {
                holder.fileInfo.setText("文件夹");
            }
        } else {
            // 文件显示大小和修改时间
            String fileInfo = formatFileInfo(file);
            holder.fileInfo.setText(fileInfo);
        }
        
        // 设置权限信息（默认隐藏，只在详细模式下显示）
        
        // 选择框状态
        holder.fileSelectionCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.fileSelectionCheckbox.setChecked(selectedFiles.contains(file.getPath()));
        
        // 书签指示器 - 为所有文件夹显示收藏图标
        if (file.isDirectory() && listener != null && listener instanceof BookmarkStateProvider) {
            BookmarkStateProvider bookmarkProvider = (BookmarkStateProvider) listener;
            boolean isBookmarked = bookmarkProvider.isBookmarked(file.getPath());
            holder.bookmarkIndicator.setVisibility(View.VISIBLE);
            // 收藏状态：实心星星，未收藏：空心星星
            holder.bookmarkIndicator.setImageResource(isBookmarked ? 
                android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            holder.bookmarkIndicator.setColorFilter(isBookmarked ? 
                0xFFFFD700 : 0x80757575); // 收藏：金色，未收藏：灰色半透明
            
            // 收藏图标点击事件 - 切换收藏状态
            holder.bookmarkIndicator.setOnClickListener(v -> {
                if (listener instanceof BookmarkToggleListener) {
                    ((BookmarkToggleListener) listener).onBookmarkToggle(file);
                }
            });
        } else {
            holder.bookmarkIndicator.setVisibility(View.GONE);
        }
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(file);
            } else if (listener != null) {
                listener.onFileClick(file);
            }
        });
        
        // 长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                enableSelectionMode();
                toggleSelection(file);
            }
            if (listener != null) {
                listener.onFileLongClick(file);
            }
            return true;
        });
        
        // 选择框点击事件
        holder.fileSelectionCheckbox.setOnClickListener(v -> toggleSelection(file));
        
        // 更多选项按钮
        holder.moreOptionsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreOptionsClick(file, v);
            }
        });
        
        // 隐藏文件样式
        if (file.isHidden()) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }
    
    @Override
    public int getItemCount() {
        if (showHiddenFiles) {
            return fileList.size();
        } else {
            int count = 0;
            for (RemoteFileItem file : fileList) {
                if (!file.isHidden()) {
                    count++;
                }
            }
            return count;
        }
    }
    
    /**
     * 更新文件列表
     */
    public void updateFiles(List<RemoteFileItem> files) {
        updateFiles(files, "/");
    }
    
    /**
     * 更新文件列表并设置当前路径
     */
    public void updateFiles(List<RemoteFileItem> files, String currentPath) {
        this.currentPath = currentPath;
        this.fileList.clear();
        
        // 如果不在根目录，添加返回上级目录的条目
        if (!"/".equals(currentPath)) {
            RemoteFileItem parentItem = createParentDirectoryItem(currentPath);
            this.fileList.add(parentItem);
        }
        
        if (files != null) {
            if (showHiddenFiles) {
                this.fileList.addAll(files);
            } else {
                for (RemoteFileItem file : files) {
                    if (!file.isHidden()) {
                        this.fileList.add(file);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 创建返回上级目录的文件项
     */
    private RemoteFileItem createParentDirectoryItem(String currentPath) {
        String parentPath = getParentPath(currentPath);
        
        RemoteFileItem parentItem = new RemoteFileItem();
        parentItem.setName("..");
        parentItem.setPath(parentPath);
        parentItem.setDirectory(true);
        parentItem.setType(FileType.DIRECTORY);
        parentItem.setSize(0);
        parentItem.setLastModified(0);
        parentItem.setPermissions("drwxr-xr-x");
        
        return parentItem;
    }
    
    /**
     * 获取父目录路径
     */
    private String getParentPath(String currentPath) {
        if ("/".equals(currentPath)) {
            return "/";
        }
        
        String path = currentPath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlashIndex);
    }
    
    /**
     * 检查指定位置是否是父目录项
     */
    public boolean isParentDirectoryItem(int position) {
        if (position == 0 && !fileList.isEmpty()) {
            RemoteFileItem item = fileList.get(0);
            return "..".equals(item.getName()) && item.getType() == FileType.DIRECTORY;
        }
        return false;
    }
    
    /**
     * 切换选择模式
     */
    public void toggleSelectionMode() {
        if (selectionMode) {
            disableSelectionMode();
        } else {
            enableSelectionMode();
        }
    }
    
    /**
     * 启用选择模式
     */
    public void enableSelectionMode() {
        selectionMode = true;
        notifyDataSetChanged();
    }
    
    /**
     * 禁用选择模式
     */
    public void disableSelectionMode() {
        selectionMode = false;
        selectedFiles.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 切换文件选择状态
     */
    private void toggleSelection(RemoteFileItem file) {
        if (selectedFiles.contains(file.getPath())) {
            selectedFiles.remove(file.getPath());
        } else {
            selectedFiles.add(file.getPath());
        }
        notifyDataSetChanged();
    }
    
    /**
     * 全选/取消全选
     */
    public void selectAll(boolean select) {
        selectedFiles.clear();
        if (select) {
            for (RemoteFileItem file : fileList) {
                selectedFiles.add(file.getPath());
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 获取选中的文件列表
     */
    public List<RemoteFileItem> getSelectedFiles() {
        List<RemoteFileItem> selected = new ArrayList<>();
        for (RemoteFileItem file : fileList) {
            if (selectedFiles.contains(file.getPath())) {
                selected.add(file);
            }
        }
        return selected;
    }
    
    /**
     * 获取选中文件数量
     */
    public int getSelectedCount() {
        return selectedFiles.size();
    }
    
    /**
     * 是否处于选择模式
     */
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    /**
     * 设置是否显示隐藏文件
     */
    public void setShowHiddenFiles(boolean showHidden) {
        this.showHiddenFiles = showHidden;
        notifyDataSetChanged();
    }
    
    /**
     * 获取指定位置的文件项
     */
    public RemoteFileItem getItem(int position) {
        if (position >= 0 && position < fileList.size()) {
            return fileList.get(position);
        }
        return null;
    }
    
    /**
     * 清空文件列表
     */
    public void clear() {
        fileList.clear();
        selectedFiles.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }
    
    /**
     * 格式化文件信息显示
     */
    private String formatFileInfo(RemoteFileItem file) {
        StringBuilder sb = new StringBuilder();
        
        // 添加文件大小
        String sizeStr = formatFileSize(file.getSize());
        sb.append(sizeStr);
        
        // 添加修改时间
        if (file.getLastModified() > 0) {
            sb.append(" · ");
            String dateStr = formatDate(file.getLastModified());
            sb.append(dateStr);
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 格式化日期时间
     */
    private String formatDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        return formatter.format(date);
    }
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        CheckBox fileSelectionCheckbox;
        ImageView fileTypeIcon;
        TextView fileName;
        TextView fileInfo;
        ImageView bookmarkIndicator;
        ImageButton moreOptionsButton;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileSelectionCheckbox = itemView.findViewById(R.id.file_selection_checkbox);
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
            bookmarkIndicator = itemView.findViewById(R.id.bookmark_indicator);
            moreOptionsButton = itemView.findViewById(R.id.more_options_button);
        }
    }
    
    /**
     * 适配器包装类，用于保持与DrawerFileAdapter的兼容性
     */
    public static class AdapterWrapper extends RemoteFileBrowserAdapter {
        private DrawerFileAdapter drawerAdapter;
        
        public AdapterWrapper(DrawerFileAdapter drawerAdapter) {
            super(null); // 不使用原有监听器
            this.drawerAdapter = drawerAdapter;
        }
        
        @Override
        public void updateFiles(List<RemoteFileItem> files, String path) {
            if (drawerAdapter != null) {
                drawerAdapter.updateFiles(files, path);
            }
        }
        
        @Override
        public void updateFiles(List<RemoteFileItem> files) {
            if (drawerAdapter != null) {
                drawerAdapter.updateFiles(files);
            }
        }
    }
}

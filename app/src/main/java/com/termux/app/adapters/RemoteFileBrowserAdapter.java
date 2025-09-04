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
import com.termux.app.models.FileTypeUtils;
import com.termux.app.models.RemoteFileItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private OnFileClickListener listener;
    
    public interface OnFileClickListener {
        void onFileClick(RemoteFileItem file);
        void onFileLongClick(RemoteFileItem file);
        void onMoreOptionsClick(RemoteFileItem file, View anchorView);
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
            holder.fileInfo.setText("文件夹");
        } else {
            holder.fileInfo.setText(file.getFileInfo());
        }
        
        // 设置权限信息（默认隐藏，只在详细模式下显示）
        holder.filePermissions.setVisibility(View.GONE);
        
        // 选择框状态
        holder.fileSelectionCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.fileSelectionCheckbox.setChecked(selectedFiles.contains(file.getPath()));
        
        // 书签指示器（暂时隐藏，待实现书签功能后启用）
        holder.bookmarkIndicator.setVisibility(View.GONE);
        
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
        this.fileList.clear();
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
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        CheckBox fileSelectionCheckbox;
        ImageView fileTypeIcon;
        TextView fileName;
        TextView fileInfo;
        TextView filePermissions;
        ImageView bookmarkIndicator;
        ImageButton moreOptionsButton;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileSelectionCheckbox = itemView.findViewById(R.id.file_selection_checkbox);
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
            filePermissions = itemView.findViewById(R.id.file_permissions);
            bookmarkIndicator = itemView.findViewById(R.id.bookmark_indicator);
            moreOptionsButton = itemView.findViewById(R.id.more_options_button);
        }
    }
}

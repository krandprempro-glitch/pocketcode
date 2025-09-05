package com.termux.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.RemoteFileItem;
import com.termux.app.models.FileType;
import com.termux.app.utils.FileIconUtils;
import com.termux.app.utils.FileUtils;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽屉文件适配器 - 专门用于抽屉中的文件树显示
 * 与原有RemoteFileBrowserAdapter类似，但针对抽屉环境优化
 */
public class DrawerFileAdapter extends RecyclerView.Adapter<DrawerFileAdapter.FileViewHolder> {
    private static final String LOG_TAG = "DrawerFileAdapter";
    
    private Context context;
    private List<RemoteFileItem> files;
    private String currentPath;
    private OnFileActionListener listener;
    private boolean showFileInfo;
    
    public interface OnFileActionListener {
        void onFileClick(RemoteFileItem file);
        void onFileMoreClick(RemoteFileItem file, View anchorView);
        void onDirectoryEnter(RemoteFileItem directory);
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
    
    public DrawerFileAdapter(Context context, OnFileActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.files = new ArrayList<>();
        this.showFileInfo = false; // 抽屉中默认不显示文件信息，节省空间
    }
    
    public void updateFiles(List<RemoteFileItem> newFiles, String path) {
        this.currentPath = path;
        this.files.clear();
        
        // 如果不在根目录，添加返回上级目录的条目
        if (!"/".equals(path)) {
            RemoteFileItem parentItem = createParentDirectoryItem(path);
            this.files.add(parentItem);
        }
        
        // 添加文件和目录
        if (newFiles != null) {
            this.files.addAll(newFiles);
        }
        
        notifyDataSetChanged();
        Logger.logInfo(LOG_TAG, "Files updated for path: " + path + ", count: " + files.size());
    }
    
    public void updateFiles(List<RemoteFileItem> newFiles) {
        updateFiles(newFiles, currentPath);
    }
    
    public void setShowFileInfo(boolean show) {
        if (this.showFileInfo != show) {
            this.showFileInfo = show;
            notifyDataSetChanged();
        }
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_file, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        RemoteFileItem file = files.get(position);
        bindFileViewHolder(holder, file, position);
    }
    
    private void bindFileViewHolder(FileViewHolder holder, RemoteFileItem file, int position) {
        // 设置文件名
        holder.fileName.setText(file.getName());
        
        // 设置文件图标
        int iconRes = FileIconUtils.getFileIcon(file);
        holder.fileIcon.setImageResource(iconRes);
        
        // 根据文件类型设置不同的文字颜色
        if (file.isDirectory()) {
            holder.fileName.setTextColor(context.getResources().getColor(R.color.directory_text_color, null));
        } else {
            holder.fileName.setTextColor(context.getResources().getColor(R.color.file_text_color, null));
        }
        
        // 设置文件信息 (大小、修改时间等)
        if (showFileInfo && !file.isDirectory()) {
            holder.fileInfo.setVisibility(View.VISIBLE);
            holder.fileInfo.setText(formatFileInfo(file));
        } else {
            holder.fileInfo.setVisibility(View.GONE);
        }
        
        // 设置书签指示器
        setupBookmarkIndicator(holder, file);
        
        // 设置更多操作按钮
        setupMoreButton(holder, file);
        
        // 设置点击事件
        setupClickListeners(holder, file);
    }
    
    /**
     * 格式化文件信息显示
     */
    private String formatFileInfo(RemoteFileItem file) {
        if (file.isDirectory()) {
            return "目录";
        }
        
        String sizeStr = FileUtils.formatFileSize(file.getSize());
        return sizeStr;
    }
    
    /**
     * 设置书签指示器
     */
    private void setupBookmarkIndicator(FileViewHolder holder, RemoteFileItem file) {
        if (listener != null && listener instanceof BookmarkStateProvider && file.isDirectory()) {
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
    }
    
    /**
     * 设置更多操作按钮
     */
    private void setupMoreButton(FileViewHolder holder, RemoteFileItem file) {
        // 只对文件显示更多按钮，目录通过长按操作
        if (!file.isDirectory()) {
            holder.fileMoreButton.setVisibility(View.VISIBLE);
            holder.fileMoreButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFileMoreClick(file, v);
                }
            });
        } else {
            holder.fileMoreButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners(FileViewHolder holder, RemoteFileItem file) {
        // 普通点击
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (file.isDirectory()) {
                    listener.onDirectoryEnter(file);
                } else {
                    listener.onFileClick(file);
                }
            }
        });
        
        // 长按操作 (主要用于目录的收藏等操作)
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && file.isDirectory()) {
                listener.onFileMoreClick(file, v);
                return true;
            }
            return false;
        });
    }
    
    @Override
    public int getItemCount() {
        return files.size();
    }
    
    public List<RemoteFileItem> getFiles() {
        return new ArrayList<>(files);
    }
    
    public String getCurrentPath() {
        return currentPath;
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
        if (position == 0 && !files.isEmpty()) {
            RemoteFileItem item = files.get(0);
            return "..".equals(item.getName()) && item.getType() == FileType.DIRECTORY;
        }
        return false;
    }
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        View indentView;
        ImageView fileIcon;
        TextView fileName;
        TextView fileInfo;
        ImageView bookmarkIndicator;
        ImageView fileMoreButton;
        
        FileViewHolder(View itemView) {
            super(itemView);
            indentView = itemView.findViewById(R.id.indent_view);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_info);
            bookmarkIndicator = itemView.findViewById(R.id.bookmark_indicator);
            fileMoreButton = itemView.findViewById(R.id.file_more_button);
        }
    }
}
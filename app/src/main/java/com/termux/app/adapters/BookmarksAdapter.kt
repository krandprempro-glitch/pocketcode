package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.DirectoryBookmark;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 书签列表适配器
 */
public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
    
    private List<DirectoryBookmark> bookmarks;
    private OnBookmarkActionListener listener;
    
    public interface OnBookmarkActionListener {
        void onBookmarkClick(DirectoryBookmark bookmark);
        void onBookmarkLongClick(DirectoryBookmark bookmark);
        void onBookmarkRemove(DirectoryBookmark bookmark);
    }
    
    public BookmarksAdapter(OnBookmarkActionListener listener) {
        this.bookmarks = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new BookmarkViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        DirectoryBookmark bookmark = bookmarks.get(position);
        
        // 设置书签图标
        holder.bookmarkIcon.setImageResource(android.R.drawable.btn_star);
        
        // 设置显示名称
        holder.bookmarkName.setText(bookmark.getDisplayName());
        
        // 设置路径
        holder.bookmarkPath.setText(bookmark.getFullPath());
        
        // 设置创建时间
        String timeStr = DATE_FORMAT.format(new Date(bookmark.getCreatedTime()));
        holder.bookmarkTime.setText(timeStr);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookmarkClick(bookmark);
            }
        });
        
        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBookmarkLongClick(bookmark);
            }
            return true;
        });
        
        // 设置删除按钮点击事件
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookmarkRemove(bookmark);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return bookmarks.size();
    }
    
    /**
     * 更新书签列表
     */
    public void updateBookmarks(List<DirectoryBookmark> newBookmarks) {
        this.bookmarks.clear();
        if (newBookmarks != null) {
            this.bookmarks.addAll(newBookmarks);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 添加书签
     */
    public void addBookmark(DirectoryBookmark bookmark) {
        if (bookmark != null) {
            bookmarks.add(0, bookmark); // 添加到顶部
            notifyItemInserted(0);
        }
    }
    
    /**
     * 删除书签
     */
    public void removeBookmark(DirectoryBookmark bookmark) {
        int index = bookmarks.indexOf(bookmark);
        if (index >= 0) {
            bookmarks.remove(index);
            notifyItemRemoved(index);
        }
    }
    
    /**
     * 获取书签列表
     */
    public List<DirectoryBookmark> getBookmarks() {
        return new ArrayList<>(bookmarks);
    }
    
    /**
     * 清空书签列表
     */
    public void clear() {
        bookmarks.clear();
        notifyDataSetChanged();
    }
    
    static class BookmarkViewHolder extends RecyclerView.ViewHolder {
        ImageView bookmarkIcon;
        TextView bookmarkName;
        TextView bookmarkPath;
        TextView bookmarkTime;
        ImageView removeButton;
        
        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            bookmarkIcon = itemView.findViewById(R.id.bookmark_icon);
            bookmarkName = itemView.findViewById(R.id.bookmark_name);
            bookmarkPath = itemView.findViewById(R.id.bookmark_path);
            bookmarkTime = itemView.findViewById(R.id.bookmark_time);
            removeButton = itemView.findViewById(R.id.remove_button);
        }
    }
}
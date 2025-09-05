package com.termux.filebrowser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.RemoteFileItem;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RemoteFileAdapter extends RecyclerView.Adapter<RemoteFileAdapter.FileViewHolder> {
    
    private List<RemoteFileItem> files = new ArrayList<>();
    private OnFileItemClickListener clickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    public interface OnFileItemClickListener {
        void onFileItemClick(RemoteFileItem file);
        void onFileItemLongClick(RemoteFileItem file);
    }
    
    public void setOnFileItemClickListener(OnFileItemClickListener listener) {
        this.clickListener = listener;
    }
    
    public void updateFiles(List<RemoteFileItem> newFiles) {
        this.files.clear();
        if (newFiles != null) {
            this.files.addAll(newFiles);
        }
        notifyDataSetChanged();
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
        RemoteFileItem file = files.get(position);
        holder.bind(file);
    }
    
    @Override
    public int getItemCount() {
        return files.size();
    }
    
    class FileViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIcon;
        private TextView tvName;
        private TextView tvSize;
        private TextView tvModified;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.file_type_icon);
            tvName = itemView.findViewById(R.id.file_name);
            tvSize = itemView.findViewById(R.id.file_info);
            tvModified = itemView.findViewById(R.id.file_info);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onFileItemClick(files.get(getAdapterPosition()));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onFileItemLongClick(files.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
        
        public void bind(RemoteFileItem file) {
            tvName.setText(file.getName());
            
            // 设置文件图标
            if (file.isDirectory()) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            }
            
            // 设置文件信息 (大小和时间)
            StringBuilder info = new StringBuilder();
            if (!file.isDirectory()) {
                info.append(formatFileSize(file.getSize()));
            }
            
            if (file.getLastModified() > 0) {
                if (info.length() > 0) {
                    info.append(" · ");
                }
                info.append(dateFormat.format(new Date(file.getLastModified())));
            }
            
            tvSize.setText(info.toString());
            
        }
        
        private String formatFileSize(long size) {
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
    }
}

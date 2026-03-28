package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.GitChangedFile;

/**
 * Adapter for displaying changed files within an expanded commit
 */
public class ChangedFileAdapter extends ListAdapter<GitChangedFile, ChangedFileAdapter.FileViewHolder> {

    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(GitChangedFile file);
    }

    public ChangedFileAdapter() {
        super(new FileDiffCallback());
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_changed_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        GitChangedFile file = getItem(position);
        android.util.Log.d("ChangedFileAdapter", "onBindViewHolder called: position=" + position + ", file=" + file.getPath());
        holder.bind(file);
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView statusView;
        private final TextView pathView;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            statusView = itemView.findViewById(R.id.file_status);
            pathView = itemView.findViewById(R.id.file_path);
        }

        void bind(GitChangedFile file) {
            statusView.setText(file.getStatus());
            pathView.setText(file.getPath());

            // Set status color based on type
            int colorRes;
            String status = file.getStatus();
            if (GitChangedFile.STATUS_ADDED.equals(status)) {
                colorRes = R.color.git_status_added;
            } else if (GitChangedFile.STATUS_DELETED.equals(status)) {
                colorRes = R.color.git_status_deleted;
            } else {
                colorRes = R.color.git_status_modified;
            }
            statusView.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFileClick(file);
                }
            });
        }
    }

    static class FileDiffCallback extends DiffUtil.ItemCallback<GitChangedFile> {
        @Override
        public boolean areItemsTheSame(@NonNull GitChangedFile oldItem, @NonNull GitChangedFile newItem) {
            return oldItem.getPath().equals(newItem.getPath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GitChangedFile oldItem, @NonNull GitChangedFile newItem) {
            return oldItem.getPath().equals(newItem.getPath()) &&
                   oldItem.getStatus().equals(newItem.getStatus());
        }
    }
}
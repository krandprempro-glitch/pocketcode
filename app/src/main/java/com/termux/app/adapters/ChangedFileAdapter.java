package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.GitChangedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying changed files within an expanded commit
 */
public class ChangedFileAdapter extends RecyclerView.Adapter<ChangedFileAdapter.FileViewHolder> {

    private final List<GitChangedFile> files = new ArrayList<>();
    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(GitChangedFile file);
    }

    public ChangedFileAdapter() {}

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_changed_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        GitChangedFile file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<GitChangedFile> newFiles) {
        files.clear();
        if (newFiles != null) {
            files.addAll(newFiles);
        }
        notifyDataSetChanged();
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
            switch (file.getStatus()) {
                case "A":
                    colorRes = R.color.git_status_added;
                    break;
                case "D":
                    colorRes = R.color.git_status_deleted;
                    break;
                case "R":
                case "M":
                default:
                    colorRes = R.color.git_status_modified;
                    break;
            }
            statusView.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFileClick(file);
                }
            });
        }
    }
}
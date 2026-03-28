package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.GitChangedFile;
import com.termux.app.models.GitCommit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GitCommitAdapter extends ListAdapter<GitCommit, GitCommitAdapter.CommitViewHolder> {

    private static final long SECONDS = 60;
    private static final long MINUTES = 3600;
    private static final long HOURS = 86400;
    private static final long DAYS = 2592000;

    private String expandedCommitHash = null;
    private List<GitChangedFile> expandedFiles = Collections.emptyList();
    private OnCommitExpandListener expandListener;
    private OnFileClickListener fileClickListener;
    private final ChangedFileAdapter changedFileAdapter;

    public interface OnCommitExpandListener {
        void onCommitExpand(String commitHash);
    }

    public interface OnFileClickListener {
        void onFileClick(String commitHash, GitChangedFile file);
    }

    public GitCommitAdapter() {
        super(new CommitDiffCallback());
        changedFileAdapter = new ChangedFileAdapter();
    }

    public void setOnCommitExpandListener(OnCommitExpandListener listener) {
        this.expandListener = listener;
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.fileClickListener = listener;
    }

    /**
     * Set the expanded files for a specific commit hash
     */
    public void setExpandedFiles(String commitHash, List<GitChangedFile> files) {
        if (commitHash != null && commitHash.equals(expandedCommitHash)) {
            expandedFiles = files != null ? new ArrayList<>(files) : Collections.emptyList();
            notifyDataSetChanged();
        }
    }

    /**
     * Clear expanded state
     */
    public void clearExpanded() {
        expandedCommitHash = null;
        expandedFiles = Collections.emptyList();
        notifyDataSetChanged();
    }

    private int findExpandedIndex() {
        if (expandedCommitHash == null) return -1;
        List<GitCommit> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getFullHash().equals(expandedCommitHash)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public CommitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_git_commit, parent, false);
        return new CommitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommitViewHolder holder, int position) {
        GitCommit commit = getItem(position);
        boolean isExpanded = commit.getFullHash().equals(expandedCommitHash);
        holder.bind(commit, isExpanded, isExpanded ? expandedFiles : Collections.emptyList());
    }

    class CommitViewHolder extends RecyclerView.ViewHolder {
        private final TextView hashView;
        private final TextView messageView;
        private final TextView authorView;
        private final TextView timeView;
        private final LinearLayout changedFilesContainer;
        private final RecyclerView changedFilesRecyclerView;

        CommitViewHolder(@NonNull View itemView) {
            super(itemView);
            hashView = itemView.findViewById(R.id.commit_hash);
            messageView = itemView.findViewById(R.id.commit_message);
            authorView = itemView.findViewById(R.id.commit_author);
            timeView = itemView.findViewById(R.id.commit_time);
            changedFilesContainer = itemView.findViewById(R.id.changed_files_container);
            changedFilesRecyclerView = itemView.findViewById(R.id.changed_files_recycler_view);

            changedFilesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            changedFilesRecyclerView.setAdapter(changedFileAdapter);
        }

        void bind(GitCommit commit, boolean isExpanded, List<GitChangedFile> files) {
            hashView.setText(commit.getHash());
            messageView.setText(commit.getMessage());
            authorView.setText(commit.getAuthor());
            timeView.setText(formatTimeAgo(commit.getTimestamp()));

            // Handle click to toggle expand/collapse
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    GitCommit clickedCommit = getItem(pos);
                    String commitHash = clickedCommit.getFullHash();

                    if (commitHash.equals(expandedCommitHash)) {
                        // Collapse
                        expandedCommitHash = null;
                        expandedFiles = Collections.emptyList();
                    } else {
                        // Expand - set hash first so UI updates immediately
                        expandedCommitHash = commitHash;
                        expandedFiles = Collections.emptyList();
                        if (expandListener != null) {
                            expandListener.onCommitExpand(commitHash);
                        }
                    }

                    // Refresh this item
                    notifyItemChanged(pos);

                    // Find and refresh previously expanded item
                    int oldIndex = findExpandedIndex();
                    if (oldIndex != -1 && oldIndex != pos) {
                        notifyItemChanged(oldIndex);
                    }
                }
            });

            // Update expanded state UI
            if (isExpanded) {
                changedFilesContainer.setVisibility(View.VISIBLE);
                changedFileAdapter.submitList(files);
                changedFileAdapter.setOnFileClickListener(file -> {
                    if (fileClickListener != null) {
                        fileClickListener.onFileClick(commit.getFullHash(), file);
                    }
                });
            } else {
                changedFilesContainer.setVisibility(View.GONE);
                changedFileAdapter.submitList(Collections.emptyList());
            }
        }

        private String formatTimeAgo(long timestamp) {
            if (timestamp <= 0) return "";

            long now = System.currentTimeMillis() / 1000;
            long diff = now - timestamp;

            if (diff < SECONDS) {
                return "just now";
            } else if (diff < MINUTES) {
                return diff / SECONDS + " min ago";
            } else if (diff < HOURS) {
                return diff / MINUTES + " hours ago";
            } else if (diff < DAYS) {
                return diff / HOURS + " days ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                return sdf.format(new Date(timestamp * 1000));
            }
        }
    }

    static class CommitDiffCallback extends DiffUtil.ItemCallback<GitCommit> {
        @Override
        public boolean areItemsTheSame(@NonNull GitCommit oldItem, @NonNull GitCommit newItem) {
            return oldItem.getFullHash().equals(newItem.getFullHash());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GitCommit oldItem, @NonNull GitCommit newItem) {
            return oldItem.getHash().equals(newItem.getHash()) &&
                   oldItem.getMessage().equals(newItem.getMessage());
        }
    }
}
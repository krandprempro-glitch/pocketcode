package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.app.R;
import com.termux.app.models.GitCommit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GitCommitAdapter extends ListAdapter<GitCommit, GitCommitAdapter.CommitViewHolder> {

    public GitCommitAdapter() {
        super(new CommitDiffCallback());
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
        holder.bind(getItem(position));
    }

    static class CommitViewHolder extends RecyclerView.ViewHolder {
        private final TextView hashView;
        private final TextView messageView;
        private final TextView authorView;
        private final TextView timeView;

        CommitViewHolder(@NonNull View itemView) {
            super(itemView);
            hashView = itemView.findViewById(R.id.commit_hash);
            messageView = itemView.findViewById(R.id.commit_message);
            authorView = itemView.findViewById(R.id.commit_author);
            timeView = itemView.findViewById(R.id.commit_time);
        }

        void bind(GitCommit commit) {
            hashView.setText(commit.getHash());
            messageView.setText(commit.getMessage());
            authorView.setText(commit.getAuthor());
            timeView.setText(formatTimeAgo(commit.getTimestamp()));
        }

        private String formatTimeAgo(long timestamp) {
            if (timestamp <= 0) return "";

            long now = System.currentTimeMillis() / 1000;
            long diff = now - timestamp;

            if (diff < 60) {
                return "just now";
            } else if (diff < 3600) {
                return diff / 60 + " min ago";
            } else if (diff < 86400) {
                return diff / 3600 + " hours ago";
            } else if (diff < 2592000) {
                return diff / 86400 + " days ago";
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

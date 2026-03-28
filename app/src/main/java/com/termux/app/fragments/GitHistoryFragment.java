package com.termux.app.fragments;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.decorations.DividerItemDecoration;
import com.termux.app.models.GitBranch;
import com.termux.app.models.GitChangedFile;
import com.termux.app.models.GitCommit;
import com.termux.app.viewmodels.GitHistoryViewModel;
import com.termux.app.adapters.GitCommitAdapter;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class GitHistoryFragment extends Fragment {
    private GitHistoryViewModel viewModel;

    private ProgressBar progressBar;
    private ProgressBar loadingMore;
    private TextView statusText;
    private TextView tvCurrentBranch;
    private View branchChip;
    private RecyclerView commitsRecyclerView;
    private TextView commitsHeader;
    private Button retryButton;

    private GitCommitAdapter commitAdapter;
    private List<GitBranch> cachedBranches;
    private RecyclerView.OnScrollListener scrollListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_git_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GitHistoryViewModel.class);

        initViews(view);
        setupObservers();
        setupListeners();
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        loadingMore = view.findViewById(R.id.loading_more);
        statusText = view.findViewById(R.id.status_text);
        tvCurrentBranch = view.findViewById(R.id.tv_current_branch);
        branchChip = view.findViewById(R.id.branch_chip);
        commitsRecyclerView = view.findViewById(R.id.commits_recycler_view);
        commitsHeader = view.findViewById(R.id.commits_header);
        retryButton = view.findViewById(R.id.retry_button);

        commitsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commitAdapter = new GitCommitAdapter();
        commitsRecyclerView.setAdapter(commitAdapter);

        // Add divider between commit items
        commitsRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext()));

        // Add scroll listener for pagination
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (total <= 0) return;
                if (last >= total - 3 && !viewModel.isLoading() && viewModel.hasMore()) {
                    viewModel.loadMore();
                }
            }
        };
        commitsRecyclerView.addOnScrollListener(scrollListener);
    }

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateUiState);
        viewModel.getBranches().observe(getViewLifecycleOwner(), this::updateBranches);
        viewModel.getCommits().observe(getViewLifecycleOwner(), this::updateCommits);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showError);
    }

    private void setupListeners() {
        retryButton.setOnClickListener(v -> viewModel.refresh());
        branchChip.setOnClickListener(v -> showBranchSwitchDialog());

        // Handle commit expand/collapse
        commitAdapter.setOnCommitExpandListener(commitHash -> loadChangedFiles(commitHash));

        // Handle file click - open GitFileDetailActivity
        commitAdapter.setOnFileClickListener((commitHash, file) -> {
            Intent intent = new Intent(getContext(), GitFileDetailActivity.class);
            intent.putExtra(GitFileDetailActivity.EXTRA_COMMIT_HASH, commitHash);
            intent.putExtra(GitFileDetailActivity.EXTRA_FILE_PATH, file.getPath());
            String workDir = viewModel.getCurrentPath().getValue();
            intent.putExtra(GitFileDetailActivity.EXTRA_WORKDIR, workDir != null ? workDir : "");
            startActivity(intent);
        });
    }

    private void updateUiState(GitHistoryViewModel.UiState state) {
        switch (state) {
            case NOT_CONNECTED:
                progressBar.setVisibility(View.GONE);
                statusBar.setVisibility(View.VISIBLE);
                statusText.setText("请先在文件浏览页连接 SSH");
                branchChip.setVisibility(View.GONE);
                commitsRecyclerView.setVisibility(View.GONE);
                commitsHeader.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                break;
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                statusBar.setVisibility(View.GONE);
                branchChip.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                statusBar.setVisibility(View.GONE);
                branchChip.setVisibility(View.VISIBLE);
                commitsRecyclerView.setVisibility(View.VISIBLE);
                commitsHeader.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                statusBar.setVisibility(View.VISIBLE);
                branchChip.setVisibility(View.GONE);
                retryButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateBranches(List<GitBranch> branches) {
        cachedBranches = branches;
        for (GitBranch branch : branches) {
            if (branch.isCurrent()) {
                tvCurrentBranch.setText(branch.getName());
                break;
            }
        }
    }

    private void updateCommits(List<GitCommit> commits) {
        commitAdapter.submitList(commits);
        // Show loadingMore when pagination loading is in progress
        if (viewModel.isPaginationLoading()) {
            loadingMore.setVisibility(View.VISIBLE);
        } else {
            loadingMore.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (loadingMore != null) {
            loadingMore.setVisibility(View.GONE);
        }
        if (message != null && !message.isEmpty()) {
            statusBar.setVisibility(View.VISIBLE);
            statusText.setText(message);
        } else {
            statusBar.setVisibility(View.GONE);
        }
    }

    /**
     * Load the list of changed files for a commit
     */
    private void loadChangedFiles(String commitHash) {
        String path = viewModel.getCurrentPath().getValue();
        if (path == null || path.isEmpty() || !viewModel.isConnected()) {
            return;
        }

        // Use git show --name-status to get file list with status
        String command = "git -C \"" + path + "\" show --name-status --pretty=format: \"" + commitHash + "\" 2>&1";
        Logger.logDebug("GitHistoryFragment", "Loading changed files: " + command);

        viewModel.executeGitCommand(command, output -> parseChangedFiles(commitHash, output));
    }

    /**
     * Parse the output of git show --name-status
     * Format: status\tpath (e.g., "M\tfile.txt", "A\tnewfile.txt")
     */
    private void parseChangedFiles(String commitHash, String output) {
        List<GitChangedFile> files = new ArrayList<>();
        if (output != null && !output.isEmpty()) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                // Skip lines that are part of commit metadata
                if (line.startsWith("commit ") || line.startsWith("Author:") ||
                    line.startsWith("Date:") || line.contains("diff --git")) {
                    continue;
                }
                // Parse status\tpath format
                String[] parts = line.split("\t", 2);
                if (parts.length >= 2) {
                    String status = parts[0].trim();
                    String filePath = parts[1].trim();
                    if (!status.isEmpty() && !filePath.isEmpty()) {
                        files.add(new GitChangedFile(filePath, status));
                    }
                }
            }
        }
        Logger.logDebug("GitHistoryFragment", "Parsed " + files.size() + " changed files for " + commitHash);
        commitAdapter.setExpandedFiles(commitHash, files);
    }

    private void showBranchSwitchDialog() {
        if (cachedBranches == null || cachedBranches.isEmpty()) {
            return;
        }

        String[] branchNames = new String[cachedBranches.size()];
        String currentBranch = "";
        for (int i = 0; i < cachedBranches.size(); i++) {
            GitBranch branch = cachedBranches.get(i);
            branchNames[i] = branch.getName();
            if (branch.isCurrent()) {
                currentBranch = branch.getName();
            }
        }

        int selectedIndex = -1;
        for (int i = 0; i < cachedBranches.size(); i++) {
            if (cachedBranches.get(i).isCurrent()) {
                selectedIndex = i;
                break;
            }
        }

        String finalCurrentBranch = currentBranch;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("切换分支")
                .setSingleChoiceItems(branchNames, selectedIndex, (d, which) -> {
                    d.dismiss();
                    String targetBranch = branchNames[which];
                    if (!targetBranch.equals(finalCurrentBranch)) {
                        viewModel.switchBranch(targetBranch);
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    /**
     * 同步文件浏览器的当前目录
     */
    public void onDirectoryChanged(String newPath) {
        Logger.logDebug("GitHistoryFragment", "onDirectoryChanged called with path: " + newPath);
        Logger.logDebug("GitHistoryFragment", "viewModel is null: " + (viewModel == null));
        if (viewModel != null) {
            Logger.logDebug("GitHistoryFragment", "isConnected: " + viewModel.isConnected());
        }
        if (viewModel != null && viewModel.isConnected() && newPath != null && !newPath.isEmpty()) {
            viewModel.loadGitHistory(newPath);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.logDebug("GitHistoryFragment", "onResume called");
        Logger.logDebug("GitHistoryFragment", "isConnected: " + (viewModel != null && viewModel.isConnected()));
        if (viewModel != null && viewModel.isConnected()) {
            String path = viewModel.getCurrentPath().getValue();
            Logger.logDebug("GitHistoryFragment", "currentPath from viewModel: " + path);
            if (path == null || path.isEmpty()) {
                // 默认使用 SSH 家目录
                viewModel.loadGitHistory("/");
            } else {
                viewModel.refresh();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (commitsRecyclerView != null && scrollListener != null) {
            commitsRecyclerView.removeOnScrollListener(scrollListener);
        }
        super.onDestroyView();
    }
}

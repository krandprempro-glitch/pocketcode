package com.termux.app.fragments;

import android.app.AlertDialog;
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
import com.termux.app.models.GitBranch;
import com.termux.app.models.GitCommit;
import com.termux.app.viewmodels.GitHistoryViewModel;
import com.termux.app.adapters.GitCommitAdapter;
import com.termux.shared.logger.Logger;

import java.util.List;

public class GitHistoryFragment extends Fragment {
    private GitHistoryViewModel viewModel;

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView tvCurrentBranch;
    private View branchChip;
    private RecyclerView commitsRecyclerView;
    private TextView commitsHeader;
    private Button retryButton;

    private GitCommitAdapter commitAdapter;
    private List<GitBranch> cachedBranches;

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
        statusText = view.findViewById(R.id.status_text);
        tvCurrentBranch = view.findViewById(R.id.tv_current_branch);
        branchChip = view.findViewById(R.id.branch_chip);
        commitsRecyclerView = view.findViewById(R.id.commits_recycler_view);
        commitsHeader = view.findViewById(R.id.commits_header);
        retryButton = view.findViewById(R.id.retry_button);

        commitsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commitAdapter = new GitCommitAdapter();
        commitsRecyclerView.setAdapter(commitAdapter);

        // Add scroll listener for pagination
        commitsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        });
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
    }

    private void updateUiState(GitHistoryViewModel.UiState state) {
        switch (state) {
            case NOT_CONNECTED:
                progressBar.setVisibility(View.GONE);
                statusText.setText("请先在文件浏览页连接 SSH");
                branchChip.setVisibility(View.GONE);
                commitsRecyclerView.setVisibility(View.GONE);
                commitsHeader.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                break;
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                statusText.setText("加载中...");
                branchChip.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText("");
                branchChip.setVisibility(View.VISIBLE);
                commitsRecyclerView.setVisibility(View.VISIBLE);
                commitsHeader.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
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
    }

    private void showError(String message) {
        if (message != null && !message.isEmpty()) {
            statusText.setText(message);
        }
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
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
}

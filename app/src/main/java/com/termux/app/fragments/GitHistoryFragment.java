package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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

import java.util.List;

public class GitHistoryFragment extends Fragment {
    private GitHistoryViewModel viewModel;

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView currentBranch;
    private LinearLayout branchSection;
    private LinearLayout branchListContainer;
    private RecyclerView commitsRecyclerView;
    private Button retryButton;

    private GitCommitAdapter commitAdapter;

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
        currentBranch = view.findViewById(R.id.current_branch);
        branchSection = view.findViewById(R.id.branch_section);
        branchListContainer = view.findViewById(R.id.branch_list_container);
        commitsRecyclerView = view.findViewById(R.id.commits_recycler_view);
        retryButton = view.findViewById(R.id.retry_button);

        commitsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commitAdapter = new GitCommitAdapter();
        commitsRecyclerView.setAdapter(commitAdapter);
    }

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateUiState);
        viewModel.getBranches().observe(getViewLifecycleOwner(), this::updateBranches);
        viewModel.getCommits().observe(getViewLifecycleOwner(), this::updateCommits);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showError);
    }

    private void setupListeners() {
        retryButton.setOnClickListener(v -> viewModel.refresh());
    }

    private void updateUiState(GitHistoryViewModel.UiState state) {
        switch (state) {
            case NOT_CONNECTED:
                progressBar.setVisibility(View.GONE);
                statusText.setText("请先在文件浏览页连接 SSH");
                branchSection.setVisibility(View.GONE);
                commitsRecyclerView.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                break;
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                statusText.setText("加载中...");
                retryButton.setVisibility(View.GONE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText("");
                branchSection.setVisibility(View.VISIBLE);
                commitsRecyclerView.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateBranches(List<GitBranch> branches) {
        branchListContainer.removeAllViews();

        for (GitBranch branch : branches) {
            if (branch.isCurrent()) {
                currentBranch.setText(branch.getName());
            }

            TextView tag = new TextView(requireContext());
            tag.setText(branch.getName());
            tag.setTextSize(12);
            tag.setPadding(16, 8, 16, 8);
            tag.setBackgroundResource(branch.isCurrent() ?
                android.R.color.holo_blue_light :
                android.R.color.darker_gray);
            tag.setTextColor(android.graphics.Color.WHITE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 0, 4, 0);
            tag.setLayoutParams(params);
            branchListContainer.addView(tag);
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

    /**
     * 同步文件浏览器的当前目录
     */
    public void onDirectoryChanged(String newPath) {
        if (viewModel != null && viewModel.isConnected() && newPath != null && !newPath.isEmpty()) {
            viewModel.loadGitHistory(newPath);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel.isConnected()) {
            String path = viewModel.getCurrentPath().getValue();
            if (path == null || path.isEmpty()) {
                // 默认使用 SSH 家目录
                viewModel.loadGitHistory("/");
            } else {
                viewModel.refresh();
            }
        }
    }
}

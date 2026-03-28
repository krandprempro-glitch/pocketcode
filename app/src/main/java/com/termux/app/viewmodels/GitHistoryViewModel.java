package com.termux.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.termux.app.models.GitBranch;
import com.termux.app.models.GitCommit;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GitHistoryViewModel extends ViewModel {

    public enum UiState {
        NOT_CONNECTED,
        LOADING,
        SUCCESS,
        ERROR
    }

    private static final int PAGE_SIZE = 30;
    private int loadedCount = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private final SFTPConnectionManager sftpManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.NOT_CONNECTED);
    private final MutableLiveData<List<GitBranch>> branches = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GitCommit>> commits = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> currentPath = new MutableLiveData<>("");

    public GitHistoryViewModel() {
        this.sftpManager = SFTPConnectionManager.getInstance();
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<List<GitBranch>> getBranches() {
        return branches;
    }

    public LiveData<List<GitCommit>> getCommits() {
        return commits;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getCurrentPath() {
        return currentPath;
    }

    public boolean isConnected() {
        return sftpManager.isConnected();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public int getLoadedCount() {
        return loadedCount;
    }

    public void loadGitHistory(String remotePath) {
        Logger.logDebug("GitHistoryViewModel", "loadGitHistory called with path: " + remotePath);
        Logger.logDebug("GitHistoryViewModel", "isConnected: " + sftpManager.isConnected());

        if (!sftpManager.isConnected()) {
            uiState.setValue(UiState.NOT_CONNECTED);
            return;
        }

        // Reset pagination for fresh load
        loadedCount = 0;
        hasMore = true;
        isLoading = false;

        currentPath.setValue(remotePath);
        uiState.setValue(UiState.LOADING);

        // 1. 获取分支列表，使用 -C 指定工作目录
        String branchCommand = "git -C \"" + remotePath + "\" branch -a --no-color 2>&1";
        Logger.logDebug("GitHistoryViewModel", "Executing branch command: " + branchCommand);

        disposables.add(
            sftpManager.executeCommand(branchCommand)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::parseBranches, this::handleError)
        );
    }

    private void parseBranches(String output) {
        Logger.logDebug("GitHistoryViewModel", "parseBranches received output: " + output);
        List<GitBranch> branchList = new ArrayList<>();
        String[] lines = output.split("\n");
        String currentBranchName = "";

        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            // 跳过错误输出行（如 "fatal: not a git repository"）
            if (line.startsWith("fatal:") || line.startsWith("Cloning into")) {
                continue;
            }

            boolean isCurrent = line.startsWith("*");
            String name = line.replaceFirst("^\\*\\s*", "").trim();

            branchList.add(new GitBranch(name, isCurrent));
            if (isCurrent) {
                currentBranchName = name;
            }
        }

        branches.setValue(branchList);

        // 2. 获取当前分支的 Commit 历史，使用 -C 指定工作目录
        String path = currentPath.getValue();
        String logCommand = "git -C \"" + path + "\" log --oneline -" + PAGE_SIZE + " --skip " + loadedCount + " --format=\"%H|%s|%an|%ad\" --date=unix 2>&1";

        disposables.add(
            sftpManager.executeCommand(logCommand)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::parseCommits, this::handleError)
        );
    }

    private void parseCommits(String output) {
        Logger.logDebug("GitHistoryViewModel", "parseCommits received output: " + output);
        List<GitCommit> commitList = new ArrayList<>();
        String[] lines = output.split("\n");

        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            // 跳过错误输出行
            if (line.startsWith("fatal:") || line.startsWith("Cloning into")) {
                continue;
            }

            // 格式: hash|message|author|timestamp
            String[] parts = line.split("\\|", 4);
            if (parts.length >= 2) {
                String fullHash = parts[0].trim();
                String shortHash = fullHash.length() > 7 ? fullHash.substring(0, 7) : fullHash;
                String message = parts[1].trim();
                String author = parts.length > 2 ? parts[2].trim() : "";
                long timestamp = parts.length > 3 ? parseTimestamp(parts[3].trim()) : 0;

                commitList.add(new GitCommit(shortHash, fullHash, message, author, timestamp));
            }
        }

        // Append new commits to existing list
        List<GitCommit> currentCommits = commits.getValue();
        if (currentCommits == null) {
            currentCommits = new ArrayList<>();
        }
        if (loadedCount == 0) {
            // Fresh load, replace the list
            commits.setValue(commitList);
        } else {
            // Pagination load, append to existing list
            List<GitCommit> newList = new ArrayList<>(currentCommits);
            newList.addAll(commitList);
            commits.setValue(newList);
        }

        // Update pagination state
        loadedCount += commitList.size();
        hasMore = (commitList.size() >= PAGE_SIZE);
        isLoading = false;

        uiState.setValue(UiState.SUCCESS);
    }

    private long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleError(Throwable error) {
        Logger.logDebug("GitHistoryViewModel", "handleError called, error: " + error);
        String msg = error.getMessage();
        if (msg != null && (msg.contains("不是 Git 仓库") || msg.contains("not a git repository"))) {
            errorMessage.setValue("该目录不是 Git 仓库");
        } else if (msg != null && msg.contains("未建立SSH连接")) {
            uiState.setValue(UiState.NOT_CONNECTED);
            return;
        } else {
            errorMessage.setValue(msg != null ? msg : "加载失败");
        }
        uiState.setValue(UiState.ERROR);
    }

    public void refresh() {
        String path = currentPath.getValue();
        if (path != null && !path.isEmpty()) {
            loadGitHistory(path);
        }
    }

    public void loadMore() {
        if (isLoading || !hasMore || !sftpManager.isConnected()) {
            return;
        }

        String path = currentPath.getValue();
        if (path == null || path.isEmpty()) {
            return;
        }

        isLoading = true;

        String logCommand = "git -C \"" + path + "\" log --oneline -" + PAGE_SIZE + " --skip " + loadedCount + " --format=\"%H|%s|%an|%ad\" --date=unix 2>&1";
        Logger.logDebug("GitHistoryViewModel", "loadMore: " + logCommand);

        disposables.add(
            sftpManager.executeCommand(logCommand)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::parseCommits, error -> {
                    isLoading = false;
                    handleError(error);
                })
        );
    }

    public void switchBranch(String branchName) {
        String path = currentPath.getValue();
        if (path == null || path.isEmpty() || !sftpManager.isConnected()) {
            return;
        }
        uiState.setValue(UiState.LOADING);
        String checkoutCommand = "git -C \"" + path + "\" checkout \"" + branchName + "\" 2>&1";
        Logger.logDebug("GitHistoryViewModel", "Switching branch: " + checkoutCommand);
        disposables.add(
            sftpManager.executeCommand(checkoutCommand)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Logger.logDebug("GitHistoryViewModel", "Branch switch result: " + result);
                    // Refresh after checkout
                    loadGitHistory(path);
                }, this::handleError)
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
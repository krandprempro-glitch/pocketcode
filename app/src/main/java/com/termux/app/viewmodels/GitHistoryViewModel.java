package com.termux.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.termux.app.models.GitBranch;
import com.termux.app.models.GitCommit;
import com.termux.app.sftp.SFTPConnectionManager;

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

    public void loadGitHistory(String remotePath) {
        if (!sftpManager.isConnected()) {
            uiState.setValue(UiState.NOT_CONNECTED);
            return;
        }

        currentPath.setValue(remotePath);
        uiState.setValue(UiState.LOADING);

        // 1. 获取分支列表
        disposables.add(
            sftpManager.executeCommand("git branch -a --no-color")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::parseBranches, this::handleError)
        );
    }

    private void parseBranches(String output) {
        List<GitBranch> branchList = new ArrayList<>();
        String[] lines = output.split("\n");
        String currentBranchName = "";

        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            boolean isCurrent = line.startsWith("*");
            String name = line.replaceFirst("^\\*\\s*", "").trim();

            branchList.add(new GitBranch(name, isCurrent));
            if (isCurrent) {
                currentBranchName = name;
            }
        }

        branches.setValue(branchList);

        // 2. 获取当前分支的 Commit 历史
        String logCommand = "git log --oneline -20 --format=\"%H|%s|%an|%ad\" --date=unix";
        if (!currentBranchName.isEmpty()) {
            logCommand = "git -C \"" + currentPath.getValue() + "\" log --oneline -20 --format=\"%H|%s|%an|%ad\" --date=unix 2>/dev/null || git log --oneline -20 --format=\"%H|%s|%an|%ad\" --date=unix";
        }

        disposables.add(
            sftpManager.executeCommand(logCommand)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::parseCommits, this::handleError)
        );
    }

    private void parseCommits(String output) {
        List<GitCommit> commitList = new ArrayList<>();
        String[] lines = output.split("\n");

        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

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

        commits.setValue(commitList);
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
        String msg = error.getMessage();
        if (msg != null && msg.contains("不是 Git 仓库")) {
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

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
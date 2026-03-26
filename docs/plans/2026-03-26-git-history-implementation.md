# Git 记录展示功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在 Tab3 实现远程 SSH 目录的 Git 记录展示，展示分支列表和 Commit 历史。

**Architecture:** 在现有 SFTPConnectionManager 上扩展命令执行能力，GitHistoryFragment 复用文件浏览器的 SSH 连接状态，跟随文件浏览器当前目录。

**Tech Stack:** Android (Kotlin/Java), RxJava3, SSHJ, ViewModel, LiveData

---

## Task 1: 扩展 SFTPConnectionManager 添加命令执行方法

**Files:**
- Modify: `app/src/main/java/com/termux/app/sftp/SFTPConnectionManager.java:636`

**Step 1: 添加 executeCommand 方法**

在 `shutdown()` 方法之前添加：

```java
/**
 * 执行远程命令
 * @param command 要执行的命令
 * @return Single<String> 命令输出结果
 */
public Single<String> executeCommand(String command) {
    return Single.<String>create(emitter -> {
        try {
            if (!isConnected) {
                emitter.onError(new RuntimeException("未建立SSH连接"));
                return;
            }
            Session session = sshClient.startSession();
            try {
                Session.Command cmd = session.exec(command);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = cmd.getInputStream().read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                cmd.join(30, TimeUnit.SECONDS);
                int exitStatus = cmd.getExitStatus();
                if (exitStatus != 0) {
                    ByteArrayOutputStream errorBaos = new ByteArrayOutputStream();
                    while ((n = cmd.getErrorStream().read(buf)) != -1) {
                        errorBaos.write(buf, 0, n);
                    }
                    String errorMsg = errorBaos.toString(StandardCharsets.UTF_8.name());
                    emitter.onError(new RuntimeException("命令执行失败: " + errorMsg));
                    return;
                }
                emitter.onSuccess(baos.toString(StandardCharsets.UTF_8.name()));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            emitter.onError(e);
        }
    }).subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread());
}
```

**Step 2: 添加 Session import**

确认文件顶部 import 区有：
```java
import net.schmizz.sshj.connection.channel.Session;
```

**Step 3: 验证编译**

Run: `./gradlew app:compileDebugJavaWithJavac --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL 或无错误

**Step 4: Commit**

```bash
git add app/src/main/java/com/termux/app/sftp/SFTPConnectionManager.java
git commit -m "feat: SFTPConnectionManager添加executeCommand方法支持远程命令执行"
```

---

## Task 2: 创建 GitBranch 数据模型

**Files:**
- Create: `app/src/main/java/com/termux/app/models/GitBranch.java`

**Step 1: Write the model class**

```java
package com.termux.app.models;

/**
 * Git 分支数据模型
 */
public class GitBranch {
    private String name;
    private boolean isCurrent;

    public GitBranch() {}

    public GitBranch(String name, boolean isCurrent) {
        this.name = name;
        this.isCurrent = isCurrent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/models/GitBranch.java
git commit -m "feat: 添加GitBranch数据模型"
```

---

## Task 3: 创建 GitCommit 数据模型

**Files:**
- Create: `app/src/main/java/com/termux/app/models/GitCommit.java`

**Step 1: Write the model class**

```java
package com.termux.app.models;

/**
 * Git Commit 数据模型
 */
public class GitCommit {
    private String hash;       // 7位hash
    private String fullHash;  // 完整40位hash
    private String message;   // 提交消息
    private String author;     // 作者
    private long timestamp;   // 时间戳(秒)

    public GitCommit() {}

    public GitCommit(String hash, String fullHash, String message, String author, long timestamp) {
        this.hash = hash;
        this.fullHash = fullHash;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFullHash() {
        return fullHash;
    }

    public void setFullHash(String fullHash) {
        this.fullHash = fullHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/models/GitCommit.java
git commit -m "feat: 添加GitCommit数据模型"
```

---

## Task 4: 创建 GitHistoryViewModel

**Files:**
- Create: `app/src/main/java/com/termux/app/viewmodels/GitHistoryViewModel.java`

**Step 1: Write the ViewModel**

```java
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
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/viewmodels/GitHistoryViewModel.java
git commit -m "feat: 添加GitHistoryViewModel管理Git记录UI状态"
```

---

## Task 5: 创建 GitHistoryFragment

**Files:**
- Create: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Create: `app/src/main/res/layout/fragment_git_history.xml`

**Step 1: Write the layout XML**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@android:color/white">

    <!-- 连接状态栏 -->
    <LinearLayout
        android:id="@+id/status_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical"
        android:background="#F5F5F5">

        <ProgressBar
            android:id="@+id/progress_bar"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:visibility="gone" />

        <TextView
            android:id="@+id/status_text"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:text="未连接 SSH"
            android:textSize="14sp"
            android:textColor="#666666" />

        <Button
            android:id="@+id/retry_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="重试"
            android:visibility="gone" />
    </LinearLayout>

    <!-- 分支信息区 -->
    <LinearLayout
        android:id="@+id/branch_section"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp"
        android:visibility="gone">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="分支"
            android:textSize="12sp"
            android:textColor="#999999" />

        <TextView
            android:id="@+id/current_branch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="main"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#333333" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="所有分支"
            android:textSize="12sp"
            android:textColor="#999999" />

        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:scrollbars="none">

            <LinearLayout
                android:id="@+id/branch_list_container"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal" />
        </HorizontalScrollView>
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="#E0E0E0" />

    <!-- Commit 历史 -->
    <TextView
        android:id="@+id/commits_header"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="12dp"
        android:text="提交历史"
        android:textSize="12sp"
        android:textColor="#999999"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/commits_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:visibility="gone" />

</LinearLayout>
```

**Step 2: Write the Fragment class**

```java
package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.app.R;
import com.termux.app.models.GitBranch;
import com.termux.app.models.GitCommit;
import com.termux.app.viewmodels.GitHistoryViewModel;
import com.termux.filebrowser.RemoteFileBrowserFragment;

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
        viewModel.getCurrentPath().observe(getViewLifecycleOwner(), path -> {
            // 路径变化时自动刷新
        });
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
                branchSection.setVisibility(View.VISIBLE);
                commitsRecyclerView.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                retryButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateBranches(List<GitBranch> branches) {
        branchListContainer.removeAllViews();

        String currentBranchName = "";
        for (GitBranch branch : branches) {
            if (branch.isCurrent()) {
                currentBranchName = branch.getName();
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
            tag.setMargin(4, 0, 4, 0);
            branchListContainer.addView(tag);
        }

        if (!currentBranchName.isEmpty()) {
            currentBranch.setText(currentBranchName);
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
    public void syncPathFromFileBrowser(String path) {
        if (viewModel.isConnected() && path != null && !path.isEmpty()) {
            viewModel.loadGitHistory(path);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当 Fragment 可见时，检查连接状态并尝试加载
        if (viewModel.isConnected()) {
            String path = viewModel.getCurrentPath().getValue();
            if (path == null || path.isEmpty()) {
                // 从 RemoteFileBrowserFragment 获取当前路径
                Fragment fileBrowser = getParentFragmentManager().findFragmentById(R.id.view_pager);
                // 尝试同步
            }
        }
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java
git add app/src/main/res/layout/fragment_git_history.xml
git commit -m "feat: 添加GitHistoryFragment展示Git记录"
```

---

## Task 6: 创建 GitCommitAdapter

**Files:**
- Create: `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java`
- Create: `app/src/main/res/layout/item_git_commit.xml`

**Step 1: Write the item layout XML**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/commit_hash"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="abc1234"
            android:textSize="12sp"
            android:textColor="#0366D6"
            android:fontFamily="monospace" />

        <TextView
            android:id="@+id/commit_message"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:text="提交消息"
            android:textSize="14sp"
            android:textColor="#333333"
            android:maxLines="1"
            android:ellipsize="end" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/commit_author"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="author"
            android:textSize="12sp"
            android:textColor="#666666" />

        <TextView
            android:id="@+id/commit_time"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:text="2 hours ago"
            android:textSize="12sp"
            android:textColor="#999999"
            android:gravity="end" />
    </LinearLayout>

</LinearLayout>
```

**Step 2: Write the Adapter**

```java
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
import java.util.concurrent.TimeUnit;

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
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java
git add app/src/main/res/layout/item_git_commit.xml
git commit -m "feat: 添加GitCommitAdapter用于Commit列表展示"
```

---

## Task 7: 更新 MainTabActivity 使用 GitHistoryFragment

**Files:**
- Modify: `app/src/main/java/com/termux/app/MainTabActivity.kt:275`

**Step 1: 添加 import**

在文件顶部添加：
```kotlin
import com.termux.app.fragments.GitHistoryFragment
```

**Step 2: 修改 TabPagerAdapter**

在 `createFragment` 方法中，将 position == 2 的分支从 `PlaceholderFragment()` 改为 `GitHistoryFragment()`。

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/MainTabActivity.kt
git commit -m "feat: MainTabActivity集成GitHistoryFragment替代占位页"
```

---

## Task 8: 实现目录同步机制

**Files:**
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/java/com/termux/filebrowser/RemoteFileBrowserFragment.java`

**Step 1: 在 GitHistoryFragment 添加同步方法**

在 GitHistoryFragment 中添加一个公共方法来接受从文件浏览器传来的路径：

```java
/**
 * 从文件浏览器同步当前路径
 */
public void onDirectoryChanged(String newPath) {
    if (viewModel != null && viewModel.isConnected()) {
        viewModel.loadGitHistory(newPath);
    }
}
```

**Step 2: 在 RemoteFileBrowserFragment 的 onDirectoryChanged 回调中调用 GitHistoryFragment**

这部分需要查看 RemoteFileBrowserFragment 的实现来确定如何集成。略过具体代码，实现思路是在目录切换时通知 GitHistoryFragment。

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java
git commit -m "feat: GitHistoryFragment添加目录同步方法"
```

---

## Task 9: 验证构建

**Step 1: 执行完整编译**

Run: `./gradlew app:assembleDebug --no-daemon 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL，APK 生成

**Step 2: 如有编译错误，逐个修复并 commit**

---

## 总结

实现完成后的文件变更：

| 文件 | 操作 |
|------|------|
| `SFTPConnectionManager.java` | 修改 |
| `GitBranch.java` | 创建 |
| `GitCommit.java` | 创建 |
| `GitHistoryViewModel.java` | 创建 |
| `GitHistoryFragment.java` | 创建 |
| `fragment_git_history.xml` | 创建 |
| `GitCommitAdapter.java` | 创建 |
| `item_git_commit.xml` | 创建 |
| `MainTabActivity.kt` | 修改 |

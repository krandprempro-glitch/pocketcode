# Git Diff Viewer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade GitFileDetailActivity to show GitLab-style colored diff (+/- lines, line numbers, hunk headers), and show filename-only in the commit accordion file list.

**Architecture:** Parse raw `git diff` output into structured `DiffLine` objects, render them in a RecyclerView with per-line coloring. The file list in the commit accordion is simplified to show only the filename instead of full path.

**Tech Stack:** Android RecyclerView, DiffUtil, custom view holder with background color spans, RxJava3 for async git command execution.

**Design doc:** `docs/plans/2026-03-29-git-diff-viewer-design.md`

---

### Task 1: Add diff color resources

**Files:**
- Modify: `app/src/main/res/values/colors.xml` (after line 126)

**Step 1: Add diff-specific colors to colors.xml**

Add after the `git_status_renamed` color (line 126):

```xml
    <!-- Diff viewer colors -->
    <color name="diff_add_background">#1B2E1B</color>
    <color name="diff_add_text">#85E89D</color>
    <color name="diff_add_line_number">#3FB950</color>
    <color name="diff_remove_background">#2E1B1B</color>
    <color name="diff_remove_text">#F85149</color>
    <color name="diff_remove_line_number">#F85149</color>
    <color name="diff_hunk_background">#1B2433</color>
    <color name="diff_hunk_text">#79C0FF</color>
    <color name="diff_context_background">@android:color/transparent</color>
    <color name="diff_context_text">#C9D1D9</color>
    <color name="diff_line_number">#6E7681</color>
    <color name="diff_file_header_background">#1C2128</color>
    <color name="diff_file_header_text">#8B949E</color>
    <color name="diff_no_newline_text">#6E7681</color>
```

Note: Colors are tuned for the existing dark theme (dark background `#121212`).

**Step 2: Commit**

```bash
git add app/src/main/res/values/colors.xml
git commit -m "feat(diff): add diff viewer color resources for dark theme"
```

---

### Task 2: Create DiffLine model

**Files:**
- Create: `app/src/main/java/com/termux/app/models/DiffLine.java`

**Step 1: Create DiffLine.java**

```java
package com.termux.app.models;

public class DiffLine {

    public enum Type {
        ADD,        // line starting with '+'
        REMOVE,     // line starting with '-'
        CONTEXT,    // unchanged line
        HUNK_HEADER,// @@ -x,y +a,b @@
        FILE_HEADER,// diff --git a/... b/...
        NO_NEWLINE  // \ No newline at end of file
    }

    private final Type type;
    private final int oldLineNumber;  // -1 if N/A
    private final int newLineNumber;  // -1 if N/A
    private final String content;     // text content without +/- prefix

    public DiffLine(Type type, int oldLineNumber, int newLineNumber, String content) {
        this.type = type;
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
        this.content = content;
    }

    public Type getType() { return type; }
    public int getOldLineNumber() { return oldLineNumber; }
    public int getNewLineNumber() { return newLineNumber; }
    public String getContent() { return content; }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/models/DiffLine.java
git commit -m "feat(diff): add DiffLine model for structured diff data"
```

---

### Task 3: Create DiffParser utility

**Files:**
- Create: `app/src/main/java/com/termux/app/utils/DiffParser.java`

**Step 1: Create DiffParser.java**

```java
package com.termux.app.utils;

import com.termux.app.models.DiffLine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw git diff output into structured DiffLine objects.
 * Input format: output of "git show --format="" <hash> -- <path>"
 */
public class DiffParser {

    private static final Pattern HUNK_PATTERN = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

    public static List<DiffLine> parse(String rawDiff) {
        List<DiffLine> lines = new ArrayList<>();
        if (rawDiff == null || rawDiff.isEmpty()) return lines;

        String[] rawLines = rawDiff.split("\n");
        int oldLine = 0;
        int newLine = 0;

        for (String raw : rawLines) {
            if (raw.startsWith("diff --git ")) {
                lines.add(new DiffLine(DiffLine.Type.FILE_HEADER, -1, -1, raw));
            } else if (raw.startsWith("@@")) {
                Matcher m = HUNK_PATTERN.matcher(raw);
                if (m.find()) {
                    oldLine = Integer.parseInt(m.group(1));
                    newLine = Integer.parseInt(m.group(2));
                }
                lines.add(new DiffLine(DiffLine.Type.HUNK_HEADER, -1, -1, raw));
            } else if (raw.startsWith("---") || raw.startsWith("+++")) {
                // Skip file path header lines
            } else if (raw.startsWith("index ") || raw.startsWith("new file") || raw.startsWith("deleted file") || raw.startsWith("Binary files")) {
                // Skip metadata lines
            } else if (raw.startsWith("+")) {
                lines.add(new DiffLine(DiffLine.Type.ADD, -1, newLine, raw.substring(1)));
                newLine++;
            } else if (raw.startsWith("-")) {
                lines.add(new DiffLine(DiffLine.Type.REMOVE, oldLine, -1, raw.substring(1)));
                oldLine++;
            } else if (raw.startsWith("\\ No newline")) {
                lines.add(new DiffLine(DiffLine.Type.NO_NEWLINE, -1, -1, raw));
            } else {
                // Context line — may have a leading space
                String content = raw.startsWith(" ") ? raw.substring(1) : raw;
                lines.add(new DiffLine(DiffLine.Type.CONTEXT, oldLine, newLine, content));
                oldLine++;
                newLine++;
            }
        }
        return lines;
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/utils/DiffParser.java
git commit -m "feat(diff): add DiffParser to parse raw git diff into structured lines"
```

---

### Task 4: Create diff line item layout

**Files:**
- Create: `app/src/main/res/layout/item_diff_line.xml`

**Step 1: Create item_diff_line.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:minHeight="22dp"
    android:gravity="center_vertical">

    <TextView
        android:id="@+id/old_line_number"
        android:layout_width="44dp"
        android:layout_height="wrap_content"
        android:gravity="end"
        android:paddingEnd="8dp"
        android:textSize="10sp"
        android:fontFamily="monospace"
        android:textColor="@color/diff_line_number" />

    <TextView
        android:id="@+id/new_line_number"
        android:layout_width="44dp"
        android:layout_height="wrap_content"
        android:gravity="end"
        android:paddingEnd="8dp"
        android:textSize="10sp"
        android:fontFamily="monospace"
        android:textColor="@color/diff_line_number" />

    <TextView
        android:id="@+id/diff_prefix"
        android:layout_width="12dp"
        android:layout_height="wrap_content"
        android:gravity="start"
        android:textSize="11sp"
        android:fontFamily="monospace" />

    <TextView
        android:id="@+id/diff_content"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="11sp"
        android:fontFamily="monospace"
        android:textIsSelectable="true"
        android:lineSpacingExtra="2dp" />

</LinearLayout>
```

**Step 2: Commit**

```bash
git add app/src/main/res/layout/item_diff_line.xml
git commit -m "feat(diff): add item_diff_line layout for RecyclerView rows"
```

---

### Task 5: Create DiffLineAdapter

**Files:**
- Create: `app/src/main/java/com/termux/app/adapters/DiffLineAdapter.java`

**Step 1: Create DiffLineAdapter.java**

```java
package com.termux.app.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.DiffLine;

import java.util.ArrayList;
import java.util.List;

public class DiffLineAdapter extends RecyclerView.Adapter<DiffLineAdapter.DiffViewHolder> {

    private final List<DiffLine> lines = new ArrayList<>();

    public void submitList(List<DiffLine> newLines) {
        lines.clear();
        if (newLines != null) {
            lines.addAll(newLines);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diff_line, parent, false);
        return new DiffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiffViewHolder holder, int position) {
        DiffLine line = lines.get(position);

        // Line numbers
        holder.oldLineNum.setText(line.getOldLineNumber() > 0 ? String.valueOf(line.getOldLineNumber()) : "");
        holder.newLineNum.setText(line.getNewLineNumber() > 0 ? String.valueOf(line.getNewLineNumber()) : "");

        // Content
        holder.diffContent.setText(line.getContent());

        // Style based on type
        int bgColor, textColor, prefixColor;
        String prefix;

        switch (line.getType()) {
            case ADD:
                bgColor = R.color.diff_add_background;
                textColor = R.color.diff_add_text;
                prefixColor = R.color.diff_add_line_number;
                prefix = "+";
                holder.oldLineNum.setText("");
                holder.newLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_add_line_number));
                break;
            case REMOVE:
                bgColor = R.color.diff_remove_background;
                textColor = R.color.diff_remove_text;
                prefixColor = R.color.diff_remove_line_number;
                prefix = "-";
                holder.newLineNum.setText("");
                holder.oldLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_remove_line_number));
                break;
            case HUNK_HEADER:
                bgColor = R.color.diff_hunk_background;
                textColor = R.color.diff_hunk_text;
                prefixColor = R.color.diff_hunk_text;
                prefix = "@@";
                holder.diffContent.setTypeface(null, Typeface.BOLD);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            case FILE_HEADER:
                bgColor = R.color.diff_file_header_background;
                textColor = R.color.diff_file_header_text;
                prefixColor = R.color.diff_file_header_text;
                prefix = "";
                holder.diffContent.setTypeface(null, Typeface.BOLD);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            case NO_NEWLINE:
                bgColor = android.R.color.transparent;
                textColor = R.color.diff_no_newline_text;
                prefixColor = R.color.diff_no_newline_text;
                prefix = "";
                holder.diffContent.setTypeface(null, Typeface.ITALIC);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            default: // CONTEXT
                bgColor = R.color.diff_context_background;
                textColor = R.color.diff_context_text;
                prefixColor = R.color.diff_line_number;
                prefix = " ";
                holder.oldLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_line_number));
                holder.newLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_line_number));
                break;
        }

        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), bgColor));
        holder.diffContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColor));
        holder.diffPrefix.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), prefixColor));
        holder.diffPrefix.setText(prefix);

        // Reset typeface for non-header lines
        if (line.getType() != DiffLine.Type.HUNK_HEADER
                && line.getType() != DiffLine.Type.FILE_HEADER
                && line.getType() != DiffLine.Type.NO_NEWLINE) {
            holder.diffContent.setTypeface(Typeface.MONOSPACE);
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class DiffViewHolder extends RecyclerView.ViewHolder {
        TextView oldLineNum;
        TextView newLineNum;
        TextView diffPrefix;
        TextView diffContent;

        DiffViewHolder(@NonNull View itemView) {
            super(itemView);
            oldLineNum = itemView.findViewById(R.id.old_line_number);
            newLineNum = itemView.findViewById(R.id.new_line_number);
            diffPrefix = itemView.findViewById(R.id.diff_prefix);
            diffContent = itemView.findViewById(R.id.diff_content);
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/adapters/DiffLineAdapter.java
git commit -m "feat(diff): add DiffLineAdapter with colored +/- line rendering"
```

---

### Task 6: Update activity_git_file_detail.xml layout

**Files:**
- Modify: `app/src/main/res/layout/activity_git_file_detail.xml`

**Step 1: Replace ScrollView+TextView with RecyclerView for diff**

In `activity_git_file_detail.xml`, replace the diff `ScrollView` block (lines 36-52) with a RecyclerView:

Replace:
```xml
        <!-- Diff Content -->
        <ScrollView
            android:id="@+id/diff_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="visible">

            <TextView
                android:id="@+id/diff_content"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:padding="12dp"
                android:textColor="@color/white"
                android:fontFamily="monospace"
                android:textIsSelectable="true"
                android:textSize="13sp"
                android:lineSpacingExtra="4dp" />
        </ScrollView>
```

With:
```xml
        <!-- Diff Content - RecyclerView for colored diff lines -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/diff_recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="visible"
            android:background="@color/dark_background"
            android:clipToPadding="false"
            android:paddingBottom="48dp" />
```

**Step 2: Commit**

```bash
git add app/src/main/res/layout/activity_git_file_detail.xml
git commit -m "feat(diff): replace diff TextView with RecyclerView in layout"
```

---

### Task 7: Update GitFileDetailActivity to use DiffParser + RecyclerView

**Files:**
- Modify: `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java`

**Step 1: Rewrite GitFileDetailActivity.java**

Replace the full content with:

```java
package com.termux.app.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.termux.R;
import com.termux.app.adapters.DiffLineAdapter;
import com.termux.app.models.DiffLine;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.app.utils.DiffParser;
import com.termux.shared.logger.Logger;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Git文件详情页面
 * 显示指定提交中某个文件的Diff（GitLab风格）或File内容
 */
public class GitFileDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMMIT_HASH = "extra_commit_hash";
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_WORKDIR = "extra_workdir";

    private String commitHash;
    private String filePath;
    private String workDir;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView diffRecyclerView;
    private View fileContainer;
    private TextView fileContent;
    private ProgressBar progressBar;
    private TextView errorView;

    private DiffLineAdapter diffAdapter;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isFileLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_git_file_detail);

        commitHash = getIntent().getStringExtra(EXTRA_COMMIT_HASH);
        filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        workDir = getIntent().getStringExtra(EXTRA_WORKDIR);

        if (commitHash == null || filePath == null || workDir == null) {
            Toast.makeText(this, "Missing parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        loadDiff();
    }

    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        diffRecyclerView = findViewById(R.id.diff_recycler_view);
        fileContainer = findViewById(R.id.file_container);
        fileContent = findViewById(R.id.file_content);
        progressBar = findViewById(R.id.progress_bar);
        errorView = findViewById(R.id.error_view);

        // Setup toolbar with back navigation — show filename as title
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Show only filename in toolbar title
            String fileName = filePath;
            int lastSlash = filePath.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
                fileName = filePath.substring(lastSlash + 1);
            }
            getSupportActionBar().setTitle(fileName);
        }

        // Setup diff RecyclerView
        diffAdapter = new DiffLineAdapter();
        diffRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        diffRecyclerView.setAdapter(diffAdapter);

        // Setup TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Diff"));
        tabLayout.addTab(tabLayout.newTab().setText("File"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showDiff();
                } else {
                    showFile();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadDiff() {
        showLoading();
        // Use git show with --format="" to get diff output only
        String cmd = "git -C \"" + workDir + "\" show --format=\"\" " + commitHash + " -- \"" + filePath + "\" 2>&1";
        Logger.logDebug("GitFileDetailActivity", "Loading diff with command: " + cmd);

        disposables.add(
            SFTPConnectionManager.getInstance().executeCommand(cmd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(output -> {
                    hideLoading();
                    List<DiffLine> diffLines = DiffParser.parse(output);
                    diffAdapter.submitList(diffLines);
                    Logger.logDebug("GitFileDetailActivity", "Diff parsed into " + diffLines.size() + " lines");
                }, error -> {
                    hideLoading();
                    showError(error.getMessage() != null ? error.getMessage() : "Failed to load diff");
                    Logger.logDebug("GitFileDetailActivity", "Error loading diff: " + error);
                })
        );
    }

    private void loadFileSnapshot() {
        if (isFileLoaded) return;
        showLoading();
        String cmd = "git -C \"" + workDir + "\" show " + commitHash + ":\"" + filePath + "\" 2>&1";
        Logger.logDebug("GitFileDetailActivity", "Loading file snapshot with command: " + cmd);

        disposables.add(
            SFTPConnectionManager.getInstance().executeCommand(cmd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(output -> {
                    hideLoading();
                    fileContent.setText(output);
                    isFileLoaded = true;
                    Logger.logDebug("GitFileDetailActivity", "File snapshot loaded, output length: " + output.length());
                }, error -> {
                    hideLoading();
                    showError(error.getMessage() != null ? error.getMessage() : "Failed to load file");
                    Logger.logDebug("GitFileDetailActivity", "Error loading file: " + error);
                })
        );
    }

    private void showDiff() {
        diffRecyclerView.setVisibility(View.VISIBLE);
        fileContainer.setVisibility(View.GONE);
    }

    private void showFile() {
        diffRecyclerView.setVisibility(View.GONE);
        fileContainer.setVisibility(View.VISIBLE);
        if (!isFileLoaded) {
            loadFileSnapshot();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java
git commit -m "feat(diff): upgrade GitFileDetailActivity with GitLab-style colored diff"
```

---

### Task 8: Show filename-only in commit accordion file list

**Files:**
- Modify: `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java` (line ~229 area in bind method)

**Step 1: Update file path display in GitCommitAdapter.bind()**

In `GitCommitAdapter.java`, inside the `bind()` method where it sets `pathView.setText(file.getPath())`, change it to extract and show only the filename:

Find (around line 229):
```java
                        pathView.setText(file.getPath());
```

Replace with:
```java
                        // Show only filename instead of full path
                        String fullPath = file.getPath();
                        int slash = fullPath.lastIndexOf('/');
                        String displayName = (slash >= 0 && slash < fullPath.length() - 1)
                                ? fullPath.substring(slash + 1) : fullPath;
                        pathView.setText(displayName);
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java
git commit -m "feat(diff): show filename only in commit accordion file list"
```

---

### Task 9: Build and verify

**Step 1: Clean build**

Run: `./gradlew app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Commit any fixups if needed**

If build fails, fix issues and commit.

---

### Summary of all new files:
1. `app/src/main/java/com/termux/app/models/DiffLine.java` — Diff line data model
2. `app/src/main/java/com/termux/app/utils/DiffParser.java` — Raw diff output parser
3. `app/src/main/java/com/termux/app/adapters/DiffLineAdapter.java` — RecyclerView adapter for diff lines
4. `app/src/main/res/layout/item_diff_line.xml` — Layout for a single diff line

### Modified files:
1. `app/src/main/res/values/colors.xml` — Add diff color resources
2. `app/src/main/res/layout/activity_git_file_detail.xml` — Replace TextView with RecyclerView
3. `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java` — Use DiffParser + RecyclerView
4. `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java` — Filename-only display

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

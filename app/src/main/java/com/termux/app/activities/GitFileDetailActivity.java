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

import com.google.android.material.tabs.TabLayout;
import com.termux.R;
import com.termux.app.sftp.SFTPConnectionManager;
import com.termux.shared.logger.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Git文件详情页面
 * 显示指定提交中某个文件的Diff或File内容
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
    private View diffContainer;
    private View fileContainer;
    private TextView diffContent;
    private TextView fileContent;
    private ProgressBar progressBar;
    private TextView errorView;

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
        loadDiff(); // Default to diff
    }

    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        diffContainer = findViewById(R.id.diff_container);
        fileContainer = findViewById(R.id.file_container);
        diffContent = findViewById(R.id.diff_content);
        fileContent = findViewById(R.id.file_content);
        progressBar = findViewById(R.id.progress_bar);
        errorView = findViewById(R.id.error_view);

        // Setup toolbar with back navigation
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(filePath);
        }

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
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadDiff() {
        showLoading();
        String cmd = "git -C \"" + workDir + "\" show " + commitHash + " -- \"" + filePath + "\" 2>&1";
        Logger.logDebug("GitFileDetailActivity", "Loading diff with command: " + cmd);

        disposables.add(
            SFTPConnectionManager.getInstance().executeCommand(cmd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(output -> {
                    hideLoading();
                    diffContent.setText(output);
                    Logger.logDebug("GitFileDetailActivity", "Diff loaded, output length: " + output.length());
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
        // Use git show to get file content at specific commit: git show <hash>:<path>
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
        diffContainer.setVisibility(View.VISIBLE);
        fileContainer.setVisibility(View.GONE);
    }

    private void showFile() {
        diffContainer.setVisibility(View.GONE);
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
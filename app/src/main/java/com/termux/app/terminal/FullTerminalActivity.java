package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import com.termux.R;
import com.termux.app.TermuxInstaller;
import com.termux.app.TermuxService;
import com.termux.app.terminal.ClaudeCodeMenuHelper;
import com.termux.app.terminal.CommandGroupAdapter;
import com.termux.app.configuration.managers.QuickCommandManager;
import com.termux.app.configuration.models.QuickCommand;
import com.termux.app.managers.ClaudeCodeCommandManager;
import com.termux.app.managers.ProjectWorkspaceManager;
import com.termux.app.models.DirectoryBookmark;
import com.termux.app.models.SSHConfigManager;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.ssh.SSHConnectionManager;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.extrakeys.ExtraKeyButton;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.extrakeys.SpecialButton;
import com.termux.shared.termux.extrakeys.SpecialButtonState;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FullTerminalActivity extends AppCompatActivity implements ServiceConnection {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_SESSION_HANDLE = "session_handle";
    public static final String EXTRA_SESSION_NAME = "session_name";
    public static final String EXTRA_SSH_CONFIG = "ssh_config";
    public static final String EXTRA_INITIAL_PATH = "initial_path";
    public static final String EXTRA_INITIAL_COMMAND = "initial_command";
    private static final String TAG = "FullTerminal";

    // Service
    private TermuxService mTermuxService;
    private boolean mServiceBound = false;

    // Views
    private TermuxActivityRootView mTermuxActivityRootView;
    private TerminalView mTerminalView;
    private EditText mTerminalCommandInput;
    private ImageButton mTerminalSendButton;
    private ImageButton mClaudeCodeMenuButton;
    private ViewPager mTerminalToolbarViewPager;
    private ExtraKeysView mExtraKeysView;

    // Session
    private String mSessionId;
    private String mSessionHandle;
    private String mSessionName;
    private String mSshConfigName;
    private String mInitialPath;
    private String mInitialCommand;
    private TerminalSession mTerminalSession;
    private boolean mAutoCommandsSent = false;
    private CommandGroupAdapter mCommandAdapter;
    private CompositeDisposable mCommandMenuDisposables = new CompositeDisposable();

    // Renderer initialized flag
    private boolean mRendererInitialized = false;
    private int mAttachRetryCount = 0;
    private static final int MAX_ATTACH_RETRIES = 20;

    // Extra keys toolbar
    private float mTerminalToolbarDefaultHeight;

    // Context menu IDs
    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_STYLING_ID = 5;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    private static final int CONTEXT_MENU_HELP_ID = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;
    private static final int CONTEXT_MENU_REPORT_ID = 9;

    // Toast utility
    private Toast mLastToast;

    // Full TerminalViewClient implementation
    private final TerminalViewClient mTerminalViewClient = new TerminalViewClient() {
        @Override
        public float onScale(float scale) {
            return 1.0f;
        }

        @Override
        public void onSingleTapUp(MotionEvent e) {
            if (mTerminalView == null) return;
            TerminalSession session = mTerminalView.getCurrentSession();
            if (session == null) return;
            TerminalEmulator term = session.getEmulator();
            if (term == null) return;

            if (!term.isMouseTrackingActive() && !e.isFromSource(InputDevice.SOURCE_MOUSE)) {
                mTerminalView.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    if (!imm.showSoftInput(mTerminalView, InputMethodManager.SHOW_IMPLICIT)) {
                        mTerminalView.post(() -> imm.showSoftInput(mTerminalView, InputMethodManager.SHOW_FORCED));
                    }
                }
            }
        }

        @Override
        public boolean shouldBackButtonBeMappedToEscape() {
            return true;
        }

        @Override
        public boolean shouldEnforceCharBasedInput() {
            return false;
        }

        @Override
        public boolean shouldUseCtrlSpaceWorkaround() {
            return false;
        }

        @Override
        public boolean isTerminalViewSelected() {
            return true;
        }

        @Override
        public void copyModeChanged(boolean copyMode) {}

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent e) {
            return false;
        }

        @Override
        public boolean onLongPress(MotionEvent event) {
            return false;
        }

        @Override
        public boolean readControlKey() {
            return false;
        }

        @Override
        public boolean readAltKey() {
            return false;
        }

        @Override
        public boolean readShiftKey() {
            return false;
        }

        @Override
        public boolean readFnKey() {
            return false;
        }

        @Override
        public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
            return false;
        }

        @Override
        public void onEmulatorSet() {}

        @Override
        public void logError(String tag, String message) {
            Log.e(tag, message);
        }

        @Override
        public void logWarn(String tag, String message) {
            Log.w(tag, message);
        }

        @Override
        public void logInfo(String tag, String message) {
            Log.i(tag, message);
        }

        @Override
        public void logDebug(String tag, String message) {
            Log.d(tag, message);
        }

        @Override
        public void logVerbose(String tag, String message) {
            Log.v(tag, message);
        }

        @Override
        public void logStackTraceWithMessage(String tag, String message, Exception e) {
            Log.e(tag, message, e);
        }

        @Override
        public void logStackTrace(String tag, Exception e) {
            Log.e(tag, "", e);
        }
    };

    // Session client callbacks - implements TerminalSessionClient directly
    private final TerminalSessionClient mSessionClient = new TerminalSessionClient() {
        @Override
        public void onTextChanged(@NonNull TerminalSession changedSession) {
            if (mTerminalView != null) {
                mTerminalView.onScreenUpdated();
            }
        }

        @Override
        public void onTitleChanged(@NonNull TerminalSession changedSession) {
            if (changedSession == mTerminalSession) {
                runOnUiThread(() -> {
                    MaterialToolbar toolbar = findViewById(R.id.toolbar);
                    if (toolbar != null) {
                        String title = changedSession.mSessionName;
                        if (title == null || title.isEmpty()) {
                            title = changedSession.getTitle();
                        }
                        if (title != null) {
                            toolbar.setTitle(title);
                        }
                    }
                });
            }
        }

        @Override
        public void onSessionFinished(@NonNull TerminalSession finishedSession) {
            Log.d(TAG, "Session finished: " + finishedSession.mHandle);
        }

        @Override
        public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {}

        @Override
        public void onPasteTextFromClipboard(@Nullable TerminalSession session) {}

        @Override
        public void onBell(@NonNull TerminalSession session) {
            runOnUiThread(() -> {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mTerminalView.postDelayed(() -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON), 1000);
            });
        }

        @Override
        public void onColorsChanged(@NonNull TerminalSession session) {
            if (mTerminalView != null) {
                mTerminalView.onScreenUpdated();
            }
        }

        @Override
        public void onTerminalCursorStateChange(boolean state) {}

        @Override
        public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {}

        @Override
        public Integer getTerminalCursorStyle() { return null; }

        @Override
        public void logError(String tag, String message) { Log.e(tag, message); }

        @Override
        public void logWarn(String tag, String message) { Log.w(tag, message); }

        @Override
        public void logInfo(String tag, String message) { Log.i(tag, message); }

        @Override
        public void logDebug(String tag, String message) { Log.d(tag, message); }

        @Override
        public void logVerbose(String tag, String message) { Log.v(tag, message); }

        @Override
        public void logStackTraceWithMessage(String tag, String message, Exception e) { Log.e(tag, message, e); }

        @Override
        public void logStackTrace(String tag, Exception e) { Log.e(tag, "", e); }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate START ===");
        setContentView(R.layout.activity_full_terminal);

        // Get session info from intent
        mSessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        mSessionHandle = getIntent().getStringExtra(EXTRA_SESSION_HANDLE);
        mSessionName = getIntent().getStringExtra(EXTRA_SESSION_NAME);
        mSshConfigName = getIntent().getStringExtra(EXTRA_SSH_CONFIG);
        mInitialPath = getIntent().getStringExtra(EXTRA_INITIAL_PATH);
        mInitialCommand = getIntent().getStringExtra(EXTRA_INITIAL_COMMAND);
        Log.d(TAG, "Intent extras: id=" + mSessionId + " handle=" + mSessionHandle + " name=" + mSessionName
            + " ssh=" + mSshConfigName + " path=" + mInitialPath + " cmd=" + mInitialCommand);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        setupTerminalView();
        setupTerminalToolbar();
        setupInputView();

        // Register context menu for terminal view (long press)
        registerForContextMenu(mTerminalView);

        // Bind to TermuxService
        Intent intent = new Intent(this, TermuxService.class);
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "=== onCreate END ===");
    }

    private void initViews() {
        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalCommandInput = findViewById(R.id.terminal_command_input);
        mTerminalSendButton = findViewById(R.id.terminal_send_button);
        mClaudeCodeMenuButton = findViewById(R.id.claude_code_menu_button);
        mTerminalToolbarViewPager = findViewById(R.id.terminal_toolbar_view_pager);

        Log.d(TAG, "initViews: terminalView=" + mTerminalView + " rootView=" + mTermuxActivityRootView);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(mSessionName != null ? mSessionName : "终端");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupTerminalView() {
        Log.d(TAG, "setupTerminalView: START");
        mTerminalView.setTerminalViewClient(mTerminalViewClient);
        mTerminalView.setFocusable(true);
        mTerminalView.setFocusableInTouchMode(true);

        // Initialize renderer after view is laid out
        mTerminalView.post(() -> {
            Log.d(TAG, "post: initializing renderer, view dims=" + mTerminalView.getWidth() + "x" + mTerminalView.getHeight());
            initTerminalRenderer();
        });

        Log.d(TAG, "setupTerminalView: END");
    }

    private void initTerminalRenderer() {
        Log.d(TAG, "initTerminalRenderer: START");
        float density = getResources().getDisplayMetrics().scaledDensity;
        final int fontSizePx = (int) (14 * density);
        Log.d(TAG, "Font size: " + fontSizePx + "px (density=" + density + ")");

        try {
            mTerminalView.setTextSize(fontSizePx);
            Log.d(TAG, "setTextSize() called, initTerminalRenderer: SUCCESS");
        } catch (Exception e) {
            Log.e(TAG, "setTextSize FAILED: " + e.getMessage());
        }
        mRendererInitialized = true;
        mTerminalView.requestLayout();
    }

    // ==================== Extra Keys Toolbar ====================

    private void setupTerminalToolbar() {
        if (mTerminalToolbarViewPager == null) {
            Log.e(TAG, "Terminal toolbar ViewPager not found");
            return;
        }

        mTerminalToolbarViewPager.setVisibility(View.VISIBLE);
        mTerminalToolbarDefaultHeight = mTerminalToolbarViewPager.getLayoutParams().height;

        // Adjust height for 2 rows of keys
        ViewGroup.LayoutParams lp = mTerminalToolbarViewPager.getLayoutParams();
        lp.height = Math.round(mTerminalToolbarDefaultHeight * 2 * 1.0f);
        mTerminalToolbarViewPager.setLayoutParams(lp);

        mTerminalToolbarViewPager.setAdapter(new TerminalToolbarPagerAdapter());
        mTerminalToolbarViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    EditText textInput = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                    if (textInput != null) textInput.requestFocus();
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private class TerminalToolbarPagerAdapter extends androidx.viewpager.widget.PagerAdapter {
        @Override
        public int getCount() { return 2; }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(FullTerminalActivity.this);
            View layout;

            if (position == 0) {
                // Page 0: Extra Keys
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                mExtraKeysView = extraKeysView;

                extraKeysView.setButtonTextAllCaps(true);

                try {
                    String basicExtraKeys = "[[\"ESC\",\"TAB\",\"CTRL\",\"C\",\"UP\"],[\"ALT\",\"SHIFT\",\"/\",\"LEFT\",\"DOWN\",\"RIGHT\"]]";
                    ExtraKeysInfo extraKeysInfo = new ExtraKeysInfo(basicExtraKeys, "default", ExtraKeysConstants.CONTROL_CHARS_ALIASES);
                    extraKeysView.reload(extraKeysInfo, mTerminalToolbarDefaultHeight);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create extra keys: " + e.getMessage());
                }

                extraKeysView.setExtraKeysViewClient(new ExtraKeysView.IExtraKeysView() {
                    @Override
                    public void onExtraKeyButtonClick(View view, ExtraKeyButton button, com.google.android.material.button.MaterialButton buttonView) {
                        if (button == null || button.getKey() == null) return;
                        String key = button.getKey();
                        TerminalSession session = mTerminalSession;

                        boolean ctrlActive = false, altActive = false, shiftActive = false;
                        if (mExtraKeysView != null) {
                            try {
                                Boolean cs = mExtraKeysView.readSpecialButton(SpecialButton.CTRL, false);
                                ctrlActive = cs != null && cs;
                                Boolean as = mExtraKeysView.readSpecialButton(SpecialButton.ALT, false);
                                altActive = as != null && as;
                                Boolean ss = mExtraKeysView.readSpecialButton(SpecialButton.SHIFT, false);
                                shiftActive = ss != null && ss;
                            } catch (Exception ignored) {}
                        }

                        if (session == null || !session.isRunning()) return;

                        if (ctrlActive && ("C".equals(key) || "c".equals(key))) {
                            // Ctrl+C: send interrupt
                            session.write(new byte[]{3}, 0, 1);
                            resetModifierStates();
                        } else if (shiftActive && "TAB".equals(key)) {
                            // Shift+Tab: backtab
                            byte[] backtab = "\u001b[Z".getBytes(StandardCharsets.UTF_8);
                            session.write(backtab, 0, backtab.length);
                            resetModifierStates();
                        } else if (ctrlActive && "/".equals(key)) {
                            // Ctrl+/: open command menu
                            openClaudeCodeMenu();
                            resetModifierStates();
                        } else if (ctrlActive || altActive || shiftActive) {
                            handleExtraKeyInput(key, ctrlActive, altActive, shiftActive);
                            resetModifierStates();
                        } else {
                            handleExtraKeyInput(key, false, false, false);
                        }
                    }

                    @Override
                    public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton button, com.google.android.material.button.MaterialButton buttonView) {
                        return false;
                    }
                });

            } else {
                // Page 1: Text Input
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        String text = editText.getText().toString();
                        if (!text.isEmpty() && mTerminalSession != null) {
                            mTerminalSession.write(text);
                            editText.setText("");
                        }
                        return true;
                    }
                    return false;
                });
            }

            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }
    }

    private void handleExtraKeyInput(String key, boolean ctrlDown, boolean altDown, boolean shiftDown) {
        Integer keyCode = getKeyCodeForString(key);

        if (keyCode != null) {
            int metaState = 0;
            if (ctrlDown) metaState |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
            if (altDown) metaState |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
            if (shiftDown) metaState |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;

            KeyEvent keyEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState);
            mTerminalView.onKeyDown(keyCode, keyEvent);
        } else if (mTerminalView != null && key.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                key.codePoints().forEach(cp ->
                    mTerminalView.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, cp, ctrlDown, altDown)
                );
            } else if (mTerminalSession != null) {
                mTerminalSession.write(key);
            }
        }
    }

    private Integer getKeyCodeForString(String key) {
        switch (key) {
            case "TAB": return KeyEvent.KEYCODE_TAB;
            case "UP": return KeyEvent.KEYCODE_DPAD_UP;
            case "DOWN": return KeyEvent.KEYCODE_DPAD_DOWN;
            case "LEFT": return KeyEvent.KEYCODE_DPAD_LEFT;
            case "RIGHT": return KeyEvent.KEYCODE_DPAD_RIGHT;
            case "ESC": return KeyEvent.KEYCODE_ESCAPE;
            case " ": return KeyEvent.KEYCODE_SPACE;
            default: return null;
        }
    }

    private void resetModifierStates() {
        if (mExtraKeysView == null) return;
        try {
            Map<SpecialButton, SpecialButtonState> buttons = mExtraKeysView.getSpecialButtons();
            if (buttons == null) return;
            for (SpecialButtonState state : buttons.values()) {
                state.setIsActive(false);
                state.setIsLocked(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset modifier states: " + e.getMessage());
        }
    }

    // ==================== Input View ====================

    private void setupInputView() {
        // Send button click
        mTerminalSendButton.setOnClickListener(v -> {
            String command = mTerminalCommandInput.getText().toString();
            if (!command.isEmpty()) {
                sendCommandToTerminal(command);
                mTerminalCommandInput.setText("");
            }
        });

        // Handle keyboard action
        mTerminalCommandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String command = mTerminalCommandInput.getText().toString();
                if (!command.isEmpty()) {
                    sendCommandToTerminal(command);
                    mTerminalCommandInput.setText("");
                }
                return true;
            }
            return false;
        });

        // Keyboard shortcuts in command input
        mTerminalCommandInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyboardShortcuts(keyCode, event);
            }
            return false;
        });

        // Claude Code menu button
        if (mClaudeCodeMenuButton != null) {
            mClaudeCodeMenuButton.setOnClickListener(v -> openClaudeCodeMenu());
        }
    }

    private void openClaudeCodeMenu() {
        showCommandMenu();
    }

    private boolean handleKeyboardShortcuts(int keyCode, KeyEvent event) {
        boolean ctrlPressed = event.isCtrlPressed();
        boolean shiftPressed = event.isShiftPressed();

        if (shiftPressed && keyCode == KeyEvent.KEYCODE_TAB) {
            openClaudeCodeMenu();
            return true;
        }

        if (ctrlPressed) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_L:
                    mTerminalCommandInput.setText("/clear");
                    mTerminalCommandInput.setSelection(mTerminalCommandInput.getText().length());
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                    sendCommandToTerminal(mTerminalCommandInput.getText().toString());
                    mTerminalCommandInput.setText("");
                    return true;
                case KeyEvent.KEYCODE_SLASH:
                    openClaudeCodeMenu();
                    return true;
            }
        }
        return false;
    }

    // ==================== Claude Code Command Menu ====================

    private void showCommandMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_claude_commands, null);
        dialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.commands_recycler_view);
        List<CommandGroupAdapter.CommandGroup> groups = prepareCommandGroups();

        mCommandAdapter = new CommandGroupAdapter(
            groups,
            mTerminalCommandInput,
            command -> dialog.dismiss()
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mCommandAdapter);
        dialog.show();

        // Subscribe to custom commands updates
        mCommandMenuDisposables.clear();
        mCommandMenuDisposables.add(
            ClaudeCodeCommandManager.getInstance().customCommandsObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(commands -> {
                    if (mCommandAdapter != null) {
                        List<CommandGroupAdapter.CommandGroup> updatedGroups = prepareCommandGroups();
                        mCommandAdapter.updateGroups(updatedGroups);
                    }
                })
        );

        // Trigger a fetch when menu opens (force refresh to get latest)
        ClaudeCodeCommandManager.getInstance().fetchRemoteCommands(true)
            .subscribe();
    }

    private List<CommandGroupAdapter.CommandGroup> prepareCommandGroups() {
        List<CommandGroupAdapter.CommandGroup> groups = new ArrayList<>();

        // 1. Bookmarks
        List<ClaudeCodeMenuHelper.Command> bookmarkCommands = new ArrayList<>();
        try {
            ProjectWorkspaceManager pwm = ProjectWorkspaceManager.getInstance(this);
            if (pwm != null) {
                for (DirectoryBookmark bm : pwm.getAllBookmarks()) {
                    bookmarkCommands.add(new ClaudeCodeMenuHelper.Command("cd " + bm.getFullPath(), bm.getDisplayName()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bookmarks: " + e.getMessage());
        }
        if (bookmarkCommands.isEmpty()) {
            bookmarkCommands.add(new ClaudeCodeMenuHelper.Command("", "暂无收藏路径"));
        }
        groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.BOOKMARKS, bookmarkCommands));

        // 2. SSH Connections
        List<ClaudeCodeMenuHelper.Command> sshCommands = new ArrayList<>();
        try {
            SSHConfigManager mgr = SSHConfigManager.getInstance(this);
            for (SSHConnectionConfig config : mgr.getAllConfigs()) {
                String cmd = SSHConnectionManager.generateTerminalSSHCommand(config);
                if (cmd != null) {
                    String desc = config.getUsername() + "@" + config.getHost();
                if (config.getPort() != 22) desc += ":" + config.getPort();
                sshCommands.add(new ClaudeCodeMenuHelper.Command(cmd, desc));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load SSH configs: " + e.getMessage());
        }
        if (!sshCommands.isEmpty()) {
            groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.SSH_CONNECTIONS, sshCommands));
        }

        // 3. Quick Commands
        List<ClaudeCodeMenuHelper.Command> quickCommands = new ArrayList<>();
        try {
            QuickCommandManager qcm = QuickCommandManager.getInstance(this);
            for (QuickCommand cmd : qcm.getAllCommands()) {
                String desc = cmd.getDescription();
                if (desc == null || desc.isEmpty()) desc = cmd.getCategory();
                quickCommands.add(new ClaudeCodeMenuHelper.Command(cmd.getCommand(), desc));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load quick commands: " + e.getMessage());
        }
        if (quickCommands.isEmpty()) {
            quickCommands.add(new ClaudeCodeMenuHelper.Command("", "暂无常用指令"));
        }
        groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.QUICK_COMMANDS, quickCommands));

        // 4. AI Commands (built-in)
        List<ClaudeCodeMenuHelper.Command> aiCommands = ClaudeCodeCommandManager.getInstance().getDefaultCommands();
        groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.AI_COMMANDS, aiCommands));

        // 5. AI Custom Commands (remote user-defined)
        List<ClaudeCodeMenuHelper.Command> customCommands = ClaudeCodeCommandManager.getInstance().getCustomCommands();
        if (!customCommands.isEmpty()) {
            groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.AI_CUSTOM_COMMANDS, customCommands));
        }

        // 6. System Commands
        List<ClaudeCodeMenuHelper.Command> systemCommands = Arrays.asList(
            new ClaudeCodeMenuHelper.Command("claude", "启动Claude Code"),
            new ClaudeCodeMenuHelper.Command("claude --resume", "恢复上次会话"),
            new ClaudeCodeMenuHelper.Command("claude -p \"\"", "快速提问模式"),
            new ClaudeCodeMenuHelper.Command("codex", "启动Codex")
        );
        groups.add(new CommandGroupAdapter.CommandGroup(CommandGroupAdapter.CommandCategory.SYSTEM_COMMANDS, systemCommands));

        return groups;
    }

    // ==================== Context Menu (Long Press) ====================

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        TerminalSession session = mTerminalSession;
        if (session == null) return;

        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (autoFillEnabled) {
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
        }
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE,
            getResources().getString(R.string.action_kill_process, session.getPid())).setEnabled(session.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = mTerminalSession;
        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                showToast("URL选择功能暂不可用");
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                showToast("分享功能暂不可用");
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                showToast("文本分享暂不可用");
                return true;
            case CONTEXT_MENU_AUTOFILL_USERNAME:
                if (mTerminalView != null) mTerminalView.requestAutoFillUsername();
                return true;
            case CONTEXT_MENU_AUTOFILL_PASSWORD:
                if (mTerminalView != null) mTerminalView.requestAutoFillPassword();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                if (session != null) {
                    session.reset();
                    showToast(getResources().getString(R.string.msg_terminal_reset));
                }
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                if (session != null) {
                    new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.title_confirm_kill_process)
                        .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                            dialog.dismiss();
                            session.finishIfRunning();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                }
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                showToast("问题报告暂不可用");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME,
            TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(this,
                        new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        }
    }

    // ==================== Service Connection ====================

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "=== onServiceConnected START ===");
        TermuxService.LocalBinder binder = (TermuxService.LocalBinder) service;
        mTermuxService = binder.service;
        mServiceBound = true;
        Log.d(TAG, "Service bound, sessions size: " + (mTermuxService != null ? mTermuxService.getTermuxSessionsSize() : -1));

        attachSession();
        Log.d(TAG, "=== onServiceConnected END ===");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mTermuxService = null;
        mServiceBound = false;
        finish();
    }

    private void attachSession() {
        Log.d(TAG, "attachSession: START rendererInit=" + mRendererInitialized + " handle=" + mSessionHandle);

        if (mSessionHandle != null && mTermuxService != null) {
            TerminalSession session = findSessionByHandle(mSessionHandle);
            if (session != null) {
                Log.d(TAG, "Found existing session: " + session.mHandle);
                mTerminalSession = session;
                doAttachSession(session);
                return;
            } else {
                Log.d(TAG, "Session not found by handle: " + mSessionHandle + ", will create new");
            }
        } else {
            Log.d(TAG, "No handle or no service, creating new session");
        }

        createNewSession();
    }

    private void doAttachSession(TerminalSession session) {
        Log.d(TAG, "doAttachSession: START session=" + session + " handle=" + session.mHandle);

        if (!mRendererInitialized) {
            if (mAttachRetryCount >= MAX_ATTACH_RETRIES) {
                Log.e(TAG, "Renderer still not initialized after " + MAX_ATTACH_RETRIES + " retries, forcing init");
                mRendererInitialized = true;
            } else {
                mAttachRetryCount++;
                Log.d(TAG, "Renderer not ready, waiting... (retry " + mAttachRetryCount + "/" + MAX_ATTACH_RETRIES + ")");
                mTerminalView.postDelayed(() -> doAttachSession(session), 50);
                return;
            }
        }

        mTerminalView.post(() -> {
            try {
                int w = mTerminalView.getWidth();
                int h = mTerminalView.getHeight();
                Log.d(TAG, "doAttachSession view dims: " + w + "x" + h);

                if (h <= 0) {
                    if (mAttachRetryCount >= MAX_ATTACH_RETRIES) {
                        Log.e(TAG, "Dimensions still invalid after " + MAX_ATTACH_RETRIES + " retries, forcing attach");
                    } else {
                        mAttachRetryCount++;
                        mTerminalView.postDelayed(() -> doAttachSession(session), 100);
                        return;
                    }
                }

                // Set session client for callbacks
                session.updateTerminalSessionClient(mSessionClient);

                float density = getResources().getDisplayMetrics().scaledDensity;
                int fontSizePx = (int) (14 * density);
                mTerminalView.setTextSize(fontSizePx);

                boolean attached = mTerminalView.attachSession(session);
                Log.d(TAG, "attachSession returned: " + attached);

                mTerminalView.updateSize();
                mTerminalView.invalidate();
                mTerminalView.onScreenUpdated();

                // Save terminal handle
                if (mSessionId != null && session.mHandle != null) {
                    com.termux.app.sessions.SessionManager.INSTANCE.updateTerminalHandle(mSessionId, session.mHandle);
                }

                Log.d(TAG, "doAttachSession: SUCCESS");

                // Auto-execute SSH and/or cd commands for new sessions
                sendAutoCommands();

            } catch (Exception e) {
                Log.e(TAG, "doAttachSession FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                showToast("附加会话失败: " + e.getMessage());
            }
        });
    }

    private TerminalSession findSessionByHandle(String handle) {
        if (mTermuxService == null) return null;
        try {
            for (TermuxSession termuxSession : mTermuxService.getTermuxSessions()) {
                TerminalSession ts = termuxSession.getTerminalSession();
                if (ts.mHandle.equals(handle)) {
                    return ts;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding session: " + e.getMessage());
        }
        return null;
    }

    private void createNewSession() {
        if (mTermuxService == null) {
            Log.e(TAG, "createNewSession: mTermuxService is null!");
            return;
        }

        // Ensure bootstrap is set up before creating session (same as TermuxActivity)
        TermuxInstaller.setupBootstrapIfNeeded(this, () -> {
            try {
                Log.d(TAG, "createNewSession: calling createTermuxSession...");
                TermuxSession newTermuxSession = mTermuxService.createTermuxSession(
                    null, null, null, null, false, mSessionName
                );

                if (newTermuxSession != null) {
                    mTerminalSession = newTermuxSession.getTerminalSession();
                    Log.d(TAG, "createNewSession: SUCCESS, handle=" + mTerminalSession.mHandle);
                    doAttachSession(mTerminalSession);
                } else {
                    Log.e(TAG, "createNewSession: returned NULL!");
                    showToast("创建会话失败");
                }
            } catch (Exception e) {
                Log.e(TAG, "createNewSession FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                showToast("创建会话失败: " + e.getMessage());
            }
        });
    }

    // ==================== Auto Commands (SSH / cd) ====================

    private void sendAutoCommands() {
        Log.d(TAG, "sendAutoCommands: called, sent=" + mAutoCommandsSent
            + " ssh=" + mSshConfigName + " path=" + mInitialPath + " cmd=" + mInitialCommand);
        if (mAutoCommandsSent) return;

        // Initial command (e.g., pkg install sshpass) — run and stay in terminal
        if (mInitialCommand != null && !mInitialCommand.isEmpty()) {
            Log.d(TAG, "Auto command: " + mInitialCommand);
            sendCommandToTerminal(mInitialCommand);
            mAutoCommandsSent = true;
            return;
        }

        if (mSshConfigName != null && !mSshConfigName.isEmpty()) {
            // SSH connection requested
            try {
                SSHConfigManager mgr = SSHConfigManager.getInstance(this);
                Log.d(TAG, "sendAutoCommands: SSHConfigManager=" + mgr);
                SSHConnectionConfig config = mgr.getConfigByName(mSshConfigName);
                Log.d(TAG, "sendAutoCommands: config for '" + mSshConfigName + "' = " + config);
                Log.d(TAG, "sendAutoCommands: password='" + config.getPassword()
                    + "' privateKey='" + config.getPrivateKeyPath() + "'");
                String sshCmd = SSHConnectionManager.generateTerminalSSHCommand(config);
                Log.d(TAG, "sendAutoCommands: sshCmd=" + sshCmd);
                if (sshCmd != null) {
                    // If path specified, SSH with cd: ssh ... "cd /path; exec bash"
                    // Use ; so bash starts even if cd fails, exec replaces shell for interactive use
                    if (mInitialPath != null && !mInitialPath.isEmpty()) {
                        sshCmd += " \"cd " + mInitialPath + " 2>/dev/null; exec bash\"";
                    }
                    Log.d(TAG, "Auto SSH: " + sshCmd);
                    sendCommandToTerminal(sshCmd);
                    mAutoCommandsSent = true;
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Auto SSH failed: " + e.getMessage());
            }
        }

        // No SSH — local terminal, cd to path if specified
        if (mInitialPath != null && !mInitialPath.isEmpty()) {
            Log.d(TAG, "Auto cd: " + mInitialPath);
            sendCommandToTerminal("cd " + mInitialPath + " && ls");
            mAutoCommandsSent = true;
        }
    }

    // ==================== Command Sending ====================

    private void sendCommandToTerminal(String command) {
        Log.d(TAG, "sendCommandToTerminal: cmd='" + command + "' session=" + mTerminalSession
            + " running=" + (mTerminalSession != null && mTerminalSession.isRunning()));
        if (mTerminalSession == null || !mTerminalSession.isRunning()) {
            showToast("终端会话未运行");
            return;
        }

        if (command == null || command.trim().isEmpty()) {
            byte[] enterBytes = "\r".getBytes(StandardCharsets.UTF_8);
            mTerminalSession.write(enterBytes, 0, enterBytes.length);
        } else {
            byte[] commandBytes = (command.trim() + "\r").getBytes(StandardCharsets.UTF_8);
            mTerminalSession.write(commandBytes, 0, commandBytes.length);
        }
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: session=" + (mTerminalSession != null) + " view=" + (mTerminalView != null));
        if (mTerminalView != null && mTerminalSession != null) {
            mTerminalView.updateSize();
            mTerminalView.invalidate();
            mTerminalView.onScreenUpdated();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCommandMenuDisposables.clear();

        if (mServiceBound) {
            unbindService(this);
            mServiceBound = false;
        }
    }

    // ==================== Toast Utility ====================

    private void showToast(String text) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }
}

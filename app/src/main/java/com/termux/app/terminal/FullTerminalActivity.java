package com.termux.app.terminal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.termux.R;
import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalRenderer;
import com.termux.view.TerminalView;

import java.lang.reflect.Field;

public class FullTerminalActivity extends AppCompatActivity implements ServiceConnection {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_SESSION_HANDLE = "session_handle";
    public static final String EXTRA_SESSION_NAME = "session_name";
    public static final String EXTRA_SSH_CONFIG = "ssh_config";
    public static final String EXTRA_INITIAL_PATH = "initial_path";
    private static final String TAG = "FullTerminal";

    private TermuxService mTermuxService;
    private boolean mServiceBound = false;

    // Views
    private TermuxActivityRootView mTermuxActivityRootView;
    private TerminalView mTerminalView;
    private EditText mTerminalCommandInput;
    private ImageButton mTerminalSendButton;
    private ImageButton mClaudeCodeMenuButton;

    // Session
    private String mSessionId;
    private String mSessionHandle;
    private String mSessionName;
    private TerminalSession mTerminalSession;

    // Renderer initialized flag
    private boolean mRendererInitialized = false;

    // Simple terminal view client
    private final TermuxTerminalViewClientBase mTerminalViewClient = new TermuxTerminalViewClientBase() {
        @Override
        public boolean isTerminalViewSelected() {
            return true;
        }
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
        Log.d(TAG, "Intent extras: id=" + mSessionId + " handle=" + mSessionHandle + " name=" + mSessionName);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initViews();
        setupTerminalView();
        setupInputView();

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
        // Set the terminal view client
        mTerminalView.setTerminalViewClient(mTerminalViewClient);
        Log.d(TAG, "TerminalViewClient set");

        // Enable touch and focus
        mTerminalView.setFocusable(true);
        mTerminalView.setFocusableInTouchMode(true);

        // Initialize renderer immediately - must happen after view is attached to window
        // Use post() to ensure the view is laid out and attached to window
        mTerminalView.post(() -> {
            Log.d(TAG, "post: initializing renderer, view dims=" + mTerminalView.getWidth() + "x" + mTerminalView.getHeight());
            initTerminalRenderer();
        });

        Log.d(TAG, "setupTerminalView: END");
    }

    private void initTerminalRenderer() {
        Log.d(TAG, "initTerminalRenderer: START");

        // Default font size in pixels (14sp * density)
        float density = getResources().getDisplayMetrics().scaledDensity;
        final int fontSizePx = (int) (14 * density);
        Log.d(TAG, "Font size: " + fontSizePx + "px (density=" + density + ")");

        // Use OnGlobalLayoutListener to initialize renderer on first layout.
        // setTextSize() handles invalid dimensions gracefully, so we always
        // initialize on first callback rather than waiting for specific dimensions.
        android.view.ViewTreeObserver.OnGlobalLayoutListener listener = new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            boolean mCalled = false;
            @Override
            public void onGlobalLayout() {
                if (mCalled) return;
                int w = mTerminalView.getWidth();
                int h = mTerminalView.getHeight();
                Log.d(TAG, "OnGlobalLayout: " + w + "x" + h);
                mCalled = true;
                mTerminalView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                try {
                    mTerminalView.setTextSize(fontSizePx);
                    Log.d(TAG, "setTextSize() called, initTerminalRenderer: SUCCESS");
                } catch (Exception e) {
                    Log.e(TAG, "setTextSize FAILED: " + e.getMessage());
                }
                mRendererInitialized = true;
            }
        };
        mTerminalView.getViewTreeObserver().addOnGlobalLayoutListener(listener);

        // Also trigger a layout pass request to ensure we get valid dimensions
        mTerminalView.requestLayout();
    }

    private void setupInputView() {
        // Send button click
        mTerminalSendButton.setOnClickListener(v -> {
            String command = mTerminalCommandInput.getText().toString();
            if (!command.isEmpty()) {
                sendCommand(command);
                mTerminalCommandInput.setText("");
            }
        });

        // Handle keyboard action
        mTerminalCommandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String command = mTerminalCommandInput.getText().toString();
                if (!command.isEmpty()) {
                    sendCommand(command);
                    mTerminalCommandInput.setText("");
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "=== onServiceConnected START ===");
        TermuxService.LocalBinder binder = (TermuxService.LocalBinder) service;
        mTermuxService = binder.service;
        mServiceBound = true;
        Log.d(TAG, "Service bound, sessions size: " + (mTermuxService != null ? mTermuxService.getTermuxSessionsSize() : -1));

        // Find and attach the session
        attachSession();
        Log.d(TAG, "=== onServiceConnected END ===");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mTermuxService = null;
        mServiceBound = false;
    }

    private void attachSession() {
        Log.d(TAG, "attachSession: START rendererInit=" + mRendererInitialized + " handle=" + mSessionHandle);

        // Try to find existing session by handle
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

        // Create new session
        createNewSession();
    }

    private void doAttachSession(TerminalSession session) {
        Log.d(TAG, "doAttachSession: START session=" + session + " handle=" + session.mHandle);
        Log.d(TAG, "View dims: " + mTerminalView.getWidth() + "x" + mTerminalView.getHeight());
        Log.d(TAG, "Renderer init: " + mRendererInitialized);

        // Ensure renderer is initialized first
        if (!mRendererInitialized) {
            Log.d(TAG, "Renderer not ready, waiting...");
            mTerminalView.postDelayed(() -> doAttachSession(session), 50);
            return;
        }

        mTerminalView.post(() -> {
            Log.d(TAG, "doAttachSession post: START");
            try {
                // Ensure we have correct dimensions and renderer
                int w = mTerminalView.getWidth();
                int h = mTerminalView.getHeight();
                Log.d(TAG, "doAttachSession post view dims: " + w + "x" + h);

                // If dimensions are still invalid, wait more
                if (h <= 0) {
                    Log.d(TAG, "Dimensions still invalid (h=" + h + "), retrying...");
                    mTerminalView.postDelayed(() -> doAttachSession(session), 100);
                    return;
                }

                // Force setTextSize to ensure renderer and emulator are properly initialized
                float density = getResources().getDisplayMetrics().scaledDensity;
                int fontSizePx = (int) (14 * density);
                mTerminalView.setTextSize(fontSizePx);

                boolean attached = mTerminalView.attachSession(session);
                Log.d(TAG, "attachSession returned: " + attached);

                // Force updateSize and redraw
                mTerminalView.updateSize();
                mTerminalView.invalidate();
                mTerminalView.onScreenUpdated();
                Log.d(TAG, "Attach: setTextSize+updateSize+invalidate+onScreenUpdated done");

                // Save the terminal handle to SessionManager
                if (mSessionId != null && session.mHandle != null) {
                    com.termux.app.sessions.SessionManager.INSTANCE.updateTerminalHandle(mSessionId, session.mHandle);
                }

                Log.d(TAG, "doAttachSession: SUCCESS");
            } catch (Exception e) {
                Log.e(TAG, "doAttachSession FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "附加会话失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TerminalSession findSessionByHandle(String handle) {
        if (mTermuxService == null) return null;

        try {
            for (TermuxSession termuxSession : mTermuxService.getTermuxSessions()) {
                TerminalSession ts = termuxSession.getTerminalSession();
                Log.d(TAG, "Comparing: '" + ts.mHandle + "' vs '" + handle + "'");
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
                Toast.makeText(this, "创建会话失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "createNewSession FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "创建会话失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCommand(String command) {
        if (mTerminalSession == null) return;

        String fullCommand = command.endsWith("\n") ? command : command + "\n";
        try {
            mTerminalSession.write(fullCommand.getBytes("UTF-8"), 0, fullCommand.length());
        } catch (Exception e) {
            Log.e(TAG, "Error sending command: " + e.getMessage());
        }
    }

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

        if (mServiceBound) {
            unbindService(this);
            mServiceBound = false;
        }
    }
}

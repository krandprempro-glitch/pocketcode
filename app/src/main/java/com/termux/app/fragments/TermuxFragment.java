package com.termux.app.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.app.terminal.CommandGroupAdapter;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxService;
import com.termux.app.TermuxActivity;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.ClaudeCodeMenuHelper;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import com.termux.app.ui.SSHFloatingActionButton;
import com.termux.app.ui.SSHConfigDialog;
import com.termux.app.models.SSHConnectionConfig;
import com.termux.app.models.SSHConfigManager;
import com.termux.app.configuration.managers.QuickCommandManager;
import com.termux.app.configuration.models.QuickCommand;
import com.termux.app.models.DirectoryBookmark;
import com.termux.app.ssh.SSHConnectionManager;
import com.termux.app.managers.ClaudeCodeCommandManager;
import com.termux.app.managers.ProjectWorkspaceManager;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSessionClient;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Terminal emulator fragment converted from TermuxActivity
 */
public class TermuxFragment extends Fragment implements ServiceConnection {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onViewCreated} with a call to
     * {@link getActivity().bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    private TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in TermuxFragment that displays the terminal.
     */
    private TerminalView mTerminalView;

    /**
     * The {@link TerminalViewClient} interface implementation to allow for communication between
     * {@link TerminalView} and {@link TermuxFragment}.
     */
    private TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     * The {@link TerminalSessionClient} interface implementation to allow for communication between
     * {@link TerminalSession} and {@link TermuxFragment}.
     */
    private TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The root view of the {@link TermuxFragment}.
     */
    private TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxFragment}.
     */
    private View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    private ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    private TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * Helper for Claude Code command menu.
     */
    private ClaudeCodeMenuHelper mClaudeCodeMenuHelper;

    /**
     * SSH floating action button for quick SSH configuration access.
     */
    private SSHFloatingActionButton mSSHFloatingActionButton;

    /**
     * SSH configuration dialog for managing SSH connections.
     */
    private SSHConfigDialog mSSHConfigDialog;

    /**
     * SSH connection manager for handling SSH connections.
     */
    private SSHConnectionManager mSSHConnectionManager;

    /**
     * Project workspace manager for handling bookmarks.
     */
    private ProjectWorkspaceManager mProjectWorkspaceManager;

    private CommandGroupAdapter mCommandAdapter;
    private final CompositeDisposable mCommandMenuDisposables = new CompositeDisposable();

    /**
     * The termux sessions list controller.
     */
    private TermuxSessionsListViewController mTermuxSessionListViewController;

    /**
     * The {@link TermuxFragment} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    private Toast mLastToast;

    /**
     * If between onResume() and onPause(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onViewCreated().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If fragment view was recreated like due to call to {@link Activity.recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsFragmentRecreated = false;

    /**
     * The {@link TermuxFragment} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private float mTerminalToolbarDefaultHeight;

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

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_FRAGMENT_RECREATED = "fragment_recreated";

    private static final String LOG_TAG = "TermuxFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreateView");
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsFragmentRecreated = savedInstanceState.getBoolean(ARG_FRAGMENT_RECREATED, false);

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(getContext(), 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        return inflater.inflate(R.layout.fragment_termux_simple, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.logInfo(LOG_TAG, "=== onViewCreated START ===");
        Logger.logInfo(LOG_TAG, "View: " + view + ", savedInstanceState: " + savedInstanceState);
        Logger.logInfo(LOG_TAG, "Fragment context: " + getContext());
        Logger.logInfo(LOG_TAG, "Fragment activity: " + getActivity());

        // Load termux shared preferences
        Logger.logInfo(LOG_TAG, "Loading TermuxAppSharedPreferences...");
        mPreferences = TermuxAppSharedPreferences.build(getContext(), true);
        if (mPreferences == null) {
            Logger.logError(LOG_TAG, "Failed to load TermuxAppSharedPreferences - marking as invalid state");
            // An AlertDialog should have shown to kill the app, so we don't continue running fragment code
            mIsInvalidState = true;
            return;
        }
        Logger.logInfo(LOG_TAG, "Successfully loaded preferences: " + mPreferences);

        // Initialize terminal view and clients - THIS IS CRUCIAL
        Logger.logInfo(LOG_TAG, "Calling setTermuxTerminalViewAndClients...");
        setTermuxTerminalViewAndClients(view);
        Logger.logInfo(LOG_TAG, "setTermuxTerminalViewAndClients completed, mTerminalView: " + mTerminalView);

        // Set up command input interface
        Logger.logInfo(LOG_TAG, "Setting up terminal input view...");
        setTerminalInputView(view);

        // Set up terminal toolbar with extra keys (arrow keys, etc.)
        Logger.logInfo(LOG_TAG, "Setting up terminal toolbar view...");
        setTerminalToolbarView(view, savedInstanceState);

        // Register for context menu only if terminal view was successfully initialized
        if (mTerminalView != null) {
            Logger.logInfo(LOG_TAG, "Registering terminal view for context menu...");
            registerForContextMenu(mTerminalView);
        } else {
            Logger.logError(LOG_TAG, "Cannot register context menu - mTerminalView is null");
        }

        // Start and bind to TermuxService
        Logger.logInfo(LOG_TAG, "Starting and binding to TermuxService...");
        try {
            Intent serviceIntent = new Intent(getContext(), TermuxService.class);
            Logger.logInfo(LOG_TAG, "Created service intent: " + serviceIntent);

            getActivity().startService(serviceIntent);
            Logger.logInfo(LOG_TAG, "Service started successfully");

            // Bind to the service
            boolean bindResult = getActivity().bindService(serviceIntent, this, 0);
            Logger.logInfo(LOG_TAG, "Bind service result: " + bindResult);

            if (!bindResult) {
                throw new RuntimeException("bindService() failed");
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to start or bind to TermuxService");
            Logger.logStackTraceWithMessage(LOG_TAG, "Service binding error", e);
            mIsInvalidState = true;
        }

        Logger.logInfo(LOG_TAG, "=== onViewCreated END ===");
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        // Enable client lifecycle methods - skip for MainTabActivity context
        // if (mTermuxTerminalSessionActivityClient != null)
        //     mTermuxTerminalSessionActivityClient.onStart();

        // if (mTermuxTerminalViewClient != null)
        //     mTermuxTerminalViewClient.onStart();

        registerTermuxActivityBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;

        // Enable client lifecycle methods - skip for MainTabActivity context
        // if (mTermuxTerminalSessionActivityClient != null)
        //     mTermuxTerminalSessionActivityClient.onResume();

        // if (mTermuxTerminalViewClient != null)
        //     mTermuxTerminalViewClient.onResume();

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        if (getContext() != null) {
            TermuxCrashUtils.notifyAppCrashFromCrashLogFile(getContext(), LOG_TAG);
        }

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        Logger.logDebug(LOG_TAG, "onPause");

        if (mIsInvalidState) return;

        mIsVisible = false;

        // Enable client lifecycle methods - skip for MainTabActivity context
        // if (mTermuxTerminalSessionActivityClient != null)
        //     mTermuxTerminalSessionActivityClient.onStop();

        // if (mTermuxTerminalViewClient != null)
        //     mTermuxTerminalViewClient.onStop();

        // 简化版本 - 不需要layout listener
        // removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();

        // 简化版本 - 没有drawer
        // if (getDrawer() != null) {
        //     getDrawer().closeDrawers();
        // }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Logger.logDebug(LOG_TAG, "onDestroyView");

        if (mIsInvalidState) return;

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to fragment.
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }

        // Cleanup Claude Code menu
        if (mClaudeCodeMenuHelper != null) {
            mClaudeCodeMenuHelper.dismiss();
            mClaudeCodeMenuHelper = null;
        }

        mCommandMenuDisposables.clear();

        // Cleanup SSH dialog
        if (mSSHConfigDialog != null) {
            mSSHConfigDialog.dismiss();
            mSSHConfigDialog = null;
        }

        // Cleanup SSH connection manager
        mSSHConnectionManager = null;

        try {
            if (getContext() != null) {
                getContext().unbindService(this);
            }
        } catch (Exception e) {
            // ignore.
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(outState);
        saveTerminalToolbarTextInput(outState);
        outState.putBoolean(ARG_FRAGMENT_RECREATED, true);
    }

    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link Context.bindService(Intent, ServiceConnection, int)} in {@link #onViewCreated} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logInfo(LOG_TAG, "=== onServiceConnected START ===");
        Logger.logInfo(LOG_TAG, "Component: " + componentName + ", Service binder: " + service);

        // Get service from binder using reflection since LocalBinder is package-private
        try {
            Logger.logInfo(LOG_TAG, "Attempting to get TermuxService from binder...");
            // Use reflection to access the service field
            Object binder = service;
            Logger.logInfo(LOG_TAG, "Binder class: " + binder.getClass().getName());
            java.lang.reflect.Field serviceField = binder.getClass().getField("service");
            mTermuxService = (TermuxService) serviceField.get(binder);
            Logger.logInfo(LOG_TAG, "Successfully obtained TermuxService: " + mTermuxService);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to get TermuxService from binder");
            Logger.logStackTraceWithMessage(LOG_TAG, "Reflection error details", e);
            return;
        }

        // For MainTabActivity context, we'll create a simple terminal session
        // without requiring TermuxActivity-specific components
        Logger.logInfo(LOG_TAG, "Checking conditions for session creation:");
        Logger.logInfo(LOG_TAG, "  - mIsVisible: " + mIsVisible);
        Logger.logInfo(LOG_TAG, "  - getActivity(): " + getActivity());
        Logger.logInfo(LOG_TAG, "  - mTerminalView: " + mTerminalView);

        if (mIsVisible && getActivity() != null && mTerminalView != null) {
            try {
                Logger.logInfo(LOG_TAG, "Creating terminal session...");

                // Set up a simple terminal session client for the Fragment context
                // This is crucial for handling session output and updates
                Logger.logInfo(LOG_TAG, "Setting up terminal session client...");
                TermuxTerminalSessionClientBase fragmentSessionClient = new TermuxTerminalSessionClientBase() {
                    @Override
                    public void onTextChanged(TerminalSession changedSession) {
                        Logger.logVerbose(LOG_TAG, "Terminal session text changed");
                        // Force terminal view to refresh when text changes
                        if (mTerminalView != null && changedSession == mTerminalView.getCurrentSession()) {
                            mTerminalView.post(() -> {
                                mTerminalView.invalidate();
                                Logger.logVerbose(LOG_TAG, "Terminal view refreshed due to text change");
                            });
                        }
                    }

                    @Override
                    public void onTitleChanged(TerminalSession changedSession) {
                        Logger.logVerbose(LOG_TAG, "Terminal title changed: " + changedSession.getTitle());
                    }

                    @Override
                    public void onSessionFinished(TerminalSession finishedSession) {
                        Logger.logInfo(LOG_TAG, "Terminal session finished: " + finishedSession.getTitle());
                    }

                    @Override
                    public void onBell(TerminalSession session) {
                        Logger.logVerbose(LOG_TAG, "Terminal bell");
                    }

                    @Override
                    public void onColorsChanged(TerminalSession changedSession) {
                        Logger.logVerbose(LOG_TAG, "Terminal colors changed");
                        if (mTerminalView != null) {
                            mTerminalView.post(() -> mTerminalView.invalidate());
                        }
                    }
                };

                // Don't use setTermuxTerminalSessionClient as it expects TermuxTerminalSessionActivityClient
                // Instead, we'll set the client directly on the session after creation
                Logger.logInfo(LOG_TAG, "Terminal session client prepared (will be set on individual sessions)");

                // Create a new shell session using TermuxService
                TermuxSession newTermuxSession = mTermuxService.createTermuxSession(null, null, null, null, false, null);
                Logger.logInfo(LOG_TAG, "Created TermuxSession: " + newTermuxSession);

                if (newTermuxSession != null) {
                    // Get the TerminalSession from TermuxSession and attach to view
                    TerminalSession terminalSession = newTermuxSession.getTerminalSession();
                    Logger.logInfo(LOG_TAG, "Got TerminalSession from TermuxSession: " + terminalSession);

                    if (terminalSession != null) {
                        Logger.logInfo(LOG_TAG, "Setting terminal session client on the session...");
                        terminalSession.updateTerminalSessionClient(fragmentSessionClient);
                        Logger.logInfo(LOG_TAG, "Terminal session client set on session");

                        Logger.logInfo(LOG_TAG, "Attaching session to terminal view...");
                        mTerminalView.attachSession(terminalSession);
                        Logger.logInfo(LOG_TAG, "Successfully attached terminal session to view");

                        // Check terminal view state after attach
                        Logger.logInfo(LOG_TAG, "Terminal view state after attach:");
                        Logger.logInfo(LOG_TAG, "  - Current session: " + mTerminalView.getCurrentSession());
                        Logger.logInfo(LOG_TAG, "  - Session attached successfully: " + (mTerminalView.getCurrentSession() == terminalSession));

                        // Check session detailed state
                        Logger.logInfo(LOG_TAG, "Terminal session details:");
                        Logger.logInfo(LOG_TAG, "  - Session is running: " + terminalSession.isRunning());
                        Logger.logInfo(LOG_TAG, "  - Session PID: " + terminalSession.getPid());
                        Logger.logInfo(LOG_TAG, "  - Session title: " + terminalSession.getTitle());

                        // Check if emulator is set
                        if (terminalSession.getEmulator() != null) {
                            Logger.logInfo(LOG_TAG, "  - Emulator columns: " + terminalSession.getEmulator().mColumns);
                            Logger.logInfo(LOG_TAG, "  - Emulator rows: " + terminalSession.getEmulator().mRows);
                            Logger.logInfo(LOG_TAG, "  - Screen lines: " + terminalSession.getEmulator().getScreen().getActiveRows());
                        } else {
                            Logger.logInfo(LOG_TAG, "  - Emulator is null");
                        }

                        // Force a view refresh
                        Logger.logInfo(LOG_TAG, "Forcing terminal view refresh...");
                        mTerminalView.post(() -> {
                            mTerminalView.invalidate();
                            mTerminalView.requestLayout();
                            Logger.logInfo(LOG_TAG, "Terminal view refresh completed");
                        });
                    } else {
                        Logger.logError(LOG_TAG, "TerminalSession from TermuxSession is null");
                    }
                } else {
                    Logger.logError(LOG_TAG, "Failed to create TermuxSession - returned null");
                }
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Exception during session creation and attachment");
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to create and attach terminal session", e);
            }
        } else {
            Logger.logWarn(LOG_TAG, "Skipping session creation due to failed conditions");
        }

        Logger.logInfo(LOG_TAG, "=== onServiceConnected END ===");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().finish();
        }
    }

    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        // Skip client property reload in MainTabActivity context
        // if (mTermuxTerminalViewClient != null)
        //     mTermuxTerminalViewClient.onReloadProperties();
    }

    private void setMargins(View view) {
        RelativeLayout relativeLayout = view.findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }

    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    private void setTermuxTerminalViewAndClients(View view) {
        Logger.logInfo(LOG_TAG, "=== setTermuxTerminalViewAndClients START ===");
        Logger.logInfo(LOG_TAG, "Fragment context: " + getContext());
        Logger.logInfo(LOG_TAG, "Fragment activity: " + getActivity());
        Logger.logInfo(LOG_TAG, "View provided: " + view);

        // Get terminal view
        mTerminalView = view.findViewById(R.id.terminal_view);
        Logger.logInfo(LOG_TAG, "Found TerminalView: " + mTerminalView);

        if (mTerminalView == null) {
            Logger.logError(LOG_TAG, "Terminal view not found in layout - checking view hierarchy");
            Logger.logError(LOG_TAG, "Available view IDs in root view:");
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    Logger.logError(LOG_TAG, "Child " + i + ": " + child + " id=" + child.getId());
                }
            }
            return;
        }

        // Log terminal view details
        Logger.logInfo(LOG_TAG, "TerminalView details:");
        Logger.logInfo(LOG_TAG, "  - Width: " + mTerminalView.getWidth() + ", Height: " + mTerminalView.getHeight());
        Logger.logInfo(LOG_TAG, "  - Layout params: " + mTerminalView.getLayoutParams());
        Logger.logInfo(LOG_TAG, "  - Visibility: " + mTerminalView.getVisibility());
        Logger.logInfo(LOG_TAG, "  - Current session: " + mTerminalView.getCurrentSession());
        Logger.logInfo(LOG_TAG, "  - Is focusable: " + mTerminalView.isFocusable());
        Logger.logInfo(LOG_TAG, "  - Has focus: " + mTerminalView.hasFocus());

        // Check if view will actually be measured and laid out
        mTerminalView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Logger.logInfo(LOG_TAG, "TerminalView layout completed - Size: " + mTerminalView.getWidth() + "x" + mTerminalView.getHeight());
                mTerminalView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        try {
            // Create a simple terminal view client that doesn't require TermuxActivity
            TerminalViewClient simpleClient = new TerminalViewClient() {
                @Override
                public float onScale(float scale) {
                    return 1.0f; // Default scaling
                }

                @Override
                public void onSingleTapUp(MotionEvent e) {
                    Logger.logInfo(LOG_TAG, "=== TERMINAL TAP DEBUG START ===");
                    Logger.logInfo(LOG_TAG, "Terminal view available: " + (mTerminalView != null));
                    
                    // 参考TermuxActivity实现，点击终端时弹出输入法
                    if (mTerminalView != null) {
                        TerminalSession currentSession = mTerminalView.getCurrentSession();
                        Logger.logInfo(LOG_TAG, "Current session available: " + (currentSession != null));
                        
                        if (currentSession != null) {
                            TerminalEmulator term = currentSession.getEmulator();
                            Logger.logInfo(LOG_TAG, "Terminal emulator available: " + (term != null));
                            
                            if (term != null) {
                                boolean mouseTracking = term.isMouseTrackingActive();
                                boolean isMouseEvent = e.isFromSource(InputDevice.SOURCE_MOUSE);
                                Logger.logInfo(LOG_TAG, "Mouse tracking active: " + mouseTracking);
                                Logger.logInfo(LOG_TAG, "Is mouse event: " + isMouseEvent);
                                
                                // 检查是否不是鼠标追踪且不是鼠标事件
                                if (!mouseTracking && !isMouseEvent) {
                                    Logger.logInfo(LOG_TAG, "Conditions met for showing keyboard");
                                    // 弹出软键盘
                                    if (getActivity() != null) {
                                        Logger.logInfo(LOG_TAG, "Activity available: true");
                                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                        if (imm != null) {
                                            Logger.logInfo(LOG_TAG, "InputMethodManager available: true");
                                            Logger.logInfo(LOG_TAG, "Requesting focus and showing soft input...");
                                            mTerminalView.requestFocus();
                                            boolean result = imm.showSoftInput(mTerminalView, InputMethodManager.SHOW_IMPLICIT);
                                            Logger.logInfo(LOG_TAG, "showSoftInput() returned: " + result);
                                            
                                            // 尝试强制显示
                                            if (!result) {
                                                Logger.logInfo(LOG_TAG, "First attempt failed, trying SHOW_FORCED...");
                                                boolean forcedResult = imm.showSoftInput(mTerminalView, InputMethodManager.SHOW_FORCED);
                                                Logger.logInfo(LOG_TAG, "showSoftInput(SHOW_FORCED) returned: " + forcedResult);
                                            }
                                        } else {
                                            Logger.logError(LOG_TAG, "InputMethodManager is null");
                                        }
                                    } else {
                                        Logger.logError(LOG_TAG, "Activity is null");
                                    }
                                } else {
                                    Logger.logInfo(LOG_TAG, "Conditions NOT met - mouse tracking: " + mouseTracking + ", mouse event: " + isMouseEvent);
                                }
                            } else {
                                Logger.logError(LOG_TAG, "Terminal emulator is null");
                            }
                        } else {
                            Logger.logError(LOG_TAG, "Current session is null");
                        }
                    } else {
                        Logger.logError(LOG_TAG, "Terminal view is null");
                    }
                    Logger.logInfo(LOG_TAG, "=== TERMINAL TAP DEBUG END ===");
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
                public void copyModeChanged(boolean copyMode) {
                    // Handle copy mode changes if needed
                }

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
                public void onEmulatorSet() {
                    // Terminal emulator has been set
                }

                @Override
                public void logError(String tag, String message) {
                    Logger.logError(tag, message);
                }

                @Override
                public void logWarn(String tag, String message) {
                    Logger.logWarn(tag, message);
                }

                @Override
                public void logInfo(String tag, String message) {
                    Logger.logInfo(tag, message);
                }

                @Override
                public void logDebug(String tag, String message) {
                    Logger.logDebug(tag, message);
                }

                @Override
                public void logVerbose(String tag, String message) {
                    Logger.logVerbose(tag, message);
                }

                @Override
                public void logStackTraceWithMessage(String tag, String message, Exception e) {
                    Logger.logStackTraceWithMessage(tag, message, e);
                }

                @Override
                public void logStackTrace(String tag, Exception e) {
                    Logger.logStackTrace(tag, e);
                }
            };

            // Set the view client - this sets the client but doesn't initialize renderer yet
            Logger.logInfo(LOG_TAG, "Setting TerminalViewClient...");
            mTerminalView.setTerminalViewClient(simpleClient);
            Logger.logInfo(LOG_TAG, "TerminalViewClient set successfully");

            // CRITICAL: Initialize the terminal renderer by setting text size
            // Without this, mRenderer stays null and causes NullPointerException
            Logger.logInfo(LOG_TAG, "Initializing terminal renderer with text size...");

            // Use density-independent pixels for text size (sp to px conversion)
            float density = getContext().getResources().getDisplayMetrics().scaledDensity;
            int defaultTextSizeSp = 14; // 14sp is a good default for terminal
            int defaultTextSizePx = (int) (defaultTextSizeSp * density);

            Logger.logInfo(LOG_TAG, "Screen density: " + density + ", text size: " + defaultTextSizeSp + "sp = " + defaultTextSizePx + "px");
            mTerminalView.setTextSize(defaultTextSizePx);
            Logger.logInfo(LOG_TAG, "Terminal renderer initialized with text size: " + defaultTextSizePx + "px");

            // Check if renderer is initialized after setting text size
            try {
                java.lang.reflect.Field rendererField = mTerminalView.getClass().getDeclaredField("mRenderer");
                rendererField.setAccessible(true);
                Object renderer = rendererField.get(mTerminalView);
                Logger.logInfo(LOG_TAG, "Terminal renderer after setTextSize: " + renderer);

                if (renderer != null) {
                    java.lang.reflect.Field fontWidthField = renderer.getClass().getDeclaredField("mFontWidth");
                    fontWidthField.setAccessible(true);
                    Object fontWidth = fontWidthField.get(renderer);
                    Logger.logInfo(LOG_TAG, "Renderer mFontWidth: " + fontWidth);

                    // Also check other critical renderer fields
                    java.lang.reflect.Field fontHeightField = renderer.getClass().getDeclaredField("mFontLineSpacing");
                    fontHeightField.setAccessible(true);
                    Object fontHeight = fontHeightField.get(renderer);
                    Logger.logInfo(LOG_TAG, "Renderer mFontLineSpacing: " + fontHeight);
                } else {
                    Logger.logError(LOG_TAG, "Renderer is still null after setTextSize - this is the problem!");
                }
            } catch (Exception reflectEx) {
                Logger.logWarn(LOG_TAG, "Could not check renderer state via reflection: " + reflectEx.getMessage());
            }

            Logger.logInfo(LOG_TAG, "=== setTermuxTerminalViewAndClients SUCCESS ===");
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "=== setTermuxTerminalViewAndClients FAILED ===");
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to initialize terminal view client", e);
        }
    }


    private void setTerminalToolbarView(View view, Bundle savedInstanceState) {
        Logger.logInfo(LOG_TAG, "=== setTerminalToolbarView START ===");

        try {
            // For TermuxFragment, we'll create a simplified extra keys setup
            // since we don't have full TermuxActivity-style clients available
            Logger.logInfo(LOG_TAG, "Setting up simplified extra keys for Fragment context...");

            // Instead of creating full TermuxActivity clients, we'll use a simplified approach
            // The terminal toolbar will still be functional with basic extra keys
            Logger.logInfo(LOG_TAG, "Skipping complex client creation in Fragment context");

            final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager(view);
            Logger.logInfo(LOG_TAG, "Terminal toolbar ViewPager: " + terminalToolbarViewPager);

            if (terminalToolbarViewPager == null) {
                Logger.logError(LOG_TAG, "Terminal toolbar ViewPager not found in layout!");
                return;
            }

            // Check preference for showing terminal toolbar - same logic as TermuxActivity
            // For Fragment context, always force visible to ensure toolbar shows up
            Logger.logInfo(LOG_TAG, "Forcing terminal toolbar to be visible in Fragment context");
            terminalToolbarViewPager.setVisibility(View.VISIBLE);
            Logger.logInfo(LOG_TAG, "Terminal toolbar visibility set to VISIBLE (forced)");

            ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
            mTerminalToolbarDefaultHeight = layoutParams.height;
            Logger.logInfo(LOG_TAG, "Terminal toolbar default height: " + mTerminalToolbarDefaultHeight);

            setTerminalToolbarHeight(view);

            String savedTextInput = null;
            if (savedInstanceState != null)
                savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);
            Logger.logInfo(LOG_TAG, "Saved text input: " + savedTextInput);

            // Set up the ViewPager adapter - create a Fragment-compatible version
            Logger.logInfo(LOG_TAG, "Setting Fragment-compatible ViewPager adapter...");
            terminalToolbarViewPager.setAdapter(new FragmentTerminalToolbarPageAdapter(this, savedTextInput));
            terminalToolbarViewPager.addOnPageChangeListener(new FragmentTerminalToolbarOnPageChangeListener(this, terminalToolbarViewPager));

            Logger.logInfo(LOG_TAG, "=== setTerminalToolbarView SUCCESS ===");
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "=== setTerminalToolbarView FAILED ===");
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to initialize terminal toolbar", e);
        }
    }

    private void setTerminalToolbarHeight(View view) {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager(view);
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();

        // Use a safe default if mTermuxTerminalExtraKeys is not initialized yet
        int extraKeysRows = 1; // Default to 1 row for arrow keys
        if (mTermuxTerminalExtraKeys != null && mTermuxTerminalExtraKeys.getExtraKeysInfo() != null) {
            extraKeysRows = mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length;
        }

        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            extraKeysRows * mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager(getView());
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(getContext(), (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            getView().findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null || getView() == null) return;

        final EditText textInputView = getView().findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty())
                savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }

    private void setSettingsButtonView(View view) {
        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            ActivityUtils.startActivity(getContext(), new Intent(getContext(), SettingsActivity.class));
        });
    }

    private void setNewSessionButtonView(View view) {
        View newSessionButton = view.findViewById(R.id.new_session_button);
        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));
        newSessionButton.setOnLongClickListener(v -> {
            TextInputDialogUtils.textInput(getActivity(), R.string.title_create_named_session, null,
                R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                -1, null, null);
            return true;
        });
    }

    private void setToggleKeyboardView(View view) {
        view.findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
            if (getDrawer() != null) {
                getDrawer().closeDrawers();
            }
        });

        view.findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            toggleTerminalToolbar();
            return true;
        });
    }

    private void setTerminalInputView(View view) {
        Logger.logInfo(LOG_TAG, "=== setTerminalInputView START ===");

        EditText terminalCommandInput = view.findViewById(R.id.terminal_command_input);
        ImageButton terminalSendButton = view.findViewById(R.id.terminal_send_button);
        ImageButton claudeCodeMenuButton = view.findViewById(R.id.claude_code_menu_button);

        Logger.logInfo(LOG_TAG, "Input UI elements:");
        Logger.logInfo(LOG_TAG, "  - terminalCommandInput: " + terminalCommandInput);
        Logger.logInfo(LOG_TAG, "  - terminalSendButton: " + terminalSendButton);
        Logger.logInfo(LOG_TAG, "  - claudeCodeMenuButton: " + claudeCodeMenuButton);

        if (terminalCommandInput == null || terminalSendButton == null) {
            Logger.logError(LOG_TAG, "Essential input elements missing - cannot set up terminal input");
            return;
        }

        // Initialize Claude Code menu helper - 使用Fragment的Activity context
        try {
            mClaudeCodeMenuHelper = new ClaudeCodeMenuHelper((TermuxActivity) getActivity());
            Logger.logInfo(LOG_TAG, "Claude Code menu helper initialized successfully");
        } catch (Exception e) {
            Logger.logWarn(LOG_TAG, "Failed to initialize Claude Code menu helper, creating simplified version: " + e.getMessage());
            mClaudeCodeMenuHelper = null; // 使用简化版本
        }

        // 设置输入框属性确保能弹出输入法
        terminalCommandInput.setFocusable(true);
        terminalCommandInput.setFocusableInTouchMode(true);
        terminalCommandInput.setClickable(true);

        // 设置点击监听器确保获得焦点并弹出输入法
        terminalCommandInput.setOnClickListener(v -> {
            v.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // 设置焦点监听器
        terminalCommandInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        terminalCommandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendCommandToTerminal(terminalCommandInput.getText().toString());
                terminalCommandInput.setText("");
                return true;
            }
            return false;
        });

        // Add keyboard shortcut support
        terminalCommandInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleClaudeCodeShortcuts(keyCode, event, terminalCommandInput);
            }
            return false;
        });

        terminalSendButton.setOnClickListener(v -> {
            String command = terminalCommandInput.getText().toString();
            Logger.logInfo(LOG_TAG, "Send button clicked with command: '" + command + "'");
            sendCommandToTerminal(command);
            terminalCommandInput.setText("");
        });

        // Setup Claude Code menu button
        if (claudeCodeMenuButton != null) {
            Logger.logInfo(LOG_TAG, "Setting up Claude Code menu button");
            claudeCodeMenuButton.setOnClickListener(v -> {
                if (mClaudeCodeMenuHelper != null) {
                    mClaudeCodeMenuHelper.showMenu(claudeCodeMenuButton);
                } else {
                    // 简化版本的快捷指令菜单
                    showSimpleClaudeCodeMenu(claudeCodeMenuButton, terminalCommandInput);
                }
            });
        } else {
            Logger.logInfo(LOG_TAG, "Claude Code menu button not found in layout");
        }

        Logger.logInfo(LOG_TAG, "=== setTerminalInputView SUCCESS ===");
    }

    public void sendCommandToTerminal(String command) {
        Logger.logInfo(LOG_TAG, "=== sendCommandToTerminal START ===");
        Logger.logInfo(LOG_TAG, "Command to send: '" + command + "'");

        TerminalSession currentSession = getCurrentSession();
        Logger.logInfo(LOG_TAG, "Current session: " + currentSession);

        if (currentSession != null && currentSession.isRunning()) {
            Logger.logInfo(LOG_TAG, "Session is running, sending command...");

            // 如果命令为空或只有空格，只发送回车
            if (command == null || command.trim().isEmpty()) {
                Logger.logInfo(LOG_TAG, "Empty command - sending just Enter key");
                byte[] enterBytes = "\r".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                currentSession.write(enterBytes, 0, enterBytes.length);
            } else {
                // 有内容的命令，发送命令+回车
                byte[] commandBytes = (command.trim() + "\r").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                currentSession.write(commandBytes, 0, commandBytes.length);
            }
            Logger.logInfo(LOG_TAG, "Command sent successfully");
        } else {
            Logger.logError(LOG_TAG, "Cannot send command - session is null or not running");
            if (currentSession != null) {
                Logger.logError(LOG_TAG, "Session state - isRunning: " + currentSession.isRunning());
            }
            showToast("终端会话未运行", false);
        }

        Logger.logInfo(LOG_TAG, "=== sendCommandToTerminal END ===");
    }

    /**
     * 从Extra Keys调用Claude Code菜单的公共方法
     */
    public void openClaudeCodeMenuFromExtraKeys() {
        Logger.logInfo(LOG_TAG, "Opening Claude Code menu from Extra Keys");
        
        if (mClaudeCodeMenuHelper != null) {
            ImageButton claudeCodeButton = getView().findViewById(R.id.claude_code_menu_button);
            if (claudeCodeButton != null) {
                mClaudeCodeMenuHelper.showMenu(claudeCodeButton);
                Logger.logInfo(LOG_TAG, "Claude Code menu opened successfully via helper");
            } else {
                Logger.logWarn(LOG_TAG, "Claude Code menu button not found");
            }
        } else {
            // 使用简化版菜单
            Logger.logInfo(LOG_TAG, "Using simplified Claude Code menu");
            ImageButton claudeCodeButton = getView().findViewById(R.id.claude_code_menu_button);
            EditText commandInput = getView().findViewById(R.id.terminal_command_input);
            if (claudeCodeButton != null && commandInput != null) {
                showSimpleClaudeCodeMenu(claudeCodeButton, commandInput);
            } else {
                Logger.logWarn(LOG_TAG, "UI elements not found for simplified menu");
            }
        }
    }

    /**
     * 显示Claude Code快捷指令菜单，使用新的BottomSheet分组展示
     */
    private void showSimpleClaudeCodeMenu(ImageButton anchor, EditText commandInput) {
        Logger.logInfo(LOG_TAG, "Showing Claude Code menu with grouped commands");

        if (getContext() == null) return;

        // 创建BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext())
            .inflate(R.layout.bottom_sheet_claude_commands, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // 获取UI控件
        RecyclerView commandsRecyclerView = bottomSheetView.findViewById(R.id.commands_recycler_view);

        // 准备分组数据
        List<CommandGroupAdapter.CommandGroup> commandGroups = prepareCommandGroups();

        // 设置适配器
        mCommandAdapter = new CommandGroupAdapter(
            commandGroups,
            commandInput,
            command -> bottomSheetDialog.dismiss()
        );

        commandsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commandsRecyclerView.setAdapter(mCommandAdapter);

        // 显示BottomSheet
        bottomSheetDialog.show();

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

    /**
     * 准备三大类命令分组数据
     */
    private List<CommandGroupAdapter.CommandGroup> prepareCommandGroups() {
        List<CommandGroupAdapter.CommandGroup> groups = new ArrayList<>();

        // 1. 收藏路径类 (第一类)
        List<ClaudeCodeMenuHelper.Command> bookmarkCommands = new ArrayList<>();
        try {
            // 延迟初始化 ProjectWorkspaceManager
            if (mProjectWorkspaceManager == null && getContext() != null) {
                mProjectWorkspaceManager = ProjectWorkspaceManager.getInstance(getContext());
            }
            if (mProjectWorkspaceManager != null) {
                List<DirectoryBookmark> bookmarks = mProjectWorkspaceManager.getAllBookmarks();
                for (DirectoryBookmark bookmark : bookmarks) {
                    String cdCommand = "cd " + bookmark.getFullPath();
                    bookmarkCommands.add(new ClaudeCodeMenuHelper.Command(cdCommand, bookmark.getDisplayName()));
                }
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to load bookmarks for menu: " + e.getMessage());
        }

        // 始终显示收藏路径分组，如果没有则显示提示
        if (bookmarkCommands.isEmpty()) {
            bookmarkCommands.add(new ClaudeCodeMenuHelper.Command("", "暂无收藏路径"));
        }
        groups.add(new CommandGroupAdapter.CommandGroup(
            CommandGroupAdapter.CommandCategory.BOOKMARKS, bookmarkCommands));

        // 2. SSH连接类 (第二类)
        List<ClaudeCodeMenuHelper.Command> sshCommands = new ArrayList<>();
        try {
            SSHConfigManager sshConfigManager = SSHConfigManager.getInstance(getContext());
            List<SSHConnectionConfig> sshConfigs = sshConfigManager.getAllConfigs();
            Logger.logDebug(LOG_TAG, "SSH configs loaded: " + sshConfigs.size());

            for (SSHConnectionConfig config : sshConfigs) {
                String sshCommand = config.generateSSHCommand();
                Logger.logDebug(LOG_TAG, "SSH command for " + config.getName() + ": " + sshCommand);
                if (sshCommand != null) {
                    String displayName = config.getDisplayName();
                    String desc = config.getUsername() + "@" + config.getHost();
                    if (config.getPort() != 22) desc += ":" + config.getPort();
                    sshCommands.add(new ClaudeCodeMenuHelper.Command(sshCommand, desc, config.getName()));
                }
            }
            Logger.logDebug(LOG_TAG, "SSH commands prepared: " + sshCommands.size());
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to load SSH configs for menu: " + e.getMessage());
        }

        if (!sshCommands.isEmpty()) {
            groups.add(new CommandGroupAdapter.CommandGroup(
                CommandGroupAdapter.CommandCategory.SSH_CONNECTIONS, sshCommands, true));
        }

        // 3. 常用指令类 (第三类) - 从 QuickCommandManager 加载用户保存的指令
        List<ClaudeCodeMenuHelper.Command> quickCommands = new ArrayList<>();
        try {
            QuickCommandManager quickCommandManager = QuickCommandManager.getInstance(getContext());
            List<QuickCommand> savedCommands = quickCommandManager.getAllCommands();

            for (QuickCommand cmd : savedCommands) {
                String description = cmd.getDescription();
                if (description == null || description.isEmpty()) {
                    description = cmd.getCategory();
                }
                quickCommands.add(new ClaudeCodeMenuHelper.Command(cmd.getCommand(), description));
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to load quick commands for menu: " + e.getMessage());
        }

        // 始终显示常用指令分组，如果没有则显示提示
        if (quickCommands.isEmpty()) {
            quickCommands.add(new ClaudeCodeMenuHelper.Command("", "暂无常用指令，可以去配置页面自定义指令"));
        }
        groups.add(new CommandGroupAdapter.CommandGroup(
            CommandGroupAdapter.CommandCategory.QUICK_COMMANDS, quickCommands));

        // 4. AI指令类 (内置命令)
        List<ClaudeCodeMenuHelper.Command> aiCommands = ClaudeCodeCommandManager.getInstance().getDefaultCommands();
        groups.add(new CommandGroupAdapter.CommandGroup(
            CommandGroupAdapter.CommandCategory.AI_COMMANDS, aiCommands));

        // 4b. AI自定义指令 (远程用户自定义命令)
        List<ClaudeCodeMenuHelper.Command> customCommands = ClaudeCodeCommandManager.getInstance().getCustomCommands();
        if (!customCommands.isEmpty()) {
            groups.add(new CommandGroupAdapter.CommandGroup(
                CommandGroupAdapter.CommandCategory.AI_CUSTOM_COMMANDS, customCommands));
        }

        // 5. 系统指令类
        List<ClaudeCodeMenuHelper.Command> systemCommands = Arrays.asList(
            new ClaudeCodeMenuHelper.Command("claude", "启动Claude Code"),
            new ClaudeCodeMenuHelper.Command("claude --resume", "恢复上次会话"),
            new ClaudeCodeMenuHelper.Command("claude -p \"\"", "快速提问模式"),
            new ClaudeCodeMenuHelper.Command("codex", "启动Codex")
        );
        groups.add(new CommandGroupAdapter.CommandGroup(
            CommandGroupAdapter.CommandCategory.SYSTEM_COMMANDS, systemCommands));

        return groups;
    }


    /**
     * Handle Claude Code keyboard shortcuts
     */
    private boolean handleClaudeCodeShortcuts(int keyCode, KeyEvent event, EditText editText) {
        boolean ctrlPressed = event.isCtrlPressed();
        boolean shiftPressed = event.isShiftPressed();

        if (shiftPressed && keyCode == KeyEvent.KEYCODE_TAB) {
            // Shift + Tab: 打开指令菜单
            if (mClaudeCodeMenuHelper != null) {
                ImageButton claudeCodeButton = getView().findViewById(R.id.claude_code_menu_button);
                if (claudeCodeButton != null) {
                    mClaudeCodeMenuHelper.showMenu(claudeCodeButton);
                }
            }
            return true;
        }

        if (ctrlPressed) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_L:
                    // Ctrl + L: 清屏
                    editText.setText("/clear");
                    editText.setSelection(editText.getText().length());
                    showToast("Claude Code: 清屏指令", false);
                    return true;

                case KeyEvent.KEYCODE_K:
                    // Ctrl + K: 文件搜索
                    showToast("Claude Code: 打开文件搜索", false);
                    return true;

                case KeyEvent.KEYCODE_ENTER:
                    // Ctrl + Enter: 发送消息
                    sendCommandToTerminal(editText.getText().toString());
                    editText.setText("");
                    return true;

                case KeyEvent.KEYCODE_SLASH:
                    // Ctrl + /: 快捷键帮助
                    if (mClaudeCodeMenuHelper != null) {
                        ImageButton claudeCodeButton = getView().findViewById(R.id.claude_code_menu_button);
                        if (claudeCodeButton != null) {
                            mClaudeCodeMenuHelper.showMenu(claudeCodeButton);
                        }
                    }
                    return true;
            }
        }

        return false;
    }

    /**
     * Show a toast and dismiss the last one if still visible.
     */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(getContext(), text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                // URL selection not available in simplified mode
                showToast("URL selection not available", false);
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                // Transcript sharing not available in simplified mode  
                showToast("Transcript sharing not available", false);
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                // Selected text sharing not available in simplified mode
                showToast("Text sharing not available", false);
                return true;
            case CONTEXT_MENU_AUTOFILL_USERNAME:
                if (mTerminalView != null) mTerminalView.requestAutoFillUsername();
                return true;
            case CONTEXT_MENU_AUTOFILL_PASSWORD:
                if (mTerminalView != null) mTerminalView.requestAutoFillPassword();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(getContext(), new Intent(getContext(), HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(getContext(), new Intent(getContext(), SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                // Issue reporting not available in simplified mode
                showToast("Issue reporting not available", false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(getContext());
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);

            // Reset terminal session without requiring TermuxActivity-specific client
            // if (mTermuxTerminalSessionActivityClient != null)
            //     mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            // The startActivity() call is not documented to throw IllegalArgumentException.
            // However, crash reporting shows that it sometimes does, so catch it here.
            new AlertDialog.Builder(getContext()).setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(getContext(), new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }

    /**
     * 设置SSH悬浮按钮
     */
    private void setSSHFloatingActionButton(View view) {
        mSSHFloatingActionButton = view.findViewById(R.id.ssh_floating_button);
        if (mSSHFloatingActionButton == null) {
            Logger.logError(LOG_TAG, "SSH floating button not found in layout");
            return;
        }

        // 初始化SSH连接管理器
        mSSHConnectionManager = new SSHConnectionManager(getContext());

        // 初始化项目工作区管理器
        mProjectWorkspaceManager = ProjectWorkspaceManager.getInstance(getContext());

        // 初始化SSH配置对话框
        mSSHConfigDialog = new SSHConfigDialog(getContext());
        mSSHConfigDialog.setOnSSHConfigListener(new SSHConfigDialog.OnSSHConfigListener() {
            @Override
            public void onSSHConnect(SSHConnectionConfig config) {
                Logger.logInfo(LOG_TAG, "Generating SSH command for: " + config.getHost());
                showToast("生成SSH连接命令...", false);

                // 使用SSH连接管理器保存配置并生成命令
                mSSHConnectionManager.saveConfigAndGenerateCommand(config, new SSHConnectionManager.SSHConnectionCallback() {
                    @Override
                    public void onCommandGenerated(String sshCommand) {
                        // 将SSH命令发送到当前终端
                        TerminalSession currentSession = getCurrentSession();
                        if (currentSession != null && currentSession.isRunning()) {
                            mSSHConnectionManager.sendCommandToTerminal(sshCommand, currentSession);
                            showToast("SSH命令已发送到终端", false);
                        } else {
                            showToast("当前没有活动的终端会话", true);
                        }
                    }

                    @Override
                    public void onConfigSaved(SSHConnectionConfig config) {
                        showToast("SSH配置已保存", false);
                    }

                    @Override
                    public void onError(String error) {
                        showToast("错误: " + error, true);
                        Logger.logError(LOG_TAG, "SSH error: " + error);
                    }
                });
            }

            @Override
            public void onSSHConfigSaved(SSHConnectionConfig config) {
                showToast("SSH配置已保存", false);
            }

            @Override
            public void onSSHConfigDeleted(String configName) {
                showToast("SSH配置已删除: " + configName, false);
            }

            @Override
            public void onDialogClosed() {
                // 对话框关闭时的处理
            }
        });

        // 设置悬浮按钮点击监听器
        mSSHFloatingActionButton.setOnSSHFabClickListener(() -> {
            if (mSSHConfigDialog != null) {
                mSSHConfigDialog.show();
            }
        });

        Logger.logInfo(LOG_TAG, "SSH floating button initialized successfully");
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        if (getActivity() != null) {
            getActivity().registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
        }
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        if (getActivity() != null) {
            try {
                getActivity().unregisterReceiver(mTermuxActivityBroadcastReceiver);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadFragmentStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        // Storage permission request not available in MainTabActivity context
                        showToast("Permission request not available", false);
                        return;
                    default:
                }
            }
        }
    }

    private void reloadFragmentStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            if (mExtraKeysView != null && mTermuxTerminalExtraKeys != null && mTermuxTerminalExtraKeys.getExtraKeysInfo() != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        if (getView() != null) {
            // setMargins(getView()); // 简化版本不需要
            // setTerminalToolbarHeight(getView()); // 简化版本不需要
        }

        if (getActivity() != null) {
            FileReceiverActivity.updateFileReceiverActivityComponentsState(getActivity());
        }

        // Skip client styling reload in MainTabActivity context
        // if (mTermuxTerminalSessionActivityClient != null)
        //     mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        // if (mTermuxTerminalViewClient != null)
        //     mTermuxTerminalViewClient.onReloadActivityStyling();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity && getActivity() != null) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            getActivity().recreate();
        }
    }

    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        if (getActivity() != null) {
            return (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        }
        return null;
    }

    public ViewPager getTerminalToolbarViewPager(View view) {
        return (ViewPager) view.findViewById(R.id.terminal_toolbar_view_pager);
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        ViewPager pager = getTerminalToolbarViewPager(getView());
        return pager != null && pager.getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        ViewPager pager = getTerminalToolbarViewPager(getView());
        return pager != null && pager.getCurrentItem() == 1;
    }

    public void termuxSessionListNotifyUpdated() {
        if (mTermuxSessionListViewController != null) {
            mTermuxSessionListViewController.notifyDataSetChanged();
        }
    }

    public boolean isFragmentVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isFragmentRecreated() {
        return mIsFragmentRecreated;
    }

    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        Logger.logVerbose(LOG_TAG, "getCurrentSession called - mTerminalView: " + mTerminalView);

        if (mTerminalView != null) {
            TerminalSession session = mTerminalView.getCurrentSession();
            Logger.logVerbose(LOG_TAG, "getCurrentSession returning: " + session);
            return session;
        } else {
            Logger.logVerbose(LOG_TAG, "getCurrentSession returning null - mTerminalView is null");
            return null;
        }
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }

    /**
     * Fragment-compatible Terminal Toolbar Page Adapter.
     * This is a modified version of TerminalToolbarViewPager.PageAdapter that works with TermuxFragment.
     */
    private static class FragmentTerminalToolbarPageAdapter extends androidx.viewpager.widget.PagerAdapter {
        final TermuxFragment mFragment;
        String mSavedTextInput;

        public FragmentTerminalToolbarPageAdapter(TermuxFragment fragment, String savedTextInput) {
            this.mFragment = fragment;
            this.mSavedTextInput = savedTextInput;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            Logger.logInfo(LOG_TAG, "*** FragmentTerminalToolbarPageAdapter.instantiateItem called ***");
            Logger.logInfo(LOG_TAG, "Position: " + position + ", Collection: " + collection);
            Logger.logInfo(LOG_TAG, "Fragment context: " + mFragment.getContext());
            
            LayoutInflater inflater = LayoutInflater.from(mFragment.getContext());
            View layout;
            if (position == 0) {
                Logger.logInfo(LOG_TAG, "*** CREATING EXTRA KEYS VIEW (position 0) ***");
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                Logger.logInfo(LOG_TAG, "Layout inflated: " + layout);
                
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                Logger.logInfo(LOG_TAG, "ExtraKeysView cast: " + extraKeysView);

                // Set basic properties FIRST
                extraKeysView.setButtonTextAllCaps(mFragment.getProperties().shouldExtraKeysTextBeAllCaps());
                mFragment.setExtraKeysView(extraKeysView);

                // Create basic extra keys info with common keys and reload FIRST
                try {
                    String basicExtraKeys = "[[\"ESC\",\"TAB\",\"CTRL\",\"C\",\"UP\"],[\"ALT\",\"SHIFT\",\"/\",\"LEFT\",\"DOWN\",\"RIGHT\"]]";
                    com.termux.shared.termux.extrakeys.ExtraKeysInfo extraKeysInfo = new com.termux.shared.termux.extrakeys.ExtraKeysInfo(basicExtraKeys, "default", com.termux.shared.termux.extrakeys.ExtraKeysConstants.CONTROL_CHARS_ALIASES);
                    extraKeysView.reload(extraKeysInfo, mFragment.getTerminalToolbarDefaultHeight());
                    Logger.logInfo(LOG_TAG, "ExtraKeysView reloaded successfully");
                } catch (Exception e) {
                    Logger.logWarn(LOG_TAG, "Failed to create basic extra keys: " + e.getMessage());
                }

                // CRITICAL: Set our custom client AFTER reload to ensure it's not overridden
                Logger.logInfo(LOG_TAG, "*** SETTING CUSTOM CLIENT AFTER RELOAD ***");
                Logger.logInfo(LOG_TAG, "*** CREATING CUSTOM EXTRA KEYS CLIENT ***");
                
                // 创建我们的自定义客户端实例
                com.termux.shared.termux.extrakeys.ExtraKeysView.IExtraKeysView customClient = new com.termux.shared.termux.extrakeys.ExtraKeysView.IExtraKeysView() {
                    
                    // 构造函数中的测试日志
                    {
                        Logger.logInfo(LOG_TAG, "*** CUSTOM CLIENT CONSTRUCTOR CALLED - CLIENT INSTANCE CREATED ***");
                    }
                        
                        @Override
                        public void onExtraKeyButtonClick(View view, com.termux.shared.termux.extrakeys.ExtraKeyButton button, com.google.android.material.button.MaterialButton buttonView) {
                            Logger.logInfo(LOG_TAG, "*** CUSTOM CLIENT HANDLING KEY CLICK ***");
                            Logger.logInfo(LOG_TAG, "=== ExtraKey Button Click START ===");
                            
                            // 首先检查button和key是否为null
                            if (button == null) {
                                Logger.logError(LOG_TAG, "ExtraKeyButton is NULL - cannot process");
                                return;
                            }
                            
                            String key = button.getKey();
                            if (key == null) {
                                Logger.logError(LOG_TAG, "Key is NULL from button - cannot process");
                                return;
                            }
                            
                            Logger.logInfo(LOG_TAG, "Button: " + button + ", ButtonView: " + buttonView);
                            Logger.logInfo(LOG_TAG, "Raw key value: '" + key + "' (length=" + key.length() + ")");
                            
                            TerminalSession session = mFragment.getCurrentSession();
                            Logger.logInfo(LOG_TAG, "Terminal session status: " + (session != null ? (session.isRunning() ? "RUNNING" : "NOT_RUNNING") : "NULL"));
                            
                            // 从原生ExtraKeysView系统读取特殊按键状态
                            boolean ctrlActive = false;
                            boolean altActive = false; 
                            boolean shiftActive = false;
                            
                            // 获取ExtraKeysView实例来读取特殊按键状态
                            if (mFragment.getExtraKeysView() != null) {
                                try {
                                    // 读取CTRL状态
                                    Boolean ctrlState = mFragment.getExtraKeysView().readSpecialButton(
                                        com.termux.shared.termux.extrakeys.SpecialButton.CTRL, false);
                                    ctrlActive = (ctrlState != null && ctrlState);
                                    
                                    // 读取ALT状态  
                                    Boolean altState = mFragment.getExtraKeysView().readSpecialButton(
                                        com.termux.shared.termux.extrakeys.SpecialButton.ALT, false);
                                    altActive = (altState != null && altState);
                                    
                                    // 读取SHIFT状态
                                    Boolean shiftState = mFragment.getExtraKeysView().readSpecialButton(
                                        com.termux.shared.termux.extrakeys.SpecialButton.SHIFT, false);  
                                    shiftActive = (shiftState != null && shiftState);
                                    
                                } catch (Exception e) {
                                    Logger.logWarn(LOG_TAG, "Failed to read special button states: " + e.getMessage());
                                }
                            }
                            
                            Logger.logInfo(LOG_TAG, "Key pressed: '" + key + "' | Native modifier states -> Ctrl: " + ctrlActive + ", Alt: " + altActive + ", Shift: " + shiftActive);
                            
                            // 只有当session运行时才处理按键
                            if (session != null && session.isRunning()) {
                                // 使用正确的方式处理组合键和普通键
                                Logger.logInfo(LOG_TAG, "Processing key: '" + key + "'");
                                
                                // 特殊处理Ctrl+C组合键
                                if (ctrlActive && ("C".equals(key) || "c".equals(key))) {
                                    Logger.logInfo(LOG_TAG, "Detected Ctrl+C combination - sending interrupt signal");
                                    // 发送Ctrl+C中断信号 (ASCII 3)
                                    if (session != null) {
                                        byte[] interruptBytes = new byte[]{3}; // Ctrl+C = ASCII 3
                                        session.write(interruptBytes, 0, 1);
                                        Logger.logInfo(LOG_TAG, "Ctrl+C interrupt signal sent successfully");
                                    }
                                    // 重置修饰键状态 - 使用原生系统
                                    Logger.logInfo(LOG_TAG, "Resetting modifier states after Ctrl+C");
                                    resetNativeModifierStates();
                                } else if (shiftActive && "TAB".equals(key)) {
                                    // 特殊处理Shift+Tab组合键 - 发送到终端
                                    Logger.logInfo(LOG_TAG, "Detected Shift+Tab combination - sending to terminal");
                                    
                                    // Shift+Tab在终端中通常是反向Tab (backtab)
                                    // 发送ESC[Z序列，这是标准的反向Tab转义序列
                                    if (session != null) {
                                        byte[] backtabBytes = "\u001b[Z".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                        session.write(backtabBytes, 0, backtabBytes.length);
                                        Logger.logInfo(LOG_TAG, "Shift+Tab (backtab) sequence sent to terminal");
                                    }
                                    
                                    // 重置修饰键状态 - 使用原生系统
                                    Logger.logInfo(LOG_TAG, "Resetting modifier states after Shift+Tab");
                                    resetNativeModifierStates();
                                } else if (ctrlActive && "/".equals(key)) {
                                    // 特殊处理Ctrl+/ 组合键 - 打开Claude Code菜单  
                                    Logger.logInfo(LOG_TAG, "Detected Ctrl+/ combination - opening Claude Code menu");
                                    // 通过Fragment的方法来访问Claude Code菜单
                                    mFragment.openClaudeCodeMenuFromExtraKeys();
                                    // 重置修饰键状态 - 使用原生系统
                                    Logger.logInfo(LOG_TAG, "Resetting modifier states after Ctrl+/");
                                    resetNativeModifierStates();
                                } else if (ctrlActive || altActive || shiftActive) {
                                    // 其他组合键或修饰键+普通键处理
                                    handleKeyInput(key, ctrlActive, altActive, shiftActive, session);
                                    Logger.logInfo(LOG_TAG, "Resetting modifier states after modifier key combination");
                                    resetNativeModifierStates();
                                } else {
                                    // 普通按键处理，无需重置修饰键
                                    handleKeyInput(key, false, false, false, session);
                                }
                            } else {
                                Logger.logWarn(LOG_TAG, "Cannot process key - terminal session is not available or not running");
                            }
                            Logger.logInfo(LOG_TAG, "=== ExtraKey Button Click END ===");
                        }
                        
                        private void updateButtonState(com.google.android.material.button.MaterialButton button, boolean pressed) {
                            Logger.logInfo(LOG_TAG, "*** updateButtonState START ***");
                            Logger.logInfo(LOG_TAG, "Updating button state: " + (button != null ? button.getText() : "null") + " -> " + (pressed ? "PRESSED" : "RELEASED"));
                            if (button != null) {
                                button.setSelected(pressed);
                                if (pressed) {
                                    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // 绿色表示激活
                                    Logger.logInfo(LOG_TAG, "Button visual state set to ACTIVE (green)");
                                } else {
                                    button.setBackgroundTintList(null); // 恢复默认颜色
                                    Logger.logInfo(LOG_TAG, "Button visual state set to DEFAULT");
                                }
                                Logger.logInfo(LOG_TAG, "Button state update completed successfully");
                            } else {
                                Logger.logWarn(LOG_TAG, "Cannot update button state - button is null");
                            }
                            Logger.logInfo(LOG_TAG, "*** updateButtonState END ***");
                        }
                        
                        private void handleKeyInput(String key, boolean ctrlDown, boolean altDown, boolean shiftDown, TerminalSession session) {
                            Logger.logInfo(LOG_TAG, ">>> HANDLE KEY INPUT START <<<");
                            Logger.logInfo(LOG_TAG, "Input key: '" + key + "' | Modifiers -> Ctrl: " + ctrlDown + ", Alt: " + altDown + ", Shift: " + shiftDown);
                            
                            // 通过Fragment引用获取TerminalView
                            TerminalView terminalView = mFragment.getTerminalView();
                            Logger.logInfo(LOG_TAG, "TerminalView availability: " + (terminalView != null ? "AVAILABLE" : "NULL"));
                            
                            // 首先检查是否是特殊按键，使用KeyEvent处理
                            Integer keyCode = getKeyCodeForString(key);
                            Logger.logInfo(LOG_TAG, "KeyCode lookup for '" + key + "': " + (keyCode != null ? keyCode + " (SPECIAL_KEY)" : "null (REGULAR_CHAR)"));
                            
                            if (keyCode != null) {
                                // 特殊按键处理路径
                                int metaState = 0;
                                if (ctrlDown) {
                                    metaState |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
                                    Logger.logInfo(LOG_TAG, "Added CTRL to metaState");
                                }
                                if (altDown) {
                                    metaState |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
                                    Logger.logInfo(LOG_TAG, "Added ALT to metaState");
                                }
                                if (shiftDown) {
                                    metaState |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
                                    Logger.logInfo(LOG_TAG, "Added SHIFT to metaState");
                                }

                                Logger.logInfo(LOG_TAG, "Final KeyEvent parameters -> keyCode: " + keyCode + ", metaState: " + metaState + " (binary: " + Integer.toBinaryString(metaState) + ")");

                                KeyEvent keyEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState);
                                if (terminalView != null) {
                                    Logger.logInfo(LOG_TAG, "Calling terminalView.onKeyDown() with KeyEvent...");
                                    boolean result = terminalView.onKeyDown(keyCode, keyEvent);
                                    Logger.logInfo(LOG_TAG, "TerminalView.onKeyDown() returned: " + result);
                                } else {
                                    Logger.logError(LOG_TAG, "TerminalView is null - cannot send KeyEvent");
                                }
                            } else {
                                // 普通字符处理路径
                                Logger.logInfo(LOG_TAG, "Processing as regular character input");
                                if (terminalView != null && key.length() > 0) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        Logger.logInfo(LOG_TAG, "Using codePoints method (Android N+)");
                                        key.codePoints().forEach(codePoint -> {
                                            Logger.logInfo(LOG_TAG, "Sending codePoint: " + codePoint + " (char: '" + (char)codePoint + "') with ctrl:" + ctrlDown + ", alt:" + altDown);
                                            terminalView.inputCodePoint(com.termux.view.TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, codePoint, ctrlDown, altDown);
                                        });
                                        Logger.logInfo(LOG_TAG, "Finished sending codePoints for: '" + key + "'");
                                    } else {
                                        // 旧版本Android fallback
                                        Logger.logInfo(LOG_TAG, "Using session.write() fallback for Android < N");
                                        if (session != null) {
                                            session.write(key);
                                            Logger.logInfo(LOG_TAG, "Wrote to session: '" + key + "'");
                                        } else {
                                            Logger.logError(LOG_TAG, "Session is null - cannot write");
                                        }
                                    }
                                } else {
                                    Logger.logError(LOG_TAG, "Cannot process character - terminalView is null or key is empty");
                                }
                            }
                            Logger.logInfo(LOG_TAG, "<<< HANDLE KEY INPUT END <<<");
                        }
                        
                        private Integer getKeyCodeForString(String key) {
                            // 映射特殊按键到KeyCode - 移除C和c的映射，让它们作为普通字符处理
                            Integer keyCode;
                            switch (key) {
                                case "TAB": keyCode = KeyEvent.KEYCODE_TAB; break;
                                case "UP": keyCode = KeyEvent.KEYCODE_DPAD_UP; break;
                                case "DOWN": keyCode = KeyEvent.KEYCODE_DPAD_DOWN; break;
                                case "LEFT": keyCode = KeyEvent.KEYCODE_DPAD_LEFT; break;
                                case "RIGHT": keyCode = KeyEvent.KEYCODE_DPAD_RIGHT; break;
                                case "ESC": keyCode = KeyEvent.KEYCODE_ESCAPE; break;
                                case " ": keyCode = KeyEvent.KEYCODE_SPACE; break;
                                // 移除了 C 和 c 的映射，让Ctrl+C通过上面的特殊处理逻辑
                                default: keyCode = null; break;
                            }
                            Logger.logInfo(LOG_TAG, "KeyCode mapping: '" + key + "' -> " + (keyCode != null ? keyCode + " (" + getKeyCodeName(keyCode) + ")" : "null (regular character)"));
                            return keyCode;
                        }
                        
                        private String getKeyCodeName(int keyCode) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_TAB: return "TAB";
                                case KeyEvent.KEYCODE_DPAD_UP: return "UP";
                                case KeyEvent.KEYCODE_DPAD_DOWN: return "DOWN";
                                case KeyEvent.KEYCODE_DPAD_LEFT: return "LEFT";
                                case KeyEvent.KEYCODE_DPAD_RIGHT: return "RIGHT";
                                case KeyEvent.KEYCODE_ESCAPE: return "ESC";
                                case KeyEvent.KEYCODE_SPACE: return "SPACE";
                                // 移除了 KEYCODE_C 的处理
                                default: return "UNKNOWN_" + keyCode;
                            }
                        }
                        
                        private void resetNativeModifierStates() {
                            Logger.logInfo(LOG_TAG, "*** RESETTING NATIVE MODIFIER STATES ***");
                            
                            if (mFragment.getExtraKeysView() != null) {
                                try {
                                    // 获取特殊按键状态并重置为非激活状态
                                    java.util.Map<com.termux.shared.termux.extrakeys.SpecialButton, com.termux.shared.termux.extrakeys.SpecialButtonState> specialButtons = 
                                        mFragment.getExtraKeysView().getSpecialButtons();
                                    
                                    if (specialButtons != null) {
                                        // 重置CTRL状态
                                        com.termux.shared.termux.extrakeys.SpecialButtonState ctrlState = 
                                            specialButtons.get(com.termux.shared.termux.extrakeys.SpecialButton.CTRL);
                                        if (ctrlState != null) {
                                            ctrlState.setIsActive(false);
                                            ctrlState.setIsLocked(false);
                                            Logger.logInfo(LOG_TAG, "CTRL state reset to inactive");
                                        }
                                        
                                        // 重置ALT状态
                                        com.termux.shared.termux.extrakeys.SpecialButtonState altState = 
                                            specialButtons.get(com.termux.shared.termux.extrakeys.SpecialButton.ALT);
                                        if (altState != null) {
                                            altState.setIsActive(false);
                                            altState.setIsLocked(false);
                                            Logger.logInfo(LOG_TAG, "ALT state reset to inactive");
                                        }
                                        
                                        // 重置SHIFT状态
                                        com.termux.shared.termux.extrakeys.SpecialButtonState shiftState = 
                                            specialButtons.get(com.termux.shared.termux.extrakeys.SpecialButton.SHIFT);
                                        if (shiftState != null) {
                                            shiftState.setIsActive(false);
                                            shiftState.setIsLocked(false);
                                            Logger.logInfo(LOG_TAG, "SHIFT state reset to inactive");
                                        }
                                        
                                        Logger.logInfo(LOG_TAG, "All native modifier states reset successfully");
                                    } else {
                                        Logger.logWarn(LOG_TAG, "Could not get special buttons map for reset");
                                    }
                                } catch (Exception e) {
                                    Logger.logError(LOG_TAG, "Failed to reset native modifier states: " + e.getMessage());
                                }
                            } else {
                                Logger.logError(LOG_TAG, "ExtraKeysView is null - cannot reset native states");
                            }
                            
                            Logger.logInfo(LOG_TAG, "*** NATIVE MODIFIER STATES RESET COMPLETE ***");
                        }

                        @Override
                        public boolean performExtraKeyButtonHapticFeedback(View view, com.termux.shared.termux.extrakeys.ExtraKeyButton button, com.google.android.material.button.MaterialButton buttonView) {
                            Logger.logInfo(LOG_TAG, "*** HAPTIC FEEDBACK METHOD CALLED ***");
                            Logger.logInfo(LOG_TAG, "=== performExtraKeyButtonHapticFeedback START ===");
                            
                            // 获取按键信息
                            if (button == null) {
                                Logger.logError(LOG_TAG, "Button is NULL in haptic feedback");
                                return false;
                            }
                            
                            String key = button.getKey();
                            if (key == null) {
                                Logger.logError(LOG_TAG, "Key is NULL in haptic feedback");
                                return false;
                            }
                            
                            Logger.logInfo(LOG_TAG, "Haptic feedback for key: '" + key + "'");
                            
                            // 对于特殊按键(CTRL, ALT, SHIFT)，我们不在这里处理状态
                            // 让原生的ExtraKeysView系统处理状态，我们只记录状态变化用于组合键检测
                            if ("CTRL".equals(key) || "ALT".equals(key) || "SHIFT".equals(key)) {
                                Logger.logInfo(LOG_TAG, "Special button haptic feedback: '" + key + "' - letting native system handle state");
                            }
                            
                            Logger.logInfo(LOG_TAG, "=== performExtraKeyButtonHapticFeedback END ===");
                            return false; // Let system handle haptic feedback
                        }
                };
                
                // 设置自定义客户端并确认
                Logger.logInfo(LOG_TAG, "*** SETTING CUSTOM CLIENT ON EXTRA KEYS VIEW ***");
                extraKeysView.setExtraKeysViewClient(customClient);
                Logger.logInfo(LOG_TAG, "*** CUSTOM CLIENT SET SUCCESSFULLY ***");
                Logger.logInfo(LOG_TAG, "*** Custom client instance: " + customClient + " ***");
                
                // 验证客户端是否正确设置 - 使用反射检查私有字段
                try {
                    java.lang.reflect.Field clientField = extraKeysView.getClass().getDeclaredField("mExtraKeysViewClient");
                    clientField.setAccessible(true);
                    Object actualClient = clientField.get(extraKeysView);
                    Logger.logInfo(LOG_TAG, "*** VERIFICATION: Actual client in ExtraKeysView: " + actualClient + " ***");
                    Logger.logInfo(LOG_TAG, "*** VERIFICATION: Client match: " + (actualClient == customClient) + " ***");
                } catch (Exception e) {
                    Logger.logWarn(LOG_TAG, "Failed to verify client via reflection: " + e.getMessage());
                }

            } else {
                Logger.logInfo(LOG_TAG, "*** CREATING TEXT INPUT VIEW (position 1) ***");
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);

                if (mSavedTextInput != null) {
                    editText.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    TerminalSession session = mFragment.getCurrentSession();
                    if (session != null) {
                        if (session.isRunning()) {
                            String textToSend = editText.getText().toString();
                            if (textToSend.length() == 0) textToSend = "\r";
                            session.write(textToSend);
                        }
                        editText.setText("");
                    }
                    return true;
                });
            }
            collection.addView(layout);
            Logger.logInfo(LOG_TAG, "*** instantiateItem COMPLETED - Layout added to collection ***");
            Logger.logInfo(LOG_TAG, "Final layout: " + layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }
    }

    /**
     * Fragment-compatible Terminal Toolbar Page Change Listener.
     * This is a modified version of TerminalToolbarViewPager.OnPageChangeListener that works with TermuxFragment.
     */
    private static class FragmentTerminalToolbarOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        final TermuxFragment mFragment;
        final ViewPager mTerminalToolbarViewPager;

        public FragmentTerminalToolbarOnPageChangeListener(TermuxFragment fragment, ViewPager viewPager) {
            this.mFragment = fragment;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                if (mFragment.getTerminalView() != null) {
                    mFragment.getTerminalView().requestFocus();
                }
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }
    }
}

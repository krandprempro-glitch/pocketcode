package com.termux.app.terminal;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;

/**
 * Helper class for Claude Code command menu functionality
 */
public class ClaudeCodeMenuHelper {

    private PopupWindow mPopupWindow;
    private TermuxActivity mActivity;

    public ClaudeCodeMenuHelper(TermuxActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Show Claude Code menu popup
     */
    public void showMenu(View anchorView) {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View menuView = inflater.inflate(R.layout.claude_code_menu, null);

        setupMenuButtons(menuView);

        int width = (int) (320 * mActivity.getResources().getDisplayMetrics().density);
        
        mPopupWindow = new PopupWindow(
            menuView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );

        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);

        // Measure the menu view to get its height
        menuView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int menuHeight = menuView.getMeasuredHeight();

        // Show popup above the anchor view
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        
        int xOffset = -width + anchorView.getWidth();
        int yOffset = -menuHeight - anchorView.getHeight() - 20;
        
        mPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, 
            location[0] + xOffset, location[1] + yOffset);
    }

    private void setupMenuButtons(View menuView) {
        EditText commandInput = mActivity.findViewById(R.id.terminal_command_input);

        // Claude Code 指令
        setupButton(menuView, R.id.cmd_help, "/help", commandInput);
        setupButton(menuView, R.id.cmd_clear, "/clear", commandInput);
        setupButton(menuView, R.id.cmd_compact, "/compact", commandInput);
        setupButton(menuView, R.id.cmd_add_on_dir, "/add-dir ", commandInput);
        setupButton(menuView, R.id.cmd_shell, "/shell ", commandInput);
        setupButton(menuView, R.id.cmd_edit, "/edit ", commandInput);
    }

    private void setupButton(View menuView, int buttonId, String command, EditText commandInput) {
        Button button = menuView.findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(v -> {
                commandInput.setText(command);
                
                // For commands ending with space, position cursor at end
                // For complete commands, select all for easy replacement
                if (command.endsWith(" ")) {
                    commandInput.setSelection(command.length());
                } else if (command.contains("\"\"")) {
                    // For git commit, position cursor inside quotes
                    int quotePos = command.lastIndexOf("\"\"");
                    commandInput.setSelection(quotePos + 1);
                } else {
                    commandInput.selectAll();
                }
                
                commandInput.requestFocus();
                mPopupWindow.dismiss();
            });
        }
    }

    /**
     * Dismiss the popup if it's showing
     */
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * Check if popup is currently showing
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }
}

package com.termux.app.terminal;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.managers.ClaudeCodeCommandManager;
import com.termux.terminal.TerminalSession;

import java.util.Arrays;
import java.util.List;

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
     * Command configuration class
     */
    public static class Command {
        public final String command;
        public final String description;

        public Command(String command, String description) {
            this.command = command;
            this.description = description;
        }
    }

    /**
     * RecyclerView Adapter for commands
     */
    private class CommandAdapter extends RecyclerView.Adapter<CommandAdapter.CommandViewHolder> {
        private final List<Command> commands;
        private final EditText commandInput;

        public CommandAdapter(List<Command> commands, EditText commandInput) {
            this.commands = commands;
            this.commandInput = commandInput;
        }

        @Override
        public CommandViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_command, parent, false);
            return new CommandViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CommandViewHolder holder, int position) {
            Command command = commands.get(position);
            holder.cmdButton.setText(command.command);
            holder.cmdDescription.setText(command.description);
            
            holder.cmdButton.setOnClickListener(v -> {
                commandInput.setText(command.command);
                
                // For commands ending with space, position cursor at end
                // For complete commands, select all for easy replacement
                if (command.command.endsWith(" ")) {
                    commandInput.setSelection(command.command.length());
                } else {
                    commandInput.selectAll();
                }
                
                commandInput.requestFocus();
                mPopupWindow.dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return commands.size();
        }

        class CommandViewHolder extends RecyclerView.ViewHolder {
            Button cmdButton;
            TextView cmdDescription;

            public CommandViewHolder(View itemView) {
                super(itemView);
                cmdButton = itemView.findViewById(R.id.cmd_button);
                cmdDescription = itemView.findViewById(R.id.cmd_description);
            }
        }
    }

    /**
     * Default command configuration - delegates to ClaudeCodeCommandManager
     */
    private List<Command> getDefaultCommands() {
        return ClaudeCodeCommandManager.getInstance().getDefaultCommands();
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
        RecyclerView recyclerView = menuView.findViewById(R.id.commands_recycler_view);
        
        if (recyclerView != null) {
            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
            CommandAdapter adapter = new CommandAdapter(getDefaultCommands(), commandInput);
            recyclerView.setAdapter(adapter);
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

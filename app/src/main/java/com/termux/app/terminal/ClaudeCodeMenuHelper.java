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
import com.termux.app.models.SSHConfigManager;
import com.termux.app.models.SSHConnectionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Claude Code command menu functionality
 */
public class ClaudeCodeMenuHelper {

    public static final int VIEW_TYPE_COMMAND = 0;
    public static final int VIEW_TYPE_SSH_CONNECTION = 1;

    private PopupWindow mPopupWindow;
    private TermuxActivity mActivity;

    public ClaudeCodeMenuHelper(TermuxActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Base class for menu items
     */
    public static abstract class MenuItem {
        public abstract int getViewType();
    }

    /**
     * Command configuration class
     */
    public static class Command extends MenuItem {
        public final String command;
        public final String description;
        public final String sshDisplayName; // SSH连接显示名称（如"home"）

        public Command(String command, String description) {
            this(command, description, null);
        }

        public Command(String command, String description, String sshDisplayName) {
            this.command = command;
            this.description = description;
            this.sshDisplayName = sshDisplayName;
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_COMMAND;
        }
    }

    /**
     * SSH Connection item class
     */
    public static class SSHConnection extends MenuItem {
        public final SSHConnectionConfig config;

        public SSHConnection(SSHConnectionConfig config) {
            this.config = config;
        }

        @Override
        public int getViewType() {
            return VIEW_TYPE_SSH_CONNECTION;
        }
    }

    /**
     * RecyclerView Adapter for commands and SSH connections
     */
    private class CommandAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<MenuItem> items;
        private final EditText commandInput;

        public CommandAdapter(List<MenuItem> items, EditText commandInput) {
            this.items = items;
            this.commandInput = commandInput;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getViewType();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SSH_CONNECTION) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_ssh_connection, parent, false);
                return new SSHConnectionViewHolder(itemView);
            } else {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_command, parent, false);
                return new CommandViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MenuItem item = items.get(position);

            if (item instanceof SSHConnection) {
                SSHConnectionViewHolder sshHolder = (SSHConnectionViewHolder) holder;
                SSHConnection sshItem = (SSHConnection) item;
                SSHConnectionConfig config = sshItem.config;

                sshHolder.sshConfigName.setText(config.getName());
                sshHolder.sshUserHost.setText(config.getUsername() + "@" + config.getHost() + ":" + config.getPort());

                sshHolder.itemView.setOnClickListener(v -> {
                    // Generate and execute SSH command
                    String sshCommand = config.generateSSHCommand();
                    if (sshCommand != null) {
                        commandInput.setText(sshCommand);
                        commandInput.setSelection(sshCommand.length());
                        commandInput.requestFocus();
                        mPopupWindow.dismiss();
                    }
                });
            } else if (item instanceof Command) {
                CommandViewHolder cmdHolder = (CommandViewHolder) holder;
                Command command = (Command) item;
                cmdHolder.cmdButton.setText(command.command);
                cmdHolder.cmdDescription.setText(command.description);

                cmdHolder.cmdButton.setOnClickListener(v -> {
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
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * ViewHolder for command items
         */
        class CommandViewHolder extends RecyclerView.ViewHolder {
            Button cmdButton;
            TextView cmdDescription;

            public CommandViewHolder(View itemView) {
                super(itemView);
                cmdButton = itemView.findViewById(R.id.cmd_button);
                cmdDescription = itemView.findViewById(R.id.cmd_description);
            }
        }

        /**
         * ViewHolder for SSH connection items
         */
        class SSHConnectionViewHolder extends RecyclerView.ViewHolder {
            TextView sshConfigName;
            TextView sshUserHost;

            public SSHConnectionViewHolder(View itemView) {
                super(itemView);
                sshConfigName = itemView.findViewById(R.id.ssh_config_name);
                sshUserHost = itemView.findViewById(R.id.ssh_user_host);
            }
        }
    }

    /**
     * Get menu items - commands and SSH connections combined
     */
    private List<MenuItem> getMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        // Add commands
        for (Command cmd : ClaudeCodeCommandManager.getInstance().getDefaultCommands()) {
            items.add(cmd);
        }

        // Add SSH connections section header if we have connections
        List<SSHConnectionConfig> sshConfigs = SSHConfigManager.getInstance(mActivity).getAllConfigs();
        if (!sshConfigs.isEmpty()) {
            // Add SSH connections
            for (SSHConnectionConfig config : sshConfigs) {
                items.add(new SSHConnection(config));
            }
        }

        return items;
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
            CommandAdapter adapter = new CommandAdapter(getMenuItems(), commandInput);
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

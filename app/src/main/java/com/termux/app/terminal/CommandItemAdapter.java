package com.termux.app.terminal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import java.util.List;

/**
 * 单个指令项适配器 - 支持两种布局:
 * - item_command_improved: 单行命令左+描述右 (普通指令)
 * - item_ssh_connection: 两行卡片config名+用户@IP (SSH连接)
 */
public class CommandItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_SSH = 1;

    private List<ClaudeCodeMenuHelper.Command> commands;
    private final EditText commandInput;
    private final CommandGroupAdapter.OnCommandClickListener commandClickListener;

    public CommandItemAdapter(List<ClaudeCodeMenuHelper.Command> commands, EditText commandInput,
                             CommandGroupAdapter.OnCommandClickListener listener) {
        this.commands = commands;
        this.commandInput = commandInput;
        this.commandClickListener = listener;
        android.util.Log.d("CommandItemAdapter", "Constructor: commands=" + (commands != null ? commands.size() : "null"));
    }

    public void updateCommands(List<ClaudeCodeMenuHelper.Command> newCommands) {
        android.util.Log.d("CommandItemAdapter", "updateCommands: " + (newCommands != null ? newCommands.size() : "null"));
        this.commands = newCommands;
        notifyDataSetChanged();
    }

    private boolean isSSHCommand(ClaudeCodeMenuHelper.Command command) {
        // SSH连接有sshDisplayName字段，或者description是user@host格式
        if (command.sshDisplayName != null && !command.sshDisplayName.isEmpty()) {
            return true;
        }
        String desc = command.description != null ? command.description : "";
        return desc.contains("@");
    }

    @Override
    public int getItemViewType(int position) {
        return isSSHCommand(commands.get(position)) ? VIEW_TYPE_SSH : VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SSH) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ssh_connection, parent, false);
            return new SSHViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_command_improved, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ClaudeCodeMenuHelper.Command command = commands.get(position);
        if (holder instanceof SSHViewHolder) {
            ((SSHViewHolder) holder).bind(command);
        } else {
            ((NormalViewHolder) holder).bind(command);
        }
    }

    @Override
    public int getItemCount() {
        android.util.Log.d("CommandItemAdapter", "getItemCount: " + (commands != null ? commands.size() : "null"));
        return commands != null ? commands.size() : 0;
    }

    // ---- Normal (single-line) ViewHolder ----
    class NormalViewHolder extends RecyclerView.ViewHolder {
        private final TextView cmdText;
        private final TextView cmdDescription;

        public NormalViewHolder(@NonNull View itemView) {
            super(itemView);
            cmdText = itemView.findViewById(R.id.cmd_text);
            cmdDescription = itemView.findViewById(R.id.cmd_description);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    ClaudeCodeMenuHelper.Command command = commands.get(pos);
                    handleClick(command);
                }
            });
        }

        public void bind(ClaudeCodeMenuHelper.Command command) {
            cmdText.setText(command.command);
            cmdDescription.setText(command.description);
        }

        private void handleClick(ClaudeCodeMenuHelper.Command command) {
            if (commandInput != null) {
                commandInput.setText(command.command);
                if (command.command.endsWith(" ")) {
                    commandInput.setSelection(command.command.length());
                } else {
                    commandInput.selectAll();
                }
                commandInput.requestFocus();
            }
            if (commandClickListener != null) {
                commandClickListener.onCommandClick(command);
            }
        }
    }

    // ---- SSH (two-line) ViewHolder ----
    class SSHViewHolder extends RecyclerView.ViewHolder {
        private final TextView sshConfigName;
        private final TextView sshUserHost;

        public SSHViewHolder(@NonNull View itemView) {
            super(itemView);
            sshConfigName = itemView.findViewById(R.id.ssh_config_name);
            sshUserHost = itemView.findViewById(R.id.ssh_user_host);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    ClaudeCodeMenuHelper.Command command = commands.get(pos);
                    handleClick(command);
                }
            });
        }

        public void bind(ClaudeCodeMenuHelper.Command command) {
            // command.command = the SSH command
            // command.description = user@host
            // command.sshDisplayName = SSH config name (e.g., "home")
            sshUserHost.setText(command.description);
            String displayName = command.sshDisplayName;
            if (displayName == null || displayName.isEmpty()) {
                displayName = "SSH连接";
            }
            sshConfigName.setText(displayName);
        }

        private void handleClick(ClaudeCodeMenuHelper.Command command) {
            if (commandInput != null) {
                commandInput.setText(command.command);
                if (command.command.endsWith(" ")) {
                    commandInput.setSelection(command.command.length());
                } else {
                    commandInput.selectAll();
                }
                commandInput.requestFocus();
            }
            if (commandClickListener != null) {
                commandClickListener.onCommandClick(command);
            }
        }
    }
}

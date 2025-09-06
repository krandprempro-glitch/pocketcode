package com.termux.app.terminal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import java.util.List;

/**
 * 单个指令项适配器 - 使用Material Card设计
 */
public class CommandItemAdapter extends RecyclerView.Adapter<CommandItemAdapter.CommandViewHolder> {
    
    private List<ClaudeCodeMenuHelper.Command> commands;
    private final EditText commandInput;
    private final CommandGroupAdapter.OnCommandClickListener commandClickListener;
    
    public CommandItemAdapter(List<ClaudeCodeMenuHelper.Command> commands, EditText commandInput,
                             CommandGroupAdapter.OnCommandClickListener listener) {
        this.commands = commands;
        this.commandInput = commandInput;
        this.commandClickListener = listener;
    }
    
    public void updateCommands(List<ClaudeCodeMenuHelper.Command> newCommands) {
        this.commands = newCommands;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command_improved, parent, false);
        return new CommandViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {
        ClaudeCodeMenuHelper.Command command = commands.get(position);
        holder.bind(command);
    }
    
    @Override
    public int getItemCount() {
        return commands.size();
    }
    
    class CommandViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cmdIcon;
        private final TextView cmdText;
        private final TextView cmdDescription;
        private final ImageView cmdArrow;
        
        public CommandViewHolder(@NonNull View itemView) {
            super(itemView);
            cmdIcon = itemView.findViewById(R.id.cmd_icon);
            cmdText = itemView.findViewById(R.id.cmd_text);
            cmdDescription = itemView.findViewById(R.id.cmd_description);
            cmdArrow = itemView.findViewById(R.id.cmd_arrow);
            
            // 设置点击监听器
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ClaudeCodeMenuHelper.Command command = commands.get(position);
                    handleCommandClick(command);
                    if (commandClickListener != null) {
                        commandClickListener.onCommandClick(command);
                    }
                }
            });
        }
        
        public void bind(ClaudeCodeMenuHelper.Command command) {
            cmdText.setText(command.command);
            cmdDescription.setText(command.description);
            
            // 根据指令类型设置不同的图标
            int iconRes = getCommandIcon(command.command);
            if (iconRes != 0) {
                cmdIcon.setVisibility(View.VISIBLE);
                cmdIcon.setImageResource(iconRes);
            } else {
                cmdIcon.setVisibility(View.GONE);
            }
        }
        
        private void handleCommandClick(ClaudeCodeMenuHelper.Command command) {
            if (commandInput != null) {
                commandInput.setText(command.command);
                
                // 如果命令以空格结尾，光标移到末尾；否则全选
                if (command.command.endsWith(" ")) {
                    commandInput.setSelection(command.command.length());
                } else {
                    commandInput.selectAll();
                }
                
                commandInput.requestFocus();
            }
        }
        
        private int getCommandIcon(String command) {
            // 根据指令类型返回对应图标
            if (command.startsWith("/")) {
                if (command.contains("help")) return R.drawable.ic_info;
                if (command.contains("config")) return R.drawable.ic_settings_small;
                if (command.contains("clear")) return R.drawable.ic_delete;
                return R.drawable.ic_code;
            } else if (command.startsWith("ssh://") || command.contains("@")) {
                return R.drawable.ic_ssh;
            } else if (command.contains("think")) {
                return R.drawable.ic_ai_group;
            }
            return 0; // 无图标
        }
    }
}
package com.termux.app.terminal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import java.util.List;

/**
 * 分组命令适配器 - 支持三大类快捷指令的分层显示
 */
public class CommandGroupAdapter extends RecyclerView.Adapter<CommandGroupAdapter.GroupViewHolder> {
    
    public enum CommandCategory {
        BOOKMARKS("收藏路径", R.drawable.ic_bookmark),
        SSH_CONNECTIONS("SSH连接", R.drawable.ic_ssh_group),
        QUICK_COMMANDS("常用指令", R.drawable.ic_terminal),  // 新增常用指令分组
        AI_COMMANDS("AI指令", R.drawable.ic_ai_group),
        AI_CUSTOM_COMMANDS("自定义AI指令", R.drawable.ic_ai_group),
        SYSTEM_COMMANDS("系统指令", R.drawable.ic_system_group);

        private final String title;
        private final int iconRes;

        CommandCategory(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }

        public String getTitle() { return title; }
        public int getIconRes() { return iconRes; }
    }
    
    public static class CommandGroup {
        private final CommandCategory category;
        private final List<ClaudeCodeMenuHelper.Command> commands;
        private boolean isExpanded;

        public CommandGroup(CommandCategory category, List<ClaudeCodeMenuHelper.Command> commands) {
            this(category, commands, false);
        }

        public CommandGroup(CommandCategory category, List<ClaudeCodeMenuHelper.Command> commands, boolean isExpanded) {
            this.category = category;
            this.commands = commands;
            this.isExpanded = isExpanded;
        }
        
        // Getters
        public CommandCategory getCategory() { return category; }
        public List<ClaudeCodeMenuHelper.Command> getCommands() { return commands; }
        public boolean isExpanded() { return isExpanded; }
        public void setExpanded(boolean expanded) { this.isExpanded = expanded; }
    }
    
    private final List<CommandGroup> commandGroups;
    private final EditText commandInput;
    private final OnCommandClickListener commandClickListener;
    
    public interface OnCommandClickListener {
        void onCommandClick(ClaudeCodeMenuHelper.Command command);
    }
    
    public CommandGroupAdapter(List<CommandGroup> commandGroups, EditText commandInput,
                              OnCommandClickListener listener) {
        this.commandGroups = commandGroups;
        this.commandInput = commandInput;
        this.commandClickListener = listener;
    }

    /** Replaces the group list and refreshes the displayed data. */
    public void updateGroups(List<CommandGroup> newGroups) {
        this.commandGroups.clear();
        this.commandGroups.addAll(newGroups);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command_group, parent, false);
        return new GroupViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        CommandGroup group = commandGroups.get(position);
        holder.bind(group);
    }
    
    @Override
    public int getItemCount() {
        return commandGroups.size();
    }
    
    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final ImageView groupIcon;
        private final TextView groupTitle;
        private final TextView groupCount;
        private final ImageView expandArrow;
        private final RecyclerView groupRecyclerView;
        private CommandItemAdapter commandAdapter;
        
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupIcon = itemView.findViewById(R.id.group_icon);
            groupTitle = itemView.findViewById(R.id.group_title);
            groupCount = itemView.findViewById(R.id.group_count);
            expandArrow = itemView.findViewById(R.id.expand_arrow);
            groupRecyclerView = itemView.findViewById(R.id.group_recycler_view);
            
            // 设置点击监听器
            itemView.findViewById(R.id.group_header).setOnClickListener(v -> toggleExpanded());
        }
        
        public void bind(CommandGroup group) {
            // 设置分组信息
            groupIcon.setImageResource(group.getCategory().getIconRes());
            groupTitle.setText(group.getCategory().getTitle());

            // 计算实际命令数量（过滤掉空命令的提示项）
            int actualCommandCount = 0;
            for (ClaudeCodeMenuHelper.Command cmd : group.getCommands()) {
                if (cmd.command != null && !cmd.command.isEmpty()) {
                    actualCommandCount++;
                }
            }
            groupCount.setText(String.valueOf(actualCommandCount));

            // 设置展开状态
            updateExpandedState(group);

            // 设置子项RecyclerView
            if (commandAdapter == null) {
                commandAdapter = new CommandItemAdapter(group.getCommands(), commandInput, commandClickListener);
                groupRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                groupRecyclerView.setAdapter(commandAdapter);
                groupRecyclerView.setNestedScrollingEnabled(false);
            } else {
                commandAdapter.updateCommands(group.getCommands());
            }
        }
        
        private void toggleExpanded() {
            CommandGroup group = commandGroups.get(getAdapterPosition());
            group.setExpanded(!group.isExpanded());
            updateExpandedState(group);
        }
        
        private void updateExpandedState(CommandGroup group) {
            boolean isExpanded = group.isExpanded();
            groupRecyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            expandArrow.setImageResource(isExpanded ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right);
        }
    }
}

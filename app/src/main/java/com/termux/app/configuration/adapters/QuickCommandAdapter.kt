package com.termux.app.configuration.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.termux.R
import com.termux.app.configuration.models.QuickCommand
import java.text.SimpleDateFormat
import java.util.*

/**
 * 常用指令列表适配器
 * 用于显示常用指令列表
 */
class QuickCommandAdapter(private val context: Context) :
    RecyclerView.Adapter<QuickCommandAdapter.QuickCommandViewHolder>() {

    private var commands: List<QuickCommand> = emptyList()
    private var actionListener: OnCommandActionListener? = null
    private val dateFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    interface OnCommandActionListener {
        fun onExecuteCommand(command: QuickCommand)
        fun onEditCommand(command: QuickCommand)
        fun onDeleteCommand(command: QuickCommand)
        fun onDuplicateCommand(command: QuickCommand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickCommandViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_quick_command, parent, false)
        return QuickCommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickCommandViewHolder, position: Int) {
        val command = commands[position]
        holder.bind(command)
    }

    override fun getItemCount(): Int = commands.size

    fun setCommands(commands: List<QuickCommand>) {
        this.commands = commands
        notifyDataSetChanged()
    }

    fun setOnCommandActionListener(listener: OnCommandActionListener) {
        this.actionListener = listener
    }

    inner class QuickCommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_command)
        private val tvCommandName: TextView = itemView.findViewById(R.id.tv_command_name)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvCommand: TextView = itemView.findViewById(R.id.tv_command)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvLastUsed: TextView = itemView.findViewById(R.id.tv_last_used)
        private val tvUseCount: TextView = itemView.findViewById(R.id.tv_use_count)
        private val ivCommandIcon: ImageView = itemView.findViewById(R.id.iv_command_icon)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        private val btnDuplicate: MaterialButton = itemView.findViewById(R.id.btn_duplicate)
        private val btnExecute: MaterialButton = itemView.findViewById(R.id.btn_execute)

        fun bind(command: QuickCommand) {
            // 设置指令信息
            tvCommandName.text = command.name
            tvCommand.text = command.command
            tvDescription.text = command.description.takeIf { it.isNotBlank() } ?: "暂无描述"

            // 设置分类标签
            if (command.category.isNotBlank()) {
                tvCategory.text = command.category
                tvCategory.visibility = View.VISIBLE
            } else {
                tvCategory.visibility = View.GONE
            }

            // 设置最后使用时间
            tvLastUsed.text = formatLastUsedTime(command.lastUsedTime)

            // 设置使用次数
            tvUseCount.text = "使用 ${command.useCount} 次"

            // 设置图标
            ivCommandIcon.setImageResource(R.drawable.ic_terminal)
            ivCommandIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.rc_accent)
            )

            // 设置按钮点击事件
            btnEdit.setOnClickListener {
                actionListener?.onEditCommand(command)
            }

            btnDelete.setOnClickListener {
                actionListener?.onDeleteCommand(command)
            }

            btnDuplicate.setOnClickListener {
                actionListener?.onDuplicateCommand(command)
            }

            btnExecute.setOnClickListener {
                actionListener?.onExecuteCommand(command)
            }

            // 点击整个卡片执行指令
            cardView.setOnClickListener {
                actionListener?.onExecuteCommand(command)
            }

            // 长按编辑
            cardView.setOnLongClickListener {
                actionListener?.onEditCommand(command)
                true
            }

            // 根据有效性设置状态
            val isValid = command.isValid()
            itemView.alpha = if (isValid) 1.0f else 0.7f
            btnExecute.isEnabled = isValid
        }

        private fun formatLastUsedTime(timestamp: Long): String {
            if (timestamp == 0L) return "从未使用"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "刚刚使用"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
                else -> dateFormatter.format(Date(timestamp))
            }
        }
    }
}

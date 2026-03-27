package com.termux.app.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.app.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onSessionClick: (SessionInfo) -> Unit,
    private val onSessionLongClick: (SessionInfo) -> Boolean
) : ListAdapter<SessionInfo, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: SessionInfo) {
            binding.apply {
                sessionName.text = session.name
                sessionLastCommand.text = session.userPrompt ?: session.lastCommand
                sessionTime.text = formatTime(session.lastActiveTime)

                // Status icon and text
                val (icon, color, statusText) = getStatusDisplay(session.status)
                sessionStatusIcon.text = icon
                sessionStatusIcon.setTextColor(color)
                sessionStatusText.text = statusText
                sessionStatusText.setTextColor(color)

                root.setOnClickListener { onSessionClick(session) }
                root.setOnLongClickListener { onSessionLongClick(session) }
            }
        }

        private fun getStatusDisplay(status: SessionStatus): Triple<String, Int, String> {
            val context = binding.root.context
            return when (status) {
                SessionStatus.IDLE -> Triple("○", getColor(context, "status_idle"), "空闲")
                SessionStatus.RUNNING -> Triple("●", getColor(context, "status_running"), "运行中")
                SessionStatus.CLAUDE_THINKING -> Triple("◐", getColor(context, "status_thinking"), "思考中")
                SessionStatus.CLAUDE_WORKING -> Triple("◑", getColor(context, "status_working"), "输出中")
                SessionStatus.WAITING_INPUT -> Triple("◕", getColor(context, "status_waiting"), "等待输入")
                SessionStatus.ERROR -> Triple("⚠", getColor(context, "status_error"), "错误")
            }
        }

        private fun getColor(context: android.content.Context, name: String): Int {
            return context.getColor(context.resources.getIdentifier(name, "color", context.packageName))
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "刚刚"
                diff < 3600_000 -> "${diff / 60_000}分钟前"
                diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<SessionInfo>() {
        override fun areItemsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
            return oldItem == newItem
        }
    }
}
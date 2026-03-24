package com.termux.filebrowser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.RemoteFileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文件列表适配器 - 全屏列表布局（类似ES文件浏览器）
 * 显示：图标、名称、文件信息（目录/大小）、修改日期（月/日 时:分），右侧有收藏按钮
 */
class FileListAdapter(
    private val onItemClick: (RemoteFileItem) -> Unit,
    private val onItemLongClick: (RemoteFileItem) -> Boolean = { false },
    private val onBookmarkClick: ((RemoteFileItem, Int) -> Unit)? = null,
    private val isBookmarked: (String) -> Boolean = { false }
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    private var items: List<RemoteFileItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    fun submitList(newItems: List<RemoteFileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.file_icon)
        private val name: TextView = itemView.findViewById(R.id.file_name)
        private val info: TextView = itemView.findViewById(R.id.file_info)
        private val date: TextView = itemView.findViewById(R.id.file_date)
        private val bookmarkBtn: ImageView = itemView.findViewById(R.id.bookmark_btn)

        fun bind(item: RemoteFileItem, position: Int) {
            name.text = item.name

            // 设置图标
            icon.setImageResource(when {
                item.isDirectory -> R.drawable.ic_folder
                item.name.endsWith(".java") || item.name.endsWith(".kt") -> R.drawable.ic_file_code
                item.name.endsWith(".xml") || item.name.endsWith(".html") -> R.drawable.ic_code
                item.name.endsWith(".txt") || item.name.endsWith(".md") -> R.drawable.ic_file_text
                else -> R.drawable.ic_file
            })

            // 文件信息（目录显示"目录"，文件显示大小）
            info.text = if (item.isDirectory) "目录" else formatFileSize(item.size)

            // 修改日期
            date.text = dateFormat.format(Date(item.lastModified * 1000))

            // 收藏按钮状态
            if (item.isDirectory) {
                bookmarkBtn.visibility = View.VISIBLE
                updateBookmarkIcon(item.path)
                bookmarkBtn.setOnClickListener {
                    onBookmarkClick?.invoke(item, position)
                }
            } else {
                bookmarkBtn.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item) }
        }

        fun updateBookmarkIcon(path: String) {
            val bookmarked = isBookmarked(path)
            bookmarkBtn.setImageResource(
                if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
            )
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}

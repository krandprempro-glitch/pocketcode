package com.termux.filebrowser.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.RemoteFileItem

class FolderGridAdapter(
    private val onItemClick: (RemoteFileItem) -> Unit,
    private val onItemLongClick: (RemoteFileItem) -> Boolean = { false }
) : RecyclerView.Adapter<FolderGridAdapter.ViewHolder>() {

    private var items: List<RemoteFileItem> = emptyList()

    fun submitList(newItems: List<RemoteFileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val name: TextView = itemView.findViewById(R.id.item_name)

        fun bind(item: RemoteFileItem) {
            name.text = item.name

            // 根据类型设置图标
            icon.setImageResource(when {
                item.isDirectory -> R.drawable.ic_folder
                item.name.endsWith(".java") || item.name.endsWith(".kt") -> R.drawable.ic_file_code
                item.name.endsWith(".xml") || item.name.endsWith(".html") -> R.drawable.ic_code
                item.name.endsWith(".txt") || item.name.endsWith(".md") -> R.drawable.ic_file_text
                else -> R.drawable.ic_file
            })

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item) }
        }
    }
}

package com.termux.filebrowser.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.DirectoryBookmark
import com.termux.databinding.ItemBookmarkBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 书签列表适配器 - Kotlin重构版本
 * 使用现代Android开发最佳实践：ListAdapter, DiffUtil, ViewBinding
 */
class BookmarksAdapter(
    private val listener: OnBookmarkActionListener
) : ListAdapter<DirectoryBookmark, BookmarksAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    }

    interface OnBookmarkActionListener {
        fun onBookmarkClick(bookmark: DirectoryBookmark)
        fun onBookmarkLongClick(bookmark: DirectoryBookmark)
        fun onBookmarkRemove(bookmark: DirectoryBookmark)
        fun onBookmarkSendToTerminal(bookmark: DirectoryBookmark)
    }

    class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: DirectoryBookmark, listener: OnBookmarkActionListener) {
            binding.apply {
                // 设置书签图标
                bookmarkIcon.setImageResource(R.drawable.ic_bookmark_filled)

                // 设置显示名称
                bookmarkName.text = bookmark.displayName

                // 设置路径
                bookmarkPath.text = bookmark.fullPath

                // 设置创建时间
                val timeStr = DATE_FORMAT.format(Date(bookmark.createdTime))
                bookmarkTime.text = timeStr

                // 点击事件
                root.setOnClickListener {
                    listener.onBookmarkClick(bookmark)
                }

                // 长按事件
                root.setOnLongClickListener {
                    listener.onBookmarkLongClick(bookmark)
                    true
                }

                // 发送到终端按钮点击事件
                sendToTerminalButton.setOnClickListener {
                    listener.onBookmarkSendToTerminal(bookmark)
                }

                // 删除按钮点击事件
                removeButton.setOnClickListener {
                    listener.onBookmarkRemove(bookmark)
                }

                // 设置项目背景
                root.setBackgroundResource(R.drawable.bookmark_item_background)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = getItem(position)
        holder.bind(bookmark, listener)
    }

    // 公共方法
    fun updateBookmarks(bookmarks: List<DirectoryBookmark>) {
        // 按创建时间倒序排列
        val sortedBookmarks = bookmarks.sortedByDescending { it.createdTime }
        submitList(sortedBookmarks)
    }

    fun removeBookmark(bookmark: DirectoryBookmark) {
        val currentList = currentList.toMutableList()
        currentList.remove(bookmark)
        submitList(currentList)
    }

    fun addBookmark(bookmark: DirectoryBookmark) {
        val currentList = currentList.toMutableList()
        currentList.add(0, bookmark) // 添加到顶部
        submitList(currentList)
    }

    // DiffUtil for efficient updates
    private class BookmarkDiffCallback : DiffUtil.ItemCallback<DirectoryBookmark>() {
        override fun areItemsTheSame(
            oldItem: DirectoryBookmark,
            newItem: DirectoryBookmark
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DirectoryBookmark,
            newItem: DirectoryBookmark
        ): Boolean {
            return oldItem == newItem
        }
    }
}

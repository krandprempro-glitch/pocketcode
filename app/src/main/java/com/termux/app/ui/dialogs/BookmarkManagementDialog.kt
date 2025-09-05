package com.termux.app.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.termux.R
import com.termux.app.adapters.BookmarksAdapter
import com.termux.app.models.DirectoryBookmark
import com.termux.app.utils.LightToast
import com.termux.databinding.DialogBookmarkManagementBinding

/**
 * 书签管理对话框 - Kotlin版本
 * 提供书签的增删改查功能
 */
class BookmarkManagementDialog(
    context: Context,
    private val bookmarks: MutableList<DirectoryBookmark>,
    private val listener: OnBookmarkManagementListener
) : BaseDialog(context), BookmarksAdapter.OnBookmarkActionListener {

    private lateinit var binding: DialogBookmarkManagementBinding
    private lateinit var bookmarksAdapter: BookmarksAdapter

    interface OnBookmarkManagementListener {
        fun onBookmarkNavigate(bookmark: DirectoryBookmark)
        fun onBookmarkEdit(bookmark: DirectoryBookmark)
        fun onBookmarkDelete(bookmark: DirectoryBookmark)
        fun onBookmarkAdd(path: String, name: String)
    }

    override fun getLayoutResource(): Int = R.layout.dialog_bookmark_management

    override fun initViews() {
        binding = DialogBookmarkManagementBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupRecyclerView()
        updateEmptyState()
        
        // 设置对话框标题
        binding.dialogTitle.text = "收藏夹管理 (${bookmarks.size})"
    }

    override fun setupEventListeners() {
        binding.apply {
            // 关闭按钮
            closeButton.setOnClickListener { dismiss() }
            
            // 取消按钮
            cancelButton.setOnClickListener { dismiss() }
            
            // 添加书签按钮
            addBookmarkButton.setOnClickListener {
                showAddBookmarkDialog()
            }
            
            // 清空所有书签按钮
            clearAllButton.setOnClickListener {
                showClearAllConfirmDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        bookmarksAdapter = BookmarksAdapter(this)
        
        binding.bookmarksList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookmarksAdapter
        }
        
        bookmarksAdapter.updateBookmarks(bookmarks)
    }

    private fun updateEmptyState() {
        binding.apply {
            val isEmpty = bookmarks.isEmpty()
            emptyStateLayout.isVisible = isEmpty
            bookmarksList.isVisible = !isEmpty
            clearAllButton.isVisible = !isEmpty
            
            if (isEmpty) {
                emptyStateText.text = "还没有收藏任何目录\n\n点击「添加书签」按钮开始收藏您常用的目录"
            }
        }
    }

    // BookmarksAdapter.OnBookmarkActionListener 实现
    override fun onBookmarkClick(bookmark: DirectoryBookmark) {
        listener.onBookmarkNavigate(bookmark)
        dismiss()
    }

    override fun onBookmarkLongClick(bookmark: DirectoryBookmark) {
        showBookmarkOptionsDialog(bookmark)
    }

    override fun onBookmarkRemove(bookmark: DirectoryBookmark) {
        showDeleteConfirmDialog(bookmark)
    }

    private fun showBookmarkOptionsDialog(bookmark: DirectoryBookmark) {
        val options = arrayOf("跳转到该目录", "编辑书签", "删除书签")
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("书签操作: ${bookmark.displayName}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        listener.onBookmarkNavigate(bookmark)
                        dismiss()
                    }
                    1 -> showEditBookmarkDialog(bookmark)
                    2 -> showDeleteConfirmDialog(bookmark)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditBookmarkDialog(bookmark: DirectoryBookmark) {
        val editText = android.widget.EditText(context).apply {
            setText(bookmark.displayName)
            setSelection(bookmark.displayName.length)
            hint = "请输入书签名称"
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("编辑书签")
            .setMessage("路径: ${bookmark.fullPath}")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != bookmark.displayName) {
                    val updatedBookmark = bookmark.copy()
                    updatedBookmark.displayName = newName
                    listener.onBookmarkEdit(updatedBookmark)
                    updateBookmarkInList(bookmark, updatedBookmark)
                    LightToast.showShort(context, "书签已更新")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(bookmark: DirectoryBookmark) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("删除书签")
            .setMessage("确定要删除书签「${bookmark.displayName}」吗？\n\n路径: ${bookmark.fullPath}")
            .setPositiveButton("删除") { _, _ ->
                listener.onBookmarkDelete(bookmark)
                removeBookmarkFromList(bookmark)
                LightToast.showShort(context, "书签已删除")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddBookmarkDialog() {
        val pathEditText = android.widget.EditText(context).apply {
            hint = "请输入目录路径，如: /home/user/documents"
        }
        
        val nameEditText = android.widget.EditText(context).apply {
            hint = "请输入书签名称"
        }

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(android.widget.TextView(context).apply {
                text = "目录路径:"
                setPadding(0, 0, 0, 8)
            })
            addView(pathEditText)
            addView(android.widget.TextView(context).apply {
                text = "书签名称:"
                setPadding(0, 32, 0, 8)
            })
            addView(nameEditText)
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("添加书签")
            .setView(container)
            .setPositiveButton("添加") { _, _ ->
                val path = pathEditText.text.toString().trim()
                val name = nameEditText.text.toString().trim()
                
                if (path.isNotEmpty() && name.isNotEmpty()) {
                    listener.onBookmarkAdd(path, name)
                    LightToast.showShort(context, "书签已添加")
                } else {
                    LightToast.showShort(context, "请填写完整信息")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("清空所有书签")
            .setMessage("确定要删除所有 ${bookmarks.size} 个书签吗？此操作无法撤销。")
            .setPositiveButton("清空") { _, _ ->
                val bookmarksToDelete = bookmarks.toList()
                bookmarks.clear()
                bookmarksAdapter.updateBookmarks(bookmarks)
                updateEmptyState()
                
                // 通知删除所有书签
                bookmarksToDelete.forEach { bookmark ->
                    listener.onBookmarkDelete(bookmark)
                }
                
                LightToast.showShort(context, "所有书签已清空")
                binding.dialogTitle.text = "收藏夹管理 (0)"
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateBookmarkInList(oldBookmark: DirectoryBookmark, newBookmark: DirectoryBookmark) {
        val index = bookmarks.indexOfFirst { it.id == oldBookmark.id }
        if (index != -1) {
            bookmarks[index] = newBookmark
            bookmarksAdapter.updateBookmarks(bookmarks)
        }
    }

    private fun removeBookmarkFromList(bookmark: DirectoryBookmark) {
        bookmarks.removeAll { it.id == bookmark.id }
        bookmarksAdapter.updateBookmarks(bookmarks)
        updateEmptyState()
        binding.dialogTitle.text = "收藏夹管理 (${bookmarks.size})"
    }

    /**
     * 添加新书签到列表
     */
    fun addBookmark(bookmark: DirectoryBookmark) {
        bookmarks.add(0, bookmark) // 添加到顶部
        bookmarksAdapter.updateBookmarks(bookmarks)
        updateEmptyState()
        binding.dialogTitle.text = "收藏夹管理 (${bookmarks.size})"
    }

    companion object {
        fun show(
            context: Context,
            bookmarks: MutableList<DirectoryBookmark>,
            listener: OnBookmarkManagementListener
        ): BookmarkManagementDialog {
            return BookmarkManagementDialog(context, bookmarks, listener).apply {
                show()
            }
        }
    }
}
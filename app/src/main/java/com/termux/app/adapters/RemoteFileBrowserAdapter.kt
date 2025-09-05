package com.termux.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.FileTypeUtils
import com.termux.app.models.RemoteFileItem
import com.termux.app.utils.FileUtils
import com.termux.databinding.ItemRemoteFileBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 远程文件列表适配器 - Kotlin重构版本
 * 使用现代Android开发最佳实践：ListAdapter, DiffUtil, ViewBinding
 */
class RemoteFileBrowserAdapter(
    private val listener: OnFileClickListener
) : ListAdapter<RemoteFileItem, RemoteFileBrowserAdapter.FileViewHolder>(FileDiffCallback()) {

    companion object {
        private const val ITEM_TYPE_PARENT = 0
        private const val ITEM_TYPE_FILE = 1
    }

    private val selectedFiles = mutableSetOf<String>()
    private var selectionMode = false
    private var showHiddenFiles = false
    private var currentPath = "/"
    private var bookmarkStateProvider: BookmarkStateProvider? = null
    private var bookmarkToggleListener: BookmarkToggleListener? = null

    interface OnFileClickListener {
        fun onFileClick(file: RemoteFileItem)
        fun onFileLongClick(file: RemoteFileItem)
        fun onMoreOptionsClick(file: RemoteFileItem, anchorView: View)
    }

    interface BookmarkStateProvider {
        fun isBookmarked(path: String): Boolean
    }

    interface BookmarkToggleListener {
        fun onBookmarkToggle(file: RemoteFileItem)
    }

    // ViewHolder with ViewBinding
    class FileViewHolder(private val binding: ItemRemoteFileBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            file: RemoteFileItem,
            isSelected: Boolean,
            selectionMode: Boolean,
            bookmarkStateProvider: BookmarkStateProvider?,
            listener: OnFileClickListener?,
            bookmarkToggleListener: BookmarkToggleListener?
        ) {
            binding.apply {
                // 文件图标
                fileTypeIcon.setImageResource(FileTypeUtils.getFileTypeIcon(file.type))

                // 文件名
                fileName.text = file.name

                // 文件信息
                fileInfo.text = if (file.isDirectory) {
                    if (file.size > 0) "${file.size} 项" else "文件夹"
                } else {
                    formatFileInfo(file)
                }

                // 选择状态
                fileSelectionCheckbox.apply {
                    visibility = if (selectionMode) View.VISIBLE else View.GONE
                    isChecked = isSelected
                }

                // 书签状态
                val isBookmarked = file.isDirectory && bookmarkStateProvider?.isBookmarked(file.path) == true
                bookmarkIndicator.visibility = if (isBookmarked) View.VISIBLE else View.GONE

                // 点击事件
                root.setOnClickListener {
                    if (selectionMode) {
                        // 选择模式下切换选中状态
                        fileSelectionCheckbox.isChecked = !fileSelectionCheckbox.isChecked
                    } else {
                        listener?.onFileClick(file)
                    }
                }

                // 长按事件
                root.setOnLongClickListener {
                    listener?.onFileLongClick(file)
                    true
                }

                // 更多选项按钮
                moreOptionsButton.setOnClickListener {
                    listener?.onMoreOptionsClick(file, it)
                }

                // 书签切换（仅目录）
                if (file.isDirectory) {
                    bookmarkIndicator.setOnClickListener {
                        bookmarkToggleListener?.onBookmarkToggle(file)
                    }
                }
            }
        }

        private fun formatFileInfo(file: RemoteFileItem): String {
            val size = FileUtils.formatFileSize(file.size)
            val date = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))
            return "$size • $date"
        }
    }

    override fun getItemViewType(position: Int): Int {
        val file = getItem(position)
        return if (file.name == "..") ITEM_TYPE_PARENT else ITEM_TYPE_FILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemRemoteFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        val isSelected = selectedFiles.contains(file.path)
        
        holder.bind(
            file = file,
            isSelected = isSelected,
            selectionMode = selectionMode,
            bookmarkStateProvider = bookmarkStateProvider,
            listener = listener,
            bookmarkToggleListener = bookmarkToggleListener
        )
    }

    // 公共方法
    fun updateFiles(files: List<RemoteFileItem>?, path: String = "/") {
        currentPath = path
        val fileList = mutableListOf<RemoteFileItem>()

        // 添加返回上级目录项（除根目录外）
        if (path != "/") {
            fileList.add(createParentDirectoryItem(path))
        }

        // 添加文件列表（过滤隐藏文件）
        files?.let { 
            val filteredFiles = if (showHiddenFiles) {
                it
            } else {
                it.filter { file -> !file.name.startsWith(".") }
            }
            fileList.addAll(filteredFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
        }

        submitList(fileList)
    }

    fun setBookmarkProviders(
        stateProvider: BookmarkStateProvider?,
        toggleListener: BookmarkToggleListener?
    ) {
        this.bookmarkStateProvider = stateProvider
        this.bookmarkToggleListener = toggleListener
    }

    fun toggleSelectionMode() {
        selectionMode = !selectionMode
        if (!selectionMode) {
            selectedFiles.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<RemoteFileItem> {
        return currentList.filter { selectedFiles.contains(it.path) }
    }

    fun selectAll() {
        selectedFiles.clear()
        selectedFiles.addAll(currentList.map { it.path })
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    fun toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles
        // 需要重新加载文件列表
    }

    private fun createParentDirectoryItem(currentPath: String): RemoteFileItem {
        val parentPath = if (currentPath == "/") "/" else currentPath.substringBeforeLast("/").ifEmpty { "/" }
        return RemoteFileItem().apply {
            name = ".."
            path = parentPath
            isDirectory = true
            type = com.termux.app.models.FileType.DIRECTORY
            size = 0
            lastModified = 0
            permissions = ""
        }
    }

    // DiffUtil for efficient list updates
    private class FileDiffCallback : DiffUtil.ItemCallback<RemoteFileItem>() {
        override fun areItemsTheSame(oldItem: RemoteFileItem, newItem: RemoteFileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: RemoteFileItem, newItem: RemoteFileItem): Boolean {
            return oldItem == newItem
        }
    }

    // 兼容性包装器，用于支持旧的接口
    class AdapterWrapper(private val drawerAdapter: DrawerFileAdapter) {
        private val remoteAdapter = RemoteFileBrowserAdapter(object : OnFileClickListener {
            override fun onFileClick(file: RemoteFileItem) {
                // 委托给 DrawerFileAdapter 的监听器
                drawerAdapter.listener?.onFileClick(file)
            }

            override fun onFileLongClick(file: RemoteFileItem) {
                // Handled by drawer adapter
            }

            override fun onMoreOptionsClick(file: RemoteFileItem, anchorView: View) {
                drawerAdapter.listener?.onFileMoreClick(file, anchorView)
            }
        })
        
        fun updateFiles(files: List<RemoteFileItem>?) {
            updateFiles(files, "/")
        }

        fun updateFiles(files: List<RemoteFileItem>?, path: String?) {
            drawerAdapter.updateFiles(files, path ?: "/")
        }
    }
}
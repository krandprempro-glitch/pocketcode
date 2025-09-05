package com.termux.filebrowser.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.FileTreeNode
import com.termux.app.models.FileTypeUtils
import com.termux.databinding.FileTreeItemBinding

/**
 * 文件树适配器 - Kotlin重构版本
 * 实现VSCode风格的层级目录树显示，使用现代Android开发最佳实践
 */
class FileTreeAdapter(
    private val listener: OnFileTreeActionListener
) : ListAdapter<FileTreeNode, FileTreeAdapter.FileTreeViewHolder>(TreeNodeDiffCallback()) {

    companion object {
        private const val INDENT_SIZE_DP = 20 // 每级缩进的dp值
    }

    private val rootNodes = mutableListOf<FileTreeNode>()
    private val flattenedNodes = mutableListOf<FileTreeNode>()
    private var selectedPath: String? = null

    interface OnFileTreeActionListener {
        fun onNodeExpanded(node: FileTreeNode)
        fun onNodeCollapsed(node: FileTreeNode)
        fun onFileSelected(node: FileTreeNode)
        fun onDirectoryBookmarked(node: FileTreeNode)
        fun onLoadNodeChildren(node: FileTreeNode)
    }

    class FileTreeViewHolder(
        private val binding: FileTreeItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            node: FileTreeNode,
            isSelected: Boolean,
            listener: OnFileTreeActionListener
        ) {
            binding.apply {
                // 设置缩进
                val density = root.context.resources.displayMetrics.density
                val indentPx = (node.depth * INDENT_SIZE_DP * density).toInt()
                val params = indentSpace.layoutParams
                params.width = indentPx
                indentSpace.layoutParams = params

                // 设置展开/折叠图标
                if (node.isDirectory) {
                    when {
                        node.hasChildren() || !node.isExpanded -> {
                            expandIcon.visibility = View.VISIBLE
                            val rotation = if (node.isExpanded) 90f else 0f
                            expandIcon.rotation = rotation
                        }
                        else -> expandIcon.visibility = View.INVISIBLE
                    }
                } else {
                    expandIcon.visibility = View.INVISIBLE
                }

                // 设置文件/目录图标
                fileIcon.setImageResource(FileTypeUtils.getFileTypeIcon(node.fileType))

                // 设置文件名
                fileName.text = node.name

                // 设置选中状态
                root.isSelected = isSelected
                root.setBackgroundResource(
                    if (isSelected) R.drawable.file_tree_item_selected_background 
                    else android.R.color.transparent
                )

                // 设置加载状态
                loadingIndicator.visibility = if (node.isLoading) View.VISIBLE else View.GONE

                // 设置书签状态
                bookmarkIcon.visibility = if (node.isBookmarked) View.VISIBLE else View.GONE

                // 展开/折叠点击事件
                expandIcon.setOnClickListener {
                    if (node.isDirectory) {
                        toggleNodeExpansion(node, listener)
                    }
                }

                // 整个项目点击事件
                root.setOnClickListener {
                    if (node.isDirectory) {
                        if (node.hasChildren() || node.children.isNotEmpty()) {
                            toggleNodeExpansion(node, listener)
                        } else {
                            // 加载子节点
                            node.isLoading = true
                            loadingIndicator.visibility = View.VISIBLE
                            listener.onLoadNodeChildren(node)
                        }
                    } else {
                        listener.onFileSelected(node)
                    }
                }

                // 长按书签事件（仅目录）
                if (node.isDirectory) {
                    root.setOnLongClickListener {
                        listener.onDirectoryBookmarked(node)
                        true
                    }
                }
            }
        }

        private fun toggleNodeExpansion(node: FileTreeNode, listener: OnFileTreeActionListener) {
            if (node.isExpanded) {
                // 折叠动画
                ObjectAnimator.ofFloat(binding.expandIcon, "rotation", 90f, 0f).apply {
                    duration = 200
                    start()
                }
                node.isExpanded = false
                listener.onNodeCollapsed(node)
            } else {
                // 展开动画
                ObjectAnimator.ofFloat(binding.expandIcon, "rotation", 0f, 90f).apply {
                    duration = 200
                    start()
                }
                node.isExpanded = true
                listener.onNodeExpanded(node)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTreeViewHolder {
        val binding = FileTreeItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileTreeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileTreeViewHolder, position: Int) {
        val node = getItem(position)
        val isSelected = node.fullPath == selectedPath
        holder.bind(node, isSelected, listener)
    }

    // 公共方法
    fun setRootNodes(nodes: List<FileTreeNode>) {
        rootNodes.clear()
        rootNodes.addAll(nodes)
        refreshFlattenedNodes()
    }

    fun addChildrenToNode(parentNode: FileTreeNode, children: List<FileTreeNode>) {
        parentNode.children.clear()
        parentNode.children.addAll(children)
        parentNode.isLoading = false
        
        // 设置子节点的深度
        children.forEach { child ->
            child.depth = parentNode.depth + 1
        }
        
        refreshFlattenedNodes()
    }

    fun refreshNode(node: FileTreeNode) {
        val position = flattenedNodes.indexOf(node)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun setSelectedPath(path: String?) {
        selectedPath = path
        notifyDataSetChanged()
    }

    fun expandToPath(path: String) {
        val pathParts = path.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        
        for (part in pathParts) {
            currentPath += "/$part"
            val node = findNodeByPath(currentPath)
            node?.let {
                if (it.isDirectory && !it.isExpanded) {
                    it.isExpanded = true
                }
            }
        }
        
        refreshFlattenedNodes()
        setSelectedPath(path)
    }

    fun getFlattenedNodes(): List<FileTreeNode> {
        return flattenedNodes.toList()
    }

    private fun refreshFlattenedNodes() {
        flattenedNodes.clear()
        for (rootNode in rootNodes) {
            addNodeToFlattened(rootNode)
        }
        submitList(flattenedNodes.toList())
    }

    private fun addNodeToFlattened(node: FileTreeNode) {
        flattenedNodes.add(node)
        
        if (node.isExpanded && node.children.isNotEmpty()) {
            for (child in node.children) {
                addNodeToFlattened(child)
            }
        }
    }

    private fun findNodeByPath(path: String): FileTreeNode? {
        return flattenedNodes.find { it.fullPath == path }
    }

    // DiffUtil for efficient updates
    private class TreeNodeDiffCallback : DiffUtil.ItemCallback<FileTreeNode>() {
        override fun areItemsTheSame(oldItem: FileTreeNode, newItem: FileTreeNode): Boolean {
            return oldItem.fullPath == newItem.fullPath
        }

        override fun areContentsTheSame(oldItem: FileTreeNode, newItem: FileTreeNode): Boolean {
            return oldItem == newItem
        }
    }
}

package com.termux.app.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.termux.R
import com.termux.app.models.RemoteFileItem
import com.termux.databinding.ViewFileOperationPanelBinding

/**
 * 文件操作面板组件
 * 提供文件/目录的常用操作功能，支持动画效果
 */
class FileOperationPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFileOperationPanelBinding
    private var currentFile: RemoteFileItem? = null
    private var operationListener: OnFileOperationListener? = null
    private var isExpanded = false

    interface OnFileOperationListener {
        fun onDownload(file: RemoteFileItem)
        fun onUpload(targetPath: String)
        fun onDelete(file: RemoteFileItem)
        fun onRename(file: RemoteFileItem)
        fun onCopy(file: RemoteFileItem)
        fun onMove(file: RemoteFileItem)
        fun onProperties(file: RemoteFileItem)
        fun onBookmarkToggle(file: RemoteFileItem)
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        binding = ViewFileOperationPanelBinding.inflate(
            LayoutInflater.from(context), this, true
        )

        setupViews()
        setupClickListeners()
        
        // 初始状态为折叠
        visibility = GONE
    }

    private fun setupViews() {
        binding.apply {
            // 设置面板背景和圆角
            operationPanel.setBackgroundResource(R.drawable.file_operation_panel_background)
            
            // 初始化图标
            downloadButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_download, 0, 0, 0
            )
            uploadButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_upload, 0, 0, 0
            )
            deleteButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_delete, 0, 0, 0
            )
            renameButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_rename, 0, 0, 0
            )
            copyButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_copy, 0, 0, 0
            )
            moveButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_move, 0, 0, 0
            )
            propertiesButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_properties, 0, 0, 0
            )
            bookmarkButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_bookmark, 0, 0, 0
            )
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // 关闭按钮
            closeButton.setOnClickListener {
                hide()
            }

            // 操作按钮点击事件
            downloadButton.setOnClickListener {
                currentFile?.let { operationListener?.onDownload(it) }
                hide()
            }

            uploadButton.setOnClickListener {
                currentFile?.let { 
                    val path = if (it.isDirectory) it.path else it.path.substringBeforeLast("/")
                    operationListener?.onUpload(path) 
                }
                hide()
            }

            deleteButton.setOnClickListener {
                currentFile?.let { operationListener?.onDelete(it) }
                hide()
            }

            renameButton.setOnClickListener {
                currentFile?.let { operationListener?.onRename(it) }
                hide()
            }

            copyButton.setOnClickListener {
                currentFile?.let { operationListener?.onCopy(it) }
                hide()
            }

            moveButton.setOnClickListener {
                currentFile?.let { operationListener?.onMove(it) }
                hide()
            }

            propertiesButton.setOnClickListener {
                currentFile?.let { operationListener?.onProperties(it) }
                hide()
            }

            bookmarkButton.setOnClickListener {
                currentFile?.let { operationListener?.onBookmarkToggle(it) }
                updateBookmarkButton()
            }

            // 点击面板外部关闭
            root.setOnClickListener {
                hide()
            }

            // 防止点击面板本身时关闭
            operationPanel.setOnClickListener { /* 阻止事件传播 */ }
        }
    }

    fun show(file: RemoteFileItem, bookmarkStateProvider: (() -> Boolean)? = null) {
        currentFile = file
        
        // 更新UI状态
        updateUIForFile(file, bookmarkStateProvider)
        
        // 显示动画
        if (!isExpanded) {
            visibility = VISIBLE
            startShowAnimation()
            isExpanded = true
        }
    }

    fun hide() {
        if (isExpanded) {
            startHideAnimation()
            isExpanded = false
        }
    }

    fun setOnFileOperationListener(listener: OnFileOperationListener?) {
        this.operationListener = listener
    }

    private fun updateUIForFile(file: RemoteFileItem, bookmarkStateProvider: (() -> Boolean)?) {
        binding.apply {
            // 设置文件信息
            fileName.text = file.name
            fileInfo.text = if (file.isDirectory) {
                "目录 • ${file.size} 项"
            } else {
                "${formatFileSize(file.size)} • ${file.type.name}"
            }

            // 根据文件类型显示/隐藏操作按钮
            when {
                file.isDirectory -> {
                    downloadButton.isVisible = false
                    uploadButton.isVisible = true
                    bookmarkButton.isVisible = true
                    
                    // 更新书签按钮状态
                    updateBookmarkButton(bookmarkStateProvider?.invoke() ?: false)
                }
                else -> {
                    downloadButton.isVisible = true
                    uploadButton.isVisible = false
                    bookmarkButton.isVisible = false
                }
            }

            // 通用操作始终可用
            deleteButton.isVisible = true
            renameButton.isVisible = true
            copyButton.isVisible = true
            moveButton.isVisible = true
            propertiesButton.isVisible = true
        }
    }

    private fun updateBookmarkButton(isBookmarked: Boolean = false) {
        binding.bookmarkButton.apply {
            val (iconRes, textRes) = if (isBookmarked) {
                R.drawable.ic_bookmark_remove to R.string.remove_bookmark
            } else {
                R.drawable.ic_bookmark_add to R.string.add_bookmark
            }
            
            setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
            setText(textRes)
        }
    }

    private fun startShowAnimation() {
        // 缩放和透明度动画
        val scaleX = ObjectAnimator.ofFloat(binding.operationPanel, "scaleX", 0.7f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.operationPanel, "scaleY", 0.7f, 1f)
        val alpha = ObjectAnimator.ofFloat(binding.operationPanel, "alpha", 0f, 1f)
        val translationY = ObjectAnimator.ofFloat(
            binding.operationPanel, "translationY", 50f, 0f
        )

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha, translationY)
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun startHideAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.operationPanel, "scaleX", 1f, 0.7f)
        val scaleY = ObjectAnimator.ofFloat(binding.operationPanel, "scaleY", 1f, 0.7f)
        val alpha = ObjectAnimator.ofFloat(binding.operationPanel, "alpha", 1f, 0f)
        val translationY = ObjectAnimator.ofFloat(
            binding.operationPanel, "translationY", 0f, 50f
        )

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha, translationY)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            
            // 动画结束后隐藏视图
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = GONE
                    currentFile = null
                }
            })
        }.start()
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * 检查面板是否正在显示
     */
    fun isShowing(): Boolean = isExpanded && isVisible

    /**
     * 获取当前操作的文件
     */
    fun getCurrentFile(): RemoteFileItem? = currentFile
}
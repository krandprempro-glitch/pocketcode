package com.termux.app.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.termux.R
import com.termux.app.models.RemoteFileItem
import com.termux.app.utils.FileUtils
import com.termux.databinding.DialogFilePropertiesBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件属性对话框 - Kotlin版本
 * 显示文件/目录的详细信息
 */
class FilePropertiesDialog(
    context: Context,
    private val fileItem: RemoteFileItem,
    private val onActionListener: OnFileActionListener? = null
) : BaseDialog(context) {

    private lateinit var binding: DialogFilePropertiesBinding
    
    interface OnFileActionListener {
        fun onDownload(file: RemoteFileItem)
        fun onDelete(file: RemoteFileItem)
        fun onRename(file: RemoteFileItem)
        fun onBookmarkToggle(file: RemoteFileItem)
    }

    override fun getLayoutResource(): Int = R.layout.dialog_file_properties

    override fun initViews() {
        binding = DialogFilePropertiesBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupFileInfo()
        setupActionButtons()
    }

    override fun setupEventListeners() {
        binding.apply {
            // 关闭按钮
            closeButton.setOnClickListener { dismiss() }
            
            // 取消按钮
            cancelButton.setOnClickListener { dismiss() }
            
            // 操作按钮
            downloadButton.setOnClickListener {
                onActionListener?.onDownload(fileItem)
                dismiss()
            }
            
            deleteButton.setOnClickListener {
                onActionListener?.onDelete(fileItem)
                dismiss()
            }
            
            renameButton.setOnClickListener {
                onActionListener?.onRename(fileItem)
                dismiss()
            }
            
            bookmarkButton.setOnClickListener {
                onActionListener?.onBookmarkToggle(fileItem)
                dismiss()
            }
        }
    }

    private fun setupFileInfo() {
        binding.apply {
            // 文件图标
            fileIcon.setImageResource(getFileTypeIcon(fileItem))
            
            // 文件名
            fileName.text = fileItem.name
            
            // 文件类型
            fileType.text = if (fileItem.isDirectory) "目录" else getFileTypeDescription(fileItem)
            
            // 文件路径
            filePath.text = fileItem.path
            
            // 文件大小
            fileSize.text = if (fileItem.isDirectory) {
                "${fileItem.size} 项"
            } else {
                FileUtils.formatFileSize(fileItem.size)
            }
            
            // 修改时间
            val modifiedDate = Date(fileItem.lastModified)
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
            lastModified.text = dateFormat.format(modifiedDate)
            
            // 权限信息
            permissions.text = fileItem.permissions.ifEmpty { "未知" }
            
            // 详细信息（仅文件）
            if (!fileItem.isDirectory) {
                // 显示文件扩展名
                val extension = FileUtils.getFileExtension(fileItem.name)
                if (extension.isNotEmpty()) {
                    fileExtension.text = ".$extension"
                    fileExtension.isVisible = true
                    fileExtensionLabel.isVisible = true
                } else {
                    fileExtension.isVisible = false
                    fileExtensionLabel.isVisible = false
                }
                
                // MIME类型
                val mimeType = FileUtils.getMimeType(fileItem.name)
                if (mimeType.isNotEmpty()) {
                    fileMimeType.text = mimeType
                    fileMimeType.isVisible = true
                    fileMimeTypeLabel.isVisible = true
                } else {
                    fileMimeType.isVisible = false
                    fileMimeTypeLabel.isVisible = false
                }
            } else {
                // 目录不显示扩展名和MIME类型
                fileExtension.isVisible = false
                fileExtensionLabel.isVisible = false
                fileMimeType.isVisible = false
                fileMimeTypeLabel.isVisible = false
            }
        }
    }

    private fun setupActionButtons() {
        binding.apply {
            // 根据文件类型显示/隐藏操作按钮
            when {
                fileItem.isDirectory -> {
                    downloadButton.isVisible = false
                    bookmarkButton.isVisible = true
                    bookmarkButton.text = "添加书签"
                }
                else -> {
                    downloadButton.isVisible = true
                    bookmarkButton.isVisible = false
                }
            }
            
            // 删除和重命名按钮始终可用
            deleteButton.isVisible = true
            renameButton.isVisible = true
            
            // 如果没有监听器，隐藏操作按钮区域
            if (onActionListener == null) {
                actionsContainer.isVisible = false
            }
        }
    }

    private fun getFileTypeIcon(file: RemoteFileItem): Int {
        return when {
            file.isDirectory -> R.drawable.ic_folder
            FileUtils.isImageFile(file.name) -> R.drawable.ic_image
            FileUtils.isVideoFile(file.name) -> R.drawable.ic_video
            FileUtils.isAudioFile(file.name) -> R.drawable.ic_audio
            FileUtils.isArchiveFile(file.name) -> R.drawable.ic_archive
            FileUtils.isCodeFile(file.name) -> R.drawable.ic_code
            FileUtils.isDocumentFile(file.name) -> R.drawable.ic_document
            else -> R.drawable.ic_file
        }
    }

    private fun getFileTypeDescription(file: RemoteFileItem): String {
        return when {
            FileUtils.isImageFile(file.name) -> "图片文件"
            FileUtils.isVideoFile(file.name) -> "视频文件"
            FileUtils.isAudioFile(file.name) -> "音频文件"
            FileUtils.isArchiveFile(file.name) -> "压缩文件"
            FileUtils.isCodeFile(file.name) -> "代码文件"
            FileUtils.isDocumentFile(file.name) -> "文档文件"
            else -> "文件"
        }
    }

    /**
     * 显示对话框的静态方法
     */
    companion object {
        fun show(
            context: Context,
            fileItem: RemoteFileItem,
            actionListener: OnFileActionListener? = null
        ): FilePropertiesDialog {
            return FilePropertiesDialog(context, fileItem, actionListener).apply {
                show()
            }
        }
    }
}
package com.termux.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.termux.R

/**
 * 路径面包屑导航组件
 * 显示当前路径，并支持点击导航到父级目录
 */
class PathBreadcrumbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val breadcrumbContainer: LinearLayout
    private var currentPath: String = "/"
    private var onPathClickListener: OnPathClickListener? = null

    interface OnPathClickListener {
        fun onPathClick(path: String)
    }

    init {
        // 配置滚动视图
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER

        // 创建水平容器
        breadcrumbContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(resources.getDimensionPixelSize(R.dimen.breadcrumb_padding))
        }

        addView(
            breadcrumbContainer,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )

        // 初始化样式
        setBackgroundResource(R.drawable.breadcrumb_background)
    }

    fun setPath(path: String) {
        if (currentPath == path) return
        
        currentPath = path
        updateBreadcrumb()
    }

    fun setOnPathClickListener(listener: OnPathClickListener?) {
        this.onPathClickListener = listener
    }

    private fun updateBreadcrumb() {
        breadcrumbContainer.removeAllViews()
        
        val pathParts = if (currentPath == "/") {
            listOf("/")
        } else {
            currentPath.split("/").filter { it.isNotEmpty() }
        }

        var accumulatedPath = ""
        
        for (i in pathParts.indices) {
            val part = pathParts[i]
            
            // 构建累积路径
            accumulatedPath = if (part == "/") {
                "/"
            } else {
                if (accumulatedPath == "/") "/$part" else "$accumulatedPath/$part"
            }

            // 添加路径部分
            addPathPart(part, accumulatedPath, i == pathParts.size - 1)
            
            // 添加分隔符（除了最后一个）
            if (i < pathParts.size - 1) {
                addSeparator()
            }
        }

        // 滚动到最右边显示当前路径
        post {
            fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
    }

    private fun addPathPart(name: String, fullPath: String, isLast: Boolean) {
        val textView = TextView(context).apply {
            text = if (name == "/") "根目录" else name
            textSize = 14f
            
            // 设置样式
            if (isLast) {
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, R.color.breadcrumb_current_color))
            } else {
                typeface = Typeface.DEFAULT
                setTextColor(ContextCompat.getColor(context, R.color.breadcrumb_normal_color))
                
                // 添加点击效果
                setBackgroundResource(R.drawable.breadcrumb_item_selector)
                isClickable = true
                isFocusable = true
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.breadcrumb_item_padding_horizontal),
                    resources.getDimensionPixelSize(R.dimen.breadcrumb_item_padding_vertical),
                    resources.getDimensionPixelSize(R.dimen.breadcrumb_item_padding_horizontal),
                    resources.getDimensionPixelSize(R.dimen.breadcrumb_item_padding_vertical)
                )
                
                setOnClickListener {
                    onPathClickListener?.onPathClick(fullPath)
                }
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        breadcrumbContainer.addView(textView, params)
    }

    private fun addSeparator() {
        val separator = TextView(context).apply {
            text = " / "
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.breadcrumb_separator_color))
            typeface = Typeface.DEFAULT
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        breadcrumbContainer.addView(separator, params)
    }

    /**
     * 获取当前路径
     */
    fun getCurrentPath(): String = currentPath

    /**
     * 优化路径显示（用于空间有限的场景）
     */
    fun getOptimizedPath(maxLength: Int = 40): String {
        if (currentPath.length <= maxLength) return currentPath
        
        val pathParts = currentPath.split("/").filter { it.isNotEmpty() }
        return if (pathParts.size > 2) {
            val current = pathParts.last()
            val parent = pathParts[pathParts.size - 2]
            ".../$parent/$current"
        } else {
            currentPath
        }
    }
}
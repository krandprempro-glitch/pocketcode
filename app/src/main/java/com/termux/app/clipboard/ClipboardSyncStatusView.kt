package com.termux.app.clipboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * 剪贴板同步状态图标
 * 显示当前同步状态
 */
class ClipboardSyncStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val syncIcon: ImageView

    init {
        orientation = HORIZONTAL
        syncIcon = ImageView(context).apply {
            layoutParams = LayoutParams(48, 48)
            setImageResource(android.R.drawable.ic_menu_sync)
            visibility = View.GONE
        }
        addView(syncIcon)
    }

    /**
     * 显示同步中状态
     */
    fun showSyncing() {
        syncIcon.visibility = View.VISIBLE
        syncIcon.setImageResource(android.R.drawable.ic_menu_sync)
        // TODO: 添加旋转动画
    }

    /**
     * 显示已同步状态
     */
    fun showSynced() {
        syncIcon.visibility = View.VISIBLE
        // 使用勾号图标表示已同步
        syncIcon.setImageResource(android.R.drawable.ic_menu_ok)
    }

    /**
     * 隐藏状态图标
     */
    fun hide() {
        syncIcon.visibility = View.GONE
    }
}

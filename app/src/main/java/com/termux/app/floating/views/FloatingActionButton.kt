package com.termux.app.floating.views

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.termux.R

import com.termux.app.models.SSHConnectionConfig

/**
 * 主悬浮按钮
 * 提供主要的悬浮操作入口
 */
class FloatingActionButton(context: Context) : DraggableView(context) {

    private lateinit var iconView: ImageView
    private var actionListener: OnFloatingActionListener? = null

    interface OnFloatingActionListener {
        fun onMenuToggle(isVisible: Boolean)
        fun onSSHConnectionClicked()
        fun onSSHConfigSelected(config: SSHConnectionConfig)
        fun onRunCommandClicked()
        fun onQuickSettingsClicked()
    }
    
    init {
        initView()
    }
    
    private fun initView() {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.floating_action_button, this, true)
        iconView = findViewById(R.id.iv_floating_icon)
        
        // 设置点击效果
        setBackgroundResource(R.drawable.bg_floating_button)
        elevation = dpToPx(8).toFloat()
        
        // 设置图标
        iconView.setImageResource(R.drawable.ic_rocket)
        iconView.setColorFilter(ContextCompat.getColor(context, android.R.color.white))
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        toggleMenu()
        return true
    }
    
    private var menuDialog: OverlayMenuDialog? = null

    private fun toggleMenu() {
        if (menuDialog?.isShowing == true) {
            menuDialog?.dismiss()
            return
        }
        menuDialog = OverlayMenuDialog(context).apply {
            setOnMenuSelectListener(object : OverlayMenuDialog.OnMenuSelectListener {
                override fun onSSHConnection() { actionListener?.onSSHConnectionClicked() }
                override fun onSSHConfigSelected(config: SSHConnectionConfig) { actionListener?.onSSHConfigSelected(config) }
                override fun onRunCommand() { actionListener?.onRunCommandClicked() }
                override fun onQuickSettings() { actionListener?.onQuickSettingsClicked() }
            })
            show()
        }
        actionListener?.onMenuToggle(true)
    }
    
    override fun onDragStart() {
        // 拖拽开始时缩放效果
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
    }
    
    override fun onDragMove() {
        // 拖拽过程中保持缩放状态
    }
    
    override fun onDragEnd() {
        // 拖拽结束后恢复正常大小
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
    }
    
    fun setOnFloatingActionListener(listener: OnFloatingActionListener) {
        this.actionListener = listener
    }
    
    override fun hide() {
        super.hide()
    }
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}

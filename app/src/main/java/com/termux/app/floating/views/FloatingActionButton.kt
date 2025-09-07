package com.termux.app.floating.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.termux.R

/**
 * 主悬浮按钮
 * 提供主要的悬浮操作入口
 */
class FloatingActionButton(context: Context) : DraggableView(context) {
    
    private lateinit var iconView: ImageView
    private lateinit var menuPanel: FloatingMenuPanel
    private var isMenuVisible = false
    private var actionListener: OnFloatingActionListener? = null
    
    interface OnFloatingActionListener {
        fun onMenuToggle(isVisible: Boolean)
        fun onSSHConnectionClicked()
        fun onRunCommandClicked()
        fun onQuickSettingsClicked()
    }
    
    init {
        initView()
        initMenuPanel()
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
    
    private fun initMenuPanel() {
        menuPanel = FloatingMenuPanel(context)
        menuPanel.setOnMenuItemClickListener(object : FloatingMenuPanel.OnMenuItemClickListener {
            override fun onSSHConnectionClicked() {
                hideMenu()
                actionListener?.onSSHConnectionClicked()
            }
            
            override fun onRunCommandClicked() {
                hideMenu()
                actionListener?.onRunCommandClicked()
            }
            
            override fun onQuickSettingsClicked() {
                hideMenu()
                actionListener?.onQuickSettingsClicked()
            }
        })
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        toggleMenu()
        return true
    }
    
    private fun toggleMenu() {
        if (isMenuVisible) {
            hideMenu()
        } else {
            showMenu()
        }
    }
    
    private fun showMenu() {
        if (!isMenuVisible) {
            isMenuVisible = true
            
            // 计算菜单位置（悬浮按钮上方）
            val location = IntArray(2)
            getLocationOnScreen(location)
            
            menuPanel.showAt(location[0], location[1] - menuPanel.getMenuHeight())
            
            // 按钮旋转动画
            animateButtonRotation(0f, 45f)
            
            actionListener?.onMenuToggle(true)
        }
    }
    
    private fun hideMenu() {
        if (isMenuVisible) {
            isMenuVisible = false
            menuPanel.hide()
            
            // 按钮旋转动画
            animateButtonRotation(45f, 0f)
            
            actionListener?.onMenuToggle(false)
        }
    }
    
    private fun animateButtonRotation(from: Float, to: Float) {
        val rotationAnimator = ObjectAnimator.ofFloat(iconView, "rotation", from, to)
        rotationAnimator.duration = 200
        rotationAnimator.interpolator = DecelerateInterpolator()
        rotationAnimator.start()
    }
    
    override fun onDragStart() {
        // 拖拽开始时缩放效果
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
        
        // 如果菜单显示中，先隐藏菜单
        if (isMenuVisible) {
            hideMenu()
        }
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
        if (isMenuVisible) {
            hideMenu()
        }
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
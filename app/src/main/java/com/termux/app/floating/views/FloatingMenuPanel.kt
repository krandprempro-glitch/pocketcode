package com.termux.app.floating.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.termux.R
import com.termux.shared.logger.Logger

/**
 * 悬浮菜单面板
 * 悬浮按钮展开的菜单面板
 */
class FloatingMenuPanel(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val LOG_TAG = "FloatingMenuPanel"
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var menuContainer: LinearLayout
    private var isShowing = false
    private var menuItemClickListener: OnMenuItemClickListener? = null
    
    interface OnMenuItemClickListener {
        fun onSSHConnectionClicked()
        fun onRunCommandClicked()
        fun onQuickSettingsClicked()
    }
    
    init {
        setupLayoutParams()
        initView()
    }
    
    private fun setupLayoutParams() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        layoutParams.gravity = Gravity.TOP or Gravity.START
    }
    
    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.floating_menu_panel, this, true)
        menuContainer = findViewById(R.id.ll_menu_container)
        
        // 设置背景和阴影
        setBackgroundResource(R.drawable.bg_floating_menu)
        elevation = dpToPx(12).toFloat()
        
        setupMenuItems()
    }
    
    private fun setupMenuItems() {
        // SSH连接菜单项
        val sshItem = createMenuItem(R.drawable.ic_ssh, "SSH连接") {
            onSSHConnectionClicked()
        }
        menuContainer.addView(sshItem)
        
        // 添加分割线
        menuContainer.addView(createDivider())
        
        // 运行命令菜单项
        val runItem = createMenuItem(R.drawable.ic_run, "运行命令") {
            onRunCommandClicked()
        }
        menuContainer.addView(runItem)
        
        // 添加分割线
        menuContainer.addView(createDivider())
        
        // 快捷设置菜单项
        val settingsItem = createMenuItem(R.drawable.ic_settings, "快捷设置") {
            onQuickSettingsClicked()
        }
        menuContainer.addView(settingsItem)
    }
    
    private fun createMenuItem(iconRes: Int, text: String, clickAction: () -> Unit): View {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_floating_menu, null)
        
        val iconView = itemView.findViewById<ImageView>(R.id.iv_menu_icon)
        val textView = itemView.findViewById<TextView>(R.id.tv_menu_text)
        
        iconView.setImageResource(iconRes)
        textView.text = text
        
        itemView.setOnClickListener { v ->
            animateItemClick(v, clickAction)
        }
        
        // 添加点击效果
        itemView.setBackgroundResource(R.drawable.bg_menu_item_selector)
        
        return itemView
    }
    
    private fun createDivider(): View {
        val divider = View(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(1)
        )
        params.setMargins(dpToPx(16), 0, dpToPx(16), 0)
        divider.layoutParams = params
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        return divider
    }
    
    private fun animateItemClick(itemView: View, clickAction: () -> Unit) {
        // 点击动画
        itemView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .withEndAction(clickAction)
                    .start()
            }
            .start()
    }
    
    private fun onSSHConnectionClicked() {
        menuItemClickListener?.onSSHConnectionClicked()
    }
    
    private fun onRunCommandClicked() {
        menuItemClickListener?.onRunCommandClicked()
    }
    
    private fun onQuickSettingsClicked() {
        menuItemClickListener?.onQuickSettingsClicked()
    }
    
    fun showAt(x: Int, y: Int) {
        if (!isShowing) {
            layoutParams.x = x
            layoutParams.y = y
            
            try {
                windowManager.addView(this, layoutParams)
                isShowing = true
                
                // 显示动画
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
                
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Failed to show menu: ${e.message}")
            }
        }
    }
    
    fun hide() {
        if (isShowing) {
            // 隐藏动画
            animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    try {
                        windowManager.removeView(this)
                        isShowing = false
                    } catch (e: Exception) {
                        Logger.logError(LOG_TAG, "Failed to hide menu: ${e.message}")
                    }
                }
                .start()
        }
    }
    
    fun getMenuHeight(): Int {
        // 测量菜单高度
        measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        return measuredHeight
    }
    
    fun setOnMenuItemClickListener(listener: OnMenuItemClickListener?) {
        this.menuItemClickListener = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
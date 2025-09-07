package com.termux.app.floating.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.termux.R

/**
 * 悬浮Dialog视图
 * 在屏幕中央显示类似Dialog的悬浮窗
 */
class FloatingDialogView(context: Context) : FrameLayout(context) {
    
    interface OnDialogItemClickListener {
        fun onSSHConnectionClicked()
        fun onRunCommandClicked()
        fun onQuickSettingsClicked()
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var listener: OnDialogItemClickListener? = null
    private var isShowing = false
    
    private lateinit var dialogCard: CardView
    private lateinit var backgroundView: View
    
    init {
        setupView()
        setupLayoutParams()
    }
    
    private fun setupView() {
        // 创建背景遮罩
        backgroundView = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = 0f
            setOnClickListener {
                // 点击背景关闭dialog
                dismiss()
            }
        }
        addView(backgroundView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        
        // 创建dialog卡片
        dialogCard = CardView(context).apply {
            cardElevation = dpToPx(16).toFloat()
            radius = dpToPx(12).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
        
        // 创建dialog内容
        val dialogContent = createDialogContent()
        dialogCard.addView(dialogContent)
        
        // 居中显示dialog
        val dialogParams = LayoutParams(
            dpToPx(280),
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(dialogCard, dialogParams)
        
        // 初始状态：缩放为0，不可见
        dialogCard.scaleX = 0f
        dialogCard.scaleY = 0f
        dialogCard.alpha = 0f
    }
    
    private fun createDialogContent(): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(16))
        }
        
        // 标题
        val title = TextView(context).apply {
            text = "悬浮菜单"
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(title, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        
        // 分隔线
        val divider = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        val dividerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
        dividerParams.setMargins(0, dpToPx(16), 0, dpToPx(8))
        container.addView(divider, dividerParams)
        
        // 菜单项
        val menuItems = listOf(
            "SSH连接" to { listener?.onSSHConnectionClicked() },
            "运行命令" to { listener?.onRunCommandClicked() },
            "快捷设置" to { listener?.onQuickSettingsClicked() }
        )
        
        menuItems.forEach { (text, action) ->
            val menuItem = createMenuItem(text) {
                action.invoke()
                dismiss()
            }
            container.addView(menuItem)
        }
        
        return container
    }
    
    private fun createMenuItem(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            
            // 设置点击效果
            background = createSelectableBackground()
            setOnClickListener {
                onClick()
            }
        }
    }
    
    private fun createSelectableBackground(): android.graphics.drawable.Drawable {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return ContextCompat.getDrawable(context, typedValue.resourceId)!!
    }
    
    private fun setupLayoutParams() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
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
        layoutParams.x = 0
        layoutParams.y = 0
    }
    
    fun show() {
        if (!isShowing) {
            try {
                windowManager.addView(this, layoutParams)
                isShowing = true
                animateShow()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun dismiss() {
        if (isShowing) {
            animateDismiss()
        }
    }
    
    private fun animateShow() {
        // 背景淡入
        val backgroundFadeIn = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 0.5f)
        
        // Dialog弹出动画
        val scaleX = ObjectAnimator.ofFloat(dialogCard, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(dialogCard, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(dialogCard, "alpha", 0f, 1f)
        
        val animatorSet = AnimatorSet().apply {
            playTogether(backgroundFadeIn, scaleX, scaleY, alpha)
            duration = 300
            interpolator = OvershootInterpolator(1.2f)
        }
        
        animatorSet.start()
    }
    
    private fun animateDismiss() {
        // 背景淡出
        val backgroundFadeOut = ObjectAnimator.ofFloat(backgroundView, "alpha", 0.5f, 0f)
        
        // Dialog缩小动画
        val scaleX = ObjectAnimator.ofFloat(dialogCard, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(dialogCard, "scaleY", 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(dialogCard, "alpha", 1f, 0f)
        
        val animatorSet = AnimatorSet().apply {
            playTogether(backgroundFadeOut, scaleX, scaleY, alpha)
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                try {
                    windowManager.removeView(this@FloatingDialogView)
                    isShowing = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        
        animatorSet.start()
    }
    
    fun setOnDialogItemClickListener(listener: OnDialogItemClickListener) {
        this.listener = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
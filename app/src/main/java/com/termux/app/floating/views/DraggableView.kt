package com.termux.app.floating.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.termux.shared.logger.Logger

/**
 * 可拖拽视图基类
 * 提供拖拽功能的基础视图组件
 */
abstract class DraggableView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val LOG_TAG = "DraggableView"
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    init {
        setupLayoutParams()
        setupTouchListener()
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
        
        // 默认位置：右下角
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = screenWidth - dpToPx(60) // 距离右边缘60dp
        layoutParams.y = screenHeight - dpToPx(120) // 距离底部120dp
    }
    
    private fun setupTouchListener() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onTouchDown(event)
                MotionEvent.ACTION_MOVE -> onTouchMove(event)
                MotionEvent.ACTION_UP -> onTouchUp(event)
                else -> false
            }
        }
    }
    
    private fun onTouchDown(event: MotionEvent): Boolean {
        initialX = layoutParams.x.toFloat()
        initialY = layoutParams.y.toFloat()
        initialTouchX = event.rawX
        initialTouchY = event.rawY
        
        onDragStart()
        return true
    }
    
    private fun onTouchMove(event: MotionEvent): Boolean {
        if (!isDragging && (kotlin.math.abs(event.rawX - initialTouchX) > 10 ||
                    kotlin.math.abs(event.rawY - initialTouchY) > 10)) {
            isDragging = true
            onDragMove()
        }
        
        if (isDragging) {
            layoutParams.x = (initialX + (event.rawX - initialTouchX)).toInt()
            layoutParams.y = (initialY + (event.rawY - initialTouchY)).toInt()
            
            // 限制拖拽范围
            constrainPosition()
            
            windowManager.updateViewLayout(this, layoutParams)
            return true
        }
        
        return false
    }
    
    private fun onTouchUp(event: MotionEvent): Boolean {
        if (isDragging) {
            isDragging = false
            snapToEdge()
            onDragEnd()
            return true
        } else {
            // 如果不是拖拽，则视为点击
            performClick()
            return true
        }
    }
    
    /**
     * 约束拖拽位置，限制在屏幕右侧边缘
     */
    private fun constrainPosition() {
        val viewWidth = width
        val viewHeight = height
        
        // 限制在屏幕右半部分
        layoutParams.x = maxOf(screenWidth / 2, minOf(screenWidth - viewWidth, layoutParams.x))
        
        // 限制在屏幕范围内
        layoutParams.y = maxOf(0, minOf(screenHeight - viewHeight, layoutParams.y))
    }
    
    /**
     * 松手后自动吸附到边缘
     */
    private fun snapToEdge() {
        val targetX = screenWidth - width // 吸附到右边缘
        
        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)
        animator.duration = 200
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            layoutParams.x = animation.animatedValue as Int
            windowManager.updateViewLayout(this, layoutParams)
        }
        animator.start()
    }
    
    // 抽象方法，子类实现
    protected abstract fun onDragStart()
    protected abstract fun onDragMove()
    protected abstract fun onDragEnd()
    
    // 工具方法
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    
    private val screenWidth: Int
        get() {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            return metrics.widthPixels
        }
    
    private val screenHeight: Int
        get() {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            return metrics.heightPixels
        }
    
    fun show() {
        try {
            windowManager.addView(this, layoutParams)
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to show floating view: ${e.message}")
        }
    }
    
    open fun hide() {
        try {
            windowManager.removeView(this)
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to hide floating view: ${e.message}")
        }
    }
}